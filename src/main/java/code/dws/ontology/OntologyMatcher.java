/**
 * 
 */

package code.dws.ontology;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.semanticweb.owlapi.model.OWLOntologyCreationException;

import code.dws.utils.Constants;

/**
 * This file takes a csv file and converts it into an owl file
 * 
 * @author Arnab Dutta
 */
public class OntologyMatcher
{

    // define Logger
    public final static Logger log = LoggerFactory.getLogger(OntologyMatcher.class);

    /**
     * @param args
     * @throws OWLOntologyCreationException
     */
    public static void main(String[] args) throws OWLOntologyCreationException
    {

        GenericConverter.convertCsvToOwl(Constants.INPUT_CSV_FILE, Constants.DELIMIT_INPUT_CSV,
            GenericConverter.TYPE.NELL_ABOX, Constants.OUTPUT_OWL_FILE);
    }

}
