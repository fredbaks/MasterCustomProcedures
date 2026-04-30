package master.neo4j;

import java.util.List;
import java.util.stream.Collectors;

import org.neo4j.driver.Driver;
import org.neo4j.driver.Record;
import org.neo4j.driver.Session;
import org.neo4j.driver.exceptions.ClientException;

public class CypherConnector extends Neo4jConnector {

    private static final String PROJECTION_QUERY_TEMPLATE = """
            Match (a)-[]->(b) WITH gds.graph.project('%s', a, b, {}, {inverseIndexedRelationshipTypes: ['*']})
            as g return g.graphName
            """;

    private static final String DROP_PROJECTION_QUERY_TEMPLATE = """
            CALL gds.graph.drop('%s')
            """;

    private static final String PATH_ENUMERATION_QUERY_TEMPLATE = """
            MATCH (source {id: '%d'}), (target {id: '%d'})
            CALL master.%s('%s', {sourceNode: source, targetNode: target, k: %d})
            YIELD source as sourceNode, startTime, endTime return sourceNode, startTime, endTime
            """;

    private static final String NODE_ID_PROPERTY_QUERY_TEMPLATE = """
            MATCH (node) WHERE id(node) = %d RETURN node.id as nodeId
            """;

    public static void main(String[] args) {
        if (args.length == 0) {
            System.err.println("Usage: CypherQuery <cypher-query-string> <show-result: false>");
            System.err.println("Runs given cypher query on Neo4j docker container.");
            System.exit(1);
        }

        String queryString = args[0];

        boolean printResult = false;
        try {
            printResult = Boolean.parseBoolean(args[1]);
        } catch (Exception e) {
        }

        try (Driver driver = createDriver()) {
            System.out.println("Running query");
            List<Record> result = runQuery(driver, queryString, printResult);

            if (result != null) {
                System.out.println(result);
            }
        } catch (Exception e) {
            System.out.println(e.getMessage() + "\n" + e.getStackTrace());
        } finally {
            System.out.println("Done");
        }
    }

    public static List<Record> runQuery(Driver driver, String queryString, boolean returnResult) {
        try (Session session = driver.session()) {
            if (returnResult) {
                List<Record> result = session.run(queryString).stream().collect(Collectors.toList());
                return result;
            }

            session.run(queryString).consume();
            return null;
        } catch (Exception e) {
            throw e;
        }
    }

    public static void createProjection(Driver driver, String projectionName, boolean withForce) {
        String projectionQuery = String.format(PROJECTION_QUERY_TEMPLATE, projectionName);

        try {
            runQuery(driver, projectionQuery, false);
        } catch (ClientException e) {
            if (e.getMessage().contains("already exists")) {
                System.out.println("Graphprojection with name " + projectionName + " already exists.");

                if (withForce) {
                    dropProjection(driver, projectionName);
                    createProjection(driver, projectionName, false);
                }
            } else {
                System.err.println(e.getMessage());
                e.printStackTrace();
                System.exit(0);
            }
        }
    };

    public static void dropProjection(Driver driver, String projectionName) {
        String dropProjectionQuery = String.format(DROP_PROJECTION_QUERY_TEMPLATE, projectionName);

        runQuery(driver, dropProjectionQuery, false);

    }

    public static void runPathEnumeration(Driver driver, Long source, Long target, String algorithm,
            String projectionName, int hopLimit) {
        String pathEnumerationQuery = String.format(PATH_ENUMERATION_QUERY_TEMPLATE, source, target, algorithm,
                projectionName, hopLimit);

        runQuery(driver, pathEnumerationQuery, false);
    }

    public static Long getNodeIdProperty(Driver driver, Long node) {
        String queryString = String.format(NODE_ID_PROPERTY_QUERY_TEMPLATE, node);

        return Long.parseLong(runQuery(driver, queryString, true).get(0).get("nodeId").asString());
    }

}
