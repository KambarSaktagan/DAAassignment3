package graph;

/** A directed adjacency entry; in an undirected graph, each road has two entries. */
public record Edge(int from, int to, int weight) { }
