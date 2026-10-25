import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;

public class Check
{
    static void require(boolean value)
    {
        if (!value)
        {
            throw new AssertionError("Check failed");
        }
    }

    static void checkRoute(int source, int target, int[][] edges, Map<String, Object> result,
                           List<Integer> expected)
    {
        int[] parent = (int[]) result.get("parent_edge");
        Long[] distance = (Long[]) result.get("distance");
        List<Integer> path = Algorithms.route(source, target, edges, parent);
        require(path.equals(expected));
        long cost = 0;
        int[] ids = new int[Math.max(0, path.size() - 1)];
        for (int i = 1; i < path.size(); i++)
        {
            int id = parent[path.get(i)];
            ids[i - 1] = id;
            require(edges[id][0] == path.get(i - 1) && edges[id][1] == path.get(i));
            cost += edges[id][2];
        }
        require(path.isEmpty() ? distance[target] == null : distance[target] == cost);
        System.out.println("route(" + source + ", " + target + ") = " + path
                + "; edge_ids=" + Arrays.toString(ids) + "; cost="
                + (path.isEmpty() ? "no route" : cost));
    }

    static void routes()
    {
        int[][] edges = {{1, 2, 2}, {1, 2, 2}, {2, 3, -3}, {1, 3, -1}, {3, 4, 1},
                {1, 5, 1000000000}, {5, 6, 1000000000}, {6, 7, 1000000000}, {8, 9, -10}};
        Map<String, Object> result = Algorithms.dag(10, edges, 1);
        checkRoute(1, 4, edges, result, List.of(1, 3, 4));
        checkRoute(1, 1, edges, result, List.of(1));
        checkRoute(1, 9, edges, result, List.of());
        checkRoute(1, 2, edges, result, List.of(1, 2));
    }

    static int[][] graph(int n, int m)
    {
        Random random = new Random(73L + n);
        int[][] edges = new int[m][3];
        for (int i = 0; i < n - 1; i++)
        {
            edges[i] = new int[]{i, i + 1, -1 - random.nextInt(1000000000)};
        }
        for (int i = n - 1; i < m; i++)
        {
            edges[i] = new int[]{random.nextInt(n), random.nextInt(n), random.nextInt(1000000001)};
        }
        return edges;
    }

    static void forest(int n, int[][] edges, Map<String, Object> result)
    {
        long expected = 0;
        for (int i = 0; i < n - 1; i++)
        {
            expected += edges[i][2];
        }
        require(((Number) result.get("cost")).longValue() == expected);
        require(((Number) result.get("components")).intValue() == 1);
        List<?> ids = (List<?>) result.get("edge_ids");
        require(ids.size() == n - 1);
        boolean[] used = new boolean[n - 1];
        for (Object item : ids)
        {
            int id = ((Number) item).intValue();
            require(id >= 0 && id < n - 1 && !used[id]);
            used[id] = true;
        }
    }

    static void benchmark()
    {
        System.out.println("BENCHMARK: System.nanoTime, milliseconds, 1 warm-up and 3 measured calls per method/size");
        System.out.println("Runtime: " + System.getProperty("java.runtime.version")
                + "; VM: " + System.getProperty("java.vm.name")
                + "; OS: " + System.getProperty("os.name") + " " + System.getProperty("os.arch"));
        for (int n : new int[]{1000, 10000, 100000})
        {
            int m = n * 5 / 2;
            int[][] edges = graph(n, m);
            forest(n, edges, Algorithms.kruskal(n, edges));
            forest(n, edges, Algorithms.prim(n, edges));
            double[][] times = new double[2][3];
            for (int run = 0; run < 3; run++)
            {
                for (int turn = 0; turn < 2; turn++)
                {
                    int method = (run + turn) % 2;
                    long start = System.nanoTime();
                    Map<String, Object> result = method == 0
                            ? Algorithms.kruskal(n, edges) : Algorithms.prim(n, edges);
                    times[method][run] = (System.nanoTime() - start) / 1000000.0;
                    forest(n, edges, result);
                }
            }
            for (int method = 0; method < 2; method++)
            {
                double[] sorted = times[method].clone();
                Arrays.sort(sorted);
                System.out.printf(Locale.ROOT,
                        "n=%d m=%d seed=%d %s: %.3f %.3f %.3f; median=%.3f ms%n",
                        n, m, 73 + n, method == 0 ? "Kruskal" : "Prim",
                        times[method][0], times[method][1], times[method][2], sorted[1]);
            }
        }
    }

    public static void main(String[] args) throws Exception
    {
        if (args.length > 0 && args[0].equals("bench"))
        {
            benchmark();
        }
        else
        {
            Path cases = Path.of("tests", "cases.jsonl");
            if (!Files.exists(cases))
            {
                cases = Path.of("Assignment 3", "tests", "cases.jsonl");
            }
            System.out.println("Example results (in the order of tests/cases.jsonl):");
            InputStream saved = System.in;
            try (InputStream input = Files.newInputStream(cases))
            {
                System.setIn(input);
                Main.main(new String[0]);
            }
            finally
            {
                System.setIn(saved);
            }
            System.out.println("Route checks:");
            routes();
        }
    }
}
