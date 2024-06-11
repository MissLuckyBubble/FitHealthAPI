package fit.health.fithealthapi.services;

import fit.health.fithealthapi.exceptions.CustomException;
import fit.health.fithealthapi.model.User;
import lombok.Getter;
import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.util.OWLEntityRemover;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Collections;

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

        // Encrypt the password before saving
        String encodedPassword = passwordEncoder.encode(user.getPassword());
        dataPropertyService.addDataProperty(userIndividual, "password", encodedPassword);

        dataPropertyService.addDataProperty(userIndividual, "birthDate", user.getBirthDate());
        dataPropertyService.addDataProperty(userIndividual, "weightKG", user.getWeightKG());
        dataPropertyService.addDataProperty(userIndividual, "goalWeight", user.getGoalWeight());
        dataPropertyService.addDataProperty(userIndividual, "heightCM", user.getHeightCM());

        // Add object property assertions for dietary preferences
        objectPropertyService.addObjectProperties(userIndividual, "userHasDietaryPreference", user.getDietaryPreferences());

        // Add object property assertions for health conditions
        objectPropertyService.addObjectProperties(userIndividual, "userHasHealthCondition", user.getHealthConditions());

        ontologyService.saveOntology();
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
        user.setPassword(dataPropertyService.getDataPropertyValue(individual, "password")); // This would be encrypted
        user.setBirthDate(dataPropertyService.getDataPropertyValue(individual, "birthDate"));
        user.setWeightKG(dataPropertyService.getFloatValue(individual, "weightKG"));
        user.setGoalWeight(dataPropertyService.getFloatValue(individual, "goalWeight"));
        user.setHeightCM(dataPropertyService.getFloatValue(individual, "heightCM"));
        user.setDietaryPreferences(objectPropertyService.getObjectPropertyValues(individual, "userHasDietaryPreference"));
        user.setHealthConditions(objectPropertyService.getObjectPropertyValues(individual, "userHasHealthCondition"));
        return user;
    }


    public void editUser(User user, String oldPassword) {
        OWLNamedIndividual userIndividual = dataFactory.getOWLNamedIndividual(IRI.create(ontologyIRIStr + user.getUsername()));

        String storedPassword = dataPropertyService.getDataPropertyValue(userIndividual, "password");

        // Validate the old password
        if (storedPassword == null || !passwordEncoder.matches(oldPassword, storedPassword)) {
            throw new CustomException("Invalid old password");
        }

        // Remove existing data properties
        dataPropertyService.removeDataProperties(userIndividual, "weightKG");
        dataPropertyService.removeDataProperties(userIndividual, "goalWeight");
        dataPropertyService.removeDataProperties(userIndividual, "heightCM");
        dataPropertyService.removeDataProperties(userIndividual, "password");


        // Add new data properties
        dataPropertyService.addDataProperty(userIndividual, "weightKG", user.getWeightKG());
        dataPropertyService.addDataProperty(userIndividual, "goalWeight", user.getGoalWeight());
        dataPropertyService.addDataProperty(userIndividual, "heightCM", user.getHeightCM());
        String encodedPassword = passwordEncoder.encode(user.getPassword());
        dataPropertyService.addDataProperty(userIndividual, "password", encodedPassword);


        // Remove existing object properties
        objectPropertyService.removeObjectProperties(userIndividual, "userHasDietaryPreference");
        objectPropertyService.removeObjectProperties(userIndividual, "userHasHealthCondition");

        // Add new object properties
        objectPropertyService.addObjectProperties(userIndividual, "userHasDietaryPreference", user.getDietaryPreferences());
        objectPropertyService.addObjectProperties(userIndividual, "userHasHealthCondition", user.getHealthConditions());

        ontologyService.saveOntology();
    }

    public void removeUser(String userId) {
        OWLNamedIndividual userIndividual = dataFactory.getOWLNamedIndividual(IRI.create(ontologyIRIStr + userId));
        OWLEntityRemover remover = new OWLEntityRemover(ontoManager, Collections.singleton(ontology));
        userIndividual.accept(remover);
        ontoManager.applyChanges(remover.getChanges());

        ontologyService.saveOntology();
    }

}
