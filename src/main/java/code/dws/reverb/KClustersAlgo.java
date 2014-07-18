/**
 * 
 */
package code.dws.reverb;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Scanner;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;

import code.dws.utils.Constants;

/**
 * define K seed of random properties and start clustertinga
 * 
 * @author adutta
 * 
 */
public class KClustersAlgo {

	private static final String WORDNET_SCORES = "/input/CLUSTERS_WORDNET_1000";
	private static final String JACCARD_SCORES = "/input/CLUSTERS_OVERLAP_1000";
	private static final int TOPK_REVERB_PROPERTIES = 1000;

	private static final String ALL_SCORES = "COMBINED_SCORE.tsv";

	private static int SEED = (int) (0.2 * TOPK_REVERB_PROPERTIES);

	private static List<String> reverbProperties = null;
	private static List<String> seedReverbProperties = null;

	private static Map<Pair<String, String>, Double> SCORE_MAP = new HashMap<Pair<String, String>, Double>();

	// cluster placeholder
	private static Map<String, List<String>> K_CLUSTER_MAP = new HashMap<String, List<String>>();

	/**
	 * 
	 */
	public static void init() {
		// feed seedc count and generate K-random cluster points
		seedReverbProperties = generateKRandomSeed();

		// load the scores in memeory
		loadScores(WORDNET_SCORES, "sameAsPropWNConf");
		loadScores(JACCARD_SCORES, "sameAsPropJacConf");
	}

	/**
	 * @return the reverbProperties
	 */
	public static List<String> getReverbProperties() {
		return reverbProperties;
	}

	/**
	 * @param args
	 * @throws IOException
	 */
	public static void main(String[] args) throws IOException {

		if (args.length > 0)
			SEED = Integer.parseInt(args[0]);

		init();

		ioRoutine();
		System.out.println("Dumped combined scores to " + ALL_SCORES);

		// perform clustering with the K-seed properties
		doKClustering();

		// write out the clusters
		printCluster();
	}

	/**
	 * a sub routine to dump the values of the pair wise scores to a file.
	 * required for clustering
	 * 
	 * @throws IOException
	 */
	private static void ioRoutine() throws IOException {
		BufferedWriter ioWriter = new BufferedWriter(new FileWriter(ALL_SCORES));
		for (Entry<Pair<String, String>, Double> e : SCORE_MAP.entrySet()) {
			ioWriter.write(e.getKey().getLeft() + "\t" + e.getKey().getRight()
					+ "\t" + Constants.formatter.format(e.getValue()) + "\n");
		}

		ioWriter.flush();
		ioWriter.close();
	}

	/**
	 * @return the sCORE_MAP
	 */
	public static Map<Pair<String, String>, Double> getScoreMap() {
		return SCORE_MAP;
	}

	/**
	 * print out all the clusters
	 */
	private static void printCluster() {

		int k = 1;

		for (Entry<String, List<String>> e : K_CLUSTER_MAP.entrySet()) {
			System.out.println(" \n************  Cluster " + k++
					+ " **************** ");
			System.out.println("\t" + e.getKey());
			for (String val : e.getValue()) {
				System.out.println("\t" + val);
			}
		}
	}

	/**
	 * load in memory all the scores, Wordnet similarity and jaccard ones
	 * 
	 * @param file
	 * @param arg
	 */
	@SuppressWarnings("resource")
	private static void loadScores(String file, String arg) {

		String sCurrentLine;
		double score;

		Scanner scan;
		scan = new Scanner(ReverbPreProcessing.class.getResourceAsStream(file),
				"UTF-8");

		Pair<String, String> pair = null;

		while (scan.hasNextLine()) {
			sCurrentLine = scan.nextLine();
			sCurrentLine = sCurrentLine.replaceAll("" + arg + "\\(", "")
					.replaceAll("\\)", "").replaceAll("\"", "");

			pair = new ImmutablePair<String, String>(
					sCurrentLine.split(", ")[0].trim(),
					sCurrentLine.split(", ")[1].trim());

			if (SCORE_MAP.containsKey(pair)) {
				score = (double) (SCORE_MAP.get(pair) + Double
						.valueOf(sCurrentLine.split(", ")[2])) / 2;

			} else {
				score = Double.valueOf(sCurrentLine.split(", ")[2]);
			}

			SCORE_MAP.put(pair, score);

			// System.out.println(sCurrentLine);
		}
	}

	/**
	 * actual clustering algo
	 * 
	 * @param seedReverbProperties
	 */
	private static void doKClustering() {

		double bestScore;
		String bestSeedProp = null;

		double score = 0;

		// iterate the full list of properties
		for (String reverbProp : reverbProperties) {
			bestScore = 0;
			// compare with each of the seed points
			for (String seedProp : seedReverbProperties) {
				if (!reverbProp.equals(seedProp)) {

					try {
						score = SCORE_MAP
								.get(new ImmutablePair<String, String>(
										reverbProp, seedProp));
					} catch (NullPointerException e) {
						try {
							score = SCORE_MAP
									.get(new ImmutablePair<String, String>(
											seedProp, reverbProp));
						} catch (Exception e1) {
							System.out.println("problem with " + seedProp
									+ ", " + reverbProp);
						}
					}

					// filter the top score
					if (bestScore <= score) {
						bestScore = score;
						bestSeedProp = seedProp;
					}
				}
			}

			// at this point, one prop is checked against all seed
			// properties..so
			// we can place this in the cluster collection
			putInCluster(bestSeedProp, reverbProp);
		}

	}

	/**
	 * once a property is checked with its closest seed property, place it in
	 * that cluster
	 * 
	 * @param keySeedProp
	 * @param valueProperty
	 */
	private static void putInCluster(String keySeedProp, String valueProperty) {

		List<String> exixtingClusterMemebers = K_CLUSTER_MAP.get(keySeedProp);
		try {
			exixtingClusterMemebers.add(valueProperty);
		} catch (Exception e) {
			System.out.println("Problem while adding for the cluster "
					+ keySeedProp);
		}

		K_CLUSTER_MAP.put(keySeedProp, exixtingClusterMemebers);
	}

	/**
	 * populae full list of properties
	 * 
	 * @param seed2
	 * @return
	 */
	private static List<String> generateKRandomSeed() {

		ReverbClusterProperty.TOPK_REV_PROPS = TOPK_REVERB_PROPERTIES;
		// call DBand retrieve a set of TOPK properties
		reverbProperties = ReverbClusterProperty.getReverbProperties();

		List<String> temp = getRandomProps((SEED < TOPK_REVERB_PROPERTIES) ? SEED
				: TOPK_REVERB_PROPERTIES);

		for (String p : temp) {
			K_CLUSTER_MAP.put(p, new ArrayList<String>());
		}
		return temp;

	}

	/**
	 * random K properties
	 * 
	 * @param revbProps
	 * @param seedK
	 * @return
	 */
	private static List<String> getRandomProps(int seedK) {

		List<String> temp = new LinkedList<String>(reverbProperties);
		Collections.shuffle(temp);
		List<String> seeedList = temp.subList(0, seedK);
		// for (String k : seeedList) {
		// reverbProperties.remove(k);
		// }
		return seeedList;
	}

}
