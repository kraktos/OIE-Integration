/**
 * 
 */
package code.dws.reverb.mln;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Scanner;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.collections.CollectionUtils;

import code.dws.reverb.ReverbPreProcessing;

/**
 * class to read the MAP state output and create clusters
 * 
 * @author adutta
 * 
 */
public class MLNOutputParser {

	private static final String MLN_MAP_STATE = "/input/reverb.output";

	private static Map<Long, List<String>> clustersMap = new ConcurrentHashMap<Long, List<String>>();

	/**
	 * @param args
	 */
	public static void main(String[] args) {

		Map<String, String> mapPairs = loadInMemory();

		// create clusters
		getClusters(mapPairs);

		// print it
		printClusters();

	}

	/**
	 * prints out the clusters in a fancy way
	 */
	private static void printClusters() {
		int k = 1;
		for (Entry<Long, List<String>> e1 : clustersMap.entrySet()) {
			System.out.println(" \n************  Cluster " + k++
					+ " **************** ");
			for (String val : e1.getValue()) {
				System.out.println("\t" + val);
			}
		}
	}

	/**
	 * re combine any left out values, reduce redundant clusters
	 */
	@SuppressWarnings("unchecked")
	private static void smoothenCluster() {
		List<String> temp = null;

		for (Entry<Long, List<String>> e1 : clustersMap.entrySet()) {
			for (Entry<Long, List<String>> e2 : clustersMap.entrySet()) {

				if (e1.getKey() < e2.getKey()) {

					if (CollectionUtils.intersection(e1.getValue(),
							e2.getValue()).size() > 0) {

						temp = (List<String>) CollectionUtils.union(
								clustersMap.get(e1.getKey()),
								clustersMap.get(e2.getKey()));

						clustersMap.remove(e2.getKey());
						clustersMap.put(e1.getKey(), temp);
					}
				}
			}
		}

	}

	/**
	 * find a connected path of values within pairs of value,
	 * 
	 * @param mapPairs
	 */
	private static void getClusters(Map<String, String> mapPairs) {
		List<String> clusterList = null;

		long cnt = 1;
		for (Entry<String, String> e : mapPairs.entrySet()) {
			clusterList = new ArrayList<String>();

			if (mapPairs.containsKey(e.getKey())) {

				clusterList.add(e.getKey());
				clusterList.add(e.getValue());

				mapPairs.remove(e.getKey());

				clusterList = getRest(e.getValue(), clusterList, mapPairs);

				clustersMap.put(cnt++, clusterList);

			}
		}

		// unify excess clusters
		smoothenCluster();

	}

	/*
	 * recursive function to automatically hop from one pair to other
	 */
	private static List<String> getRest(String key, List<String> clusterList,
			Map<String, String> mapPairs) {
		String va = null;
		while (mapPairs.get(key) != null) {
			va = mapPairs.get(key);
			clusterList.add(va);
			mapPairs.remove(key);

			getRest(va, clusterList, mapPairs);
		}
		return clusterList;
	}

	/**
	 * read the MLN output file in memory
	 * 
	 * @return
	 */
	@SuppressWarnings("resource")
	private static Map<String, String> loadInMemory() {

		String sCurrentLine;
		Scanner scan;
		scan = new Scanner(
				ReverbPreProcessing.class.getResourceAsStream(MLN_MAP_STATE),
				"UTF-8");

		Map<String, String> mapPairValues = new ConcurrentHashMap<String, String>();
		while (scan.hasNextLine()) {

			sCurrentLine = scan.nextLine();
			sCurrentLine = sCurrentLine.replaceAll("sameAsPropWN\\(", "")
					.replaceAll("\\)", "").replaceAll("\"", "");

			mapPairValues.put(sCurrentLine.split(", ")[0],
					sCurrentLine.split(", ")[1]);
			System.out.println(sCurrentLine);
		}

		return mapPairValues;
	}
}
