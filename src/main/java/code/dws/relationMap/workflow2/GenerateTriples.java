/**
 * 
 */
package code.dws.relationMap.workflow2;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Scanner;

import org.apache.log4j.Logger;

import code.dws.dbConnectivity.DBWrapper;
import code.dws.relationMap.GenerateNewProperties;
import code.dws.relationMap.PropertyStatisticsImproved;
import code.dws.reverb.clustering.KMediodCluster;
import code.dws.reverb.clustering.analysis.CompareClusters;
import code.dws.utils.Constants;
import code.dws.utils.FileUtil;

/**
 * Main class for workflow 2. reads the cluster info, finds mapping factors for
 * each factor and tries to generate new triples
 * 
 * @author adutta
 * 
 */
public class GenerateTriples {

	public static final String PAIRWISE_SCORES_FILE = "src/main/resources/input/All6.csv";

	private static final String DBPEDIA_CLUSTERED_FILE = "src/main/resources/input/DBPEDIA.cluster.";

	private static final String NEW_TRIPLES = "src/main/resources/input/NEW_TRIPLES_REVERB_WF2_.tsv";

	private static final String DISTRIBUTION_NEW_TRIPLES = "src/main/resources/input/NEW_TRIPLES_REVERB_DOM_RAN_WF2_.tsv";

	public static final String PATH_SEPERATOR = "\t";

	/**
	 * logger
	 */
	// define Logger
	public static Logger logger = Logger.getLogger(GenerateTriples.class
			.getName());

	/**
	 * 
	 */
	public GenerateTriples() {

	}

	/**
	 * @param args
	 * @throws IOException
	 */
	public static void main(String[] args) throws IOException {

		GenerateNewProperties.init();
		String inputLog = GenerateNewProperties.INVERSE_PROP_LOG;//GenerateNewProperties.DIRECT_PROP_LOG;

		// read through all clusters to find the best, maximum clusters
		// essentially is not best, have to compute cluster index
		Map<String, List<String>> mCl = null;

		// load the clusters in memory
		Map<String, List<String>> map = readClusters();
		// // debug
		for (Entry<String, List<String>> e : map.entrySet()) {
			logger.debug(e.getKey() + "\t" + e.getValue());
		}

		// skim through the OIE input data file and try mapping
		createNewTriples(inputLog, map);

	}

	/**
	 * fetches the best cluster and returns it
	 * 
	 * @return
	 * @throws FileNotFoundException
	 */
	@SuppressWarnings("resource")
	public static Map<String, List<String>> readClusters()
			throws FileNotFoundException {

		double mclIndex = 0;
		double bestMclIndex = Double.MAX_VALUE;

		Map<String, List<String>> bestClusterMap = new HashMap<String, List<String>>();

		String line = null;
		String[] arr = null;

		String dbpProp = null;
		List<String> dbpProps = null;

		Map<String, Double> isoMcl = null;
		Map<String, List<String>> map = null;

		Scanner scan = null;
		KMediodCluster.loadScores(PAIRWISE_SCORES_FILE, "", "\t");

		// read the cluster information file
		for (int i = 3; i <= 30; i++) {

			isoMcl = new HashMap<String, Double>();
			map = new HashMap<String, List<String>>();

			try {
				scan = new Scanner(
						new File(DBPEDIA_CLUSTERED_FILE + i + ".out"), "UTF-8");

				while (scan.hasNextLine()) {
					line = scan.nextLine();
					arr = line.split("\t");

					dbpProps = new ArrayList<String>();
					for (String elem : arr) {
						if (elem.indexOf(" ") == -1)
							dbpProps.add(elem);
					}

					if (dbpProps.size() > 0)
						for (String elem : arr) {
							if (elem.indexOf(" ") != -1) {
								map.put(elem, dbpProps);
							}
						}
					dbpProps = null;
				}
			} catch (FileNotFoundException e) {
				logger.error(e.getMessage());
			}

			mclIndex = CompareClusters.computeClusterIndex(map, isoMcl);

			if (mclIndex < bestMclIndex) {
				bestMclIndex = mclIndex;
				bestClusterMap = map;
				logger.info("best clustering at inflation = " + i);
			}
		}

		return bestClusterMap;
	}

