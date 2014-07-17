package code.dws.reverb.graphOps;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class Graph {

	private final Map<Integer, Vertex> vertices = new TreeMap<Integer, Vertex>(
			new Comparator<Integer>() {
				// for pretty printing
				@Override
				public int compare(Integer arg0, Integer arg1) {
					return arg0.compareTo(arg1);
				}
			});

	public final List<Edge> edges = new ArrayList<Edge>();

	/**
	 * @return the vertices
	 */
	public Map<Integer, Vertex> getVertices() {
		return vertices;
	}

	public void addVertex(Vertex v) {
		vertices.put(v.getLbl(), v);
	}

	public Vertex getVertex(int lbl) {
		Vertex v;
		if ((v = vertices.get(lbl)) == null) {
			v = new Vertex(lbl);
			addVertex(v);
		}
		return v;
	}
}