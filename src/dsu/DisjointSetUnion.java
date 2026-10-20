package dsu;

/** Union by size + path compression. Instrumentation counts logical operations. */
public final class DisjointSetUnion {
    private final int[] parent, size;
    private int components;
    private long findCalls, unionCalls, pathCompressions;

    public DisjointSetUnion(int n) {
        parent = new int[n]; size = new int[n]; components = n;
        for (int i = 0; i < n; i++) { parent[i] = i; size[i] = 1; }
    }
    public int find(int x) {
        findCalls++;
        int root = x;
        while (root != parent[root]) root = parent[root];
        while (x != root) {
            int next = parent[x];
            if (parent[x] != root) pathCompressions++;
            parent[x] = root;
            x = next;
        }
        return root;
    }
    public boolean union(int a, int b) {
        unionCalls++;
        int ra = find(a), rb = find(b);
        if (ra == rb) return false;
        if (size[ra] < size[rb]) { int tmp = ra; ra = rb; rb = tmp; }
        parent[rb] = ra;
        size[ra] += size[rb];
        components--;
        return true;
    }
    public int components() { return components; }
    public long findCalls() { return findCalls; }
    public long unionCalls() { return unionCalls; }
    public long pathCompressions() { return pathCompressions; }
}
