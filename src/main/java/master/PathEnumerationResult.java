package master;

import org.neo4j.gds.collections.ha.HugeLongArray;
import org.neo4j.gds.procedures.algorithms.pathfinding.PathFactoryFacade;
import org.neo4j.graphdb.Path;
import org.neo4j.graphdb.RelationshipType;
import org.neo4j.gds.api.Graph;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class PathEnumerationResult {
    public Long source;
    public List<List<Long>> results = new ArrayList<>();
    public Map<String, Long> nodeTimestamps = new HashMap<>();
    public List<Path> paths = new ArrayList<>();
    public Long startTime;
    public Long endTime;
    public boolean timedOut;

    public PathEnumerationResult(Long source, Long target, HashMap<HugeLongArray, Long> timestamps, Graph graph,
            PathFactoryFacade pathFactoryFacade, Long startTime, Long endTime, boolean timedOut) {

        this.source = graph.toOriginalNodeId(source);

        this.startTime = startTime;
        this.endTime = endTime;
        this.timedOut = timedOut;

        if (timestamps == null) {
            return;
        }

        ArrayList<HugeLongArray> arrayPaths = new ArrayList<HugeLongArray>(timestamps.keySet());

        arrayPaths.sort(Comparator.comparing(timestamps::get));

        for (HugeLongArray result : arrayPaths) {
            results.add(Arrays.stream(result.toArray())
                    .boxed()
                    .map((node) -> {
                        return graph.toOriginalNodeId(node);
                    })
                    .collect(Collectors.toList()));

            paths.add(pathFactoryFacade.createPath(
                    this.results.getLast(),
                    RelationshipType.withName("NEXT")));

            for (int i = 0; i < result.size(); i++) {
                nodeTimestamps.putIfAbsent(String.valueOf(result.get(i)), timestamps.get(result));
            }
        }
    }
}
