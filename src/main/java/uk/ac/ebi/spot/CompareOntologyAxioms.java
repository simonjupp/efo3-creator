package uk.ac.ebi.spot;

import org.semanticweb.HermiT.Reasoner;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Simon Jupp
 * @date 18/01/2016
 * Samples, Phenotypes and Ontologies Team, EMBL-EBI
 */
@Deprecated
public class CompareOntologyAxioms {
    private Logger log = LoggerFactory.getLogger(getClass());

    public CompareOntologyAxioms () {
        OWLOntologyManager originalManager = OWLManager.createOWLOntologyManager();
        OWLOntologyManager newManager = OWLManager.createOWLOntologyManager();

        try {


            OWLOntology originalOntology = originalManager.loadOntology(IRI.create("file:///Users/jupp/dev/ontologies/efo-sf-code/src/efoinowl/efo.owl"));

            System.out.println("orginal loaded!");

            OWLOntology newOntology = newManager.loadOntology(IRI.create("file:///Users/jupp/dev/java/efo3-creator/efo/release/efo.owl"));

            System.out.println(" new loaded!");
            Reasoner originalReasoner = new Reasoner(originalOntology);
            Reasoner newReasoner = new Reasoner(newOntology);

            int x = 0;
            for (OWLSubClassOfAxiom originalSubclassAxiom : originalOntology.getAxioms(AxiomType.SUBCLASS_OF)) {

                if (newOntology.containsAxiom(originalSubclassAxiom)) {
                    continue;
                }

                // does axiom hold in new ontology
                if (!newReasoner.isEntailed(originalSubclassAxiom))  {


                    if (!originalSubclassAxiom.toString().contains("Orphanet_"))  {
                        log.info(originalSubclassAxiom + " is missing");
                             x++;

                    }
                }

            }
            System.out.println("total: " + x);
        } catch (OWLOntologyCreationException e) {
            e.printStackTrace();
        }

    }

    public static void main(String[] args) {

        new CompareOntologyAxioms();
    }
}
