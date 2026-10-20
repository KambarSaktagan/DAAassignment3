package mst;

import graph.Edge;
import java.util.List;

/** Disconnected graphs produce a minimum spanning forest; connected is then false. */
public record MSTResult(long weight, List<Edge> edges, boolean connected, int components,
                        long edgeExaminations, long comparisons, long dsuFindCalls,
                        long dsuUnionCalls, long pathCompressions,
                        long heapPushes, long heapPops) {
    public long operations() {
        return edgeExaminations + comparisons + dsuFindCalls + dsuUnionCalls +
               pathCompressions + heapPushes + heapPops;
    }
}
