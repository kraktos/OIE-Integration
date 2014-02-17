/**
 * 
 */
package code.dws.relationMap;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.io.InputStream;
import java.util.Scanner;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This takes in the property hopped log files and tries to generate new properties
 * 
 * @author Arnab Dutta
 */
public class GenerateNewProperties
{
    public final static Logger log = LoggerFactory.getLogger(GenerateNewProperties.class);

    private static final String PROPERTY_LOGS_PATH = "./src/main/resources/output";

    /**
     * @param args
     */
    public static void main(String[] args)
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
