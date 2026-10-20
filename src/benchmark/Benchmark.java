package benchmark;

import connectivity.KosarajuSCC;
import connectivity.TopologicalSort;
import graph.GraphFactory;
import graph.WeightedGraph;
import mst.Kruskal;
import mst.MSTResult;
import mst.Prim;
import shortestpath.Dijkstra;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.function.Supplier;

/** Five measured repeats per algorithm and size, after unmeasured JVM warm-up. */
public final class Benchmark {
    public static final int REPEATS = 5;
    public static final int WARMUPS = 30;
    public static final long BASE_SEED = 31_415_926L;
    private static volatile long blackHole = 0;

    private Benchmark() { }

    public record Metrics(long edgeExaminations, long comparisons, long dsuFindCalls,
                          long dsuUnionCalls, long heapPushes, long heapPops,
                          long verticesProcessed, long successfulRelaxations,
                          long otherOperations, int componentCount, long totalWeight,
                          boolean connected, boolean cycleDetected, long checksum) {
        public long operations() {
            return edgeExaminations + comparisons + dsuFindCalls + dsuUnionCalls +
                    heapPushes + heapPops + verticesProcessed + successfulRelaxations +
                    otherOperations;
        }
    }

    public record Row(String size, int vertices, int edges, String algorithm,
                      String graphType, long seed, double averageNs, Metrics metrics) {
        public double milliseconds() { return averageNs / 1_000_000.0; }
    }

