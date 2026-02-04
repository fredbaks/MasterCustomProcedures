package master;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.testcontainers.junit.jupiter.Testcontainers;

import org.neo4j.driver.Session;
import org.neo4j.driver.Result;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Testcontainers
public class ModifiedYensTest extends TestSetup {

    @Test
    public void testContainerConnection() {
        try (Session session = driver.session()) {
            long one = session.run("RETURN 1", Collections.emptyMap()).next().get(0).asLong();
            assertTrue(one == 1, "Returned should have been 1 got " + one);
        }
    }

    @Test
    public void procedureIsRegistered() {

        try (Session session = driver.session()) {

            Result result = session.run(
                    "SHOW PROCEDURES YIELD name " +
                            "WHERE name = 'master.ModifiedYens' " +
                            "RETURN name");

            assertTrue(result.hasNext(), "Procedure example.ModifiedYens should be registered");
        }
    }

    @Test
    public void procedureReturnsAllPaths() {
        try (Session session = driver.session()) {
            session.run(
                    "CREATE (a:Node {name:'A'}), (b:Node {name:'B'}), (c:Node {name:'C'}), (d:Node {name:'D'}) " +
                            "CREATE (a)-[:REL]->(d), (a)-[:REL]->(b), (b)-[:REL]->(d), (a)-[:REL]->(c), (c)-[:REL]->(d)");

            session.run(
                    "MATCH (source: Node)-[:REL]->(target: Node) WITH gds.graph.project('testGraph', source, target) as g return g.graphName");

            long srcId = session.run("MATCH (a:Node {name:'A'}) RETURN id(a) AS id").single().get("id").asLong();
            long tgtId = session.run("MATCH (d:Node {name:'D'}) RETURN id(d) AS id").single().get("id").asLong();

            Map<String, Object> algParams = new HashMap<>();
            algParams.put("sourceNode", srcId);
            algParams.put("targetNode", tgtId);
            algParams.put("l", 2);

            Result r = session.run(
                    "CALL master.ModifiedYens($graphName, $params) YIELD path RETURN count(path) AS cnt",
                    Map.of("graphName", "testGraph", "params", algParams));

            long count = r.single().get("cnt").asLong();

            assertTrue(count == 2, "Expected 2 paths but got " + count);

            algParams.put("l", 3);

            r = session.run(
                    "CALL master.ModifiedYens($graphName, $params) YIELD path RETURN count(path) AS cnt",
                    Map.of("graphName", "testGraph", "params", algParams));

            count = r.single().get("cnt").asLong();

            assertTrue(count == 3, "Expected 3 paths but got " + count);

            algParams.put("l", 1);

            r = session.run(
                    "CALL master.ModifiedYens($graphName, $params) YIELD path RETURN count(path) AS cnt",
                    Map.of("graphName", "testGraph", "params", algParams));

            count = r.single().get("cnt").asLong();

            assertTrue(count == 1, "Expected 1 paths but got " + count);

            session.run("CALL gds.graph.drop('testGraph') YIELD graphName");
        }
    }

}
