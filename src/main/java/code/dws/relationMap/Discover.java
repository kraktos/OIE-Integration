/**
 * 
 */

package code.dws.relationMap;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import code.dws.extractor.RelationExplorer;
import code.dws.reverb.ReverbPropertyReNaming;
import code.dws.utils.Constants;
import code.dws.utils.Utilities;

/**
 * Class to find related predicates between two given dbp entities
 * 
 * @author Arnab Dutta
 */
public class Discover {

	public final static Logger log = LoggerFactory.getLogger(Discover.class);

	static Map<String, String> SAMEAS_MAP = new HashMap<String, String>();

	@SuppressWarnings("resource")
	private static void createPropertyPaths(String mappingFile,
			List<ArrayList<String>> propertyPaths, double hops,
			String predicate, boolean needInverse) throws IOException,
			URISyntaxException {

		Scanner scan;
		scan = new Scanner(new File(mappingFile), "UTF-8");

		String line;

		String oieSubj = null;
		String oieObj = null;
		String dbSubj = null;
		String dbObj = null;

		Matcher matcher = null;
		Pattern pattern = Pattern.compile("sameAs\\((.*?)\\)");

		BufferedWriter propPathWriter = new BufferedWriter(new FileWriter(
				predicate
						+ ((needInverse) ? "_invPropertyPaths.log"
								: "_propertyPaths.log")));

		while (scan.hasNextLine()) {

			line = scan.nextLine();
			matcher = pattern.matcher(line);

			while (matcher.find()) {
				line = line.replaceAll("sameAs\\(", "").replaceAll("\\)", "");

				SAMEAS_MAP.put(line.split(",")[1].trim(),
						line.split(",")[0].trim());
			}
		}

		pattern = Pattern.compile("propAsst\\((.*?)\\)");
		scan = new Scanner(new File(mappingFile), "UTF-8");

		scan = new Scanner(new File(mappingFile), "UTF-8");

		while (scan.hasNextLine()) {
			line = scan.nextLine();

			matcher = pattern.matcher(line);

			while (matcher.find()) {
				line = line.replaceAll("propAsst\\(", "").replaceAll("\\)", "");
				oieSubj = line.split(",")[1].trim();
				oieObj = line.split(",")[2].trim();

				dbSubj = SAMEAS_MAP.get(oieSubj);
				dbObj = SAMEAS_MAP.get(oieObj);

				// we are ready to explore the graph
				// for each pair explore the relations

				ArrayList<String> p = (!needInverse) ? doGraphExploration(
						dbSubj, dbObj, hops) : doGraphExploration(dbObj,
						dbSubj, hops);

				if (p.size() > 0) {
					log.info(dbSubj + "\t" + dbObj + "\t" + p.toString());
				}

				if (p.size() > 0) {
					propPathWriter.write(predicate + "\t");
					for (String path : p)
						propPathWriter.write(path + "\t");

					propPathWriter.write("\n");
					propPathWriter.flush();
				}
			}
		}

		propPathWriter.close();
	}

	private static ArrayList<String> doGraphExploration(String dbSubj,
			String dbObj, double hops) {

		ArrayList<String> paths = new ArrayList<String>();

		if (dbSubj != null && dbObj != null) {

			// some cleanup
			dbSubj = cleanUp(dbSubj);
			dbObj = cleanUp(dbObj);

			log.debug("Exploring for " + dbSubj + "\t" + dbObj);

			// make call to relation finder
			RelationExplorer relExp = new RelationExplorer(dbSubj, dbObj,
					(int) hops);
			paths = relExp.init();
			if (paths.size() > 0) {
				log.debug(paths.toString());
			}
		}
		return paths;

	}

	private static String cleanUp(String input) {
		return Utilities.cleanTerms(input)
				.replaceAll("DBP#resource/", Constants.DBPEDIA_INSTANCE_NS)
				.replaceAll("~", "%").replaceAll("\\*", "'")
				.replaceAll("DBP#resource/", Constants.DBPEDIA_INSTANCE_NS)
				.replaceAll("\\[", "\\(").replaceAll("\\]", "\\)");
	}

	/**
	 * @param args
	 * @throws IOException
	 * @throws URISyntaxException
	 */
	public static void main(String[] args) throws IOException,
			URISyntaxException {

		// create a collection of property paths to be generated
		List<ArrayList<String>> propertyPaths = new ArrayList<ArrayList<String>>();

		// the interested OIE predicate
		String predicate = args[0];

		// number of node jumps interested in
		double hops = Double.valueOf(args[1]);

		// determines if direct or inverse properties
		boolean mode = (args[2].equals("T")) ? true : false;

		String mappingFile = "/output/ds_" + predicate + "/outT3_IT6.db";

		createPropertyPaths(mappingFile, propertyPaths, hops, predicate, mode);

	}
}
