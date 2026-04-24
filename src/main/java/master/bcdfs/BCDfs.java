package master.bcdfs;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import master.AlgorithmTimeoutException;
import master.PathEnumerationAlgorithmResult;

import org.neo4j.gds.api.Graph;
import org.neo4j.gds.collections.ha.HugeLongArray;
import org.neo4j.logging.Log;

import com.carrotsearch.hppc.BitSet;

public class BCDfs {

    private Graph graph;
    private long source;
    private long target;
    private long k;
    private long timeoutDuration;
    private Log log;

    private ConcurrentHashMap<HugeLongArray, Long> results;

    private BitSet visited;
    private HashMap<Long, Long> bar;

    private boolean timedOut = false;

    public PathEnumerationAlgorithmResult startBCDfs() {

        log.debug("Started BC-Dfs");
        results = new ConcurrentHashMap<>();

        visited = new BitSet(graph.nodeCount());

        bar = new HashMap<Long, Long>();

        HugeLongArray path = HugeLongArray.newArray(k + 1);

        ExecutorService executor = Executors.newSingleThreadExecutor();
        Future<?> future = executor.submit(() -> computeBcDfs(path, source, 0));
        try {
            future.get(timeoutDuration, TimeUnit.MILLISECONDS);
        } catch (TimeoutException e) {
            future.cancel(true);
            log.debug("TimedoutException sent");
            timedOut = true;
        } catch (java.util.concurrent.ExecutionException e) {
            if (e.getCause() instanceof AlgorithmTimeoutException) {
                log.debug("AlgorithmTimeOutExcpetion caught");
                timedOut = true;
            } else {
                log.warn("BCDfs encountered an unexpected exception: " + e.getCause().getMessage());
            }
        } catch (Exception e) {
            log.warn("BCDfs encountered an unexpected exception: " + e.getMessage());
        } finally {
            executor.shutdownNow();
        }

        return new PathEnumerationAlgorithmResult(new HashMap<HugeLongArray, Long>(results), timedOut);
    }

    public BCDfs(Graph graph, long source, long target, long k, long timeoutDuration, Log log) {
        this.graph = graph;
        this.source = source;
        this.target = target;
        this.k = k;
        this.timeoutDuration = timeoutDuration;
        this.log = log;
    }

    private long computeBcDfs(HugeLongArray path, long current, int hopCount) {

        if (Thread.currentThread().isInterrupted())
            throw new AlgorithmTimeoutException();

        long F = k + 1;

        path.set(hopCount, current);

        if (current == target) {
            results.put(path.copyOf(hopCount + 1), System.nanoTime());
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
