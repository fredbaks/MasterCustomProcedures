package master;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.neo4j.gds.GraphParameters;
import org.neo4j.gds.NodeLabel;
import org.neo4j.gds.RelationshipType;
import org.neo4j.gds.api.CloseableResourceRegistry;
import org.neo4j.gds.api.Graph;
import org.neo4j.gds.api.GraphLoaderContext;
import org.neo4j.gds.api.GraphName;
import org.neo4j.gds.api.GraphStore;
import org.neo4j.gds.api.NodeLookup;
import org.neo4j.gds.api.User;
import org.neo4j.gds.applications.algorithms.machinery.ProgressTrackerCreator;
import org.neo4j.gds.applications.algorithms.machinery.RequestScopedDependencies;
import org.neo4j.gds.configuration.DefaultsConfiguration;
import org.neo4j.gds.configuration.LimitsConfiguration;
import org.neo4j.gds.core.Username;
import org.neo4j.gds.core.loading.GraphStoreCatalogService;
import org.neo4j.gds.core.loading.validation.GraphStoreValidation;
import org.neo4j.gds.core.utils.logging.LoggerForProgressTrackingAdapter;
import org.neo4j.gds.core.utils.progress.EmptyTaskStore;
import org.neo4j.gds.core.utils.progress.TaskRegistryFactory;
import org.neo4j.gds.core.utils.warnings.EmptyUserLogRegistryFactory;
import org.neo4j.gds.core.utils.warnings.EmptyUserLogStore;
import org.neo4j.gds.logging.LogAdapter;
import org.neo4j.gds.procedures.DatabaseIdAccessor;
import org.neo4j.gds.procedures.algorithms.configuration.ConfigurationParser;
import org.neo4j.gds.procedures.algorithms.configuration.UserSpecificConfigurationParser;
import org.neo4j.gds.transaction.TransactionCloseableResourceRegistry;
import org.neo4j.gds.transaction.TransactionNodeLookup;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.kernel.api.KernelTransaction;
import org.neo4j.logging.Log;

public class ProcedureHelper {

        public final Collection<NodeLabel> nodeLabels = List.of(NodeLabel.ALL_NODES);
        public final Collection<RelationshipType> relationshipLabels = List.of(RelationshipType.ALL_RELATIONSHIPS);
        public final GraphParameters defaulGraphParameters = new GraphParameters(nodeLabels, relationshipLabels, false,
                        Optional.empty());

        public final LogAdapter logAdapter;
        public final LoggerForProgressTrackingAdapter loggerForProgressTrackingAdapter;

        public final NodeLookup nodeLookup;
        public final CloseableResourceRegistry closeableResourceRegistry;
        public final GraphStoreCatalogService graphStoreCatalogService = new GraphStoreCatalogService();

        public final PathFindingResultBuilderForStreamMode resultBuilder;

        public final RequestScopedDependencies requestScopedDependencies;
        public final ProgressTrackerCreator progressTrackerCreator;

        public final UserSpecificConfigurationParser configurationParser;

        public ProcedureHelper(Log log, KernelTransaction transaction, TaskRegistryFactory taskRegistryFactory,
                        GraphDatabaseService dbService, Username username) {
                this.logAdapter = new LogAdapter(log);
                this.loggerForProgressTrackingAdapter = new LoggerForProgressTrackingAdapter(
                                logAdapter);

                this.nodeLookup = new TransactionNodeLookup(transaction);
                this.closeableResourceRegistry = new TransactionCloseableResourceRegistry(
                                transaction);

                this.resultBuilder = new PathFindingResultBuilderForStreamMode(
                                closeableResourceRegistry,
                                nodeLookup,
                                true);

                this.requestScopedDependencies = RequestScopedDependencies.builder()
                                .databaseId(new DatabaseIdAccessor().getDatabaseId(dbService))
                                .graphLoaderContext(GraphLoaderContext.NULL_CONTEXT)
                                .taskRegistryFactory(taskRegistryFactory)
                                .taskStore(EmptyTaskStore.INSTANCE)
                                .user(new User(username == null ? Username.EMPTY_USERNAME.username()
                                                : username.username(), false))
                                .userLogRegistryFactory(EmptyUserLogRegistryFactory.INSTANCE)
                                .userLogStore(EmptyUserLogStore.INSTANCE)
                                .build();

                this.progressTrackerCreator = new ProgressTrackerCreator(
                                loggerForProgressTrackingAdapter,
                                requestScopedDependencies);

                this.configurationParser = new UserSpecificConfigurationParser(new ConfigurationParser(
                                DefaultsConfiguration.Instance,
                                LimitsConfiguration.Instance), requestScopedDependencies.user());

        }

        public GraphStore getGraphStore(GraphName graphName) {
                return graphStoreCatalogService.getGraphStoreCatalogEntry(graphName,
                                requestScopedDependencies.user(), Optional.empty(),
                                requestScopedDependencies.databaseId()).graphStore();
        }

        public Graph getGraph(String graphNameString,
                        Optional<GraphParameters> optionalGraphParameters) {
                GraphName graphName = GraphName.parse(graphNameString);

                var graphParameters = optionalGraphParameters.orElse(defaulGraphParameters);

                var graphStoreValidation = new GraphStoreValidation(new MockAlgoValidation());

                var graphResources = graphStoreCatalogService.fetchGraphResources(graphName,
                                graphParameters, Optional.empty(), graphStoreValidation, Optional.empty(),
                                requestScopedDependencies.user(),
                                requestScopedDependencies.databaseId());

                return graphResources.graph();
        }
}
