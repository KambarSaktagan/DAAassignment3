package connectivity;

import graph.Edge;
import graph.WeightedGraph;
import java.util.*;

/** Kahn's algorithm with a FIFO queue; returns no order when a cycle exists. */
public final class TopologicalSort {
    private TopologicalSort() { }
    public record Result(boolean isDag, List<Integer> order, long edgeExaminations,
                         long verticesProcessed) {
        public long operations() { return edgeExaminations + verticesProcessed; }
    }
    public static Result compute(WeightedGraph g) {
        if (!g.isDirected()) throw new IllegalArgumentException("Topological sort requires directed graph");
        int[] indegree = new int[g.vertexCount()];
        long examined = 0, processed = 0;
        for (Edge e : g.edges()) { indegree[e.to()]++; examined++; }
        Queue<Integer> queue = new ArrayDeque<>();
        for (int i = 0; i < indegree.length; i++) if (indegree[i] == 0) queue.add(i);
        List<Integer> order = new ArrayList<>();
        while (!queue.isEmpty()) {
            int u = queue.remove();
            order.add(u); processed++;
            for (Edge e : g.neighbors(u)) {
                examined++;
                if (--indegree[e.to()] == 0) queue.add(e.to());
            }
        }
        boolean dag = order.size() == g.vertexCount();
        return new Result(dag, dag ? List.copyOf(order) : List.of(), examined, processed);
    }
}
