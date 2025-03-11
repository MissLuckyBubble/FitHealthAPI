
package fit.health.fithealthapi.services;

import fit.health.fithealthapi.model.dto.InferredPreferences;
import fit.health.fithealthapi.model.enums.Allergen;
import fit.health.fithealthapi.model.enums.DietaryPreference;
import fit.health.fithealthapi.model.enums.HealthCondition;
import fit.health.fithealthapi.model.enums.HealthConditionSuitability;
import lombok.Getter;
import org.semanticweb.HermiT.Reasoner;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.reasoner.*;
import org.semanticweb.owlapi.util.OWLEntityRemover;
import org.semanticweb.owlapi.util.OWLEntityRenamer;
import org.springframework.stereotype.Service;

import java.io.File;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

@Service
public class OntologyService {

    @Getter
    private static final Logger LOGGER = Logger.getLogger(OntologyService.class.getName());

    @Getter
    private final OWLOntologyManager ontologyManager;
    @Getter
    private final OWLDataFactory dataFactory;
    @Getter
    private OWLOntology ontology;
    @Getter
    private String ontologyIRI;
    @Getter
    private OWLReasoner reasoner;

    public OntologyService() {
        ontologyManager = OWLManager.createOWLOntologyManager();
        dataFactory = ontologyManager.getOWLDataFactory();
        try {
            loadOntologyFromFile();
            ontologyIRI = ontology.getOntologyID().getOntologyIRI().toString() + "#";
            initializeReasoner();
        } catch (OWLOntologyCreationException e) {
            LOGGER.log(Level.SEVERE, "Ontology creation failed", e);
            throw new RuntimeException("Ontology creation failed", e);
        }
    }

    private void loadOntologyFromFile() throws OWLOntologyCreationException {
        File ontoFile = new File("src/main/java/fit/health/fithealthapi/ontology/health.owl");
        if (!ontoFile.exists()) {
            throw new OWLOntologyCreationException("Ontology file not found: " + ontoFile.getAbsolutePath());
        }
        try {
            ontology = ontologyManager.loadOntologyFromOntologyDocument(ontoFile);
            LOGGER.info("Ontology loaded successfully from " + ontoFile.getAbsolutePath());
        } catch (OWLOntologyCreationException e) {
            LOGGER.log(Level.SEVERE, "Failed to load ontology", e);
            throw e;
        }
    }

    private void initializeReasoner() {
        try {
            OWLReasonerFactory reasonerFactory = new Reasoner.ReasonerFactory();
            LOGGER.info("Initializing reasoner...");
            reasoner = reasonerFactory.createReasoner(ontology);
            reasoner.precomputeInferences();
            LOGGER.info("Reasoner initialized successfully.");
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to initialize reasoner", e);
            throw new RuntimeException("Failed to initialize reasoner", e);
        }
    }


    public String getFragment(String iriString) {
        return iriString.contains("#") ? iriString.substring(iriString.indexOf('#') + 1) : iriString;
    }

    public void saveOntology() {
        try {
            ontologyManager.saveOntology(ontology);
        } catch (OWLOntologyStorageException e) {
            throw new RuntimeException("Failed to save ontology: " + e.getMessage(), e);
        }
    }

    public OWLClass getOWLClass(String className) {
        return dataFactory.getOWLClass(IRI.create(ontologyIRI + className));
    }
    /**
     * General method to create a new subclass and link it to a parent class in the ontology.
     *
     * @param itemName  The name of the new subclass to create.
     * @param parentClassName The name of the parent class to link the new subclass under.
     */
    public void createItemType(String itemName, String parentClassName) {
        OWLClass newItemClass = getOWLClass(itemName);
        OWLClass parentClass = getOWLClass(parentClassName);
        OWLSubClassOfAxiom axiom = dataFactory.getOWLSubClassOfAxiom(newItemClass, parentClass);

        ontologyManager.applyChange(new AddAxiom(ontology, axiom));
        saveOntology();

        // Flush reasoner instead of full precompute
        reasoner.flush();
    }

