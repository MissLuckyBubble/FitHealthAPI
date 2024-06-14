package fit.health.fithealthapi.services;

import fit.health.fithealthapi.exceptions.CustomException;
import fit.health.fithealthapi.model.Recipe;
import fit.health.fithealthapi.model.User;
import lombok.Getter;
import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.util.OWLEntityRemover;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Calendar;
import java.util.Collections;
import java.util.List;

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

    public UserService(DataPropertyService dataPropertyService, ObjectPropertyService objectPropertyService, OntologyService ontologyService) {
        this.dataPropertyService = dataPropertyService;
        this.objectPropertyService = objectPropertyService;
        this.ontologyService = ontologyService;

        init();
    }

    private void init(){
        ontoManager = ontologyService.getOntoManager();
        dataFactory = ontologyService.getDataFactory();
        ontology = ontologyService.getOntology();
        ontologyIRIStr = ontologyService.getOntologyIRIStr();
    }

    public void createUser(User user) {
        OWLNamedIndividual userIndividual = dataFactory.getOWLNamedIndividual(IRI.create(ontologyIRIStr + user.getUsername().replace(" ", "_")));
        if (ontology.containsIndividualInSignature(userIndividual.getIRI())) {
            throw new CustomException("User already exists!");
        }

        OWLClass userClass = dataFactory.getOWLClass(IRI.create(ontologyIRIStr + "User"));
        OWLAxiom classAssertion = dataFactory.getOWLClassAssertionAxiom(userClass, userIndividual);
        ontoManager.addAxiom(ontology, classAssertion);

        dataPropertyService.addDataProperty(userIndividual, "username", user.getUsername());
        String encodedPassword = passwordEncoder.encode(user.getPassword());
        dataPropertyService.addDataProperty(userIndividual, "password", encodedPassword);
        dataPropertyService.addDataProperty(userIndividual, "birthDate", user.getBirthDate());
        dataPropertyService.addDataProperty(userIndividual, "weightKG", user.getWeightKG());
        dataPropertyService.addDataProperty(userIndividual, "goalWeight", user.getGoalWeight());
        dataPropertyService.addDataProperty(userIndividual, "heightCM", user.getHeightCM());

        // Calculate and add dailyCalorieGoal
        float dailyCalorieGoal = calculateDailyCalorieGoal(user);
        dataPropertyService.addDataProperty(userIndividual, "dailyCalorieGoal", dailyCalorieGoal);

        // Add gender
        OWLNamedIndividual genderIndividual = dataFactory.getOWLNamedIndividual(IRI.create(ontologyIRIStr + user.getGender()));
        OWLObjectProperty hasGender = dataFactory.getOWLObjectProperty(IRI.create(ontologyIRIStr + "hasGender"));
        OWLAxiom genderAxiom = dataFactory.getOWLObjectPropertyAssertionAxiom(hasGender, userIndividual, genderIndividual);
        ontoManager.addAxiom(ontology, genderAxiom);

        objectPropertyService.addObjectProperties(userIndividual, "userHasDietaryPreference", user.getDietaryPreferences());
        objectPropertyService.addObjectProperties(userIndividual, "userHasHealthCondition", user.getHealthConditions());

        ontologyService.saveOntology();
    }

    private float calculateDailyCalorieGoal(User user) {
        String[] birthDateParts = user.getBirthDate().split("-");
        int birthYear = Integer.parseInt(birthDateParts[0]);
        int currentYear = Calendar.getInstance().get(Calendar.YEAR);
        int age = currentYear - birthYear;

        if ("Male".equals(user.getGender())) {
            return (float) ((10 * user.getWeightKG()) + (6.25 * user.getHeightCM()) - (5 * age) + 5);
        } else if ("Female".equals(user.getGender())) {
            return (float) ((10 * user.getWeightKG()) + (6.25 * user.getHeightCM()) - (5 * age) - 161);
        } else {
            throw new CustomException("Invalid gender value");
        }
    }

    public User loginUser(String username, String password) {
        OWLNamedIndividual userIndividual = dataFactory.getOWLNamedIndividual(IRI.create(ontologyIRIStr + username));
        String storedPassword = dataPropertyService.getDataPropertyValue(userIndividual, "password");

        if (storedPassword != null && passwordEncoder.matches(password, storedPassword)) {
            return getUser(userIndividual);
        } else {
            throw new CustomException("Invalid username or password");
        }
    }

    private User getUser(OWLNamedIndividual individual) {
        User user = new User();
        user.setId(individual.getIRI().toString());
        user.setUsername(dataPropertyService.getDataPropertyValue(individual, "username"));
        user.setPassword(dataPropertyService.getDataPropertyValue(individual, "password"));
        user.setBirthDate(dataPropertyService.getDataPropertyValue(individual, "birthDate"));
        user.setWeightKG(dataPropertyService.getFloatValue(individual, "weightKG"));
        user.setGoalWeight(dataPropertyService.getFloatValue(individual, "goalWeight"));
        user.setHeightCM(dataPropertyService.getFloatValue(individual, "heightCM"));
        user.setDailyCalorieGoal(dataPropertyService.getFloatValue(individual, "dailyCalorieGoal"));
        user.setGender(getGender(individual));
        user.setDietaryPreferences(objectPropertyService.getObjectPropertyValues(individual, "userHasDietaryPreference"));
        user.setHealthConditions(objectPropertyService.getObjectPropertyValues(individual, "userHasHealthCondition"));
        return user;
    }

    private String getGender(OWLNamedIndividual individual) {
        return objectPropertyService.getObjectPropertyValues(individual, "hasGender").stream()
                .map(ontologyService::getFragment)
                .findFirst()
                .orElse(null);
    }

    public void editUser(User user, String oldPassword) {
        OWLNamedIndividual userIndividual = dataFactory.getOWLNamedIndividual(IRI.create(ontologyIRIStr + user.getUsername()));
        String storedPassword = dataPropertyService.getDataPropertyValue(userIndividual, "password");

        if (storedPassword == null || !passwordEncoder.matches(oldPassword, storedPassword)) {
            throw new CustomException("Invalid old password");
        }

        dataPropertyService.removeDataProperties(userIndividual, "weightKG");
        dataPropertyService.removeDataProperties(userIndividual, "goalWeight");
        dataPropertyService.removeDataProperties(userIndividual, "heightCM");
        dataPropertyService.removeDataProperties(userIndividual, "password");
        dataPropertyService.removeDataProperties(userIndividual, "dailyCalorieGoal");

        dataPropertyService.addDataProperty(userIndividual, "weightKG", user.getWeightKG());
        dataPropertyService.addDataProperty(userIndividual, "goalWeight", user.getGoalWeight());
        dataPropertyService.addDataProperty(userIndividual, "heightCM", user.getHeightCM());
        String encodedPassword = passwordEncoder.encode(user.getPassword());
        dataPropertyService.addDataProperty(userIndividual, "password", encodedPassword);

        float dailyCalorieGoal = calculateDailyCalorieGoal(user);
        dataPropertyService.addDataProperty(userIndividual, "dailyCalorieGoal", dailyCalorieGoal);

        objectPropertyService.removeObjectProperties(userIndividual, "userHasDietaryPreference");
        objectPropertyService.removeObjectProperties(userIndividual, "userHasHealthCondition");
        objectPropertyService.removeObjectProperties(userIndividual, "hasGender");

        objectPropertyService.addObjectProperties(userIndividual, "userHasDietaryPreference", user.getDietaryPreferences());
        objectPropertyService.addObjectProperties(userIndividual, "userHasHealthCondition", user.getHealthConditions());

        OWLNamedIndividual genderIndividual = dataFactory.getOWLNamedIndividual(IRI.create(ontologyIRIStr + user.getGender()));
        OWLObjectProperty hasGender = dataFactory.getOWLObjectProperty(IRI.create(ontologyIRIStr + "hasGender"));
        OWLAxiom genderAxiom = dataFactory.getOWLObjectPropertyAssertionAxiom(hasGender, userIndividual, genderIndividual);
        ontoManager.addAxiom(ontology, genderAxiom);

        ontologyService.saveOntology();
    }

    public void removeUser(String userId) {
        OWLNamedIndividual userIndividual = dataFactory.getOWLNamedIndividual(IRI.create(ontologyIRIStr + userId));
        OWLEntityRemover remover = new OWLEntityRemover(ontoManager, Collections.singleton(ontology));
        userIndividual.accept(remover);
        ontoManager.applyChanges(remover.getChanges());

        ontologyService.saveOntology();
    }

    public User getUserByUsername(String username) {
        OWLNamedIndividual userIndividual = dataFactory.getOWLNamedIndividual(IRI.create(ontologyIRIStr + username.replace(" ", "_")));
        if (ontology.containsIndividualInSignature(userIndividual.getIRI())) {
            return getUser(userIndividual);
        }
        return null;
    }

    public void saveMealPlan(String username, List<Recipe> mealPlan) {
        OWLNamedIndividual userIndividual = dataFactory.getOWLNamedIndividual(IRI.create(ontologyIRIStr + username.replace(" ", "_")));
        OWLNamedIndividual mealPlanIndividual = dataFactory.getOWLNamedIndividual(IRI.create(ontologyIRIStr + username.replace(" ", "_") + "_MealPlan"));
        OWLClass mealPlanClass = dataFactory.getOWLClass(IRI.create(ontologyIRIStr + "MealPlan"));

        // Create meal plan individual
        OWLAxiom classAssertion = dataFactory.getOWLClassAssertionAxiom(mealPlanClass, mealPlanIndividual);
        ontoManager.addAxiom(ontology, classAssertion);

        // Add meal plan name
        dataPropertyService.addDataProperty(mealPlanIndividual, "mealPlanName", username + "'s Meal Plan (" + now()+")" );

        // Add recipes to meal plan
        float totalCalories = 0;
        for (Recipe recipe : mealPlan) {
            OWLNamedIndividual recipeIndividual = dataFactory.getOWLNamedIndividual(IRI.create(ontologyIRIStr + recipe.getRecipeName().replace(" ", "_")));
            OWLObjectProperty hasRecipe = dataFactory.getOWLObjectProperty(IRI.create(ontologyIRIStr + "hasRecipe"));
            OWLAxiom axiom = dataFactory.getOWLObjectPropertyAssertionAxiom(hasRecipe, mealPlanIndividual, recipeIndividual);
            ontoManager.addAxiom(ontology, axiom);
            totalCalories += recipe.getCaloriesPer100gram();

        }

        dataPropertyService.addDataProperty(mealPlanIndividual, "hasTotalCalories", totalCalories);


        OWLObjectProperty hasMealPlan = dataFactory.getOWLObjectProperty(IRI.create(ontologyIRIStr + "hasMealPlan"));
        OWLAxiom linkAxiom = dataFactory.getOWLObjectPropertyAssertionAxiom(hasMealPlan, userIndividual, mealPlanIndividual);
        ontoManager.addAxiom(ontology, linkAxiom);

        ontologyService.saveOntology();
    }

    public List<Recipe> getMealPlan(String username) {
        //TODO
        return  null;
    }
}
