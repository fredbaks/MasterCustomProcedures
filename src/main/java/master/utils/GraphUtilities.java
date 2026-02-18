package master.utils;

import java.util.Map;
import java.util.Set;

import org.neo4j.graphdb.Transaction;
import org.neo4j.logging.Log;

public class GraphUtilities {

    private final Transaction tx;
    private final Log log;

    private final String tempRelationship = "TEMP";

    public GraphUtilities(Transaction tx, Log log) {
        this.tx = tx;
        this.log = log;
    }

    public Long addNode(String nodeType, Map<String, String> properties) {
        log.debug("Create node with type " + nodeType);

        String propertyString = createPropertiesString(properties);

        String queryString = String.format("CREATE (new:%s %s) RETURN id(new)", nodeType, propertyString);

        String result = tx.execute(queryString).columns().getFirst();

        log.debug("Created node with id: " + result);

        return Long.parseLong(result);
    }

    public void removeNode(Long nodeId) {
        String queryString = String.format("MATCH (node) WHERE id(node) = %d DETACH DELETE node", nodeId);

        tx.execute(queryString);

        log.debug("Removed node with id:" + nodeId);
    }

    public void addRelationship(Long sourceId, Long targetId) {

        String queryString = String.format(
                "MATCH (source), (target) WHERE id(source) = %d AND id(target) = %d CREATE (source)-[:%s]->(target)",
                sourceId, targetId, tempRelationship);

        tx.execute(queryString);
    }

    public void addRelationships(Long sourceId, Set<Long> targetIds) {
        String queryString = String.format(
                "MATCH (source) WHERE id(source) = %d " +
                        "UNWIND $targetIds AS targetId " +
                        "MATCH (target) WHERE id(target) = targetId " +
                        "CREATE (source)-[%s]->(target)",
                sourceId, tempRelationship);

        tx.execute(queryString, Map.of("targetIds", targetIds));

        log.debug("Created edges from node with id: " + sourceId + " to target nodes: " + targetIds);
    }

    public void addInverseRelationships(Long sourceId, Set<Long> targetIds) {
        String queryString = String.format(
                "MATCH (source) WHERE id(source) = %d " +
                        "UNWIND $targetIds AS targetId " +
                        "MATCH (target) WHERE id(target) = targetId " +
                        "CREATE (target)-[%s]->(source)",
                sourceId, tempRelationship);

        tx.execute(queryString, Map.of("targetIds", targetIds));

        log.debug("Created edges from node with id: " + sourceId + " to target nodes: " + targetIds);
    }

    private String createPropertiesString(Map<String, String> properties) {

        StringBuilder propertyString = new StringBuilder("{");

        properties.forEach((key, value) -> {
            propertyString.append(String.format("%s:%s,", key, value));
        });

        propertyString.append("}");

        return propertyString.toString();
    }

    public void createGDSProjection(String graphName) {

        String queryString = String.format(
                "MATCH (source)-[]->(target) WITH gds.graph.project('%s', source, target) AS g RETURN g.graphName",
                graphName);

        tx.execute(queryString);
    }

    public void dropGDSProjection(String graphName) {
        String queryString = String.format(
                "CALL gds.graph.drop('%s') YIELD g.graphName",
                graphName);

        tx.execute(queryString);
    }

}
