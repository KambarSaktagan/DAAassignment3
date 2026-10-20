# Benchmark results

Numbers below are from **5 timed runs** of each algorithm on the **same fixed-seed generated graph**, after 30 untimed warm-up runs. Graph generation is excluded from timing. Times are environment-dependent.

| Size | V | E | Algorithm | Graph | Mean time (ms) | Instrumented operations | Components | MST cost | Cycle? |
|---|---:|---:|---|---|---:|---:|---:|---:|---|
| Small | 10 | 30 | Kruskal | undirected connected | 0.016760 | 154 | 1 | 244 | No |
| Small | 10 | 30 | Prim | undirected connected | 0.044160 | 320 | 1 | 244 | No |
| Small | 10 | 30 | Topological DAG | directed DAG | 0.029300 | 70 | — | — | No |
| Small | 10 | 30 | Topological cyclic | directed cyclic | 0.014040 | 30 | — | — | Yes |
| Small | 10 | 30 | Kosaraju SCC | directed cyclic | 0.050840 | 150 | 3 | — | Yes |
| Small | 10 | 30 | Dijkstra | directed cyclic | 0.030120 | 143 | — | — | No |
| Medium | 100 | 500 | Kruskal | undirected connected | 0.220920 | 5,390 | 1 | 1432 | No |
| Medium | 100 | 500 | Prim | undirected connected | 0.399940 | 9,729 | 1 | 1432 | No |
| Medium | 100 | 500 | Topological DAG | directed DAG | 0.066660 | 1,100 | — | — | No |
| Medium | 100 | 500 | Topological cyclic | directed cyclic | 0.018840 | 500 | — | — | Yes |
| Medium | 100 | 500 | Kosaraju SCC | directed cyclic | 0.398400 | 2,100 | 3 | — | Yes |
| Medium | 100 | 500 | Dijkstra | directed cyclic | 0.222900 | 2,427 | — | — | No |
| Large | 1000 | 5000 | Kruskal | undirected connected | 2.009060 | 72,344 | 1 | 12711 | No |
| Large | 1000 | 5000 | Prim | undirected connected | 2.463200 | 131,628 | 1 | 12711 | No |
| Large | 1000 | 5000 | Topological DAG | directed DAG | 0.546400 | 11,000 | — | — | No |
| Large | 1000 | 5000 | Topological cyclic | directed cyclic | 0.148460 | 5,000 | — | — | Yes |
| Large | 1000 | 5000 | Kosaraju SCC | directed cyclic | 0.909020 | 21,000 | 3 | — | Yes |
| Large | 1000 | 5000 | Dijkstra | directed cyclic | 1.251200 | 28,849 | — | — | No |

*Operation counts* sum instrumented events (such as inspected edges, heap pushes/pops, comparisons, DSU calls and DFS stack actions), **not CPU instructions**. See `benchmark_results.csv` for the per-category breakdown. `connected` applies to MST runs only; `cycle_detected` applies to topological-sort runs. SCC count is 3 by construction for the cyclic experiment. For SCC the counted 'edge examinations' include building the transpose and traversals on both directions.
