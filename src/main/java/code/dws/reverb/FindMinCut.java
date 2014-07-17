package code.dws.reverb;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Scanner;

import code.dws.reverb.graphOps.Edge;
import code.dws.reverb.graphOps.Graph;
import code.dws.reverb.graphOps.Vertex;

/**
 * https://class.coursera.org/algo/quiz/attempt?quiz_id=52
 */
public class FindMinCut {

	private static Map<Integer, Edge> EDGE_MAP = null;

	private static Edge lastEdge = null;

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		int[][] arr = getArray("/input/kargerAdj.txt");
		Map<Integer, Integer> statistics = new LinkedHashMap<Integer, Integer>();

		int min = arr.length;
		int iter = arr.length * arr.length;
		Graph gr = null;

		EDGE_MAP = new HashMap<Integer, Edge>();

		// iterate severl times to find the most likely mincut number
		for (int i = 0; i < arr.length * arr.length; i++) {
			gr = createGraph(arr);

			// printGraph( gr );
			int currMin = minCut(gr);

			min = Math.min(min, currMin);

			Integer counter;
			if ((counter = statistics.get(currMin)) == null) {
				counter = 0;
			}
			statistics.put(currMin, counter + 1);

			EDGE_MAP.put(currMin, lastEdge);
		}

		System.out.println(statistics);
		System.out.println("Min: " + min + " stat: "
				+ (statistics.get(min) * 100 / iter) + "%");
		System.out.println("Edge to be removed = "
				+ EDGE_MAP.get(min).toString());

		performGraphPartition(EDGE_MAP.get(min), arr);
	}

	private static void performGraphPartition(Edge edge, int[][] arr) {
		System.out.println(" in partitioning algo = " + edge);
		Graph gr = createGraph(arr);
		
	}

	/**
	 * main block of the algorithm
	 * 
	 * @param gr
	 * @return
	 */
	public static int minCut(Graph gr) {

		Random rnd = new Random();

		/**
		 * contraction algorithm
		 */
		while (gr.getVertices().size() > 2) {
			Edge edge = gr.edges.remove(rnd.nextInt(gr.edges.size()));
			Vertex v1 = cleanVertex(gr, edge.ends.get(0), edge);
			Vertex v2 = cleanVertex(gr, edge.ends.get(1), edge);
			// contract
			Vertex mergedVertex = new Vertex(v1.getLbl());
			redirectEdges(gr, v1, mergedVertex);
			redirectEdges(gr, v2, mergedVertex);

			gr.addVertex(mergedVertex);
		}
		List<Edge> a = gr.edges;

		lastEdge = new Edge(a.get(0).ends.get(0), a.get(0).ends.get(1));

		return gr.edges.size();
	}

	private static Vertex cleanVertex(Graph gr, Vertex v, Edge e) {
		gr.getVertices().remove(v.getLbl());
		v.edges.remove(e);
		return v;
	}

	private static void redirectEdges(Graph gr, Vertex fromV, Vertex toV) {
		for (Iterator<Edge> it = fromV.edges.iterator(); it.hasNext();) {
			Edge edge = it.next();
			it.remove();
			if (edge.getOppositeVertex(fromV) == toV) {
				// remove self-loop
				toV.edges.remove(edge);
				gr.edges.remove(edge);
			} else {
				edge.replaceVertex(fromV, toV);
				toV.addEdge(edge);
			}
		}
	}

	public static int[][] getArray(String relPath) {

		Map<Integer, List<Integer>> vertices = new LinkedHashMap<Integer, List<Integer>>();
		Scanner scan;
		String sCurrentLine;

		try {

			scan = new Scanner(
					ReverbPreProcessing.class.getResourceAsStream(relPath),
					"UTF-8");

			while (scan.hasNextLine()) {
				sCurrentLine = scan.nextLine();
				String[] split = sCurrentLine.trim().split("(\\s)+");
				List<Integer> adjList = new ArrayList<Integer>();
				for (int i = 1; i < split.length; i++) {
					adjList.add(Integer.parseInt(split[i]) - 1);
				}
				vertices.put(Integer.parseInt(split[0]) - 1, adjList);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		int[][] array = new int[vertices.size()][];
		for (Map.Entry<Integer, List<Integer>> entry : vertices.entrySet()) {
			List<Integer> adjList = entry.getValue();
			int[] adj = new int[adjList.size()];
			for (int i = 0; i < adj.length; i++) {
				adj[i] = adjList.get(i);
			}
			array[entry.getKey()] = adj;
		}
		return array;
	}

	private static Graph createGraph(int[][] array) {
		Graph gr = new Graph();
		for (int i = 0; i < array.length; i++) {
			Vertex v = gr.getVertex(i);
			for (int edgeTo : array[i]) {
				Vertex v2 = gr.getVertex(edgeTo);
				Edge e;
				if ((e = v2.getEdgeTo(v)) == null) {
					e = new Edge(v, v2);
					gr.edges.add(e);
					v.addEdge(e);
					v2.addEdge(e);
				}
			}
		}

		return gr;
	}

	private static void printGraph(Graph gr) {
		System.out.println("Printing graph");
		for (Vertex v : gr.getVertices().values()) {
			System.out.print(v.getLbl() + ":");
			for (Edge edge : v.edges) {
				System.out.print(" " + edge.getOppositeVertex(v).getLbl());
			}
			System.out.println();
		}
	}

	// Adj format to visualize in
	// http://www.cs.rpi.edu/research/groups/pb/graphdraw/headpage.html
	private static void toAdjFormat(int[][] arr) {
		System.out.println(arr.length);
		for (int[] adj : arr) {
			System.out.print(adj.length);
			for (int i : adj) {
				System.out.print(" " + i);
			}
			System.out.println();
		}
	}
}
