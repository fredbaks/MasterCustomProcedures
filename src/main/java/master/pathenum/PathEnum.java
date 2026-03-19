package master.pathenum;

import org.neo4j.logging.Log;

import master.bfs.BFS;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.neo4j.gds.api.Graph;
import org.neo4j.gds.collections.ha.HugeLongArray;

public class PathEnum {

    private Graph graph;
    private long source;
    private long target;
    private int k;
    private Log log;

    private HashMap<HugeLongArray, Long> results;

    private Set<Long> sourceSet;
    private Set<Long> targetSet;

    private ArrayList<ArrayList<ArrayList<Long>>> distanceMatrix;
    private HashMap<Long, List<Long>> hashMap;

    private int[][][] bucketDegreeSum;

    private HashMap<Long, Integer> sourceDistance;
    private HashMap<Long, Integer> targetDistance;

    private HashSet<Long> allNodes;

    public PathEnum(Graph graph, Long source, Long target, int k, Log log) {
        this.source = source;
        this.target = target;
        this.k = k;
        this.graph = graph;
        this.log = log;

        sourceSet = new HashSet<Long>();
        sourceSet.add(source);

        targetSet = new HashSet<Long>();
        targetSet.add(target);
    }

    public HashMap<HugeLongArray, Long> computePathEnum(boolean runJoin) {

        log.debug("Started PathEnum");

        results = new HashMap<HugeLongArray, Long>();

        BuildIndex();

        log.debug("Index, distanceMatrix: " + distanceMatrix + ", neighborMap: " + hashMap);

        boolean doJoin = CardinalityEstimator();

        if (doJoin || runJoin) {
            int cutIndex = JoinOrderOptimization();

            JoinOnIndex(cutIndex);
        } else {
            HugeLongArray path = HugeLongArray.newArray(1);
            path.addTo(0, source);

            DfsSearch(path);
        }

        return results;
    }

    private void BuildIndex() {
        sourceDistance = new HashMap<Long, Integer>();
        targetDistance = new HashMap<Long, Integer>();

        allNodes = new HashSet<Long>((int) graph.nodeCount());

        // The nodes are added to a list first as method calls inside graph.forEach has
        // not run as excepted in the past
        graph.forEachNode((node) -> {
            allNodes.add(node);
            return true;
        });

        for (Long node : allNodes) {
            sourceDistance.put(node, BFS.computeBFS(source, node, graph, log, Optional.of(targetSet)).size() - 1);
            targetDistance.put(node, BFS.computeBFS(node, target, graph, log, Optional.of(sourceSet)).size() - 1);
        }

        distanceMatrix = new ArrayList<ArrayList<ArrayList<Long>>>();

        for (int i = 0; i < k + 2; i++) {
            distanceMatrix.add(i, new ArrayList<ArrayList<Long>>());
            for (int j = 0; j < k + 2; j++) {
                distanceMatrix.get(i).add(new ArrayList<Long>());
            }
        }

        for (Long node : allNodes) {
            Integer sDistance = sourceDistance.get(node);
            Integer tDistance = targetDistance.get(node);

            if (sDistance + tDistance <= k) {
                distanceMatrix.get(sDistance).get(tDistance).add(node);
            }
        }

        hashMap = new HashMap<Long, List<Long>>();

        HashSet<Long> allNodesMinusT = new HashSet<>(allNodes);
        allNodesMinusT.remove(target);

        for (Long node : allNodesMinusT) {

            ArrayList<Long> offSet = new ArrayList<Long>();

            hashMap.put(node, offSet);

            List<Long> neighbors = new ArrayList<Long>();

            graph.forEachRelationship(node, (current, neighbor) -> {
                neighbors.add(neighbor);
                return true;
            });

            for (Long neighbor : neighbors) {
                if (sourceDistance.get(node) + targetDistance.get(neighbor) + 1 <= k) {
                    offSet.add(neighbor);
                }
            }

            offSet.sort(Comparator.comparing(targetDistance::get));
        }

        ArrayList<Long> targetList = new ArrayList<Long>();
        targetList.add(target);
        hashMap.put(target, targetList);

        // AI written - OBS OBS
        bucketDegreeSum = new int[k + 2][k + 2][k + 1];

        // Special case: src is bucket (0,*)
        // bucket_degree_sum_[budget] in C++ = src's cumulative degree at budget
        List<Long> srcNeighbors = hashMap.get(source);
        if (srcNeighbors != null) {
            for (int budget = 0; budget < k; budget++) {
                int count = 0;
                for (Long neighbor : srcNeighbors) {
                    Integer ntd = targetDistance.get(neighbor);
                    if (ntd != null && ntd <= budget) {
                        count++;
                    } else {
                        break; // sorted
                    }
                }
                bucketDegreeSum[0][sourceDistance.getOrDefault(source, 0)][budget] = count;
            }
        }

        // For all other active vertices in bucket (i, j), i >= 1
        for (int i = 1; i < k + 2; i++) {
            for (int j = 1; j < k + 2; j++) {
                List<Long> bucketNodes = distanceMatrix.get(i).get(j);
                if (bucketNodes.isEmpty())
                    continue;

                for (int budget = 0; budget < k; budget++) {
                    int degreeSum = 0;
                    for (Long node : bucketNodes) {
                        List<Long> neighbors = hashMap.get(node);
                        if (neighbors == null)
                            continue;
                        for (Long neighbor : neighbors) {
                            Integer ntd = targetDistance.get(neighbor);
                            if (ntd != null && ntd <= budget) {
                                degreeSum++;
                            } else {
                                break;
                            }
                        }
                    }
                    bucketDegreeSum[i][j][budget] = degreeSum;
                }
            }
        }
    }

