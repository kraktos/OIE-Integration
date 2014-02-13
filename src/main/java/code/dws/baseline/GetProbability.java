/**
 * 
 */

package code.dws.baseline;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.log4j.Logger;

import code.dws.dbConnectivity.DBWrapper;
import code.dws.utils.Constants;
import code.dws.utils.Utilities;

/**
 * This class computes the probabilities of each of the term-concept links
 * 
 * @author Arnab Dutta
 */
public class GetProbability {

    // logger
    static Logger logger = Logger.getLogger(GetProbability.class.getName());

    /**
     * @param args
     * @throws IOException
     */
    public static void main(String[] args) throws IOException {

        getConceptLinksProbability();
    }

    /**
     * get the most frequent URI
     * 
     * @param bw
     * @throws IOException
     */
    private static final void getConceptLinksProbability() throws IOException {

        BufferedWriter bw = new BufferedWriter(new FileWriter(Constants.APRIORI_PROB_FILE));

        BufferedReader baseReader = new BufferedReader(new FileReader(Constants.INPUT_CSV_FILE));

        List<String> uriVsProbabilities = null;

        String ieSubj = null;
        String ieObj = null;

        String[] arrBaseLineInst = null;

        // init DB
        DBWrapper.init(Constants.GET_WIKI_LINKS_APRIORI_SQL);

        Set<String> termConceptPairSet = new HashSet<String>();

        String bLine;
        while ((bLine = baseReader.readLine()) != null) {

            arrBaseLineInst = bLine.split("\t");

            ieSubj = arrBaseLineInst[0];
            ieObj = arrBaseLineInst[2];

            uriVsProbabilities = DBWrapper.fetchTopKLinksWikiPrepProb(Utilities.cleanse(ieSubj)
                    .replaceAll("\\_+", " "), Constants.SAMEAS_TOPK);

            for (String val : uriVsProbabilities) {
                if (!termConceptPairSet.contains(ieSubj + val)) {
                    bw.write(ieSubj + "\t" + Utilities.utf8ToCharacter(val) + "\n");
                    termConceptPairSet.add(ieSubj + val);
                }
            }
            uriVsProbabilities = null;

            uriVsProbabilities = DBWrapper.fetchTopKLinksWikiPrepProb(Utilities.cleanse(ieObj)
                    .replaceAll("\\_+", " "), Constants.SAMEAS_TOPK);

            for (String val : uriVsProbabilities) {
                if (!termConceptPairSet.contains(ieObj + val)) {
                    bw.write(ieObj + "\t" + Utilities.utf8ToCharacter(val) + "\n");
                    termConceptPairSet.add(ieObj + val);
                }
            }

        }// end of for loop
        
        // close stream
        bw.close();
        
        // clear set
        termConceptPairSet.clear();
        logger.info("Output written to " + Constants.APRIORI_PROB_FILE);
    }

}
