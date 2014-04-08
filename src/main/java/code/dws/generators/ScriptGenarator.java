/**
 * 
 */
package code.dws.generators;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import code.dws.relationMap.GenerateNewProperties;
import code.dws.utils.FileUtil;

/**
 * @author arnab
 */
public class ScriptGenarator
{

    // define class logger
    public final static Logger log = LoggerFactory.getLogger(ScriptGenarator.class);

    // data separator of the NELL data file
    private static final String PATH_SEPERATOR = ",";

    static List<String> PROPS = new ArrayList<String>();

    private static final String SHELL_SCRIPT = "FULL_SCRIPT.sh";

    private static final int MAX_BOOT_ITER = 6;

    /**
     * @param args
     * @throws IOException
     */
    public static void main(String[] args) throws IOException
    {
        loadOIEProps(GenerateNewProperties.NELL_FILE_PATH);
        log.info("Loaded all properties from " + GenerateNewProperties.NELL_FILE_PATH + ";  " + PROPS.size());

        generateScript();

    }

    private static void generateScript() throws IOException
    {
        BufferedWriter scriptWriter = new BufferedWriter(new FileWriter(SHELL_SCRIPT));

        scriptWriter.write("#!/bin/bash\n\n");

        for (String oieProp : PROPS) {
            int bootIter = 2;
            scriptWriter.write("sh ./PIPELINE_ALL_NELL.sh " + oieProp + "\n");
            while (bootIter != MAX_BOOT_ITER + 2) {
                scriptWriter.write("sh ./BOOTSTRAP_ALL_NELL.sh " + oieProp + " " + bootIter++ + "\n");
            }
            scriptWriter.write("echo \"Done with complete reasoning of " + oieProp + "\"\n\n");

            System.out.println("java -jar /home/arnab/Workspaces/UPDATE_REFINED.jar " + oieProp);
        }

        scriptWriter.flush();
        scriptWriter.close();

    }

    private static void loadOIEProps(String oieFilePath)
    {
        String oieProp = null;

        // load the NELL file in memory as a collection
        ArrayList<ArrayList<String>> nellFile =
            FileUtil.genericFileReader(GenerateNewProperties.class.getResourceAsStream(oieFilePath), PATH_SEPERATOR,
                false);

        // iterate the file
        for (ArrayList<String> line : nellFile) {
            oieProp = line.get(1);
            if (!PROPS.contains(oieProp.trim()))
                PROPS.add(oieProp);
        }
    }

}
