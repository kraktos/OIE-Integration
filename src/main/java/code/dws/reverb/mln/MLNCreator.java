/**
 * 
 */
package code.dws.reverb.mln;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.List;
import java.util.Scanner;

import code.dws.dbConnectivity.DBWrapper;
import code.dws.reverb.ReverbClusterProperty;
import code.dws.utils.Constants;

/**
 * @author arnab
 */
public class MLNCreator
{

    /**
     * weighted domain and range for the R everb properties
     */
    private static final String WEIGHTED_ASSERTION_FILE = "/input/CLUSTERS_TYPE";

    private static final String DELIMIT = "\t";

    private static final String EVIDENCE = "reverb.dom.ran.conf";

    private static final long TOPK = 100;

    static DecimalFormat formatter = new DecimalFormat("#.############");

    /**
     * 
     */
    public MLNCreator()
    {
        // TODO Auto-generated constructor stub
    }

    /**
     * @param args
     * @throws IOException
     */
    public static void main(String[] args) throws IOException
    {

        createConditionalProb();

        // createGroundTruths(WEIGHTED_ASSERTION_FILE);
    }

    private static void createConditionalProb() throws IOException
    {
        String prop = null;
        long nProp = 0;

        List<String> results = null;
        BufferedWriter evidenceWriter = null;

        DBWrapper.init(Constants.GET_TOP_PROPERTIES_REVERB);
        try {
            results = DBWrapper.fetchTopReverbProperties(TOPK);
            if (results.size() > 0) {

                evidenceWriter = new BufferedWriter(new FileWriter(EVIDENCE));

                for (String result : results) {
                    prop = result.split("\t")[0];
                    nProp = Long.valueOf(result.split("\t")[1]);

                    getPropSpecificData(prop, nProp, evidenceWriter);

                    System.out.println(prop + "\t" + nProp);
                }
            }
        } finally {
            evidenceWriter.close();
            results.clear();
            results = null;
            DBWrapper.shutDown();
        }

    }

    /**
     * generate domConf and ranConf evidences
     * 
     * @param prop
     * @param nProp
     * @param evidenceWriter
     * @throws IOException
     */
    private static void getPropSpecificData(String prop, long nProp, BufferedWriter evidenceWriter) throws IOException
    {
        String dom = null;
        String ran = null;

        long nDomProb = 0;
        long nRanProb = 0;

        List<String> subTypesWithCounts = DBWrapper.getSubTypesCount(prop);

        if (subTypesWithCounts.size() > 0)
            for (String result : subTypesWithCounts) {
                dom = result.split("\t")[0];
                nDomProb = Long.valueOf(result.split("\t")[1]);
                System.out.println("P(" + dom + "|" + prop + ")  =" + (double) nDomProb / nProp);

                evidenceWriter.write("domConf(\"DBP#ontology/" + dom + "\", \"" + prop + "\", "
                    + formatter.format((double) nDomProb / nProp) + ")\n");
            }

        List<String> objTypesWithCounts = DBWrapper.getObjTypesCount(prop);
        if (objTypesWithCounts.size() > 0)
            for (String result : objTypesWithCounts) {
                ran = result.split("\t")[0];
                nRanProb = Long.valueOf(result.split("\t")[1]);
                System.out.println("P(" + ran + "|" + prop + ")  =" + (double) nRanProb / nProp);

                evidenceWriter.write("ranConf(\"DBP#ontology/" + ran + "\", \"" + prop + "\", "
                    + formatter.format((double) nRanProb / nProp) + ")\n");
            }

        evidenceWriter.flush();

    }

    private static void createGroundTruths(String weightedAssertionFile) throws IOException
    {
        Scanner scan;
        String sCurrentLine;
        String[] strArr = null;
        double weight = 0;

        String domain = null;
        String range = null;
        String property = null;
        BufferedWriter evidenceWriter = null;

        DecimalFormat formatter = new DecimalFormat("###.########");
        scan = new Scanner(ReverbClusterProperty.class.getResourceAsStream(weightedAssertionFile), "UTF-8");

        evidenceWriter = new BufferedWriter(new FileWriter(EVIDENCE));

        while (scan.hasNextLine()) {
            sCurrentLine = scan.nextLine();

            strArr = sCurrentLine.split(DELIMIT);
            weight = Double.parseDouble(strArr[0]);

            domain = strArr[1];
            range = strArr[2];
            property = strArr[3].replaceAll(" ", "_");

            evidenceWriter.write("propAsstConf(\"" + property + "\",\"DBP#ontology/" + domain + "\",\"DBP#ontology/"
                + range + "\"," + formatter.format(weight) + ")\n");

            evidenceWriter.flush();
        }
        System.out.println("Done writing to " + EVIDENCE);
        evidenceWriter.close();
    }

}
