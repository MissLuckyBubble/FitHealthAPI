package fit.health.fithealthapi.services;

import fit.health.fithealthapi.exceptions.CustomException;
import fit.health.fithealthapi.model.MealPlan;
import fit.health.fithealthapi.model.Recipe;
import fit.health.fithealthapi.model.User;
import lombok.Getter;
import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.util.OWLEntityRemover;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;

import static java.time.LocalTime.now;

@Service
public class UserService {

    @Autowired
    DataPropertyService dataPropertyService;
    @Autowired
    ObjectPropertyService objectPropertyService;
    @Autowired
    OntologyService ontologyService;

    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    private OWLOntologyManager ontoManager;
    private OWLDataFactory dataFactory;
    private OWLOntology ontology;
    private String ontologyIRIStr;

    // Cache for frequently accessed data
    private ConcurrentMap<String, OWLNamedIndividual> individualCache = new ConcurrentHashMap<>();

    public UserService(DataPropertyService dataPropertyService, ObjectPropertyService objectPropertyService, OntologyService ontologyService) {
        this.dataPropertyService = dataPropertyService;
        this.objectPropertyService = objectPropertyService;
        this.ontologyService = ontologyService;
        init();
    }

    private void init() {
        ontoManager = ontologyService.getOntoManager();
        dataFactory = ontologyService.getDataFactory();
        ontology = ontologyService.getOntology();
        ontologyIRIStr = ontologyService.getOntologyIRIStr();
    }

    public void createUser(User user) {
        IRI userIRI = IRI.create(ontologyIRIStr + user.getUsername().replace(" ", "_"));
        OWLNamedIndividual userIndividual = dataFactory.getOWLNamedIndividual(userIRI);

        if (ontology.containsIndividualInSignature(userIRI)) {
            throw new CustomException("User already exists!");
        }

        OWLClass userClass = dataFactory.getOWLClass(IRI.create(ontologyIRIStr + "User"));
        OWLAxiom classAssertion = dataFactory.getOWLClassAssertionAxiom(userClass, userIndividual);

        List<OWLOntologyChange> changes = new ArrayList<>();
        changes.add(new AddAxiom(ontology, classAssertion));

        addDataPropertyChanges(changes, userIndividual, "username", user.getUsername());
        addDataPropertyChanges(changes, userIndividual, "password", passwordEncoder.encode(user.getPassword()));
        addDataPropertyChanges(changes, userIndividual, "birthDate", user.getBirthDate());
        addDataPropertyChanges(changes, userIndividual, "weightKG", user.getWeightKG());
        addDataPropertyChanges(changes, userIndividual, "goalWeight", user.getGoalWeight());
        addDataPropertyChanges(changes, userIndividual, "heightCM", user.getHeightCM());
        addDataPropertyChanges(changes, userIndividual, "dailyCalorieGoal", calculateDailyCalorieGoal(user));

        OWLNamedIndividual genderIndividual = dataFactory.getOWLNamedIndividual(IRI.create(ontologyIRIStr + user.getGender()));
        OWLObjectProperty hasGender = dataFactory.getOWLObjectProperty(IRI.create(ontologyIRIStr + "hasGender"));
        OWLAxiom genderAxiom = dataFactory.getOWLObjectPropertyAssertionAxiom(hasGender, userIndividual, genderIndividual);
        changes.add(new AddAxiom(ontology, genderAxiom));

        changes.addAll(createObjectPropertiesChanges(userIndividual, "userHasDietaryPreference", user.getDietaryPreferences()));
        changes.addAll(createObjectPropertiesChanges(userIndividual, "userHasHealthCondition", user.getHealthConditions()));

        ontoManager.applyChanges(changes);
        ontologyService.saveOntology();

        // Update cache
        individualCache.put(user.getUsername(), userIndividual);
    }

    private float calculateDailyCalorieGoal(User user) {
        String[] birthDateParts = user.getBirthDate().split("-");
        int birthYear = Integer.parseInt(birthDateParts[0]);
        int currentYear = Calendar.getInstance().get(Calendar.YEAR);
        int age = currentYear - birthYear;

        switch (user.getGender()) {
            case "Male":
                return (float) ((10 * user.getWeightKG()) + (6.25 * user.getHeightCM()) - (5 * age) + 5);
            case "Female":
                return (float) ((10 * user.getWeightKG()) + (6.25 * user.getHeightCM()) - (5 * age) - 161);
            case "PreferNotToSay":
                return (float) ((10 * user.getWeightKG()) + (6.25 * user.getHeightCM()) - (5 * age));
            default:
                throw new CustomException("Invalid gender value");
        }
    }

