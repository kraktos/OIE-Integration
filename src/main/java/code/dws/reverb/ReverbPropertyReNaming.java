/**
 * 
 */
package code.dws.reverb;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

import code.dws.reverb.clustering.MarkovClustering;
import code.dws.reverb.clustering.analysis.CompareClusters;

/**
 * In analysis, we figure out the optimal cluster size..
 * 
 * @author adutta
 * 
 */
public class ReverbPropertyReNaming {

	/**
	 * the mega collection for reverb, holding the mapping from new propery name
	 * to the list of actual properties it represents
	 */
	private static Map<String, List<String>> CLUSTERED_REVERB_PROPERTIES = new HashMap<String, List<String>>();

	/**
	 * 
	 */
	public ReverbPropertyReNaming() {

	}

	/**
	 * @param args
	 * @throws IOException
	 */
	public static void main(String[] args) throws IOException {

		double optimalInflation = getOptimalInflation();

		// use the inflation factor to regenerate the clusters
		// perform a Markov Cluster
		MarkovClustering
				.main(new String[] { String.valueOf(optimalInflation) });

		// get the clusters in memory
		CLUSTERED_REVERB_PROPERTIES = MarkovClustering.getAllClusters();

	}

	/**
	 * @return the mCl
	 */
	public static Map<String, List<String>> getReNamedProperties() {
		return CLUSTERED_REVERB_PROPERTIES;
	}

	/**
	 * read the file for different inflation parameter and use it to find the
	 * inflation giving max number of clusters
	 * 
	 * @return
	 * @throws FileNotFoundException
	 */
	@SuppressWarnings("resource")
	private static double getOptimalInflation() throws FileNotFoundException {
		Scanner scan;
		String[] elem = null;
		String sCurrentLine = null;
		double inflation = 30;
		double tInfl = 0;
		int clusterSize = 0;

		double minMclIndex = Integer.MAX_VALUE;
		double tIndex = 0;

		scan = new Scanner(new File((CompareClusters.CLUSTER_INDICES)), "UTF-8");
		scan.nextLine();
		while (scan.hasNextLine()) {
			sCurrentLine = scan.nextLine();
			elem = sCurrentLine.split("\t");
			tIndex = Double.parseDouble(elem[3]);
			if (tIndex <= minMclIndex) {
				minMclIndex = tIndex;
				clusterSize = Integer.parseInt(elem[1]);
				// System.out.println(minMclIndex + "\t" + clusterSize);
			}
		}

		// System.out.println(clusterSize);

		scan = new Scanner(new File((CompareClusters.CLUSTER_INDICES)), "UTF-8");
		scan.nextLine();
		while (scan.hasNextLine()) {
			sCurrentLine = scan.nextLine();
			elem = sCurrentLine.split("\t");

			if (elem[1].equals(String.valueOf(clusterSize))) {
				tInfl = Double.parseDouble(elem[0]);
				inflation = (tInfl < inflation) ? tInfl : inflation;
			}
		}

		return inflation;
	}

}
