# Assignment 3: Graph Algorithms

Java 17-compatible code. `Algorithms.java` contains the six algorithms and route reconstruction. The supplied `Main.java`, `Input.java` and `Json.java` retain the template logic, with comments removed and brace placement adjusted. They handle UTF-8 JSON Lines: one request and one answer per line, including `ping`. Six short overloads connect the supplied input format to the algorithm methods also used by the benchmark.

## Run

From the `Assignment 3` folder in PowerShell:

```powershell
New-Item -ItemType Directory -Force build | Out-Null
javac --release 17 -encoding UTF-8 -d build src/Java/*.java tests/Check.java
Get-Content tests/cases.jsonl | java -cp build Main
py -3 tests/check.py
py -3 tools/run.py --setup
py -3 tools/run.py --check
```

Python 3.12+ and a JDK are required; no external packages are needed. The full check builds the program, checks answers and runs the benchmark. It saves the 16 fixed-case outputs in `tests/actual.jsonl` and the complete log in `tests/results.txt`.

For a one-click demonstration, open `tests/Check.java` in VS Code and press Run above `main`. It prints the 16 examples and four route checks, then exits. Either `DAA` or `Assignment 3` may be the working folder. The normal `Main` entry point still waits for JSON input. To run only the benchmark after building:

```powershell
java -Xmx1g -cp build Check bench
```

## Correctness

### DSU

Each root represents a component, with its size stored at the root. `find` compresses the path to that root. `union` attaches the smaller component to the larger one, preserving the partition. A successful merge decreases the component count and updates the largest size. Repeated and self-unions leave both values unchanged. Each operation produces one state; zero vertices produce no states.

### Kruskal

Original edge IDs are sorted by weight. DSU accepts an edge only when it connects different current components, so selected edges remain a forest. Each accepted edge is a lightest edge across a cut between these components; the cut property makes it safe for an optimum forest. Processing all edges spans every input component. Self-loops are rejected, and isolates remain components. Parallel, tied and negative edges follow the same rule.

### Prim

Adjacency lists store original edge IDs. The priority queue contains every edge crossing from the current tree to unvisited vertices, possibly with stale internal edges. After discarding internal edges, the minimum remaining edge is safe by the cut property. It adds one new vertex without a cycle. An empty queue finishes a component; the next unvisited vertex starts another tree, including isolates. Loops are discarded. Empty graphs return cost zero, no IDs and zero components in both forest methods.

### Kosaraju SCC

The first DFS records exit order. The second DFS uses the transposed graph in decreasing exit order. For an edge between distinct SCCs, the source SCC has a greater maximum finish time. Thus the next unassigned SCC cannot reach another unassigned SCC in the transpose. Mutual reachability inside it ensures that the second DFS finds exactly that SCC. Repeating finds the whole partition, including isolates. A hash set removes repeated cross-component pairs; internal edges are omitted. Empty input produces two empty arrays.

### DFS topological ordering

States are unvisited, active and finished. Active vertices form the current DFS path, so an edge to an active vertex proves a cycle. An edge to a finished vertex alone does not. Without cycles, every edge destination finishes before its source; reversing exit order therefore respects every dependency. Searching all components detects disconnected cycles and self-loops. Cycles return an empty order; an empty graph is acyclic.

### DAG shortest paths and routes

DFS checks the whole graph first, including components unreachable from the source. Any cycle returns false and two empty arrays. Otherwise, distances start at zero for the source and null elsewhere. Only reachable vertices relax edges in topological order. All predecessors have been processed before a vertex, so induction over that order proves its shortest distance is final, even with negative weights.

Each strict improvement saves the original incoming edge ID. The edge ends at the updated vertex and extends a shortest source path. Following parents moves backwards in topological order and reaches the source. The helper follows these IDs and reverses the collected vertices. It expects a parent array from a successful acyclic result. It returns `[source]` for the source and `[]` for an unreachable target. A finite distance of -1 is valid; only null means unreachable. Ties keep the first found parent.

### Complexity and limits

Here `n` is the vertex count, `m` the edge count, `q` the union count and `L` the route length. Memory includes results.

| Method | Time | Memory |
| --- | --- | --- |
| DSU | `O(n + q alpha(n))` amortised | `O(n + q)` |
| Kruskal | `O(n + m log(m+1))` | `O(n + m)` |
| Prim with lazy heap | `O(n + m log(m+1))` | `O(n + m)` |
| Kosaraju | `O(n + m)` expected with hashing | `O(n + m)` |
| DFS ordering | `O(n + m)` | `O(n + m)` |
| DAG paths | `O(n + m)` | `O(n + m)` |
| Route helper | `O(L)` | `O(L)` |

