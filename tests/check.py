import json
import subprocess
import time
from collections import deque
from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]
LOG = []


def report(text):
    print(text, flush=True)
    LOG.append(text)


def parts(n, edges):
    graph = [[] for _ in range(n)]
    for a, b, *_ in edges:
        graph[a].append(b)
        graph[b].append(a)
    labels = [-1] * n
    count = 0
    for start in range(n):
        if labels[start] != -1:
            continue
        labels[start] = count
        stack = [start]
        while stack:
            for v in graph[stack.pop()]:
                if labels[v] == -1:
                    labels[v] = count
                    stack.append(v)
        count += 1
    return labels, count


def order(n, edges):
    graph = [[] for _ in range(n)]
    degree = [0] * n
    for a, b, *_ in edges:
        graph[a].append(b)
        degree[b] += 1
    queue = deque(v for v in range(n) if degree[v] == 0)
    result = []
    while queue:
        v = queue.popleft()
        result.append(v)
        for to in graph[v]:
            degree[to] -= 1
            if degree[to] == 0:
                queue.append(to)
    return result if len(result) == n else None


def validate(req, out, want):
    task = req['task']
    if task == 'ping':
        assert out == {'status': 'ready'}
        return
    n = req['n']
    edges = req.get('edges', [])
    if task == 'dsu':
        assert out == {'states': want}
    elif task in ('kruskal', 'prim'):
        ids = out['edge_ids']
        assert (out['cost'], out['components']) == want
        assert len(ids) == n - want[1] and len(set(ids)) == len(ids)
        assert all(type(i) is int and 0 <= i < len(edges) for i in ids)
        assert all(edges[i][0] != edges[i][1] for i in ids)
        assert sum(edges[i][2] for i in ids) == out['cost']
        assert parts(n, [edges[i] for i in ids])[0] == parts(n, edges)[0]
    elif task == 'scc':
        labels = out['component']
        assert len(labels) == n and all(type(v) is int and 0 <= v < 2**63 for v in labels)
        forward, back = {}, {}
        for actual, target in zip(labels, want):
            assert forward.setdefault(actual, target) == target
            assert back.setdefault(target, actual) == actual
        pairs = out['condensation']
        assert all(len(pair) == 2 for pair in pairs)
        wanted = {(labels[a], labels[b]) for a, b in edges if labels[a] != labels[b]}
        assert len(pairs) == len(wanted) and set(map(tuple, pairs)) == wanted
    elif task == 'topo':
        assert out['acyclic'] is (want is not None)
        if want is None:
            assert out['order'] == []
        else:
            path = out['order']
            assert len(path) == n and set(path) == set(range(n))
            pos = {v: i for i, v in enumerate(path)}
            assert all(pos[a] < pos[b] for a, b in edges)
    else:
        assert out['acyclic'] is (want is not None)
        if want is None:
            assert out['distance'] == [] and out['parent_edge'] == []
            return
        assert out['distance'] == want
        parent = out['parent_edge']
        assert len(parent) == n
        reached = {req['source']}
        for v in order(n, edges):
            if v == req['source'] or want[v] is None:
                assert parent[v] == -1
                continue
            id = parent[v]
            assert type(id) is int and 0 <= id < len(edges)
            a, b, w = edges[id]
            assert b == v and a in reached and want[a] + w == want[v]
            reached.add(v)


def run(cases, timeout=20):
    data = ''.join(json.dumps(req, separators=(',', ':')) + '\n' for req in cases)
    start = time.perf_counter()
    result = subprocess.run(['java', '-Xmx384m', '-cp', 'build', 'Main'], input=data,
                            capture_output=True, text=True, encoding='utf-8', cwd=ROOT, timeout=timeout)
    elapsed = time.perf_counter() - start
    assert result.returncode == 0, result.stderr
    assert not result.stderr, result.stderr
    outputs = [json.loads(line) for line in result.stdout.splitlines()]
    assert len(outputs) == len(cases)
    return outputs, elapsed


def witnesses():
    cases = [
        ({'task': 'kruskal', 'n': 3, 'edges': [[0, 1, 1], [1, 2, 1], [0, 2, 1]]},
         [{'cost': 2, 'components': 1, 'edge_ids': ids} for ids in ([0, 1], [0, 2], [0, 0])]),
        ({'task': 'scc', 'n': 3, 'edges': [[0, 1], [1, 0], [1, 2]]},
         [{'component': [0, 0, 1], 'condensation': [[0, 1]]},
          {'component': [99, 99, 7000000000], 'condensation': [[99, 7000000000]]},
          {'component': [0, 0, 0], 'condensation': []}]),
        ({'task': 'topo', 'n': 3, 'edges': [[0, 2], [1, 2]]},
         [{'acyclic': True, 'order': path} for path in ([0, 1, 2], [1, 0, 2], [2, 0, 1])]),
        ({'task': 'dag', 'n': 3, 'source': 0, 'edges': [[0, 1, 1], [0, 1, 1], [1, 2, 2]]},
         [{'acyclic': True, 'distance': [0, 1, 3], 'parent_edge': ids}
          for ids in ([-1, 0, 2], [-1, 1, 2], [-1, 2, 0])])]
    wants = [(2, 1), [0, 0, 2], [0, 1, 2], [0, 1, 3]]
    for (req, answers), want in zip(cases, wants):
        validate(req, answers[0], want)
        validate(req, answers[1], want)
        try:
            validate(req, answers[2], want)
        except AssertionError:
            pass
        else:
            raise AssertionError('Invalid witness accepted: ' + req['task'])
    report('Semantic checkers: alternative valid forests/SCC labels/orders/parents accepted; invalid witnesses rejected: PASS')


