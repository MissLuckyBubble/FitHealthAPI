package fit.health.fithealthapi.services;

import fit.health.fithealthapi.exceptions.CustomException;
import fit.health.fithealthapi.model.FoodItem;
import fit.health.fithealthapi.model.User;
import org.semanticweb.HermiT.Reasoner;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.reasoner.OWLReasoner;
import org.semanticweb.owlapi.reasoner.OWLReasonerFactory;
import org.semanticweb.owlapi.util.OWLEntityRemover;
import org.springframework.stereotype.Service;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;


import java.io.File;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

@Service
public class OntologyService {

    private static final Logger LOGGER = Logger.getLogger(OntologyService.class.getName());
    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    private final OWLOntologyManager ontoManager;
    private final OWLDataFactory dataFactory;
    private OWLOntology ontology;
    private final String ontologyIRIStr;
    private OWLReasoner reasoner;
    private final Map<OWLClass, String> dietaryPreferencesMap = new HashMap<>();

    public OntologyService() throws OWLOntologyCreationException {
        ontoManager = OWLManager.createOWLOntologyManager();
        dataFactory = ontoManager.getOWLDataFactory();
        loadOntologyFromFile();
        ontologyIRIStr = ontology.getOntologyID().getOntologyIRI().toString() + "#";
        initializeReasoner();
        loadDietaryPreferences();
    }

    private void loadOntologyFromFile() throws OWLOntologyCreationException {
        try {
            File ontoFile = new File("src/main/java/fit/health/fithealthapi/ontology/health.owl");
            if (!ontoFile.exists()) {
                throw new OWLOntologyCreationException("Ontology file not found: " + ontoFile.getAbsolutePath());
            }
            ontology = ontoManager.loadOntologyFromOntologyDocument(ontoFile);
            LOGGER.info("Ontology loaded successfully from " + ontoFile.getAbsolutePath());
        } catch (OWLOntologyCreationException e) {
            LOGGER.log(Level.SEVERE, "Failed to load ontology", e);
            throw e;
        }
    }

    private void initializeReasoner() {
        OWLReasonerFactory reasonerFactory = new Reasoner.ReasonerFactory();
        reasoner = reasonerFactory.createReasoner(ontology);
    }

    private void loadDietaryPreferences() {
        OWLClass dietaryPreferenceClass = dataFactory.getOWLClass(IRI.create(ontologyIRIStr + "DietaryPreference"));
        Set<OWLClass> dietaryPreferences = reasoner.getSubClasses(dietaryPreferenceClass, false).getFlattened();
        for (OWLClass dietaryPreference : dietaryPreferences) {
            String shortForm = dietaryPreference.getIRI().toString();
            dietaryPreferencesMap.put(dietaryPreference, shortForm);
        }
    }

    public List<String> getDietaryPreferences() {
        LOGGER.info("Returning dietary preferences...");
        return dietaryPreferencesMap.values().stream()
                .map(this::getFragment)
                .collect(Collectors.toList());
    }

    public List<FoodItem> getFoodItems() {
        List<FoodItem> foodItems = new ArrayList<>();
        OWLClass foodItemClass = dataFactory.getOWLClass(IRI.create(ontologyIRIStr + "FoodItem"));
        Set<OWLNamedIndividual> individuals = ontology.getIndividualsInSignature();

        for (OWLNamedIndividual individual : individuals) {
            if (ontology.getClassAssertionAxioms(individual).stream()
                    .anyMatch(axiom -> axiom.getClassesInSignature().contains(foodItemClass))) {
                getFoodItem(foodItems, individual);
            }
        }
        return foodItems;
    }

