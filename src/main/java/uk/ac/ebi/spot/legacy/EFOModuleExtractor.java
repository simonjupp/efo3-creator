package uk.ac.ebi.spot.legacy;

import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.util.OWLOntologyMerger;
import org.semanticweb.owlapi.util.SimpleShortFormProvider;
import uk.ac.ebi.spot.ontobuilder.ConfigParser;
import uk.ac.ebi.spot.ontobuilder.OntologyConfiguration;

import java.io.File;
import java.util.*;

/**
 * @author Simon Jupp
 * @date 06/01/2016
 * Samples, Phenotypes and Ontologies Team, EMBL-EBI
 *
 * This class extracts all the individuals ontologies referenced in EFO out into individual files using a Full MIREOT from the source
 *
 */
public class EFOModuleExtractor {

    Collection<OntologyConfiguration> ontologyConfigurations;

    public EFOModuleExtractor(String configFile) {

        ontologyConfigurations = ConfigParser.readConfig(new File(configFile));

    }

    public void convert() {

        OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
        SimpleShortFormProvider simpleShortFormProvider = new SimpleShortFormProvider();

        try {

            // load all the latest EFO 3 sources
            manager.loadOntologyFromOntologyDocument(IRI.create("file:///Users/jupp//dev/ontologies/ExperimentalFactorOntology/ExFactorInOWL/releasecandidate/efoDiseaseAxioms.owl"));
            manager.loadOntologyFromOntologyDocument(IRI.create("file:///Users/jupp//dev/ontologies/ExperimentalFactorOntology/ExFactorInOWL/releasecandidate/efoordoaxioms.owl"));
            manager.loadOntologyFromOntologyDocument(IRI.create("file:///Users/jupp//dev/ontologies/ExperimentalFactorOntology/ExFactorInOWL/releasecandidate/efo_disease_module.owl"));
            manager.loadOntologyFromOntologyDocument(IRI.create("file:///Users/jupp//dev/ontologies/ExperimentalFactorOntology/ExFactorInOWL/releasecandidate/efo_ordo_module.owl"));
            manager.loadOntologyFromOntologyDocument(IRI.create("file:///Users/jupp//dev/ontologies/ExperimentalFactorOntology/ExFactorInOWL/releasecandidate/efo_release_candidate.owl"));


            // merge to create single ontology
            OWLOntologyMerger merger = new OWLOntologyMerger(manager);
            OWLOntology merged = merger.createMergedOntology(manager, IRI.create("http://www.ebi.ac.uk/efo-merged")) ;
//            OntologyConfiguration patoConfig = new OntologyConfiguration(
//                    "pato", IRI.create("http://purl.obolibrary.org/obo/pato.owl"), IRI.create("http://purl.obolibrary.org/obo/pato.owl"), "PATO_");

            // dump the terms from EFO into a text file
            TermDumper termDumper = new TermDumper(merged, ontologyConfigurations);

            // remove any non EFO axioms (i.e. axioms that come from an external ontology)

            ModuleExtractor moduleExtractor = new ModuleExtractor();
            Set<OWLAxiom> externalAxioms = moduleExtractor.getExternalAxioms(merged, ontologyConfigurations);


            List<OWLOntologyChange> changes = manager.removeAxioms(merged, externalAxioms);

            for (OWLOntologyChange change : changes) {
                System.out.println(change.toString());
            }

            // remove any non EFO subclass axioms between named classes. subclasses we may have created



            List<RemoveAxiom> axiomsToRemove = new ArrayList<RemoveAxiom>();
            for (OWLAxiom axiom : merged.getAxioms(AxiomType.SUBCLASS_OF)) {
                OWLSubClassOfAxiom subClassOfAxiom = (OWLSubClassOfAxiom) axiom;

                OWLClassExpression subclass = subClassOfAxiom.getSubClass();
                OWLClassExpression superclass = subClassOfAxiom.getSuperClass();
                // only get named classes
                if (!subclass.isAnonymous() && !superclass.isAnonymous())  {


                    // special case ignore any BFO classes
                    if (subclass.asOWLClass().getIRI().toString().startsWith("http://www.ifomis.org/bfo")
                            || superclass.asOWLClass().getIRI().toString().startsWith("http://www.ifomis.org/bfo"))  {
                        continue;
                    }

                    String sub = simpleShortFormProvider.getShortForm(subclass.asOWLClass());
                    String sup = simpleShortFormProvider.getShortForm(superclass.asOWLClass())  ;

                    // keep any axioms involving orphanet terms
                    if (sub.startsWith("Orphanet_") || sup.startsWith("Orphanet_")) {
                    }
                    else if (!sub.startsWith("EFO_") && !sup.startsWith("EFO_")) {
                        System.out.println("Removing axiom that shouldn't be in EFO" + axiom.toString());

                        axiomsToRemove.add(new RemoveAxiom(merged, axiom));
                    }

                }
            }

            manager.applyChanges(axiomsToRemove);
            manager.saveOntology(merged, IRI.create("file:///Users/jupp/dev/java/efo3-creator/efo/src/efo-release-candidate.owl"));


        } catch (OWLOntologyCreationException e) {
            e.printStackTrace();
        } catch (OWLOntologyStorageException e) {
            e.printStackTrace();
        }


    }

    public static void main(String[] args) {
        System.setProperty("entityExpansionLimit", "100000000");

        EFOModuleExtractor converter = new EFOModuleExtractor("/Users/jupp/dev/java/efo3-creator/efo/src/ontology-config.json");
        converter.convert();
    }

}
