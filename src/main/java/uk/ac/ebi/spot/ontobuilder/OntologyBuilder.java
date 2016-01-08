package uk.ac.ebi.spot.ontobuilder;

import org.apache.commons.io.FileUtils;
import org.semanticweb.HermiT.Reasoner;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.io.OWLFunctionalSyntaxOntologyFormat;
import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.reasoner.OWLReasoner;
import org.semanticweb.owlapi.reasoner.structural.StructuralReasonerFactory;
import org.semanticweb.owlapi.util.OWLEntityRemover;
import org.semanticweb.owlapi.util.OWLOntologyMerger;
import org.semanticweb.owlapi.util.OWLOntologyURIChanger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.ebi.spot.ModuleProfileExtractor;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.*;
import java.util.stream.Collector;
import java.util.stream.Collectors;


/**
 * @author Simon Jupp
 * @date 06/01/2016
 * Samples, Phenotypes and Ontologies Team, EMBL-EBI
 */
public class OntologyBuilder {

    private Logger log = LoggerFactory.getLogger(getClass());


    /**
     *
     * Read imported terms, check all are valid, generate new slim
     * Load the source ontology and the generated slim
     * Remove any terms that are not under source root terms
     * Report any slim terms that are not rooted under source
     *
     *
     * @param source
     * @param baseDir
     */
    public OntologyBuilder (String name, IRI source, String baseDir, String outputdir, String baseIri, Collection<IRI> roots) {


        File ontologyConfigFile = new File(new File(baseDir) , "ontology-config.json");
        log.info("Reading config from " + ontologyConfigFile);

        Set<OntologyConfiguration> ontologyConfigurations = ConfigParser.readConfig(ontologyConfigFile);

        Map<String, Collection<IRI>> slim = new HashMap<>();
        Collection<IRI> allSlimTerms = new HashSet<>();
        Map<String, Collection<IRI>> missingTerms = new HashMap<>();

        OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
        manager.setSilentMissingImportsHandling(true);
        OWLOntology sourceOntology =   null;
        try {
            sourceOntology = manager.loadOntology(source);
        } catch (OWLOntologyCreationException e) {
            e.printStackTrace();
        }

        for (OntologyConfiguration ontologyConfiguration : ontologyConfigurations) {

            IRI iri = ontologyConfiguration.getOntologyIri();
            IRI url = ontologyConfiguration.getOntologyUrl();
            if (url == null) {
                url = iri;
            }

            log.info("Reading " + ontologyConfiguration.getShortName() + " from " + url.toString());
            try {

                // read slim terms
                Set<IRI> terms = readTermsFromFile(baseDir, ontologyConfiguration.getShortName());

                slim.put(ontologyConfiguration.getShortName(), new HashSet<>());
                slim.get(ontologyConfiguration.getShortName()).addAll(terms);
                allSlimTerms.addAll(terms);

                OWLOntology extractedModule = createModule(terms, iri, url);

                missingTerms.put(ontologyConfiguration.getShortName(), new HashSet<>());
                for (IRI term : terms) {
                    if (!extractedModule.containsClassInSignature(term)) {
                          missingTerms.get(ontologyConfiguration.getShortName()).add(term);
                    }
                }

                String dir = baseDir + File.separator + "generated";
                File generateDir = new File(dir);
                if (!generateDir.exists())       {
                    FileUtils.forceMkdir(generateDir);
                }

                File ontoDir= new File(generateDir, ontologyConfiguration.getShortName());
                FileUtils.deleteDirectory(ontoDir);
                FileUtils.forceMkdir(ontoDir);


                OWLOntologyManager extractedManager = extractedModule.getOWLOntologyManager();

                OWLOntologyURIChanger changer = new OWLOntologyURIChanger(extractedManager);
                List<OWLOntologyChange> changes = changer.getChanges(extractedModule, IRI.create(baseIri + "/" + ontologyConfiguration.getShortName()));
                extractedManager.applyChanges(changes);

                File output = new File( ontoDir, ontologyConfiguration.getShortName() + ".owl");
                extractedManager.saveOntology(extractedModule, IRI.create(output));
                manager.loadOntology(IRI.create(output));

                log.info("Module created for " + ontologyConfiguration.getShortName() + " in " + output);


            } catch (IOException e) {
                e.printStackTrace();
            } catch (OWLOntologyStorageException e) {
                e.printStackTrace();
            } catch (OWLOntologyCreationException e) {
                e.printStackTrace();
            }
        }


        try {

            // merge ontologies
            OWLOntologyMerger merger = new OWLOntologyMerger(manager);
            OWLOntology mergedOntology = merger.createMergedOntology(manager, IRI.create(name + "-merged"));
            File releaseDir = new File(outputdir);
            manager.saveOntology(mergedOntology, new OWLFunctionalSyntaxOntologyFormat(), IRI.create(new File(releaseDir, name + "-merged.owl")));
//            manager = null;

            manager.removeOntology(sourceOntology);
            // find dangling slim terms

            // remove all dangling
//            StructuralReasonerFactory reasonerFactory= new StructuralReasonerFactory();
            Reasoner reasoner = new Reasoner(mergedOntology);
//            OWLOntologyManager danglingManager = OWLManager.createOWLOntologyManager();
//            OWLOntology dangling = danglingManager.loadOntology(IRI.create(new File(releaseDir, name + "-merged.owl")));
            OWLEntityRemover owlEntityRemover = new OWLEntityRemover(manager, Collections.singleton(mergedOntology));
//            OWLReasoner reasoner = reasonerFactory.createReasoner(mergedOntology);
            reasoner.classifyClasses();
            for (OWLClass cls : mergedOntology.getClassesInSignature()) {

                boolean isUnderRoot = false;
                for (IRI iri : roots)  {
                    OWLClass root = manager.getOWLDataFactory().getOWLClass(iri);
                    if (reasoner.isEntailed(manager.getOWLDataFactory().getOWLSubClassOfAxiom(cls, root))) {
                        isUnderRoot = true;
                    }
                }
                if (!isUnderRoot) {

                    if (allSlimTerms.contains(cls.getIRI())) {
                        log.warn("Removing a slim term: " + cls.getIRI());
                    }

                    owlEntityRemover.visit(cls);
                }
            }

            manager.applyChanges(owlEntityRemover.getChanges());
            manager.saveOntology(mergedOntology,new OWLFunctionalSyntaxOntologyFormat(), IRI.create(new File(releaseDir, name + ".owl")) );

        } catch (OWLOntologyCreationException e) {
            e.printStackTrace();
        } catch (OWLOntologyStorageException e) {
            e.printStackTrace();
        }

        // report missing terms
        System.out.println("Term missing in external ontologies");
        for (String onto : missingTerms.keySet()) {
            for (IRI iri : missingTerms.get(onto)) {
                log.warn(iri.toString() + " missing from " + onto);
            }
        }

    }

