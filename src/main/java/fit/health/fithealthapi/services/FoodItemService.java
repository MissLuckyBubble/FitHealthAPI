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
       return ontologyService.getTypes(individual);
    }

    public void createFoodItem(FoodItem foodItem) {
        System.out.println(foodItem.getFoodName());
        OWLNamedIndividual foodIndividual = dataFactory.getOWLNamedIndividual(IRI.create(ontologyIRIStr + foodItem.getFoodName().replace(" ", "_")));
        if (ontology.containsIndividualInSignature(foodIndividual.getIRI())) {
            throw new CustomException("Food item already exists");
        }
        OWLClass foodItemClass = dataFactory.getOWLClass(IRI.create(ontologyIRIStr + "FoodItem"));

        List<OWLOntologyChange> changes = new ArrayList<>();
        OWLAxiom classAssertion = dataFactory.getOWLClassAssertionAxiom(foodItemClass, foodIndividual);
        changes.add(new AddAxiom(ontology, classAssertion));

        changes.add(dataPropertyService.addDataProperty(foodIndividual, "foodName", foodItem.getFoodName()));
        changes.add(dataPropertyService.addDataProperty(foodIndividual, "caloriesPer100gram", foodItem.getCaloriesPer100gram()));
        changes.add(dataPropertyService.addDataProperty(foodIndividual, "fatContent", foodItem.getFatContent()));
        changes.add(dataPropertyService.addDataProperty(foodIndividual, "proteinContent", foodItem.getProteinContent()));
        changes.add(dataPropertyService.addDataProperty(foodIndividual, "sugarContent", foodItem.getSugarContent()));

        // Add object property assertions for allergens with restrictions
        addAllergens(foodItem, foodIndividual, changes);

        // Apply all changes
        ontoManager.applyChanges(changes);

        ontologyService.saveOntology();
    }




    public void editFoodItem(FoodItem foodItem) {
        OWLNamedIndividual foodIndividual = dataFactory.getOWLNamedIndividual(IRI.create(ontologyIRIStr + foodItem.getId()));

        List<OWLOntologyChange> changes = new ArrayList<>();

        // Remove existing data properties
        changes.addAll(dataPropertyService.removeDataProperties(foodIndividual, "caloriesPer100gram"));
        changes.addAll(dataPropertyService.removeDataProperties(foodIndividual, "fatContent"));
        changes.addAll(dataPropertyService.removeDataProperties(foodIndividual, "proteinContent"));
        changes.addAll(dataPropertyService.removeDataProperties(foodIndividual, "sugarContent"));

        // Add new data properties
        changes.add(dataPropertyService.addDataProperty(foodIndividual, "caloriesPer100gram", foodItem.getCaloriesPer100gram()));
        changes.add(dataPropertyService.addDataProperty(foodIndividual, "fatContent", foodItem.getFatContent()));
        changes.add(dataPropertyService.addDataProperty(foodIndividual, "proteinContent", foodItem.getProteinContent()));
        changes.add(dataPropertyService.addDataProperty(foodIndividual, "sugarContent", foodItem.getSugarContent()));

        // Remove existing object properties
        changes.addAll(objectPropertyService.removeObjectProperties(foodIndividual, "hasAllergen"));

        addAllergens(foodItem, foodIndividual, changes);

        // Apply all changes
        ontoManager.applyChanges(changes);

        ontologyService.saveOntology();
    }


    public void removeFoodItem(String foodItemId) {
        OWLNamedIndividual foodIndividual = dataFactory.getOWLNamedIndividual(IRI.create(ontologyIRIStr + foodItemId));
        OWLEntityRemover remover = new OWLEntityRemover(ontoManager, Collections.singleton(ontology));
        foodIndividual.accept(remover);
        ontoManager.applyChanges(remover.getChanges());

        ontologyService.saveOntology();
    }

    private void addAllergens(FoodItem foodItem, OWLNamedIndividual foodIndividual, List<OWLOntologyChange> changes) {
        for (String allergenName : foodItem.getAllergens()) {
            OWLObjectProperty hasAllergenProperty = dataFactory.getOWLObjectProperty(IRI.create(ontologyIRIStr + "hasAllergen"));

            OWLClass allergenClass = dataFactory.getOWLClass(IRI.create(ontologyIRIStr + allergenName.replace(" ", "_")));
            OWLClassExpression allergenRestriction = dataFactory.getOWLObjectSomeValuesFrom(hasAllergenProperty, allergenClass);

            OWLAxiom allergenAxiom = dataFactory.getOWLClassAssertionAxiom(allergenRestriction, foodIndividual);
            changes.add(new AddAxiom(ontology, allergenAxiom));
        }
    }

    private List<String> getAllergens(OWLNamedIndividual individual) {
        OWLObjectProperty hasAllergenProperty = dataFactory.getOWLObjectProperty(IRI.create(ontologyIRIStr + "hasAllergen"));

        return ontology.getClassAssertionAxioms(individual).parallelStream()
                .map(OWLClassAssertionAxiom::getClassExpression)
                .filter(ce -> ce instanceof OWLObjectSomeValuesFrom)
                .map(ce -> (OWLObjectSomeValuesFrom) ce)
                .filter(sv -> sv.getProperty().equals(hasAllergenProperty))
                .map(sv -> sv.getFiller())
                .filter(filler -> filler instanceof OWLClass)
                .map(filler -> (OWLClass) filler)
                .map(allergenClass -> ontologyService.getFragment(allergenClass.getIRI().toString()))
                .collect(Collectors.toList());
    }

}
