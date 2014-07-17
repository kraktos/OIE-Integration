package code.dws.reverb.graphOps;

import java.util.HashSet;
import java.util.Set;

public class Vertex {

	private final int lbl;
	public final Set<Edge> edges = new HashSet<Edge>();

	public Vertex(int lbl) {
		this.lbl = lbl;
	}

	public void addEdge(Edge edge) {
		edges.add(edge);
	}

	public Edge getEdgeTo(Vertex v2) {
		for (Edge edge : edges) {
			if (edge.contains(this, v2))
				return edge;
		}
		return null;
	}

	/**
	 * @return the lbl
	 */
	public int getLbl() {
		return this.lbl;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append(lbl + " ");
		return builder.toString();
	}

}