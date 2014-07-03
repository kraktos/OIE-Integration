package code.dws.wordnet;

import java.util.ArrayList;
import java.util.List;

import edu.smu.tspell.wordnet.*;

/**
 * Displays word forms and definitions for synsets containing the word form specified on the command line. To use this
 * application, specify the word form that you wish to view synsets for, as in the following example which displays all
 * synsets containing the word form "airplane": <br>
 * java TestJAWS airplane
 */
public class WordNetAPI
{
    static {
        System.setProperty("wordnet.database.dir", "/home/arnab/Work/data/Wordnet/WordNet-3.0/dict/");
    }

    /**
     * Main entry point. The command-line arguments are concatenated together (separated by spaces) and used as the word
     * form to look up.
     */
    public static void main(String[] args)
    {

        if (args.length > 0) {
            // Concatenate the command-line arguments
            StringBuffer buffer = new StringBuffer();
            for (int i = 0; i < args.length; i++) {
                buffer.append((i > 0 ? " " : "") + args[i]);
            }
            String wordForm = buffer.toString();

            getSynonyms(wordForm);

        } else {
            // System.err.println("You must specify " + "a word form for which to retrieve synsets.");
        }
    }

    /**
     * @param wordForm
     * @return
     * @throws WordNetException
     */
    public static List<String> getSynonyms(String wordForm) throws WordNetException
    {
        List<String> result = null;

        // Get the synsets containing the wrod form
        WordNetDatabase database = WordNetDatabase.getFileInstance();

        Synset[] synsets = database.getSynsets(wordForm);

        // Display the word forms and definitions for synsets retrieved
        if (synsets.length > 0) {

            result = new ArrayList<String>();

            for (int i = 0; i < synsets.length; i++) {

                String[] wordForms = synsets[i].getWordForms();
                for (int j = 0; j < wordForms.length; j++) {

                    result.add(wordForms[j]);
                    // System.out.print((j > 0 ? ", " : "") + wordForms[j]);
                }
                // System.out.println(": " + synsets[i].getDefinition());
            }
        }

        return (result != null) ? result : new ArrayList<String>();
    }

}
