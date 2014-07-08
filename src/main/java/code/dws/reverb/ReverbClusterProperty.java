/**
 * 
 */
package code.dws.reverb;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Scanner;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;

import code.dws.dbConnectivity.DBWrapper;
import code.dws.utils.Constants;
import code.dws.wordnet.SimilatityWebService;
import code.dws.wordnet.WordNetAPI;

/**
 * this class clubs together properties with similar domain, range distribution
 * 
 * @author arnab
 */
public class ReverbClusterProperty
{

    // Reverb original triples file

    private static final String PREPROCESSED_REVERBFILE = "/input/OUTPUT_WEIGHTED.log";

    private static final String DELIMIT = "\t";

    /*
     * output location for the type pairs and the properties that are common
     */
    private static final String CLUSTERS = "CLUSTERS_TYPE";

    private static final String CLUSTERS_WORDNET = "CLUSTERS_UMBC";

    /**
     * 
     */
    public ReverbClusterProperty()
    {
        //
    }

    private static Map<Pair<String, String>, Map<String, Double>> MAP_CLUSTER =
        new HashMap<Pair<String, String>, Map<String, Double>>();

    /**
     * @param args
     * @throws IOException
     */
    public static void main(String[] args) throws IOException
    {
        getWordNetSimScores();

        // getDistinctClusterNames(new String[][] {new String[] {"is a member of"}, new String[]
        // {"is an active member of"}, new String[] {"is the spouse of"}, new String[] {"located in"}, new String[]
        // {"will take place in"}});

        // readProcessedFile(PREPROCESSED_REVERBFILE);

        // dumpPropCluster();

    }

    private static void getWordNetSimScores() throws IOException
    {
        BufferedWriter outputWriter = new BufferedWriter(new FileWriter(CLUSTERS_WORDNET));

        // init DB
        DBWrapper.init(Constants.GET_DISTINCT_REVERB_PROPERTIES);
        try {
            List<String> results = DBWrapper.fetchDistinctReverbProperties(100);
            for (int id = 0; id < results.size(); id++) {
                for (int id2 = id + 1; id2 < results.size(); id2++) {
                    // System.out.println(results.get(id) + "\t" + results.get(id2) + " ==> " +
                    // WordNetAPI.scoreWordNet(results.get(id).split(" "), results.get(id2).split(" ")));

                    outputWriter.write(results.get(id) + "\t" + results.get(id2) + " ==> "
                        + SimilatityWebService.getSimScore(results.get(id), results.get(id2) + "\n"));

                    // outputWriter.write(results.get(id) + "\t" + results.get(id2) + " ==> "
                    // + WordNetAPI.scoreWordNet(results.get(id).split(" "), results.get(id2).split(" ")) + "\n");
                }
                outputWriter.flush();
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            outputWriter.close();
            DBWrapper.shutDown();
        }

    }

    /**
     * method to get the baseline cluster without any MLN or intelligent ways
     * 
     * @param strings
     * @throws IOException
     */
    private static void getDistinctClusterNames(String[][] strings) throws IOException
    {
        String[] reverbProperties = null;

        if (strings.length == 0) {

            BufferedWriter outputWriter = new BufferedWriter(new FileWriter(CLUSTERS));

            // init DB
            DBWrapper.init(Constants.GET_DISTINCT_REVERB_PROP_CLUSTERS);

            try {
                List<String> results = DBWrapper.fetchDistinctReverbClusterNames();

                // iterate the distinct cluster names = [Domain, Range]
                for (String val : results) {

                    outputWriter.write(val.split("\t")[0] + "\t" + val.split("\t")[1] + "\n");

                    reverbProperties = val.split("\t")[2].split("~");

                    for (String prop : reverbProperties) {
                        outputWriter.write("\t" + prop + "\t" + WordNetAPI.getSynonyms(prop) + "\n");

                        // look for wordnet senses
                        // System.out.println(prop + "==> " + WordNetAPI.getSynonyms(prop));

                    }

                    outputWriter.flush();
                }

            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                outputWriter.close();
                DBWrapper.shutDown();
            }
        } else {

            // compute the intra property sim score using Wordnet
            for (int outer = 0; outer < strings.length - 1; outer++) {
                for (int inner = outer + 1; inner < strings.length; inner++) {
                    System.out
                        .println("COMPARING:  " + strings[outer][0].toString() + " \t " + strings[inner][0].toString()
                            + "\t" + WordNetAPI.scoreWordNet(strings[outer], strings[inner]));
                }
            }
        }
    }

    /**
     * read the pre processed Reverb triples and cluster them on domain, range
     * 
     * @param filePath
     */
    private static void readProcessedFile(String filePath)
    {
        Scanner scan;
        String sCurrentLine;
        String[] strArr = null;
        double prob = 0;
        double weight = 0;

        String domain = null;
        String range = null;
        String property = null;

        scan = new Scanner(ReverbClusterProperty.class.getResourceAsStream(filePath), "UTF-8");

        Pair<String, String> pairConcepts = null;

        while (scan.hasNextLine()) {
            sCurrentLine = scan.nextLine();

            strArr = sCurrentLine.split(DELIMIT);
            prob = Double.parseDouble(strArr[0]);
            weight = Double.parseDouble(strArr[1]);

            domain = strArr[2];
            property = strArr[4];
            range = strArr[6];

            pairConcepts = new ImmutablePair<String, String>(domain.trim(), range.trim());

            // put all the weighted types for each properties in memory
            updateMap(pairConcepts, property, weight);

            if (MAP_CLUSTER.size() % 1000 == 0)
                System.out.println("Processed " + MAP_CLUSTER.size());
        }

    }

    /**
     * put the romain range pair as key and the associated proeprty value with weights in a memory collection
     * 
     * @param pair
     * @param property
     * @param weight
     */
    private static void updateMap(Pair<String, String> pair, String property, double weight)
    {

        Map<String, Double> values = null;

        if (MAP_CLUSTER.containsKey(pair)) {
            // get old collection
            values = MAP_CLUSTER.get(pair);
        } else {
            // create new collection
            values = new HashMap<String, Double>();
        }

        if (!values.containsKey(property)) {
            values.put(property, weight);
        } else {
            if (values.get(property) < weight) {
                // update the property with a better score
                values.put(property, weight);
            }
        }

        MAP_CLUSTER.put(pair, values);

    }

    /**
     * write out the clusters to a file
     * 
     * @throws IOException
     */
    private static void dumpPropCluster() throws IOException
    {
        BufferedWriter outputWriter = null;
        try {
            outputWriter = new BufferedWriter(new FileWriter(CLUSTERS));
            for (Entry<Pair<String, String>, Map<String, Double>> e : MAP_CLUSTER.entrySet()) {

                for (Entry<String, Double> propEntry : e.getValue().entrySet()) {

                    outputWriter.write(propEntry.getValue() + "\t" + e.getKey().getLeft() + "\t"
                        + e.getKey().getRight() + "\t" + propEntry.getKey() + "\n");
                }
                outputWriter.flush();
            }

        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            outputWriter.close();
        }

    }

}
