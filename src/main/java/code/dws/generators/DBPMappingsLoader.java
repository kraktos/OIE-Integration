/**
 * 
 */
package code.dws.generators;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;

import code.dws.utils.Constants;

/**
 * this class reads the refined output files for each of the NELL property files. and stores the mappings into Database
 * 
 * @author arnab
 */
public class DBPMappingsLoader
{
    static String PREDICATE;

    /**
     * @param args
     * @throws IOException
     */
    public static void main(String[] args) throws IOException
    {
        PREDICATE = args[0];
        readOutputFiles();
    }

    private static void readOutputFiles() throws IOException
    {
        String path = Constants.sample_dumps + PREDICATE + "/out.db";

        BufferedReader mappingsReader = new BufferedReader(new InputStreamReader(new FileInputStream(path)));

        String triple;
        while ((triple = mappingsReader.readLine()) != null) {
            System.out.println(triple);
        }

    }
}