    public User loginUser(String username, String password) {
        OWLNamedIndividual userIndividual = getCachedIndividual(username);
        String storedPassword = dataPropertyService.getDataPropertyValue(userIndividual, "password");

        if (storedPassword != null && passwordEncoder.matches(password, storedPassword)) {
            return getUser(userIndividual);
        } else {
            throw new CustomException("Invalid username or password");
        }
    }

    private User getUser(OWLNamedIndividual individual) {
        return new User(
                individual.getIRI().toString(),
                dataPropertyService.getDataPropertyValue(individual, "username"),
                dataPropertyService.getDataPropertyValue(individual, "password"),
                dataPropertyService.getDataPropertyValue(individual, "birthDate"),
                dataPropertyService.getFloatValue(individual, "weightKG"),
                dataPropertyService.getFloatValue(individual, "goalWeight"),
                dataPropertyService.getFloatValue(individual, "heightCM"),
                dataPropertyService.getFloatValue(individual, "dailyCalorieGoal"),
                getGender(individual),
                objectPropertyService.getObjectPropertyValues(individual, "userHasDietaryPreference"),
                objectPropertyService.getObjectPropertyValues(individual, "userHasHealthCondition")
        );
    }

    private String getGender(OWLNamedIndividual individual) {
        return objectPropertyService.getObjectPropertyValues(individual, "hasGender").stream()
                .map(ontologyService::getFragment)
                .findFirst()
                .orElse(null);
    }

    public void editUser(User user, String oldPassword) {
        OWLNamedIndividual userIndividual = getCachedIndividual(user.getUsername());
        String storedPassword = dataPropertyService.getDataPropertyValue(userIndividual, "password");

        if (storedPassword == null || !passwordEncoder.matches(oldPassword, storedPassword)) {
            throw new CustomException("Invalid old password");
        }

        List<OWLOntologyChange> changes = new ArrayList<>();

        changes.addAll(createRemoveDataPropertiesChanges(userIndividual, "weightKG"));
        changes.addAll(createRemoveDataPropertiesChanges(userIndividual, "goalWeight"));
        changes.addAll(createRemoveDataPropertiesChanges(userIndividual, "heightCM"));
        changes.addAll(createRemoveDataPropertiesChanges(userIndividual, "password"));
        changes.addAll(createRemoveDataPropertiesChanges(userIndividual, "dailyCalorieGoal"));

        addDataPropertyChanges(changes, userIndividual, "weightKG", user.getWeightKG());
        addDataPropertyChanges(changes, userIndividual, "goalWeight", user.getGoalWeight());
        addDataPropertyChanges(changes, userIndividual, "heightCM", user.getHeightCM());
        addDataPropertyChanges(changes, userIndividual, "password", passwordEncoder.encode(user.getPassword()));
        addDataPropertyChanges(changes, userIndividual, "dailyCalorieGoal", calculateDailyCalorieGoal(user));

        changes.addAll(createRemoveObjectPropertiesChanges(userIndividual, "userHasDietaryPreference"));
        changes.addAll(createRemoveObjectPropertiesChanges(userIndividual, "userHasHealthCondition"));
        changes.addAll(createRemoveObjectPropertiesChanges(userIndividual, "hasGender"));

        changes.addAll(createObjectPropertiesChanges(userIndividual, "userHasDietaryPreference", user.getDietaryPreferences()));
        changes.addAll(createObjectPropertiesChanges(userIndividual, "userHasHealthCondition", user.getHealthConditions()));

        OWLNamedIndividual genderIndividual = dataFactory.getOWLNamedIndividual(IRI.create(ontologyIRIStr + user.getGender()));
        OWLObjectProperty hasGender = dataFactory.getOWLObjectProperty(IRI.create(ontologyIRIStr + "hasGender"));
        OWLAxiom genderAxiom = dataFactory.getOWLObjectPropertyAssertionAxiom(hasGender, userIndividual, genderIndividual);
        changes.add(new AddAxiom(ontology, genderAxiom));

        ontoManager.applyChanges(changes);
        ontologyService.saveOntology();

        // Update cache
        individualCache.put(user.getUsername(), userIndividual);
    }

