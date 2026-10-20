package graph;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Vertices are numbered 0 through vertexCount - 1; all weights must be nonnegative. */
public final class WeightedGraph {
    private final boolean directed;
    private final List<Edge> edges = new ArrayList<>();
    private final List<List<Edge>> adjacency = new ArrayList<>();

    public WeightedGraph(int vertexCount, boolean directed) {
        if (vertexCount < 1) throw new IllegalArgumentException("At least one vertex required");
        this.directed = directed;
        for (int i = 0; i < vertexCount; i++) adjacency.add(new ArrayList<>());
    }

    public void addEdge(int from, int to, int weight) {
        if (from < 0 || to < 0 || from >= vertexCount() || to >= vertexCount()) {
            throw new IllegalArgumentException("Vertex out of range");
        }
        if (weight < 0) throw new IllegalArgumentException("Dijkstra requires nonnegative weights");
        Edge edge = new Edge(from, to, weight);
        edges.add(edge);
        adjacency.get(from).add(edge);
        if (!directed) adjacency.get(to).add(new Edge(to, from, weight));
    }

    public int vertexCount() { return adjacency.size(); }
    public int edgeCount() { return edges.size(); }
    public boolean isDirected() { return directed; }
    public List<Edge> edges() { return Collections.unmodifiableList(edges); }
    public List<Edge> neighbors(int vertex) { return Collections.unmodifiableList(adjacency.get(vertex)); }
}
