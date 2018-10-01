package uk.ac.ebi.spot.ontobuilder;

import org.apache.commons.io.FileUtils;
import org.semanticweb.HermiT.Reasoner;
import org.semanticweb.HermiT.structural.OWLAxioms;
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
import uk.ac.manchester.cs.owlapi.modularity.ModuleType;
import uk.ac.manchester.cs.owlapi.modularity.SyntacticLocalityModuleExtractor;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
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
@Deprecated
public class OntologyBuilder {

    private Logger log = LoggerFactory.getLogger(getClass());


    private String name;
    private IRI source;
    private String baseDir;
    private String outputdir;
    private String baseIri;
    private Collection<IRI> roots;

    private OWLOntologyManager manager = OWLManager.createOWLOntologyManager();

    public OntologyBuilder(String name, IRI source, String baseDir, String outputdir, String baseIri, Collection<IRI> roots) {
        this.name = name;
        this.source = source;
        this.baseDir = baseDir;
        this.outputdir = outputdir;
        this.baseIri = baseIri;
        this.roots = roots;

        manager.setSilentMissingImportsHandling(true);

    }

    /**
     *
     * Read imported terms, check all are valid, generate new slim
     * Load the source ontology and the generated slim
     * Remove any terms that are not under source root terms
     * Report any slim terms that are not rooted under source
     *
     *
     */
    public void generateRelease () {


        log.info("Preparing release for " + name + "...");

        Collection<IRI> allSlimTerms = new HashSet<>();
        OWLOntology sourceOntology =   null;
        try {
            sourceOntology = manager.loadOntology(source);
        } catch (OWLOntologyCreationException e) {
            e.printStackTrace();
        }

        try {

            // keep a copy of all slim terms, used for checking later
            String dir = baseDir + File.separator + "generated";
            File generateDir = new File(dir);

            for (OntologyConfiguration ontologyConfiguration : getConfirguation()) {
                log.info("Loading " + ontologyConfiguration.getShortName());
                allSlimTerms.addAll(readTermsFromFile(baseDir, ontologyConfiguration.getShortName()));
                File ontoDir= new File(generateDir, ontologyConfiguration.getShortName());
                File inputSlim = new File( ontoDir, ontologyConfiguration.getShortName() + ".owl");
                manager.loadOntologyFromOntologyDocument(inputSlim);
            }

            // merge all the ontologies (source + slims)
            OWLOntologyMerger merger = new OWLOntologyMerger(manager);
            OWLOntology mergedOntology = merger.createMergedOntology(manager, IRI.create(name + "-merged"));
            File releaseDir = new File(outputdir);
            // save the merged file
            manager.saveOntology(mergedOntology, new OWLFunctionalSyntaxOntologyFormat(), IRI.create(new File(releaseDir, name + "-merged.owl")));
            manager.removeOntology(sourceOntology);
            // find dangling slim terms

            // remove all dangling
            Reasoner reasoner = new Reasoner(mergedOntology);
            OWLEntityRemover owlEntityRemover = new OWLEntityRemover(manager, Collections.singleton(mergedOntology));
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

                    // if it's ordo add it back under genetic disorder
                    if (cls.getIRI().getFragment().startsWith("Orphanet_") && allSlimTerms.contains(cls.getIRI())) {
                        OWLSubClassOfAxiom subClassOfAxiom = manager.getOWLDataFactory().getOWLSubClassOfAxiom(
                                manager.getOWLDataFactory().getOWLClass(cls.getIRI()),
                                manager.getOWLDataFactory().getOWLClass(IRI.create("http://www.ebi.ac.uk/efo/EFO_0000508"))
                        );
                        manager.addAxiom(mergedOntology, subClassOfAxiom);
                        log.info("Adding Orphanet term back as sublass of genetic disorder");
                    } else if (cls.getIRI().getFragment().startsWith("UBERON_") && allSlimTerms.contains(cls.getIRI())) {
                        OWLSubClassOfAxiom subClassOfAxiom = manager.getOWLDataFactory().getOWLSubClassOfAxiom(
                                manager.getOWLDataFactory().getOWLClass(cls.getIRI()),
                                manager.getOWLDataFactory().getOWLClass(IRI.create("http://www.ebi.ac.uk/efo/EFO_0000787"))
                        );
                        manager.addAxiom(mergedOntology, subClassOfAxiom);
                        log.info("Adding UBERON term back as sublass of animal component");
                    }
                    else if (allSlimTerms.contains(cls.getIRI())){
                        log.warn("Removing a slim term: " + cls.getIRI());
                        owlEntityRemover.visit(cls);
                    } else {
                        owlEntityRemover.visit(cls);
                    }

                }
            }