    public List<FoodItem> getFoodItemsByPreference(String preference) {
        List<FoodItem> foodItems = new ArrayList<>();
        OWLClass foodItemClass = dataFactory.getOWLClass(IRI.create(ontologyIRIStr + "FoodItem"));
        OWLClass preferenceClass = dataFactory.getOWLClass(IRI.create(ontologyIRIStr + preference));
        Set<OWLNamedIndividual> individuals = ontology.getIndividualsInSignature();

        LOGGER.info("Looking for food items with preference: " + preference);

        for (OWLNamedIndividual individual : individuals) {
            if (ontology.getClassAssertionAxioms(individual).stream()
                    .anyMatch(axiom -> axiom.getClassesInSignature().contains(foodItemClass))) {
                if (reasoner.getTypes(individual, false).containsEntity(preferenceClass)) {
                    LOGGER.info("Food item " + individual.getIRI().toString() + " matches preference " + preference);
                    getFoodItem(foodItems, individual);
                } else {
                    LOGGER.info("Food item " + individual.getIRI().toString() + " does not match preference " + preference);
                }
            }
        }
        return foodItems;
    }

    public List<FoodItem> getFoodItemsByPreferences(List<String> preferences) {
        List<FoodItem> foodItems = new ArrayList<>();
        OWLClass foodItemClass = dataFactory.getOWLClass(IRI.create(ontologyIRIStr + "FoodItem"));
        Set<OWLNamedIndividual> individuals = ontology.getIndividualsInSignature();

        LOGGER.info("Looking for food items with preferences: " + preferences);

        // Convert preference strings to OWLClass objects
        List<OWLClass> preferenceClasses = preferences.stream()
                .map(pref -> dataFactory.getOWLClass(IRI.create(ontologyIRIStr + pref)))
                .collect(Collectors.toList());

        for (OWLNamedIndividual individual : individuals) {
            if (ontology.getClassAssertionAxioms(individual).stream()
                    .anyMatch(axiom -> axiom.getClassesInSignature().contains(foodItemClass))) {

                // Check if the individual matches all preference classes
                boolean matchesAllPreferences = preferenceClasses.stream()
                        .allMatch(preferenceClass -> reasoner.getTypes(individual, false).containsEntity(preferenceClass));

                if (matchesAllPreferences) {
                    LOGGER.info("Food item " + individual.getIRI().toString() + " matches all preferences " + preferences);
                    getFoodItem(foodItems, individual);
                }
            }
        }
        return foodItems;
    }


    private void getFoodItem(List<FoodItem> foodItems, OWLNamedIndividual individual) {
        FoodItem foodItem = new FoodItem();
        foodItem.setId(individual.getIRI().toString());
        String foodName = getDataPropertyValue(individual, "foodName"); // Retrieve foodName from data property
        LOGGER.info("Retrieved foodName: " + foodName + " for individual: " + individual.getIRI().toString());
        foodItem.setFoodName(foodName);
        foodItem.setCaloriesPer100gram(getFloatValue(individual, "caloriesPer100gram"));
        foodItem.setFatContent(getFloatValue(individual, "fatContent"));
        foodItem.setProteinContent(getFloatValue(individual, "proteinContent"));
        foodItem.setSugarContent(getFloatValue(individual, "sugarContent"));
        foodItem.setAllergens(getAllergens(individual));
        foodItem.setDietaryPreferences(getDietaryPreferences(individual));
        foodItems.add(foodItem);
    }

    private List<String> getDietaryPreferences(OWLNamedIndividual individual) {
        List<String> preferences = new ArrayList<>();
        for (Map.Entry<OWLClass, String> entry : dietaryPreferencesMap.entrySet()) {
            if (isIndividualOfClass(individual, entry.getKey())) {
                preferences.add(getFragment(entry.getValue()));
            }
        }
        return preferences;
    }

