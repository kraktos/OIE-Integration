/**
 * 
 */
package code.dws.reverb.clustering.analysis;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;

import code.dws.reverb.clustering.KMediodCluster;
import code.dws.reverb.clustering.MarkovClustering;

/**
 * @author adutta
 * 
 */
public class CompareClusters {

	/**
	 * 
	 */
	public CompareClusters() {
		// TODO Auto-generated constructor stub
	}

	/**
	 * @param args
	 * @throws IOException
	 */
	public static void main(String[] args) throws IOException {

		KMediodCluster.loadScores();
		KMediodCluster.ioRoutine();

		BufferedWriter writer = new BufferedWriter(new FileWriter("KCL_MCL_CL"));

		for (int p = 1; p <= 100; p++) {
			// perform a Markov Cluster
			MarkovClustering.main(new String[] { String.valueOf(p * 0.1) });

			// get the clusters in memory
			Map<String, List<String>> mCl = MarkovClustering.getAllClusters();

			for (int i = 0; i < 10; i++) {
				// perform k-mediod cluster
				KMediodCluster.doKClustering(mCl.size());
				// get the cluster in memory
				Map<String, List<String>> kmCl = KMediodCluster
						.getAllClusters();
				double compactnessMCl = getCompactness(mCl);
				double compactnessKCl = getCompactness(kmCl);
				System.out.println("MCL = " + compactnessMCl);
				System.out.println("K-Mediod = " + compactnessKCl);
				writer.write(p * 0.1 + "\t" + mCl.size() + "\t"
						+ compactnessKCl + "\t" + compactnessMCl + "\n");
			}

			writer.flush();
		}
		writer.close();
	}

	/**
	 * computes the inter cluster score for a given cluster Collection
	 * 
	 * @param clusters
	 * @return
	 */
	private static double getCompactness(Map<String, List<String>> clusters) {

		String key = null;
		List<String> cluster = null;
		double interClusterScore = 0D;

		// iterate the map
		for (Entry<String, List<String>> e : clusters.entrySet()) {
			key = e.getKey();
			cluster = e.getValue();

			// this gives a score for each cluster and for all the clusters for
			// the method

			interClusterScore = interClusterScore
					+ getInterClusterScore(cluster);

		}
		return (double) interClusterScore / clusters.size();
	}

	/**
	 * get the pairwise similarity scores for each elements in a cluster
	 * 
	 * @param cluster
	 * @return
	 */
	private static double getInterClusterScore(List<String> cluster) {

		Pair<String, String> pair = null;

		double score = 0;
		double tempScore = 0;

		for (int outer = 0; outer < cluster.size(); outer++) {
			for (int inner = outer + 1; inner < cluster.size(); inner++) {
				// create a pair
				pair = new ImmutablePair<String, String>(cluster.get(outer),
						cluster.get(inner));

				try {
					// retrieve the key from the collection
					tempScore = KMediodCluster.getScoreMap().get(pair);
				} catch (Exception e) {
					try {
						pair = new ImmutablePair<String, String>(
								cluster.get(inner), cluster.get(outer));
						tempScore = KMediodCluster.getScoreMap().get(pair);

					} catch (Exception e1) {
						e1.printStackTrace();
						tempScore = 0;
					}
				}

				if (tempScore > 1)
					System.out.println(pair.toString());

				score = score + tempScore;
			}
		}

		// System.out.println("Inter Cluster sum = " + score);
		// System.out.println("Cluster size = " + cluster.size());
		return (score == 0) ? 0 : (double) (2 * score)
				/ (cluster.size() * (cluster.size() - 1));
	}
}