            manager.applyChanges(owlEntityRemover.getChanges());
            manager.saveOntology(mergedOntology,new OWLFunctionalSyntaxOntologyFormat(), IRI.create(new File(releaseDir, name + ".owl")) );

        } catch (OWLOntologyCreationException e) {
            e.printStackTrace();
        } catch (OWLOntologyStorageException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }



    }

    public Set<OntologyConfiguration> getConfirguation ()  {
        //        // file loading and set up
        File ontologyConfigFile = new File(new File(baseDir) , "ontology-config.json");
        log.info("Reading config from " + ontologyConfigFile);
        return ConfigParser.readConfig(ontologyConfigFile);

    }

    public void generateSlims () {


        log.info("Preparing slims...");

        Set<OntologyConfiguration> ontologyConfigurations = getConfirguation();

//        Map<String, Collection<IRI>> slim = new HashMap<>();
        Map<String, Collection<IRI>> missingTerms = new HashMap<>();

        // for each configured ontology , generate the slim
        for (OntologyConfiguration ontologyConfiguration : ontologyConfigurations) {


            IRI iri = ontologyConfiguration.getOntologyIri();
            IRI url = ontologyConfiguration.getOntologyUrl();
            if (url == null) {
                url = iri;
            }

            log.info("Reading " + ontologyConfiguration.getShortName() + " from " + url.toString());
            try {

                // read slim terms from file
                Set<IRI> terms = readTermsFromFile(baseDir, ontologyConfiguration.getShortName());

                // create modules from slim + ontology
                log.info("Creating module for slim " + ontologyConfiguration.getShortName());
                OWLOntology extractedModule = createSyntacticModule(terms, iri, url);

                // find any terms from slim that are no longer in the source ontology (i.e. have been deleted or obsoleted externally
                missingTerms.put(ontologyConfiguration.getShortName(), new HashSet<>());
                for (IRI term : terms) {
                    if (!extractedModule.containsClassInSignature(term)) {
                        missingTerms.get(ontologyConfiguration.getShortName()).add(term);
                    }
                }

                // create directory to save slim module
                String dir = baseDir + File.separator + "generated";
                File generateDir = new File(dir);
                if (!generateDir.exists())       {
                    FileUtils.forceMkdir(generateDir);
                }

                File ontoDir= new File(generateDir, ontologyConfiguration.getShortName());
                FileUtils.deleteDirectory(ontoDir);
                FileUtils.forceMkdir(ontoDir);

                // set the URI of the slims ontology
                OWLOntologyManager extractedManager = extractedModule.getOWLOntologyManager();
                OWLOntologyURIChanger changer = new OWLOntologyURIChanger(extractedManager);
                List<OWLOntologyChange> changes = changer.getChanges(extractedModule, IRI.create(baseIri + "/" + ontologyConfiguration.getShortName()));
                extractedManager.applyChanges(changes);

                // save the file
                File output = new File( ontoDir, ontologyConfiguration.getShortName() + ".owl");
                extractedManager.saveOntology(extractedModule, IRI.create(output));
                manager.removeOntology(extractedModule);
//                manager.loadOntology(IRI.create(output));

                log.info("Mireot created for " + ontologyConfiguration.getShortName() + " in " + output);


            } catch (IOException e) {
                e.printStackTrace();
            } catch (OWLOntologyStorageException e) {
                e.printStackTrace();
            }
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

        // merge ontologies into single ontology for mireot extraction
        try {

            manager.loadOntology(location);

            IRI mergedOntologyIri = IRI.create(iri.toString() + "-merged");

            OWLOntologyMerger owlOntologyMerger = new OWLOntologyMerger(manager);
            OWLOntology mergedOntology = owlOntologyMerger.createMergedOntology(manager,mergedOntologyIri );
            if (iri.toString().contains("orphanet")) {

                // swap part_of for is-a
                Set<OWLAxiom> partOfAxioms = manager.getOntology(mergedOntologyIri).getReferencingAxioms(manager.getOWLDataFactory().getOWLObjectProperty(IRI.create("http://purl.obolibrary.org/obo/BFO_0000050")));

                Set<OWLAxiom> axiomToRemove = new HashSet<>();
                Set<OWLAxiom> axiomToAdd = new HashSet<>();
                for (OWLAxiom ax : partOfAxioms) {
                    if (ax instanceof OWLSubClassOfAxiom) {
                        OWLSubClassOfAxiom partOfSomeAxiom = (OWLSubClassOfAxiom) ax;
                        if (partOfSomeAxiom.getSuperClass() instanceof OWLObjectSomeValuesFrom) {
                            OWLObjectSomeValuesFrom someRestriction = (OWLObjectSomeValuesFrom) partOfSomeAxiom.getSuperClass();
                            if (someRestriction.getFiller() instanceof OWLClass) {
                                axiomToRemove.add(ax);
                                axiomToAdd.add(manager.getOWLDataFactory().getOWLSubClassOfAxiom(
                                        partOfSomeAxiom.getSubClass().asOWLClass(),
                                        someRestriction.getFiller().asOWLClass()
                                ));
                            }
                        }
                    }
                }

                manager.removeAxioms(mergedOntology, axiomToRemove);
                manager.addAxioms(mergedOntology, axiomToAdd);


            }


            OWLOntology ontology =  moduleExtractor.getMireotFull(
                    signature,
                    manager,
                    Collections.singleton(mergedOntologyIri)
            );


            return ontology;


        } catch (OWLOntologyCreationException e) {
            e.printStackTrace();
            throw  new RuntimeException("Couldn't get module for " + iri, e);
        } catch (Exception e) {
            throw  new RuntimeException("Couldn't get module for " + iri, e);

        }


    }

    public static OWLOntology createSyntacticModule (Set<IRI> signature, IRI iri, IRI location) {


        OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
        manager.setSilentMissingImportsHandling(true);

        // merge ontologies into single ontology for module extraction
        try {

            OWLOntology moduleOntology = manager.loadOntology(location);

            IRI mergedOntologyIri = IRI.create(iri.toString() + "-merged");

            OWLOntologyMerger owlOntologyMerger = new OWLOntologyMerger(manager);
            OWLOntology mergedOntology = owlOntologyMerger.createMergedOntology(manager,mergedOntologyIri );
            if (iri.toString().contains("orphanet")) {

                // swap part_of for is-a
                Set<OWLAxiom> partOfAxioms = manager.getOntology(mergedOntologyIri).getReferencingAxioms(manager.getOWLDataFactory().getOWLObjectProperty(IRI.create("http://purl.obolibrary.org/obo/BFO_0000050")));

                Set<OWLAxiom> axiomToRemove = new HashSet<>();
                Set<OWLAxiom> axiomToAdd = new HashSet<>();
                for (OWLAxiom ax : partOfAxioms) {
                    if (ax instanceof OWLSubClassOfAxiom) {
                        OWLSubClassOfAxiom partOfSomeAxiom = (OWLSubClassOfAxiom) ax;
                        if (partOfSomeAxiom.getSuperClass() instanceof OWLObjectSomeValuesFrom) {
                            OWLObjectSomeValuesFrom someRestriction = (OWLObjectSomeValuesFrom) partOfSomeAxiom.getSuperClass();
                            if (someRestriction.getFiller() instanceof OWLClass) {
                                axiomToRemove.add(ax);
                                axiomToAdd.add(manager.getOWLDataFactory().getOWLSubClassOfAxiom(
                                        partOfSomeAxiom.getSubClass().asOWLClass(),
                                        someRestriction.getFiller().asOWLClass()
                                ));
                            }
                        }
                    }
                }

                manager.removeAxioms(mergedOntology, axiomToRemove);
                manager.addAxioms(mergedOntology, axiomToAdd);


            }

            SyntacticLocalityModuleExtractor moduleExtractor = new SyntacticLocalityModuleExtractor(manager, mergedOntology, ModuleType.BOT);

            Set<OWLEntity> owlEntities = new HashSet<>();
            for (IRI iri1 : signature) {
                owlEntities.add(manager.getOWLDataFactory().getOWLClass(iri1));
            }
            IRI moduleUri = IRI.create(iri.toString() + "-module");

            return moduleExtractor.extractAsOntology(owlEntities, moduleUri);

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

    private String getLabel(OWLEntity entity, OWLOntology owlOntology) {
        for (OWLAnnotation annotation:  entity.getAnnotations(owlOntology)) {
            if (annotation.getProperty().isLabel())  {
                return ( (OWLLiteral) annotation.getValue()).getLiteral().toString();
            }
        }
        return "no label";
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
        ontologyBuilder.generateSlims();
        ontologyBuilder.generateRelease();
//        OntologyBuilder ontologyBuilder = new OntologyBuilder("go", IRI.create("file:///Users/jupp/dev/ontologies/go/go-slimmer/go-metagenomics-slim.owl"), "/Users/jupp/dev/ontologies/go/go-slimmer", "/Users/jupp/dev/ontologies/go/go-slimmer/release", "http://purl.obolibrrary.org/obo/go", roots);
    }
}
