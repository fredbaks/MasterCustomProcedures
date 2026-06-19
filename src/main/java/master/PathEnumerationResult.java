package master;

import org.neo4j.gds.api.Graph;

import com.carrotsearch.hppc.LongLongHashMap;
import com.carrotsearch.hppc.cursors.LongLongCursor;

import java.util.HashMap;
import java.util.Map;

public class PathEnumerationResult {
    public Long source;
    public Long pathCount;
    public Map<String, Long> nodeTimestamps = new HashMap<>();
    public Long startTime;
    public Long endTime;
    public boolean timedOut;

    public PathEnumerationResult(Long source, Long target, long[] pathData, int stride,
            long[] timestampList, Graph graph, Long startTime, Long endTime, boolean timedOut) {

        this.source = graph.toOriginalNodeId(source);
        this.startTime = startTime;
        this.endTime = endTime;
        this.timedOut = timedOut;

        if (pathData == null || pathData.length == 0) {
            this.pathCount = 0L;
            return;
        }

        this.pathCount = (long) (pathData.length / stride);

        LongLongHashMap nodeFirstSeen = new LongLongHashMap();

        for (int i = 0; i < pathCount; i++) {
            long timestamp = timestampList[i];
            for (int j = 0; j < stride; j++) {
                long internalId = pathData[i * stride + j];
                if (internalId == -1L)
                    break;
                if (!nodeFirstSeen.containsKey(internalId)) {
                    nodeFirstSeen.put(internalId, timestamp);
                }
            }
        }

        for (LongLongCursor cursor : nodeFirstSeen) {
            nodeTimestamps.put(String.valueOf(graph.toOriginalNodeId(cursor.key)), cursor.value);
        }
    }
}
