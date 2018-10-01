package uk.ac.ebi.spot;

import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.util.OWLEntityRenamer;

import java.io.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static java.lang.System.exit;

/**
 * @author Simon Jupp
 * @date 24/07/2018
 * Samples, Phenotypes and Ontologies Team, EMBL-EBI
 *
 * Quick and dirty script to switch MONDO IRIs with EFO or Orphanet IRI given a mapping file as input, the latest mondo OWL file and the name of the output file that will be created
 * This script will also adjust set the mondo label as alternate so we keep the original EFO label, change the id to the EFO or Orphanet id
 * and add an xref to the MONDO id being replaced
 *
 */
public class MondoIdSwitcher {

    public static void main(String[] args) {

        if (args.length != 3) {
            System.err.println("You must provide three arguments: the mapping tsv file, the source ontology and the output ontology");
            exit(1);
        }

        System.out.println("Switching MONDO ids to EFO or Orphanet");
        OWLOntologyManager manager = OWLManager.createOWLOntologyManager();

             OWLOntology ontology = null;
             try {
                 ontology = manager.loadOntology(IRI.create(new File(args[1])));

                 String csvFile = args[0];
                 BufferedReader br = null;
                 String line = "";
                 String cvsSplitBy = "\t";

                 OWLEntityRenamer renamer = new OWLEntityRenamer(ontology.getOWLOntologyManager(), Collections.singleton(ontology));

                 OWLDataFactory factory = manager.getOWLDataFactory();
                 OWLAnnotationProperty idProperty = factory.getOWLAnnotationProperty(IRI.create("http://www.geneontology.org/formats/oboInOwl#id"));
                 OWLAnnotationProperty xrefProperty = factory.getOWLAnnotationProperty(IRI.create("http://www.geneontology.org/formats/oboInOwl#hasDbXref"));
                 OWLAnnotationProperty synonymProperty = factory.getOWLAnnotationProperty(IRI.create("http://www.geneontology.org/formats/oboInOwl#hasExactSynonym"));

                 OWLAnnotation mondoPreferredLabelAnnotation = factory.getOWLAnnotation(factory.getRDFSComment(), factory.getOWLLiteral("preferred label from MONDO"));
                 try {

                     br = new BufferedReader(new FileReader(csvFile));
                     while ((line = br.readLine()) != null) {

                         // use comma as separator
                         String[] mappings = line.split(cvsSplitBy);
                         IRI mondoIri = IRI.create(mappings[0]) ;

                         if (!ontology.containsClassInSignature(mondoIri)) {
                             System.err.println("WARNING: The MONDO iri " +  mondoIri.toString() + " can't be found but it is mapped to an EFO term");
                         }

                         String mondoCurie =  mondoIri.toString().substring(mondoIri.toString().lastIndexOf("/")+1).replace("_", ":");

                         IRI efo_iri = IRI.create(mappings[1]) ;
                         List<OWLOntologyChange> change = renamer.changeIRI(mondoIri, efo_iri);
                         ontology.getOWLOntologyManager().applyChanges(change);

                         List<RemoveAxiom> toRemove = new ArrayList<>();

                         // set the label to an alternate label
                         for (OWLAnnotationAssertionAxiom axiom : factory.getOWLClass(efo_iri).getAnnotationAssertionAxioms(ontology)) {
                             if (axiom.getProperty().equals(factory.getRDFSLabel())) {

                                 toRemove.add(new RemoveAxiom(ontology, axiom));

                                 OWLAnnotationAssertionAxiom newAxiom = manager.getOWLDataFactory().getOWLAnnotationAssertionAxiom(
                                    synonymProperty,
                                    efo_iri,
                                    axiom.getValue(),
                                    Collections.singleton(mondoPreferredLabelAnnotation)
                                 );
                                 ontology.getOWLOntologyManager().applyChange(new AddAxiom(ontology, newAxiom));
                             }


                         }


                         // set the id
                         for (OWLAnnotationAssertionAxiom axiom : factory.getOWLClass(efo_iri).getAnnotationAssertionAxioms(ontology)) {

                             if (axiom.getProperty().equals(idProperty)) {
                                 String newId = efo_iri.toString().substring(efo_iri.toString().lastIndexOf("/")+1).replace("_", ":");

                                 toRemove.add(new RemoveAxiom(ontology, axiom));

                                 OWLAnnotationAssertionAxiom newAxiom = manager.getOWLDataFactory().getOWLAnnotationAssertionAxiom(
                                    idProperty,
                                    efo_iri,
                                    factory.getOWLLiteral(newId)
                                 );
                                 ontology.getOWLOntologyManager().applyChange(new AddAxiom(ontology, newAxiom));
                                 
                             }
                         }
                         ontology.getOWLOntologyManager().applyChanges(toRemove);

                         // set the MONDO id as an xref
                         OWLAnnotationAssertionAxiom newXrefAxiom = manager.getOWLDataFactory().getOWLAnnotationAssertionAxiom(
                                xrefProperty,
                                efo_iri,
                                factory.getOWLLiteral(mondoCurie)
                         );


                     }

                 } catch (FileNotFoundException e) {
                     System.err.println("Must provide a mapping file as input");
                     e.printStackTrace();
                 } catch (IOException e) {
                     System.err.println("Can't read the input file");
                     e.printStackTrace();
                 } finally {
                     if (br != null) {
                         try {
                             br.close();
                         } catch (IOException e) {
                             e.printStackTrace();
                         }
                     }
                 }


                 manager.saveOntology(ontology, IRI.create(new File(args[2])));

             } catch (OWLOntologyCreationException e) {
                 e.printStackTrace();
             }
             catch (OWLOntologyStorageException e) {
                 e.printStackTrace();
             }
    }
}
