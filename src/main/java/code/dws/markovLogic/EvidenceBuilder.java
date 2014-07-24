/**
 * 
 */

package code.dws.markovLogic;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import code.dws.dao.Pair;
import code.dws.dbConnectivity.DBWrapper;
import code.dws.ontology.GenericConverter;
import code.dws.query.SPARQLEndPointQueryAPI;
import code.dws.reverb.ReverbPropertyReNaming;
import code.dws.utils.Constants;
import code.dws.utils.Constants.OIE;
import code.dws.utils.Utilities;

/**
 * @author Arnab Dutta
 */
public class EvidenceBuilder {

	private String propertyName;
	private List<String> propertyNames;

	public static Map<String, Long> MAP_COUNTER = new HashMap<String, Long>();

	private static Map<String, Pair<String, String>> GS_PAIRS = new HashMap<String, Pair<String, String>>();

	// The input OIE file with raw web extracted data
	static File oieFile = null;

	public EvidenceBuilder(String[] args) throws IOException {

		if (Constants.OIE_IS_NELL) {
			this.propertyName = args[0];
			oieFile = new File(Constants.NELL_DATA_PATH);
			/**
			 * process the full NELL data dump
			 */
			this.processTriple(oieFile, OIE.NELL, ",");

		} else {
			ReverbPropertyReNaming.main(new String[] { "" });

			this.propertyNames = ReverbPropertyReNaming.getReNamedProperties()
					.get(args[0]);

			oieFile = new File(Constants.REVERB_DATA_PATH);

			/**
			 * process the full REVERB data dump
			 */
			this.processTriple(oieFile, OIE.REVERB, ";");
		}
	}

	/**
	 * @param args
	 * @throws IOException
	 */
	public static void main(String[] args) throws IOException {

		EvidenceBuilder evdBuilder = null;

		if (args.length < 1)
			throw (new RuntimeException(
					"Usage : java -jar DS.jar <inputFilePath> <topK>"));
		else {
			// start processing the triples
			evdBuilder = new EvidenceBuilder(args);

		}
	}

	/**
	 * process the OIE input file
	 * 
	 * @param inputFile
	 *            the raw file
	 * @param oieType
	 *            the type of OIE project, NELL or Reverb, different types have
	 *            different format
	 * @param delimit
	 *            delimiter, comma or tab or something else
	 * @throws IOException
	 */
	public void processTriple(File inputFile, OIE oieType, String delimit)
			throws IOException {

		String triple;
		String[] arrStr = null;

		Set<String> termConceptPairSet = new HashSet<String>();

		// initiate DB
		// DBWrapper.init(Constants.GET_WIKI_TITLES_SQL);

		BufferedReader input = null;

		// the file where the evidences for the MLN are written out
		BufferedWriter allEvidenceWriter = new BufferedWriter(new FileWriter(
				Constants.ALL_MLN_EVIDENCE));

		BufferedWriter allEvidenceWriterTop1 = new BufferedWriter(
				new FileWriter(Constants.ALL_MLN_EVIDENCE_T1));

		// the file where the evidences for the MLN are written out
		BufferedWriter goldEvidenceWriter = new BufferedWriter(new FileWriter(
				Constants.GOLD_MLN_EVIDENCE_ALL));

		// load the gold standard file.
		// loadGoldStandard();

		// init DB
		DBWrapper.init(Constants.GET_WIKI_LINKS_APRIORI_SQL);

		if (Constants.OIE_IS_NELL) {
			input = new BufferedReader(new InputStreamReader(
					new FileInputStream(inputFile)));

			// iterate the file from OIE and process each triple at a time
			while ((triple = input.readLine()) != null) {

				// split on the delimiter
				arrStr = triple.split(delimit);

				// if the property is the one we want to sample upon
				if (this.propertyName != null && this.propertyName.length() > 0)
					if (this.propertyName.equals(arrStr[1])) {

						// process them
						this.createEvidences(arrStr[0].replaceAll("\\s+", "_"),
								arrStr[1], arrStr[2], arrStr[3],
								allEvidenceWriterTop1, allEvidenceWriter,
								termConceptPairSet);
					}

			}

		} else {

			for (String prop : this.propertyNames) {

				this.propertyName = prop;

				System.out
						.println("Creating evidence for " + this.propertyName);

				input = new BufferedReader(new InputStreamReader(
						new FileInputStream(inputFile)));

				// iterate the file from OIE and process each triple at a time
				while ((triple = input.readLine()) != null) {

					// split on the delimiter
					arrStr = triple.split(delimit);

					// if the property is the one we want to sample upon
					if (this.propertyName.equals(arrStr[1])) {

						// process them
						this.createEvidences(
								arrStr[0].replaceAll("\\s+", "_")
										.replaceAll(",", "~2C")
										.replaceAll("$", "~24"),
								arrStr[1].replaceAll("\\s+", "_"),
								arrStr[2].replaceAll("\\s+", "_")
										.replaceAll(",", "~2C")
										.replaceAll("$", "~24"), arrStr[3],
								allEvidenceWriterTop1, allEvidenceWriter,
								termConceptPairSet);
					}
				}
			}
		}

		// remove from memory
		termConceptPairSet.clear();

		// close stream writer
		allEvidenceWriterTop1.close();
		allEvidenceWriter.close();
		goldEvidenceWriter.close();

		// flush residuals
		DBWrapper.saveResidualDBPTypes();

		// shutdown DB
		DBWrapper.shutDown();

	}

