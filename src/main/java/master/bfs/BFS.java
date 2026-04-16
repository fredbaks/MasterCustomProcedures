package master.bfs;

import org.neo4j.logging.Log;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Optional;
import java.util.Set;

import org.neo4j.gds.api.Graph;
import org.neo4j.gds.core.utils.paged.HugeLongArrayQueue;
import org.neo4j.gds.core.utils.paged.HugeLongArrayStack;

import com.carrotsearch.hppc.BitSet;

public class BFS {

    public static ArrayList<Long> computeBFS(Long source, Long target, Graph graph, Log log,
            Optional<Set<Long>> ignoreList) {
        return computeBFS(source, target, graph, log, ignoreList, -1);
    }

    public static ArrayList<Long> computeBFS(Long source, Long target, Graph graph, Log log, int hopLimit) {
        return computeBFS(source, target, graph, log, Optional.empty(), hopLimit);
    }

    public static ArrayList<Long> computeBFS(Long source, Long target, Graph graph, Log log,
            Optional<Set<Long>> ignoreList, int hopLimit) {

        HugeLongArrayQueue queue = HugeLongArrayQueue.newQueue(graph.nodeCount());
        queue.add(source);

        HashMap<Long, Long> prev = new HashMap<Long, Long>();

        BitSet visited = new BitSet();

        visited.set(source);

        ignoreList.ifPresent((set) -> {
            for (Long node : set) {
                if (!node.equals(target)) {
                    visited.set(node);
                }
            }
        });

        Long currentNode = -1L;

        HashMap<Long, Integer> depthMap = new HashMap<Long, Integer>();
        depthMap.put(source, 0);
        int currentDepth = -1;

        while (!queue.isEmpty()) {
            currentNode = queue.remove();
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
                queue.add(neighbor);
            }
        }

        if (!currentNode.equals(target)) {
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