    public static OWLOntology createModule (Set<IRI> signature, IRI iri, IRI location) {
        ModuleProfileExtractor moduleExtractor = new ModuleProfileExtractor();


        OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
        manager.setSilentMissingImportsHandling(true);

        try {
            manager.loadOntology(location);

            OWLOntology ontology =  moduleExtractor.getMireotFull(
                            signature,
                            manager,
                            Collections.singleton(iri)
                    );


            return ontology;


        } catch (OWLOntologyCreationException e) {
            e.printStackTrace();
            throw  new RuntimeException("Couldn't get module for " + iri, e);
        } catch (Exception e) {
            throw  new RuntimeException("Couldn't get module for " + iri, e);

        }


    }
    private Set<IRI> readTermsFromFile(String baseDir, String shortName) throws IOException {

        String path = baseDir + File.separator + "imported-terms" + File.separator + shortName + File.separator + shortName + ".txt";

        return Files.lines(new File(path).toPath()).map(OntologyBuilder::firstIri).collect(Collectors.toSet());

    }

    public static IRI firstIri(String s) {
        return IRI.create(s.split("\\t")[0]);
    }

    public static void main(String[] args) {

        System.setProperty("entityExpansionLimit", "100000000");

        Set<IRI> roots = new HashSet<>();

        roots.add(IRI.create("http://www.ebi.ac.uk/efo/EFO_0000001"));
        roots.add(IRI.create("http://www.geneontology.org/formats/oboInOwl#ObsoleteClass"));
        OntologyBuilder ontologyBuilder = new OntologyBuilder("efo", IRI.create("file:///Users/jupp/dev/java/efo3-creator/efo/src/efo-release-candidate.owl"), "/Users/jupp/dev/java/efo3-creator/efo/src", "/Users/jupp/dev/java/efo3-creator/efo/release", "http://www.ebi.ac.uk/efo", roots);
    }
}
