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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.log4j.Logger;

import code.dws.query.SPARQLEndPointQueryAPI;
import code.dws.reverb.ReverbClusterProperty;
import code.dws.utils.Constants;
import code.dws.utils.Utilities;
import code.dws.utils.Worker;

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

	static BufferedWriter writerDbpProps = null;
	static BufferedWriter writerDbpSims = null;
	static BufferedWriter writerRevDbpSims = null;

	static int k = -1; // ReverbClusterProperty.TOPK_REV_PROPS;

	/**
	 * initialize writers.
	 */
	private static void init() {
		try {
			writerDbpSims = new BufferedWriter(new FileWriter(new File(
					Constants.REVERB_DATA_PATH).getParent()
					+ "/tdbp."
					+ k
					+ ".pairwise.sim.csv"));

			writerRevDbpSims = new BufferedWriter(new FileWriter(new File(
					Constants.REVERB_DATA_PATH).getParent()
					+ "/trvb.dbp."
					+ k
					+ ".pairwise.sim.csv"));

			writerDbpProps = new BufferedWriter(new FileWriter(new File(
					Constants.REVERB_DATA_PATH).getParent()
					+ "/dbp."
					+ k
					+ ".object.properties.csv"));

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

		init();

		List<String> revbProps = null;
		List<String> dbpProps = null;

		logger.info("Getting top-" + k + " properties from "
				+ Constants.REVERB_DATA_PATH);
		// call TO RETRIEVE of TOPK reverb properties
		revbProps = ReverbClusterProperty.getReverbProperties(
				Constants.REVERB_DATA_PATH, k);
		logger.info("Loaded " + revbProps.size() + " Reverb properties");

		// call to retrieve DBPedia owl object property
		dbpProps = loadDbpediaProperties(k);
		logger.info("Loaded " + dbpProps.size() + " DBpedia properties");
		for (String prop : dbpProps) {
			writerDbpProps.write(prop + "\n");
		}
		writerDbpProps.flush();

		ExecutorService executor = Executors.newFixedThreadPool(2);

		logger.info("Writing sim scores to "
				+ new File(Constants.REVERB_DATA_PATH).getParent() + "/tdbp."
				+ k + ".pairwise.sim.csv");
		executor.execute(new Worker(dbpProps, dbpProps, writerDbpSims, true));

		logger.info("Writing sim scores to "
				+ new File(Constants.REVERB_DATA_PATH).getParent()
				+ "/trvb.dbp." + k + ".pairwise.sim.csv");
		executor.execute(new Worker(dbpProps, revbProps, writerRevDbpSims,
				false));

		executor.shutdown();
		while (!executor.isTerminated()) {
		}

		// Utilities.getPairwiseSimScore(dbpProps, dbpProps, writerDbpSims,
		// true);

		// Utilities.getPairwiseSimScore(dbpProps, revbProps, writerRevDbpSims,
		// false);

		// try {
		// writerDbpSims.close();
		// writerRevDbpSims.close();
		// writerDbpProps.close();
		//
		// } catch (IOException e) {
		// logger.error(e.getMessage());
		// }
	}

	/**
	 * load DBP properties from SPARQL endpoint, -1 means all properties
	 * 
	 * @param topKDBPediaProperties
	 * 
	 * @return
	 */
	private static List<String> loadDbpediaProperties(long topKDBPediaProperties) {

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

			if ((prop.indexOf(Constants.DBPEDIA_PREDICATE_NS) != -1)
					&& (prop.indexOf("wikiPageWikiLink") == -1)
					&& (prop.indexOf("wikiPageExternalLink") == -1)
					&& (prop.indexOf("wikiPageRedirects") == -1)
					&& (prop.indexOf("thumbnail") == -1)
					&& (prop.indexOf("wikiPageDisambiguates") == -1)
					&& (prop.indexOf("wikiPageInterLanguageLink") == -1)) {

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

		// sort only when interested in top-k, else makes no sense
		if (topKDBPediaProperties != -1)
			props = Utilities.sortByValue(props);

		for (Entry<String, Long> e : props.entrySet()) {
			retS.add(e.getKey());

			c++;
			if (c == topKDBPediaProperties)
				return retS;
		}

		return retS;
	}
}
