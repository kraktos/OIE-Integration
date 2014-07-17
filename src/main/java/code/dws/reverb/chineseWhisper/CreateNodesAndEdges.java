package code.dws.reverb.chineseWhisper;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.lang3.tuple.Pair;

import code.dws.reverb.KClustersAlgo;
import code.dws.utils.Constants;

public class CreateNodesAndEdges {

	public CreateNodesAndEdges() {

	}

	public static void main(String[] args) throws IOException {

		BufferedWriter writerNodes = new BufferedWriter(new FileWriter(
				"nodes.txt"));
		BufferedWriter writerEdges = new BufferedWriter(new FileWriter(
				"edges.txt"));

		KClustersAlgo.init();

		Map<String, Integer> keys = new HashMap<String, Integer>();

		int cnt = 1;

		for (Entry<Pair<String, String>, Double> e : KClustersAlgo
				.getScoreMap().entrySet()) {
			// System.out.println(e.getKey() + "\t" + e.getValue());

			// writerEdges.write(e.getKey().getLeft().hashCode() + "\t"
			// + e.getKey().getRight().hashCode() + "\t" + e.getValue()
			// + "\n");
			//
			// writerNodes.write(e.getKey().getLeft().hashCode() + "\t"
			// + e.getKey().getLeft() + "\n");
			// writerNodes.write(e.getKey().getRight().hashCode() + "\t"
			// + e.getKey().getRight() + "\n");
			//

			if (!keys.containsKey(e.getKey().getLeft())) {

				keys.put(e.getKey().getLeft(), cnt);
				writerNodes.write(cnt + "\t" + e.getKey().getLeft() + "\n");

				cnt++;

			} else {
				// cnt = keys.get(e.getKey().getLeft());
			}

			if (!keys.containsKey(e.getKey().getRight())) {
				keys.put(e.getKey().getRight(), cnt);
				writerNodes.write(cnt + "\t" + e.getKey().getRight() + "\n");
				cnt++;

			} else {
				// cnt = keys.get(e.getKey().getLeft());
			}

			writerEdges.write(keys.get(e.getKey().getLeft()) + "\t"
					+ keys.get(e.getKey().getRight()) + "\t"
					+ Constants.formatter.format(e.getValue()) + "\n");

			/*
			 * if (!hashTab.contains(e.getKey().getLeft().hashCode())) {
			 * writerNodes.write(e.getKey().getLeft().hashCode() + "\t" +
			 * e.getKey().getLeft() + "\n");
			 * 
			 * hashTab.put(e.getKey().getLeft().hashCode(), e.getKey()
			 * .getLeft()); }
			 * 
			 * if (!hashTab.contains(e.getKey().getRight().hashCode())) {
			 * writerNodes.write(e.getKey().getRight().hashCode() + "\t" +
			 * e.getKey().getRight() + "\n");
			 * 
			 * hashTab.put(e.getKey().getRight().hashCode(), e.getKey()
			 * .getRight()); } writerEdges.write(e.getKey().getLeft().hashCode()
			 * + "\t" + e.getKey().getRight().hashCode() + "\t" + e.getValue() +
			 * "\n");
			 */

		}
		writerEdges.flush();
		writerNodes.flush();

		writerEdges.close();
		writerNodes.close();

	}
}
