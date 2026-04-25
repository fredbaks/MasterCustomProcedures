package master.dfs;

import java.io.PrintWriter;
import java.io.StringWriter;
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
import org.neo4j.gds.core.utils.paged.HugeLongArrayStack;
import org.neo4j.logging.Log;

import com.carrotsearch.hppc.BitSet;

public class CDfs {

    private Graph graph;
    private long source;
    private long target;
    private long k;
    private long timeoutDuration;
    private Log log;

    private ConcurrentHashMap<HugeLongArray, Long> results;

    private HugeLongArrayStack stack;
    private BitSet visited;

    private boolean timedOut = false;

    public PathEnumerationAlgorithmResult startCDfs() {

        log.debug("Started Cdfs");

        results = new ConcurrentHashMap<>();

        stack = HugeLongArrayStack.newStack(graph.nodeCount());
        stack.push(source);

        visited = new BitSet();

        HugeLongArray path = HugeLongArray.newArray(k + 1);

        ExecutorService executor = Executors.newSingleThreadExecutor();
        Future<?> future = executor.submit(() -> computeCDfs(path, source, 0));
        try {
            future.get(timeoutDuration, TimeUnit.MILLISECONDS);
        } catch (TimeoutException e) {
            future.cancel(true);
            timedOut = true;
        } catch (java.util.concurrent.ExecutionException e) {
            if (e.getCause() instanceof AlgorithmTimeoutException) {
                timedOut = true;
            } else {
                log.warn("CDfs encountered an unexpected exception: " + e.getCause().getMessage());
                StringWriter sw = new StringWriter();
                PrintWriter pw = new PrintWriter(sw);
                e.getCause().printStackTrace(pw);
                String sStackTrace = sw.toString();
                log.warn("Stacktrace: " + sStackTrace);
            }
        } catch (Exception e) {
            log.warn("CDfs encountered an unexpected exception: " + e.getMessage());
        } finally {
            executor.shutdownNow();
        }

        return new PathEnumerationAlgorithmResult(new HashMap<HugeLongArray, Long>(results), timedOut);
    }

    public CDfs(Graph graph, long source, long target, long k, long timeoutDuration, Log log) {
        this.graph = graph;
        this.source = source;
        this.target = target;
        this.k = k;
        this.timeoutDuration = timeoutDuration;
        this.log = log;
    }

    private void computeCDfs(HugeLongArray path, long current, int hopCount) {

        if (Thread.currentThread().isInterrupted())
            throw new AlgorithmTimeoutException();

        path.set(hopCount, current);

        if (current == target) {
            results.put(path.copyOf(hopCount + 1), System.nanoTime());
            return;
        }

        visited.set(current);

        if (hopCount < k || k == -1) {

            List<Long> neighbors = new ArrayList<Long>();

            graph.forEachRelationship(current, (long source, long neighbor) -> {

                if (!visited.get(neighbor)) {
                    neighbors.add(neighbor);
                }

                return true;
            });

            for (long neighbor : neighbors) {
                computeCDfs(path, neighbor, hopCount + 1);
            }
        }

        visited.clear(current);
        path.set(hopCount, 0);
        return;
    }
}
