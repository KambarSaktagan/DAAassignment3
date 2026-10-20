package demo;

import connectivity.KosarajuSCC;
import connectivity.TopologicalSort;
import graph.Edge;
import graph.WeightedGraph;
import mst.Kruskal;
import mst.Prim;
import shortestpath.Dijkstra;

import java.util.List;
import java.util.stream.Collectors;

/** Readable example of how a planner can use all five graph algorithms. */
public final class CityDemo {
    private static final String[] DISTRICTS = {
            "Central", "University", "Hospital", "Station", "Park", "Airport"
    };
    private CityDemo() { }
    private static String label(int v) { return DISTRICTS[v]; }
    private static String formatPath(List<Integer> path) {
        return path.stream().map(CityDemo::label).collect(Collectors.joining(" -> "));
    }
    public static void main(String[] args) {
        System.out.println("SMART CITY INFRASTRUCTURE DEMO");
        WeightedGraph roads = new WeightedGraph(DISTRICTS.length, false);
        roads.addEdge(0, 1, 4); roads.addEdge(0, 2, 6);
        roads.addEdge(1, 2, 1); roads.addEdge(1, 3, 7);
        roads.addEdge(2, 3, 3); roads.addEdge(3, 4, 2);
        roads.addEdge(4, 5, 5); roads.addEdge(2, 5, 12);
        roads.addEdge(1, 4, 8);
        var kruskal = Kruskal.compute(roads);
        var prim = Prim.compute(roads);
        System.out.printf("Minimum infrastructure cost: Kruskal=%d, Prim=%d; connected=%s%n",
                kruskal.weight(), prim.weight(), kruskal.connected());
        for (Edge edge : kruskal.edges()) {
            System.out.printf("  Build %s -- %s, cost %d%n",
                    label(edge.from()), label(edge.to()), edge.weight());
        }

        WeightedGraph oneWayRoads = new WeightedGraph(DISTRICTS.length, true);
        oneWayRoads.addEdge(0, 1, 4); oneWayRoads.addEdge(0, 2, 10);
        oneWayRoads.addEdge(1, 2, 2); oneWayRoads.addEdge(1, 4, 10);
        oneWayRoads.addEdge(2, 3, 3); oneWayRoads.addEdge(3, 4, 2);
        oneWayRoads.addEdge(4, 5, 5);
        var paths = Dijkstra.compute(oneWayRoads, 0);
        System.out.println("Shortest routes from Central (directed roads):");
        for (int i = 0; i < DISTRICTS.length; i++) {
            if (!paths.reachable(i)) System.out.println("  " + label(i) + ": UNREACHABLE");
            else System.out.printf("  %s: distance %d; %s%n", label(i),
                    paths.distance(i), formatPath(paths.pathTo(i)));
        }

        WeightedGraph dependencies = new WeightedGraph(DISTRICTS.length, true);
        dependencies.addEdge(0, 1, 1); dependencies.addEdge(1, 2, 1);
        dependencies.addEdge(2, 3, 1); dependencies.addEdge(1, 4, 1);
        dependencies.addEdge(4, 5, 1);
        var topo = TopologicalSort.compute(dependencies);
        System.out.println("Infrastructure work order: " + formatPath(topo.order()));

        WeightedGraph linkedServices = new WeightedGraph(DISTRICTS.length, true);
        linkedServices.addEdge(0, 1, 1); linkedServices.addEdge(1, 2, 1);
        linkedServices.addEdge(2, 0, 1); linkedServices.addEdge(2, 3, 1);
        linkedServices.addEdge(3, 4, 1); linkedServices.addEdge(4, 3, 1);
        linkedServices.addEdge(4, 5, 1);
        System.out.println("Mutually accessible groups (strongly connected components):");
        for (List<Integer> group : KosarajuSCC.compute(linkedServices).components()) {
            System.out.println("  " + group.stream().map(CityDemo::label)
                    .collect(Collectors.joining(", ")));
        }
    }
}