	/**
	 * create the necessary evidence for running reasoning
	 * 
	 * @param sub
	 * @param prop
	 * @param obj
	 * @param conf
	 * @param oieType
	 * @param allEvidenceWriter
	 * @param termConceptPairSet
	 * @param goldEvidenceWriter
	 * @throws IOException
	 */
	private void createEvidences(String sub, String prop, String obj,
			String conf, BufferedWriter allEvidenceWriterTop1,
			BufferedWriter allEvidenceWriter, Set<String> termConceptPairSet)
			throws IOException {

		String nellSub = null;
		String nellPred = null;
		String nellObj = null;
		double confidence = 0;

		String nellSubPFxd = null;
		String nellObjPFxd = null;

		nellSub = GenericConverter.getInst(sub);
		nellPred = prop;
		nellObj = GenericConverter.getInst(obj);
		confidence = Double.parseDouble(conf);

		// uniquely identify each instance by concating a post fixd number
		nellSubPFxd = generateUniqueURI(nellSub);
		nellObjPFxd = generateUniqueURI(nellObj);

		// create a list of local mapping pair and return it

		/**
		 * create the property assertions
		 */
		allEvidenceWriter.write("propAsstConf(\"NELL#Predicate/" + nellPred
				+ "\", \"NELL#Instance/" + nellSubPFxd + "\", \"NELL#Instance/"
				+ nellObjPFxd + "\", " + confidence + ")\n");

		/**
		 * create the property assertions
		 */
		allEvidenceWriterTop1.write("propAsstConf(\"NELL#Predicate/" + nellPred
				+ "\", \"NELL#Instance/" + nellSubPFxd + "\", \"NELL#Instance/"
				+ nellObjPFxd + "\", " + confidence + ")\n");

		/**
		 * create top-k evidences for subject
		 */
		List<String> mappingsSub = createEvidenceForTopKCandidates(
				allEvidenceWriterTop1, allEvidenceWriter, nellSub, nellSubPFxd,
				termConceptPairSet, Constants.DOMAIN);

		/**
		 * create top-k evidences for object
		 */
		List<String> mappingsObj = createEvidenceForTopKCandidates(
				allEvidenceWriterTop1, allEvidenceWriter, nellObj, nellObjPFxd,
				termConceptPairSet, Constants.RANGE);

	}

	/**
	 * fetch the top-k instances and confidences for the subject
	 * 
	 * @param allEvidenceWriter
	 * @param nellInst
	 * @param nellPostFixdInst
	 * @param termConceptPairSet
	 * @param identifier
	 * @return
	 * @throws IOException
	 */
	public List<String> createEvidenceForTopKCandidates(
			BufferedWriter allEvidenceWriterTop1,
			BufferedWriter allEvidenceWriter, String nellInst,
			String nellPostFixdInst, Set<String> termConceptPairSet,
			String identifier) throws IOException {

		DecimalFormat decimalFormatter = new DecimalFormat("0.00000000");

		List<String> sameAsConfidences;
		String conc;

		// get the top-k concepts, confidence pairs
		sameAsConfidences = DBWrapper.fetchTopKLinksWikiPrepProb(Utilities
				.cleanse(nellInst).replaceAll("\\_+", " "),
				Constants.SAMEAS_TOPK);

		List<String> listMappings = new ArrayList<String>();

		for (String val : sameAsConfidences) {

			// if one instance-dbpedia pair is already in, skip it
			if (!termConceptPairSet.contains(nellPostFixdInst + val)) {

				conc = Utilities.utf8ToCharacter(val.split("\t")[0]);

				generateDBPediaTypeMLN(conc, allEvidenceWriter);

				conc = MLNFileGenerator.removeTags("DBP#resource/"
						+ Utilities.characterToUTF8(conc.replaceAll("~", "%")));

				// write it out to the evidence file
				allEvidenceWriter.write("sameAsConf("
						+ conc
						+ ", \"NELL#Instance/"
						+ nellPostFixdInst
						+ "\", "
						+ decimalFormatter.format(Utilities
								.convertProbabilityToWeight(Double
										.parseDouble(val.split("\t")[1])))
						+ ")\n");

				// add to the list
				// listMappings.add("sameAsConf("
				// + conc
				// + ", \"NELL#Instance/"
				// + nellPostFixdInst
				// + "\", f)");

				listMappings.add(conc);

				termConceptPairSet.add(nellPostFixdInst + val);
			}
		}

		if (sameAsConfidences.size() > 0) {
			// write it out to the evidence file

			conc = Utilities.utf8ToCharacter(sameAsConfidences.get(0).split(
					"\t")[0]);
			conc = MLNFileGenerator.removeTags("DBP#resource/"
					+ Utilities.characterToUTF8(conc.replaceAll("~", "%")));

			allEvidenceWriterTop1.write("sameAsConf("
					+ conc
					+ ", \"NELL#Instance/"
					+ nellPostFixdInst
					+ "\", "
					+ decimalFormatter.format(Utilities
							.convertProbabilityToWeight(Double
									.parseDouble(sameAsConfidences.get(0)
											.split("\t")[1]))) + ")\n");
		}

		// cache the type information for the top most candidate, the 0th
		// element is the most frequent candidate
		// cacheType(sameAsConfidences.get(0), identifier);

		return listMappings;
	}

