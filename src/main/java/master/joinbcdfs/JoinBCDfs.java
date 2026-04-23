package master.joinbcdfs;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.neo4j.gds.api.Graph;
import org.neo4j.gds.collections.ha.HugeLongArray;
import org.neo4j.logging.Log;

import com.carrotsearch.hppc.BitSet;

public class JoinBCDfs {

    private Graph graph;
    private long source;
    private long target;
    private long k;
    private long kCeil;
    private long kFloor;
    private Log log;

    private HashMap<HugeLongArray, Long> results;
    private ArrayList<HugeLongArray> tempResults;
    private ArrayList<HugeLongArray> leftResults;
    private ArrayList<HugeLongArray> rightResults;

    private BitSet visited;
    private HashMap<Long, Long> bar;

    private HashMap<Long, Set<Long>> middleVertices;
    private Long virtualTarget;
    private BitSet targetSet;

    private Set<Long> emptySet = new HashSet<Long>();

    public HashMap<HugeLongArray, Long> startJoinBCDfs() {

        log.debug("Started Join BC-Dfs");

        results = new HashMap<HugeLongArray, Long>();
        tempResults = new ArrayList<HugeLongArray>();

        kCeil = (long) Math.ceil(((double) k) / 2);
        kFloor = (long) Math.floor(((double) k) / 2);

        findMiddleVertices();

        computeLeftPaths();
        computeRightPaths();

        joinPartialResults();

        return results;
    }

    public JoinBCDfs(Graph graph, long source, long target, long k, Log log) {
        this.graph = graph;
        this.source = source;
        this.target = target;
        this.k = k;
        this.log = log;
    }

    private void findMiddleVertices() {

        middleVertices = new HashMap<Long, Set<Long>>();

        HashSet<Long> lastLeftSide = new HashSet<Long>();
        HashSet<Long> lastRightSide = new HashSet<Long>();

        lastLeftSide.add(source);
        lastRightSide.add(target);

        for (int i = 1; i <= (double) kCeil; i++) {

            HashSet<Long> leftSide = new HashSet<Long>();
            HashSet<Long> rightSide = new HashSet<Long>();

            for (long node : lastLeftSide) {
                graph.forEachRelationship(node, (long currentNode, long outgoingNeighbour) -> {
                    if (outgoingNeighbour == source || outgoingNeighbour == target) {
                        return true;
                    }

                    leftSide.add(outgoingNeighbour);
                    return true;
                });
            }

            Set<Long> tempMiddle = new HashSet<Long>(leftSide);
            tempMiddle.retainAll(lastRightSide);

            middleVertices.put(2L * i - 1, tempMiddle);

            if (i == kFloor + 1) {
                break;
            }

            for (long node : lastRightSide) {
                graph.forEachInverseRelationship(node, (long currentNode, long incomingNeighbour) -> {
                    if (incomingNeighbour == source || incomingNeighbour == target) {
                        return true;
                    }

                    rightSide.add(incomingNeighbour);
                    return true;
                });
            }

            tempMiddle = new HashSet<Long>(leftSide);
            tempMiddle.retainAll(rightSide);

            middleVertices.put(2L * i, tempMiddle);

            lastLeftSide = leftSide;
            lastRightSide = rightSide;
        }
    }

    private void computeLeftPaths() {

        targetSet = new BitSet();

        middleVertices.forEach((pathLength, vertexSet) -> {
            for (Long vertex : vertexSet) {
                targetSet.set(vertex);
            }
        });

        virtualTarget = -99L;

        visited = new BitSet();
        bar = new HashMap<Long, Long>();

        HugeLongArray path = HugeLongArray.newArray(1);

        computeBcDfs(path, source, 0, kCeil + 1);

        leftResults = new ArrayList<HugeLongArray>(tempResults);
        tempResults.clear();
    }

