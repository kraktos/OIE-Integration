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

import org.apache.commons.math3.stat.regression.SimpleRegression;
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
public class PropertyStatisticsImproved
{

    // read the mother mappings file, containing nell triples and possible mappings
    public static final String INPUT_LOG = "/input/DIRECT_PROP.log"; // INVERSE_PROP.log");

    // define class logger
    public final static Logger log = LoggerFactory.getLogger(PropertyStatisticsImproved.class);

    // path seperator for the output property files
    public static final String PATH_SEPERATOR = "\t";

    // threshold to consider mappable predicates. It means consider NELL
    // predicates
    // which are atleast x % map-able
    private static final double OIE_PROPERTY_MAPPED_THRESHOLD = 40;

    // use it to set a threshold on the mapped DBpedia property mapping
    private static double DBP_PROPERTY_CONFIDENCE_THRESHOLD = 0D;

    private static final String PROP_STATS = "PROP_STATISTICS.tsv"; // "PROP_STATISTICS_TOP5.tsv";

    private static final String ITEMS_RULES = "PROP_TRANSC.tsv"; // "PROP_STATISTICS_TOP5.tsv";

    // tolerance of error, 1.1 means 10%
    private static final double ERROR_TOLERANCE = 1.1;

    // total triples that can be reconstructed
    private static int newTriples = 0;

    // map to hold the nell properties and the equivalent DBpedia properties
    // key is the nell property, value is a map with the dbp property and the
    // corresponding count
    private static Map<String, Map<String, Integer>> MAP_OIE_IE_PROP_COUNTS =
        new HashMap<String, Map<String, Integer>>();

    /**
     * collection to store the final mapped tripels from NELL to dbpedia
     */
    public static Map<String, List<String>> FINAL_MAPPINGS = new HashMap<String, List<String>>();

    static DecimalFormat twoDForm = new DecimalFormat("#.##");

    static SimpleRegression regression = new SimpleRegression(true);

    // static OLSMultipleLinearRegression regression2 = new
    // OLSMultipleLinearRegression();

    // map keeping count of the nell predicate occurrence, should be identical
    // to
    // the number of triples with that
    // property in the raw input file
    private static Map<String, Integer> MAP_PRED_COUNT = new HashMap<String, Integer>();

