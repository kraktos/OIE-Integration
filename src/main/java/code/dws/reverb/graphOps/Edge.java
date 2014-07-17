package code.dws.reverb.graphOps;

import java.util.ArrayList;
import java.util.List;

;

public class Edge {

	public final List<Vertex> ends = new ArrayList<Vertex>();

	public Edge(Vertex fst, Vertex snd) {
		if (fst == null || snd == null) {
			throw new IllegalArgumentException("Both vertices are required");
		}
		ends.add(fst);
		ends.add(snd);
	}

	public boolean contains(Vertex v1, Vertex v2) {
		return ends.contains(v1) && ends.contains(v2);
	}

	public Vertex getOppositeVertex(Vertex v) {
		if (!ends.contains(v)) {
			throw new IllegalArgumentException("Vertex " + v.getLbl());
		}
		return ends.get(1 - ends.indexOf(v));
	}

	public void replaceVertex(Vertex oldV, Vertex newV) {
		if (!ends.contains(oldV)) {
			throw new IllegalArgumentException("Vertex " + oldV.getLbl());
		}
		ends.remove(oldV);
		ends.add(newV);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append(this.ends.get(0)).append(" --- ")
				.append(this.ends.get(1));
		return builder.toString();
	}

}