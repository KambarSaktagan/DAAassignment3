package connectivity;

import graph.Edge;
import graph.WeightedGraph;
import java.util.*;

/** Kosaraju: iterative DFS to avoid recursion depth limits. */
public final class KosarajuSCC {
    private KosarajuSCC() { }
    private static final class Frame {
        final int vertex;
        int nextEdge;
        Frame(int vertex) { this.vertex = vertex; }
    }
    public record Result(List<List<Integer>> components, long edgeExaminations,
                         long verticesProcessed, long stackOperations) {
        public long operations() { return edgeExaminations + verticesProcessed + stackOperations; }
    }
    public static Result compute(WeightedGraph g) {
        if (!g.isDirected()) throw new IllegalArgumentException("SCC requires directed graph");
        int n = g.vertexCount();
        boolean[] visited = new boolean[n];
        List<Integer> finishingOrder = new ArrayList<>();
        long scanned = 0, processed = 0, stackOps = 0;
        for (int source = 0; source < n; source++) {
            if (visited[source]) continue;
            Deque<Frame> stack = new ArrayDeque<>();
            stack.push(new Frame(source)); stackOps++; visited[source] = true;
            while (!stack.isEmpty()) {
                Frame frame = stack.peek();
                List<Edge> outgoing = g.neighbors(frame.vertex);
                if (frame.nextEdge < outgoing.size()) {
                    int v = outgoing.get(frame.nextEdge++).to(); scanned++;
                    if (!visited[v]) {
                        visited[v] = true;
                        stack.push(new Frame(v)); stackOps++;
                    }
                } else {
                    stack.pop(); stackOps++;
                    finishingOrder.add(frame.vertex); processed++;
                }
            }
        }
        List<List<Integer>> reversed = new ArrayList<>();
        for (int i = 0; i < n; i++) reversed.add(new ArrayList<>());
        for (Edge edge : g.edges()) reversed.get(edge.to()).add(edge.from());
        scanned += g.edgeCount(); // transpose construction visits every edge
        Arrays.fill(visited, false);
        List<List<Integer>> components = new ArrayList<>();
        for (int i = finishingOrder.size() - 1; i >= 0; i--) {
            int root = finishingOrder.get(i);
            if (visited[root]) continue;
            List<Integer> component = new ArrayList<>();
            Deque<Integer> stack = new ArrayDeque<>();
            stack.push(root); stackOps++; visited[root] = true;
            while (!stack.isEmpty()) {
                int u = stack.pop(); stackOps++;
                component.add(u); processed++;
                for (int v : reversed.get(u)) {
                    scanned++;
                    if (!visited[v]) { visited[v] = true; stack.push(v); stackOps++; }
                }
            }
            Collections.sort(component);
            components.add(List.copyOf(component));
        }
        components.sort(Comparator.comparingInt(c -> c.get(0)));
        return new Result(List.copyOf(components), scanned, processed, stackOps);
    }
}