    public static void main(String[] args) throws IOException
    {
        if (args.length == 0) {
            DBP_PROPERTY_CONFIDENCE_THRESHOLD = 0;
        } else {
            DBP_PROPERTY_CONFIDENCE_THRESHOLD = 15 + Integer.parseInt(args[0]);
        }
        log.info("Computing for Threshold = " + DBP_PROPERTY_CONFIDENCE_THRESHOLD + "%");

        try {
            newTriples = 0;
            PropertyStatisticsImproved.loadPropStatsInMem(INPUT_LOG);
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
    public static void loadPropStatsInMem(String filePath) throws IOException
    {

        int blankMapCntr = 0;
        int nonBlankMapCntr = 0;

        // nell property in concern
        String nellProp = null;

        // read the file into memory
        ArrayList<ArrayList<String>> directPropsFile =
            FileUtil.genericFileReader(PropertyStatisticsImproved.class.getResourceAsStream(filePath), PATH_SEPERATOR,
                false);

        // write transactions to the file for analysis
        BufferedWriter itemsWriter = new BufferedWriter(new FileWriter(ITEMS_RULES));

        List<String> possibleProps = null;
        List<String> possibleTypes = null;

        // iterate through them
        for (ArrayList<String> line : directPropsFile) {
            nellProp = line.get(1);

            if (nellProp != null) {

                if (line.size() == 3) {
                    blankMapCntr++;
                    updateMapValues(nellProp, "NA");
                } else { // cases which could be mapoped
                    possibleProps = new ArrayList<String>();
                    possibleTypes = new ArrayList<String>();
                    
                    nonBlankMapCntr++;
                    for (int cnt = 3; cnt < line.size(); cnt++) {
                        if (line.get(cnt).contains(Constants.ONTOLOGY_NAMESPACE)) {
                            possibleProps.add(line.get(cnt));
                            updateMapValues(nellProp, line.get(cnt));
                        } else {
                            possibleTypes.add(line.get(cnt));
                        }
                    }

                    try {
                        for (String prop : possibleProps) {
                            itemsWriter.write(nellProp + "\t" + prop + "\t" + possibleTypes.get(0) + "\t"
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

        // train the regression model by feedin the data observed by the
        // underlying data set
        // trainRegressionModel();

        // log.info(String.valueOf(regression.getSignificance()));

        // System.exit(1);
        // find statistics on every property
        // performPredicateBasedAnalysis();

        // some stats
        log.info("TOTAL TRIPLES = " + directPropsFile.size());

        log.info("TOTAL NON-MAPABLE TRIPLES = " + blankMapCntr + " i.e "
            + Math.round((double) (blankMapCntr * 100) / directPropsFile.size()) + "%");

        log.info("TOTAL MAPPED TRIPLES = " + nonBlankMapCntr + " i.e "
            + Math.round((double) (100 * nonBlankMapCntr) / directPropsFile.size()) + "%");

        log.info("TOTAL PROPERTIES = " + MAP_OIE_IE_PROP_COUNTS.size() + "\n\n");

        log.warn(FINAL_MAPPINGS.toString());

        itemsWriter.close();
    }

    /**
     * update the nell pred counts in the whole raw NEll file
     * 
     * @param nellProp
     */
    private static void updateCount(String nellProp)
    {
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
     * method to learn the underlying data set behavior and fit a regression line
     * 
     * @param regression
     * @return
     * @throws IOException
     */
    public static void trainRegressionModel() throws IOException
    {

        double percentageMapped = 0D;
        double propConf = 0D;
        double tau = 0D;

        // writer to dump the property stats
        BufferedWriter propStatsWriter = new BufferedWriter(new FileWriter(PROP_STATS));

        for (Map.Entry<String, Map<String, Integer>> entry : MAP_OIE_IE_PROP_COUNTS.entrySet()) {

            int countPredOccurncs = 0;
            int countNonMappedOccrncs = 0;

            // iterate and find how many are actually not mappable
            for (Map.Entry<String, Integer> valueEntry : entry.getValue().entrySet()) {
                countPredOccurncs = countPredOccurncs + valueEntry.getValue();
                if (valueEntry.getKey().equals("NA"))
                    countNonMappedOccrncs = valueEntry.getValue();
            }

            // compute the mappable value for this predicate
            percentageMapped =
                (double) (MAP_PRED_COUNT.get(entry.getKey()) - countNonMappedOccrncs) * 100
                    / (MAP_PRED_COUNT.get(entry.getKey()));

            // write to output file for R
            if (Math.round(percentageMapped) > OIE_PROPERTY_MAPPED_THRESHOLD)
                propStatsWriter.write("\n" + String.valueOf(Math.round(percentageMapped)) + "\t");

            // iterate the individual possible DBpedia mappings to analyze
            // further
            for (Map.Entry<String, Integer> valueEntry : MapUtils.sortByValue(entry.getValue(), false).entrySet()) {

                propConf = ((double) valueEntry.getValue() * 100 / MAP_PRED_COUNT.get(entry.getKey()));

                if (!valueEntry.getKey().equals("NA")) {

                    tau = (double) (100 - percentageMapped) / propConf;

                    if (percentageMapped > OIE_PROPERTY_MAPPED_THRESHOLD)
                        try {
                            log.debug(Math.round(percentageMapped) + "\t" + Double.valueOf(twoDForm.format(tau)));

                            regression.addData(percentageMapped, Double.valueOf(twoDForm.format(tau)));

                            propStatsWriter.write(Double.valueOf(twoDForm.format(tau)) + "\t");

                        } catch (NumberFormatException e) {
                            log.debug(Math.round(percentageMapped) + "\t" + "-1");
                            propStatsWriter.write("-1\t");
                        }
                    break;
                }
            }
        }

        regression.addData(100, 0);
        propStatsWriter.write("\n100\t0");
        propStatsWriter.flush();
        propStatsWriter.close();

    }

    /**
     * iterate all the predicates stored in memory and find the statistics for each
     * 
     * @throws IOException
     */
    private static void performPredicateBasedAnalysis() throws IOException
    {

        double percentageMapped = 0D;
        double propConf = 0D;

        int totalEntries = 0;
        int totalNonMapped = 0;
        double tau = 0D;

        double error = 0D;

        for (Map.Entry<String, Map<String, Integer>> entry : MAP_OIE_IE_PROP_COUNTS.entrySet()) {
            int countPredOccurncs = 0;
            int countNonMappedOccrncs = 0;

            for (Map.Entry<String, Integer> valueEntry : entry.getValue().entrySet()) {
                countPredOccurncs = countPredOccurncs + valueEntry.getValue();
                if (valueEntry.getKey().equals("NA"))
                    countNonMappedOccrncs = valueEntry.getValue();
            }

            percentageMapped =
                (double) (MAP_PRED_COUNT.get(entry.getKey()) - countNonMappedOccrncs) * 100
                    / (MAP_PRED_COUNT.get(entry.getKey()));

            // if (percentageMapped > OIE_PROPERTY_MAPPED_THRESHOLD) {
            log.info("Predicate = " + entry.getKey());
            log.info("Number of triples in the data set = " + MAP_PRED_COUNT.get(entry.getKey()));
            log.info("Total non-mapped triples = " + countNonMappedOccrncs);
            log.info("Total mapped triples = " + (MAP_PRED_COUNT.get(entry.getKey()) - countNonMappedOccrncs));
            log.info("Percentage actually map-able (rounded) = " + Math.round(percentageMapped) + "%");

            log.info("Number of values  = " + countPredOccurncs);

            ArrayList<String> possibleProps = new ArrayList<String>();

            for (Map.Entry<String, Integer> valueEntry : MapUtils.sortByValue(entry.getValue(), false).entrySet()) {

                propConf = ((double) valueEntry.getValue() * 100 / MAP_PRED_COUNT.get(entry.getKey()));

                // use this prop confidence to determine if the unmapped
                // triples could be newly generated

                tau = (double) (100 - percentageMapped) / propConf;

                error =
                    (double) Math.abs((regression.predict(Math.round(percentageMapped)) - tau))
                        / (regression.predict(Math.round(percentageMapped)));

                log.info(entry.getKey() + "\t" + valueEntry.getKey() + "\t" + valueEntry.getValue() + " (" + propConf
                    + "%)" + "\t" + tau + "\t" + regression.predict(percentageMapped));

                if (!valueEntry.getKey().equals("NA")
                // && tau <=
                // regression.getIntercept()*(1-(double)OIE_PROPERTY_MAPPED_THRESHOLD/100)
                    && (tau <= ERROR_TOLERANCE * regression.predict(percentageMapped))) {
                    // && propConf >= DBP_PROPERTY_CONFIDENCE_THRESHOLD)
                    // {
                    // && Math.round(percentageMapped) >
                    // OIE_PROPERTY_MAPPED_THRESHOLD) {
                    // keep track of number of reconstructed triples
                    // and if already reconstructed with some confident
                    // property, other possible properties will also be
                    // added,
                    // since they can also create a set of new knowledge
                    //
                    // newTriples = newTriples + valueEntry.getValue();

                    newTriples = newTriples + countNonMappedOccrncs;

                    log.warn("Yes, " + countNonMappedOccrncs + " triples can be newly added with "
                        + valueEntry.getKey());

                    log.info(entry.getKey() + "\t" + "Can be predicated with " + valueEntry.getKey() + "\t"
                        + valueEntry.getValue() + " (" + propConf + "%)" + "\t" + tau);

                    log.warn("	" + entry.getKey() + "\t" + valueEntry.getKey() + "\t" + propConf + "\t"
                        + percentageMapped);

                    // store in memory
                    storeMappings(entry.getKey(), valueEntry.getKey());

                    possibleProps.add(valueEntry.getKey());

                }
            }
            // propStatsWriter.write("\n");

            checkForSubsumptionRelations(possibleProps);

            log.info("\n\n");
            // }

            // dump all to the file
            // propStatsWriter
            // .write(entry.getKey()
            // + "\t"
            // + MAP_PRED_COUNT.get(entry.getKey())
            // + "\t"
            // + (MAP_PRED_COUNT.get(entry.getKey()) - countNonMappedOccrncs)
            // + "\t" + countNonMappedOccrncs + "\t"
            // + Math.round(percentageMapped) + "\n");

            totalEntries = totalEntries + MAP_PRED_COUNT.get(entry.getKey());
            totalNonMapped = totalNonMapped + countNonMappedOccrncs;

        } // end of for for all NELL properties

        log.info("Total triples in data set = " + totalEntries);
        log.info("Total mapped triples = " + (totalEntries - totalNonMapped));
        log.info("Total non-mapped triples = " + totalNonMapped);
        log.info("Total that can be newly added = " + newTriples + " (" + (double) newTriples * 100 / totalNonMapped
            + "%)\n\n");
        log.warn("Amount of new knowledge created =\t" + newTriples + "(" + (double) newTriples * 100 / totalNonMapped
            + ")");
    }

    /**
     * store the finally learnt property mappings
     * 
     * @param oie property
     * @param dbpedia property
     */
    private static void storeMappings(String oieProp, String dbpProp)
    {
        List<String> possibleCands = null;
        if (FINAL_MAPPINGS.containsKey(oieProp)) {
            possibleCands = FINAL_MAPPINGS.get(oieProp);
            possibleCands.add(dbpProp);
        } else {
            possibleCands = new ArrayList<String>();
            possibleCands.add(dbpProp);
        }
        FINAL_MAPPINGS.put(oieProp, possibleCands);
    }

    // TODO
    private static void checkForSubsumptionRelations(ArrayList<String> possibleProps)
    {
        for (int outer = 0; outer < possibleProps.size(); outer++) {
            for (int inner = outer + 1; inner < possibleProps.size() - outer; inner++) {
                log.debug("Comparing = " + possibleProps.get(outer) + ", " + possibleProps.get(inner));
            }
        }
    }

    /*
     * update the prop counts
     */
    private static void updateMapValues(String nellProp, String dbProp)
    {

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
