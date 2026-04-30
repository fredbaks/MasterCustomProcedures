package master.bcdfs;

import static org.neo4j.gds.config.NodeIdParser.parseToSingleNodeId;
import static org.neo4j.procedure.Mode.READ;

import java.io.IOException;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import org.neo4j.gds.api.Graph;
import org.neo4j.gds.procedures.algorithms.pathfinding.PathFactoryFacade;
import org.neo4j.procedure.Name;
import org.neo4j.procedure.Procedure;

import master.PathEnumerationAlgorithmResult;
import master.PathEnumerationResult;
import master.ProcedureHelper;
import master.dataHandling.PathEnumerationResultWriter;

public class BCDfsProc extends master.Procedure {

        @Procedure(name = "master.bcdfs", mode = READ)
        public Stream<PathEnumerationResult> stream(
                        @Name(value = "graphName") String graphNameString,
                        @Name(value = "configuration", defaultValue = "{}") Map<String, Object> configuration) {

                log.debug("Started dfs with params, graphname: " + graphNameString);

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

                long timeoutDuration = configuration.containsKey("timeoutDuration")
                                ? ((Number) configuration.get("timeoutDuration")).longValue()
                                : 300000L;

                log.debug("Parsed timeoutDuration to: " + timeoutDuration);

                Long startTime = System.nanoTime();
                BCDfs dfsEnum = new BCDfs(graph, source, target, k, timeoutDuration, log);
                PathEnumerationAlgorithmResult results = dfsEnum.startBCDfs();
                Long endTime = System.nanoTime();

                PathFactoryFacade pathFactoryFacade = PathFactoryFacade.create(true, procHelper.nodeLookup, true);

                PathEnumerationResult bcdfsResult = new PathEnumerationResult(source, target, results.results, graph,
                                pathFactoryFacade, startTime,
                                endTime, results.timedOut);

                try {
                        new PathEnumerationResultWriter(bcdfsResult, "BCDFS", graphNameString, k, source,
                                        target);
                } catch (IOException e) {
                        e.printStackTrace();
                }

                return Stream.of(bcdfsResult);

        }
}