	/**
	 * create type of DBP instances
	 * 
	 * @param pair
	 * @param isOfTypeEvidenceWriter
	 * @param identifier
	 */
	private void generateDBPediaTypeMLN(String dbPediaInstance,
			BufferedWriter isOfTypeEvidenceWriter) {

		List<String> listTypes;

		String tempInst = null;

		tempInst = Utilities.utf8ToCharacter(dbPediaInstance.replaceAll("~",
				"%"));
		//
		// if(tempInst.indexOf("Liverpool")!= -1)
		// System.out.println("");

		// if(dbPediaInstance.indexOf("The_Ring_") != -1)
		// System.out.println();

		// get DBPedia types
		if (Constants.RELOAD_DBPEDIA_TYPES)
			listTypes = SPARQLEndPointQueryAPI.getInstanceTypes(Utilities
					.utf8ToCharacter(tempInst));

		else { // load cached copy from DB

			listTypes = DBWrapper.getDBPInstanceType(Utilities
					.characterToUTF8(tempInst));
		}

		try {
			if (listTypes.size() == 0
					|| listTypes.get(0).indexOf(Constants.UNTYPED) != -1) {

				isOfTypeEvidenceWriter.write("isOfType(\""
						+ Constants.UNTYPED
						+ "\", "
						+ MLNFileGenerator.removeTags("DBP#resource/"
								+ Utilities.characterToUTF8(tempInst)) + ")\n");
				if (Constants.RELOAD_DBPEDIA_TYPES)
					DBWrapper.saveToDBPediaTypes(
							Utilities.characterToUTF8(tempInst),
							Constants.UNTYPED);

			} else if (listTypes.size() > 0) {

				// get the most specific type
				if (Constants.RELOAD_DBPEDIA_TYPES)
					listTypes = SPARQLEndPointQueryAPI.getLowestType(listTypes);

				for (String type : listTypes) {
					isOfTypeEvidenceWriter.write("isOfType(\"DBP#ontology/"
							+ type
							+ "\", "
							+ MLNFileGenerator.removeTags("DBP#resource/"
									+ Utilities.characterToUTF8(tempInst))
							+ ")\n");

					// for faster future processing, store types in Database,
					// flip side is this may be old data, so change CONFIG
					// parameters to
					// reload fresh data and should be run once in a week or
					// so..
					if (Constants.RELOAD_DBPEDIA_TYPES)
						DBWrapper.saveToDBPediaTypes(
								Utilities.characterToUTF8(tempInst), type);
				}
			}

		} catch (IOException e) {
			System.err.println("Exception in generateDBPediaTypeMLN() "
					+ e.getMessage());
		}
	}

	/**
	 * takes a nell/reverb instance and creates an unique URI out of it. So if
	 * multiple times an entity occurs, each one will have different uris.
	 * 
	 * @param nellInst
	 * @param classInstance
	 * @param elements2
	 * @param elements
	 * @return
	 */
	private static String generateUniqueURI(String nellInst) {
		// check if this URI is already there
		if (MAP_COUNTER.containsKey(nellInst)) {
			long value = MAP_COUNTER.get(nellInst);
			MAP_COUNTER.put(nellInst, value + 1);

			// create an unique URI because same entity already has been
			// encountered before
			nellInst = nellInst + Constants.POST_FIX
					+ String.valueOf(value + 1);

		} else {
			MAP_COUNTER.put(nellInst, 1L);
		}

		return nellInst;
	}

	// /**
	// * load the gold standard file in memory
	// *
	// * @throws IOException
	// */
	// private void loadGoldStandard() throws IOException {
	// BufferedReader tupleReader = new BufferedReader(new FileReader(
	// Constants.INPUT_CSV_FILE));
	// String line;
	// String key = null;
	// Pair<String, String> valuePair = null;
	//
	// while ((line = tupleReader.readLine()) != null) {
	//
	// key = line.split("\t")[0] + "\t" + line.split("\t")[2];
	// valuePair = new Pair<String, String>(line.split("\t")[5],
	// line.split("\t")[6]);
	//
	// GS_PAIRS.put(key, valuePair);
	// }
	//
	// System.out
	// .println("Loaded Gold Standard MAP size = " + GS_PAIRS.size());
	// }
}
