/**
 * 
 */
package code.dws.relationMap;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import code.dws.dbConnectivity.DBWrapper;
import code.dws.ontology.GenericConverter;
import code.dws.utils.Constants;
import code.dws.utils.FileUtil;
import code.dws.utils.Utilities;

/**
 * This takes in the property hopped log files and tries to generate new properties
 * 
 * @author Arnab Dutta
 */
public class GenerateNewProperties
{
    public final static Logger log = LoggerFactory.getLogger(GenerateNewProperties.class);

    private static final String PROPERTY_LOGS_PATH = "./src/main/resources/output";

    private static final String PATH_SEPERATOR = ",";

    private static final String NELL_FILE_PATH = "/input/Nell_truncated.csv";

    private static final int SAMEAS_TOPK = 1;

    /**
     * @param args
     */
    public static void main(String[] args)
    {
        readFiles(NELL_FILE_PATH);
    }

    /**
     * read the NELL file to extract the subject-predicate-object
     * 
     * @param filePath
     */
    public static void readFiles(String filePath)
    {
        String nellRawSubj = null;
        String nellRawPred = null;
        String nellRawObj = null;

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

                List<String> sameAsConfidences;

                // get the top-k concepts, confidence pairs
                sameAsConfidences =
                    DBWrapper.fetchTopKLinksWikiPrepProb(Utilities.cleanse(nellRawSubj).replaceAll("\\_+", " ").trim(),
                        SAMEAS_TOPK);

                log.info(nellRawSubj + "\t" + sameAsConfidences.get(0).split("\t")[0]);

            } catch (Exception e) {
                log.error("Problem with line {}" + line.toString());
                continue;
            }
        }
        log.info(String.valueOf(nellFile.size()));
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
