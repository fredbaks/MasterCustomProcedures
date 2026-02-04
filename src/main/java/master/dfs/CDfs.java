package master.dfs;

import java.util.ArrayList;
import java.util.List;

import org.neo4j.gds.api.Graph;
import org.neo4j.gds.collections.ha.HugeLongArray;
import org.neo4j.gds.core.utils.paged.HugeLongArrayStack;
import org.neo4j.logging.Log;

import com.carrotsearch.hppc.BitSet;

public class CDfs {

    private Graph graph;
    private long source;
    private long target;
    private long k;
    private Log log;

    private ArrayList<HugeLongArray> results;

    private HugeLongArrayStack stack;
    private BitSet visited;

    public ArrayList<HugeLongArray> startCDfs() {

        log.debug("Started dfs");

        results = new ArrayList<HugeLongArray>();

        stack = HugeLongArrayStack.newStack(graph.nodeCount());
        stack.push(source);

        visited = new BitSet(graph.nodeCount());

        HugeLongArray path = HugeLongArray.newArray(k + 1);

        computeCDfs(path, source, 0);

        return results;
    }

    public CDfs(Graph graph, long source, long target, long k, Log log) {
        this.graph = graph;
        this.source = source;
        this.target = target;
        this.k = k;
        this.log = log;
    }

    private void computeCDfs(HugeLongArray path, long current, int hopCount) {

        log.debug("ComputeDfs new depth, hops: " + hopCount);

        visited.set(source);
        path.set(hopCount, current);

        if (current == target) {
            log.debug("New path to results: " + path.toString());
            results.add(path.copyOf(hopCount + 1));
            path.set(hopCount, 0);
            return;
        }

        if (hopCount <= k || k == -1) {
            log.debug("degree of " + current + ": " + graph.degree(current));

            List<Long> neighbors = new ArrayList<Long>();

            graph.forEachRelationship(current, (long source, long neighbor) -> {

                log.debug("Continouing with neighbor: " + neighbor + " visited: " + visited.get(neighbor));

                if (!visited.get(neighbor)) {
                    neighbors.add(neighbor);
                }

                return true;
            });

            for (long neighbor : neighbors) {
                computeCDfs(path, neighbor, hopCount + 1);
            }
        }

        log.debug("Finished with " + current);
        visited.clear(current);
        path.set(hopCount, 0);
        return;
    }
}
