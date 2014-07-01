/**
 * 
 */
package code.dws.reverb.mln;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.Scanner;

import code.dws.reverb.ReverbClusterProperty;

/**
 * @author arnab
 */
public class MLNCreator {

	/**
	 * weighted domain and range for the R everb properties
	 */
	private static final String WEIGHTED_ASSERTION_FILE = "/input/CLUSTERS_TYPE";
	private static final String DELIMIT = "\t";
	private static final String EVIDENCE = "reverb.type.weights";

	
	
	/**
     * 
     */
	public MLNCreator() {
		// TODO Auto-generated constructor stub
	}

	/**
	 * @param args
	 * @throws IOException
	 */
	public static void main(String[] args) throws IOException {
		createGroundTruths(WEIGHTED_ASSERTION_FILE);
	}

	private static void createGroundTruths(String weightedAssertionFile)
			throws IOException {
		Scanner scan;
		String sCurrentLine;
		String[] strArr = null;
		double weight = 0;

		String domain = null;
		String range = null;
		String property = null;
		BufferedWriter evidenceWriter = null;

		DecimalFormat formatter = new DecimalFormat("###.########");
		scan = new Scanner(
				ReverbClusterProperty.class
						.getResourceAsStream(weightedAssertionFile),
				"UTF-8");

		evidenceWriter = new BufferedWriter(new FileWriter(EVIDENCE));

		while (scan.hasNextLine()) {
			sCurrentLine = scan.nextLine();

			strArr = sCurrentLine.split(DELIMIT);
			weight = 14+Double.parseDouble(strArr[0]);

			domain = strArr[1];
			range = strArr[2];
			property = strArr[3].replaceAll(" ", "_");

			evidenceWriter.write("propAsstConf(\"" + property + "\",\"" + domain
					+ "\",\"" + range + "\"," + formatter.format(weight) + ")\n");

			evidenceWriter.flush();
		}
		System.out.println("Done writing to " + EVIDENCE);
		evidenceWriter.close();
	}

}
