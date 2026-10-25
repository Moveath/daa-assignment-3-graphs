import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;

public class Algorithms
{
    public static Map<String, Object> dsu(Map<String, Object> input)
    {
        return dsu(Input.vertexCount(input), Input.operations(input));
    }

    public static Map<String, Object> kruskal(Map<String, Object> input)
    {
        return kruskal(Input.vertexCount(input), Input.edges(input));
    }

    public static Map<String, Object> prim(Map<String, Object> input)
    {
        return prim(Input.vertexCount(input), Input.edges(input));
    }

    public static Map<String, Object> scc(Map<String, Object> input)
    {
        return scc(Input.vertexCount(input), Input.edges(input));
    }

    public static Map<String, Object> topo(Map<String, Object> input)
    {
        return topo(Input.vertexCount(input), Input.edges(input));
    }

    public static Map<String, Object> dag(Map<String, Object> input)
    {
        return dag(Input.vertexCount(input), Input.edges(input), Input.source(input));
    }

    static class DSU
    {
        int[] parent, size;
        int count, largest;

        DSU(int n)
        {
            parent = new int[n];
            size = new int[n];
            count = n;
            largest = n == 0 ? 0 : 1;
            for (int i = 0; i < n; i++)
            {
                parent[i] = i;
                size[i] = 1;
            }
        }

        int find(int v)
        {
            if (v != parent[v])
            {
                parent[v] = find(parent[v]);
            }
            return parent[v];
        }

        boolean union(int a, int b)
        {
            a = find(a);
            b = find(b);
            if (a == b)
            {
                return false;
            }
            if (size[a] < size[b])
            {
                int temp = a;
                a = b;
                b = temp;
            }
            parent[b] = a;
            size[a] += size[b];
            largest = Math.max(largest, size[a]);
            count--;
            return true;
        }
    }

    public static Map<String, Object> dsu(int n, int[][] operations)
    {
        DSU sets = new DSU(n);
        int[][] states = new int[operations.length][2];
        for (int i = 0; i < operations.length; i++)
        {
            sets.union(operations[i][0], operations[i][1]);
            states[i][0] = sets.count;
            states[i][1] = sets.largest;
        }
        return Map.of("states", states);
    }

    public static Map<String, Object> kruskal(int n, int[][] edges)
    {
        Integer[] ids = new Integer[edges.length];
        for (int i = 0; i < ids.length; i++)
        {
            ids[i] = i;
        }
        Arrays.sort(ids, (a, b) -> Integer.compare(edges[a][2], edges[b][2]));
        DSU sets = new DSU(n);
        ArrayList<Integer> chosen = new ArrayList<>();
        long cost = 0;
        for (int id : ids)
        {
            if (sets.union(edges[id][0], edges[id][1]))
            {
                chosen.add(id);
                cost += edges[id][2];
            }
        }
        return Map.of("cost", cost, "edge_ids", chosen, "components", sets.count);
    }

    public static Map<String, Object> prim(int n, int[][] edges)
    {
        ArrayList<ArrayList<Integer>> graph = newGraph(n);
        for (int i = 0; i < edges.length; i++)
        {
            graph.get(edges[i][0]).add(i);
            graph.get(edges[i][1]).add(i);
        }
        PriorityQueue<Integer> queue = new PriorityQueue<>(
                (a, b) -> Integer.compare(edges[a][2], edges[b][2]));
        boolean[] used = new boolean[n];
        ArrayList<Integer> chosen = new ArrayList<>();
        long cost = 0;
        int count = 0;
        for (int start = 0; start < n; start++)
        {
            if (used[start])
            {
                continue;
            }
            count++;
            used[start] = true;
            queue.addAll(graph.get(start));
            while (!queue.isEmpty())
            {
                int id = queue.poll();
                int a = edges[id][0];
                int b = edges[id][1];
                if (used[a] && used[b])
                {
                    continue;
                }
                int v = used[a] ? b : a;
                used[v] = true;
                chosen.add(id);
                cost += edges[id][2];
                queue.addAll(graph.get(v));
            }
        }
        return Map.of("cost", cost, "edge_ids", chosen, "components", count);
    }

    static ArrayList<ArrayList<Integer>> newGraph(int n)
    {
        ArrayList<ArrayList<Integer>> graph = new ArrayList<>(n);
        for (int i = 0; i < n; i++)
        {
            graph.add(new ArrayList<>());
        }
        return graph;
    }

