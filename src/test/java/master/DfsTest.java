package master;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.neo4j.driver.Result;
import org.neo4j.driver.Record;
import org.neo4j.driver.Session;
import org.neo4j.driver.types.Path;
import org.testcontainers.junit.jupiter.Testcontainers;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Testcontainers
public class DfsTest extends TestSetup {

    @Test
    public void dfsProcedureIsRegistered() {
        try (Session session = driver.session()) {
            Result result = session.run(
                    "SHOW PROCEDURES YIELD name " +
                            "WHERE name = 'master.dfs' " +
                            "RETURN name");
            assertTrue(result.hasNext(), "Procedure master.dfs should be registered");
        }
    }

    @Test
    public void dfsProcedureReturnsExpectedPaths() {
        try (Session session = driver.session()) {
            session.run(
                    "CREATE (a:Node {name:'A'}), (b:Node {name:'B'}), (c:Node {name:'C'}), (d:Node {name:'D'}), (e:Node {name:'E'}), (f:Node {name:'f'}), (g:Node {name:'g'}) "
                            +
                            "CREATE (a)-[:REL]->(b), (b)-[:REL]->(c), (c)-[:REL]->(d), (a)-[:REL]->(d), (a)-[:REL]->(e), (b)-[:REL]->(f), (d)-[:REL]->(e), (c)-[:REL]->(f), (f)-[:REL]->(g)");

            session.run(
                    "MATCH (source)-[]->(target) WITH gds.graph.project('dfsGraph', source, target) as g return g.graphName");

            Long src = session.run("MATCH (a:Node {name:'A'}) RETURN id(a) AS id").single().get("id").asLong();
            // Long src = 3L;

            Map<String, Object> algParams = new HashMap<>();
            algParams.put("sourceNode", src);

            Record r = session.run(
                    "CALL master.dfs($graphName, $params) YIELD source, result, path RETURN source, result, path",
                    Map.of("graphName", "dfsGraph", "params", algParams)).single();

            long source = r.get("source").asLong();
            List<Long> result = r.get("result").asList(t -> Long.parseLong(t.toString()));
            Path path = r.get("path").asPath();

            System.out.print(source + ", " + result + ", " + path + "\n");

            assertTrue(source == src);

            assertEquals(7, result.size(), "Results size should match");
            assertEquals(6, path.length(), "Path length should match");

            // assertTrue(count == 2, "Expected 2 paths but got " + count);

            session.run("CALL gds.graph.drop('dfsGraph') YIELD graphName");
        }
    }

}
