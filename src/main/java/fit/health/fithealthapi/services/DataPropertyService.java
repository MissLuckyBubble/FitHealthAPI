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

    public DataPropertyService(OntologyService ontologyService){
      this.ontologyService = ontologyService;
    }

  public String getDataPropertyValue(OWLNamedIndividual individual, String propertyName) {
    OWLDataProperty property = ontologyService.getDataFactory().getOWLDataProperty(IRI.create(ontologyService.getOntologyIRIStr() + propertyName));
    Set<OWLLiteral> literals = ontologyService.getOntology().getDataPropertyAssertionAxioms(individual).stream()
            .filter(axiom -> axiom.getProperty().equals(property))
            .map(OWLDataPropertyAssertionAxiom::getObject)
            .collect(Collectors.toSet());
    if (literals.isEmpty()) {
      return null;
    }
    return literals.iterator().next().getLiteral();
  }

  public void addDataProperty(OWLNamedIndividual individual, String propertyName, String value) {
    OWLDataProperty property = ontologyService.getDataFactory().getOWLDataProperty(IRI.create(ontologyService.getOntologyIRIStr() + propertyName));
    OWLAxiom axiom = ontologyService.getDataFactory().getOWLDataPropertyAssertionAxiom(property, individual, value);
    ontologyService.getOntoManager().addAxiom(ontologyService.getOntology(), axiom);
  }

  public void addDataProperty(OWLNamedIndividual individual, String propertyName, float value) {
    OWLDataProperty property = ontologyService.getDataFactory().getOWLDataProperty(IRI.create(ontologyService.getOntologyIRIStr() + propertyName));
    OWLAxiom axiom = ontologyService.getDataFactory().getOWLDataPropertyAssertionAxiom(property, individual, value);
    ontologyService.getOntoManager().addAxiom(ontologyService.getOntology(), axiom);
  }


  public void addDataProperty(OWLNamedIndividual individual, String propertyName, int value) {
    OWLDataProperty property = ontologyService.getDataFactory().getOWLDataProperty(IRI.create(ontologyService.getOntologyIRIStr() + propertyName));
    OWLAxiom axiom = ontologyService.getDataFactory().getOWLDataPropertyAssertionAxiom(property, individual, value);
    ontologyService.getOntoManager().addAxiom(ontologyService.getOntology(), axiom);
  }

  public float getFloatValue(OWLNamedIndividual individual, String propertyName) {
    String value = getDataPropertyValue(individual, propertyName);
    return value == null ? 0 : Float.parseFloat(value);
  }

  public int getIntValue(OWLNamedIndividual individual, String propertyName) {
    String value = getDataPropertyValue(individual, propertyName);
    return value == null ? 0 : Integer.parseInt(value);
  }

  public void removeDataProperties(OWLNamedIndividual individual, String propertyName) {
    OWLDataProperty property = ontologyService.getDataFactory().getOWLDataProperty(IRI.create(ontologyService.getOntologyIRIStr() + propertyName));
    Set<OWLAxiom> axiomsToRemove = ontologyService.getOntology().getDataPropertyAssertionAxioms(individual).stream()
            .filter(axiom -> axiom.getProperty().equals(property))
            .collect(Collectors.toSet());
    ontologyService.getOntoManager().removeAxioms(ontologyService.getOntology(), axiomsToRemove);
  }




}
