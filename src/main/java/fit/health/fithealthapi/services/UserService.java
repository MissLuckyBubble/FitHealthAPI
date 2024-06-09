package fit.health.fithealthapi.services;

import fit.health.fithealthapi.model.User;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLNamedIndividual;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.logging.Logger;

@Service
public class UserService {
/*
    private static final Logger LOGGER = Logger.getLogger(UserService.class.getName());
    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    private final OntologyService ontologyService;

    public UserService(OntologyService ontologyService) {
        this.ontologyService = ontologyService;
    }

    public void createUser(User user) {
        OWLNamedIndividual userIndividual = ontologyService.getDataFactory().getOWLNamedIndividual(IRI.create(ontologyService.getOntologyIRIStr() + user.getUsername().replace(" ", "_")));
        OWLClass userClass = ontologyService.getDataFactory().getOWLClass(IRI.create(ontologyService.getOntologyIRIStr() + "User"));

        OWLAxiom classAssertion = ontologyService.getDataFactory().getOWLClassAssertionAxiom(userClass, userIndividual);
        ontologyService.getOntoManager().addAxiom(ontologyService.getOntology(), classAssertion);

        ontologyService.addDataProperty(userIndividual, "username", user.getUsername());

        // Encrypt the password before saving
        String encodedPassword = passwordEncoder.encode(user.getPassword());
        ontologyService.addDataProperty(userIndividual, "password", encodedPassword);

        ontologyService.addDataProperty(userIndividual, "birthDate", user.getBirthDate());
        ontologyService.addDataProperty(userIndividual, "weightKG", user.getWeightKG());
        ontologyService.addDataProperty(userIndividual, "goalWeight", user.getGoalWeight());
        ontologyService.addDataProperty(userIndividual, "heightCM", user.getHeightCM());

        // Add object property assertions for dietary preferences
        ontologyService.addObjectProperties(userIndividual, "userHasDietaryPreference", user.getDietaryPreferences());

        // Add object property assertions for health conditions
        ontologyService.addObjectProperties(userIndividual, "userHasHealthCondition", user.getHealthConditions());

        ontologyService.saveOntology();
    }

    public User loginUser(String username, String password) {
        OWLNamedIndividual userIndividual = ontologyService.getDataFactory().getOWLNamedIndividual(IRI.create(ontologyService.getOntologyIRIStr() + username));

        String storedPassword = ontologyService.getDataPropertyValue(userIndividual, "password");

        if (storedPassword != null && passwordEncoder.matches(password, storedPassword)) {
            return getUser(userIndividual);
        } else {
            throw new RuntimeException("Invalid username or password");
        }
    }

    private User getUser(OWLNamedIndividual individual) {
        User user = new User();
        user.setId(individual.getIRI().toString());
        user.setUsername(ontologyService.getDataPropertyValue(individual, "username"));
        user.setPassword(ontologyService.getDataPropertyValue(individual, "password")); // This would be encrypted
        user.setBirthDate(ontologyService.getDataPropertyValue(individual, "birthDate"));
        user.setWeightKG(ontologyService.getFloatValue(individual, "weightKG"));
        user.setGoalWeight(ontologyService.getFloatValue(individual, "goalWeight"));
        user.setHeightCM(ontologyService.getFloatValue(individual, "heightCM"));
        user.setDietaryPreferences(ontologyService.getObjectPropertyValues(individual, "userHasDietaryPreference"));
        user.setHealthConditions(ontologyService.getObjectPropertyValues(individual, "userHasHealthCondition"));
        return user;
    }

    public void editUser(User user) {
        OWLNamedIndividual userIndividual = ontologyService.getDataFactory().getOWLNamedIndividual(IRI.create(ontologyService.getOntologyIRIStr() + user.getUsername()));

        LOGGER.info(user.getUsername());
        // Remove existing data properties
        ontologyService.removeDataProperties(userIndividual, "weightKG");
        ontologyService.removeDataProperties(userIndividual, "goalWeight");
        ontologyService.removeDataProperties(userIndividual, "heightCM");
        ontologyService.removeDataProperties(userIndividual, "password");

        // Add new data properties
        ontologyService.addDataProperty(userIndividual, "weightKG", user.getWeightKG());
        ontologyService.addDataProperty(userIndividual, "goalWeight", user.getGoalWeight());
        ontologyService.addDataProperty(userIndividual, "heightCM", user.getHeightCM());
        String encodedPassword = passwordEncoder.encode(user.getPassword());
        ontologyService.addDataProperty(userIndividual, "password", encodedPassword);

        // Remove existing object properties
        ontologyService.removeObjectProperties(userIndividual, "userHasDietaryPreference");
        ontologyService.removeObjectProperties(userIndividual, "userHasHealthCondition");

        // Add new object properties
        ontologyService.addObjectProperties(userIndividual, "userHasDietaryPreference", user.getDietaryPreferences());
        ontologyService.addObjectProperties(userIndividual, "userHasHealthCondition", user.getHealthConditions());

        ontologyService.saveOntology();
    }

    public void removeUser(String userId) {
        OWLNamedIndividual userIndividual = ontologyService.getDataFactory().getOWLNamedIndividual(IRI.create(ontologyService.getOntologyIRIStr() + userId));
        OWLEntityRemover remover = new OWLEntityRemover(ontologyService.getOntoManager(), Collections.singleton(ontologyService.getOntology()));
        userIndividual.accept(remover);
        ontologyService.getOntoManager().applyChanges(remover.getChanges());

        ontologyService.saveOntology();
    }*/
}
