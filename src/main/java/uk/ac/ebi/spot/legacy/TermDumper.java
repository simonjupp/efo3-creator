package uk.ac.ebi.spot.legacy;

import org.apache.commons.io.FileUtils;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.util.OWLOntologyURIChanger;
import org.semanticweb.owlapi.util.SimpleShortFormProvider;
import uk.ac.ebi.spot.ModuleProfileExtractor;
import uk.ac.ebi.spot.ontobuilder.OntologyBuilder;
import uk.ac.ebi.spot.ontobuilder.OntologyConfiguration;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.*;

/**
 * @author Simon Jupp
 * @date 06/01/2016
 * Samples, Phenotypes and Ontologies Team, EMBL-EBI
 *
 *
 * Given an ontology and a configuration file this class will go through the configuration files and look for terms matching each config.
 * The terms for each cofnig will be dumped into a text file in a folder named by the config
 *
 */
@Deprecated
public class TermDumper {

    private SimpleShortFormProvider simpleShortFormProvider = new SimpleShortFormProvider();
    private String slimBase = "/Users/jupp/dev/java/efo3-creator/efo/src/imported-terms";
    public TermDumper() {

    }

    public void dumpTerms(OWLOntology owlOntology, Collection<OntologyConfiguration> ontologyConfigurations) {

        for (OntologyConfiguration ontologyConfiguration : ontologyConfigurations) {
            try {

                Set<IRI> signature = getEntitiesByPrefix(owlOntology, ontologyConfiguration.getIdPrefix(), ontologyConfiguration.getPrefix());

                String slimdir = slimBase + File.separator + ontologyConfiguration.getShortName();
                FileUtils.deleteDirectory(new File(slimdir));
                FileUtils.forceMkdir(new File(slimdir));
                PrintWriter writer = new PrintWriter(new File(slimdir + File.separator + ontologyConfiguration.getShortName() + ".txt"), "UTF-8");
                for (IRI term : signature) {
                    writer.println(term.toString() + "\t" + getLabel(owlOntology.getOWLOntologyManager().getOWLDataFactory().getOWLClass(term), owlOntology));
                }
                writer.close();

            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void dumpTerms(OWLOntology owlOntology, OntologyConfiguration ontologyConfiguration) {

        try {

            Set<IRI> signature = getEntitiesByPrefix(owlOntology, ontologyConfiguration.getIdPrefix(), ontologyConfiguration.getPrefix());

            String slimdir = slimBase + File.separator + ontologyConfiguration.getShortName();
            FileUtils.deleteDirectory(new File(slimdir));
            FileUtils.forceMkdir(new File(slimdir));
            PrintWriter writer = new PrintWriter(new File(slimdir + File.separator + ontologyConfiguration.getShortName() + ".txt"), "UTF-8");
            for (IRI term : signature) {
                writer.println(term.toString() + "\t" + getLabel(owlOntology.getOWLOntologyManager().getOWLDataFactory().getOWLClass(term), owlOntology));
            }
            writer.close();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private String getLabel(OWLEntity entity, OWLOntology owlOntology) {
        for (OWLAnnotation annotation:  entity.getAnnotations(owlOntology)) {
            if (annotation.getProperty().isLabel())  {
                return ( (OWLLiteral) annotation.getValue()).getLiteral().toString();
            }
        }
        return simpleShortFormProvider.getShortForm(entity);

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