    /**
     * Converts a class to a defined class (EquivalentClass) using its current subclass restrictions.
     *
     * @param className The name of the ontology class to redefine.
     */
    public void convertToDefinedClass(String className) {
        OWLClass owlClass = dataFactory.getOWLClass(IRI.create(ontologyIRI + className));

        Set<OWLClassExpression> restrictions = ontology.getAxioms(owlClass).stream()
                .filter(axiom -> axiom.isOfType(AxiomType.SUBCLASS_OF))
                .map(axiom -> ((OWLSubClassOfAxiom) axiom).getSuperClass())
                .collect(Collectors.toSet());

        if (restrictions.isEmpty()) {
            LOGGER.warning("No subclass restrictions found for " + className);
            return;
        }

        OWLEquivalentClassesAxiom equivalentClassesAxiom = dataFactory.getOWLEquivalentClassesAxiom(
                owlClass, dataFactory.getOWLObjectIntersectionOf(restrictions));
        ontologyManager.addAxiom(ontology, equivalentClassesAxiom);

        saveOntology();
        reasoner.flush();
    }
    /**
     * Remove a defined class to a class (EquivalentClass)
     *
     * @param className The name of the ontology class to redefine.
     */
    public void removeDefinedClass(String className) {
        OWLClass owlClass = getOWLClass(className);

        Set<OWLEquivalentClassesAxiom> axiomsToRemove = ontology.getEquivalentClassesAxioms(owlClass);
        for (OWLEquivalentClassesAxiom axiom : axiomsToRemove) {
            ontologyManager.applyChange(new RemoveAxiom(ontology, axiom));
        }

        saveOntology();
        reasoner.flush();
    }



    /**
     * Renames an ontology item (class, property, or individual) by updating its IRI.
     *
     * @param oldName The current name of the item.
     * @param newName The new name for the item.
     *
     * Example: renameItem("Broccoli", "NewBroccoli");
     */
    public void renameItem(String oldName, String newName) {
        OWLEntityRenamer renamer = new OWLEntityRenamer(ontologyManager, ontology.getImportsClosure());
        IRI oldIRI = IRI.create(ontologyIRI + oldName);
        IRI newIRI = IRI.create(ontologyIRI + newName);

        // Apply renaming changes
        ontologyManager.applyChanges(renamer.changeIRI(oldIRI, newIRI));
        saveOntology();
        reasoner.flush();
    }

    /**
     * Add a data property restriction to a class.
     */
    public void addDataPropertyRestriction(String className, String propertyName, Object value) {
        if (value == null) {
            LOGGER.warning("Value for property " + propertyName + " is null. Skipping data property restriction.");
            return;
        }

        OWLClass targetClass = getOWLClass(className);
        OWLDataProperty dataProperty = dataFactory.getOWLDataProperty(IRI.create(ontologyIRI + propertyName));
        OWLLiteral literal;

        if (value instanceof Integer) {
            literal = dataFactory.getOWLLiteral((Integer) value);
        } else if (value instanceof Float) {
            literal = dataFactory.getOWLLiteral((Float) value);
        } else if (value instanceof String) {
            literal = dataFactory.getOWLLiteral((String) value);
        } else {
            throw new IllegalArgumentException("Unsupported data type for property value: " + value.getClass());
        }

        OWLClassExpression restriction = dataFactory.getOWLDataHasValue(dataProperty, literal);

        OWLAxiom axiom = dataFactory.getOWLSubClassOfAxiom(targetClass, restriction);
        ontologyManager.applyChange(new AddAxiom(ontology, axiom));
    }

