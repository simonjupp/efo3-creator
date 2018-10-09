package uk.ac.ebi.spot;

import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.util.OWLEntityRenamer;

import java.io.*;
import java.util.*;

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

                 Set<IRI> seenMondoIri = new HashSet<IRI>();
                 Set<IRI> seenEfoIri = new HashSet<IRI>();

                 try {

                     br = new BufferedReader(new FileReader(csvFile));
                     while ((line = br.readLine()) != null) {

                         // use comma as separator
                         String[] mappings = line.split(cvsSplitBy);
                         IRI mondoIri = IRI.create(mappings[0]) ;

                         seenMondoIri.add(mondoIri);
                         seenMondoIri.add(mondoIri);
                         if (!ontology.containsClassInSignature(mondoIri)) {

                             if (seenMondoIri.contains(mondoIri)) {
                                 System.err.println("WARNING: The MONDO iri " +  mondoIri.toString() + " is duplicated in the mapping file");
                             }   else {
                                 System.err.println("WARNING: The MONDO iri " +  mondoIri.toString() + " can't be found but it is mapped to an EFO term");
                             }

                         } else {

                             String mondoCurie =  mondoIri.toString().substring(mondoIri.toString().lastIndexOf("/")+1).replace("_", ":");

                             IRI efoIri = IRI.create(mappings[1]) ;
                             String efoCurie =  efoIri.toString().substring(efoIri.toString().lastIndexOf("/")+1).replace("_", ":");

                             if (!seenEfoIri.contains(efoIri)) {
                                 seenEfoIri.add(efoIri);
                             } else {
                                 System.err.println("WARNING: EFO iri " +  efoIri.toString() + " is duplicated in the mapping file");
                             }

                             List<OWLOntologyChange> change = renamer.changeIRI(mondoIri, efoIri);
                             ontology.getOWLOntologyManager().applyChanges(change);

                             List<RemoveAxiom> toRemove = new ArrayList<>();

                             // set the label to an alternate label
                             for (OWLAnnotationAssertionAxiom axiom : factory.getOWLClass(efoIri).getAnnotationAssertionAxioms(ontology)) {
                                 if (axiom.getProperty().equals(factory.getRDFSLabel())) {

                                     toRemove.add(new RemoveAxiom(ontology, axiom));

                                     OWLAnnotationAssertionAxiom newAxiom = manager.getOWLDataFactory().getOWLAnnotationAssertionAxiom(
                                        synonymProperty,
                                        efoIri,
                                        axiom.getValue(),
                                        Collections.singleton(mondoPreferredLabelAnnotation)
                                     );
                                     ontology.getOWLOntologyManager().applyChange(new AddAxiom(ontology, newAxiom));
                                 }


                             }


                             // set the id
                             for (OWLAnnotationAssertionAxiom axiom : factory.getOWLClass(efoIri).getAnnotationAssertionAxioms(ontology)) {

                                 if (axiom.getProperty().equals(idProperty)) {
                                     String newId = efoIri.toString().substring(efoIri.toString().lastIndexOf("/")+1).replace("_", ":");

                                     toRemove.add(new RemoveAxiom(ontology, axiom));

                                     OWLAnnotationAssertionAxiom newAxiom = manager.getOWLDataFactory().getOWLAnnotationAssertionAxiom(
                                        idProperty,
                                        efoIri,
                                        factory.getOWLLiteral(newId)
                                     );
                                     ontology.getOWLOntologyManager().applyChange(new AddAxiom(ontology, newAxiom));

                                 }
                             }
                             ontology.getOWLOntologyManager().applyChanges(toRemove);
                             toRemove.clear();

                             // set the MONDO id as an xref
                             for (OWLAnnotationAssertionAxiom axiom : factory.getOWLClass(efoIri).getAnnotationAssertionAxioms(ontology)) {

                                 if (axiom.getProperty().equals(xrefProperty)) {
                                     if (axiom.getValue() instanceof OWLLiteral)  {

                                         OWLLiteral literalId = (OWLLiteral) axiom.getValue();
                                         if (literalId.getLiteral().equals(efoCurie))  {

                                             toRemove.add(new RemoveAxiom(ontology, axiom));
                                             OWLAnnotationAssertionAxiom newXrefAxiom = manager.getOWLDataFactory().getOWLAnnotationAssertionAxiom(
                                                    xrefProperty,
                                                    efoIri,
                                                    factory.getOWLLiteral(mondoCurie)
                                             );
                                             ontology.getOWLOntologyManager().applyChange(new AddAxiom(ontology, newXrefAxiom));
                                         }
                                     }

                                 }
                             }
                             ontology.getOWLOntologyManager().applyChanges(toRemove);
                         }



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
