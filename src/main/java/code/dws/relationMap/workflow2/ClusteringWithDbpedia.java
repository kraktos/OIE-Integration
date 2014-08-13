/**
 * 
 */
package code.dws.relationMap.workflow2;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;

import code.dws.query.SPARQLEndPointQueryAPI;
import code.dws.reverb.ReverbClusterProperty;
import code.dws.utils.Constants;
import code.dws.utils.Utilities;

import com.hp.hpl.jena.query.QuerySolution;

/**
 * This is another way of finding new triples, Here, we do not try to find what
 * DBPedia proeprty a cluster might map to, but feed the set of DBpedia property
 * along with Reverb and cluster all together.
 * 
 * 
 * @author adutta
 * 
 */
public class ClusteringWithDbpedia {

	private static final String QUERY = "select distinct ?val where {?val <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://www.w3.org/2002/07/owl#ObjectProperty>} ";
	/**
	 * logger
	 */
	// define Logger
	public static Logger logger = Logger.getLogger(ClusteringWithDbpedia.class
			.getName());

	static BufferedWriter writerDbpSims = null;
	static BufferedWriter writerRevDbpSims = null;

	/**
	 * initialize writers.
	 */
	private static void init() {
		try {
			writerDbpSims = new BufferedWriter(new FileWriter(new File(
					Constants.REVERB_DATA_PATH).getParent()
					+ "/tdbp.pairwise.sim.csv"));

			writerRevDbpSims = new BufferedWriter(new FileWriter(new File(
					Constants.REVERB_DATA_PATH).getParent()
					+ "/trvb.dbp.pairwise.sim.csv"));

		} catch (IOException e) {
			logger.error(e.getMessage());
		}

	}

	/**
	 * 
	 */
	public ClusteringWithDbpedia() {

	}

	/**
	 * @param args
	 * @throws IOException
	 */
	public static void main(String[] args) throws IOException {
		List<String> revbProps = null;
		List<String> dbpProps = null;

		init();

		logger.info("Getting top-" + ReverbClusterProperty.TOPK_REV_PROPS
				+ " properties from " + Constants.REVERB_DATA_PATH);

		// call TO RETRIEVE of TOPK reverb properties
		revbProps = ReverbClusterProperty.getReverbProperties(
				Constants.REVERB_DATA_PATH,
				ReverbClusterProperty.TOPK_REV_PROPS);
		logger.info("Loaded " + revbProps.size() + " DBpedia properties");

		// call to retrieve DBPedia owl object property
		dbpProps = loadDbpediaProperties();
		logger.info("Loaded " + dbpProps.size() + " DBpedia properties");

		logger.info("Writing sim scores to "
				+ new File(Constants.REVERB_DATA_PATH).getParent()
				+ "/tdbp.pairwise.sim.csv");
		Utilities.getPairwiseSimScore(dbpProps, dbpProps, writerDbpSims, true);

		logger.info("Writing sim scores to "
				+ new File(Constants.REVERB_DATA_PATH).getParent()
				+ "/trvb.dbp.pairwise.sim.csv");
		Utilities.getPairwiseSimScore(dbpProps, revbProps, writerRevDbpSims,
				true);

		try {
			writerDbpSims.close();
			writerRevDbpSims.close();

		} catch (IOException e) {
			logger.error(e.getMessage());
		}
	}

	/**
	 * load DBP properties from SPARQL endpoint
	 * 
	 * @return
	 */
	private static List<String> loadDbpediaProperties() {

		String prop = null;

		List<String> props = new ArrayList<String>();

		List<QuerySolution> dbpObjProps = SPARQLEndPointQueryAPI
				.queryDBPediaEndPoint(QUERY);

		for (QuerySolution querySol : dbpObjProps) {
			prop = querySol.get("val").toString();

			if ((prop.indexOf("wikiPage") == -1)
					&& (prop.toLowerCase().indexOf("thumbnail") == -1))
				props.add(prop.replaceAll(Constants.DBPEDIA_PREDICATE_NS, ""));
		}

		return props;
	}
}
