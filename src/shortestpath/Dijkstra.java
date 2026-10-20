package shortestpath;

import graph.Edge;
import graph.WeightedGraph;
import java.util.*;

/** Single-source shortest paths for nonnegative-weight directed or undirected graphs. */
public final class Dijkstra {
    private Dijkstra() { }
    private record State(int vertex, long distance) { }
    public static final long INF = Long.MAX_VALUE;
    public record Result(int source, long[] distances, int[] parents,
                         long edgeExaminations, long comparisons,
                         long heapPushes, long heapPops, long relaxations) {
        public boolean reachable(int v) { return distances[v] != INF; }
        public long distance(int v) { return distances[v]; }
        public List<Integer> pathTo(int target) {
            if (target < 0 || target >= distances.length) throw new IllegalArgumentException("Bad target");
            if (!reachable(target)) return List.of();
            LinkedList<Integer> path = new LinkedList<>();
            for (int at = target; at != -1; at = parents[at]) path.addFirst(at);
            return List.copyOf(path);
        }
        public long operations() {
            return edgeExaminations + comparisons + heapPushes + heapPops + relaxations;
        }
    }
    public static Result compute(WeightedGraph g, int source) {
        if (source < 0 || source >= g.vertexCount()) throw new IllegalArgumentException("Bad source");
        long[] distances = new long[g.vertexCount()];
        int[] parents = new int[g.vertexCount()];
        Arrays.fill(distances, INF);
        Arrays.fill(parents, -1);
        long[] comparisons = {0};
        PriorityQueue<State> heap = new PriorityQueue<>((a, b) -> {
            comparisons[0]++;
            int c = Long.compare(a.distance(), b.distance());
            return c != 0 ? c : Integer.compare(a.vertex(), b.vertex());
        });
        distances[source] = 0;
        heap.add(new State(source, 0));
        long examined = 0, pushes = 1, pops = 0, relaxations = 0;
        while (!heap.isEmpty()) {
            State current = heap.poll(); pops++;
            if (current.distance() != distances[current.vertex()]) continue;
            for (Edge edge : g.neighbors(current.vertex())) {
                examined++;
                if (edge.weight() < 0) throw new IllegalArgumentException("Negative road weight");
                // Saturation guards against long overflow.
                long candidate = current.distance() > Long.MAX_VALUE - edge.weight()
                        ? INF : current.distance() + edge.weight();
                if (candidate < distances[edge.to()]) {
                    distances[edge.to()] = candidate;
                    parents[edge.to()] = current.vertex();
                    heap.add(new State(edge.to(), candidate));
                    pushes++; relaxations++;
                }
            }
        }
        return new Result(source, distances, parents, examined, comparisons[0],
                pushes, pops, relaxations);
    }
}
