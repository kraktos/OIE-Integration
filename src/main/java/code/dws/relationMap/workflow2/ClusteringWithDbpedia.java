/**
 * 
 */
package code.dws.relationMap.workflow2;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Map.Entry;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.log4j.Logger;

import code.dws.experiment.PropertyGoldStandard;
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
	// static BufferedWriter writerDbpSims = null;
	static BufferedWriter writerRevDbpSims = null;
	static BufferedWriter writerRevRevSims = null;

	static int k = -1; // ReverbClusterProperty.TOPK_REV_PROPS;

	/**
	 * initialize writers.
	 */
	private static void init() {
		try {
			// writerDbpSims = new BufferedWriter(new FileWriter(new File(
			// Constants.REVERB_DATA_PATH).getParent()
			// + "/tdbp."
			// + k
			// + ".pairwise.sim.csv"));

			writerRevRevSims = new BufferedWriter(new FileWriter(new File(
					Constants.REVERB_DATA_PATH).getParent()
					+ "/trvb."
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
		revbProps = getReverbProperties(Constants.REVERB_DATA_PATH, k, 10L);

		logger.info("Loaded " + revbProps.size() + " Reverb properties");

		// call to retrieve DBPedia owl object property
		dbpProps = loadDbpediaProperties(k);
		logger.info("Loaded " + dbpProps.size() + " DBpedia properties");
		for (String prop : dbpProps) {
			writerDbpProps.write(prop + "\n");
		}
		writerDbpProps.flush();

		ExecutorService executor = Executors.newFixedThreadPool(3);

		logger.info("Writing sim scores to "
				+ new File(Constants.REVERB_DATA_PATH).getParent() + "/trvb."
				+ k + ".pairwise.sim.csv");
		executor.submit(new Worker(revbProps, revbProps, writerRevRevSims, true));

		logger.info("Writing sim scores to "
				+ new File(Constants.REVERB_DATA_PATH).getParent()
				+ "/trvb.dbp." + k + ".pairwise.sim.csv");
		executor.submit(new Worker(dbpProps, revbProps, writerRevDbpSims, false));

		executor.shutdown();
		while (!executor.isTerminated()) {
		}

	}

	/**
	 * get the list of Reverb properties
	 * 
	 * CAn be used to get both top-k properties, or properties with atleast x
	 * number of instances
	 * 
	 * @param OIE_FILE
	 * @param TOPK_REV_PROPS
	 * @param atLeastInstancesCount
	 * 
	 * @return List of properties
	 */
	public static List<String> getReverbProperties(String OIE_FILE,
			int TOPK_REV_PROPS, Long atLeastInstancesCount) {

		String line = null;
		String[] arr = null;
		long val = 0;
		int c = 0;
		List<String> ret = new ArrayList<String>();
		Map<String, Long> COUNT_PROPERTY_INST = new HashMap<String, Long>();

		try {
			Scanner scan = new Scanner(new File(OIE_FILE));

			while (scan.hasNextLine()) {
				line = scan.nextLine();
				arr = line.split(";");
				if (COUNT_PROPERTY_INST.containsKey(arr[1])) {
					val = COUNT_PROPERTY_INST.get(arr[1]);
					val++;
				} else {
					val = 1;
				}
				COUNT_PROPERTY_INST.put(arr[1], val);
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		// load the properties with atleast 500 instances each
		COUNT_PROPERTY_INST = Utilities.sortByValue(COUNT_PROPERTY_INST,
				atLeastInstancesCount);

		for (Entry<String, Long> e : COUNT_PROPERTY_INST.entrySet())
			ret.add(e.getKey());

		return ret;
	}

	/**
	 * load DBP properties from SPARQL endpoint, -1 means all properties
	 * 
	 * @param topKDBPediaProperties
	 * 
	 * @return
	 */
	public static List<String> loadDbpediaProperties(long topKDBPediaProperties) {

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
