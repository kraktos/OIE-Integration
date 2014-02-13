/**
 * 
 */

package code.dws.core;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.hp.hpl.jena.query.QuerySolution;

import sun.swing.plaf.synth.Paint9Painter;

import code.dws.dao.Pair;
import code.dws.experiment.ExperimentAutomation;
import code.dws.query.SPARQLEndPointQueryAPI;
import code.dws.utils.Constants;
import code.dws.utils.Utilities;

/**
 * This class is responsible for clustering the properties from NELL with
 * analogous properties from DBpedia with the filtered set of output coming from
 * the reasoning output, using our Alpha tree algorithm
 * 
 * @author Arnab Dutta
 */
public class PropertyCluster {

    // stores the same as statements finally which stays after being reasoned
    private static Map<String, String> GLOBAL_SAME_AS_MAP = new HashMap<String, String>();

    // stores a list of well filtered triples
    private static List<Pair<String, String>> PROP_ASST_LIST = new ArrayList<Pair<String, String>>();

    // store a collection of SAme as statements for the subjects
    private static Map<String, String> SUB_SAME_AS_MAP = new HashMap<String, String>();

    // store a collection of SAme as statements for the objects
    private static Map<String, String> OBJ_SAME_AS_MAP = new HashMap<String, String>();

    // stores the set of possible properties
    private static Map<String, Double> DBP_RANKED_PROP = new HashMap<String, Double>();

    // stores the set of possible properties
    private static Map<String, Double> DBP_INV_RANKED_PROP = new HashMap<String, Double>();

    /**
     * @param args
     * @throws Exception
     */
    public static void main(String[] args) throws Exception {

        if (args.length < 1) {
            throw (new RuntimeException(
                    "Usage : java -jar cluster.jar <NELL Property>"));
        }
        else {
            ExperimentAutomation.PREDICATE = args[0];

            ExperimentAutomation.DBPEDIA_SPARQL_ENDPOINT = "http://dbpedia.org/sparql";

            System.out.println("\n\n");
            // load the output from reasoner in memory
            loadFileInMemory();

            // use the maps loaded for further processing
            findProperties();

            if (getDBPProp().size() > 0) {
                System.out.println("\n ************* Direct Properties for "
                        + ExperimentAutomation.PREDICATE + " ***** ");
                printValues(getDBPProp());
            }

            if (getInvDBPProp().size() > 0) {
                System.out.println("\n ************* Inverse Properties for "
                        + ExperimentAutomation.PREDICATE + " ***** ");
                printValues(getInvDBPProp());
            }

        }
    }

    private static void printValues(Map<String, Double> collMap) {
        for (Map.Entry<String, Double> entry : collMap.entrySet()) {
            System.out.println(entry.getKey() + "\t" + entry.getValue() + "\t"
                    + (double) entry.getValue() / PROP_ASST_LIST.size());
        }
    }

    /**
     * @return the dBP_RANKED_PROP
     */
    public static Map<String, Double> getDBPProp() {
        return DistantSupervised.sortByValue(DBP_RANKED_PROP);
    }

    /**
     * @return the dBP_RANKED_PROP
     */
    public static Map<String, Double> getInvDBPProp() {
        return DistantSupervised.sortByValue(DBP_INV_RANKED_PROP);
    }

    private static void findProperties() {

        String nSub = null;
        String nObj = null;

        String dSub = null;
        String dObj = null;

        String sparqlQuery = null;
        List<QuerySolution> listResults = null;

        String dbpPred = null;

        // iterate the list of filtered triples
        for (Pair<String, String> pair : PROP_ASST_LIST) {
            nSub = pair.getFirst();
            nObj = pair.getSecond();

            dSub = cleanseInstances(getSUB_SAME_AS_MAP().get(nSub));
            dObj = cleanseInstances(getOBJ_SAME_AS_MAP().get(nObj));

            // use the DBP subject and object to find triples
            sparqlQuery = "select ?pred where{ <http://dbpedia.org/resource/" + dSub
                    + "> ?pred <http://dbpedia.org/resource/" + dObj + ">}";

            getProperties(sparqlQuery, DBP_RANKED_PROP);

            // use the DBP subject and object to find triples
            sparqlQuery = "select ?pred where{ <http://dbpedia.org/resource/" + dObj
                    + "> ?pred <http://dbpedia.org/resource/" + dSub + ">}";

            getProperties(sparqlQuery, DBP_INV_RANKED_PROP);
        }
    }

