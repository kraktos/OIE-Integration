/**
 * 
 */
package code.dws.generators;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import code.dws.relationMap.GenerateNewProperties;
import code.dws.reverb.ReverbPropertyReNaming;
import code.dws.utils.Constants;
import code.dws.utils.FileUtil;

/**
 * @author arnab
 */
public class ScriptGenarator {

	// define class logger
	public final static Logger log = LoggerFactory
			.getLogger(ScriptGenarator.class);

	// data separator of the NELL data file
	private static final String PATH_SEPERATOR = ",";

	static List<String> PROPS = new ArrayList<String>();

	private static final String SHELL_SCRIPT = "FULL_SCRIPT.sh";

	private static final int MAX_BOOT_ITER = 6;

	private static final String PIPELINE_NAME = (Constants.OIE_IS_NELL) ? "PIPELINE_ALL_NELL.sh "
			: "PIPELINE_ALL_REVERB.sh ";

	private static final String BOOTSTRAP_NAME = (Constants.OIE_IS_NELL) ? "BOOTSTRAP_ALL_NELL.sh "
			: "BOOTSTRAP_ALL_REVERB.sh ";

	/**
	 * @param args
	 * @throws IOException
	 */
	public static void main(String[] args) throws IOException {

		loadOIEProps(GenerateNewProperties.NELL_FILE_PATH);

		generateScript();
	}

	private static void generateScript() throws IOException {
		BufferedWriter scriptWriter = new BufferedWriter(new FileWriter(
				SHELL_SCRIPT));

		scriptWriter.write("#!/bin/bash\n\n");

		for (String oieProp : PROPS) {
			int bootIter = 2;
			scriptWriter.write("sh ./" + PIPELINE_NAME + oieProp + "\n");
//			while (bootIter != MAX_BOOT_ITER + 2) {
//				scriptWriter.write("sh ./" + BOOTSTRAP_NAME + oieProp + " "						+ bootIter++ + "\n");
//			}
			scriptWriter.write("echo \"Done with complete reasoning of "
					+ oieProp + "\"\n\n");

			// System.out
			// .println("java -jar /home/arnab/Workspaces/UPDATE_REFINED.jar " +
			// oieProp);
			System.out.println("echo \"Done with " + oieProp + "\"\n");
		}

		scriptWriter.flush();
		scriptWriter.close();
	}

	/**
	 * load the valid properties in which we are interested. for Nell its easy,
	 * for Reverb this comes from the clustered ones
	 * 
	 * @param oieFilePath
	 */
	private static void loadOIEProps(String oieFilePath) {
		String oieProp = null;

		if (Constants.OIE_IS_NELL) {
			// load the NELL file in memory as a collection
			ArrayList<ArrayList<String>> nellFile = FileUtil.genericFileReader(
					GenerateNewProperties.class
							.getResourceAsStream(oieFilePath), PATH_SEPERATOR,
					false);

			// iterate the file
			for (ArrayList<String> line : nellFile) {
				oieProp = line.get(1);
				if (!PROPS.contains(oieProp.trim()))
					PROPS.add(oieProp);
			}

			log.info("Loaded all properties from "
					+ GenerateNewProperties.NELL_FILE_PATH + ";  "
					+ PROPS.size());

		} else {
			try {
				ReverbPropertyReNaming.main(new String[] { "" });

				for (Entry<String, List<String>> e : ReverbPropertyReNaming
						.getReNamedProperties().entrySet()) {
					PROPS.add(e.getKey());
				}
			} catch (IOException e1) {
				e1.printStackTrace();
			}
		}
	}
}
