/**
 * 
 */
package code.dws.experiment;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Scanner;

import com.sun.xml.internal.ws.Closeable;

import code.dws.dbConnectivity.DBWrapper;
import code.dws.utils.Constants;
import code.dws.utils.Utilities;

/**
 * A gold standard creation for property mapping
 * 
 * @author adutta
 * 
 */
public class PropertyGoldStandard {

	private static final String SAMPLE_OIE_FILE_PATH = "src/main/resources/input/sample.500.csv";
	public static int TOPK_REV_PROPS = 500;
	private static String OIE_FILE_PATH = null;
	private static Map<String, Long> counts = new HashMap<String, Long>();
	private static Map<String, Long> revbProps = null;

	private static final String HEADER = "http://dbpedia.org/resource/";

	/**
	 * 
	 */
	public PropertyGoldStandard() {

	}

	/**
	 * @param args
	 * @throws IOException
	 */
	public static void main(String[] args) throws IOException {

		if (args.length == 1)
			OIE_FILE_PATH = args[0];

		// READ THE INPUT RAW FILE AND FETCH THE TOP-K PROPERTIES
		// revbProps = loadRandomTriples();

		// USE THOSE TO FILTER SAMPLES OF TRIPLES
		// topKTriples();

		createGSFromRandomSample();

	}

	private static void createGSFromRandomSample() throws IOException {
		String line = null;
		String[] arr = null;
		String oieSub = null;
		String oieProp = null;
		String oieObj = null;

		List<String> topkSubjects = null;
		List<String> topkObjects = null;

		// writing annotation file to
		BufferedWriter writer = new BufferedWriter(new FileWriter(new File(
				OIE_FILE_PATH).getParent() + "/sample.500.Annotated.csv"));

		// Reading from
		Scanner scan = new Scanner(new File(SAMPLE_OIE_FILE_PATH));

		// init DB
		DBWrapper.init(Constants.GET_WIKI_LINKS_APRIORI_SQL);

		while (scan.hasNextLine()) {
			line = scan.nextLine();
			arr = line.split(";");
			oieSub = Utilities.clean(arr[0]);
			oieProp = arr[1];
			oieObj = Utilities.clean(arr[2]);

			// get top-k candidates of the subject
			topkSubjects = DBWrapper.fetchTopKLinksWikiPrepProb(oieSub, 5);

			// get the topk instances for oieObj
			topkObjects = DBWrapper.fetchTopKLinksWikiPrepProb(oieObj, 5);

			// ITERATE AND WRITE OUT EACH POSSIBLE PAIR

			writer.write(oieSub + "\t" + oieProp + "\t" + oieObj + "\t" + "?"
					+ "\t" + "?" + "\t" + "?" + "\n");

			if (topkSubjects.size() > 0 && topkObjects.size() > 0) {
				for (String candSub : topkSubjects) {
					for (String candObj : topkObjects) {
						writer.write("\t"
								+ "\t"
								+ "\t"
								+ HEADER
								+ Utilities.utf8ToCharacter(candSub.split("\t")[0])
								+ "\t"
								+ oieProp
								+ "\t"
								+ HEADER
								+ Utilities.utf8ToCharacter(candObj.split("\t")[0])
								+ "\n");
					}
				}
			}

			if (topkSubjects.size() > 0
					&& (topkObjects == null || topkObjects.size() == 0)) {
				for (String candSub : topkSubjects) {
					writer.write("\t" + "\t" + "\t" + HEADER
							+ Utilities.utf8ToCharacter(candSub.split("\t")[0])
							+ "\t" + oieProp + "\t" + "-" + "\n");
				}
			}

			if ((topkSubjects == null || topkSubjects.size() == 0)
					&& topkObjects != null) {
				for (String candObj : topkObjects) {
					writer.write("\t" + "\t" + "\t" + "-" + "\t" + oieProp
							+ "\t" + HEADER
							+ Utilities.utf8ToCharacter(candObj.split("\t")[0])
							+ "\n");
				}
			}

			if ((topkSubjects == null || topkSubjects.size() == 0)
					&& (topkObjects == null || topkObjects.size() == 0)) {
				writer.write("\t" + "\t" + "\t" + "-" + "\t" + oieProp + "\t"
						+ "-" + "\n");
			}

			writer.write("\n");
			writer.flush();
		}
		writer.close();
		DBWrapper.shutDown();
	}

	private static void topKTriples() throws IOException {
		String line = null;
		String[] arr = null;

		BufferedWriter writer = null;

		try {
			Scanner scan = new Scanner(new File(OIE_FILE_PATH));
			writer = new BufferedWriter(new FileWriter(
					new File(OIE_FILE_PATH).getParent() + "/topKTriples.csv"));

			while (scan.hasNextLine()) {
				line = scan.nextLine();
				arr = line.split(";");
				if (revbProps.containsKey(arr[1])) {
					writer.write(line + "\n");
					writer.flush();
				}
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
	}

	private static Map<String, Long> loadRandomTriples()
			throws FileNotFoundException {
		String line = null;
		String[] arr = null;
		long val = 0;
		int c = 0;
		Map<String, Long> ret = new HashMap<String, Long>();

		try {
			Scanner scan = new Scanner(new File(OIE_FILE_PATH));

			while (scan.hasNextLine()) {
				line = scan.nextLine();
				arr = line.split(";");

				if (counts.containsKey(arr[1])) {
					val = counts.get(arr[1]);
					val++;
				} else {
					val = 1;
				}
				counts.put(arr[1], val);

			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		counts = Utilities.sortByValue(counts);

		for (Entry<String, Long> e : counts.entrySet()) {
			ret.put(e.getKey(), e.getValue());
			c++;
			if (c == TOPK_REV_PROPS)
				return ret;
		}

		return ret;
	}
}
