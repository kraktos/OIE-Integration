/**
 * 
 */
package code.dws.reverb;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Scanner;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.log4j.Logger;

import code.dws.dbConnectivity.DBWrapper;
import code.dws.utils.Constants;
import code.dws.utils.Utilities;
import code.dws.wordnet.SimilatityWebService;

/**
 * this class clubs together properties with similar domain, range distribution
 * 
 * @author arnab
 */
public class ReverbClusterProperty {

	/**
	 * logger
	 */
	// define Logger
	public static Logger logger = Logger.getLogger(ReverbClusterProperty.class
			.getName());
	// Reverb original triples file

	/**
	 * top K most frequent Reverb properties
	 */
	public static int TOPK_REV_PROPS = -1;
	public static String OIE_FILE = "src/main/resources/input/noDigitHighAll.csv";

	private static final String DELIMIT = "\t";

	/*
	 * output location for the type pairs and the properties that are common
	 */
	private static final String CLUSTERS = "CLUSTERS_TYPE";

	private static final String CLUSTERS_NAME = "src/main/resources/input/CLUSTERS_";

	private static List<String> revbProps = null;

	private static Map<String, List<ImmutablePair<String, String>>> ALL_PROPS = new HashMap<String, List<ImmutablePair<String, String>>>();

	/**
     * 
     */
	public ReverbClusterProperty() {
		//
	}

	private static Map<Pair<String, String>, Map<String, Double>> MAP_CLUSTER = new HashMap<Pair<String, String>, Map<String, Double>>();

	/**
	 * @param args
	 * @throws IOException
	 */
	public static void main(String[] args) throws IOException {

		if (args.length == 2) {
			OIE_FILE = args[0];
			TOPK_REV_PROPS = Integer.parseInt(args[1]);
		}

		// call DBand retrieve a set of TOPK properties
		revbProps = getReverbProperties(OIE_FILE, TOPK_REV_PROPS);
		logger.info("Loaded " + revbProps.size() + " OIE properties");
		logger.info("Loaded " + ALL_PROPS.size() + " OIE properties");
		// enable scoring mechanism
		doScoring(revbProps);	

		// dumpPropCluster();

	}

	private static void doScoring(List<String> properties) throws IOException {

		int cnt = 0;

		BufferedWriter writerWordNet = new BufferedWriter(new FileWriter(
				CLUSTERS_NAME + "WORDNET_" + TOPK_REV_PROPS));

		BufferedWriter writerOverlap = new BufferedWriter(new FileWriter(
				CLUSTERS_NAME + "OVERLAP_" + TOPK_REV_PROPS));

		// init DB

		DBWrapper.init(Constants.GET_MOST_FREQUENT_SENSE);

		try {
			// iterate the list of size n, n(n-1)/2 comparison !! :D
			for (int outerIdx = 0; outerIdx < properties.size(); outerIdx++) {
				for (int innerIdx = outerIdx + 1; innerIdx < properties.size(); innerIdx++) {

					// based on Wordnet scores
					// getWordNetSimilarityScores(outerIdx, innerIdx,
					// writerWordNet);

					// based on number of common instance pairs for each
					// property
					getInstanceOverlapSimilarityScores(outerIdx, innerIdx,
							writerOverlap);

					cnt++;
					// System.out.println(cnt);
					writerOverlap.flush();

				}
				System.out.println("Completed " + (double) 200 * cnt
						/ (properties.size() * (properties.size() - 1)) + " %");

				// writerOverlap.flush();
				writerWordNet.flush();

			}
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			writerOverlap.close();
			writerWordNet.close();

			DBWrapper.shutDown();
		}

	}

