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

        HugeLongArrayQueue queue = HugeLongArrayQueue.newQueue(graph.nodeCount());
        queue.add(source);

        HashMap<Long, Long> prev = new HashMap<Long, Long>();

        BitSet visited = new BitSet();

        visited.set(source);

        ignoreList.ifPresent((set) -> {
            for (Long node : set) {
                if (node != target) {
                    visited.set(node);
                }
            }
        });

        Long currentNode = -1L;

        while (!queue.isEmpty()) {
            currentNode = queue.remove();

            if (currentNode == target) {
                break;
            }

            ArrayList<Long> neighbors = new ArrayList<Long>();

            graph.forEachRelationship(currentNode, (current, neighbor) -> {
                if (!visited.get(neighbor)) {
                    neighbors.add(neighbor);
                    prev.put(neighbor, current);
                }

                return true;
            });

            for (Long neighbor : neighbors) {
                visited.set(neighbor);
                queue.add(neighbor);
            }
        }

        if (currentNode != target) {
            return new ArrayList<Long>();
        }

        HugeLongArrayStack path = HugeLongArrayStack.newStack(graph.nodeCount());
        ArrayList<Long> result = new ArrayList<Long>();

        path.push(currentNode);

        while (currentNode != source) {

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
