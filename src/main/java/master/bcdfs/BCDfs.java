package master.bcdfs;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.neo4j.gds.api.Graph;
import org.neo4j.gds.collections.ha.HugeLongArray;
import org.neo4j.logging.Log;

import com.carrotsearch.hppc.BitSet;

public class BCDfs {

    private Graph graph;
    private long source;
    private long target;
    private long k;
    private Log log;

    private ArrayList<HugeLongArray> results;

    private BitSet visited;
    private HashMap<Long, Long> bar;

    public ArrayList<HugeLongArray> startBCDfs() {

        log.debug("Started BC-Dfs");
        results = new ArrayList<HugeLongArray>();

        visited = new BitSet(graph.nodeCount());

        bar = new HashMap<Long, Long>();

        HugeLongArray path = HugeLongArray.newArray(1);

        computeBcDfs(path, source, 0);

        return results;
    }

    public BCDfs(Graph graph, long source, long target, long k, Log log) {
        this.graph = graph;
        this.source = source;
        this.target = target;
        this.k = k;
        this.log = log;
    }

    private long computeBcDfs(HugeLongArray oldPath, long current, int hopCount) {

        long F = k + 1;

        HugeLongArray path = oldPath.copyOf(hopCount + 1);

        path.set(hopCount, current);

        if (current == target) {
            results.add(path.copyOf(hopCount + 1));
            return 0L;
        }

        visited.set(current);

        if (hopCount < k) {

            List<Long> neighbors = new ArrayList<Long>();

            graph.forEachRelationship(current, (long source, long neighbor) -> {

                if (!visited.get(neighbor)) {
                    neighbors.add(neighbor);
                }

                return true;
            });

            for (long neighbor : neighbors) {
                if (hopCount + 1 + bar.getOrDefault(neighbor, 0L) <= k) {
                    long f = computeBcDfs(path, neighbor, hopCount + 1);
                    if (f != k + 1) {
                        F = Long.min(F, f + 1);
                    }
                }
            }
        }

        if (F == k + 1) {
            bar.put(current, k - (hopCount + 1) + 1);
        } else {
            updateBarrier(current, F);
        }

        visited.clear(current);
        return F;
    }

    private void updateBarrier(long node, long l) {
        if (bar.getOrDefault(node, 0L) > l) {
            bar.put(node, l);

            List<Long> neighbours = new ArrayList<Long>();

            graph.forEachInverseRelationship(node, (long source, long incomingNeighbour) -> {
                neighbours.add(incomingNeighbour);

                return true;
            });

            for (long incomingNeighbour : neighbours) {
                updateBarrier(incomingNeighbour, l + 1);
            }
        }
    }
}
