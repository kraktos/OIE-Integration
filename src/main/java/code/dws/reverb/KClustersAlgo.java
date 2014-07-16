/**
 * 
 */
package code.dws.reverb;

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

/**
 * define K seed of random properties and start clustertinga
 * 
 * @author adutta
 * 
 */
public class KClustersAlgo {

	private static final String WORDNET_SCORES = "/input/CLUSTERS_WORDNET_500";
	private static final String JACCARD_SCORES = "/input/CLUSTERS_OVERLAP_500";
	private static final int TOPK_REVERB_PROPERTIES = 100;

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
		loadScores(WORDNET_SCORES);
		loadScores(JACCARD_SCORES);
	}

	/**
	 * @return the reverbProperties
	 */
	public static List<String> getReverbProperties() {
		return reverbProperties;
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {

		if (args.length > 0)
			SEED = Integer.parseInt(args[0]);

		init();

		// perform clustering with the K-seed properties
		doKClustering();

		// write out the clusters
		printCluster();
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
	 */
	@SuppressWarnings("resource")
	private static void loadScores(String file) {

		String sCurrentLine;
		double score;

		Scanner scan;
		scan = new Scanner(ReverbPreProcessing.class.getResourceAsStream(file),
				"UTF-8");

		Pair<String, String> pair = null;

		while (scan.hasNextLine()) {
			sCurrentLine = scan.nextLine();
			sCurrentLine = sCurrentLine.replaceAll("sameAsPropWNConf\\(", "")
					.replaceAll("\\)", "").replaceAll("\"", "");

			pair = new ImmutablePair<String, String>(
					sCurrentLine.split(", ")[0].trim(),
					sCurrentLine.split(", ")[1].trim());

			if (SCORE_MAP.containsKey(pair)) {
				score = SCORE_MAP.get(pair)
						+ Double.valueOf(sCurrentLine.split(", ")[2]);

			} else {
				score = Double.valueOf(sCurrentLine.split(", ")[2]);
			}
			SCORE_MAP.put(pair, score - 0.5);
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
