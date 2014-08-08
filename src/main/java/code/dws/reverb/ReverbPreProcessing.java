/**
 * 
 */
package code.dws.reverb;

import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.List;
import java.util.Scanner;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tartarus.snowball.ext.PorterStemmer;

import code.dws.dbConnectivity.DBWrapper;
import code.dws.query.SPARQLEndPointQueryAPI;
import code.dws.utils.Constants;
import code.dws.utils.Utilities;

/**
 * this class takes a rax Reverb input, gets the type of top-1 candidate for
 * each instance, and dumps into a file for further analysis
 * 
 * @author arnab
 */
public class ReverbPreProcessing {

	// define class logger
	public final static Logger log = LoggerFactory
			.getLogger(ReverbPreProcessing.class);

	// Reverb original triples file
	private static final String REVERB_FILE = "src/main/resources/input/highConfidenceReverbData.csv"; // uniq_props.txt";

	// defines the top-k candidate to be fetched for each NELL term
	private static final int SAMEAS_TOPK = 1;

	static BufferedWriter outputWriter;

	static String DELIMIT = ";";

	/*
	 * remove some prepositions
	 */
	static String regex = "\\ban?\\b|\\bbe\\b|\\bof\\b|\\bfor\\b|\\bin\\b"
			+ "|\\bat\\b|\\bby\\b|\\bto\\b|\\bthe\\b|\\bfrom\\b|\\bon\\b|\\b-\\b";

	/**
     * 
     */
	public ReverbPreProcessing() {
		Constants.USE_LOGIT_FUNC = true;
		Constants.BATCH_SIZE = 10000;
	}

	/**
	 * @param args
	 * @throws IOException
	 */
	public static void main(String[] args) throws IOException {
		// readPropFiles(REVERB_PROP_FILE);
		readTriples(REVERB_FILE);
	}

	@SuppressWarnings("resource")
	public static void readTriples(String filePath) throws IOException {
		FileInputStream inputStream = null;

		String sCurrentLine;
		String[] strArr = null;
		String revSubj = null;
		String revProp = null;
		String revObj = null;

		List<String> candidateSubjs = null;
		List<String> candidateObjs = null;

		String subType = null;
		String objType = null;

		double simScoreSubj = 0;
		double simScoreObj = 0;

		// outputWriter = new BufferedWriter(new FileWriter(OUTPUT));

		try {
			Constants.USE_LOGIT_FUNC = true;
			// init DB
			DBWrapper.init(Constants.GET_WIKI_LINKS_APRIORI_SQL);

			Scanner scan;
			scan = new Scanner(new FileInputStream(filePath), "UTF-8");

			while (scan.hasNextLine()) {

				sCurrentLine = scan.nextLine();

				strArr = sCurrentLine.split(DELIMIT);
				revSubj = strArr[0].trim();
				revProp = stemTerm(strArr[1].trim());
				revObj = strArr[2].trim();

				// get the top-k concepts for the subject
				candidateSubjs = DBWrapper.fetchTopKLinksWikiPrepProb(Utilities
						.cleanse(revSubj).replaceAll("\\_+", " ").trim(),
						SAMEAS_TOPK);
				// get the top-k concepts for the object
				candidateObjs = DBWrapper.fetchTopKLinksWikiPrepProb(Utilities
						.cleanse(revObj).replaceAll("\\_+", " ").trim(),
						SAMEAS_TOPK);

				if (candidateSubjs.size() > 0) {
					// subType =
					// getTypeInfo(candidateSubjs.get(0).split("\t")[0]);
					simScoreSubj = Double.parseDouble(candidateSubjs.get(0)
							.split("\t")[1]);
				}

				if (candidateObjs.size() > 0) {
					// objType =
					// getTypeInfo(candidateObjs.get(0).split("\t")[0]);
					simScoreObj = Double.parseDouble(candidateObjs.get(0)
							.split("\t")[1]);
				}

				// flush it to DB
				DBWrapper.saveReverbTypeWeights(simScoreSubj, simScoreObj,
						subType, revSubj, strArr[1].trim(), revProp, revObj,
						objType);

				candidateSubjs = null;
				candidateObjs = null;
				subType = null;
				objType = null;
				simScoreSubj = 0;
				simScoreObj = 0;

			}

			DBWrapper.saveResidualReverbData();

		} finally {
			if (inputStream != null) {
				inputStream.close();
			}

			// shutdown DB
			DBWrapper.shutDown();

		}
	}

	@SuppressWarnings("finally")
	private static String getTypeInfo(String inst) {
		String mostSpecificVal = null;

		List<String> types = SPARQLEndPointQueryAPI.getInstanceTypes(Utilities
				.utf8ToCharacter(inst));

		try {
			mostSpecificVal = SPARQLEndPointQueryAPI.getLowestType(types)
					.get(0);
		} catch (IndexOutOfBoundsException e) {
		} finally {
			return mostSpecificVal;
		}
	}

	/**
	 * stem every predicate to reduce to base form
	 * 
	 * @param term
	 * @return
	 */

	private static String stemTerm(String term) {
		PorterStemmer stem = new PorterStemmer();
		stem.setCurrent(term);
		stem.stem();
		return stem.getCurrent();
	}

}
