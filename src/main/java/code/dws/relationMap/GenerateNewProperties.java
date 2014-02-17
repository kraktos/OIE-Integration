/**
 * 
 */
package code.dws.relationMap;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileFilter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import code.dws.dbConnectivity.DBWrapper;
import code.dws.query.SPARQLEndPointQueryAPI;
import code.dws.utils.Constants;
import code.dws.utils.FileUtil;
import code.dws.utils.Utilities;

import com.hp.hpl.jena.query.QuerySolution;

/**
 * This takes in the property hopped log files and tries to generate new properties
 * 
 * @author Arnab Dutta
 */
public class GenerateNewProperties
{
    // define class logger
    public final static Logger log = LoggerFactory.getLogger(GenerateNewProperties.class);

    private static final String PROPERTY_LOGS_PATH = "./src/main/resources/output";

    // location of the raw downloaded NELL path
    private static final String NELL_FILE_PATH = "/input/Nell_truncated.csv";

    // data seperator of the NELL data file
    private static final String PATH_SEPERATOR = ",";

    // defines the top-k candidate to be fetched for each NELL term
    private static final int SAMEAS_TOPK = 1;

    private static Map<String, List<String>> GLOBAL_PROPERTY_MAPPINGS = new HashMap<String, List<String>>();

    /**
     * @param args
     * @throws IOException
     */
    public static void main(String[] args) throws IOException
    {
        readFiles(NELL_FILE_PATH);
    }

    /**
     * read the NELL file to extract the subject-predicate-object
     * 
     * @param filePath
     * @throws IOException
     */
    public static void readFiles(String filePath) throws IOException
    {

        int lineCounter = 0;

        String nellRawSubj = null;
        String nellRawObj = null;

        String candidateSubj = null;
        String candidateObj = null;

        BufferedWriter directPropWriter = new BufferedWriter(new FileWriter("DIRECT_PROP.log"));
        BufferedWriter inversePropWriter = new BufferedWriter(new FileWriter("INVERSE_PROP.log"));

        // init DB for getting the most frequebt URI for the NELL terms
        DBWrapper.init(Constants.GET_WIKI_LINKS_APRIORI_SQL);

        // load the NELL file in memory as a collection
        ArrayList<ArrayList<String>> nellFile =
            FileUtil
                .genericFileReader(GenerateNewProperties.class.getResourceAsStream(filePath), PATH_SEPERATOR, false);

        // iterate the file
        for (ArrayList<String> line : nellFile) {
            try {
                log.debug(line.get(0) + "\t" + line.get(1) + "\t" + line.get(2) + "\n");

                // get the nell subjects and objects
                nellRawSubj = line.get(0);
                nellRawObj = line.get(2);

                // get the top-k concepts for the subject
                candidateSubj =
                    DBWrapper.fetchTopKLinksWikiPrepProb(Utilities.cleanse(nellRawSubj).replaceAll("\\_+", " ").trim(),
                        SAMEAS_TOPK).get(0);

                // get the top-k concepts for the object
                candidateObj =
                    DBWrapper.fetchTopKLinksWikiPrepProb(Utilities.cleanse(nellRawObj).replaceAll("\\_+", " ").trim(),
                        SAMEAS_TOPK).get(0);

                log.debug(candidateSubj.split("\t")[0] + "\t" + candidateObj.split("\t")[0]);

                findDirectIndirectProps(line, candidateSubj, candidateObj, directPropWriter, inversePropWriter);

                // update GLOBAL_PROPERTY_MAPPINGS with the possible values
                // updateTheCollection(nellRawPred, directProperties);

                if (lineCounter++ % 100 == 0)
                    log.info("Completed " + lineCounter + " of " + nellFile.size() + " lines ");

            } catch (IndexOutOfBoundsException e) {
                continue;
            } catch (Exception e) {
                log.error("Problem with line " + line.toString());
                e.printStackTrace();
                continue;
            }
        }

        // close streams
        directPropWriter.close();
        inversePropWriter.close();

    }

