package master;

import org.neo4j.gds.collections.ha.HugeLongArray;
import org.neo4j.gds.api.Graph;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.carrotsearch.hppc.LongLongHashMap;
import com.carrotsearch.hppc.cursors.LongCursor;
import com.carrotsearch.hppc.cursors.LongLongCursor;

public class PathEnumerationResult {
    public Long source;
    // public List<List<Long>> results = new ArrayList<>();
    public Map<String, Long> nodeTimestamps = new HashMap<>();
    public Long pathCount;
    public Long startTime;
    public Long endTime;
    public boolean timedOut;

    public PathEnumerationResult(Long source, Long target,
            LongLongHashMap projNodeTimestamps, Long pathCount,
            // List<HugeLongArray> pathList, List<Long> timestampList,
            Graph graph, Long startTime, Long endTime, boolean timedOut) {

        this.source = graph.toOriginalNodeId(source);

        this.startTime = startTime;
        this.endTime = endTime;
        this.timedOut = timedOut;
        this.pathCount = pathCount;

        for (LongLongCursor nodeItem : projNodeTimestamps) {
            Long originalNode = graph.toOriginalNodeId(nodeItem.key);
            this.nodeTimestamps.put(Long.toString(originalNode), nodeItem.value);
        }

        // if (pathList == null) {
        // return;
        // }

        // for (int i = 0; i < pathList.size(); i++) {
        // HugeLongArray result = pathList.get(i);
        // Long timestamp = timestampList.get(i);

        // results.add(Arrays.stream(result.toArray())
        // .boxed()
        // .map((node) -> {
        // return graph.toOriginalNodeId(node);
        // })
        // .collect(Collectors.toList()));

        // for (int j = 0; j < result.size(); j++) {
        // nodeTimestamps.putIfAbsent(String.valueOf(result.get(j)), timestamp);
        // }
        // }
    }
}