    public void createFoodItem(FoodItem foodItem) {


        OWLNamedIndividual foodIndividual = dataFactory.getOWLNamedIndividual(IRI.create(ontologyIRIStr + foodItem.getFoodName().replace(" ", "_")));
        if (ontology.containsIndividualInSignature(foodIndividual.getIRI())) {
            throw new CustomException("Food item already exists");
        }
        OWLClass foodItemClass = dataFactory.getOWLClass(IRI.create(ontologyIRIStr + "FoodItem"));


        OWLAxiom classAssertion = dataFactory.getOWLClassAssertionAxiom(foodItemClass, foodIndividual);
        ontoManager.addAxiom(ontology, classAssertion);

        addDataProperty(foodIndividual, "foodName", foodItem.getFoodName());
        addDataProperty(foodIndividual, "caloriesPer100gram", foodItem.getCaloriesPer100gram());
        addDataProperty(foodIndividual, "fatContent", foodItem.getFatContent());
        addDataProperty(foodIndividual, "proteinContent", foodItem.getProteinContent());
        addDataProperty(foodIndividual, "sugarContent", foodItem.getSugarContent());

        // Log the allergens being processed
        LOGGER.info("Adding allergens: " + foodItem.getAllergens());

        // Add object property assertions for allergens
        addObjectProperties(foodIndividual, "hasAllergen", foodItem.getAllergens());

        saveOntology();
    }



    public void editFoodItem(FoodItem foodItem) {
        OWLNamedIndividual foodIndividual = dataFactory.getOWLNamedIndividual(IRI.create(ontologyIRIStr + foodItem.getId()));

        // Remove existing data properties
        removeDataProperties(foodIndividual, "caloriesPer100gram");
        removeDataProperties(foodIndividual, "fatContent");
        removeDataProperties(foodIndividual, "proteinContent");
        removeDataProperties(foodIndividual, "sugarContent");

        // Add new data properties
        addDataProperty(foodIndividual, "caloriesPer100gram", foodItem.getCaloriesPer100gram());
        addDataProperty(foodIndividual, "fatContent", foodItem.getFatContent());
        addDataProperty(foodIndividual, "proteinContent", foodItem.getProteinContent());
        addDataProperty(foodIndividual, "sugarContent", foodItem.getSugarContent());

        // Remove existing object properties
        removeObjectProperties(foodIndividual, "hasAllergen");

        // Add new object properties
        addObjectProperties(foodIndividual, "hasAllergen", foodItem.getAllergens());

        saveOntology();
    }

    public void removeFoodItem(String foodItemId) {
        OWLNamedIndividual foodIndividual = dataFactory.getOWLNamedIndividual(IRI.create(ontologyIRIStr + foodItemId));
        OWLEntityRemover remover = new OWLEntityRemover(ontoManager, Collections.singleton(ontology));
        foodIndividual.accept(remover);
        ontoManager.applyChanges(remover.getChanges());

        saveOntology();
    }

    private void addDataProperty(OWLNamedIndividual individual, String propertyName, String value) {
        OWLDataProperty property = dataFactory.getOWLDataProperty(IRI.create(ontologyIRIStr + propertyName));
        OWLAxiom axiom = dataFactory.getOWLDataPropertyAssertionAxiom(property, individual, value);
        ontoManager.addAxiom(ontology, axiom);
    }

    private void addDataProperty(OWLNamedIndividual individual, String propertyName, float value) {
        OWLDataProperty property = dataFactory.getOWLDataProperty(IRI.create(ontologyIRIStr + propertyName));
        OWLAxiom axiom = dataFactory.getOWLDataPropertyAssertionAxiom(property, individual, value);
        ontoManager.addAxiom(ontology, axiom);
    }


    private void addObjectProperties(OWLNamedIndividual individual, String propertyName, List<String> values) {
        if (values != null) {
            OWLObjectProperty property = dataFactory.getOWLObjectProperty(IRI.create(ontologyIRIStr + propertyName));
            for (String value : values) {
                OWLNamedIndividual objectIndividual = dataFactory.getOWLNamedIndividual(IRI.create(ontologyIRIStr + value));
                OWLAxiom axiom = dataFactory.getOWLObjectPropertyAssertionAxiom(property, individual, objectIndividual);
                ontoManager.addAxiom(ontology, axiom);
            }
        }
    }

