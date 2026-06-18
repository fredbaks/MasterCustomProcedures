package master;

import org.neo4j.gds.collections.ha.HugeLongArray;
import org.neo4j.gds.api.Graph;

import com.carrotsearch.hppc.LongLongHashMap;
import com.carrotsearch.hppc.cursors.LongLongCursor;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PathEnumerationResult {
    public Long source;
    public Long pathCount;
    public Map<String, Long> nodeTimestamps = new HashMap<>();
    public Long startTime;
    public Long endTime;
    public boolean timedOut;

    public PathEnumerationResult(Long source, Long target, List<HugeLongArray> pathList, long[] timestampList,
            Graph graph, Long startTime, Long endTime, boolean timedOut) {

        this.source = graph.toOriginalNodeId(source);

        this.startTime = startTime;
        this.endTime = endTime;
        this.timedOut = timedOut;

        if (pathList == null) {
            return;
        }

        this.pathCount = (long) pathList.size();

        LongLongHashMap nodeFirstSeen = new LongLongHashMap();

        for (int i = 0; i < pathList.size(); i++) {
            HugeLongArray result = pathList.get(i);
            long timestamp = timestampList[i];

            for (int j = 0; j < result.size(); j++) {
                long internalId = result.get(j);
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
