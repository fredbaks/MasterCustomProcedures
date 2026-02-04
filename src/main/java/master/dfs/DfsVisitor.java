package master.dfs;

import org.neo4j.gds.api.Graph;
import org.neo4j.gds.collections.ha.HugeLongArray;
import org.neo4j.gds.core.utils.paged.HugeLongArrayStack;
import org.neo4j.logging.Log;

import com.carrotsearch.hppc.BitSet;

public class DfsVisitor {

    private final Graph graph;
    private final long nodeCount;
    private final HugeLongArrayStack stack;
    private final BitSet visited;
    private Log log;

    public DfsVisitor(Graph graph, long source, Log log) {
        this.log = log;
        this.graph = graph;
        this.nodeCount = graph.nodeCount();

        this.stack = HugeLongArrayStack.newStack(nodeCount);
        this.visited = new BitSet(nodeCount);

        stack.push(source);
        visited.set(source);
    }

    private DfsVisitor(Graph graph, Log log, HugeLongArrayStack stack, BitSet visited) {
        this.log = log;
        this.graph = graph;
        this.nodeCount = graph.nodeCount();

        this.stack = stack;
        this.visited = visited;
    }

    public boolean hasNext() {
        return !stack.isEmpty();
    }

    public Long next() {
        Long node = stack.pop();

        log.debug("current stacksize: " + stack.size());
        log.debug("current visited: " + visited.toString());

        graph.forEachRelationship(node, (long source, long target) -> {
            log.debug("target is " + target);
            if (!visited.get(target)) {
                log.debug("Found new target, and added to visited " + target);
                visited.set(target);
                stack.push(target);
            }
            return true;
        });

        return node;
    }

    public BitSet returnVisited() {
        return visited;
    }

    public DfsVisitor copy() {
        return new DfsVisitor(graph, log, stack, visited);
    }
}
