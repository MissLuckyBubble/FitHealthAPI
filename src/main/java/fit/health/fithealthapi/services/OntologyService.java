package fit.health.fithealthapi.services;

import lombok.Getter;
import org.semanticweb.HermiT.Reasoner;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.reasoner.OWLReasoner;
import org.semanticweb.owlapi.reasoner.OWLReasonerFactory;
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
    private final OWLOntologyManager ontoManager;
    @Getter
    private final OWLDataFactory dataFactory;
    @Getter
    private OWLOntology ontology;
    @Getter
    private String ontologyIRIStr;
    @Getter
    private OWLReasoner reasoner;
    @Getter
    private Map<OWLClass, String> dietaryPreferencesMap = new HashMap<>();

    public OntologyService() {
        ontoManager = OWLManager.createOWLOntologyManager();
        dataFactory = ontoManager.getOWLDataFactory();
        try {
            loadOntologyFromFile();
            ontologyIRIStr = ontology.getOntologyID().getOntologyIRI().toString() + "#";
            initializeReasoner();
            loadDietaryPreferences();
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
            ontology = ontoManager.loadOntologyFromOntologyDocument(ontoFile);
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

    private void loadDietaryPreferences() {
        LOGGER.info("IN DIETARY");
        try {
            OWLClass dietaryPreferenceClass = dataFactory.getOWLClass(IRI.create(ontologyIRIStr + "DietaryPreference"));

            // Iterate over all classes in the ontology and check if they are subclasses of DietaryPreference
            for (OWLClass owlClass : ontology.getClassesInSignature()) {
                for (OWLSubClassOfAxiom axiom : ontology.getSubClassAxiomsForSubClass(owlClass)) {
                    if (axiom.getSuperClass().equals(dietaryPreferenceClass)) {
                        String shortForm = owlClass.getIRI().toString();
                        dietaryPreferencesMap.put(owlClass, shortForm);
                        LOGGER.info("Loaded dietary preference: " + shortForm);
                    }
                }
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to load dietary preferences", e);
        }
    }




    public List<String> getDietaryPreferences() {
        return dietaryPreferencesMap.values().stream()
                .map(this::getFragment)
                .collect(Collectors.toList());
    }

    public boolean isIndividualOfClass(OWLNamedIndividual individual, OWLClass owlClass) {
        if (reasoner == null || !reasoner.isConsistent()) {
            LOGGER.log(Level.SEVERE, "Reasoner is not initialized or inconsistent.");
            return false;
        }
        try {
            boolean result = reasoner.getTypes(individual, false).containsEntity(owlClass);
            LOGGER.info("Individual " + individual + " is of class " + owlClass + ": " + result);
            return result;
        } catch (Exception e) {
            return false;
        }
    }

    public String getFragment(String iriString) {
        return iriString.contains("#") ? iriString.substring(iriString.indexOf('#') + 1) : iriString;
    }

    public void updateReasoner() {
        try {
            initializeReasoner();
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to update reasoner", e);
        }
    }

    public List<String> getTypes(OWLNamedIndividual individual) {
        System.out.println("Getting types: " + individual.getIRI().getFragment());
        return reasoner.getTypes(individual, false).getFlattened().stream()
                .map(owlClass -> getFragment(owlClass.getIRI().toString()))
                .collect(Collectors.toList());
    }

    public void saveOntology() {
        try {
            ontoManager.saveOntology(ontology);
            updateReasoner();
        } catch (OWLOntologyStorageException e) {
            LOGGER.log(Level.SEVERE, "Failed to save ontology", e);
        }
    }
}
