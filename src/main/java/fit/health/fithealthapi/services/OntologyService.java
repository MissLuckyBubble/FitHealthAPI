package fit.health.fithealthapi.services;

import fit.health.fithealthapi.model.FoodItem;
import fit.health.fithealthapi.model.User;
import lombok.Getter;
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

    @Getter
    private final OWLOntologyManager ontoManager;
    @Getter
    private final OWLDataFactory dataFactory;
    @Getter
    private OWLOntology ontology;
    @Getter
    private final String ontologyIRIStr;
    @Getter
    private OWLReasoner reasoner;
    @Getter
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
        return dietaryPreferencesMap.values().stream()
                .map(this::getFragment)
                .collect(Collectors.toList());
    }

     boolean isIndividualOfClass(OWLNamedIndividual individual, OWLClass owlClass) {
        return reasoner.getTypes(individual, false).containsEntity(owlClass);
    }
    public String getFragment(String iriString) {
        return iriString.contains("#") ? iriString.substring(iriString.indexOf('#') + 1) : iriString;
    }

    public void updateReasoner() {
        initializeReasoner();
        loadDietaryPreferences();
    }


    void saveOntology() {
        try {
            ontoManager.saveOntology(ontology);
            updateReasoner();
        } catch (OWLOntologyStorageException e) {
            e.printStackTrace();
        }
    }
}