	public static int readClusters(String s) throws FileNotFoundException {

		int bestInflation = 0;
		double mclIndex = 0;
		double bestMclIndex = Double.MAX_VALUE;

		// Map<String, List<String>> bestClusterMap = new HashMap<String,
		// List<String>>();

		String line = null;
		String[] arr = null;

		String dbpProp = null;
		List<String> dbpProps = null;

		Map<String, Double> isoMcl = null;
		Map<String, List<String>> map = null;

		Scanner scan = null;
		KMediodCluster.loadScores(PAIRWISE_SCORES_FILE, "", "\t");

		// read the cluster information file
		for (int i = 3; i <= 30; i++) {

			isoMcl = new HashMap<String, Double>();
			map = new HashMap<String, List<String>>();

			try {
				scan = new Scanner(
						new File(DBPEDIA_CLUSTERED_FILE + i + ".out"), "UTF-8");

				while (scan.hasNextLine()) {
					line = scan.nextLine();
					arr = line.split("\t");

					dbpProps = new ArrayList<String>();
					for (String elem : arr) {
						if (elem.indexOf(" ") == -1)
							dbpProps.add(elem);
					}

					if (dbpProps.size() > 0)
						for (String elem : arr) {
							if (elem.indexOf(" ") != -1) {
								map.put(elem, dbpProps);
							}
						}
					dbpProps = null;
				}
			} catch (FileNotFoundException e) {
				logger.error(e.getMessage());
			}

			mclIndex = CompareClusters.computeClusterIndex(map, isoMcl);

			if (mclIndex < bestMclIndex) {
				bestMclIndex = mclIndex;
				// bestClusterMap = map;
				bestInflation = i;
				logger.info("best clustering at inflation = " + i);
			}
		}

		return bestInflation;
	}

	/**
	 * USE THE MAPPED PROPERTY, AND MAPPED INSTANCES TO GENERATE NEW-TRIPLES
	 * FROM THE NON-MAPPED CASES
	 * 
	 * @param filePath
	 * @param clusterNames
	 * @param mappedProps
	 * @throws IOException
	 */
	private static void createNewTriples(String filePath,
			Map<String, List<String>> mappedProps) throws IOException {

		int cnt = 0;

		// oie property in concern
		String oieProp = null;

		List<String> dbProps = null;

		// write transactions to the file for analysis
		BufferedWriter triplesWriter = new BufferedWriter(new FileWriter(
				NEW_TRIPLES));
		BufferedWriter statStriplesWriter = new BufferedWriter(new FileWriter(
				DISTRIBUTION_NEW_TRIPLES));

		// read the file into memory
		ArrayList<ArrayList<String>> directPropsFile = FileUtil
				.genericFileReader(new FileInputStream(filePath),
						PATH_SEPERATOR, false);

		// build the class hierarchy
		PropertyStatisticsImproved.buildClassHierarchy();

		// init DB for getting the most frequebt URI for the NELL terms

		// MOST FREQUENT CASE
		// DBWrapper.init(Constants.GET_WIKI_LINKS_APRIORI_SQL);

		// REFINED CASE
		DBWrapper.init(Constants.GET_REFINED_MAPPINGS_SQL);

		logger.info("Generating New triples with DBPedia Clustering method...");
		// iterate through them
		for (ArrayList<String> line : directPropsFile) {
			cnt++;
			oieProp = line.get(1);

			if (line.size() == 3) { // non-mapped lines, thats where we can
									// generate something

				if (mappedProps.containsKey(oieProp)) {

					dbProps = new ArrayList<String>();
					for (String p : mappedProps.get(oieProp)) {
						dbProps.add(Constants.DBPEDIA_CONCEPT_NS + p);
					}

					PropertyStatisticsImproved.reCreateTriples(dbProps, line,
							triplesWriter, statStriplesWriter);
				}
			}
			if (cnt % 10000 == 0 && cnt > 10000)
				logger.info("Completed = " + 100
						* ((double) cnt / directPropsFile.size()) + "%");
		}

		triplesWriter.close();
		statStriplesWriter.close();
	}
}