    private static Metrics kruskal(WeightedGraph g) {
        MSTResult r = Kruskal.compute(g);
        return new Metrics(r.edgeExaminations(), r.comparisons(), r.dsuFindCalls(),
                r.dsuUnionCalls(), 0, 0, 0, 0, r.pathCompressions(),
                r.components(), r.weight(), r.connected(), false, r.weight());
    }
    private static Metrics prim(WeightedGraph g) {
        MSTResult r = Prim.compute(g);
        return new Metrics(r.edgeExaminations(), r.comparisons(), 0, 0,
                r.heapPushes(), r.heapPops(), 0, 0, 0, r.components(),
                r.weight(), r.connected(), false, r.weight());
    }
    private static Metrics topo(WeightedGraph g) {
        var r = TopologicalSort.compute(g);
        long checksum = r.order().stream().mapToLong(Integer::longValue).sum();
        return new Metrics(r.edgeExaminations(), 0, 0, 0, 0, 0,
                r.verticesProcessed(), 0, 0, -1, -1, false,
                !r.isDag(), checksum + (r.isDag() ? 1 : 0));
    }
    private static Metrics scc(WeightedGraph g) {
        var r = KosarajuSCC.compute(g);
        return new Metrics(r.edgeExaminations(), 0, 0, 0, 0, 0,
                r.verticesProcessed(), 0, r.stackOperations(), r.components().size(),
                -1, false, true, r.components().size());
    }
    private static Metrics shortestPaths(WeightedGraph g) {
        var r = Dijkstra.compute(g, 0);
        long checksum = 0;
        int reachable = 0;
        for (long distance : r.distances()) {
            if (distance != Dijkstra.INF) { checksum += distance; reachable++; }
        }
        return new Metrics(r.edgeExaminations(), r.comparisons(), 0, 0,
                r.heapPushes(), r.heapPops(), reachable, r.relaxations(), 0,
                -1, -1, false, false, checksum);
    }
    private static Row measure(String size, int v, int e, String algorithm, String type,
                               long seed, Supplier<Metrics> action) {
        for (int k = 0; k < WARMUPS; k++) blackHole ^= action.get().checksum();
        long total = 0;
        Metrics last = null;
        for (int k = 0; k < REPEATS; k++) {
            long start = System.nanoTime();
            Metrics r = action.get();
            total += System.nanoTime() - start;
            blackHole ^= r.checksum(); // Make algorithm result observable to optimizer.
            if (last != null && r.operations() != last.operations()) {
                throw new IllegalStateException("Non-deterministic operation counts");
            }
            last = r;
        }
        return new Row(size, v, e, algorithm, type, seed, total / (double) REPEATS, last);
    }
    private static String csv(Row r) {
        Metrics m = r.metrics();
        return String.format(Locale.US,
                "%s,%d,%d,%s,%s,%d,%.0f,%.6f,%d,%d,%d,%d,%d,%d,%d,%d,%d,%d,%d,%d,%d,%s,%s",
                r.size(), r.vertices(), r.edges(), r.algorithm(), r.graphType(), r.seed(),
                r.averageNs(), r.milliseconds(), m.operations(), m.edgeExaminations(),
                m.comparisons(), m.dsuFindCalls(), m.dsuUnionCalls(),
                m.heapPushes(), m.heapPops(), m.verticesProcessed(),
                m.successfulRelaxations(), m.otherOperations(), m.componentCount(),
                m.totalWeight(), m.checksum(), m.connected(), m.cycleDetected());
    }
    private static void writeTables(List<Row> rows) throws IOException {
        Path dir = Path.of("results", "tables");
        Files.createDirectories(dir);
        List<String> csv = new ArrayList<>();
        csv.add("size,vertices,edges,algorithm,graph_type,seed,avg_time_ns,avg_time_ms,operations,edge_examinations,comparisons,dsu_find_calls,dsu_union_calls,heap_pushes,heap_pops,vertices_processed,successful_relaxations,other_operations,component_count,total_weight,checksum,connected,cycle_detected");
        List<String> markdown = new ArrayList<>();
        markdown.add("# Benchmark results");
        markdown.add("");
        markdown.add("Numbers below are from **5 timed runs** of each algorithm on the **same fixed-seed generated graph**, after 30 untimed warm-up runs. Graph generation is excluded from timing. Times are environment-dependent.");
        markdown.add("");
        markdown.add("| Size | V | E | Algorithm | Graph | Mean time (ms) | Instrumented operations | Components | MST cost | Cycle? |");
        markdown.add("|---|---:|---:|---|---|---:|---:|---:|---:|---|");
        for (Row row : rows) {
            csv.add(csv(row));
            Metrics m = row.metrics();
            markdown.add(String.format(Locale.US,
                    "| %s | %d | %d | %s | %s | %.6f | %,d | %s | %s | %s |",
                    row.size(), row.vertices(), row.edges(), row.algorithm(),
                    row.graphType(), row.milliseconds(), m.operations(),
                    m.componentCount() < 0 ? "—" : Integer.toString(m.componentCount()),
                    m.totalWeight() < 0 ? "—" : Long.toString(m.totalWeight()),
                    m.cycleDetected() ? "Yes" : "No"));
        }
        markdown.add("");
        markdown.add("*Operation counts* sum instrumented events (such as inspected edges, heap pushes/pops, comparisons, DSU calls and DFS stack actions), **not CPU instructions**. See `benchmark_results.csv` for the per-category breakdown. `connected` applies to MST runs only; `cycle_detected` applies to topological-sort runs. SCC count is 3 by construction for the cyclic experiment. For SCC the counted 'edge examinations' include building the transpose and traversals on both directions.");
        Files.write(dir.resolve("benchmark_results.csv"), csv, StandardCharsets.UTF_8);
        Files.write(dir.resolve("benchmark_results.md"), markdown, StandardCharsets.UTF_8);
    }
    public static void main(String[] args) throws Exception {
        int[] vertices = {10, 100, 1_000};
        int[] edges = {30, 500, 5_000};
        String[] names = {"Small", "Medium", "Large"};
        List<Row> rows = new ArrayList<>();
        for (int i = 0; i < vertices.length; i++) {
            int v = vertices[i], e = edges[i];
            long seedU = BASE_SEED + v * 31L;
            long seedDag = seedU + 10_000;
            long seedCyclic = seedU + 20_000;
            WeightedGraph undirected = GraphFactory.connectedUndirected(v, e, seedU);
            WeightedGraph dag = GraphFactory.dag(v, e, seedDag);
            WeightedGraph cyclic = GraphFactory.cyclicThreeScc(v, e, seedCyclic);
            rows.add(measure(names[i], v, e, "Kruskal", "undirected connected", seedU,
                    () -> kruskal(undirected)));
            rows.add(measure(names[i], v, e, "Prim", "undirected connected", seedU,
                    () -> prim(undirected)));
            rows.add(measure(names[i], v, e, "Topological DAG", "directed DAG", seedDag,
                    () -> topo(dag)));
            rows.add(measure(names[i], v, e, "Topological cyclic", "directed cyclic", seedCyclic,
                    () -> topo(cyclic)));
            rows.add(measure(names[i], v, e, "Kosaraju SCC", "directed cyclic", seedCyclic,
                    () -> scc(cyclic)));
            rows.add(measure(names[i], v, e, "Dijkstra", "directed cyclic", seedCyclic,
                    () -> shortestPaths(cyclic)));
            var k = Kruskal.compute(undirected);
            var p = Prim.compute(undirected);
            if (!k.connected() || !p.connected() || k.weight() != p.weight()) {
                throw new AssertionError("Kruskal/Prim disagreement at V=" + v);
            }
            if (!TopologicalSort.compute(dag).isDag() || TopologicalSort.compute(cyclic).isDag() ||
                    KosarajuSCC.compute(cyclic).components().size() != 3) {
                throw new AssertionError("Generated graph type invalid at V=" + v);
            }
        }
        writeTables(rows);
        Plotter.createCharts(rows);
        System.out.println("\nBENCHMARK RESULTS (5 runs; 30 warmups; fixed seeds)");
        System.out.printf("%-7s %5s %5s %-20s %14s %14s%n",
                "Size", "V", "E", "Algorithm", "Mean (ms)", "Operations");
        for (Row r : rows) {
            System.out.printf(Locale.US, "%-7s %5d %5d %-20s %14.6f %,14d%n",
                    r.size(), r.vertices(), r.edges(), r.algorithm(), r.milliseconds(),
                    r.metrics().operations());
        }
        System.out.println("Saved: results/tables/benchmark_results.csv");
        System.out.println("Saved: results/tables/benchmark_results.md");
        System.out.println("Saved: results/plots/time_vs_vertices.png");
        System.out.println("Saved: results/plots/time_vs_edges.png");
    }
}
