package master;

import java.util.Collection;

import org.neo4j.gds.api.GraphStore;
import org.neo4j.gds.core.loading.validation.AlgorithmGraphStoreRequirements;
import org.neo4j.gds.NodeLabel;
import org.neo4j.gds.RelationshipType;

// The plan is to not specify relationship types, but rather decide this through projections
public class MockAlgoValidation implements AlgorithmGraphStoreRequirements {
    @Override
    public void validate(
            GraphStore graphStore,
            Collection<NodeLabel> selectedLabels,
            Collection<RelationshipType> selectedRelationshipTypes) {
    }
}