    static ArrayList<Integer> finishOrder(ArrayList<ArrayList<Integer>> graph, boolean checkCycle)
    {
        int n = graph.size();
        int[] state = new int[n];
        int[] next = new int[n];
        int[] stack = new int[n];
        ArrayList<Integer> order = new ArrayList<>();
        for (int start = 0; start < n; start++)
        {
            if (state[start] != 0)
            {
                continue;
            }
            int top = 0;
            stack[top] = start;
            state[start] = 1;
            while (top >= 0)
            {
                int v = stack[top];
                if (next[v] == graph.get(v).size())
                {
                    state[v] = 2;
                    order.add(v);
                    top--;
                    continue;
                }
                int to = graph.get(v).get(next[v]++);
                if (state[to] == 0)
                {
                    state[to] = 1;
                    stack[++top] = to;
                }
                else if (checkCycle && state[to] == 1)
                {
                    return null;
                }
            }
        }
        Collections.reverse(order);
        return order;
    }

    public static Map<String, Object> scc(int n, int[][] edges)
    {
        ArrayList<ArrayList<Integer>> graph = newGraph(n);
        ArrayList<ArrayList<Integer>> reverse = newGraph(n);
        for (int[] edge : edges)
        {
            graph.get(edge[0]).add(edge[1]);
            reverse.get(edge[1]).add(edge[0]);
        }
        ArrayList<Integer> order = finishOrder(graph, false);
        int[] component = new int[n];
        Arrays.fill(component, -1);
        int[] stack = new int[n];
        int count = 0;
        for (int start : order)
        {
            if (component[start] != -1)
            {
                continue;
            }
            int top = 0;
            stack[top] = start;
            component[start] = count;
            while (top >= 0)
            {
                int v = stack[top--];
                for (int to : reverse.get(v))
                {
                    if (component[to] == -1)
                    {
                        component[to] = count;
                        stack[++top] = to;
                    }
                }
            }
            count++;
        }
        HashSet<Long> seen = new HashSet<>();
        ArrayList<int[]> condensation = new ArrayList<>();
        for (int[] edge : edges)
        {
            int a = component[edge[0]];
            int b = component[edge[1]];
            if (a != b && seen.add((long) a * n + b))
            {
                condensation.add(new int[]{a, b});
            }
        }
        return Map.of("component", component, "condensation", condensation);
    }

    public static Map<String, Object> topo(int n, int[][] edges)
    {
        ArrayList<ArrayList<Integer>> graph = newGraph(n);
        for (int[] edge : edges)
        {
            graph.get(edge[0]).add(edge[1]);
        }
        ArrayList<Integer> order = finishOrder(graph, true);
        return Map.of("acyclic", order != null, "order", order == null ? List.of() : order);
    }

    public static Map<String, Object> dag(int n, int[][] edges, int source)
    {
        ArrayList<ArrayList<Integer>> graph = newGraph(n);
        ArrayList<ArrayList<Integer>> outgoing = newGraph(n);
        for (int i = 0; i < edges.length; i++)
        {
            graph.get(edges[i][0]).add(edges[i][1]);
            outgoing.get(edges[i][0]).add(i);
        }
        ArrayList<Integer> order = finishOrder(graph, true);
        if (order == null)
        {
            return Map.of("acyclic", false, "distance", List.of(), "parent_edge", List.of());
        }
        Long[] distance = new Long[n];
        int[] parent = new int[n];
        Arrays.fill(parent, -1);
        distance[source] = 0L;
        for (int v : order)
        {
            if (distance[v] == null)
            {
                continue;
            }
            for (int id : outgoing.get(v))
            {
                int to = edges[id][1];
                long value = distance[v] + edges[id][2];
                if (distance[to] == null || value < distance[to])
                {
                    distance[to] = value;
                    parent[to] = id;
                }
            }
        }
        return Map.of("acyclic", true, "distance", distance, "parent_edge", parent);
    }

    public static List<Integer> route(int source, int target, int[][] edges, int[] parent)
    {
        ArrayList<Integer> path = new ArrayList<>();
        int v = target;
        while (v != source)
        {
            if (parent[v] == -1)
            {
                return List.of();
            }
            path.add(v);
            v = edges[parent[v]][0];
        }
        path.add(source);
        Collections.reverse(path);
        return path;
    }
}
