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

import code.dws.core.AutomatedNodeScoringWrapper;
import code.dws.markovLogic.EvidenceBuilder;
import code.dws.markovLogic.YagoDbpediaMapping;

/**
 * @author Arnab Dutta
 */
public class ExperimentAutomation {

	public static int BATCH_SIZE = 2000;

	public static String PREDICATE = null;

	public static Double PROPGTN_FACTOR = 0D;

	public static int TOP_K_MATCHES = 1;

	public static String DBPEDIA_SPARQL_ENDPOINT;

	/**
	 * logger
	 */
	public final static Logger logger = LoggerFactory
			.getLogger(ExperimentAutomation.class);

	public static String OIE_DATA_PATH = "src/main/resources/input/noDigitHighAll.csv";

	public static boolean IS_NELL = false;

	public static boolean USE_LOGIT;

	public static boolean BOOTSTRAP;

	public static boolean RELOAD_TYPE;

	public static int SCALE_WEIGHT;

	public static boolean ENGAGE_INTER_STEP;

	public static int TOP_K_NUMERIC_PROPERTIES;

	public static boolean INCLUDE_YAGO_TYPES = true;

	public static boolean WORKFLOW_NORMAL = true;

	/**
	 * @param args
	 * @throws Exception
	 */
	public static void main(String[] args) throws Exception {

		logger.info("\n\n =========" + args[0] + "============= \n ");

		loadConfigParameters(args);
		ExperimentAutomation.BOOTSTRAP = false;

		runAll();
	}

	public static void loadConfigParameters(String[] args) {

		Properties prop = new Properties();

		try {
			ExperimentAutomation.PREDICATE = args[0];

			// load a properties file
			prop.load(new FileInputStream(args[1]));

			ExperimentAutomation.PROPGTN_FACTOR = Double.parseDouble(prop
					.getProperty("TREE_PROPAGATION_FACTOR"));
			ExperimentAutomation.TOP_K_MATCHES = Integer.parseInt(prop
					.getProperty("TOPK_ANCHORS"));
			ExperimentAutomation.DBPEDIA_SPARQL_ENDPOINT = prop
					.getProperty("DBPEDIA_SPARQL_ENDPOINT");
			ExperimentAutomation.USE_LOGIT = Boolean.valueOf(prop
					.getProperty("USE_LOGIT"));
			ExperimentAutomation.IS_NELL = Boolean.valueOf(prop
					.getProperty("IS_NELL"));

			ExperimentAutomation.INCLUDE_YAGO_TYPES = Boolean.valueOf(prop
					.getProperty("INCLUDE_YAGO_TYPES"));

			ExperimentAutomation.RELOAD_TYPE = Boolean.valueOf(prop
					.getProperty("RELOAD_TYPE"));
			ExperimentAutomation.BATCH_SIZE = Integer.parseInt(prop
					.getProperty("BATCH_SIZE"));

			ExperimentAutomation.SCALE_WEIGHT = Integer.parseInt(prop
					.getProperty("SCALE_WEIGHT"));

			ExperimentAutomation.ENGAGE_INTER_STEP = Boolean.valueOf(prop
					.getProperty("ENGAGE_INTER_STEP"));

			ExperimentAutomation.TOP_K_NUMERIC_PROPERTIES = Integer
					.parseInt(prop.getProperty("TOP_K_NUMERIC_PROPERTIES"));

			ExperimentAutomation.OIE_DATA_PATH = prop
					.getProperty("OIE_DATA_PATH");

			ExperimentAutomation.WORKFLOW_NORMAL = Boolean.valueOf(prop
					.getProperty("WORKFLOW_NORMAL"));

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
	private static void runAll() throws IOException,
			OWLOntologyCreationException, Exception {

		// inititate yago info
		if (INCLUDE_YAGO_TYPES)
			YagoDbpediaMapping.main(new String[] { "" });

		EvidenceBuilder.main(new String[] { PREDICATE });

		AutomatedNodeScoringWrapper.main(new String[] { PREDICATE });

	}
}