    private void computeRightPaths() {

        targetSet = new BitSet();
        virtualTarget = target;

        visited = new BitSet();
        visited.set(source);
        bar = new HashMap<Long, Long>();

        Set<Long> startVertices = new HashSet<Long>();

        middleVertices.forEach((pathLength, vertexSet) -> {
            for (Long vertex : vertexSet) {
                startVertices.add(vertex);
            }
        });

        startVertices.forEach((vertex) -> {
            HugeLongArray path = HugeLongArray.newArray(1);
            // Does not use a virtual start vertex -> only kFloor
            computeBcDfs(path, vertex, 0, kFloor);
        });

        rightResults = new ArrayList<HugeLongArray>(tempResults);
        tempResults.clear();
    }

    private long computeBcDfs(HugeLongArray oldPath, long current, int hopCount, long searchDepth) {

        long F = searchDepth + 1;

        HugeLongArray path = oldPath.copyOf(hopCount + 1);

        path.set(hopCount, current);

        if (current == virtualTarget) {

            if ((virtualTarget != -99) ||
                    middleVertices.getOrDefault((long) (2 * (hopCount - 1)), emptySet).contains(path.get(hopCount - 1))
                    || middleVertices.getOrDefault((long) (2 * (hopCount - 1)) - 1, emptySet)
                            .contains(path.get(hopCount - 1))) {
                tempResults.add(path.copyOf(hopCount + 1));
            }
            return 0L;
        }

        visited.set(current);

        if (hopCount < searchDepth) {

            List<Long> neighbors = new ArrayList<Long>();

            if (targetSet.get(current)) {
                neighbors.add(virtualTarget);
            }

            graph.forEachRelationship(current, (long source, long neighbor) -> {
                if (!visited.get(neighbor)) {
                    neighbors.add(neighbor);
                }

                return true;
            });

            for (long neighbor : neighbors) {
                if (hopCount + 1 + bar.getOrDefault(neighbor, 0L) <= searchDepth) {
                    long f = computeBcDfs(path, neighbor, hopCount + 1, searchDepth);
                    if (f != searchDepth + 1) {
                        F = Long.min(F, f + 1);
                    }
                }
            }
        }

        if (F == searchDepth + 1) {
            bar.put(current, searchDepth - (hopCount + 1) + 1);
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

    private void joinPartialResults() {
        HashMap<Long, ArrayList<HugeLongArray>> leftResultHash = new HashMap<Long, ArrayList<HugeLongArray>>();

        for (HugeLongArray leftResult : leftResults) {
            // leftResult last index are virtual targets
            Long leftSize = leftResult.size() - 1;

            Long joinNode = leftResult.get(leftSize - 1);
            if (!leftResultHash.containsKey(joinNode)) {
                leftResultHash.put(joinNode, new ArrayList<HugeLongArray>());
            }

            ArrayList<HugeLongArray> arrayList = leftResultHash.get(joinNode);
            arrayList.add(leftResult);
        }

        for (HugeLongArray rightResult : rightResults) {
            long rightSize = rightResult.size();

            Long joinNode = rightResult.get(0);

            ArrayList<HugeLongArray> joinableLeftResults = leftResultHash.getOrDefault(joinNode,
                    new ArrayList<HugeLongArray>());

            for (HugeLongArray leftResult : joinableLeftResults) {
                Long leftSize = leftResult.size() - 1;

                if (leftResult.get(leftSize - 1) == rightResult.get(0)) {
                    if (leftSize == rightSize || leftSize == rightSize + 1) {
                        // -2 size because of one common middlevertices and one vertual target
                        HugeLongArray path = HugeLongArray.newArray(leftSize + rightSize - 1);

                        Set<Long> leftNodes = new HashSet<>();
                        for (int i = 0; i < leftSize; i++) {
                            leftNodes.add(leftResult.get(i));
                            path.addTo(i, leftResult.get(i));
                        }

                        boolean hasCommonNode = false;
                        for (int i = 1; i < rightSize; i++) {
                            if (leftNodes.contains(rightResult.get(i))) {
                                hasCommonNode = true;
                                continue;
                            }

                            path.addTo(leftSize - 1 + i, rightResult.get(i));
                        }

                        if (hasCommonNode) {
                            continue;
                        }

                        results.put(path, System.nanoTime());
                    }
                }
            }
        }
    }
}