DFS uses explicit stacks, not recursion. `next[v]` retains neighbour progress, so each edge is examined once. The second SCC pass marks vertices when pushing; its stack also holds at most `n` vertices. Every request creates fresh algorithm state. DSU's short recursive `find` is safe: union by size limits its parent depth to `O(log n)`, at most 17 active calls for these limits. Path compression only reduces that depth. Prim adds all incident edges when visiting a vertex and skips internal edges when removing them; each undirected edge is queued twice at most.

Costs use `long`, and distances use `Long[]`. A forest or DAG path has at most `n-1` edges, so its absolute total is at most 99999000000000 within these limits. Original edge IDs are preserved. SCC pair keys use `(long) a * n + b`, which uniquely encodes our labels from zero to `n-1`.

## Own tests

The assignment specifies coverage, not a minimum number of own cases. This compact set uses 16 fixed requests, combining related boundaries. No random test campaign is included. Expected answers are written explicitly in `tests/check.py` and justified below. `tests/actual.jsonl` records the actual answers in the same line order.

| Lines | Expected answer and justification |
| --- | --- |
| 1 | Ping returns `status=ready`. |
| 2-3 | Empty DSU states, then `[[5,1],[4,2],[3,2],[2,4],[2,4]]`. Self/repeated unions preserve the partition; vertex 4 stays isolated. |
| 4, 6 | Empty forests: cost 0, no edges, 0 components. |
| 5, 7 | Both methods: cost -3000000000 and 3 components. The negative chain on 0-3 needs IDs 1 or 2, then 3 and 4. On 4-5, ID 7 costs zero and beats ID 6. Vertex 6 has only a loop. This covers disconnection, isolates, loops, parallel/tied edges, zero/negative weights and a total beyond 32 bits. |
| 8-9 | Empty SCC output, then groups `{0,1}`, `{2,3}`, `{4}`. Exactly one condensation edge goes from the first group to the second, despite duplicate input edges. |
| 10-11 | Empty order, then any order with 0 and 1 before 2, and 2 before 3. Vertex 4 is isolated; duplicate edges do not make a cycle. |
| 12 | A self-loop in a separate component makes the graph cyclic. |
| 13 | Source alone: distance `[0]`, parent `[-1]`. |
| 14 | From source 1: `[null,0,2,-1,0,1000000000,2000000000,3000000000,null,null]`. Paths to 2 have parallel choices; paths to 3 tie at -1. The three positive edges reach 3000000000. The negative edge 8 to 9 remains unreachable. |
| 15-16 | An unreachable two-vertex cycle and an unreachable self-loop both give false and empty distance/parent arrays. |

All 16 requests run twice in the same process to check state reset. Forest validation checks unique IDs, no loops, total weight, edge count and component coverage against the known optimum. SCC validation compares partitions and exact condensation pairs. Topological checks verify every vertex and edge precedence. Parent checks verify edge direction, shortest distance and that the chain connects to the source, using an independent Kahn ordering.

Four checker examples also accept alternative valid forest edges, SCC labels, topological orders and parallel-edge parents, then reject an invalid witness of each type. This avoids treating one particular valid output as the only answer.

There is one generated full-limit request per algorithm, each with 100000 vertices and 250000 edges or operations:

| Method | Input and independently known answer |
| --- | --- |
| DSU | Chain unions followed by self-unions. After zero-based operation `i` of the chain, count is `n-i-1` and largest is `i+2`; later states stay `[1,n]`. |
| Kruskal and Prim | 100 disjoint chains of 1000 vertices, negative chain weights, extra positive edges inside a component. The 99900 chain edges are the unique optimum, costing -99900000000000. Both methods are checked separately. |
| SCC | A forward chain with a back edge inside each block of ten, plus duplicate edges. There are 10000 SCCs and 9999 condensation edges. |
| Topological order | A deep chain plus duplicate forward shortcuts. Only increasing vertex order is valid. |
| DAG paths | Chain weights -1000000000 and extra zero-weight forward shortcuts. The chain is optimal, so distance to vertex `v` is `-v * 1000000000`. |

