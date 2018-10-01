package uk.ac.ebi.spot.legacy;

import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.util.OWLOntologyMerger;
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
@Deprecated
public class ModuleExtractor {

    private SimpleShortFormProvider simpleShortFormProvider = new SimpleShortFormProvider();

    public Set<OWLAxiom> getExternalAxioms (OWLOntology owlOntology, Collection<OntologyConfiguration> configurations) {

        Set<OWLAxiom> owlAxioms = new HashSet<>();

        OWLOntologyManager manager = owlOntology.getOWLOntologyManager();
        OWLDataFactory dataFactory = manager.getOWLDataFactory();
        for (OntologyConfiguration ontologyConfiguration : configurations) {

//            if (!ontologyConfiguration.getShortName().equals("ncbitaxon")) {
                try {


                    Set<IRI> iris = getEntitiesByPrefix(owlOntology, ontologyConfiguration.getIdPrefix(), ontologyConfiguration.getPrefix());

                    System.out.println("Loading " + ontologyConfiguration.getOntologyIri() + " for extraction");

                    OWLOntologyMerger merger = new OWLOntologyMerger(manager, false);

                    manager.loadOntology(ontologyConfiguration.getOntologyUrl());
                    OWLOntology merged = merger.createMergedOntology(manager, IRI.create("http://www.ebi.ac.uk/" + ontologyConfiguration.getShortName())) ;

                    if (ontologyConfiguration.getShortName().equals("ordo")) {

                        Set<OWLAxiom> axiomToRemove = new HashSet<>();
                        Set<OWLAxiom> axiomToAdd = new HashSet<>();
                        // convert part of to isa for ordo
                        for (OWLAxiom ax : merged.getReferencingAxioms(manager.getOWLDataFactory().getOWLEntity(EntityType.OBJECT_PROPERTY, IRI.create("http://purl.obolibrary.org/obo/BFO_0000050")))) {
                            if (ax instanceof OWLSubClassOfAxiom) {

                                 OWLClassExpression owlClassExpression = ((OWLSubClassOfAxiom) ax).getSuperClass();
                                 if (owlClassExpression instanceof OWLObjectSomeValuesFrom) {
                                     OWLObjectSomeValuesFrom someValuesFrom = (OWLObjectSomeValuesFrom) owlClassExpression;
                                     if (!someValuesFrom.getFiller().isAnonymous()) {
                                         // make this a superclass and remove the part of

                                         axiomToRemove.add(ax);
                                         axiomToAdd.add(manager.getOWLDataFactory().getOWLSubClassOfAxiom(
                                                 ((OWLSubClassOfAxiom) ax).getSubClass(),
                                                 someValuesFrom.getFiller()));

                                     }
                                 }
                            }
                        }

                        manager.removeAxioms(merged, axiomToRemove);
                        manager.addAxioms(merged, axiomToAdd);

                    }

                    Set<OWLEntity> signature = new HashSet<>();
                    for (IRI iri : iris) {
                        signature.add(dataFactory.getOWLClass(iri));
                    }

                    SyntacticLocalityModuleExtractor moduleExtractor = new SyntacticLocalityModuleExtractor(manager, merged, ModuleType.BOT);


                    owlAxioms.addAll(moduleExtractor.extract(signature));
//                    System.out.println(owlAxioms);
                    manager.removeOntology(merged);

                }
                catch (OWLOntologyCreationException e) {
                    e.printStackTrace();
                }

            }

//        }




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
