package master;

import org.neo4j.gds.collections.ha.HugeLongArray;
import org.neo4j.gds.api.Graph;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PathEnumerationResult {
    public Long source;
    public int pathCount;
    public Map<String, Long> nodeTimestamps = new HashMap<>();
    public Long startTime;
    public Long endTime;
    public boolean timedOut;

    public PathEnumerationResult(Long source, Long target, List<HugeLongArray> pathList, List<Long> timestampList,
            Graph graph, Long startTime, Long endTime, boolean timedOut) {

        this.source = graph.toOriginalNodeId(source);

        this.startTime = startTime;
        this.endTime = endTime;
        this.timedOut = timedOut;

        if (pathList == null) {
            return;
        }

        this.pathCount = pathList.size();

        for (int i = 0; i < pathList.size(); i++) {
            HugeLongArray result = pathList.get(i);
            long timestamp = timestampList.get(i);

            for (int j = 0; j < result.size(); j++) {
                long originalId = graph.toOriginalNodeId(result.get(j));
                nodeTimestamps.putIfAbsent(String.valueOf(originalId), timestamp);
            }
        }
    }
}
