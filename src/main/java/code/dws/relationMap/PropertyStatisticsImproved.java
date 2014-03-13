/**
 * 
 */
package code.dws.relationMap;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.math3.stat.regression.SimpleRegression;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import code.dws.dao.Pair;
import code.dws.utils.Constants;
import code.dws.utils.FileUtil;

/**
 * Responsible for computing the property statistics
 * 
 * @author adutta
 */
public class PropertyStatisticsImproved {

	// read the mother mappings file, containing nell triples and possible
	// mappings
	public static final String INPUT_LOG = "/input/DIRECT_PROP.log"; // INVERSE_PROP.log");

	// define class logger
	public final static Logger log = LoggerFactory
			.getLogger(PropertyStatisticsImproved.class);

	// path seperator for the output property files
	public static final String PATH_SEPERATOR = "\t";

	// threshold to consider mappable predicates. It means consider NELL
	// predicates
	// which are atleast x % map-able
	private static final double OIE_PROPERTY_MAPPED_THRESHOLD = 40;

	private static final String PROP_STATS = "PROP_STATISTICS.tsv"; // "PROP_STATISTICS_TOP5.tsv";

	private static final String ITEMS_RULES = "PROP_TRANSC.tsv"; // "PROP_STATISTICS_TOP5.tsv";

	private static Map<String, Map<String, Map<Pair<String, String>, Long>>> GLOBAL_TRANSCS_MAP = new HashMap<String, Map<String, Map<Pair<String, String>, Long>>>();

	// tolerance of error, 1.1 means 10%
	private static final double ERROR_TOLERANCE = 1;

	// total triples that can be reconstructed
	private static int newTriples = 0;

	// map to hold the nell properties and the equivalent DBpedia properties
	// key is the nell property, value is a map with the dbp property and the
	// corresponding count
	private static Map<String, Map<String, Integer>> MAP_OIE_IE_PROP_COUNTS = new HashMap<String, Map<String, Integer>>();

	/**
	 * collection to store the final mapped tripels from NELL to dbpedia
	 */
	public static Map<String, List<String>> FINAL_MAPPINGS = new HashMap<String, List<String>>();

	static DecimalFormat twoDForm = new DecimalFormat("#.######");

	static SimpleRegression regression = new SimpleRegression(true);

	// static OLSMultipleLinearRegression regression2 = new
	// OLSMultipleLinearRegression();

	// map keeping count of the nell predicate occurrence, should be identical
	// to
	// the number of triples with that
	// property in the raw input file
	private static Map<String, Integer> MAP_PRED_COUNT = new HashMap<String, Integer>();

	public static void main(String[] args) throws IOException {

		try {
			newTriples = 0;
			PropertyStatisticsImproved.run(INPUT_LOG);
			// INVERSE_PROP.log

		} finally {

			MAP_PRED_COUNT.clear();
			MAP_OIE_IE_PROP_COUNTS.clear();
		}
	}

