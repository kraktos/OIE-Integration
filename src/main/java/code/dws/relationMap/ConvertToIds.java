package code.dws.relationMap;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import code.dws.utils.MapUtils;

//import ca.pfv.spmf.test.MainTestFPGrowth_saveToMemory;

/**
 * class to convert a set of patterns for the predicates to ids.
 * 
 * @author Arnab Dutta
 */
public class ConvertToIds
{

    public final static Logger log = LoggerFactory.getLogger(ConvertToIds.class);

    private static Map<String, Integer> occurancePositionMap = new HashMap<String, Integer>();

    private static String[] finalPositions = null;

    public static void main(String[] args) throws FileNotFoundException, IOException, URISyntaxException
    {

        String fileName = "/output/ds_" + args[0] + "/propertyPaths.log";
        String pred = args[0];

        log.info("File Name {} and property {}", fileName, pred);

        analysePropertyPaths(fileName);

        // MainTestFPGrowth_saveToMemory.setTest(occurancePositionMap);
        // MainTestFPGrowth_saveToMemory.main(new String[] {
        // ""
        // });
    }

    /**
     * iterate the file and get the maximum possible postion of all the items
     * 
     * @param fileName
     * @throws URISyntaxException
     */
    private static void analysePropertyPaths(String fileName) throws URISyntaxException
    {

        ArrayList<ArrayList<String>> file = new ArrayList<ArrayList<String>>();

        try {
            File fileTmp = new File(Discover.class.getResource(fileName).toURI());

            BufferedReader tupleReader = new BufferedReader(new FileReader(fileTmp));

            String line;
            while ((line = tupleReader.readLine()) != null) {
                ArrayList<String> eachLine = new ArrayList<String>();

                String[] arr = line.split("\t");
                for (int idx = 0; idx < arr.length; idx++) {

                    String key = arr[idx];

                    if (!occurancePositionMap.containsKey(key)) {
                        occurancePositionMap.put(key, idx);
                    } else {
                        int value = occurancePositionMap.get(key);
                        occurancePositionMap.put(key, (value < idx) ? idx : value);
                    }

                    eachLine.add(key);
                }
                file.add(eachLine);
            }

            updatePositions(file);

            recreateFile(file);

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    /**
     * recreate the original String file to an id file
     * 
     * @param file
     * @throws IOException
     */
    private static void recreateFile(ArrayList<ArrayList<String>> file) throws IOException
    {
        BufferedWriter idWriter =
            new BufferedWriter(new FileWriter(
                "/home/arnab/Workspaces/SchemaMapping/DataMining/src/ca/pfv/spmf/test/idFormat.log"));

        for (ArrayList<String> line : file) {
            for (String word : line) {
                // System.out.print(occurancePositionMap.get(word) + " ");
                idWriter.write(occurancePositionMap.get(word) + " ");
            }
            idWriter.write("\n");
            // System.out.println();
        }
        idWriter.close();
    }

    /**
     * once positions found, start updating th positions if another item is already sitting in its position
     * 
     * @param file
     */
    private static void updatePositions(ArrayList<ArrayList<String>> file)
    {

        occurancePositionMap = MapUtils.sortByValue(occurancePositionMap);
        log.info(occurancePositionMap.toString());

        finalPositions = new String[occurancePositionMap.size()];

        Comparator<ArrayList<String>> x = new Comparator<ArrayList<String>>()
        {
            @Override
            public int compare(ArrayList<String> s1, ArrayList<String> s2)
            {
                return s2.size() - s1.size();
            }

        };

        Collections.sort(file, x);

        for (ArrayList<String> line : file) {
            for (String word : line) {
                int position = occurancePositionMap.get(word);
                checkAndAdd(position, word);
            }
        }

        updateMap();
    }

    /**
     * once array is full, update the positions back in the original map
     */
    private static void updateMap()
    {
        for (int id = 0; id < finalPositions.length; id++) {
            String key = finalPositions[id];

            occurancePositionMap.put(key, id);
        }
    }

    /**
     * look for positions and collisions, and keep filling the array
     * 
     * @param position
     * @param word
     */
    private static void checkAndAdd(int position, String word)
    {
        if (finalPositions[position] == null)
            finalPositions[position] = word;
        else {
            if (!finalPositions[position].equals(word))
                checkAndAdd(position + 1, word);
        }
    }

}
