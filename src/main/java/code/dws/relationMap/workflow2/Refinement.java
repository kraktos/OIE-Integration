package code.dws.relationMap.workflow2;

import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import code.dws.query.SPARQLEndPointQueryAPI;
import code.dws.utils.FileUtil;
import code.dws.utils.Utilities;

import com.hp.hpl.jena.query.QuerySolution;

public class Refinement {

	public Refinement() {
	}

	public static void main(String[] args) throws IOException {
		// read the file into memory
		ArrayList<ArrayList<String>> directPropsFile = FileUtil
				.genericFileReader(
						new FileInputStream(
								"/home/adutta/git/OIE-Integration/src/main/resources/input/NEW_TRIPLES_REVERB_DIRECT_W_DBP_CLUSTER.tsv"),
						"\t", false);

		String dbpSub = null;
		String dbpObj = null;
		String dbpProp = null;

		String query = null;
		String val = null;

		boolean exists = false;

		// write transactions to the file for analysis
		BufferedWriter triplesWriter = new BufferedWriter(
				new FileWriter(
						"/home/adutta/git/OIE-Integration/src/main/resources/input/NEW_NEW_TRIPLES_REVERB_DIRECT_W_DBP_CLUSTER.tsv"));

		BufferedWriter triplesWriter2 = new BufferedWriter(
				new FileWriter(
						"/home/adutta/git/OIE-Integration/src/main/resources/input/BAD_TRIPLES_REVERB_DIRECT_W_DBP_CLUSTER.tsv"));

		for (ArrayList<String> line : directPropsFile) {
			exists = false;
			// System.out.println(line);
			dbpSub = line.get(3);
			dbpProp = line.get(4);
			dbpObj = line.get(5);
			// System.out.println(Utilities.utf8ToCharacter(dbpSub) + "\t" +
			// dbpProp + "\t" + Utilities.utf8ToCharacter(dbpObj));

			if (line.get(0).indexOf("Mocha") != -1)
				System.out.println();

			query = "select ?val where {<" + Utilities.utf8ToCharacter(dbpSub)
					+ "> ?val <" + Utilities.utf8ToCharacter(dbpObj) + ">}";
			List<QuerySolution> s;
			try {
				s = SPARQLEndPointQueryAPI.queryDBPediaEndPoint(query);

				for (QuerySolution a : s) {
					val = a.get("val").toString();
					if (val.indexOf(dbpProp) != -1) {
						exists = true;
					}
				}

				if (!exists) {
					triplesWriter.write(line.get(0) + "\t" + line.get(1) + "\t"
							+ line.get(2) + "\t" + line.get(3) + "\t"
							+ line.get(4) + "\t" + line.get(5) + "\n");

					System.out.println(line);
					triplesWriter.flush();
				} else {
					triplesWriter2.write(line.get(0) + "\t" + line.get(1)
							+ "\t" + line.get(2) + "\t" + line.get(3) + "\t"
							+ line.get(4) + "\t" + line.get(5) + "\n");
					triplesWriter2.flush();
				}

			} catch (Exception e) {
			}

		}

		triplesWriter.close();
		triplesWriter2.close();
	}
}
