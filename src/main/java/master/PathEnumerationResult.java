package master;

import org.neo4j.gds.api.Graph;

import com.carrotsearch.hppc.LongLongHashMap;
import com.carrotsearch.hppc.cursors.LongLongCursor;

import it.unimi.dsi.fastutil.longs.LongBigArrayBigList;
import it.unimi.dsi.fastutil.longs.LongIterator;

import java.util.HashMap;
import java.util.Map;

public class PathEnumerationResult {
    public Long source;
    public Long pathCount;
    public Map<String, Long> nodeTimestamps = new HashMap<>();
    public Long startTime;
    public Long endTime;
    public boolean timedOut;

    public PathEnumerationResult(Long source, Long target, LongBigArrayBigList pathData, int stride,
            long[] timestampList, Graph graph, Long startTime, Long endTime, boolean timedOut) {

        this.source = graph.toOriginalNodeId(source);
        this.startTime = startTime;
        this.endTime = endTime;
        this.timedOut = timedOut;

        if (pathData == null || pathData.size64() == 0) {
            this.pathCount = 0L;
            return;
        }

        long totalPaths = pathData.size64() / stride;
        this.pathCount = totalPaths;

        LongLongHashMap nodeFirstSeen = new LongLongHashMap();

        LongIterator it = pathData.iterator();
        int pos = 0;
        int pathIndex = -1;
        long timestamp = 0L;
        while (it.hasNext()) {
            if (pos == 0) {
                timestamp = timestampList[++pathIndex];
            }
            long internalId = it.nextLong();
            if (internalId != -1L && !nodeFirstSeen.containsKey(internalId)) {
                nodeFirstSeen.put(internalId, timestamp);
            }
            if (++pos == stride) {
                pos = 0;
            }
        }

        for (LongLongCursor cursor : nodeFirstSeen) {
            nodeTimestamps.put(String.valueOf(graph.toOriginalNodeId(cursor.key)), cursor.value);
        }
    }
}
