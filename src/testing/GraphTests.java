package testing;

import connectivity.KosarajuSCC;
import connectivity.TopologicalSort;
import dsu.DisjointSetUnion;
import graph.Edge;
import graph.GraphFactory;
import graph.WeightedGraph;
import mst.Kruskal;
import mst.MSTResult;
import mst.Prim;
import shortestpath.Dijkstra;
import java.util.*;

/** Self-contained correctness checks; does not require third-party JUnit. */
public final class GraphTests {
    private static int passed = 0;
    private GraphTests() { }

    private static void check(boolean condition, String description) {
        if (!condition) throw new AssertionError(description);
    }
    private static void run(String name, Runnable test) {
        test.run(); passed++;
        System.out.println("  PASS  " + name);
    }
    private static void throwsArgument(Runnable task) {
        boolean thrown = false;
        try { task.run(); } catch (IllegalArgumentException expected) { thrown = true; }
        check(thrown, "Expected an IllegalArgumentException");
    }
    private static WeightedGraph weightedSample() {
        WeightedGraph g = new WeightedGraph(4, false);
        g.addEdge(0, 1, 1);
        g.addEdge(0, 2, 4);
        g.addEdge(1, 2, 2);
        g.addEdge(1, 3, 5);
        g.addEdge(2, 3, 3);
        return g;
    }
    private static void testMstKnown() {
        WeightedGraph g = weightedSample();
        MSTResult k = Kruskal.compute(g), p = Prim.compute(g);
        check(k.weight() == 6 && p.weight() == 6, "Known MST weight should be 6");
        check(k.connected() && p.connected(), "Sample should be connected");
        check(k.edges().size() == 3 && p.edges().size() == 3, "MST must have V-1 edges");
    }
    private static void testDisconnectedForest() {
        WeightedGraph g = new WeightedGraph(5, false);
        g.addEdge(0, 1, 4);
        g.addEdge(2, 3, 7);
        MSTResult a = Kruskal.compute(g), b = Prim.compute(g);
        check(!a.connected() && !b.connected(), "Disconnected graph detected");
        check(a.weight() == 11 && b.weight() == 11, "Both find minimum spanning forest");
        check(a.components() == 3 && b.components() == 3, "Three components (incl isolated)");
    }
    private static void testDuplicateWeightsAndMsts() {
        WeightedGraph g = new WeightedGraph(4, false);
        g.addEdge(0, 1, 1); g.addEdge(1, 2, 1);
        g.addEdge(2, 3, 1); g.addEdge(3, 0, 1);
        g.addEdge(0, 2, 2);
        check(Kruskal.compute(g).weight() == 3 && Prim.compute(g).weight() == 3,
                "Several MSTs exist; equal optimal weight");
    }
    private static void testSingleVertex() {
        WeightedGraph u = new WeightedGraph(1, false);
        WeightedGraph d = new WeightedGraph(1, true);
        check(Kruskal.compute(u).connected() && Prim.compute(u).connected(), "V=1 is connected");
        check(Kruskal.compute(u).weight() == 0 && Prim.compute(u).weight() == 0,
                "Single-node MST cost is 0");
        check(TopologicalSort.compute(d).order().equals(List.of(0)), "Single-node topo");
        check(KosarajuSCC.compute(d).components().equals(List.of(List.of(0))), "Single-node SCC");
        check(Dijkstra.compute(d, 0).pathTo(0).equals(List.of(0)), "Source path itself");
    }
    private static void validateTopological(WeightedGraph g, List<Integer> ordering) {
        check(ordering.size() == g.vertexCount(), "Missing vertices from topological order");
        int[] position = new int[g.vertexCount()];
        boolean[] seen = new boolean[g.vertexCount()];
        for (int i = 0; i < ordering.size(); i++) {
            int v = ordering.get(i);
            check(!seen[v], "Repeated vertex in ordering");
            position[v] = i; seen[v] = true;
        }
        for (Edge edge : g.edges()) {
            check(position[edge.from()] < position[edge.to()], "Topological constraint violated");
        }
    }
    private static void testTopoDag() {
        WeightedGraph g = new WeightedGraph(6, true);
        g.addEdge(5, 2, 1); g.addEdge(5, 0, 1); g.addEdge(4, 0, 1);
        g.addEdge(4, 1, 1); g.addEdge(2, 3, 1); g.addEdge(3, 1, 1);
        var topo = TopologicalSort.compute(g);
        check(topo.isDag(), "Should accept DAG");
        validateTopological(g, topo.order());
    }
    private static void testTopoCycle() {
        WeightedGraph g = new WeightedGraph(4, true);
        g.addEdge(0, 1, 1); g.addEdge(1, 2, 1);
        g.addEdge(2, 0, 1); g.addEdge(2, 3, 1);
        var order = TopologicalSort.compute(g);
        check(!order.isDag() && order.order().isEmpty(), "Must reject directed cycles");
    }
    private static void testSccMultiple() {
        WeightedGraph g = new WeightedGraph(7, true);
        g.addEdge(0, 1, 1); g.addEdge(1, 0, 1);
        g.addEdge(1, 2, 1); g.addEdge(2, 3, 1);
        g.addEdge(3, 4, 1); g.addEdge(4, 2, 1);
        g.addEdge(4, 5, 1);
        var components = KosarajuSCC.compute(g).components();
        check(components.equals(List.of(List.of(0, 1), List.of(2, 3, 4), List.of(5), List.of(6))),
                "Expected four disjoint SCCs: " + components);
    }
    private static void testEveryVertexExactlyOneScc() {
        WeightedGraph g = GraphFactory.cyclicThreeScc(100, 500, 17L);
        var components = KosarajuSCC.compute(g).components();
        check(components.size() == 3, "Generated graph must contain exactly 3 SCCs");
        boolean[] seen = new boolean[g.vertexCount()];
        int count = 0;
        for (List<Integer> component : components) {
            for (int v : component) {
                check(!seen[v], "SCC overlap");
                seen[v] = true; count++;
            }
        }
        check(count == g.vertexCount(), "SCC partition must cover all vertices");
        check(!TopologicalSort.compute(g).isDag(), "Generated cyclic graph must be cyclic");
    }
    private static void testDijkstraDistancesAndPaths() {
        WeightedGraph g = new WeightedGraph(6, true);
        g.addEdge(0, 1, 4); g.addEdge(0, 2, 1); g.addEdge(2, 1, 2);
        g.addEdge(1, 3, 1); g.addEdge(2, 3, 5); g.addEdge(3, 4, 3);
        var result = Dijkstra.compute(g, 0);
        check(Arrays.equals(result.distances(), new long[]{0, 3, 1, 4, 7, Dijkstra.INF}),
                "Unexpected shortest path distances: " + Arrays.toString(result.distances()));
        check(result.pathTo(4).equals(List.of(0, 2, 1, 3, 4)), "Reconstructed path");
        check(result.pathTo(5).isEmpty() && !result.reachable(5), "Unreachable vertex");
    }
    private static void testDijkstraZeroAndDuplicateWeights() {
        WeightedGraph g = new WeightedGraph(3, true);
        g.addEdge(0, 1, 0); g.addEdge(1, 2, 0); g.addEdge(0, 2, 0);
        var result = Dijkstra.compute(g, 0);
        check(result.distance(2) == 0, "Nonnegative zero weights supported");
        check(result.pathTo(2).get(0) == 0, "Path begins with source");
    }
    private static void testRejectNegativeWeight() {
        WeightedGraph g = new WeightedGraph(2, true);
        throwsArgument(() -> g.addEdge(0, 1, -2));
        throwsArgument(() -> Dijkstra.compute(g, 3));
    }
    private static void testInvalidAlgorithmType() {
        WeightedGraph directed = new WeightedGraph(2, true);
        WeightedGraph undirected = new WeightedGraph(2, false);
        throwsArgument(() -> Kruskal.compute(directed));
        throwsArgument(() -> Prim.compute(directed));
        throwsArgument(() -> TopologicalSort.compute(undirected));
        throwsArgument(() -> KosarajuSCC.compute(undirected));
    }
    private static void testDsu() {
        DisjointSetUnion dsu = new DisjointSetUnion(5);
        check(dsu.union(0, 1) && dsu.union(1, 2), "Merge groups");
        check(!dsu.union(0, 2), "Already in same component");
        check(dsu.find(0) == dsu.find(2), "Transitive connection");
        check(dsu.components() == 3 && dsu.findCalls() > 0 && dsu.unionCalls() == 3,
                "DSU counters and component count");
    }
    private static void testFactoryReproducible() {
        WeightedGraph a = GraphFactory.connectedUndirected(10, 30, 42);
        WeightedGraph b = GraphFactory.connectedUndirected(10, 30, 42);
        check(a.edges().equals(b.edges()), "Same seed reproduces same graph");
        check(a.edgeCount() == 30 && Kruskal.compute(a).connected(), "Exactly 30 connected roads");
        WeightedGraph dag = GraphFactory.dag(10, 30, 42);
        check(dag.edgeCount() == 30 && TopologicalSort.compute(dag).isDag(), "30-edge DAG");
        WeightedGraph cyclic = GraphFactory.cyclicThreeScc(10, 30, 42);
        check(cyclic.edgeCount() == 30 && KosarajuSCC.compute(cyclic).components().size() == 3,
                "30-edge three-SCC cyclic graph");
    }
    private static void testGeneratedMstAgreement() {
        for (int n : new int[]{10, 100, 1000}) {
            int e = n == 10 ? 30 : n * 5;
            WeightedGraph g = GraphFactory.connectedUndirected(n, e, 99);
            MSTResult a = Kruskal.compute(g), b = Prim.compute(g);
            check(a.connected() && b.connected() && a.weight() == b.weight(),
                    "MST mismatch for " + n + " vertices");
        }
    }
    private static void testGeneratedTopoAndScc() {
        for (int n : new int[]{10, 100, 1000}) {
            int e = n == 10 ? 30 : n * 5;
            WeightedGraph dag = GraphFactory.dag(n, e, 12);
            var sorted = TopologicalSort.compute(dag);
            check(sorted.isDag(), "DAG rejected");
            validateTopological(dag, sorted.order());
            check(KosarajuSCC.compute(dag).components().size() == n,
                    "Every vertex in a DAG is its own SCC");
        }
    }
    private static void testAgainstBellmanFord() {
        WeightedGraph g = GraphFactory.cyclicThreeScc(10, 30, 2026);
        long[] d = new long[g.vertexCount()];
        Arrays.fill(d, Dijkstra.INF); d[0] = 0;
        for (int i = 0; i < g.vertexCount() - 1; i++) {
            for (Edge e : g.edges()) {
                if (d[e.from()] != Dijkstra.INF) {
                    d[e.to()] = Math.min(d[e.to()], d[e.from()] + e.weight());
                }
            }
        }
        check(Arrays.equals(d, Dijkstra.compute(g, 0).distances()),
                "Dijkstra should agree with independent Bellman-Ford baseline");
    }

