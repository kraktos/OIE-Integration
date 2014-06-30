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

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;

/**
 * this class clubs together properties with similar domain, range distribution
 * 
 * @author arnab
 */
public class ReverbClusterProperty {

	// Reverb original triples file

	private static final String PREPROCESSED_REVERBFILE = "/input/DevOutput.log";

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

	private static Map<Pair<String, String>, List<String>> MAP_CLUSTER = new HashMap<Pair<String, String>, List<String>>();

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
		String domain = null;
		String range = null;
		String property = null;

		scan = new Scanner(
				ReverbClusterProperty.class.getResourceAsStream(filePath),
				"UTF-8");

		Pair<String, String> pair = null;

		while (scan.hasNextLine()) {
			sCurrentLine = scan.nextLine();

			strArr = sCurrentLine.split(DELIMIT);
			domain = strArr[0];
			property = strArr[2];
			range = strArr[4];

			pair = new ImmutablePair<String, String>(domain.trim(),
					range.trim());

			updateMap(pair, property);

			if (MAP_CLUSTER.size() % 1000 == 0 )
				System.out.println("Processed " + MAP_CLUSTER.size());
		}

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
			for (Entry<Pair<String, String>, List<String>> e : MAP_CLUSTER
					.entrySet()) {
				outputWriter.write("[" + e.getKey().getLeft() + ","
						+ e.getKey().getRight() + "]" + "\n");
				for (String prop : e.getValue()) {
					outputWriter.write("\t" + prop + "\n");
				}
				outputWriter.write("\n");
				outputWriter.flush();
			}

		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			outputWriter.close();
		}

	}

	private static void updateMap(Pair<String, String> pair, String property) {
		List<String> values = null;
		if (MAP_CLUSTER.containsKey(pair)) {
			// get old list
			values = MAP_CLUSTER.get(pair);
		} else {
			// create new list
			values = new ArrayList<String>();
		}
		if (!values.contains(property))
			values.add(property);

		MAP_CLUSTER.put(pair, values);
	}
}