    // AI written OBS OBS
    private boolean CardinalityEstimator() {

        // ── Step 1: seed from src (bucket (0,*)) ──────────────────────────────
        // Mirrors: bucket_degree_sum_[length_constraint_ - 1]
        long estimatedCount = bucketDegreeSum[0][targetDistance.getOrDefault(source, 0)][k - 1];

        // ── Step 2: level-by-level multiplication ─────────────────────────────
        for (int i = 1; i < k; i++) {
            int budget = k - i - 1;

            long degreeSum = 1; // mirrors C++ initialisation
            long vertexSum = 1;

            for (int j = 1; j <= i; j++) {
                for (int kk = 1; kk <= k - i; kk++) {
                    List<Long> bucketNodes = distanceMatrix.get(j).get(kk);
                    if (bucketNodes.isEmpty())
                        continue;

                    // vertex count for this bucket
                    vertexSum += bucketNodes.size();

                    // cumulative degree at this budget, pre-computed
                    if (budget >= 0) {
                        degreeSum += bucketDegreeSum[j][kk][budget];
                    }
                }
            }

            long avgDegree = degreeSum / vertexSum;
            estimatedCount *= (avgDegree > 1) ? avgDegree : 1;
        }

        return estimatedCount > 100_000;
    }

    private void DfsSearch(HugeLongArray M) {
        int MSize = (int) M.size();

        Long node = M.get(MSize - 1);

        if (node == target) {
            results.put(M, System.nanoTime());
            return;
        }

        Set<Long> neighbors = NeighborIndex(node, k - MSize, true);

        for (Long neighbor : neighbors) {
            boolean contains = false;
            for (int i = 0; i < MSize; i++) {
                if (M.get(i) == neighbor) {
                    contains = true;
                    break;
                }
            }
            if (contains) {
                continue;
            }

            HugeLongArray newM = M.copyOf(MSize + 1);
            newM.addTo(MSize, neighbor);

            DfsSearch(newM);
        }
    }

    private int JoinOrderOptimization() {

        // c_i^j given by cardinalityEstimation.get(i).get(j)
        HashMap<Integer, HashMap<Integer, HashMap<Long, Integer>>> cardinalityEstimation = new HashMap<Integer, HashMap<Integer, HashMap<Long, Integer>>>();

        cardinalityEstimation.put(k, new HashMap<Integer, HashMap<Long, Integer>>());
        cardinalityEstimation.get(k).put(k, new HashMap<Long, Integer>());

        Set<Long> kSet = IndexLookup(k);

        for (Long node : kSet) {
            cardinalityEstimation.get(k).get(k).put(node, 1);
        }

        for (int i = k - 1; i >= 0; i--) {
            cardinalityEstimation.get(k).put(i, new HashMap<Long, Integer>());
            Set<Long> set = IndexLookup(i);

            for (Long node : set) {
                Set<Long> neighbors = NeighborIndex(node, k - i, true);

                for (Long neighbor : neighbors) {
                    Integer nodeCard = cardinalityEstimation.get(k).get(i).getOrDefault(node, 0);
                    Integer neighborCard = cardinalityEstimation.get(k).get(i + 1).getOrDefault(neighbor, 0);

                    cardinalityEstimation.get(k).get(i).put(node, nodeCard + neighborCard);
                }
            }
        }

        cardinalityEstimation.put(0, new HashMap<Integer, HashMap<Long, Integer>>());
        cardinalityEstimation.get(0).put(0, new HashMap<Long, Integer>());

        Set<Long> zeroSet = IndexLookup(0);

        for (Long node : zeroSet) {
            cardinalityEstimation.get(0).get(0).put(node, 1);
        }

        for (int i = 1; i <= k; i++) {
            if (!cardinalityEstimation.containsKey(i)) {
                cardinalityEstimation.put(i, new HashMap<Integer, HashMap<Long, Integer>>());
            }
            cardinalityEstimation.get(i).put(0, new HashMap<Long, Integer>());
            Set<Long> set = IndexLookup(i);

            for (Long node : set) {
                Set<Long> neighbors = NeighborIndex(node, k - i, false);

                for (Long neighbor : neighbors) {
                    Integer nodeCard = cardinalityEstimation.get(i).get(0).getOrDefault(node, 0);
                    Integer neighborCard = cardinalityEstimation.get(i - 1).get(0).getOrDefault(neighbor, 0);

                    cardinalityEstimation.get(i).get(0).put(node, nodeCard + neighborCard);
                }
            }
        }

        int minValue = Integer.MAX_VALUE;
        int cutIndex = -1;

        for (int i = 0; i <= k; i++) {
            Set<Long> set = IndexLookup(i);

            int leftSum = 0;
            int rightSum = 0;

            for (Long node : set) {
                leftSum += cardinalityEstimation.get(i).get(0).getOrDefault(node, 0);
                rightSum += cardinalityEstimation.get(k).get(i).getOrDefault(node, 0);
            }

            log.debug("for i: " + i + " with set: " + set + " Sum: " + (leftSum + rightSum));

            if (leftSum + rightSum < minValue) {
                minValue = leftSum + rightSum;
                cutIndex = i;
            }
        }

        log.debug("Found cutIndex: " + cutIndex);

        return cutIndex;
    }

