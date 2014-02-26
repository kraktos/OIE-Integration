/**
 * 
 */
package code.dws.relationMap;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import code.dws.utils.Constants;
import code.dws.utils.FileUtil;
import code.dws.utils.MapUtils;

/**
 * Responsible for computing the property statistics
 * 
 * @author adutta
 */
public class PropertyStatistics {

	// define class logger
	public final static Logger log = LoggerFactory
			.getLogger(PropertyStatistics.class);

	// path seperator for the output property files
	private static final String PATH_SEPERATOR = "\t";

	// threshold to consider mappable predicates. It means consider NELL
	// predicates
	// which are atleast x % map-able
	private static final double OIE_PROPERTY_MAPPED_THRESHOLD = 0;

	// use it to set a threshold on the mapped DBpedia property mapping
	private static double DBP_PROPERTY_CONFIDENCE_THRESHOLD = 0D;

	// use it to put threshold on the ratio of map/non-map
	private static final double DROP_THRESHOLD = 4;

	private static final String PROP_STATS = "PROP_STATISTICS_TOP5.tsv"; // "PROP_STATISTICS.tsv";

	// total triples that can be reconstructed
	private static int newTriples = 0;

	// map to hold the nell properties and the equivalent DBpedia properties
	// key is the nell property, value is a map with the dbp property and the
	// corresponding count
	private static Map<String, Map<String, Integer>> MAP_OIE_IE_PROP_COUNTS = new HashMap<String, Map<String, Integer>>();

	// map keeping count of the nell predicate occurrence, should be identical
	// to
	// the number of triples with that
	// property in the raw input file
	private static Map<String, Integer> MAP_PRED_COUNT = new HashMap<String, Integer>();

	public static void main(String[] args) throws IOException {
		if (args.length == 0) {
			DBP_PROPERTY_CONFIDENCE_THRESHOLD = 40;
		} else {
			DBP_PROPERTY_CONFIDENCE_THRESHOLD = 15 + Integer.parseInt(args[0]);
		}
		log.info("Computing for Threshold = "
				+ DBP_PROPERTY_CONFIDENCE_THRESHOLD + "%");

		try {
			newTriples = 0;
			PropertyStatistics.loadPropStatsInMem("/input/DIRECT_PROP.log");

		} finally {

			MAP_PRED_COUNT.clear();
			MAP_OIE_IE_PROP_COUNTS.clear();
		}

		// PropertyStatistics.loadPropStatsInMem("/input/" +
		// GenerateNewProperties.INVERSE_PROP_LOG);

	}

	/**
	 * method to read the property distribution files in memory
	 * 
	 * @throws IOException
	 */
	public static void loadPropStatsInMem(String filePath) throws IOException {

		int blankMapCntr = 0;
		int nonBlankMapCntr = 0;

		// nell property in concern
		String nellProp = null;

		// read the file into memory
		ArrayList<ArrayList<String>> directPropsFile = FileUtil
				.genericFileReader(
						PropertyStatistics.class.getResourceAsStream(filePath),
						PATH_SEPERATOR, false);

		// iterate through them
		for (ArrayList<String> line : directPropsFile) {
			nellProp = line.get(1);

			if (nellProp != null) {

				if (line.size() == 3) {
					blankMapCntr++;
					updateMapValues(nellProp, "NA");
				} else {
					nonBlankMapCntr++;
					for (int cnt = 3; cnt < line.size(); cnt++) {
						updateMapValues(nellProp, line.get(cnt));
					}
				}

				// update the count of the occurance of this predicate
				updateCount(nellProp);
			}
		}

		// find statistics on every property
		performPredicateBasedAnalysis();

		// some stats
		log.info("TOTAL TRIPLES = " + directPropsFile.size());

		log.info("TOTAL NON-MAPABLE TRIPLES = "
				+ blankMapCntr
				+ " i.e "
				+ Math.round((double) (blankMapCntr * 100)
						/ directPropsFile.size()) + "%");

		log.info("TOTAL MAPPED TRIPLES = "
				+ nonBlankMapCntr
				+ " i.e "
				+ Math.round((double) (100 * nonBlankMapCntr)
						/ directPropsFile.size()) + "%");

		log.info("TOTAL PROPERTIES = " + MAP_OIE_IE_PROP_COUNTS.size() + "\n\n");

	}

	/**
	 * update the nell pred counts in the whole raw NEll file
	 * 
	 * @param nellProp
	 */
	private static void updateCount(String nellProp) {
		int count = 0;

		if (!MAP_PRED_COUNT.containsKey(nellProp)) {
			count = 1;
		} else {
			count = MAP_PRED_COUNT.get(nellProp);
			count++;
		}
		MAP_PRED_COUNT.put(nellProp, count);

	}

