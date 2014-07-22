/**
 * 
 */
package code.dws.reverb.clustering;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Scanner;

/**
 * define a transition matrix of all the connected nodes with edge weights
 * perform a markov chain simulation of the state jumps till convergence
 * 
 * @author adutta
 * 
 */
public class MarkovClustering {

	/**
	 * cluster collection
	 */
	static Map<String, List<String>> CLUSTER = new HashMap<String, List<String>>();

	/**
	 * mcl output file
	 */
	private static final String OUTPUT = "/home/adutta/git/OIE-Integration/src/main/resources/input/mcl.output";

	/**
	 * 
	 */
	public MarkovClustering() {
		// TODO Auto-generated constructor stub
	}

	/**
	 * @return the cLUSTER
	 */
	public static Map<String, List<String>> getAllClusters() {
		return CLUSTER;
	}

	/**
	 * @param args
	 * @throws IOException
	 */
	public static void main(String[] args) throws IOException {

		String inflation = null;
		if (args.length > 0)
			inflation = args[0];
		else
			inflation = "1";

		// make mcl call to perform clustering
		systemRoutine(inflation);

		// read the output to load in memory
		readMarkovClusters();

	}

	@SuppressWarnings("resource")
	private static void readMarkovClusters() throws FileNotFoundException {

		Scanner scan;
		scan = new Scanner(new File((OUTPUT)), "UTF-8");

		List<String> list = null;

		int cnt = 1;
		String sCurrentLine = null;
		String[] elem = null;

		while (scan.hasNextLine()) {
			list = new ArrayList<String>();
			sCurrentLine = scan.nextLine();
			elem = sCurrentLine.split("\t");
			for (String s : elem)
				list.add(s);

			CLUSTER.put("C" + cnt++, list);
		}

		System.out.println("Loaded " + CLUSTER.size() + " markov clusters...");
	}

	private static void systemRoutine(String inflation) {
		Runtime r = Runtime.getRuntime();

		System.out.println("Running Markov clustering...");
		try {
			Process p = r
					.exec("/home/adutta/Work/mcl/mcl-14-137/bin/mcl "
							+ "/home/adutta/git/OIE-Integration/COMBINED_SCORE.tsv --abc -I "
							+ inflation + " -o " + OUTPUT);

			BufferedReader bufferedreader = new BufferedReader(
					new InputStreamReader(new BufferedInputStream(
							p.getInputStream())));

			String line;
			while ((line = bufferedreader.readLine()) != null) {
				System.out.println(line);
			}

			try {
				if (p.waitFor() != 0)
					System.err.println("exit value = " + p.exitValue());
			} catch (Exception e) {
				e.printStackTrace();
			} finally {
				bufferedreader.close();
				System.out.println("Done...");
			}

		} catch (Exception e) {
			System.err.println(e.getMessage());
		}

	}
}