	/**
	 * method to read the property distribution files in memory
	 * 
	 * @throws IOException
	 */
	public static void run(String filePath) throws IOException {

		double percentageMapped = 0D;

		int blankMapCntr = 0;
		int nonBlankMapCntr = 0;

		// nell property in concern
		String nellProp = null;

		// read the file into memory
		ArrayList<ArrayList<String>> directPropsFile = FileUtil
				.genericFileReader(PropertyStatisticsImproved.class
						.getResourceAsStream(filePath), PATH_SEPERATOR, false);

		// write transactions to the file for analysis
		BufferedWriter itemsWriter = new BufferedWriter(new FileWriter(
				ITEMS_RULES));

		// writer to dump the property stats
		BufferedWriter propStatsWriter = new BufferedWriter(new FileWriter(
				PROP_STATS));

		List<String> possibleProps = null;
		List<String> possibleTypes = null;

		// iterate through them
		for (ArrayList<String> line : directPropsFile) {
			nellProp = line.get(1);

			if (nellProp != null) {

				if (line.size() == 3) {
					blankMapCntr++;
					updateMapValues(nellProp, "NA");
				} else { // cases which could be mapped
					possibleProps = new ArrayList<String>();
					possibleTypes = new ArrayList<String>();

					nonBlankMapCntr++;
					for (int cnt = 3; cnt < line.size(); cnt++) {
						if (line.get(cnt)
								.contains(Constants.ONTOLOGY_NAMESPACE)) {
							possibleProps.add(line.get(cnt));
							updateMapValues(nellProp, line.get(cnt));
						} else {
							possibleTypes.add(line.get(cnt));
						}
					}

					// small routine to dump separately the property
					// transactions with classes associate
					try {
						for (String prop : possibleProps) {
							itemsWriter.write(nellProp + "\t" + prop + "\t"
									+ possibleTypes.get(0) + "\t"
									+ possibleTypes.get(1) + "\n");
						}
					} catch (Exception e) {
						log.error("Problem with line = " + line);
					}
					possibleProps.clear();
				}

				// update the count of the occurrence of this predicate
				updateCount(nellProp);
			}

			itemsWriter.flush();
		}

		loadPropDistributionInCollection();

		// DEBUG POINT, print the distribution
		for (Map.Entry<String, Map<String, Map<Pair<String, String>, Long>>> entry : GLOBAL_TRANSCS_MAP
				.entrySet()) {
			double probMax = 0D;

			long nellPredCount = getNellPropCount(entry.getKey());

			// compute the mappable ratio for this predicate
			percentageMapped = 100 * (1 - ((double) MAP_OIE_IE_PROP_COUNTS.get(
					entry.getKey()).get("NA") / nellPredCount));

			for (Map.Entry<String, Map<Pair<String, String>, Long>> nellVal : entry
					.getValue().entrySet()) {

				long nellDbpPredCount = getNellAndDbpPropCount(entry.getKey(),
						nellVal.getKey());

				for (Map.Entry<Pair<String, String>, Long> pairs : nellVal
						.getValue().entrySet()) {

					double domProb = (double) getNellAndDbpTypeCount(
							entry.getKey(), pairs.getKey().getFirst(), true)
							/ nellPredCount;

					double ranProb = (double) getNellAndDbpTypeCount(
							entry.getKey(), pairs.getKey().getSecond(), false)
							/ nellPredCount;

					double countProb = (double) pairs.getValue()
							/ nellPredCount;

					double jointProb = (double) getNellAndBothDbpTypeCount(
							entry.getKey(), pairs.getKey().getFirst(), pairs
									.getKey().getSecond())
							/ nellPredCount; // domProb * ranProb * countProb;

					// look for the max probability of two classes occurring
					// together
					if (jointProb > probMax) {
						probMax = jointProb;
					}

					log.info(entry.getKey()
							+ "("
							+ nellPredCount
							+ ")\t"
							+ nellVal.getKey()
							+ "("
							+ nellDbpPredCount
							+ ")\t"
							+ pairs.getKey().getFirst()
							+ "("
							+ getNellAndDbpTypeCount(entry.getKey(), pairs
									.getKey().getFirst(), true)
							+ ")\t"
							+ pairs.getKey().getSecond()
							+ "("
							+ getNellAndDbpTypeCount(entry.getKey(), pairs
									.getKey().getSecond(), false) + ")\t"
							+ pairs.getValue() + "\t" + jointProb);

				}
			}

			double tau = (double) (MAP_OIE_IE_PROP_COUNTS.get(entry.getKey())
					.get("NA")) / (nellPredCount * probMax);

			log.info(entry.getKey() + "(" + getNellPropCount(entry.getKey())
					+ ")" + "\tNA\t"
					+ MAP_OIE_IE_PROP_COUNTS.get(entry.getKey()).get("NA")
					+ "\t" + probMax + "\t" + tau);

			log.debug(entry.getKey() + "\t" + percentageMapped);

			if (percentageMapped >= OIE_PROPERTY_MAPPED_THRESHOLD) {
				// train the regression model by feedin the data observed by the

				// underlying data set
				// adding maximum tau for the nell property to the regression
				// model
				regression.addData(
						Double.valueOf(twoDForm.format(percentageMapped)),
						Double.valueOf(twoDForm.format(tau)));

				propStatsWriter.write(Double.valueOf(twoDForm
						.format(percentageMapped))
						+ "\t"
						+ Double.valueOf(twoDForm.format(tau)) + "\n");
			}
		}

		regression.addData(100, 0);
		propStatsWriter.write("100\t0");

		// apply selection based on the underlying regression line
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

		for (Entry<String, List<String>> entry : FINAL_MAPPINGS.entrySet()) {
			log.warn(entry.getKey() + "\t" + entry.getValue());
		}

		itemsWriter.close();
		propStatsWriter.flush();
		propStatsWriter.close();

	}