    public void removeUser(String userId) {
        OWLNamedIndividual userIndividual = getCachedIndividual(userId);
        OWLEntityRemover remover = new OWLEntityRemover(ontoManager, Collections.singleton(ontology));
        userIndividual.accept(remover);
        ontoManager.applyChanges(remover.getChanges());
        ontologyService.saveOntology();
        individualCache.remove(userId);
    }

    public User getUserByUsername(String username) {
        OWLNamedIndividual userIndividual = getCachedIndividual(username);
        if (ontology.containsIndividualInSignature(userIndividual.getIRI())) {
            return getUser(userIndividual);
        }
        return null;
    }

    public void saveMealPlan(String username, List<Recipe> mealPlan) {
        OWLNamedIndividual userIndividual = getCachedIndividual(username);
        OWLNamedIndividual mealPlanIndividual = dataFactory.getOWLNamedIndividual(IRI.create(ontologyIRIStr + username.replace(" ", "_") + "_MealPlan_"+ now()));
        OWLClass mealPlanClass = dataFactory.getOWLClass(IRI.create(ontologyIRIStr + "MealPlan"));


        List<OWLOntologyChange> changes = new ArrayList<>();
        changes.add(new AddAxiom(ontology, dataFactory.getOWLClassAssertionAxiom(mealPlanClass, mealPlanIndividual)));
        changes.add(new AddAxiom(ontology, dataFactory.getOWLDataPropertyAssertionAxiom(dataFactory.getOWLDataProperty(IRI.create(ontologyIRIStr + "mealPlanName")), mealPlanIndividual, username + "'s Meal Plan")));

        float totalCalories = 0;
        for (Recipe recipe : mealPlan) {
            OWLNamedIndividual recipeIndividual = dataFactory.getOWLNamedIndividual(IRI.create(recipe.getId()));
            changes.add(new AddAxiom(ontology, dataFactory.getOWLObjectPropertyAssertionAxiom(dataFactory.getOWLObjectProperty(IRI.create(ontologyIRIStr + "hasRecipe")), mealPlanIndividual, recipeIndividual)));
            totalCalories += recipe.getCaloriesPer100gram();
        }

        changes.add(new AddAxiom(ontology, dataFactory.getOWLDataPropertyAssertionAxiom(dataFactory.getOWLDataProperty(IRI.create(ontologyIRIStr + "hasTotalCalories")), mealPlanIndividual, totalCalories)));
        changes.add(new AddAxiom(ontology, dataFactory.getOWLObjectPropertyAssertionAxiom(dataFactory.getOWLObjectProperty(IRI.create(ontologyIRIStr + "hasMealPlan")), userIndividual, mealPlanIndividual)));

        ontoManager.applyChanges(changes);
        ontologyService.saveOntology();
    }

    public List<MealPlan> getMealPlans(String username) {
        OWLNamedIndividual userIndividual = getCachedIndividual(username);
        List<String> mealPlanIRIs = objectPropertyService.getObjectPropertyValues(userIndividual, "hasMealPlan");

        List<MealPlan> mealPlans = new ArrayList<>();
        for (String mpIRI : mealPlanIRIs) {
            System.out.println(mpIRI);
            OWLNamedIndividual mealPlanIndividual = dataFactory.getOWLNamedIndividual(IRI.create(ontologyIRIStr + mpIRI));
            String mealPlanId = mealPlanIndividual.getIRI().toString();

            String mealPlanName = dataPropertyService.getDataPropertyValue(mealPlanIndividual, "mealPlanName");
            float totalCalories = dataPropertyService.getFloatValue(mealPlanIndividual, "hasTotalCalories");

            List<String> recipeIRIs = objectPropertyService.getObjectPropertyValues(mealPlanIndividual, "hasRecipe");
            List<String> recipes = new ArrayList<>();
            for (String recipeIRI : recipeIRIs) {
                OWLNamedIndividual recipeIndividual = dataFactory.getOWLNamedIndividual(IRI.create(ontologyIRIStr + recipeIRI));
                String recipeName = dataPropertyService.getDataPropertyValue(recipeIndividual, "recipeName");
                if (recipeName == null || recipeName.isEmpty()) {
                    String iriFragment = ontologyService.getFragment(recipeIRI);
                    recipeName = iriFragment.replace('_', ' ');
                }
                recipes.add(recipeName);
            }

            mealPlans.add(new MealPlan(mealPlanId, mealPlanName, recipes, totalCalories));
        }
        return mealPlans;
    }
    public void deleteMealPlan(String mealPlanIdFragment) {
        // Construct the full IRI for the meal plan individual
        IRI mealPlanIRI = IRI.create(ontologyIRIStr + mealPlanIdFragment);

        // Retrieve the meal plan individual
        OWLNamedIndividual mealPlanIndividual = dataFactory.getOWLNamedIndividual(mealPlanIRI);

        // Check if the individual exists in the ontology
        if (!ontology.containsIndividualInSignature(mealPlanIRI)) {
            throw new CustomException("Meal Plan not found with ID: " + mealPlanIdFragment);
        }

        // Use OWLEntityRemover to remove the individual
        OWLEntityRemover remover = new OWLEntityRemover(ontoManager, Collections.singleton(ontology));
        mealPlanIndividual.accept(remover);
        ontoManager.applyChanges(remover.getChanges());
        ontologyService.saveOntology();

        // Optionally, log or notify about the deletion
        System.out.println("Deleted Meal Plan with ID: " + mealPlanIdFragment);
    }


