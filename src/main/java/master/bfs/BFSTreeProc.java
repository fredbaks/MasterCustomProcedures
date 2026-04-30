package master.bfs;

import static org.neo4j.gds.config.NodeIdParser.parseToSingleNodeId;
import static org.neo4j.procedure.Mode.READ;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import org.neo4j.gds.api.Graph;
import org.neo4j.procedure.Name;
import org.neo4j.procedure.Procedure;

import master.ProcedureHelper;

public class BFSTreeProc extends master.Procedure {

    @Procedure(name = "master.bfstree", mode = READ)
    public Stream<BFSResult> stream(
            @Name(value = "graphName") String graphNameString,
            @Name(value = "configuration", defaultValue = "{}") Map<String, Object> configuration) {

        log.debug("Started bfs with params, graphname: " + graphNameString);

        ProcedureHelper procHelper = new ProcedureHelper(log, transaction, taskRegistryFactory, dbService,
                username);

        Long source = parseToSingleNodeId(configuration.get("sourceNode"), "sourceNode");
        long k = (long) configuration.get("k");

        log.debug("Parsed sourceNode to Long: " + source);
        log.debug("Parsed h to Long: " + k);

        Graph graph = procHelper.getGraph(graphNameString, Optional.empty());

        source = graph.toMappedNodeId(source);

        Long startTime = System.nanoTime();
        HashMap<Long, Integer> result = BFS.computeBFSTree(source, graph, log, Optional.empty(), (int) k);
        Long endTime = System.nanoTime();

        log.debug("time used: " + (endTime - startTime));

        if (result.isEmpty()) {
            return Stream.empty();
        }

        BFSResult BFSResult = new BFSResult(source, result, graph, startTime,
                endTime);

        return Stream.of(BFSResult);

    }

    public static class BFSResult {

        public Long source;
        public Long startTime;
        public Long endTime;

        public Map<String, Long> result;

        public BFSResult(Long source, HashMap<Long, Integer> result, Graph graph, Long startTime, Long endTime) {

            this.source = graph.toOriginalNodeId(source);

            this.startTime = startTime;
            this.endTime = endTime;

            this.result = new HashMap<String, Long>();

            for (Map.Entry<Long, Integer> node : result.entrySet()) {
                Long originalNode = graph.toOriginalNodeId(node.getKey());
                this.result.put(originalNode.toString(), Long.valueOf(node.getValue()));
            }
        }
    }
}