	/**
	 * loads the entire property distribution of nell over dbpedia in a
	 * colelction
	 */
	private static void loadPropDistributionInCollection() {

		// read the file into memory
		ArrayList<ArrayList<String>> propRules = FileUtil.genericFileReader(
				PropertyStatisticsImproved.class.getResourceAsStream("/input/"
						+ ITEMS_RULES), PATH_SEPERATOR, false);

		String nellProp = null;
		String dbProp = null;

		String dom = null;
		String ran = null;
		long count = 0;

		for (ArrayList<String> line : propRules) {
			nellProp = line.get(0);
			dbProp = line.get(1);
			dom = line.get(2);
			ran = line.get(3);

			Map<String, Map<Pair<String, String>, Long>> nellPropMap = null;
			Map<Pair<String, String>, Long> dbpPropMap = null;

			Pair<String, String> pair = new Pair<String, String>(dom, ran);

			if (GLOBAL_TRANSCS_MAP.containsKey(nellProp)) {
				nellPropMap = GLOBAL_TRANSCS_MAP.get(nellProp);

				if (nellPropMap.containsKey(dbProp)) {
					dbpPropMap = nellPropMap.get(dbProp);

					if (dbpPropMap.containsKey(pair)) {
						count = dbpPropMap.get(pair);
						count++;
						dbpPropMap.put(pair, count);
					} else {
						dbpPropMap.put(pair, 1L);
					}
				} else {
					dbpPropMap = new HashMap<Pair<String, String>, Long>();
					dbpPropMap.put(pair, 1L);
				}
			} else {
				nellPropMap = new HashMap<String, Map<Pair<String, String>, Long>>();
				dbpPropMap = new HashMap<Pair<String, String>, Long>();
				dbpPropMap.put(new Pair<String, String>(dom, ran), 1L);
			}
			nellPropMap.put(dbProp, dbpPropMap);

			GLOBAL_TRANSCS_MAP.put(nellProp, nellPropMap);
		}

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
		double tau = 0D;

		// iterate over nell properties
		for (Map.Entry<String, Map<String, Map<Pair<String, String>, Long>>> entry : GLOBAL_TRANSCS_MAP
				.entrySet()) {

			long nellPredCount = getNellPropCount(entry.getKey());

			// compute the mappable value for this predicate
			percentageMapped = 100 * (1 - ((double) MAP_OIE_IE_PROP_COUNTS.get(
					entry.getKey()).get("NA") / nellPredCount));

			// iterate over dbpedia properties
			for (Map.Entry<String, Map<Pair<String, String>, Long>> nellVal : entry
					.getValue().entrySet()) {

				long nellDbpPredCount = getNellAndDbpPropCount(entry.getKey(),
						nellVal.getKey());

				// iterate over all possible class types
				for (Map.Entry<Pair<String, String>, Long> pairs : nellVal
						.getValue().entrySet()) {

					double domProb = (double) getNellAndDbpTypeCount(
							entry.getKey(), pairs.getKey().getFirst(), true)
							/ nellPredCount;

					double ranProb = (double) getNellAndDbpTypeCount(
							entry.getKey(), pairs.getKey().getSecond(), false)
							/ nellPredCount;

					double countProb = (double) pairs.getValue()
							/ nellPredCount;

					double jointProb = (double) pairs.getValue()
							/ getNellAndBothDbpTypeCount(entry.getKey(), pairs
									.getKey().getFirst(), pairs.getKey()
									.getSecond());// domProb * ranProb *
													// countProb;

					if (entry.getKey().equals("parentofperson"))
						System.out.println();
					tau = (double) MAP_OIE_IE_PROP_COUNTS.get(entry.getKey())
							.get("NA") / (nellPredCount * jointProb);

					log.info(entry.getKey()
							+ "("
							+ nellPredCount
							+ ")\t"
							+ nellVal.getKey()
							+ "("
							+ nellDbpPredCount
							+ ")\t"
							+ pairs.getKey().getFirst()
							+ "("
							+ getNellAndDbpTypeCount(entry.getKey(), pairs
									.getKey().getFirst(), true)
							+ ")\t"
							+ pairs.getKey().getSecond()
							+ "("
							+ getNellAndDbpTypeCount(entry.getKey(), pairs
									.getKey().getSecond(), false) + ")\t"
							+ pairs.getValue() + "\t" + tau 
							+ "\t" + Math.round(percentageMapped) + "%\t"
							+ regression.predict(percentageMapped));

					if (tau <= ERROR_TOLERANCE
							* Math.abs(regression.predict(percentageMapped))) {
						// store in memory
						storeMappings(entry.getKey(), nellVal.getKey());
					}

				}
			}

		}

	}

