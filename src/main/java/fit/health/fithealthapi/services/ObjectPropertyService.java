package fit.health.fithealthapi.services;

import org.semanticweb.owlapi.model.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

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
                .map(ind -> ontologyService.getFragment(ind.asOWLNamedIndividual().getIRI().toString()))
                .collect(Collectors.toList());
    }

    public List<OWLOntologyChange> addObjectProperties(OWLNamedIndividual individual, String propertyName, List<String> values) {
        if (values == null) {
            return List.of();
        }
        OWLObjectProperty property = ontologyService.getDataFactory().getOWLObjectProperty(IRI.create(ontologyService.getOntologyIRIStr() + propertyName));
        return values.stream()
                .map(value -> {
                    OWLNamedIndividual objectIndividual = ontologyService.getDataFactory().getOWLNamedIndividual(IRI.create(ontologyService.getOntologyIRIStr() + value));
                    OWLAxiom axiom = ontologyService.getDataFactory().getOWLObjectPropertyAssertionAxiom(property, individual, objectIndividual);
                    return new AddAxiom(ontologyService.getOntology(), axiom);
                })
                .collect(Collectors.toList());
    }

    public List<OWLOntologyChange> removeObjectProperties(OWLNamedIndividual individual, String propertyName) {
        OWLObjectProperty property = ontologyService.getDataFactory().getOWLObjectProperty(IRI.create(ontologyService.getOntologyIRIStr() + propertyName));
        Set<OWLAxiom> axiomsToRemove = ontologyService.getOntology().getObjectPropertyAssertionAxioms(individual).stream()
                .filter(axiom -> axiom.getProperty().equals(property))
                .collect(Collectors.toSet());
        return axiomsToRemove.stream()
                .map(axiom -> new RemoveAxiom(ontologyService.getOntology(), axiom))
                .collect(Collectors.toList());
    }
}
