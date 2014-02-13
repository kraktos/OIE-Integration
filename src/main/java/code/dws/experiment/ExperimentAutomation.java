/**
 * 
 */

package code.dws.experiment;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import code.dws.baseline.GetProbability;
import code.dws.core.AutomatedNodeScoringWrapper;
import code.dws.goldStandard.ProcessAnnotatedGoldStd;
import code.dws.markovLogic.EvidenceBuilder;
import code.dws.markovLogic.MLNFileGenerator;
import code.dws.ontology.OntologyMatcher;
import code.dws.relationMap.Discover;
import code.dws.utils.Constants;

/**
 * @author Arnab Dutta
 */
public class ExperimentAutomation
{

    public static int BATCH_SIZE = 0;

    public static String PREDICATE = null;

    public static Double PROPGTN_FACTOR = 0D;

    public static int TOP_K_MATCHES = 1;

    public static String DBPEDIA_SPARQL_ENDPOINT;

    /**
     * logger
     */
    public final static Logger logger = LoggerFactory.getLogger(ExperimentAutomation.class);

    public static boolean USE_LOGIT;

    public static boolean BOOTSTRAP;

    public static boolean RELOAD_TYPE;

    public static int SCALE_WEIGHT;

    public static boolean ENGAGE_INTER_STEP;

    public static int TOP_K_NUMERIC_PROPERTIES;

    /**
     * @param args
     * @throws Exception
     */
    public static void main(String[] args) throws Exception
    {

        logger.info("\n\n =========" + args[0] + "============= \n ");

        loadConfigParameters(args);
        ExperimentAutomation.BOOTSTRAP = false;

        runAll();
    }

    public static void loadConfigParameters(String[] args)
    {

        Properties prop = new Properties();

        try {
            ExperimentAutomation.PREDICATE = args[0];

            // load a properties file
            prop.load(new FileInputStream(args[1]));

            ExperimentAutomation.PROPGTN_FACTOR = Double.parseDouble(prop.getProperty("TREE_PROPAGATION_FACTOR"));
            ExperimentAutomation.TOP_K_MATCHES = Integer.parseInt(prop.getProperty("TOPK_ANCHORS"));
            ExperimentAutomation.DBPEDIA_SPARQL_ENDPOINT = prop.getProperty("DBPEDIA_SPARQL_ENDPOINT");
            ExperimentAutomation.USE_LOGIT = Boolean.valueOf(prop.getProperty("USE_LOGIT"));
            ExperimentAutomation.RELOAD_TYPE = Boolean.valueOf(prop.getProperty("RELOAD_TYPE"));
            ExperimentAutomation.BATCH_SIZE = Integer.parseInt(prop.getProperty("BATCH_SIZE"));

            ExperimentAutomation.SCALE_WEIGHT = Integer.parseInt(prop.getProperty("SCALE_WEIGHT"));

            ExperimentAutomation.ENGAGE_INTER_STEP = Boolean.valueOf(prop.getProperty("ENGAGE_INTER_STEP"));

            ExperimentAutomation.TOP_K_NUMERIC_PROPERTIES =
                Integer.parseInt(prop.getProperty("TOP_K_NUMERIC_PROPERTIES"));

        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    /**
     * @param prop
     * @throws IOException
     * @throws OWLOntologyCreationException
     * @throws Exception
     */
    private static void runAll() throws IOException, OWLOntologyCreationException, Exception
    {

        // The density estimator is also called here..
        // EstimatorEngine.main(new String[] {ExperimentAutomation.PREDICATE});
        //

        // System.exit(1);
        //
        EvidenceBuilder.main(new String[] {PREDICATE});

        // Create the subset of data from the dump of annotated gold standard
        // for the given predicate

        ProcessAnnotatedGoldStd.main(null);
        //
        // // Run the data files to create owl file and goldStandard MLN and
        // // isOfTypeConfMLN
        OntologyMatcher.main(null);
        //
        // // create the same as prior weights
        GetProbability.main(null);
        //
        // // create MLN for sameAsConf
        //
        // // System.out.println(GenericConverter.SUB_SET_TYPES);
        // // System.out.println("************************************ ");
        // // System.out.println(GenericConverter.OBJ_SET_TYPES);
        //
        for (int k = 1; k <= ExperimentAutomation.TOP_K_MATCHES; k++) {
            MLNFileGenerator.main(new String[] {Constants.OUTPUT_OWL_FILE,
            Constants.sample_dumps + PREDICATE + "/sameAsConf.nell-dbpedia-top" + k + ".db", "sameAsConf",
            String.valueOf(k)});
        }
        //
        // // create MLN for propAsst
        //
        MLNFileGenerator.main(new String[] {Constants.OUTPUT_OWL_FILE,
        Constants.sample_dumps + PREDICATE + "/propAsstConf.nell.db", "propAsstConf"});

        AutomatedNodeScoringWrapper.main(new String[] {PREDICATE});

    }
}