    /**
     * @param line
     * @param candidateSubj
     * @param candidateObj
     * @param propMapWriter
     * @param inversePropWriter
     * @return
     * @throws IOException
     */
    public static void findDirectIndirectProps(ArrayList<String> line, String candidateSubj, String candidateObj,
        BufferedWriter propMapWriter, BufferedWriter inversePropWriter) throws IOException
    {
        // for the current NELL predicate get the possible db:properties from
        // SPARQL endpoint

        // DIRECT PROPERTIES
        List<String> directProperties = getPredsFromEndpoint(candidateSubj.split("\t")[0], candidateObj.split("\t")[0]);

        propMapWriter.write(line.get(0) + "\t" + line.get(1) + "\t" + line.get(2) + "\t" + directProperties.toString()
            + "\n");
        propMapWriter.flush();

        log.debug(line + "\t" + directProperties.toString() + "\n");

        // INDIRECT PROPERTIES
        List<String> inverseProps = getPredsFromEndpoint(candidateObj.split("\t")[0], candidateSubj.split("\t")[0]);

        inversePropWriter.write(line.get(0) + "\t" + line.get(1) + "\t" + line.get(2) + "\t" + inverseProps.toString()
            + "\n");
        inversePropWriter.flush();

        log.debug(line + "\t" + inverseProps.toString() + "\n");

    }

    /**
     * updates a global collection of NELL property to possible mappings
     * 
     * @param nellRawPred
     * @param probablePredicates
     */
    private static void updateTheCollection(String nellRawPred, List<String> probablePredicates)
    {
        List<String> propValues = null;
        if (!GLOBAL_PROPERTY_MAPPINGS.containsKey(nellRawPred)) {
            propValues = new ArrayList<String>();

        } else {
            propValues = GLOBAL_PROPERTY_MAPPINGS.get(nellRawPred);
        }
        propValues.addAll(probablePredicates);
        GLOBAL_PROPERTY_MAPPINGS.put(nellRawPred, propValues);
    }

    /**
     * get the possible predicates for a particular combination
     * 
     * @param candSubj
     * @param candObj
     * @return
     */
    private static List<String> getPredsFromEndpoint(String candSubj, String candObj)
    {
        // possible predicate variable
        String possiblePred = null;

        // return list of all possible predicates
        List<String> returnPredicates = new ArrayList<String>();

        String sparqlQuery =
            "select * where {<"
                + Constants.DBPEDIA_INSTANCE_NS
                + candSubj
                + "> ?val <"
                + Constants.DBPEDIA_INSTANCE_NS
                + candObj
                + ">. "
                + "?val <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://www.w3.org/2002/07/owl#ObjectProperty>."
                + "FILTER(!regex(str(?val), 'http://dbpedia.org/ontology/wikiPageWikiLink'))}";

        log.debug(sparqlQuery);

        // fetch the result set
        List<QuerySolution> listResults = SPARQLEndPointQueryAPI.queryDBPediaEndPoint(sparqlQuery);

        for (QuerySolution querySol : listResults) {
            possiblePred = querySol.get("val").toString();

            // add the sub classes to a set
            returnPredicates.add(possiblePred);

        }

        return returnPredicates;
    }

    /**
     * read the files generated from the property learner
     */
    public static void readFiles()
    {
        File folder = null;
        File[] paths;

        try {
            // create new file
            folder = new File(PROPERTY_LOGS_PATH);

            FileFilter filter = new FileFilter()
            {
                @Override
                public boolean accept(File pathname)
                {
                    return pathname.isFile() && pathname.length() > 0;
                }
            };

            // returns pathnames for files and directory
            paths = folder.listFiles(filter);

            // for each pathname in pathname array
            for (File path : paths) {

                // individually parse each file and get the properties with hop lengths greater than one
                // these signify the ones which are not directly map-able to DBpedia

                processFile(path.toString());

            }
        } catch (Exception e) {
            // if any error occurs
            e.printStackTrace();
        }
    }

    /**
     * take the individual property files and parse them for likely paths
     * 
     * @param path
     * @throws IOException
     */
    private static void processFile(String path) throws IOException
    {

        InputStream is =
            GenerateNewProperties.class.getResourceAsStream(path.toString().replaceAll("./src/main/resources", ""));

        Scanner scan = new Scanner(is, "UTF-8");

        while (scan.hasNextLine()) {

            String line = scan.nextLine();
            String[] arr = line.split("\t");
            if (arr.length > 2)
                log.info(String.valueOf(line));

        }
    }
}