    private OWLNamedIndividual getCachedIndividual(String username) {
        return individualCache.computeIfAbsent(username, key -> dataFactory.getOWLNamedIndividual(IRI.create(ontologyIRIStr + key.replace(" ", "_"))));
    }

    private void addDataPropertyChanges(List<OWLOntologyChange> changes, OWLNamedIndividual individual, String propertyName, Object value) {
        OWLDataProperty property = dataFactory.getOWLDataProperty(IRI.create(ontologyIRIStr + propertyName));
        OWLAxiom axiom;
        if (value instanceof String) {
            axiom = dataFactory.getOWLDataPropertyAssertionAxiom(property, individual, (String) value);
        } else if (value instanceof Float) {
            axiom = dataFactory.getOWLDataPropertyAssertionAxiom(property, individual, (Float) value);
        } else if (value instanceof Integer) {
            axiom = dataFactory.getOWLDataPropertyAssertionAxiom(property, individual, (Integer) value);
        } else {
            throw new IllegalArgumentException("Unsupported data property value type");
        }
        changes.add(new AddAxiom(ontology, axiom));
    }

    private List<OWLOntologyChange> createRemoveDataPropertiesChanges(OWLNamedIndividual individual, String propertyName) {
        OWLDataProperty property = dataFactory.getOWLDataProperty(IRI.create(ontologyIRIStr + propertyName));
        Set<OWLAxiom> axiomsToRemove = ontology.getDataPropertyAssertionAxioms(individual).stream()
                .filter(axiom -> axiom.getProperty().equals(property))
                .collect(Collectors.toSet());
        return axiomsToRemove.stream()
                .map(axiom -> new RemoveAxiom(ontology, axiom))
                .collect(Collectors.toList());
    }

    private List<OWLOntologyChange> createObjectPropertiesChanges(OWLNamedIndividual individual, String propertyName, List<String> values) {
        List<OWLOntologyChange> changes = new ArrayList<>();
        if (values != null) {
            OWLObjectProperty property = dataFactory.getOWLObjectProperty(IRI.create(ontologyIRIStr + propertyName));
            for (String value : values) {
                OWLNamedIndividual objectIndividual = dataFactory.getOWLNamedIndividual(IRI.create(ontologyIRIStr + value));
                OWLAxiom axiom = dataFactory.getOWLObjectPropertyAssertionAxiom(property, individual, objectIndividual);
                changes.add(new AddAxiom(ontology, axiom));
            }
        }
        return changes;
    }

    private List<OWLOntologyChange> createRemoveObjectPropertiesChanges(OWLNamedIndividual individual, String propertyName) {
        OWLObjectProperty property = dataFactory.getOWLObjectProperty(IRI.create(ontologyIRIStr + propertyName));
        Set<OWLAxiom> axiomsToRemove = ontology.getObjectPropertyAssertionAxioms(individual).stream()
                .filter(axiom -> axiom.getProperty().equals(property))
                .collect(Collectors.toSet());
        return axiomsToRemove.stream()
                .map(axiom -> new RemoveAxiom(ontology, axiom))
                .collect(Collectors.toList());
    }
}
