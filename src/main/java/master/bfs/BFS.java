package master.bfs;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Optional;
import java.util.Set;

import org.neo4j.gds.api.Graph;
import org.neo4j.gds.core.utils.paged.HugeLongArrayQueue;
import org.neo4j.gds.core.utils.paged.HugeLongArrayStack;
import org.neo4j.logging.Log;

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

    public static HashMap<Long, Integer> computeBFSTree(Long source, Graph graph, Log log,
            Optional<Set<Long>> ignoreList, int hopLimit) {
        return computeBFSTree(source, graph, log, ignoreList, hopLimit, false);
    }

    public static HashMap<Long, Integer> computeInverseBFSTree(Long source, Graph graph, Log log,
            Optional<Set<Long>> ignoreList, int hopLimit) {
        return computeBFSTree(source, graph, log, ignoreList, hopLimit, true);
    }

    public static HashMap<Long, Integer> computeBFSTree(Long source, Graph graph, Log log,
            Optional<Set<Long>> ignoreList, int hopLimit, boolean inverse) {

        HugeLongArrayQueue queue = HugeLongArrayQueue.newQueue(graph.nodeCount());
        queue.add(source);

        BitSet visited = new BitSet();

        visited.set(source);

        ignoreList.ifPresent((set) -> {
            for (Long node : set) {
                visited.set(node);
            }
        });

        HashMap<Long, Integer> depthMap = new HashMap<Long, Integer>();
        depthMap.put(source, 0);
        int currentDepth = -1;
        Long currentNode = -1L;

        while (!queue.isEmpty()) {
            currentNode = queue.remove();
            currentDepth = depthMap.get(currentNode);

            if ((hopLimit != -1 && currentDepth == hopLimit)) {
                break;
            }

            final int depth = currentDepth;

            if (inverse) {
                graph.forEachInverseRelationship(currentNode, (current, neighbor) -> {
                    if (!visited.get(neighbor)) {
                        visited.set(neighbor);
                        queue.add(neighbor);
                        depthMap.put(neighbor, depth + 1);
                    }
                    return true;
                });
            } else {
                graph.forEachRelationship(currentNode, (current, neighbor) -> {
                    if (!visited.get(neighbor)) {
                        visited.set(neighbor);
                        queue.add(neighbor);
                        depthMap.put(neighbor, depth + 1);
                    }
                    return true;
                });
            }
        }

        return depthMap;
    }

}
