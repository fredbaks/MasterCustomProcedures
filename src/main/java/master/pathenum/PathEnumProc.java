package master.pathenum;

import static org.neo4j.gds.config.NodeIdParser.parseToSingleNodeId;
import static org.neo4j.procedure.Mode.READ;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.neo4j.gds.api.Graph;
import org.neo4j.gds.collections.ha.HugeLongArray;
import org.neo4j.gds.procedures.algorithms.pathfinding.PathFactoryFacade;
import org.neo4j.graphdb.Path;
import org.neo4j.graphdb.RelationshipType;
import org.neo4j.procedure.Name;
import org.neo4j.procedure.Procedure;

import master.ProcedureHelper;

public class PathEnumProc extends master.Procedure {

    @Procedure(name = "master.pathenum", mode = READ)
    public Stream<PathEnumResult> stream(
            @Name(value = "graphName") String graphNameString,
            @Name(value = "configuration", defaultValue = "{}") Map<String, Object> configuration) {

        log.debug("Started pathenum with params, graphname: " + graphNameString);

        ProcedureHelper procHelper = new ProcedureHelper(log, transaction, taskRegistryFactory, dbService,
                username);

        log.debug("Created procHelper");

        Long source = parseToSingleNodeId(configuration.get("sourceNode"), "sourceNode");
        Long target = parseToSingleNodeId(configuration.get("targetNode"), "targetNode");
        long k = (long) configuration.get("k");
        boolean runJoin = (boolean) configuration.getOrDefault("runJoin", false);

        log.debug("Parsed sourceNode to Long: " + source);
        log.debug("Parsed targetNode to Long: " + target);
        log.debug("Parsed h to Long: " + k);

        Graph graph = procHelper.getGraph(graphNameString, Optional.empty());

        source = graph.toMappedNodeId(source);
        target = graph.toMappedNodeId(target);

        log.debug("Parsed sourceNode to mapped Long: " + source);
        log.debug("Parsed targetNode to mapped Long: " + target);

        log.debug(graph.nodeCount() + ", " + source + ", " + target);

        Long startTime = System.nanoTime();
        PathEnum dfsEnum = new PathEnum(graph, source, target, (int) k, log);
        HashMap<HugeLongArray, Long> results = dfsEnum.computePathEnum(runJoin);
        Long endTime = System.nanoTime();

        log.debug("time used: " + (endTime - startTime));

        PathFactoryFacade pathFactoryFacade = PathFactoryFacade.create(true, procHelper.nodeLookup, true);

        log.debug("Results: " + results);

        return Stream.of(new PathEnumResult(source, target, results, graph, pathFactoryFacade, startTime, endTime));

    }

    public static class PathEnumResult {
        public Long source;
        public List<List<Long>> results = new ArrayList<List<Long>>();
        public Map<String, Long> nodeTimestamps = new HashMap<String, Long>();
        public List<Path> paths = new ArrayList<Path>();
        public Long startTime;
        public Long endTime;

        public PathEnumResult(Long source, Long target, HashMap<HugeLongArray, Long> timestamps, Graph graph,
                PathFactoryFacade pathFactoryFacade, Long startTime, Long endTime) {

            this.source = graph.toOriginalNodeId(source);

            this.startTime = startTime;
            this.endTime = endTime;

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
}
