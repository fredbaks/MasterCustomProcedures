package master.dfs;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Optional;

import org.neo4j.gds.api.Graph;
import org.neo4j.gds.collections.ha.HugeLongArray;
import org.neo4j.gds.core.utils.paged.HugeLongArrayStack;
import org.neo4j.logging.Log;

import com.carrotsearch.hppc.BitSet;

public class Dfs {

    public static HugeLongArray computeDfs(Graph graph, long source, Log log) {

        HugeLongArray result = HugeLongArray.newArray(graph.nodeCount());

        log.debug("Started dfs");
        DfsVisitor traverse = new DfsVisitor(graph, source, log);

        int counter = 0;
        while (traverse.hasNext()) {
            log.debug("On " + counter + " iteration");
            result.set(counter, traverse.next());
            counter++;
        }

        log.debug("Stopped after " + counter + " iterations");

        return result;
    }

    public static ArrayList<Long> computeDfs(Long source, Optional<Long> potentialTarget, Graph graph, Log log,
            int hopLimit) {

        Long target = potentialTarget.isPresent() ? potentialTarget.get() : -1;

        HugeLongArrayStack stack = HugeLongArrayStack.newStack(graph.nodeCount());
        stack.push(source);

        HashMap<Long, Long> prev = new HashMap<Long, Long>();

        BitSet visited = new BitSet();

        visited.set(source);

        Long currentNode = -1L;

        HashMap<Long, Integer> depthMap = new HashMap<Long, Integer>();
        depthMap.put(source, 0);
        int currentDepth = -1;

        while (!stack.isEmpty()) {
            currentNode = stack.pop();
            currentDepth = depthMap.get(currentNode);

            if (currentNode.equals(target) || (hopLimit != -1 && currentDepth == hopLimit)) {
                break;
            }

            ArrayList<Long> neighbors = new ArrayList<Long>();
            final int depth = currentDepth;

            graph.forEachRelationship(currentNode, (current, neighbor) -> {
                if (!visited.get(neighbor)) {
                    visited.set(neighbor);
                    neighbors.add(neighbor);
                    prev.put(neighbor, current);
                    depthMap.put(neighbor, depth + 1);
                }

                return true;
            });

            for (Long neighbor : neighbors) {
                stack.push(neighbor);
            }
        }

        if (target != -1 && !currentNode.equals(target)) {
            return new ArrayList<Long>();
        }

        HugeLongArrayStack path = HugeLongArrayStack.newStack(graph.nodeCount());
        ArrayList<Long> result = new ArrayList<Long>();

        path.push(currentNode);

        while (!currentNode.equals(source)) {

            currentNode = prev.get(currentNode);

            if (currentNode == null) {
                return new ArrayList<Long>();
            }

            path.push(currentNode);
        }

        while (!path.isEmpty()) {
            result.add(path.pop());
        }

        return result;
    }
}
