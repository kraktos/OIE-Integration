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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
	private static final double OIE_PROPERTY_MAPPED_THRESHOLD = 50;

	// use it to set a threshold on the mapped DBpedia property mapping
	private static final double DBP_PROPERTY_CONFIDENCE_THRESHOLD = 30;

	private static final String PROP_STATS = "PROP_STATISTICS_TOP5.tsv"; // "PROP_STATISTICS.tsv";

	// total triples that can be reconstructed
	private static int reconstructedTriples = 0;

	// map to hold the nell properties and the equivalent DBpedia properties
	// key is the nell property, value is a map with the dbp property and the
	// corresponding count
	private static Map<String, Map<String, Integer>> MAP_OIE_IE_PROP_COUNTS = new HashMap<String, Map<String, Integer>>();

	// map keeping count of the nell predicate occurance, should be identical to
	// the number of triples with that
	// property in the raw input file
	private static Map<String, Integer> MAP_PRED_COUNT = new HashMap<String, Integer>();

	public static void main(String[] args) throws IOException {
		PropertyStatistics.loadPropStatsInMem("/input/"
				+ GenerateNewProperties.DIRECT_PROP_LOG);

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

				for (Map.Entry<String, Integer> valueEntry : MapUtils
						.sortByValue(entry.getValue(), false).entrySet()) {

					propConf = Math
							.round(((double) valueEntry.getValue() * 100 / MAP_PRED_COUNT
									.get(entry.getKey())));

					log.info("\t" + valueEntry.getKey() + "\t"
							+ valueEntry.getValue() + " (" + propConf + "%)");

					// use this prop confidence to determine if the unmapped
					// triples could be rectifired	
					// once in this block, then both the dbproperty confidence
					// and the portion of non-mapped
					// triples are in favour of knowledge re-construction

					if (propConf > DBP_PROPERTY_CONFIDENCE_THRESHOLD
							&& !valueEntry.getKey().equals("NA")) {

						// keep track of number of reconstructed triples
						// and if already reconstructed with some confident
						// property, other possible properties will also be
						// added,
						// since they can also create a set of new knowledge
						//
						reconstructedTriples = reconstructedTriples
								+ valueEntry.getValue();

						log.info("Yes, " + countNonMappedOccrncs
								+ " triples can be reconstructed with "
								+ valueEntry.getKey());

					}
				}
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
		log.info("Total that can be reconstructed = "
				+ reconstructedTriples
				+ " ("
				+ Math.round((double) reconstructedTriples * 100
						/ totalNonMapped) + "%)\n\n");
	}

	/*
	 * update the prop counts
	 */
	private static void updateMapValues(String nellProp, String dbProp) {

		Map<String, Integer> mapValues = null;

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
