package uk.ac.ebi.spot.legacy;

import org.coode.owlapi.manchesterowlsyntax.ManchesterOWLSyntax;
import org.semanticweb.elk.owlapi.ElkReasonerFactory;
import org.semanticweb.elk.owlapi.wrapper.OwlClassAxiomConverterVisitor;
import org.semanticweb.elk.reasoner.Reasoner;
import org.semanticweb.elk.reasoner.config.ReasonerConfiguration;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.io.OWLRendererException;
import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.profiles.OWL2ELProfile;
import org.semanticweb.owlapi.profiles.OWLProfileReport;
import org.semanticweb.owlapi.profiles.OWLProfileViolation;
import org.semanticweb.owlapi.reasoner.OWLReasoner;
import org.semanticweb.owlapi.util.AnnotationValueShortFormProvider;
import org.semanticweb.owlapi.util.OWLOntologyMerger;
import org.semanticweb.owlapi.util.SimpleShortFormProvider;
import org.semanticweb.owlapi.vocab.OWLRDFVocabulary;
import uk.ac.manchester.cs.owl.owlapi.mansyntaxrenderer.ManchesterOWLSyntaxFrameRenderer;
import uk.ac.manchester.cs.owl.owlapi.mansyntaxrenderer.ManchesterOWLSyntaxObjectRenderer;
import uk.ac.manchester.cs.owl.owlapi.mansyntaxrenderer.ManchesterOWLSyntaxRenderer;

import java.io.PrintWriter;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @author Simon Jupp
 * @date 19/05/2017
 * Samples, Phenotypes and Ontologies Team, EMBL-EBI
 */
@Deprecated
public class EFOtoEL {

    public EFOtoEL () {

        OWLOntologyManager manager = OWLManager.createOWLOntologyManager();

        try {

            // load all the latest EFO 3 sources
            manager.loadOntologyFromOntologyDocument(IRI.create("file:///Users/jupp//dev/ontologies/ExperimentalFactorOntology/ExFactorInOWL/releasecandidate/efoDiseaseAxioms.owl"));
            manager.loadOntologyFromOntologyDocument(IRI.create("file:///Users/jupp//dev/ontologies/ExperimentalFactorOntology/ExFactorInOWL/releasecandidate/efoordoaxioms.owl"));
            manager.loadOntologyFromOntologyDocument(IRI.create("file:///Users/jupp//dev/ontologies/ExperimentalFactorOntology/ExFactorInOWL/releasecandidate/efo_disease_module.owl"));
            manager.loadOntologyFromOntologyDocument(IRI.create("file:///Users/jupp//dev/ontologies/ExperimentalFactorOntology/ExFactorInOWL/releasecandidate/efo_ordo_module.owl"));
            manager.loadOntologyFromOntologyDocument(IRI.create("file:///Users/jupp//dev/ontologies/ExperimentalFactorOntology/ExFactorInOWL/releasecandidate/efo_release_candidate.owl"));


            OWLOntologyMerger merger = new OWLOntologyMerger(manager, false);
            OWLOntology merged = merger.createMergedOntology(manager, IRI.create("http://www.ebi.ac.uk/efo-merged"));


            OWL2ELProfile owl2ELProfile = new OWL2ELProfile();
            OWLProfileReport profileReport = owl2ELProfile.checkOntology(merged);

            AnnotationValueShortFormProvider annotationValueShortFormProvider = new AnnotationValueShortFormProvider(
                                     Collections.singletonList(manager.getOWLDataFactory().getOWLAnnotationProperty(OWLRDFVocabulary.RDFS_LABEL.getIRI())),
                                     Collections.<OWLAnnotationProperty, List<String>>emptyMap(),
                                     manager);

            ManchesterOWLSyntaxObjectRenderer owlSyntaxObjectRenderer = new ManchesterOWLSyntaxObjectRenderer(new PrintWriter(System.out), annotationValueShortFormProvider);


            Set<OWLAxiom> axiomsSet = new HashSet();

            for (OWLProfileViolation violation : profileReport.getViolations()) {
                System.out.println(violation.getAxiom());
//
                axiomsSet.add(violation.getAxiom());
//                if (violation.getAxiom() instanceof  OWLSubClassOfAxiom) {
//                    OWLSubClassOfAxiom subClassOfAxiom = (OWLSubClassOfAxiom) violation.getAxiom();
//                    OWLClass subClass = subClassOfAxiom.getSubClass().asOWLClass();
//                    OWLClassExpression superClass = subClassOfAxiom.getSuperClass();
//
//                    if (superClass instanceof OWLObjectSomeValuesFrom) {
//                        System.out.print(annotationValueShortFormProvider.getShortForm(subClass) + " subClassOf ");
//                        owlSyntaxObjectRenderer.visit((OWLObjectSomeValuesFrom) superClass);
//                    }
//
////                    owlSyntaxObjectRenderer.visit(superClass);
//
//                    System.out.println();
//
//                } else {
//                }


            }


            ManchesterOWLSyntaxFrameRenderer renderer = new ManchesterOWLSyntaxFrameRenderer(manager.createOntology(axiomsSet), new PrintWriter(System.out), annotationValueShortFormProvider);
            renderer.writeOntology();

//            ElkReasonerFactory reasonerFactory = new ElkReasonerFactory();
//            OWLReasoner reasoner = reasonerFactory.createReasoner(merged);





        } catch (OWLOntologyCreationException e) {
            e.printStackTrace();
        } catch (OWLRendererException e) {
            e.printStackTrace();
        }


    }

    public static void main(String[] args) {

        new EFOtoEL();
    }
}