The generators are in `large_cases()`. Large results are checked fully and logged as short summaries. Each input batch has a 20-second timeout and runs with `-Xmx384m`, matching the official launcher. All fixed cases, checker examples and full-limit tests passed in the saved run.

### Actual route-helper calls

These calls use the graph in case 14 and its returned parent array. `Check.java` checks the vertex sequence, edge direction and total weight.

| Call | Vertices | Edge IDs | Weight |
| --- | --- | --- | --- |
| `route(1,4,edges,parent)` | `[1,3,4]` | `[3,4]` | `-1+1=0` |
| `route(1,1,edges,parent)` | `[1]` | `[]` | 0 |
| `route(1,9,edges,parent)` | `[]` | `[]` | No route; distance is null |
| `route(1,2,edges,parent)` | `[1,2]` | `[0]` | 2 |

ID 1 is an equally good parallel edge to 2. Both `1->3` and `1->2->3` cost -1; the first found direct route is retained.

## Benchmark

Recorded on 25 October 2026: Windows 11 Home 10.0.26200, Intel Core i7-13650HX, 14 cores/20 logical processors, about 16 GB RAM. Runtime: Oracle JDK 25.0.1+8-LTS-27, 64-bit HotSpot, `-Xmx1g`; compilation targets Java 17. The Python runner was version 3.12.14.

`Check.graph(n,m)` uses `java.util.Random(73L+n)`. The first `n-1` edges form a chain with negative weights from -1000000000 to -1. Remaining random edges have weights from 0 to 1000000000. Both methods get the identical edge array. The negative chain is the unique optimum: replacing any chain edge with a nonnegative edge raises the cost. Before timing and after each measured call, the checker verifies the cost, one component and every chain edge exactly once.

Each method has one validated warm-up call per size and exactly three measured calls. Their order alternates. `System.nanoTime()` measures the whole algorithm call, including its internal adjacency/DSU/heap construction, sorting, allocation and result creation. Input generation, parsing, validation, printing, JVM startup and compilation are excluded for both methods. No explicit garbage collection is requested.

Times below are milliseconds from `tests/results.txt`; the median is the middle of the three sorted values.

| n | m | Seed | Method | Run 1 | Run 2 | Run 3 | Median |
| --- | --- | --- | --- | --- | --- | --- | --- |
| 1000 | 2500 | 1073 | Kruskal | 1.446 | 1.200 | 1.095 | 1.200 |
| 1000 | 2500 | 1073 | Prim | 2.114 | 2.081 | 1.883 | 2.081 |
| 10000 | 25000 | 10073 | Kruskal | 12.959 | 22.358 | 19.767 | 19.767 |
| 10000 | 25000 | 10073 | Prim | 18.505 | 18.060 | 18.976 | 18.505 |
| 100000 | 250000 | 100073 | Kruskal | 162.383 | 139.743 | 112.075 | 139.743 |
| 100000 | 250000 | 100073 | Prim | 318.580 | 322.200 | 354.353 | 322.200 |

Kruskal had a lower median at the first and largest sizes; Prim at the middle size. This is one sparse graph family with a known optimum, not evidence of a universal winner. One warm-up and three samples leave considerable JIT, garbage-collection and system noise. Those effects were not separately measured. The complexity bounds above come from algorithm analysis, not these timings.

## Status and sources

The official template was integrated from commit `76c1d91daa90ceadcb225f5408d54d4bfcf397f9`. The Java input support was reformatted and comments were removed from Java and Python support files. The checking logic and `tools/public.json` were preserved. Unused language starters, copies of the assignment PDFs, template README files and regression tests for maintaining the template's checking tools were removed. The workflow installs only Java 17 and Python 3.12 and runs `python tools/run.py --setup` and `python tools/run.py --check`. All 36 public cases and the repeated batch remain intact. These checks passed locally. The actual Moodle deadline has not been verified, and Moodle submission is a separate step.

Sources: the [assignment specification and template](https://github.com/RZhaniya/daa-assignment-3-graphs); the supplied English/Russian PDFs; Lecture 7 for Kosaraju, DFS ordering and DAG relaxation; and the syllabus for submission and AI-use policies. The assignment's null marker takes precedence over the lecture's general suggestion of -1 for unreachable distances.

ChatGPT/Codex assisted with the algorithm code, tests, template integration and this analysis, and ran the recorded checks and measurements. Verification consists of the explicit expected answers, semantic checks, full-limit tests and official Public checks above. A separate manual review by the student has not yet been confirmed.