    /**
     * Add an object property restriction to a class.
     */
    public void addObjectPropertyRestriction(String className, String propertyName, String relatedClassName) {
        OWLClass targetClass = getOWLClass(className);
        OWLObjectProperty objectProperty = dataFactory.getOWLObjectProperty(IRI.create(ontologyIRI + propertyName));
        OWLClass relatedClass = getOWLClass(relatedClassName);

        OWLClassExpression restriction = dataFactory.getOWLObjectSomeValuesFrom(objectProperty, relatedClass);
        OWLAxiom axiom = dataFactory.getOWLSubClassOfAxiom(targetClass, restriction);
        ontologyManager.applyChange(new AddAxiom(ontology, axiom));
    }

    public void addIngredientsAsOnlyUnionRestriction(
            String recipeClassName,
            List<String> ingredientClassNames
    ) {
        // 1) Get the recipe class (e.g., #CarrotPotatoSoup)
        OWLClass recipeClass = dataFactory.getOWLClass(
                IRI.create(ontologyIRI + recipeClassName)
        );

        // 2) Hardcoded object property (hasIngredient)
        OWLObjectProperty hasIngredient = dataFactory.getOWLObjectProperty(
                IRI.create(ontologyIRI + "hasIngredient")
        );

        // 3) Build the union of all ingredient classes:
        //    (IngredientClass1 OR IngredientClass2 OR ...)
        Set<OWLClassExpression> ingredientClassExprs = new HashSet<>();
        for (String ing : ingredientClassNames) {
            OWLClass ingClass = dataFactory.getOWLClass(
                    IRI.create(ontologyIRI + ing)
            );
            ingredientClassExprs.add(ingClass);
        }

        // Handle edge cases: empty or single ingredient list
        OWLClassExpression unionOfIngredients;
        if (ingredientClassExprs.isEmpty()) {
            // No ingredients → owl:Nothing (meaning: "hasIngredient only Nothing" → no ingredients allowed)
            unionOfIngredients = dataFactory.getOWLNothing();
        } else if (ingredientClassExprs.size() == 1) {
            // Only one ingredient → no need for a union
            unionOfIngredients = ingredientClassExprs.iterator().next();
        } else {
            // Multiple ingredients → create a union
            unionOfIngredients = dataFactory.getOWLObjectUnionOf(ingredientClassExprs);
        }

        // 4) Build "hasIngredient only (unionOfIngredients)"
        OWLClassExpression onlyRestriction = dataFactory.getOWLObjectAllValuesFrom(
                hasIngredient,
                unionOfIngredients
        );

        // 5) SubClassOf(recipeClass, that 'only' restriction)
        OWLAxiom onlyAxiom = dataFactory.getOWLSubClassOfAxiom(recipeClass, onlyRestriction);

        // 6) Add axiom to the ontology
        ontologyManager.addAxiom(ontology, onlyAxiom);
    }