	/**
	 * store the finally learnt property mappings
	 * 
	 * @param oie
	 *            property
	 * @param dbpedia
	 *            property
	 */
	private static void storeMappings(String oieProp, String dbpProp) {
		List<String> possibleCands = null;
		if (FINAL_MAPPINGS.containsKey(oieProp)) {
			possibleCands = FINAL_MAPPINGS.get(oieProp);
			if (!possibleCands.contains(dbpProp))
				possibleCands.add(dbpProp);
		} else {
			possibleCands = new ArrayList<String>();
			possibleCands.add(dbpProp);
		}
		FINAL_MAPPINGS.put(oieProp, possibleCands);
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

	/**
	 * number of itemsets wher a nell and dbp property occur
	 * 
	 * @param nellProp
	 * @param dbpProp
	 * @return
	 */
	private static long getNellAndDbpPropCount(String nellProp, String dbpProp) {
		long val = 0;

		for (Map.Entry<String, Map<String, Map<Pair<String, String>, Long>>> entry : GLOBAL_TRANSCS_MAP
				.entrySet()) {

			if (entry.getKey().equals(nellProp)) {
				for (Map.Entry<String, Map<Pair<String, String>, Long>> nellVal : entry
						.getValue().entrySet()) {
					if (nellVal.getKey().equals(dbpProp)) {
						for (Map.Entry<Pair<String, String>, Long> pairs : nellVal
								.getValue().entrySet()) {
							val = val + pairs.getValue();
						}
					}
				}
			}
		}

		return val;
	}

	/**
	 * number of itemsets where the given nell prop occurs
	 * 
	 * @param nellProp
	 * @return
	 */
	private static long getNellPropCount(String nellProp) {
		long val = 0;

		for (Map.Entry<String, Map<String, Map<Pair<String, String>, Long>>> entry : GLOBAL_TRANSCS_MAP
				.entrySet()) {

			if (entry.getKey().equals(nellProp)) {
				for (Map.Entry<String, Map<Pair<String, String>, Long>> nellVal : entry
						.getValue().entrySet()) {
					for (Map.Entry<Pair<String, String>, Long> pairs : nellVal
							.getValue().entrySet()) {
						val = val + pairs.getValue();
					}
				}
			}
		}

		return val + MAP_OIE_IE_PROP_COUNTS.get(nellProp).get("NA");
	}

	/**
	 * number of itemsets where the nell propertz and a given type (domain or
	 * range ) occurs
	 * 
	 * @param nellProp
	 * @param type
	 * @param isDomain
	 * @return
	 */
	private static long getNellAndDbpTypeCount(String nellProp, String type,
			boolean isDomain) {
		long val = 0;

		boolean flag = false;

		for (Map.Entry<String, Map<String, Map<Pair<String, String>, Long>>> entry : GLOBAL_TRANSCS_MAP
				.entrySet()) {

			if (entry.getKey().equals(nellProp)) {
				for (Map.Entry<String, Map<Pair<String, String>, Long>> nellVal : entry
						.getValue().entrySet()) {
					for (Map.Entry<Pair<String, String>, Long> pairs : nellVal
							.getValue().entrySet()) {
						flag = (isDomain) ? (pairs.getKey().getFirst()
								.equals(type)) : (pairs.getKey().getSecond()
								.equals(type));
						if (flag) {
							val = val + pairs.getValue();
						}
					}
				}
			}
		}

		return val;
	}

	/**
	 * number of itemsets where the nell property and a given type (domain or
	 * range ) occurs
	 * 
	 * @param nellProp
	 * @param type
	 * @param isDomain
	 * @return
	 */
	private static long getNellAndBothDbpTypeCount(String nellProp,
			String domain, String range) {
		long val = 0;

		boolean flag = false;

		for (Map.Entry<String, Map<String, Map<Pair<String, String>, Long>>> entry : GLOBAL_TRANSCS_MAP
				.entrySet()) {

			if (entry.getKey().equals(nellProp)) {
				for (Map.Entry<String, Map<Pair<String, String>, Long>> nellVal : entry
						.getValue().entrySet()) {
					for (Map.Entry<Pair<String, String>, Long> pairs : nellVal
							.getValue().entrySet()) {
						flag = (pairs.getKey().getFirst().equals(domain))
								&& (pairs.getKey().getSecond().equals(range));
						if (flag) {
							val = val + pairs.getValue();
						}
					}
				}
			}
		}

		return val;
	}

}
