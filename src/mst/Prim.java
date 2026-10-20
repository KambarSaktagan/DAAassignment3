package mst;

import graph.Edge;
import graph.WeightedGraph;
import java.util.*;

public final class Prim {
    private Prim() { }
    public static MSTResult compute(WeightedGraph g) {
        if (g.isDirected()) throw new IllegalArgumentException("MST requires undirected graph");
        boolean[] visited = new boolean[g.vertexCount()];
        long[] comparisons = {0};
        PriorityQueue<Edge> queue = new PriorityQueue<>((a, b) -> {
            comparisons[0]++;
            int c = Integer.compare(a.weight(), b.weight());
            if (c != 0) return c;
            c = Integer.compare(a.from(), b.from());
            return c != 0 ? c : Integer.compare(a.to(), b.to());
        });
        List<Edge> tree = new ArrayList<>();
        long weight = 0, examined = 0, pushes = 0, pops = 0;
        int components = 0;
        for (int start = 0; start < g.vertexCount(); start++) {
            if (visited[start]) continue;
            components++;
            queue.add(new Edge(-1, start, 0)); pushes++;
            while (!queue.isEmpty()) {
                Edge e = queue.poll(); pops++;
                if (visited[e.to()]) continue;
                visited[e.to()] = true;
                if (e.from() != -1) {
                    tree.add(e);
                    weight += e.weight();
                }
                for (Edge next : g.neighbors(e.to())) {
                    examined++;
                    if (!visited[next.to()]) { queue.add(next); pushes++; }
                }
            }
        }
        return new MSTResult(weight, List.copyOf(tree), components == 1,
                components, examined, comparisons[0], 0, 0, 0, pushes, pops);
    }
}