    public static void main(String[] args) {
        System.out.println("Running graph algorithm correctness tests:");
        run("Known MST weight", GraphTests::testMstKnown);
        run("Disconnected graph and isolated vertex", GraphTests::testDisconnectedForest);
        run("Duplicate weights and multiple valid MSTs", GraphTests::testDuplicateWeightsAndMsts);
        run("Single vertex", GraphTests::testSingleVertex);
        run("Valid DAG topological ordering", GraphTests::testTopoDag);
        run("Cycle prevents topological ordering", GraphTests::testTopoCycle);
        run("Multiple strongly connected components", GraphTests::testSccMultiple);
        run("Each vertex in exactly one SCC", GraphTests::testEveryVertexExactlyOneScc);
        run("Dijkstra distances, path and unreachable vertices", GraphTests::testDijkstraDistancesAndPaths);
        run("Zero and duplicate edge weights", GraphTests::testDijkstraZeroAndDuplicateWeights);
        run("Reject negative weights and bad sources", GraphTests::testRejectNegativeWeight);
        run("Reject unsupported graph types", GraphTests::testInvalidAlgorithmType);
        run("DSU find, union and counts", GraphTests::testDsu);
        run("Fixed-seed reproducible generators", GraphTests::testFactoryReproducible);
        run("Kruskal / Prim agreement for every graph size", GraphTests::testGeneratedMstAgreement);
        run("DAG and SCC correctness for every graph size", GraphTests::testGeneratedTopoAndScc);
        run("Dijkstra cross-check against Bellman-Ford", GraphTests::testAgainstBellmanFord);
        System.out.printf("Tests passed: %d/%d%n", passed, passed);
    }
}
