package master.dfs;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import java.util.Arrays;

import master.AlgorithmTimeoutException;
import master.PathEnumerationAlgorithmResult;

import org.neo4j.gds.api.Graph;
import org.neo4j.gds.core.utils.paged.HugeLongArrayStack;
import org.neo4j.logging.Log;

import com.carrotsearch.hppc.BitSet;

import it.unimi.dsi.fastutil.longs.LongBigArrayBigList;

public class CDfs {

    private Graph graph;
    private long source;
    private long target;
    private long k;
    private long timeoutDuration;
    private Log log;

    private LongBigArrayBigList resultPaths;
    private int stride;
    private long[] padding;
    private com.carrotsearch.hppc.LongArrayList resultTimestamps;

    private HugeLongArrayStack stack;
    private BitSet visited;

    private boolean timedOut = false;

    public PathEnumerationAlgorithmResult startCDfs() {

        log.debug("Started Cdfs");

        if (k < 0)
            throw new IllegalArgumentException("CDfs requires non-negative k for flat array storage");
        resultPaths = new LongBigArrayBigList();
        resultTimestamps = new com.carrotsearch.hppc.LongArrayList();
        stride = (int) k + 1;
        padding = new long[stride];
        Arrays.fill(padding, -1L);

        stack = HugeLongArrayStack.newStack(graph.nodeCount());
        stack.push(source);

        visited = new BitSet();

        long[] path = new long[stride];

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

        return new PathEnumerationAlgorithmResult(resultPaths, stride, resultTimestamps.toArray(), timedOut);
    }

    public CDfs(Graph graph, long source, long target, long k, long timeoutDuration, Log log) {
        this.graph = graph;
        this.source = source;
        this.target = target;
        this.k = k;
        this.timeoutDuration = timeoutDuration;
        this.log = log;
    }

    private void computeCDfs(long[] path, long current, int hopCount) {

        if (Thread.currentThread().isInterrupted())
            throw new AlgorithmTimeoutException();

        path[hopCount] = current;

        if (current == target) {
            Arrays.fill(path, hopCount + 1, stride, -1L);
            resultPaths.addElements(resultPaths.size64(), new long[][] { path }, 0L, stride);
            resultTimestamps.add(System.nanoTime());
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
        path[hopCount] = 0;
        return;
    }
}
