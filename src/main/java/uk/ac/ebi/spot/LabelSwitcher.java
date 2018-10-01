package uk.ac.ebi.spot;

import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.util.OWLEntityRenamer;

import java.io.*;
import java.util.Collections;
import java.util.List;

import static java.lang.System.exit;

/**
 * @author Simon Jupp
 * @date 25/07/2018
 * Samples, Phenotypes and Ontologies Team, EMBL-EBI
 */
public class LabelSwitcher {


    public static void main(String[] args) {

        if (args.length != 3) {
            System.err.println("You must provide an input and output file name");
            exit(1);
        }

        OWLOntologyManager manager = OWLManager.createOWLOntologyManager();

             OWLOntology ontology = null;
             try {
                 ontology = manager.loadOntology(IRI.create(new File(args[0])));

                 // find all terms with two labels

                 manager.saveOntology(ontology, IRI.create(new File(args[1])));

             } catch (OWLOntologyCreationException e) {
                 e.printStackTrace();
             }
             catch (OWLOntologyStorageException e) {
                 e.printStackTrace();
             }
    }

}
