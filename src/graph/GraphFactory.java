package graph;

import java.util.*;

/** All data sets are deterministic for a chosen seed; no parallel edges or self-loops. */
public final class GraphFactory {
    private GraphFactory() { }

    private static long key(int a, int b) {
        return ((long) a << 32) | (b & 0xffffffffL);
    }
    private static void addUnique(WeightedGraph g, Set<Long> seen, int a, int b,
                                  Random random) {
        if (seen.add(key(a, b))) g.addEdge(a, b, 1 + random.nextInt(100));
    }
    private static List<Integer> permutation(int n, Random random) {
        List<Integer> p = new ArrayList<>();
        for (int i = 0; i < n; i++) p.add(i);
        Collections.shuffle(p, random);
        return p;
    }

    /** Connect a random spanning tree first, then add distinct random undirected roads. */
    public static WeightedGraph connectedUndirected(int vertices, int edges, long seed) {
        long maximum = (long) vertices * (vertices - 1) / 2;
        if (edges < vertices - 1 || edges > maximum) {
            throw new IllegalArgumentException("Invalid edge count for connected simple graph");
        }
        Random random = new Random(seed);
        WeightedGraph g = new WeightedGraph(vertices, false);
        Set<Long> seen = new HashSet<>();
        List<Integer> order = permutation(vertices, random);
        for (int i = 1; i < vertices; i++) {
            int a = order.get(i), b = order.get(random.nextInt(i));
            addUnique(g, seen, Math.min(a, b), Math.max(a, b), random);
        }
        while (g.edgeCount() < edges) {
            int a = random.nextInt(vertices), b = random.nextInt(vertices);
            if (a != b) addUnique(g, seen, Math.min(a, b), Math.max(a, b), random);
        }
        return g;
    }

    /** Only add forward edges in a randomly permuted vertex order: always a DAG. */
    public static WeightedGraph dag(int vertices, int edges, long seed) {
        if (edges < 0 || edges > (long) vertices * (vertices - 1) / 2) {
            throw new IllegalArgumentException("Invalid DAG edge count");
        }
        Random random = new Random(seed);
        WeightedGraph g = new WeightedGraph(vertices, true);
        List<Integer> order = permutation(vertices, random);
        Set<Long> seen = new HashSet<>();
        while (g.edgeCount() < edges) {
            int i = random.nextInt(vertices), j = random.nextInt(vertices);
            if (i != j) {
                int a = order.get(Math.min(i, j)), b = order.get(Math.max(i, j));
                addUnique(g, seen, a, b, random);
            }
        }
        return g;
    }

    /**
     * Build exactly three SCCs (vertices partitioned into consecutive thirds).
     * Every group starts as a directed ring, so contains a cycle and is strongly
     * connected; remaining edges go within groups or from lower to higher groups,
     * never backwards. Thus the condensation graph is acyclic.
     */
    public static WeightedGraph cyclicThreeScc(int vertices, int edges, long seed) {
        if (vertices < 9 || edges < vertices) {
            throw new IllegalArgumentException("Requires >= 9 vertices and >= V edges");
        }
        int[] group = new int[vertices];
        for (int v = 0; v < vertices; v++) group[v] = (int) ((long) v * 3 / vertices);
        long max = 0;
        for (int a = 0; a < vertices; a++) {
            for (int b = 0; b < vertices; b++) {
                if (a != b && group[a] <= group[b]) max++;
            }
        }
        if (edges > max) throw new IllegalArgumentException("Too many edges");
        Random random = new Random(seed);
        WeightedGraph g = new WeightedGraph(vertices, true);
        Set<Long> seen = new HashSet<>();
        for (int k = 0; k < 3; k++) {
            List<Integer> members = new ArrayList<>();
            for (int v = 0; v < vertices; v++) if (group[v] == k) members.add(v);
            for (int j = 0; j < members.size(); j++) {
                addUnique(g, seen, members.get(j), members.get((j + 1) % members.size()), random);
            }
        }
        while (g.edgeCount() < edges) {
            int a = random.nextInt(vertices), b = random.nextInt(vertices);
            if (a != b && group[a] <= group[b]) addUnique(g, seen, a, b, random);
        }
        return g;
    }
}
