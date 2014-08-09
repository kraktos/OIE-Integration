/**
 * 
 */
package code.dws.reverb;

import java.io.BufferedWriter;
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

import code.dws.dbConnectivity.DBWrapper;
import code.dws.utils.Constants;
import code.dws.wordnet.SimilatityWebService;
import code.dws.wordnet.WordNetAPI;

/**
 * this class clubs together properties with similar domain, range distribution
 * 
 * @author arnab
 */
public class ReverbClusterProperty {

	// Reverb original triples file

	/**
	 * top K most frequent Reverb properties
	 */
	public static int TOPK_REV_PROPS = 500;

	private static final String DELIMIT = "\t";

	/*
	 * output location for the type pairs and the properties that are common
	 */
	private static final String CLUSTERS = "CLUSTERS_TYPE";

	private static final String CLUSTERS_NAME = "src/main/resources/input/CLUSTERS_";

	private static List<String> revbProps = null;

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

		if (args.length > 0) {
			TOPK_REV_PROPS = Integer.parseInt(args[0]);
		}
		// call DBand retrieve a set of TOPK properties
		revbProps = getReverbProperties();

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
					getWordNetSimilarityScores(outerIdx, innerIdx,
							writerWordNet);

					// based on number of common instance pairs for each
					// property
					getInstanceOverlapSimilarityScores(outerIdx, innerIdx,
							writerOverlap);

					cnt++;
//					System.out.println(cnt);

				}
				System.out.println("Completed " + (double) 200 * cnt
						/ (properties.size() * (properties.size() - 1)) + " %");

				writerOverlap.flush();
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
	 * @return List of properties
	 */
	public static List<String> getReverbProperties() {

		try {
			// init DB
			DBWrapper.init(Constants.GET_DISTINCT_REVERB_PROPERTIES);

			List<String> results = DBWrapper
					.fetchDistinctReverbProperties(TOPK_REV_PROPS);

			return (results != null) ? results : new ArrayList<String>();
		} finally {
			DBWrapper.shutDown();

		}

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

		List<ImmutablePair<String, String>> revbSubObj1 = DBWrapper
				.getReverbInstances(propArg1);
		List<ImmutablePair<String, String>> revbSubObj2 = DBWrapper
				.getReverbInstances(propArg2);

		double score = (double) CollectionUtils.intersection(revbSubObj1,
				revbSubObj2).size()
				/ (revbSubObj1.size() + revbSubObj2.size());

		writerOverlap
				.write("sameAsPropJacConf(\"" + propArg1 + "\", \"" + propArg2
						+ "\", " + Constants.formatter.format(score) + ")\n");

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
