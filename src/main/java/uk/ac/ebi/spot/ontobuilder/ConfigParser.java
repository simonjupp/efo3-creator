package uk.ac.ebi.spot.ontobuilder;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.io.IOException;
import java.util.Set;

/**
 * @author Simon Jupp
 * @date 06/01/2016
 * Samples, Phenotypes and Ontologies Team, EMBL-EBI
 */
public class ConfigParser {

    public static Set<OntologyConfiguration> readConfig(File jsonData) {
        //read json file data to String

        //create ObjectMapper instance
        ObjectMapper objectMapper = new ObjectMapper();

        //convert json string to object
        try {
            return objectMapper.readValue(jsonData, new TypeReference<Set<OntologyConfiguration>>(){});
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException("Couldn't read config file");
        }

    }
}
