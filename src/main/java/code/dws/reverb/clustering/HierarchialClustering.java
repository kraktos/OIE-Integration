/**
 * 
 */
package code.dws.reverb.clustering;

import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;

import code.dws.reverb.clustering.KMediodCluster;

import com.apporiented.algorithm.clustering.AverageLinkageStrategy;
import com.apporiented.algorithm.clustering.Cluster;
import com.apporiented.algorithm.clustering.ClusteringAlgorithm;
import com.apporiented.algorithm.clustering.DefaultClusteringAlgorithm;

/**
 * @author adutta
 * 
 */
public class HierarchialClustering {

	static List<String> props;
	static Map<Pair<String, String>, Double> map;

	static String[] names;
	static double[][] distances;

	/**
	 * @param args
	 */
	public static void main(String[] args) {

		createDataMatrix();

		// for (int outer = 0; outer < names.length; outer++) {
		// for (int inner = 0; inner < names.length; inner++) {
		// System.out.print(distances[outer][inner] + "\t\t");
		// }
		// System.out.println();
		// }

		Cluster cluster = performClustering();

		levelWisePrinting(cluster, 1);
	}

	private static void levelWisePrinting(Cluster c, int count) {

		for (Cluster cluster : c.getChildren()) {
			// for (int i = 0; i < count; i++) {
			// System.out.print("\t\t");
			// }
			// levelWisePrinting(cluster, count + 1);

			System.out.println(cluster.toString());
			for (Cluster c1 : cluster.getChildren()) {
				System.out.println("\t" + c1.toString());
				for (Cluster c2 : c1.getChildren()) {
					System.out.println("\t\t" + c2.toString());
					for (Cluster c3 : c2.getChildren()) {
						System.out.println("\t\t\t" + c3.toString());
						for (Cluster c4 : c3.getChildren()) {
							System.out.println("\t\t\t\t" + c4.toString());
							for (Cluster c5 : c4.getChildren()) {
								System.out
										.println("\t\t\t\t\t" + c5.toString());
								for (Cluster c6 : c5.getChildren()) {
									System.out.println("\t\t\t\t\t\t"
											+ c6.toString());
								}
							}
						}
					}
				}
			}
		}
	}

	/**
	 * perform the clustering
	 * 
	 * @return
	 * 
	 */
	public static Cluster performClustering() {

		System.out.println("Clustering please wait...");
		ClusteringAlgorithm alg = new DefaultClusteringAlgorithm();
		Cluster cluster = alg.performClustering(distances, names,
				new AverageLinkageStrategy());

		return cluster;

	}

	/**
	 * creates a data matrix with a[] vs b[] values and corresponding pairwise
	 * scores
	 */
	private static void createDataMatrix() {

		KMediodCluster.init();
		props = KMediodCluster.getReverbProperties();
		map = KMediodCluster.getScoreMap();

		names = new String[props.size()];

		for (int i = 0; i < props.size(); i++) {
			names[i] = props.get(i);
			// System.out.println(names[i]);
		}

		distances = new double[names.length][names.length];

		for (int outer = 0; outer < names.length; outer++) {
			distances[outer][outer] = 0;
			for (int inner = outer + 1; inner < names.length; inner++) {
				try {
					Pair p = new ImmutablePair<String, String>(names[outer],
							names[inner]);

					distances[outer][inner] = map.get(p);
					distances[inner][outer] = distances[outer][inner];
				} catch (Exception e) {
					e.printStackTrace();
					System.out.println(" gar bar...");
				}
			}
		}

	}
}