    private void removeDataProperties(OWLNamedIndividual individual, String propertyName) {
        OWLDataProperty property = dataFactory.getOWLDataProperty(IRI.create(ontologyIRIStr + propertyName));
        Set<OWLAxiom> axiomsToRemove = ontology.getDataPropertyAssertionAxioms(individual).stream()
                .filter(axiom -> axiom.getProperty().equals(property))
                .collect(Collectors.toSet());
        ontoManager.removeAxioms(ontology, axiomsToRemove);
    }

    private void removeObjectProperties(OWLNamedIndividual individual, String propertyName) {
        OWLObjectProperty property = dataFactory.getOWLObjectProperty(IRI.create(ontologyIRIStr + propertyName));
        Set<OWLAxiom> axiomsToRemove = ontology.getObjectPropertyAssertionAxioms(individual).stream()
                .filter(axiom -> axiom.getProperty().equals(property))
                .collect(Collectors.toSet());
        ontoManager.removeAxioms(ontology, axiomsToRemove);
    }

    private boolean isIndividualOfClass(OWLNamedIndividual individual, OWLClass owlClass) {
        return reasoner.getTypes(individual, false).containsEntity(owlClass);
    }

    private String getDataPropertyValue(OWLNamedIndividual individual, String propertyName) {
        OWLDataProperty property = dataFactory.getOWLDataProperty(IRI.create(ontologyIRIStr + propertyName));
        Set<OWLLiteral> literals = ontology.getDataPropertyAssertionAxioms(individual).stream()
                .filter(axiom -> axiom.getProperty().equals(property))
                .map(OWLDataPropertyAssertionAxiom::getObject)
                .collect(Collectors.toSet());
        if (literals.isEmpty()) {
            LOGGER.warning("No data property value found for " + propertyName + " in individual: " + individual.getIRI().toString());
            return null;
        }
        return literals.iterator().next().getLiteral();
    }

    private float getFloatValue(OWLNamedIndividual individual, String propertyName) {
        String value = getDataPropertyValue(individual, propertyName);
        return value == null ? 0 : Float.parseFloat(value);
    }

    private String getFragment(String iriString) {
        return iriString.contains("#") ? iriString.substring(iriString.indexOf('#') + 1) : iriString;
    }

    private List<String> getObjectPropertyValues(OWLNamedIndividual individual, String propertyName) {
        OWLObjectProperty property = dataFactory.getOWLObjectProperty(IRI.create(ontologyIRIStr + propertyName));
        Set<OWLIndividual> individuals = individual.getObjectPropertyValues(property, ontology);
        return individuals.stream()
                .map(ind -> ind.asOWLNamedIndividual().getIRI().toString())
                .collect(Collectors.toList());
    }

    private List<String> getAllergens(OWLNamedIndividual individual) {
        return getObjectPropertyValues(individual, "hasAllergen").stream()
                .map(this::getFragment)
                .collect(Collectors.toList());
    }


    public void createUser(User user) {
        OWLNamedIndividual userIndividual = dataFactory.getOWLNamedIndividual(IRI.create(ontologyIRIStr + user.getUsername().replace(" ", "_")));
        if (ontology.containsIndividualInSignature(userIndividual.getIRI())) {
            throw new CustomException("User already exists!");
        }

        OWLClass userClass = dataFactory.getOWLClass(IRI.create(ontologyIRIStr + "User"));


        OWLAxiom classAssertion = dataFactory.getOWLClassAssertionAxiom(userClass, userIndividual);
        ontoManager.addAxiom(ontology, classAssertion);

        addDataProperty(userIndividual, "username", user.getUsername());

        // Encrypt the password before saving
        String encodedPassword = passwordEncoder.encode(user.getPassword());
        addDataProperty(userIndividual, "password", encodedPassword);

        addDataProperty(userIndividual, "birthDate", user.getBirthDate());
        addDataProperty(userIndividual, "weightKG", user.getWeightKG());
        addDataProperty(userIndividual, "goalWeight", user.getGoalWeight());
        addDataProperty(userIndividual, "heightCM", user.getHeightCM());

        // Add object property assertions for dietary preferences
        addObjectProperties(userIndividual, "userHasDietaryPreference", user.getDietaryPreferences());

        // Add object property assertions for health conditions
        addObjectProperties(userIndividual, "userHasHealthCondition", user.getHealthConditions());

        saveOntology();
    }