    /**
     * Check if a class name is a valid DietaryPreference.
     */
    public boolean isDietaryPreference(String className) {
        try {
            DietaryPreference.valueOf(className.toUpperCase());
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
    public boolean isAllergen(String className) {
        try {
            Allergen.valueOf(className.toUpperCase());
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    public boolean isHealthConditionSuitability(String className){
        try {
            HealthConditionSuitability.valueOf(className.toUpperCase());
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    public boolean isHealthCondition(String className){
        try {
            HealthCondition.valueOf(className.toUpperCase());
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    public void removeDataPropertyRestrictions(String className){
        OWLClass owlClass = getOWLClass(className);
        Set<OWLSubClassOfAxiom> axiomsToRemove = ontology.getAxioms(owlClass).stream()
                .filter(axiom -> axiom.isOfType(AxiomType.SUBCLASS_OF))
                .map(axiom -> (OWLSubClassOfAxiom) axiom)
                .filter(axiom -> axiom.getSuperClass() instanceof OWLDataHasValue)
                .collect(Collectors.toSet());

        for (OWLSubClassOfAxiom axiom : axiomsToRemove) {
            ontologyManager.applyChange(new RemoveAxiom(ontology, axiom));
        }
        saveOntology();
        reasoner.flush();
    }
    public void removeObjectPropertyRestrictions(String className){
        OWLClass owlClass = getOWLClass(className);
        Set<OWLSubClassOfAxiom> axiomsToRemove = ontology.getAxioms(owlClass).stream()
                .filter(axiom -> axiom.isOfType(AxiomType.SUBCLASS_OF))
                .map(axiom -> (OWLSubClassOfAxiom) axiom)
                .collect(Collectors.toSet());

        for (OWLSubClassOfAxiom axiom : axiomsToRemove) {
            ontologyManager.applyChange(new RemoveAxiom(ontology, axiom));
        }
        saveOntology();
        reasoner.flush();
    }


    /**
     * Updates the parent class of a given item in the ontology.
     *
     * @param itemName      The name of the item to update.
     * @param newParentName The name of the new parent class.
     */
    public void updateParentClass(String itemName, String newParentName) {
        OWLClass itemClass = dataFactory.getOWLClass(IRI.create(ontologyIRI + itemName));
        OWLClass newParentClass = dataFactory.getOWLClass(IRI.create(ontologyIRI + newParentName));

        ontology.getAxioms(itemClass).stream()
                .filter(axiom -> axiom.isOfType(AxiomType.SUBCLASS_OF))
                .forEach(axiom -> ontologyManager.applyChange(new RemoveAxiom(ontology, axiom)));

        // Add the new subclass axiom
        OWLSubClassOfAxiom newAxiom = dataFactory.getOWLSubClassOfAxiom(itemClass, newParentClass);
        ontologyManager.applyChange(new AddAxiom(ontology, newAxiom));
        saveOntology();
    }

    /**
     * Deletes a class from the ontology.
     *
     * @param className The name of the class to delete.
     */
    public void deleteItem(String className) {
        OWLClass classToRemove = dataFactory.getOWLClass(IRI.create(ontologyIRI + className));
        OWLEntityRemover remover = new OWLEntityRemover(ontologyManager, ontology.getImportsClosure());

        classToRemove.accept(remover);
        ontologyManager.applyChanges(remover.getChanges());
        saveOntology();
    }

    /**
     * Get all SuperClasses for a given Class using reasoner
     *
     * @param className The name of the ontology class to inspect (e.g., "Broccoli").
     * @return A list of inferred dietary preferences.
     */
    public Set<String> getSuperClasses(String className) {
        try {
            OWLClass targetClass = getOWLClass(className);

            long startTime = System.nanoTime();

            // Fetch direct superclasses
            NodeSet<OWLClass> superClasses = reasoner.getSuperClasses(targetClass, false);

            // End the timer
            long endTime = System.nanoTime();
            long durationMs = (endTime - startTime) / 1_000_000; // Convert to milliseconds
            LOGGER.info("Time taken to fetch superclasses for " + className + ": " + durationMs + " ms");


            return superClasses.getFlattened().stream()
                    .map(cls -> getFragment(cls.getIRI().toString()))
                    .collect(Collectors.toSet());

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error fetching dietary preferences", e);
            return Collections.emptySet();
        }
    }

    /**
     * Infers dietary preferences and health condition suitabilities for a given class name.
     * @param className The name of the class in the ontology.
     * @return An InferredPreferences object containing the inferred preferences.
     */
    public InferredPreferences inferPreferences(String className) {
        Set<String> superClasses = this.getSuperClasses(className);

        Set<DietaryPreference> dietaryPreferences = superClasses.stream()
                .filter(this::isDietaryPreference)
                .map(String::toUpperCase)
                .map(DietaryPreference::valueOf)
                .collect(Collectors.toSet());

        Set<HealthConditionSuitability> healthConditionSuitabilities = superClasses.stream()
                .filter(this::isHealthConditionSuitability)
                .map(String::toUpperCase)
                .map(HealthConditionSuitability::valueOf)
                .collect(Collectors.toSet());

        return new InferredPreferences(dietaryPreferences, healthConditionSuitabilities);
    }

}
