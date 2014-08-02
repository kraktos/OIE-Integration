/**
 * 
 */

package code.dws.utils;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
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

import code.dws.dbConnectivity.DBWrapper;
import code.dws.experiment.ExperimentAutomation;
import code.dws.reverb.ReverbPropertyReNaming;

/**
 * this class reads the refined output files for each of the NELL property
 * files. and stores the mappings into Database
 * 
 * @author arnab
 */
public class DBPMappingsLoader {
	static String PREDICATE;

	static Map<String, String> SAMEAS = new HashMap<String, String>();

	/**
	 * @param args
	 * @throws IOException
	 */
	public static void main(String[] args) throws IOException {

		ExperimentAutomation.loadConfigParameters(new String[] { "", args[0] });

		String clusterName = null;

		DBWrapper.init(Constants.UPDT_OIE_POSTFIXED);

		if (Constants.OIE_IS_NELL) {
			PREDICATE = args[0];
			readOutputFiles();
		} else {

			// load the cluster names and reverb properties
			ReverbPropertyReNaming.main(new String[] { "" });

			Path filePath = Paths.get("src/main/resources/output/");

			final List<Path> files = new ArrayList<Path>();
			FileVisitor<Path> fv = new SimpleFileVisitor<Path>() {
				@Override
				public FileVisitResult visitFile(Path file,
						BasicFileAttributes attrs) throws IOException {
					if (file.endsWith("out.db"))
						files.add(file);
					return FileVisitResult.CONTINUE;
				}
			};

			try {
				// gets the only relevant output files
				Files.walkFileTree(filePath, fv);

				// iterate the files
				for (Path path : files) {
					clusterName = path.getParent().toString()
							.replaceAll("src/main/resources/output/", "")
							.replaceAll("ds_", "");

					PREDICATE = clusterName;

					readOutputFiles();
				}
			} catch (IOException e) {
				e.printStackTrace();
			} finally {
				DBWrapper.updateResidualOIERefined();
				DBWrapper.shutDown();
			}
		}
	}

	/**
	 * reads the particular output file defined by the PREDICATE variable
	 * 
	 * @throws IOException
	 */
	private static void readOutputFiles() throws IOException {
		String path = Constants.sample_dumps + PREDICATE + "/out.db";

		long t1 = System.currentTimeMillis();

		@SuppressWarnings("resource")
		BufferedReader mappingsReader = new BufferedReader(
				new InputStreamReader(new FileInputStream(path)));

		String triple;
		String value;
		String key;

		String nSub;
		String nProp;
		String nObj;

		String dbpSVal;
		String dbpOVal;

		String[] arr;

		while ((triple = mappingsReader.readLine()) != null) {
			if (triple.startsWith("sameAs")) {

				arr = triple.split("\"");
				if (arr.length == 5) {

					value = arr[1].replaceAll("DBP#resource/", "");
					key = arr[3].replaceAll("NELL#Instance/", "");

					if (!SAMEAS.containsKey(key)) {
						SAMEAS.put(key, value);
					}
				}
			}
		}

		long t2 = System.currentTimeMillis();
		System.out.println("done with same as = " + (t2 - t1));
		mappingsReader = new BufferedReader(new InputStreamReader(
				new FileInputStream(path)));

		while ((triple = mappingsReader.readLine()) != null) {

			if (triple.startsWith("propAsst")) {
				arr = triple.split("\"");

				if (arr.length == 7) {

					nProp = arr[1].replaceAll("NELL#Predicate/", "")
							.replaceAll("_", " ");

					nSub = arr[3].replaceAll("NELL#Instance/", "");
					nObj = arr[5].replaceAll("NELL#Instance/", "");

					if (SAMEAS.containsKey(nSub))
						dbpSVal = SAMEAS.get(nSub);
					else
						dbpSVal = null;

					if (SAMEAS.containsKey(nObj))
						dbpOVal = SAMEAS.get(nObj);
					else
						dbpOVal = null;

					dbpOVal = (dbpOVal != null ? Utilities
							.utf8ToCharacter(dbpOVal.replaceAll("~", "%"))
							.replaceAll("\\[", "\\(").replaceAll("\\]", "\\)")
							: dbpOVal);

					dbpSVal = (dbpSVal != null ? Utilities
							.utf8ToCharacter(dbpSVal.replaceAll("~", "%"))
							.replaceAll("\\[", "\\(").replaceAll("\\]", "\\)")
							: dbpSVal);

					if (dbpSVal != null || dbpOVal != null)
						updateDB(nSub, nProp, nObj, dbpSVal, dbpOVal);

				}
			}
		}

		System.out.println("one read takes = "
				+ (System.currentTimeMillis() - t1));
	}

	private static void updateDB(String nSub, String pred, String nObj,
			String dbpSVal, String dbpOVal) {

		DBWrapper.updateOIEPostFxd(nSub, pred, nObj, dbpSVal, dbpOVal);

	}
}
