/**
 * 
 */
package code.dws.reverb;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tartarus.snowball.ext.PorterStemmer;

import code.dws.relationMap.PropertyStatisticsImproved;
import code.dws.utils.FileUtil;

/**
 * @author arnab
 */
public class ReverbPropCluster
{

    // define class logger
    public final static Logger log = LoggerFactory.getLogger(ReverbPropCluster.class);

    private static final String PATH_SEPERATOR = "";

    private static final String REVERB_PROP_FILE = "/input/uniq_props.txt";

    private static final String OUTPUT = "OUTPUT.log";

    static BufferedWriter outputWriter;

    /*
     * remove some prepositions
     */
    static String regex = "\\ban?\\b|\\bbe\\b|\\bof\\b|\\bfor\\b|\\bin\\b"
        + "|\\bat\\b|\\bby\\b|\\bto\\b|\\bthe\\b|\\bfrom\\b|\\bon\\b|\\b-\\b";

    /**
     * 
     */
    public ReverbPropCluster()
    {

    }

    /**
     * @param args
     */
    public static void main(String[] args)
    {

        try {
            outputWriter = new BufferedWriter(new FileWriter(OUTPUT));
            readPropFiles(REVERB_PROP_FILE);

        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    private static void readPropFiles(String filePath) throws IOException
    {
        // read the file into memory
        ArrayList<ArrayList<String>> reverbPropFile =
            FileUtil.genericFileReader(PropertyStatisticsImproved.class.getResourceAsStream(filePath), PATH_SEPERATOR,
                false);

        // iterate
        for (ArrayList<String> line : reverbPropFile) {
            log.debug(line.toString() + "\t" + stemTerm(line.get(0)));
//            outputWriter.write(line.get(0) + "\t" + stemTerm(line.get(0).replaceAll(regex, "")) + "\n");
            outputWriter.write(stemTerm(line.get(0).trim().replaceAll(regex, "").replaceAll(" ", "~")) + "\n");
            outputWriter.flush();
        }

        outputWriter.close();
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
