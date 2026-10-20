package mst;

import dsu.DisjointSetUnion;
import graph.Edge;
import graph.WeightedGraph;
import java.util.*;

public final class Kruskal {
    private Kruskal() { }
    public static MSTResult compute(WeightedGraph g) {
        if (g.isDirected()) throw new IllegalArgumentException("MST requires undirected graph");
        long[] comparisons = {0};
        List<Edge> sorted = new ArrayList<>(g.edges());
        sorted.sort((a, b) -> {
            comparisons[0]++;
            int c = Integer.compare(a.weight(), b.weight());
            if (c != 0) return c;
            c = Integer.compare(a.from(), b.from());
            return c != 0 ? c : Integer.compare(a.to(), b.to());
        });
        DisjointSetUnion dsu = new DisjointSetUnion(g.vertexCount());
        List<Edge> tree = new ArrayList<>();
        long weight = 0, examined = 0;
        for (Edge edge : sorted) {
            examined++;
            if (dsu.union(edge.from(), edge.to())) {
                tree.add(edge);
                weight += edge.weight();
            }
            if (tree.size() == g.vertexCount() - 1) break;
        }
        return new MSTResult(weight, List.copyOf(tree), dsu.components() == 1,
                dsu.components(), examined, comparisons[0], dsu.findCalls(),
                dsu.unionCalls(), dsu.pathCompressions(), 0, 0);
    }
}