	/**
	 * get the list of Reverb properties
	 * 
	 * @param OIE_FILE
	 * @param TOPK_REV_PROPS
	 * 
	 * @return List of properties
	 */
	public static List<String> getReverbProperties(String OIE_FILE,
			int TOPK_REV_PROPS) {

		Map<String, Long> counts = new HashMap<String, Long>();

		String line = null;
		String[] arr = null;
		long val = 0;
		int c = 0;
		List<String> ret = new ArrayList<String>();
		List<ImmutablePair<String, String>> list = null;

		try {
			Scanner scan = new Scanner(new File(OIE_FILE));

			while (scan.hasNextLine()) {
				line = scan.nextLine();
				arr = line.split(";");
				if (counts.containsKey(arr[1])) {
					val = counts.get(arr[1]);
					val++;
				} else {
					val = 1;
				}
				counts.put(arr[1], val);

				if (ALL_PROPS.containsKey(arr[1])) {
					list = ALL_PROPS.get(arr[1]);
				} else {
					list = new ArrayList<ImmutablePair<String, String>>();
				}
				list.add(new ImmutablePair<String, String>(arr[0], arr[2]));
				ALL_PROPS.put(arr[1], list);
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}

		if (TOPK_REV_PROPS != -1)
			counts = Utilities.sortByValue(counts);

		for (Entry<String, Long> e : counts.entrySet()) {
			ret.add(e.getKey());
			c++;
			if (TOPK_REV_PROPS != -1 && c == TOPK_REV_PROPS)
				return ret;
		}

		return ret;
		// try {
		// // init DB
		// DBWrapper.init(Constants.GET_DISTINCT_REVERB_PROPERTIES);
		//
		// List<String> results = DBWrapper
		// .fetchDistinctReverbProperties(TOPK_REV_PROPS);
		//
		// return (results != null) ? results : new ArrayList<String>();
		// } finally {
		// DBWrapper.shutDown();
		//
		// }

	}

	/**
	 * method to compute properties sharing reverb instances
	 * 
	 * @param id2
	 * @param id
	 * 
	 * @param writerOverlap
	 * @return
	 * @throws IOException
	 */
	private static void getInstanceOverlapSimilarityScores(int outerIdx,
			int innerIdx, BufferedWriter writerOverlap) throws IOException {

		String propArg1 = revbProps.get(outerIdx);
		String propArg2 = revbProps.get(innerIdx);

		// List<ImmutablePair<String, String>> revbSubObj1 =
		// DBWrapper.getReverbInstances(propArg1);
		// List<ImmutablePair<String, String>> revbSubObj2 =
		// DBWrapper.getReverbInstances(propArg2);

		List<ImmutablePair<String, String>> revbSubObj1 = ALL_PROPS
				.get(propArg1);
		List<ImmutablePair<String, String>> revbSubObj2 = ALL_PROPS
				.get(propArg2);

		double scoreJaccard = (double) CollectionUtils.intersection(
				revbSubObj1, revbSubObj2).size()
				/ (revbSubObj1.size() + revbSubObj2.size());

		double scoreOverlap = (double) CollectionUtils.intersection(
				revbSubObj1, revbSubObj2).size()
				/ Math.min(revbSubObj1.size(), revbSubObj2.size());

		// writerOverlap.write("sameAsPropJacConf(\"" + propArg1 + "\", \""
		// + propArg2 + "\", " + Constants.formatter.format(scoreOverlap)
		// + ")\n");

		if (scoreOverlap > 0.002)
			writerOverlap.write(propArg1 + "\t" + propArg2 + "\t"
					+ Constants.formatter.format(scoreOverlap) + "\n");

	}

	/**
	 * call the web service to compute the inter phrase similarity
	 * 
	 * @param properties
	 * 
	 * @param properties
	 * @param id2
	 * @param id
	 * @throws Exception
	 */
	private static void getWordNetSimilarityScores(int id, int id2,
			BufferedWriter writerWordNet) throws Exception {

		// outputWriter.write(results.get(id) + "\t" +
		// results.get(id2) + " ==> "
		// + WordNetAPI.scoreWordNet(results.get(id).split(" "),
		// results.get(id2).split(" ")) + "\n");

		String propArg1 = revbProps.get(id);
		String propArg2 = revbProps.get(id2);

		double score = SimilatityWebService.getSimScore(revbProps.get(id),
				revbProps.get(id2));

		writerWordNet
				.write("sameAsPropWNConf(\"" + propArg1 + "\", \"" + propArg2
						+ "\", " + Constants.formatter.format(score) + ")\n");

	}

	/**
	 * write out the clusters to a file
	 * 
	 * @throws IOException
	 */
	private static void dumpPropCluster() throws IOException {
		BufferedWriter outputWriter = null;
		try {
			outputWriter = new BufferedWriter(new FileWriter(CLUSTERS));
			for (Entry<Pair<String, String>, Map<String, Double>> e : MAP_CLUSTER
					.entrySet()) {

				for (Entry<String, Double> propEntry : e.getValue().entrySet()) {

					outputWriter.write(propEntry.getValue() + "\t"
							+ e.getKey().getLeft() + "\t"
							+ e.getKey().getRight() + "\t" + propEntry.getKey()
							+ "\n");
				}
				outputWriter.flush();
			}

		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			outputWriter.close();
		}

	}

}
