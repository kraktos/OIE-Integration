/**
 * 
 */
package code.dws.reverb.mln;

import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;

import code.dws.reverb.KClustersAlgo;

import com.apporiented.algorithm.clustering.AverageLinkageStrategy;
import com.apporiented.algorithm.clustering.Cluster;
import com.apporiented.algorithm.clustering.ClusteringAlgorithm;
import com.apporiented.algorithm.clustering.CompleteLinkageStrategy;
import com.apporiented.algorithm.clustering.DefaultClusteringAlgorithm;

/**
 * @author adutta
 * 
 */
public class HierarchialCLustering {

	static List<String> props;
	static Map<Pair<String, String>, Double> map;

	static String[] names;
	static double[][] distances;

	/**
	 * @param args
	 */
	public static void main(String[] args) {

		createDataMatrix();

		Cluster cluster = performClustering();

		levelWisePrinting(cluster, 1);
	}

	private static void levelWisePrinting(Cluster c, int count) {

		for (Cluster cluster : c.getChildren()) {
			for (int i = 0; i < count; i++) {
				System.out.print("\t\t");
			}
			System.out.println(cluster.toString());
			levelWisePrinting(cluster, count + 1);

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
				new CompleteLinkageStrategy());

		return cluster;

	}

	/**
	 * creates a data matrix with a[] vs b[] values and corresponding pairwise
	 * scores
	 */
	private static void createDataMatrix() {

		KClustersAlgo.init();
		props = KClustersAlgo.getReverbProperties();
		map = KClustersAlgo.getScoreMap();

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
