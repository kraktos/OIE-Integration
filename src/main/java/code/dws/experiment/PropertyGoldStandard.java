/**
 * 
 */
package code.dws.experiment;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.Scanner;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

	// define class logger
	public final static Logger logger = LoggerFactory
			.getLogger(PropertyGoldStandard.class);

	private static final String SAMPLE_OIE_FILE_PATH = "src/main/resources/input/sample.500.csv";
	public static int TOPK_REV_PROPS = 500;
	private static String OIE_FILE_PATH = null;
	private static Map<String, Long> counts = new HashMap<String, Long>();
	private static Map<String, Long> revbProps = null;

	private static final String HEADER = "http://dbpedia.org/resource/";

	// top-k wikipedia links
	private static final int TOP_K = 5;

	// number of gold standard facts
	private static final int SIZE = 1500;

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
		getReverbProperties(OIE_FILE_PATH, -1, 200L);

		// writing annotation file to
		BufferedWriter writer = new BufferedWriter(new FileWriter(new File(
				OIE_FILE_PATH).getParent()
				+ "/reverb.properties.distribution.out"));

		int cnt = 1;
		for (Entry<String, Long> e : counts.entrySet()) {
			logger.info(e.getKey() + "\t" + e.getValue());
			writer.write(cnt++ + "\t" + e.getValue() + "\n");
		}
		writer.flush();
		writer.close();
		logger.info("Loaded " + counts.size() + " properties");

		// read the file again to randomly select from those finally filtered
		// property
		createGoldStandard();

	}

	/**
	 * get the list of Reverb properties
	 * 
	 * CAn be used to get both top-k properties, or properties with atleast x
	 * number of instances
	 * 
	 * @param OIE_FILE
	 * @param TOPK_REV_PROPS
	 * @param atLeastInstancesCount
	 * 
	 * @return List of properties
	 */
	public static List<String> getReverbProperties(String OIE_FILE,
			int TOPK_REV_PROPS, Long atLeastInstancesCount) {

		String line = null;
		String[] arr = null;
		long val = 0;
		int c = 0;
		List<String> ret = new ArrayList<String>();

		try {
			Scanner scan = new Scanner(new File(OIE_FILE));

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
		// load the properties with atleast 500 instances each
		counts = Utilities.sortByValue(counts, atLeastInstancesCount);

		if (TOPK_REV_PROPS != -1) {
			for (Entry<String, Long> e : counts.entrySet()) {
				ret.add(e.getKey());
				c++;
				if (c == TOPK_REV_PROPS)
					return ret;
			}
		}
		return ret;
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

			ioRoutine(oieProp, topkSubjects, topkObjects, writer);

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

	/**
	 * 
	 * @throws IOException
	 */
	private static void createGoldStandard() throws IOException {
		String line = null;
		String[] arr = null;
		String oieSub = null;
		String oieProp = null;
		String oieObj = null;

		List<String> topkSubjects = null;
		List<String> topkObjects = null;
		List<String> lines = new ArrayList<String>();

		// writing annotation file to
		BufferedWriter writer = new BufferedWriter(new FileWriter(new File(
				OIE_FILE_PATH).getParent()
				+ "/gs.reverb.sample."
				+ SIZE
				+ ".csv"));

		// Reading from
		Scanner scan = new Scanner(new File(OIE_FILE_PATH));

		// init DB
		DBWrapper.init(Constants.GET_WIKI_LINKS_APRIORI_SQL);

		// select the lines from input relevant
		while (scan.hasNextLine()) {
			line = scan.nextLine();
			arr = line.split(";");
			oieProp = arr[1];

			// if this is the selected property, add it
			if (counts.containsKey(oieProp))
				lines.add(line);

		}

		// randomize the list so as to avoid one type of facts in contiguous
		// locations
		Collections.shuffle(lines);

		Random rand = new Random();

		Set<Integer> randomNumSet = new HashSet<Integer>();

		while (randomNumSet.size() < SIZE) {

			Integer randomNum = rand.nextInt(lines.size()) + 1;

			if (!randomNumSet.contains(randomNum)) {

				randomNumSet.add(randomNum);
				logger.info("Reading line " + randomNum);

				line = lines.get(randomNum);

				arr = line.split(";");
				oieSub = Utilities.clean(arr[0]);
				oieProp = arr[1];
				oieObj = Utilities.clean(arr[2]);

				// get top-k candidates of the subject
				topkSubjects = DBWrapper.fetchTopKLinksWikiPrepProb(oieSub,
						TOP_K);

				// get the topk instances for oieObj
				topkObjects = DBWrapper.fetchTopKLinksWikiPrepProb(oieObj,
						TOP_K);

				writer.write(oieSub + "\t" + oieProp + "\t" + oieObj + "\t"
						+ "?" + "\t" + "?" + "\t" + "?" + "\t" + "IP\n");

				ioRoutine(oieProp, topkSubjects, topkObjects, writer);

				writer.write("\n");
				writer.flush();
			}
		}

		randomNumSet.clear();
		counts.clear();
		writer.close();
		DBWrapper.shutDown();
	}

	/**
	 * @param oieProp
	 * @param topkSubjects
	 * @param topkObjects
	 * @param writer
	 * @throws IOException
	 */
	public static void ioRoutine(String oieProp, List<String> topkSubjects,
			List<String> topkObjects, BufferedWriter writer) throws IOException {

		if (topkSubjects.size() > 0 && topkObjects.size() > 0) {

			String candSub = null;
			String candObj = null;

			for (int j = 0; j < ((topkSubjects.size() > topkObjects.size()) ? topkSubjects
					.size() : topkObjects.size()); j++) {

				candSub = (j > topkSubjects.size() - 1) ? "-" : HEADER
						+ topkSubjects.get(j).split("\t")[0];

				candObj = (j > topkObjects.size() - 1) ? "-" : HEADER
						+ topkObjects.get(j).split("\t")[0];

				writer.write("\t" + "\t" + "\t"
						+ Utilities.utf8ToCharacter(candSub) + "\t" + "" + "\t"
						+ Utilities.utf8ToCharacter(candObj) + "\n");
			}
		}

		if (topkSubjects.size() > 0
				&& (topkObjects == null || topkObjects.size() == 0)) {
			for (String candSub : topkSubjects) {
				writer.write("\t" + "\t" + "\t" + HEADER
						+ Utilities.utf8ToCharacter(candSub.split("\t")[0])
						+ "\t" + "-" + "\t" + "-" + "\n");
			}
		}
		if ((topkSubjects == null || topkSubjects.size() == 0)
				&& topkObjects != null) {
			for (String candObj : topkObjects) {
				writer.write("\t" + "\t" + "\t" + "-" + "\t" + "-" + "\t"
						+ HEADER
						+ Utilities.utf8ToCharacter(candObj.split("\t")[0])
						+ "\n");
			}
		}
		if ((topkSubjects == null || topkSubjects.size() == 0)
				&& (topkObjects == null || topkObjects.size() == 0)) {
			writer.write("\t" + "\t" + "\t" + "-" + "\t" + "" + "\t" + "-"
					+ "\n");
		}
	}
}