def large_cases():
    n, m = 100000, 250000
    operations = [[i, i + 1] for i in range(n - 1)] + [[0, 0]] * (m - n + 1)
    want = [[n - i - 1, i + 2] for i in range(n - 1)] + [[1, n]] * (m - n + 1)
    req = {'task': 'dsu', 'n': n, 'operations': operations}
    outputs, elapsed = run([req])
    validate(req, outputs[0], want)
    report(f'Full DSU n={n} q={m}: PASS ({elapsed:.3f} s)')
    del operations, want, req, outputs
    edges = [[i, i + 1, -1000000000] for i in range(n - 1) if i % 1000 != 999]
    count = len(edges)
    edges += [[0, 999, 1000000000]] * (m - count)
    reqs = [{'task': task, 'n': n, 'edges': edges} for task in ('kruskal', 'prim')]
    outputs, elapsed = run(reqs)
    for req, out in zip(reqs, outputs):
        validate(req, out, (-count * 1000000000, 100))
    report(f'Full MSF n={n} m={m}: 100 components, 99900 edges, cost=-99900000000000: PASS ({elapsed:.3f} s)')
    edges = [[i, i + 1] for i in range(n - 1)]
    edges += [[i + 9, i] for i in range(0, n, 10)]
    edges += [[0, 1]] * (m - len(edges))
    req = {'task': 'scc', 'n': n, 'edges': edges}
    outputs, elapsed = run([req])
    validate(req, outputs[0], [v // 10 for v in range(n)])
    report(f'Full SCC n={n} m={m}: 10000 groups of 10, 9999 condensation edges: PASS ({elapsed:.3f} s)')
    chain = [[i, i + 1] for i in range(n - 1)]
    chain += [[0, 99999]] * (m - len(chain))
    req = {'task': 'topo', 'n': n, 'edges': chain}
    outputs, elapsed = run([req])
    validate(req, outputs[0], list(range(n)))
    report(f'Full DFS n={n} m={m}: deep order: PASS ({elapsed:.3f} s)')
    weighted = [[a, b, -1000000000 if b == a + 1 else 0] for a, b in chain]
    req = {'task': 'dag', 'n': n, 'source': 0, 'edges': weighted}
    outputs, elapsed = run([req])
    validate(req, outputs[0], [-v * 1000000000 for v in range(n)])
    report(f'Full DAG n={n} m={m}: distance[last]=-99999000000000, parent chain valid: PASS ({elapsed:.3f} s)')


def main():
    (ROOT / 'build').mkdir(exist_ok=True)
    files = sorted(str(path) for path in (ROOT / 'src' / 'Java').glob('*.java'))
    subprocess.run(['javac', '--release', '17', '-encoding', 'UTF-8', '-d', 'build',
                    *files, 'tests/Check.java'], cwd=ROOT, check=True, timeout=60)
    report('Build: javac --release 17: PASS')
    cases = [json.loads(line) for line in (ROOT / 'tests' / 'cases.jsonl').read_text(encoding='utf-8').splitlines()]
    wants = [
        None,
        [],
        [[5, 1], [4, 2], [3, 2], [2, 4], [2, 4]],
        (0, 0), (-3000000000, 3),
        (0, 0), (-3000000000, 3),
        [], [0, 0, 2, 2, 4],
        [], [0, 1, 2, 3, 4], None,
        [0], [None, 0, 2, -1, 0, 1000000000, 2000000000, 3000000000, None, None],
        None, None
    ]
    assert len(cases) == len(wants)
    outputs, elapsed = run(cases * 2)
    for req, out, want in zip(cases * 2, outputs, wants * 2):
        validate(req, out, want)
    (ROOT / 'tests' / 'actual.jsonl').write_text(
        ''.join(json.dumps(out, separators=(',', ':')) + '\n' for out in outputs[:len(cases)]), encoding='utf-8')
    report(f'Fixed cases: {len(cases)} requests repeated twice in one process: PASS ({elapsed:.3f} s)')
    witnesses()
    large_cases()
    for args in ([], ['bench']):
        result = subprocess.run(['java', '-Xmx1g', '-cp', 'build', 'Check', *args],
                                cwd=ROOT, capture_output=True, text=True, encoding='utf-8', check=True, timeout=60)
        report(result.stdout.rstrip())
    report('All own tests passed. Official Public checks use: python tools/run.py --check')
    (ROOT / 'tests' / 'results.txt').write_text('\n'.join(LOG) + '\n', encoding='utf-8')


if __name__ == '__main__':
    main()
