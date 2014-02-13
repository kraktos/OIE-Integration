/**
 * 
 */

package code.dws.goldStandard;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

import org.apache.log4j.Logger;

import code.dws.dbConnectivity.DBWrapper;
import code.dws.experiment.ExperimentAutomation;
import code.dws.utils.Constants;

/**
 * @author Arnab Dutta
 */
public class ProcessAnnotatedGoldStd {

    public static Logger logger = Logger.getLogger(ProcessAnnotatedGoldStd.class.getName());

    /**
     * @param args
     * @throws IOException
     */
    public static void main(String[] args) throws IOException {
        // here we chunk out only the data pertaining to a predicate...
        createBLandGSFile();
    }

    /**
     * create gs and bl file for owl creation
     * 
     * @param predicate
     * @throws IOException
     */
    private static void createBLandGSFile() throws IOException {

        // location of the new data set files directory
        new File(Constants.sample_dumps + ExperimentAutomation.PREDICATE).mkdir();

        System.out.println(Constants.FULL_GS_DUMP);
        BufferedReader goldReader = new BufferedReader(new FileReader(Constants.FULL_GS_DUMP));
        BufferedReader baseReader = new BufferedReader(new FileReader(Constants.BL_DUMP));

        BufferedWriter nellConfWriter = new BufferedWriter(new FileWriter(
                Constants.NELL_CONFIDENCE_FILE));

        BufferedWriter gsBlWriter = new BufferedWriter(new FileWriter(
                Constants.sample_dumps + ExperimentAutomation.PREDICATE + "/goldBL_" + ExperimentAutomation.PREDICATE + ".tsv"));

        String gLine = null;
        String[] goldElemnts = null;
        String bLine = null;

        try {
            while ((gLine = goldReader.readLine()) != null
                    && (bLine = baseReader.readLine()) != null) {

                goldElemnts = gLine.split("\t");

                if (goldElemnts[1].equals(ExperimentAutomation.PREDICATE))
                {
                    if (goldElemnts.length > 5) {
                        gsBlWriter.write(bLine + "\t" +
                                goldElemnts[3] + "\t"
                                + goldElemnts[4] + "\t" + "IC" + "\n");
                    }

                    if (goldElemnts.length == 5) {
                        gsBlWriter.write(bLine + "\t" +
                                goldElemnts[3] + "\t"
                                + goldElemnts[4] + "\t" + "C" + "\n");
                    }

                    findConfidences(goldElemnts[0], goldElemnts[1],
                            goldElemnts[2],
                            nellConfWriter);
                }
            }

            gsBlWriter.close();
            nellConfWriter.close();

        } catch (Exception e) {
            System.out.println("Exception with " + gLine + "  \n" + bLine);
        }

        // close the reader writer streams
        nellConfWriter.close();
        gsBlWriter.close();

    }

    /**
     * for the given subset of data, fetch its confidences and create a file in
     * its folder
     * 
     * @param sub
     * @param pred
     * @param obj
     * @param nellConfWriter
     * @throws IOException
     */
    private static void findConfidences(String sub, String pred, String obj,
            BufferedWriter nellConfWriter) throws IOException {
        // As we read the triples, generate the confidence values from the table

        DBWrapper.init(Constants.GET_NELL_CONF);

        List<Double> listConf = DBWrapper.fetchNELLConfidence(sub, pred, obj);

        nellConfWriter.write(sub + "\t" + pred + "\t" + obj + "\t"
                + String.valueOf(listConf.get(0)) + "\n");

        DBWrapper.shutDown();
    }
}
