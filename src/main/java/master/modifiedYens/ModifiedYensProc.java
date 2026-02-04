/*
 * Copyright (c) "Neo4j"
 * Neo4j Sweden AB [http://neo4j.com]
 *
 * This file is part of Neo4j.
 *
 * Neo4j is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package master.modifiedYens;

import org.neo4j.gds.api.Graph;
import org.neo4j.gds.api.GraphName;
import org.neo4j.gds.api.GraphStore;
import org.neo4j.gds.core.concurrency.Concurrency;
import org.neo4j.gds.core.utils.progress.tasks.ProgressTracker;
import org.neo4j.gds.core.utils.progress.tasks.Tasks;
import org.neo4j.gds.paths.dijkstra.PathFindingResult;
import org.neo4j.gds.procedures.algorithms.pathfinding.PathFindingStreamResult;
import org.neo4j.gds.termination.TerminationFlag;
import org.neo4j.procedure.Description;
import org.neo4j.procedure.Name;
import org.neo4j.procedure.Procedure;

import master.ProcedureHelper;

import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import static org.neo4j.procedure.Mode.READ;
import static org.neo4j.gds.config.NodeIdParser.parseToSingleNodeId;

public class ModifiedYensProc extends master.Procedure {
        @Procedure(name = "master.ModifiedYens", mode = READ)
        @Description("YENS_DESCRIPTION")
        public Stream<PathFindingStreamResult> stream(
                        @Name(value = "graphName") String graphNameString,
                        @Name(value = "configuration", defaultValue = "{}") Map<String, Object> configuration) {
                log.info("Modified yens started");

                ProcedureHelper procHelper = new ProcedureHelper(log, transaction, taskRegistryFactory, dbService,
                                username);

                log.info("username given " + username.toString());

                GraphName graphName = GraphName.parse(graphNameString);
                Long sourceNode = parseToSingleNodeId(configuration.get("sourceNode"),
                                "sourceNode");
                Long targetNode = parseToSingleNodeId(configuration.get("targetNode"),
                                "targetNode");
                int l = Integer.parseInt(configuration.get("l").toString());

                log.debug("Parsed " + configuration.get("sourceNode") + " to " + sourceNode);
                log.debug("Parsed " + configuration.get("targetNode") + " to " + targetNode);

                ModifiedYensConfig algorithmConfig = procHelper.configurationParser.parseConfiguration(
                                configuration,
                                ModifiedYensConfig::of);

                log.info("Passed algorithmConfig creation");

                ModifiedYensParameters config = new ModifiedYensParameters(sourceNode,
                                targetNode, l, new Concurrency(4));

                Graph graph = procHelper.getGraph(graphNameString, Optional.empty());

                log.info("Graph found");

                var initialTask = Tasks.leaf(AlgorithmLabel.Dijkstra.asString(),
                                graph.relationshipCount());
                var pathGrowingTask = Tasks.leaf("Path growing", config.l());
                var yensTask = Tasks.task(AlgorithmLabel.Yens.asString(), initialTask,
                                pathGrowingTask);

                log.info("Started creating progresstracker");
                log.debug("Algorithm config jobId: " + algorithmConfig.jobId());

                var progressTracker = procHelper.progressTrackerCreator.createProgressTracker(yensTask,
                                algorithmConfig);

                log.info("Finished creating progresstracker");

                GraphStore graphStore = procHelper.getGraphStore(graphName);

                log.info("Started yens");

                var result = singlePairPathModifiedYens(graph,
                                config, progressTracker, TerminationFlag.RUNNING_TRUE);

                log.info("Finished yens, started building result");

                return procHelper.resultBuilder.build(graph, graphStore, Optional.of(result));

        }

        public PathFindingResult singlePairPathModifiedYens(
                        Graph graph,
                        ModifiedYensParameters parameters,
                        ProgressTracker progressTracker,
                        TerminationFlag terminationFlag) {
                var algorithm = ModifiedYens.sourceTarget(
                                graph,
                                parameters,
                                progressTracker,
                                terminationFlag,
                                log);

                return algorithm.compute();
        }
}
