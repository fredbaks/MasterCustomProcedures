package master.dfs;

import static org.neo4j.gds.config.NodeIdParser.parseToSingleNodeId;
import static org.neo4j.procedure.Mode.READ;

import java.util.ArrayList;
import java.util.Arrays;
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

public class BCDfsProc extends master.Procedure {

    @Procedure(name = "master.bcdfs", mode = READ)
    public Stream<BCDfsResult> stream(
            @Name(value = "graphName") String graphNameString,
            @Name(value = "configuration", defaultValue = "{}") Map<String, Object> configuration) {

        log.debug("Started dfs with params, graphname: " + graphNameString);

        ProcedureHelper procHelper = new ProcedureHelper(log, transaction, taskRegistryFactory, dbService,
                username);

        log.debug("Created procHelper");

        Long source = parseToSingleNodeId(configuration.get("sourceNode"), "sourceNode");
        Long target = parseToSingleNodeId(configuration.get("targetNode"), "targetNode");
        long h = (long) configuration.get("h");

        log.debug("Parsed sourceNode to Long: " + source);
        log.debug("Parsed targetNode to Long: " + target);
        log.debug("Parsed h to Long: " + h);

        Graph graph = procHelper.getGraph(graphNameString, Optional.empty());

        source = graph.toMappedNodeId(source);
        target = graph.toMappedNodeId(target);

        log.debug("Parsed sourceNode to mapped Long: " + source);
        log.debug("Parsed targetNode to mapped Long: " + target);

        log.debug(graph.nodeCount() + ", " + source + ", " + target);

        CDfs dfsEnum = new CDfs(graph, source, target, h, log);
        ArrayList<HugeLongArray> results = dfsEnum.startDfsEnum();

        PathFactoryFacade pathFactoryFacade = PathFactoryFacade.create(true, procHelper.nodeLookup, true);

        return Stream.of(new BCDfsResult(source, target, results, graph, pathFactoryFacade));

    }

    public static class BCDfsResult {
        public Long source;
        public List<List<Long>> results;
        public List<Path> paths;

        public BCDfsResult(Long source, Long target, ArrayList<HugeLongArray> arrayListResults, Graph graph,
                PathFactoryFacade pathFactoryFacade) {

            this.source = graph.toOriginalNodeId(source);

            results = new ArrayList<List<Long>>();
            paths = new ArrayList<Path>();

            if (arrayListResults == null) {
                return;
            }

            for (HugeLongArray result : arrayListResults) {

                results.add(Arrays.stream(result.toArray())
                        .boxed()
                        .map((node) -> {
                            return graph.toOriginalNodeId(node);
                        })
                        .collect(Collectors.toList()));

                paths.add(pathFactoryFacade.createPath(
                        this.results.getLast(),
                        RelationshipType.withName("NEXT")));
            }
        }
    }
}
