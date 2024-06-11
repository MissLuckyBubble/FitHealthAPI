package fit.health.fithealthapi.services;

import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.util.OWLEntityRemover;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class ObjectPropertyService {


    @Autowired
    OntologyService ontologyService;


    public ObjectPropertyService(OntologyService ontologyService){
        this.ontologyService = ontologyService;
    }

    public List<String> getObjectPropertyValues(OWLNamedIndividual individual, String propertyName) {
        OWLObjectProperty property = ontologyService.getDataFactory().getOWLObjectProperty(IRI.create(ontologyService.getOntologyIRIStr() + propertyName));
        Set<OWLIndividual> individuals = individual.getObjectPropertyValues(property, ontologyService.getOntology());
        return individuals.stream()
                .map(ind -> ind.asOWLNamedIndividual().getIRI().toString())
                .collect(Collectors.toList());
    }

    public void addObjectProperties(OWLNamedIndividual individual, String propertyName, List<String> values) {
        if (values != null) {
            OWLObjectProperty property = ontologyService.getDataFactory().getOWLObjectProperty(IRI.create(ontologyService.getOntologyIRIStr() + propertyName));
            for (String value : values) {
                OWLNamedIndividual objectIndividual = ontologyService.getDataFactory().getOWLNamedIndividual(IRI.create(ontologyService.getOntologyIRIStr() + value));
                OWLAxiom axiom = ontologyService.getDataFactory().getOWLObjectPropertyAssertionAxiom(property, individual, objectIndividual);
                ontologyService.getOntoManager().addAxiom(ontologyService.getOntology(), axiom);
            }
        }
    }

    public void removeObjectProperties(OWLNamedIndividual individual, String propertyName) {
        OWLObjectProperty property = ontologyService.getDataFactory().getOWLObjectProperty(IRI.create(ontologyService.getOntologyIRIStr() + propertyName));
        Set<OWLAxiom> axiomsToRemove = ontologyService.getOntology().getObjectPropertyAssertionAxioms(individual).stream()
                .filter(axiom -> axiom.getProperty().equals(property))
                .collect(Collectors.toSet());
        ontologyService.getOntoManager().removeAxioms(ontologyService.getOntology(), axiomsToRemove);
    }
}
