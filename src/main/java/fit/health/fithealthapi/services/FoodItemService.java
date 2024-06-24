package fit.health.fithealthapi.services;

import fit.health.fithealthapi.exceptions.CustomException;
import fit.health.fithealthapi.model.FoodItem;
import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.util.OWLEntityRemover;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.stream.Collectors;

@Service
public class FoodItemService {
    @Autowired
    DataPropertyService dataPropertyService;
    @Autowired
    ObjectPropertyService objectPropertyService;
    @Autowired
    OntologyService ontologyService;


    private OWLOntologyManager ontoManager;
    private OWLDataFactory dataFactory;
    private OWLOntology ontology;
    private String ontologyIRIStr;

    public FoodItemService(DataPropertyService dataPropertyService, ObjectPropertyService objectPropertyService, OntologyService ontologyService) {
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

    public List<FoodItem> getFoodItems() {
        ConcurrentLinkedQueue<FoodItem> foodItems = new ConcurrentLinkedQueue<>();
        OWLClass foodItemClass = dataFactory.getOWLClass(IRI.create(ontologyIRIStr + "FoodItem"));
        Set<OWLNamedIndividual> individuals = ontology.getIndividualsInSignature();

        individuals.parallelStream().forEach(individual -> {
            if (ontology.getClassAssertionAxioms(individual).stream()
                    .anyMatch(axiom -> axiom.getClassesInSignature().contains(foodItemClass))) {
                getFoodItem(foodItems, individual);
            }
        });

        return new ArrayList<>(foodItems);
    }

    public List<FoodItem> getFoodItemsByPreference(String preference) {
        ConcurrentLinkedQueue<FoodItem> foodItems = new ConcurrentLinkedQueue<>();
        OWLClass foodItemClass = dataFactory.getOWLClass(IRI.create(ontologyIRIStr + "FoodItem"));
        OWLClass preferenceClass = dataFactory.getOWLClass(IRI.create(ontologyIRIStr + preference));
        Set<OWLNamedIndividual> individuals = ontology.getIndividualsInSignature();

        individuals.parallelStream().forEach(individual -> {
            if (ontology.getClassAssertionAxioms(individual).stream()
                    .anyMatch(axiom -> axiom.getClassesInSignature().contains(foodItemClass))) {
                if (ontologyService.isIndividualOfClass(individual, preferenceClass)) {
                    getFoodItem(foodItems, individual);
                }
            }
        });

        return new ArrayList<>(foodItems);
    }

    public List<FoodItem> getFoodItemsByPreferences(List<String> preferences) {
        List<FoodItem> allFoodItems = getFoodItems();

        return allFoodItems.stream()
                .filter(foodItem -> {
                    List<String> dietaryPreferences = foodItem.getDietaryPreferences();
                    return preferences.stream().allMatch(dietaryPreferences::contains);
                })
                .collect(Collectors.toList());
    }




    private synchronized void getFoodItem(ConcurrentLinkedQueue<FoodItem> foodItems, OWLNamedIndividual individual) {
        FoodItem foodItem = new FoodItem();
        foodItem.setId(individual.getIRI().toString());
        String foodName = dataPropertyService.getDataPropertyValue(individual, "foodName"); // Retrieve foodName from data property
        foodItem.setFoodName(foodName);
        foodItem.setCaloriesPer100gram(dataPropertyService.getFloatValue(individual, "caloriesPer100gram"));
        foodItem.setFatContent(dataPropertyService.getFloatValue(individual, "fatContent"));
        foodItem.setProteinContent(dataPropertyService.getFloatValue(individual, "proteinContent"));
        foodItem.setSugarContent(dataPropertyService.getFloatValue(individual, "sugarContent"));
        foodItem.setAllergens(getAllergens(individual));
        foodItem.setDietaryPreferences(getDietaryPreferences(individual));
        foodItems.add(foodItem);
    }

    private synchronized List<String> getDietaryPreferences(OWLNamedIndividual individual) {
        List<String> preferences = new ArrayList<>();
        for (Map.Entry<OWLClass, String> entry : ontologyService.getDietaryPreferencesMap().entrySet()) {
            if (ontologyService.isIndividualOfClass(individual, entry.getKey())) {
                preferences.add(ontologyService.getFragment(entry.getValue()));
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

        dataPropertyService.addDataProperty(foodIndividual, "foodName", foodItem.getFoodName());
        dataPropertyService.addDataProperty(foodIndividual, "caloriesPer100gram", foodItem.getCaloriesPer100gram());
        dataPropertyService.addDataProperty(foodIndividual, "fatContent", foodItem.getFatContent());
        dataPropertyService.addDataProperty(foodIndividual, "proteinContent", foodItem.getProteinContent());
        dataPropertyService.addDataProperty(foodIndividual, "sugarContent", foodItem.getSugarContent());

        // Add object property assertions for allergens
        objectPropertyService.addObjectProperties(foodIndividual, "hasAllergen", foodItem.getAllergens());

        ontologyService.saveOntology();
    }

    public void editFoodItem(FoodItem foodItem) {
        OWLNamedIndividual foodIndividual = dataFactory.getOWLNamedIndividual(IRI.create(ontologyIRIStr + foodItem.getId()));

        // Remove existing data properties
        dataPropertyService.removeDataProperties(foodIndividual, "caloriesPer100gram");
        dataPropertyService.removeDataProperties(foodIndividual, "fatContent");
        dataPropertyService.removeDataProperties(foodIndividual, "proteinContent");
        dataPropertyService.removeDataProperties(foodIndividual, "sugarContent");

        // Add new data properties
        dataPropertyService.addDataProperty(foodIndividual, "caloriesPer100gram", foodItem.getCaloriesPer100gram());
        dataPropertyService.addDataProperty(foodIndividual, "fatContent", foodItem.getFatContent());
        dataPropertyService.addDataProperty(foodIndividual, "proteinContent", foodItem.getProteinContent());
        dataPropertyService.addDataProperty(foodIndividual, "sugarContent", foodItem.getSugarContent());

        // Remove existing object properties
        objectPropertyService.removeObjectProperties(foodIndividual, "hasAllergen");

        // Add new object properties
        objectPropertyService.addObjectProperties(foodIndividual, "hasAllergen", foodItem.getAllergens());

        ontologyService.saveOntology();
    }

    public void removeFoodItem(String foodItemId) {
        OWLNamedIndividual foodIndividual = dataFactory.getOWLNamedIndividual(IRI.create(ontologyIRIStr + foodItemId));
        OWLEntityRemover remover = new OWLEntityRemover(ontoManager, Collections.singleton(ontology));
        foodIndividual.accept(remover);
        ontoManager.applyChanges(remover.getChanges());

        ontologyService.saveOntology();
    }

    private List<String> getAllergens(OWLNamedIndividual individual) {
        return objectPropertyService.getObjectPropertyValues(individual, "hasAllergen").stream()
                .map(o -> ontologyService.getFragment(o))
                .collect(Collectors.toList());
    }
}
