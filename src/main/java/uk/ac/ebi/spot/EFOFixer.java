package uk.ac.ebi.spot;

import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.util.OWLEntityRemover;
import org.semanticweb.owlapi.util.OWLEntityRenamer;
import org.semanticweb.owlapi.vocab.OWL2Datatype;
import org.semanticweb.owlapi.vocab.OWLRDFVocabulary;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static java.lang.System.exit;

/**
 * @author Simon Jupp
 * @date 18/04/2018
 * Samples, Phenotypes and Ontologies Team, EMBL-EBI
 *
 * This class takes EFO 2, convert all untyped annotations to strings
 * convert EFO synonym property to OBO synonym
 * convert definition property to ISA definition
 * convert all xrefs to OBO xref
 * changes how we obsolete classes
 *
 */
public class EFOFixer {


    public EFOFixer() {

    }

    public OWLOntology fixProperties(OWLOntology ontology) {

        OWLEntityRenamer renamer = new OWLEntityRenamer(ontology.getOWLOntologyManager(), Collections.singleton(ontology));

        System.out.println("Changing efo:alternative_term to oboInOwl:hasExactSynonym");
        List<OWLOntologyChange> changes = new ArrayList<OWLOntologyChange>();
        changes.addAll(
                renamer.changeIRI(IRI.create("http://www.ebi.ac.uk/efo/alternative_term"), IRI.create("http://www.geneontology.org/formats/oboInOwl#hasExactSynonym")));

        ontology.getOWLOntologyManager().applyChanges(changes);
        changes.clear();

        System.out.println("check for usages of efo:definition and change to use IAO_00000115");

        OWLAnnotationProperty efoDefinition = ontology.getOWLOntologyManager().getOWLDataFactory().getOWLAnnotationProperty(IRI.create("http://www.ebi.ac.uk/efo/definition"));
        OWLAnnotationProperty iaoDefinition = ontology.getOWLOntologyManager().getOWLDataFactory().getOWLAnnotationProperty(IRI.create("http://purl.obolibrary.org/obo/IAO_0000115"));
        for (OWLAxiom axiom : ontology.getReferencingAxioms(efoDefinition)) {
            if (axiom instanceof OWLAnnotationAssertionAxiom) {
                OWLAnnotationAssertionAxiom oldDefinitionAssertionAxiom = (OWLAnnotationAssertionAxiom) axiom;

                changes.add(new RemoveAxiom(ontology, oldDefinitionAssertionAxiom));
                OWLAnnotationAssertionAxiom newDefinitionAssertionAxiom = ontology.getOWLOntologyManager().getOWLDataFactory().getOWLAnnotationAssertionAxiom(
                        iaoDefinition, oldDefinitionAssertionAxiom.getSubject(), oldDefinitionAssertionAxiom.getValue(), oldDefinitionAssertionAxiom.getAnnotations()
                );
                changes.add(new AddAxiom(ontology, newDefinitionAssertionAxiom));

            }
        }

        ontology.getOWLOntologyManager().applyChanges(changes);
        changes.clear();

        // change to owl:deprecated
        OWLClass deprecatedClass = ontology.getOWLOntologyManager().getOWLDataFactory().getOWLClass(IRI.create("http://www.geneontology.org/formats/oboInOwl#ObsoleteClass"));
        for (OWLClassExpression owlClass : deprecatedClass.getSubClasses(ontology)) {
            if (!owlClass.isAnonymous())   {

                OWLAnnotationProperty deprecated = ontology.getOWLOntologyManager().getOWLDataFactory().getOWLAnnotationProperty(OWLRDFVocabulary.OWL_DEPRECATED.getIRI());
                OWLAnnotationAssertionAxiom axiom = ontology.getOWLOntologyManager().getOWLDataFactory().getOWLAnnotationAssertionAxiom(
                        deprecated,owlClass.asOWLClass().getIRI(),
                        ontology.getOWLOntologyManager().getOWLDataFactory().getOWLLiteral(true) );
                ontology.getOWLOntologyManager().applyChange(new AddAxiom(ontology, axiom));
            }
        }

        // remove http://www.ebi.ac.uk/efo/bioportal_provenance property

        OWLEntityRemover remover = new OWLEntityRemover(ontology.getOWLOntologyManager(), Collections.singleton(ontology));
        remover.visit(ontology.getOWLOntologyManager().getOWLDataFactory().getOWLAnnotationProperty(IRI.create("http://www.ebi.ac.uk/efo/bioportal_provenance property")));
        remover.visit(ontology.getOWLOntologyManager().getOWLDataFactory().getOWLAnnotationProperty(IRI.create("http://www.ebi.ac.uk/efo/bioportal_provenance")));
        remover.visit(ontology.getOWLOntologyManager().getOWLDataFactory().getOWLClass(IRI.create("http://www.geneontology.org/formats/oboInOwl#ObsoleteClass")));
        ontology.getOWLOntologyManager().applyChanges(remover.getChanges());


        // get all sub propert of http://www.ebi.ac.uk/efo/definition_citation

        OWLAnnotationProperty def_citation = ontology.getOWLOntologyManager().getOWLDataFactory().getOWLAnnotationProperty(IRI.create("http://www.ebi.ac.uk/efo/definition_citation"));
        for (OWLAnnotationProperty property : def_citation.getSubProperties(ontology)) {
            for (OWLAnnotationAssertionAxiom annotationAssertionAxiom : property.getAnnotationAssertionAxioms(ontology)) {
                changes.add(new RemoveAxiom(ontology, annotationAssertionAxiom));
            };
        }
        ontology.getOWLOntologyManager().applyChanges(changes);
        changes.clear();

        for (OWLAnnotationProperty property : def_citation.getSubProperties(ontology)) {

            changes.addAll(
                            renamer.changeIRI(property.getIRI(), IRI.create("http://www.geneontology.org/formats/oboInOwl#hasDbXref")));
        }
        ontology.getOWLOntologyManager().applyChanges(changes);
        changes.clear();

        for (OWLAxiom ax : ontology.getSubAnnotationPropertyOfAxioms(ontology.getOWLOntologyManager().getOWLDataFactory().getOWLAnnotationProperty(IRI.create("http://www.geneontology.org/formats/oboInOwl#hasDbXref")))) {
            changes.add(new RemoveAxiom(ontology, ax));
        }
        ontology.getOWLOntologyManager().applyChanges(changes);
        return ontology;
    }

