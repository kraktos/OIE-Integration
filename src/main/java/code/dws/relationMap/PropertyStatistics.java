/**
 * 
 */
package code.dws.relationMap;

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
public class PropertyStatistics
{

    // define class logger
    public final static Logger log = LoggerFactory.getLogger(PropertyStatistics.class);

    // path seperator for the output property files
    private static final String PATH_SEPERATOR = "\t";

    // map to hold the nell properties and the equivalent DBpedia properties
    // key is the nell property, value is a map with the dbp property and the
    // corresponding count
    private static Map<String, Map<String, Integer>> MAP_OIE_IE_PROP_COUNTS =
        new HashMap<String, Map<String, Integer>>();

    // map keeping count of the nell predicate occurance, should be identical to the number of triples with that
    // property in the raw input file
    private static Map<String, Integer> MAP_PRED_COUNT = new HashMap<String, Integer>();

    public static void main(String[] args)
    {
        PropertyStatistics.loadPropStatsInMem("/input/" + GenerateNewProperties.DIRECT_PROP_LOG);

        // PropertyStatistics.loadPropStatsInMem("/input/" + GenerateNewProperties.INVERSE_PROP_LOG);

    }

    /**
     * method to read the property distribution files in memory
     */
    public static void loadPropStatsInMem(String filePath)
    {

        int blankMapCntr = 0;
        int nonBlankCntr = 0;

        String nellProp = null;

        // read the file into memory
        ArrayList<ArrayList<String>> directPropsFile =
            FileUtil.genericFileReader(PropertyStatistics.class.getResourceAsStream(filePath), PATH_SEPERATOR, false);

        // iterate through them
        for (ArrayList<String> line : directPropsFile) {
            nellProp = line.get(1);

            if (nellProp != null) {

                if (line.size() == 3) {
                    blankMapCntr++;
                    updateMapValues(nellProp, "NA");
                } else {
                    nonBlankCntr++;
                    for (int cnt = 3; cnt < line.size(); cnt++) {
                        updateMapValues(nellProp, line.get(cnt));
                    }
                }

                // update the count of the occurance of this predicate
                updateCount(nellProp);
            }
        }

        // some stats
        log.info("TOTAL TRIPLES = " + directPropsFile.size());

        log.info("TOTAL NON-MAPABLE TRIPLES = " + blankMapCntr + " i.e " + (double) blankMapCntr
            / directPropsFile.size());

        log.info("TOTAL MAPPED TRIPLES = " + nonBlankCntr + " i.e " + (double) nonBlankCntr / directPropsFile.size());

        log.info("TOTAL PROPERTIES = " + MAP_OIE_IE_PROP_COUNTS.size() + "\n\n");

        // find statistics on every property
        performPredicateBasedAnalysis();

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
     * iterate all the predicates stored in memory and find the statistics for each
     */
    private static void performPredicateBasedAnalysis()
    {
        for (Map.Entry<String, Map<String, Integer>> entry : MAP_OIE_IE_PROP_COUNTS.entrySet()) {
            int countPredOccurncs = 0;

            log.info("Predicate = " + entry.getKey());

            for (Map.Entry<String, Integer> valueEntry : MapUtils.sortByValue(entry.getValue(), false).entrySet()) {
                log.info(valueEntry.getKey() + "\t" + valueEntry.getValue());
                countPredOccurncs = countPredOccurncs + valueEntry.getValue();
            }

            log.info("Possible values for it  = " + countPredOccurncs);
            log.info("Number of entries in the data set = " + MAP_PRED_COUNT.get(entry.getKey()) + "\n\n");
        }
    }

    /*
     * update the prop counts
     */
    private static void updateMapValues(String nellProp, String dbProp)
    {

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