    private void JoinOnIndex(int cutIndex) {
        ArrayList<HugeLongArray> R_a = new ArrayList<HugeLongArray>();

        HugeLongArray M = HugeLongArray.newArray(1);
        M.set(0L, source);

        Search(M, 0, cutIndex + 1, R_a);

        Set<Long> cutNodes = new HashSet<>();
        for (HugeLongArray path : R_a) {
            cutNodes.add(path.get(path.size() - 1));
        }

        HashMap<Long, ArrayList<HugeLongArray>> R_b = new HashMap<>();
        for (Long node : cutNodes) {
            ArrayList<HugeLongArray> R_b_v = new ArrayList<>();
            HugeLongArray Mv = HugeLongArray.newArray(1);
            Mv.set(0, node);
            Search(Mv, cutIndex, k - cutIndex + 1, R_b_v);
            R_b.put(node, R_b_v);
        }

        for (HugeLongArray leftPath : R_a) {
            Long cutNode = leftPath.get(leftPath.size() - 1);
            ArrayList<HugeLongArray> rightPaths = R_b.get(cutNode);
            if (rightPaths == null)
                continue;

            for (HugeLongArray rightPath : rightPaths) {
                HugeLongArray full = validateAndMerge(leftPath, rightPath);

                if (full == null) {
                    continue;
                }

                results.put(full, System.nanoTime());
            }
        }
    }

    private void Search(HugeLongArray M, int i, int l, ArrayList<HugeLongArray> R) {
        int MSize = (int) M.size();

        if (MSize == l) {
            R.add(M);
            return;
        }

        Long node = M.get(MSize - 1);

        Set<Long> set = NeighborIndex(node, k - i - MSize, true);

        for (Long neighbor : set) {
            HugeLongArray newM = M.copyOf(MSize + 1);
            newM.set(MSize, neighbor);

            Search(newM, i, l, R);
        }
    }

    private HugeLongArray validateAndMerge(HugeLongArray left, HugeLongArray right) {
        Set<Long> seen = new HashSet<>();
        for (int i = 0; i < left.size(); i++)
            seen.add(left.get(i));

        int lastValidIndex = 0;
        for (int i = 1; i < right.size(); i++) {
            long node = right.get(i);
            if (node == target) {
                lastValidIndex = i;
                break;
            }
            if (!seen.add(node))
                return null;
            lastValidIndex = i;
        }

        int leftSize = (int) left.size();
        int totalSize = leftSize + lastValidIndex;
        HugeLongArray merged = HugeLongArray.newArray(totalSize);
        for (int i = 0; i < leftSize; i++)
            merged.set(i, left.get(i));
        for (int i = 1; i <= lastValidIndex; i++)
            merged.set(leftSize + i - 1, right.get(i));

        return merged;
    }

    private Set<Long> IndexLookup(int length) {
        Set<Long> output = new HashSet<Long>();

        for (int i = 0; i <= length; i++) {
            for (int j = 0; j <= k - length; j++) {
                output.addAll(distanceMatrix.get(i).get(j));
            }
        }

        return output;
    }

    private Set<Long> NeighborIndex(Long node, int length, boolean fromTarget) {
        Set<Long> output = new HashSet<Long>();
        List<Long> neighbors = hashMap.get(node);
        if (neighbors == null)
            return output;

        if (fromTarget) {
            for (Long neighbor : neighbors) {
                if (targetDistance.get(neighbor) <= length) {
                    output.add(neighbor);
                } else {
                    break;
                }
            }
        } else {
            for (Long neighbor : neighbors) {
                if (sourceDistance.get(neighbor) <= length) {
                    output.add(neighbor);
                }
            }
        }

        return output;
    }

}