    public User loginUser(String username, String password) {
        OWLNamedIndividual userIndividual = dataFactory.getOWLNamedIndividual(IRI.create(ontologyIRIStr + username));

        String storedPassword = getDataPropertyValue(userIndividual, "password");

        if (storedPassword != null && passwordEncoder.matches(password, storedPassword)) {
            return getUser(userIndividual);
        } else {
            throw new RuntimeException("Invalid username or password");
        }
    }

    private User getUser(OWLNamedIndividual individual) {
        User user = new User();
        user.setId(individual.getIRI().toString());
        user.setUsername(getDataPropertyValue(individual, "username"));
        user.setPassword(getDataPropertyValue(individual, "password")); // This would be encrypted
        user.setBirthDate(getDataPropertyValue(individual, "birthDate"));
        user.setWeightKG(getFloatValue(individual, "weightKG"));
        user.setGoalWeight(getFloatValue(individual, "goalWeight"));
        user.setHeightCM(getFloatValue(individual, "heightCM"));
        user.setDietaryPreferences(getObjectPropertyValues(individual, "userHasDietaryPreference"));
        user.setHealthConditions(getObjectPropertyValues(individual, "userHasHealthCondition"));
        return user;
    }


    public void editUser(User user, String oldPassword) {
        OWLNamedIndividual userIndividual = dataFactory.getOWLNamedIndividual(IRI.create(ontologyIRIStr + user.getUsername()));

        String storedPassword = getDataPropertyValue(userIndividual, "password");

        // Validate the old password
        if (storedPassword == null || !passwordEncoder.matches(oldPassword, storedPassword)) {
            throw new RuntimeException("Invalid old password");
        }

        LOGGER.info(user.getUsername());
        // Remove existing data properties
        removeDataProperties(userIndividual, "weightKG");
        removeDataProperties(userIndividual, "goalWeight");
        removeDataProperties(userIndividual, "heightCM");
        removeDataProperties(userIndividual, "password");


        // Add new data properties
        addDataProperty(userIndividual, "weightKG", user.getWeightKG());
        addDataProperty(userIndividual, "goalWeight", user.getGoalWeight());
        addDataProperty(userIndividual, "heightCM", user.getHeightCM());
        String encodedPassword = passwordEncoder.encode(user.getPassword());
        addDataProperty(userIndividual, "password", encodedPassword);


        // Remove existing object properties
        removeObjectProperties(userIndividual, "userHasDietaryPreference");
        removeObjectProperties(userIndividual, "userHasHealthCondition");

        // Add new object properties
        addObjectProperties(userIndividual, "userHasDietaryPreference", user.getDietaryPreferences());
        addObjectProperties(userIndividual, "userHasHealthCondition", user.getHealthConditions());

        saveOntology();
    }

    public void removeUser(String userId) {
        OWLNamedIndividual userIndividual = dataFactory.getOWLNamedIndividual(IRI.create(ontologyIRIStr + userId));
        OWLEntityRemover remover = new OWLEntityRemover(ontoManager, Collections.singleton(ontology));
        userIndividual.accept(remover);
        ontoManager.applyChanges(remover.getChanges());

        saveOntology();
    }

    private void saveOntology() {
        try {
            ontoManager.saveOntology(ontology);
        } catch (OWLOntologyStorageException e) {
            e.printStackTrace();
        }
    }
}
