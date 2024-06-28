package fit.health.fithealthapi.services;

import org.semanticweb.owlapi.model.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Set;
import java.util.logging.Logger;
import java.util.stream.Collectors;

@Service
public class DataPropertyService {

  private static final Logger LOGGER = Logger.getLogger(OntologyService.class.getName());

  @Autowired
  OntologyService ontologyService;

  public DataPropertyService(OntologyService ontologyService) {
    this.ontologyService = ontologyService;
  }

  public String getDataPropertyValue(OWLNamedIndividual individual, String propertyName) {
    OWLDataProperty property = ontologyService.getDataFactory().getOWLDataProperty(IRI.create(ontologyService.getOntologyIRIStr() + propertyName));
    Set<OWLDataPropertyAssertionAxiom> axioms = ontologyService.getOntology().getDataPropertyAssertionAxioms(individual);

    if (axioms == null || axioms.isEmpty()) {
      return null;
    }

    for (OWLDataPropertyAssertionAxiom axiom : axioms) {
      if (axiom.getProperty().equals(property)) {
        OWLLiteral literal = axiom.getObject();
        if (literal != null) {
          return literal.getLiteral();
        }
      }
    }

    return null;
  }

  public OWLOntologyChange addDataProperty(OWLNamedIndividual individual, String propertyName, String value) {
    System.out.println(propertyName + " " + value);
    OWLDataProperty property = ontologyService.getDataFactory().getOWLDataProperty(IRI.create(ontologyService.getOntologyIRIStr() + propertyName));
    OWLAxiom axiom = ontologyService.getDataFactory().getOWLDataPropertyAssertionAxiom(property, individual, value);
    return new AddAxiom(ontologyService.getOntology(), axiom);
  }

  public OWLOntologyChange addDataProperty(OWLNamedIndividual individual, String propertyName, float value) {
    System.out.println(propertyName + " " + value);

    OWLDataProperty property = ontologyService.getDataFactory().getOWLDataProperty(IRI.create(ontologyService.getOntologyIRIStr() + propertyName));
    OWLAxiom axiom = ontologyService.getDataFactory().getOWLDataPropertyAssertionAxiom(property, individual, value);
    return new AddAxiom(ontologyService.getOntology(), axiom);
  }

  public OWLOntologyChange addDataProperty(OWLNamedIndividual individual, String propertyName, int value) {
    System.out.println(propertyName + " " + value);

    OWLDataProperty property = ontologyService.getDataFactory().getOWLDataProperty(IRI.create(ontologyService.getOntologyIRIStr() + propertyName));
    OWLAxiom axiom = ontologyService.getDataFactory().getOWLDataPropertyAssertionAxiom(property, individual, value);
    return new AddAxiom(ontologyService.getOntology(), axiom);
  }

  public Set<OWLOntologyChange> removeDataProperties(OWLNamedIndividual individual, String propertyName) {
    OWLDataProperty property = ontologyService.getDataFactory().getOWLDataProperty(IRI.create(ontologyService.getOntologyIRIStr() + propertyName));
    Set<OWLAxiom> axiomsToRemove = ontologyService.getOntology().getDataPropertyAssertionAxioms(individual).stream()
            .filter(axiom -> axiom.getProperty().equals(property))
            .collect(Collectors.toSet());
    return axiomsToRemove.stream()
            .map(axiom -> new RemoveAxiom(ontologyService.getOntology(), axiom))
            .collect(Collectors.toSet());
  }

  public float getFloatValue(OWLNamedIndividual individual, String propertyName) {
    String value = getDataPropertyValue(individual, propertyName);
    return value == null ? 0 : Float.parseFloat(value);
  }

  public int getIntValue(OWLNamedIndividual individual, String propertyName) {
    String value = getDataPropertyValue(individual, propertyName);
    return value == null ? 0 : Integer.parseInt(value);
  }
}
