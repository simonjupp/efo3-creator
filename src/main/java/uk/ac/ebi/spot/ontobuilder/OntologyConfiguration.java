package uk.ac.ebi.spot.ontobuilder;

import org.semanticweb.owlapi.model.IRI;

/**
 * @author Simon Jupp
 * @date 06/01/2016
 * Samples, Phenotypes and Ontologies Team, EMBL-EBI
 */
@Deprecated
public class OntologyConfiguration {

    private String shortName;
    private IRI ontologyIri;
    private IRI ontologyUrl;
    private String idPrefix;
    private String prefix;

    public OntologyConfiguration() {

    }

    public OntologyConfiguration(String shortName, IRI ontologyIri, IRI ontologyUrl, String idPrefix) {

        this.shortName = shortName;
        this.ontologyIri = ontologyIri;
        this.ontologyUrl = ontologyUrl;
        this.idPrefix = idPrefix;
    }

    public String getPrefix() {
        return prefix;
    }

    public void setPrefix(String prefix) {
        this.prefix = prefix;
    }

    public String getShortName() {
        return shortName;
    }

    public IRI getOntologyIri() {
        return ontologyIri;
    }

    public IRI getOntologyUrl() {
        if (ontologyUrl == null) {
            return  ontologyIri;
        }
        return ontologyUrl;
    }

    public String getIdPrefix() {
        return idPrefix;
    }


}
