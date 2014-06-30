/**
 * 
 */
package code.dws.reverb;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tartarus.snowball.ext.PorterStemmer;

import code.dws.dbConnectivity.DBWrapper;
import code.dws.query.SPARQLEndPointQueryAPI;
import code.dws.relationMap.PropertyStatisticsImproved;
import code.dws.utils.Constants;
import code.dws.utils.FileUtil;
import code.dws.utils.Utilities;

/**
 * this class takes a rax Reverb input, gets the type of top-1 candidate for each instance, and dumps into a file for
 * further analysis
 * 
 * @author arnab
 */
public class ReverbPreProcessing
{

    // define class logger
    public final static Logger log = LoggerFactory.getLogger(ReverbPreProcessing.class);

    private static final String PATH_SEPERATOR = "";

    // Reverb original triples file
    private static final String REVERB_FILE = "/input/highReverb.csv"; // uniq_props.txt";

    private static final String REVERB_PROP_FILE = "/input/test.txt"; // uniq_props.txt";

    private static final String OUTPUT = "OUTPUT_WEIGHTED.log";

    // defines the top-k candidate to be fetched for each NELL term
    private static final int SAMEAS_TOPK = 1;

    static BufferedWriter outputWriter;

    static String DELIMIT = ";";

    /*
     * remove some prepositions
     */
    static String regex = "\\ban?\\b|\\bbe\\b|\\bof\\b|\\bfor\\b|\\bin\\b"
        + "|\\bat\\b|\\bby\\b|\\bto\\b|\\bthe\\b|\\bfrom\\b|\\bon\\b|\\b-\\b";

    /**
     * 
     */
    public ReverbPreProcessing()
    {

    }

    /**
     * @param args
     * @throws IOException
     */
    public static void main(String[] args) throws IOException
    {
        // readPropFiles(REVERB_PROP_FILE);
        readTriples(REVERB_FILE);
    }

    public static void readTriples(String filePath) throws IOException
    {
        FileInputStream inputStream = null;
        Scanner sc = null;
        String sCurrentLine;
        String[] strArr = null;
        String revSubj = null;
        String revProp = null;
        String revObj = null;
        BufferedReader br = null;

        List<String> candidateSubjs = null;
        List<String> candidateObjs = null;

        String subType = null;
        String objType = null;

        double simScoreSubj = 0;
        double simScoreObj = 0;

        String types = null;

        outputWriter = new BufferedWriter(new FileWriter(OUTPUT));

        try {
            // init DB
            DBWrapper.init(Constants.GET_WIKI_LINKS_APRIORI_SQL);

            Scanner scan;
            scan = new Scanner(ReverbPreProcessing.class.getResourceAsStream(filePath), "UTF-8");

            while (scan.hasNextLine()) {

                sCurrentLine = scan.nextLine();

                strArr = sCurrentLine.split(DELIMIT);
                revSubj = strArr[0].trim();
                revProp = stemTerm(strArr[1].trim());
                revObj = strArr[2].trim();

                // get the top-k concepts for the subject
                candidateSubjs =
                    DBWrapper.fetchTopKLinksWikiPrepProb(Utilities.cleanse(revSubj).replaceAll("\\_+", " ").trim(),
                        SAMEAS_TOPK);
                // get the top-k concepts for the object
                candidateObjs =
                    DBWrapper.fetchTopKLinksWikiPrepProb(Utilities.cleanse(revObj).replaceAll("\\_+", " ").trim(),
                        SAMEAS_TOPK);

                if (candidateSubjs.size() > 0) {
                    subType = getTypeInfo(candidateSubjs.get(0).split("\t")[0]);
                    simScoreSubj = Double.parseDouble(candidateSubjs.get(0).split("\t")[1]);
                }

                if (candidateObjs.size() > 0) {
                    objType = getTypeInfo(candidateObjs.get(0).split("\t")[0]);
                    simScoreObj = Double.parseDouble(candidateObjs.get(0).split("\t")[1]);
                }

                outputWriter.write(simScoreSubj * simScoreObj + "\t"
                    + Utilities.convertProbabilityToWeight(simScoreSubj * simScoreObj) + "\t" + subType + "\t"
                    + revSubj + "\t" + revProp + "\t" + revObj + "\t" + objType + "\n");
                outputWriter.flush();

                candidateSubjs = null;
                candidateObjs = null;
                subType = null;
                objType = null;

            }
        } finally {
            if (inputStream != null) {
                inputStream.close();
            }

            outputWriter.close();

        }
    }

    private static String getTypeInfo(String inst)
    {
        String mostSpecificVal = null;

        List<String> types = SPARQLEndPointQueryAPI.getInstanceTypes(Utilities.utf8ToCharacter(inst));

        try {
            mostSpecificVal = SPARQLEndPointQueryAPI.getLowestType(types).get(0);
        } catch (IndexOutOfBoundsException e) {
        } finally {
            return mostSpecificVal;
        }
    }

    private static void readPropFiles(String filePath) throws IOException
    {
        // read the file into memory
        ArrayList<ArrayList<String>> reverbPropFile =
            FileUtil.genericFileReader(ReverbPreProcessing.class.getResourceAsStream(filePath), PATH_SEPERATOR, false);

        // iterate

        String arg1 = null;
        String arg2 = null;
        int score = 0;

        outputWriter = new BufferedWriter(new FileWriter(OUTPUT));

        for (int outer = 0; outer < reverbPropFile.size(); outer++) {
            arg1 = stemTerm(reverbPropFile.get(outer).get(0));
            for (int inner = outer + 1; inner < reverbPropFile.size(); inner++) {
                arg2 = stemTerm(reverbPropFile.get(inner).get(0));
                score = StringUtils.getLevenshteinDistance(arg1, arg2);

                log.debug("Comparing " + arg1 + ", " + arg2 + "\t " + score);
                // outputWriter.write(stemTerm(line.get(0).trim().replaceAll(regex, "").replaceAll(" ", "~")) + "\n");
                // outputWriter.flush();
            }
        }

        // outputWriter.close();
    }

    /**
     * stem every predicate to reduce to base form
     * 
     * @param term
     * @return
     */

    private static String stemTerm(String term)
    {
        PorterStemmer stem = new PorterStemmer();
        stem.setCurrent(term);
        stem.stem();
        return stem.getCurrent();
    }

}
