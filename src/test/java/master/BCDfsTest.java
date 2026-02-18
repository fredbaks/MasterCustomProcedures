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
public class BCDfsTest extends TestSetup {

    @Test
    public void dfsProcedureIsRegistered() {
        try (Session session = driver.session()) {
            Result result = session.run(
                    "SHOW PROCEDURES YIELD name " +
                            "WHERE name = 'master.bcdfs' " +
                            "RETURN name");
            assertTrue(result.hasNext(), "Procedure master.bcdfs should be registered");
        }
    }

    @Test
    public void dfsProcedureReturnsExpectedPaths() {
        try (Session session = driver.session()) {
            session.run(
                    "CREATE (a:Node {name:'A'}), (b:Node {name:'B'}), (c:Node {name:'C'}), (d:Node {name:'D'}), (e:Node {name:'E'}), (f:Node {name:'F'})"
                            +
                            "CREATE (a)-[:REL]->(b), (a)-[:REL]->(c), (a)-[:REL]->(d), (a)-[:REL]->(f), (b)-[:REL]->(e), (c)-[:REL]->(e), (d)-[:REL]->(e), (e)-[:REL]->(f)");

            session.run(
                    "MATCH (source)-[]->(target) WITH gds.graph.project('bcdfsGraph', source, target) as g return g.graphName");

            Record record = session
                    .run("MATCH (a:Node {name:'A'}), (f:Node {name:'F'}) RETURN id(a) AS a_id, id(f) AS f_id").single();

            long src = record.get("a_id").asLong();
            long trg = record.get("f_id").asLong();
            // Long src = 3L;

            Map<String, Object> algParams = new HashMap<>();
            algParams.put("sourceNode", src);
            algParams.put("targetNode", trg);
            algParams.put("k", 3);

            Record r = session.run(
                    "CALL master.cdfs($graphName, $params) YIELD source, results, paths RETURN source, results, paths",
                    Map.of("graphName", "bcdfsGraph", "params", algParams)).single();

            long source = r.get("source").asLong();
            List<List<Long>> results = r.get("results").asList(t -> t.asList(n -> Long.parseLong(n.toString())));
            List<Path> paths = r.get("paths").asList(p -> p.asPath());

            System.out.print(source + ", " + results + ", " + paths + "\n");

            assertTrue(source == src);

            assertEquals(4, results.size(), "Results size should match");
            assertEquals(4, paths.size(), "Paths length should match");

            session.run("CALL gds.graph.drop('bcdfsGraph') YIELD graphName");
        }
    }

}
