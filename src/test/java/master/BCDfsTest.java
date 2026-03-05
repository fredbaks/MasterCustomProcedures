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
            session.writeTransaction(tx -> {
                tx.run("UNWIND range(1,60) AS i CREATE (:Node {number:i, name: 'N' + toString(i)});");
                tx.run("UNWIND range(1,60) AS i UNWIND [1,2,3,10,20] AS k WITH i, ((i-1 + k) % 60) + 1 AS j MATCH (a:Node {number:i}), (b:Node {number:j}) CREATE (a)-[:REL]->(b);");
                tx.run("UNWIND range(1,30) AS i MATCH (a:Node {number:i}), (b:Node {number:60 - i + 1}) CREATE (a)-[:CROSS]->(b), (b)-[:CROSS]->(a);");
                return null;
            });

            session.run(
                    "MATCH (source)-[]->(target) WITH gds.graph.project('bcdfsGraph', source, target, {}, {inverseIndexedRelationshipTypes: ['*']}) as g return g.graphName");

            Record record = session
                    .run("MATCH (a:Node {number:1}), (f:Node {number:59}) RETURN id(a) AS a_id, id(f) AS f_id")
                    .single();

            long src = record.get("a_id").asLong();
            long trg = record.get("f_id").asLong();

            Map<String, Object> algParams = new HashMap<>();
            algParams.put("sourceNode", src);
            algParams.put("targetNode", trg);
            for (int k = 1; k < 7; k++) {
                algParams.put("k", k);

                Record r = session.run(
                        "CALL master.cdfs($graphName, $params) YIELD source, results, paths RETURN source, results, paths",
                        Map.of("graphName", "bcdfsGraph", "params", algParams)).single();

                long source = r.get("source").asLong();
                List<List<Long>> results = r.get("results").asList(t -> t.asList(n -> Long.parseLong(n.toString())));

                assertTrue(source == src);

                assertEquals(EXPECTED_DFS_RESULTS[k], results.size(), "Results size should match for k:" + k);
            }
            session.run("CALL gds.graph.drop('bcdfsGraph') YIELD graphName");
        }
    }

}
