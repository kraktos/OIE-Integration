/**
 * 
 */
package code.dws.core;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import code.dws.dbConnectivity.DBWrapper;
import code.dws.relationMap.PropertyStatistics;
import code.dws.utils.Constants;
import code.dws.utils.FileUtil;
import code.dws.utils.Utilities;

/**
 * core class for generating new triples in DBpedia
 * 
 * @author Arnab Dutta
 */
public class GenerateNewKnowledge
{

    // define class logger
    public final static Logger log = LoggerFactory.getLogger(GenerateNewKnowledge.class);

    private static final String NEW_TRIPLES_LOG = "NEW_TRIPLES.log";

    /**
     * @param args
     * @throws IOException
     */
    public static void main(String[] args) throws IOException
    {
        // Call the property learner to populate the possible mappings in
        // memory
        PropertyStatistics.main(new String[] {});

        // use this in memory mappings to generate the new triples
        createNewTriples();
    }

    /**
     * read the log file, and the property mappings to fill up the cases where there are no possible mappings
     * 
     * @throws IOException
     */
    private static void createNewTriples() throws IOException
    {
        int countNewTripels = 0;

        // nell property in concern
        String nellProp = null;
        String nellRawSubj = null;
        String nellRawObj = null;

        List<String> candidateSubjs = null;
        List<String> candidateObjs = null;

        // read the file into memory
        ArrayList<ArrayList<String>> oieTriples =
            FileUtil.genericFileReader(PropertyStatistics.class.getResourceAsStream(PropertyStatistics.INPUT_LOG),
                PropertyStatistics.PATH_SEPERATOR, false);

        // init DB for getting the most frequebt URI for the NELL terms
        DBWrapper.init(Constants.GET_WIKI_LINKS_APRIORI_SQL);

        // new knowledge file
        BufferedWriter newKnowledgeWriter = new BufferedWriter(new FileWriter(NEW_TRIPLES_LOG));

        // iterate through them
        for (ArrayList<String> triple : oieTriples) {
            nellProp = triple.get(1);

            if (nellProp != null && PropertyStatistics.FINAL_MAPPINGS.containsKey(nellProp)) {
                // indicates that no mapping for this triple
                if (triple.size() == 3) {
                    // get the nell subjects and objects
                    nellRawSubj = triple.get(0);
                    nellRawObj = triple.get(2);

                    // get the top-k concepts for the subject
                    candidateSubjs =
                        DBWrapper.fetchTopKLinksWikiPrepProb(Utilities.cleanse(nellRawSubj).replaceAll("\\_+", " ")
                            .trim(), 1);

                    // get the top-k concepts for the object
                    candidateObjs =
                        DBWrapper.fetchTopKLinksWikiPrepProb(Utilities.cleanse(nellRawObj).replaceAll("\\_+", " ")
                            .trim(), 1);

                    try {
                        for (String candSubj : candidateSubjs) {
                            for (String candObj : candidateObjs) {

                                log.info("NEW = " + candidateSubjs.get(0).split("\t")[0] + "\t"
                                    + PropertyStatistics.FINAL_MAPPINGS.get(nellProp) + "\t"
                                    + candidateObjs.get(0).split("\t")[0]);

                                for (String dbpProp : PropertyStatistics.FINAL_MAPPINGS.get(nellProp)) {

                                    newKnowledgeWriter.write(candidateSubjs.get(0).split("\t")[0] + "\t" + dbpProp
                                        + "\t" + candidateObjs.get(0).split("\t")[0] + "\n");

                                    countNewTripels++;
                                }
                            }
                        }
                        newKnowledgeWriter.flush();

                    } catch (Exception e) {
                        log.error(candidateSubjs + "\t" + candidateObjs);
                    }
                }
            }
        }

        log.warn(countNewTripels + " new triples were added..");
        newKnowledgeWriter.close();
    }
}
