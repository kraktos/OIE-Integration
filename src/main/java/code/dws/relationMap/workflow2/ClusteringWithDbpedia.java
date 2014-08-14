/**
 * 
 */
package code.dws.relationMap.workflow2;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

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
		logger.info("Loaded " + revbProps.size() + " Reverb properties");

		// call to retrieve DBPedia owl object property
		dbpProps = loadDbpediaProperties(100);
		logger.info("Loaded " + dbpProps.size() + " DBpedia properties");

		logger.info("Writing sim scores to "
				+ new File(Constants.REVERB_DATA_PATH).getParent()
				+ "/tdbp.pairwise.sim.csv");
		Utilities.getPairwiseSimScore(dbpProps, dbpProps, writerDbpSims, true);

		logger.info("Writing sim scores to "
				+ new File(Constants.REVERB_DATA_PATH).getParent()
				+ "/trvb.dbp.pairwise.sim.csv");
		Utilities.getPairwiseSimScore(dbpProps, revbProps, writerRevDbpSims,
				false);

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
	 * @param TOPK_REV_PROPS
	 * 
	 * @return
	 */
	private static List<String> loadDbpediaProperties(long TOPK_REV_PROPS) {

		String prop = null;
		String cnt = "0";
		int c = 0;

		List<String> retS = new ArrayList<String>();

		Map<String, Long> props = new HashMap<String, Long>();

		List<QuerySolution> count = null;

		List<QuerySolution> dbpObjProps = SPARQLEndPointQueryAPI
				.queryDBPediaEndPoint(QUERY);

		for (QuerySolution querySol : dbpObjProps) {
			prop = querySol.get("val").toString();

			if ((prop.indexOf("wikiPage") == -1)
					&& (prop.toLowerCase().indexOf("thumbnail") == -1)) {

				count = SPARQLEndPointQueryAPI
						.queryDBPediaEndPoint("select (count(*)  as ?val)  where {?a <"
								+ prop + "> ?c} ");

				for (QuerySolution sol : count) {
					cnt = sol.get("val").toString();
				}
				cnt = cnt.substring(0, cnt.indexOf("^"));
				props.put(prop.replaceAll(Constants.DBPEDIA_PREDICATE_NS, ""),
						Long.parseLong(cnt));
			}
		}

		props = Utilities.sortByValue(props);

		for (Entry<String, Long> e : props.entrySet()) {
			retS.add(e.getKey());

			c++;
			if (c == TOPK_REV_PROPS)
				return retS;
		}

		return retS;
	}
}
