package master.bfs;

import static org.neo4j.gds.config.NodeIdParser.parseToSingleNodeId;
import static org.neo4j.procedure.Mode.READ;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import org.neo4j.gds.api.Graph;
import org.neo4j.gds.procedures.algorithms.pathfinding.PathFactoryFacade;
import org.neo4j.graphdb.Path;
import org.neo4j.graphdb.RelationshipType;
import org.neo4j.procedure.Name;
import org.neo4j.procedure.Procedure;

import master.ProcedureHelper;

public class BFSProc extends master.Procedure {

    @Procedure(name = "master.bfs", mode = READ)
    public Stream<BFSResult> stream(
            @Name(value = "graphName") String graphNameString,
            @Name(value = "configuration", defaultValue = "{}") Map<String, Object> configuration) {

        log.debug("Started bfs with params, graphname: " + graphNameString);

        ProcedureHelper procHelper = new ProcedureHelper(log, transaction, taskRegistryFactory, dbService,
                username);

        log.debug("Created procHelper");

        Long source = parseToSingleNodeId(configuration.get("sourceNode"), "sourceNode");
        Long target = parseToSingleNodeId(configuration.get("targetNode"), "targetNode");
        long k = (long) configuration.get("k");

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
        ArrayList<Long> result = BFS.computeBFS(source, target, graph, log, (int) k);
        Long endTime = System.nanoTime();

        log.debug("time used: " + (endTime - startTime));

        if (result.isEmpty()) {
            return Stream.empty();
        }

        PathFactoryFacade pathFactoryFacade = PathFactoryFacade.create(true, procHelper.nodeLookup, true);

        BFSResult BFSResult = new BFSResult(source, target, result, graph, pathFactoryFacade, startTime,
                endTime);

        return Stream.of(BFSResult);

    }

    public static class BFSResult {

        public Long source;
        public Long startTime;
        public Long endTime;

        public List<Long> result;
        public Path path;

        public BFSResult(Long source, Long target, ArrayList<Long> result, Graph graph,
                PathFactoryFacade pathFactoryFacade, Long startTime, Long endTime) {

            this.source = graph.toOriginalNodeId(source);

            this.startTime = startTime;
            this.endTime = endTime;

            this.result = new ArrayList<Long>();

            for (Long node : result) {
                this.result.add(graph.toOriginalNodeId(node));
            }

            this.path = pathFactoryFacade.createPath(
                    this.result,
                    RelationshipType.withName("NEXT"));

        }
    }
}
