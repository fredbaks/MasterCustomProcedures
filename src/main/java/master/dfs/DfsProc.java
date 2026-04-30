package master.dfs;

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

public class DfsProc extends master.Procedure {

    @Procedure(name = "master.dfs", mode = READ)
    public Stream<DfsResult> stream(
            @Name(value = "graphName") String graphNameString,
            @Name(value = "configuration", defaultValue = "{}") Map<String, Object> configuration) {

        log.debug("Started dfs with params, graphname: " + graphNameString + " and sourceNode: "
                + configuration.getClass().toString() + ", sourceNode: " + configuration.get("sourceNode"));

        ProcedureHelper procHelper = new ProcedureHelper(log, transaction, taskRegistryFactory, dbService,
                username);

        log.debug("Created procHelper");

        Long source = parseToSingleNodeId(configuration.get("sourceNode"), "sourceNode");

        boolean hasTarget = configuration.get("targetNode") != null;

        Optional<Long> target = hasTarget
                ? Optional.of(parseToSingleNodeId(configuration.get("targetNode"), "targetNode"))
                : Optional.empty();

        boolean hasHoplimit = configuration.get("k") != null;

        Long k = hasHoplimit ? (Long) configuration.get("k") : -1;

        log.debug("Parsed sourceNode to Long: " + source);

        Graph graph = procHelper.getGraph(graphNameString, Optional.empty());

        source = graph.toMappedNodeId(source);
        target = target.isPresent() ? Optional.of(graph.toMappedNodeId(target.get())) : Optional.empty();

        log.debug(graph.nodeCount() + ", " + source);

        ArrayList<Long> result = Dfs.computeDfs(source, target, graph, log, k.intValue());

        if (result.isEmpty()) {
            return Stream.empty();
        }

        PathFactoryFacade pathFactoryFacade = PathFactoryFacade.create(true, procHelper.nodeLookup, true);

        return Stream.of(new DfsResult(source, result, graph, pathFactoryFacade));

    }

    public static class DfsResult {
        public Long source;
        public List<Long> result;
        public Path path;

        public DfsResult(Long source, ArrayList<Long> result, Graph graph, PathFactoryFacade pathFactoryFacade) {

            this.source = graph.toOriginalNodeId(source);

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