    /**
     * @param sparqlQuery
     * @param coll
     */
    public static void getProperties(String sparqlQuery, Map<String, Double> coll) {
        List<QuerySolution> listResults;
        String dbpPred;
        listResults = SPARQLEndPointQueryAPI.queryDBPediaEndPoint(sparqlQuery);

        for (QuerySolution querySol : listResults) {
            dbpPred = querySol.get("pred").toString();

            if (dbpPred.indexOf(Constants.ONTOLOGY_NAMESPACE) != -1) {
                updateCollection(dbpPred, coll);
            }
        }
    }

    private static void updateCollection(String dbpPred, Map<String, Double> coll) {
        double count = 0;
        if (coll.containsKey(dbpPred)) {
            count = coll.get(dbpPred);
            coll.put(dbpPred, count + 1);
        } else {
            coll.put(dbpPred, 1D);
        }
    }

    public static String cleanseInstances(String dbpInst) {
        dbpInst = dbpInst.replaceAll("~", "%");
        dbpInst = dbpInst.replaceAll("\\[", "(");
        dbpInst = dbpInst.replaceAll("\\]", ")");
        dbpInst = dbpInst.replaceAll("\\*", "'");
        return Utilities.utf8ToCharacter(dbpInst);
    }

    /**
     * @return the sUB_SAME_AS_MAP
     */
    private static Map<String, String> getSUB_SAME_AS_MAP() {
        return SUB_SAME_AS_MAP;
    }

    /**
     * @return the oBJ_SAME_AS_MAP
     */
    private static Map<String, String> getOBJ_SAME_AS_MAP() {
        return OBJ_SAME_AS_MAP;
    }

    /**
     * read the output file into memory
     * 
     * @param outputFromReasonerFile
     */
    private static void loadFileInMemory() {

        String triple;
        String subject = null;
        String object = null;

        BufferedReader input;

        try {
            input = new BufferedReader
                    (new InputStreamReader(new FileInputStream(
                            Constants.REASONER_OUTPUT_MLN_EVIDENCE)));

            Matcher matcher = null;
            Pattern pattern = Pattern.compile("sameAs\\((.*?)\\)");

            // iterate the file from OIE and process each triple at a time
            while ((triple = input.readLine()) != null) {
                matcher = pattern.matcher(triple);

                while (matcher.find()) {
                    // 0th element is the DBPedia instance\
                    // 1st element is the NELL instance, this is postfixed and
                    // so serves well as unique key
                    GLOBAL_SAME_AS_MAP.put(matcher.group(1).split(",")[1].replaceAll("\"", "")
                            .replaceAll("NELL#Instance/", ""),
                            matcher.group(1).split(",")[0].replaceAll("\"", "")
                                    .replaceAll("DBP#resource/", ""));
                }
            }

            // after reaching file end, re-initialize the reader
            input = new BufferedReader
                    (new InputStreamReader(new FileInputStream(
                            Constants.REASONER_OUTPUT_MLN_EVIDENCE)));

            // redefine a different pattern
            pattern = Pattern.compile("propAsst\\((.*?)\\)");

            // re-iterate to find the non-erroneous triples
            while ((triple = input.readLine()) != null) {
                matcher = pattern.matcher(triple);

                while (matcher.find()) {
                    subject = matcher.group(1).split(",")[1].replaceAll("\"", "")
                            .replaceAll("NELL#Instance/", "");
                    object = matcher.group(1).split(",")[2].replaceAll("\"", "")
                            .replaceAll("NELL#Instance/", "");

                    // if both the subject and object is present in the same As
                    // collection, then it is a non-erroneous triple
                    if (GLOBAL_SAME_AS_MAP.containsKey(subject)
                            && GLOBAL_SAME_AS_MAP.containsKey(object))
                    {
                        // keep on adding the subjects to subject map
                        SUB_SAME_AS_MAP.put(subject, GLOBAL_SAME_AS_MAP.get(subject));

                        // .. and objects to object map
                        OBJ_SAME_AS_MAP.put(object, GLOBAL_SAME_AS_MAP.get(object));

                        // also add the subject, object pairs
                        PROP_ASST_LIST.add(new Pair<String, String>(subject, object));
                    }

                }
            }

            // iterate once more to find the good triples, the ones where both
            // subject and object
            // have actually a same as entry, ones where one of them is not in
            // the
            // output file denotes a wrong mapping and hence was eliminated by
            // the inference engine

//            System.out.println("same as statment count = " + GLOBAL_SAME_AS_MAP.size());
//            System.out.println("Good triples = " + PROP_ASST_LIST.size());
//            System.out.println("Good subject sameAs asserts  = " + SUB_SAME_AS_MAP.size());
//            System.out.println("Good object sameAs asserts  = " + OBJ_SAME_AS_MAP.size());

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            // clear up memory
            GLOBAL_SAME_AS_MAP.clear();
            GLOBAL_SAME_AS_MAP = null;
        }

    }
}