    public OWLOntology fixLiterals (OWLOntology ontology) {

        List<OWLOntologyChange> changes = new ArrayList<>();

        for (OWLDatatype datatype : ontology.getDatatypesInSignature()) {
            if (datatype.isRDFPlainLiteral()) {

                for (OWLAxiom axiom : ontology.getReferencingAxioms(datatype)) {
                    if (axiom instanceof  OWLAnnotationAssertionAxiom) {
                        OWLAnnotationAssertionAxiom ax = (OWLAnnotationAssertionAxiom) axiom ;
                        changes.add(new RemoveAxiom(ontology, ax));

                        OWLLiteral newLiteral = ontology.getOWLOntologyManager().getOWLDataFactory().getOWLLiteral(
                                ((OWLLiteral) ax.getValue()).getLiteral(), OWL2Datatype.XSD_STRING);

                        OWLAnnotationAssertionAxiom annotationAssertionAxiom = ontology.getOWLOntologyManager().getOWLDataFactory().getOWLAnnotationAssertionAxiom(
                                ax.getProperty(),
                                ax.getSubject(),
                                newLiteral);

                        changes.add(new AddAxiom(ontology, annotationAssertionAxiom));

                    }
                }
            }

        }

        ontology.getOWLOntologyManager().applyChanges(changes);

        return ontology;
    }

    public static void main(String[] args) {

        if (args.length != 2) {
            System.err.println("You must provide two arguments: the original EFO file and the new file you want to build");
            exit(1);
        }
        OWLOntologyManager manager = OWLManager.createOWLOntologyManager();

        OWLOntology ontology = null;
        try {
            ontology = manager.loadOntology(IRI.create(new File(args[0])));

            EFOFixer efoFixer = new EFOFixer();
            ontology = efoFixer.fixLiterals(ontology);
            ontology = efoFixer.fixProperties(ontology);
            manager.saveOntology(ontology, IRI.create(new File(args[1])));



        } catch (OWLOntologyCreationException e) {
            e.printStackTrace();
        } catch (OWLOntologyStorageException e) {
            e.printStackTrace();
        }

    }
}
