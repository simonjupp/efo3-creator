package uk.ac.ebi.spot.legacy;

import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.util.SimpleShortFormProvider;
import uk.ac.ebi.spot.ontobuilder.OntologyConfiguration;
import uk.ac.manchester.cs.owlapi.modularity.ModuleType;
import uk.ac.manchester.cs.owlapi.modularity.SyntacticLocalityModuleExtractor;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/**
 * @author Simon Jupp
 * @date 14/10/2016
 * Samples, Phenotypes and Ontologies Team, EMBL-EBI
 */
public class ModuleExtractor {

    private SimpleShortFormProvider simpleShortFormProvider = new SimpleShortFormProvider();

    public Set<OWLAxiom> getExternalAxioms (OWLOntology owlOntology, Collection<OntologyConfiguration> configurations) {

        Set<OWLAxiom> owlAxioms = new HashSet<>();

        OWLOntologyManager manager = owlOntology.getOWLOntologyManager();
        OWLDataFactory dataFactory = manager.getOWLDataFactory();
        for (OntologyConfiguration ontologyConfiguration : configurations) {

            if (ontologyConfiguration.getShortName().equals("uberon")) {
                try {


                    Set<IRI> iris = getEntitiesByPrefix(owlOntology, ontologyConfiguration.getIdPrefix(), ontologyConfiguration.getPrefix());

                    System.out.println("Loading " + ontologyConfiguration.getOntologyIri() + " for extraction" );
                    OWLOntology moduleOntology = manager.loadOntology(ontologyConfiguration.getOntologyUrl());

                    Set<OWLEntity> signature = new HashSet<>();
                    for (IRI iri : iris) {
                        signature.add(dataFactory.getOWLClass(iri));
                    }

                    SyntacticLocalityModuleExtractor moduleExtractor = new SyntacticLocalityModuleExtractor(manager, moduleOntology, ModuleType.TOP);


                    owlAxioms = moduleExtractor.extract(signature);
                    System.out.println(owlAxioms);

                }
                catch (OWLOntologyCreationException e) {
                    e.printStackTrace();
                }

            }

        }




    return owlAxioms;

}

    private Set<IRI> getEntitiesByPrefix(OWLOntology owlOntology, String idPrefix, String prefix) {

        Set<IRI> iris = new HashSet<IRI>();
        for (OWLClass cls : owlOntology.getClassesInSignature()) {

            String shortForm = simpleShortFormProvider.getShortForm(cls);
            if (prefix != null) {
                if (cls.getIRI().toString().startsWith(prefix)) {
                    iris.add(cls.getIRI());
                }
            }
            else if (shortForm.startsWith(idPrefix)) {
                iris.add(cls.getIRI());
            }

        }
        return iris;
    }

}