	/**
	 * iterate all the predicates stored in memory and find the statistics for
	 * each
	 * 
	 * @throws IOException
	 */
	private static void performPredicateBasedAnalysis() throws IOException {

		double percentageMapped = 0D;
		double propConf = 0D;

		int totalEntries = 0;
		int totalNonMapped = 0;

		// writer to dump the property stats
		BufferedWriter propStatsWriter = new BufferedWriter(new FileWriter(
				PROP_STATS));

		for (Map.Entry<String, Map<String, Integer>> entry : MAP_OIE_IE_PROP_COUNTS
				.entrySet()) {
			int countPredOccurncs = 0;
			int countNonMappedOccrncs = 0;
			// int countMappedOccrncs = 0;
			// int previousValue = 0;

			double factorDrop = 0D;

			for (Map.Entry<String, Integer> valueEntry : entry.getValue()
					.entrySet()) {
				countPredOccurncs = countPredOccurncs + valueEntry.getValue();
				if (valueEntry.getKey().equals("NA"))
					countNonMappedOccrncs = valueEntry.getValue();
			}

			percentageMapped = (double) (MAP_PRED_COUNT.get(entry.getKey()) - countNonMappedOccrncs)
					* 100 / (MAP_PRED_COUNT.get(entry.getKey()));

			if (percentageMapped > OIE_PROPERTY_MAPPED_THRESHOLD) {
				log.info("Predicate = " + entry.getKey());
				log.info("Number of triples in the data set = "
						+ MAP_PRED_COUNT.get(entry.getKey()));
				log.info("Total non-mapped triples = " + countNonMappedOccrncs);
				log.info("Total mapped triples = "
						+ (MAP_PRED_COUNT.get(entry.getKey()) - countNonMappedOccrncs));
				log.info("Percentage actually map-able (rounded) = "
						+ Math.round(percentageMapped) + "%");

				log.info("Number of values  = " + countPredOccurncs);

				ArrayList<String> possibleProps = new ArrayList<String>();

				// int count = 1;
				for (Map.Entry<String, Integer> valueEntry : MapUtils
						.sortByValue(entry.getValue(), false).entrySet()) {

					propConf = Math
							.round(((double) valueEntry.getValue() * 100 / MAP_PRED_COUNT
									.get(entry.getKey())));

					// use this prop confidence to determine if the unmapped
					// triples could be newly generated

					log.info("\t" + valueEntry.getKey() + "\t"
							+ valueEntry.getValue() + " (" + propConf + "%)");

					if (!valueEntry.getKey().equals("NA")
							&& propConf >= DBP_PROPERTY_CONFIDENCE_THRESHOLD) {

						// keep track of number of reconstructed triples
						// and if already reconstructed with some confident
						// property, other possible properties will also be
						// added,
						// since they can also create a set of new knowledge
						//
						newTriples = newTriples + valueEntry.getValue();

						log.info("Yes, " + countNonMappedOccrncs
								+ " triples can be newly added with "
								+ valueEntry.getKey());

						possibleProps.add(valueEntry.getKey());

					}

					log.debug("\t" + valueEntry.getKey() + "\t"
							+ valueEntry.getValue() + " (" + propConf + "%)\t"
							+ factorDrop);

				}

				checkForSubsumptionRelations(possibleProps);

				log.info("\n\n");
			}

			// dump all to the file
			propStatsWriter
					.write(entry.getKey()
							+ "\t"
							+ MAP_PRED_COUNT.get(entry.getKey())
							+ "\t"
							+ (MAP_PRED_COUNT.get(entry.getKey()) - countNonMappedOccrncs)
							+ "\t" + countNonMappedOccrncs + "\t"
							+ Math.round(percentageMapped) + "\n");

			totalEntries = totalEntries + MAP_PRED_COUNT.get(entry.getKey());
			totalNonMapped = totalNonMapped + countNonMappedOccrncs;

		} // end of for for all NELL properties

		propStatsWriter.flush();
		propStatsWriter.close();

		log.info("Total triples in data set = " + totalEntries);
		log.info("Total mapped triples = " + (totalEntries - totalNonMapped));
		log.info("Total non-mapped triples = " + totalNonMapped);
		log.info("Total that can be newly added = " + newTriples + " ("
				+ (double) newTriples * 100 / totalNonMapped + "%)\n\n");
		log.warn("\t" + DBP_PROPERTY_CONFIDENCE_THRESHOLD + "\t"
				+ (double) newTriples * 100 / totalNonMapped);
	}

	private static void checkForSubsumptionRelations(
			ArrayList<String> possibleProps) {
		for (int outer = 0; outer < possibleProps.size(); outer++) {
			for (int inner = outer + 1; inner < possibleProps.size() - outer; inner++) {
				log.debug("Comparing = " + possibleProps.get(outer) + ", "
						+ possibleProps.get(inner));
			}
		}

	}

	/**
	 * this method takes the all possible predicate matches, and looks for a
	 * clear winner, based on a particular threshold factor. If candProp1 = 74%,
	 * candProp2 = 25%, then candProp1 is a clear winner. However, there can be
	 * severalcorner cases, where they are close enough, or one prop subsumes
	 * other.
	 * 
	 * @param valueEntry
	 * @param propConf
	 */
	private static void findTheWinningDbpProp(String valueEntry, double propConf) {

		log.info("\t" + valueEntry + " (" + propConf + "%)");
		// add to a collection all the competitors

	}

	/*
	 * update the prop counts
	 */
	private static void updateMapValues(String nellProp, String dbProp) {

		Map<String, Integer> mapValues = null;
		dbProp = dbProp.replaceAll(Constants.DBPEDIA_CONCEPT_NS, "dbo:");

		if (!MAP_OIE_IE_PROP_COUNTS.containsKey(nellProp)) { // no key inserted
																// for nell
																// prop, create
																// one entry
			mapValues = new HashMap<String, Integer>();
			mapValues.put(dbProp, 1);
		} else { // if nell prop key exists

			// retrieve the existing collection first
			mapValues = MAP_OIE_IE_PROP_COUNTS.get(nellProp);

			// check and update the count of the dbprop values
			if (!mapValues.containsKey(dbProp)) {
				mapValues.put(dbProp, 1);
			} else {
				int val = mapValues.get(dbProp);
				mapValues.put(dbProp, val + 1);
			}
		}

		MAP_OIE_IE_PROP_COUNTS.put(nellProp, mapValues);
	}
}
