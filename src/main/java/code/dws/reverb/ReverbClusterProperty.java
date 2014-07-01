/**
 * 
 */
package code.dws.reverb;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Scanner;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;

/**
 * this class clubs together properties with similar domain, range distribution
 * 
 * @author arnab
 */
public class ReverbClusterProperty {

	// Reverb original triples file

	private static final String PREPROCESSED_REVERBFILE = "/input/OUTPUT_WEIGHTED.log";

	private static final String DELIMIT = "\t";

	/*
	 * output location for the type pairs and the properties that are common
	 */
	private static final String CLUSTERS = "CLUSTERS_TYPE";

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
		readProcessedFile(PREPROCESSED_REVERBFILE);

		dumpPropCluster();

	}

	private static void readProcessedFile(String filePath) {
		Scanner scan;
		String sCurrentLine;
		String[] strArr = null;
		double prob = 0;
		double weight = 0;

		String domain = null;
		String range = null;
		String property = null;

		scan = new Scanner(
				ReverbClusterProperty.class.getResourceAsStream(filePath),
				"UTF-8");

		Pair<String, String> pairConcepts = null;

		while (scan.hasNextLine()) {
			sCurrentLine = scan.nextLine();

			strArr = sCurrentLine.split(DELIMIT);
			prob = Double.parseDouble(strArr[0]);
			weight = Double.parseDouble(strArr[1]);

			domain = strArr[2];
			property = strArr[4];
			range = strArr[6];

			pairConcepts = new ImmutablePair<String, String>(domain.trim(),
					range.trim());

			// put all the weighted types for each properties in memory
			updateMap(pairConcepts, property, weight);

			if (MAP_CLUSTER.size() % 1000 == 0)
				System.out.println("Processed " + MAP_CLUSTER.size());
		}

	}

	private static void updateMap(Pair<String, String> pair, String property,
			double weight) {

		Map<String, Double> values = null;

		if (MAP_CLUSTER.containsKey(pair)) {
			// get old collection
			values = MAP_CLUSTER.get(pair);
		} else {
			// create new collection
			values = new HashMap<String, Double>();
		}

		if (!values.containsKey(property)) {
			values.put(property, weight);
		} else {
			if (values.get(property) < weight) {
				// update the property with a better score
				values.put(property, weight);
			}
		}

		MAP_CLUSTER.put(pair, values);

	}

	/**
	 * write out the clusters
	 * 
	 * @throws IOException
	 */
	private static void dumpPropCluster() throws IOException {
		BufferedWriter outputWriter = null;
		try {
			outputWriter = new BufferedWriter(new FileWriter(CLUSTERS));
			for (Entry<Pair<String, String>, Map<String, Double>> e : MAP_CLUSTER
					.entrySet()) {

				// outputWriter.write("[" + e.getKey().getLeft() + ","
				// + e.getKey().getRight() + "]" + "\n");

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
