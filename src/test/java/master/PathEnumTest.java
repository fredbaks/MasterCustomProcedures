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
public class PathEnumTest extends TestSetup {

    @Test
    public void dfsProcedureIsRegistered() {
        try (Session session = driver.session()) {
            Result result = session.run(
                    "SHOW PROCEDURES YIELD name " +
                            "WHERE name = 'master.pathenum' " +
                            "RETURN name");
            assertTrue(result.hasNext(), "Procedure master.pathenum should be registered");
        }
    }

    @Test
    public void PathEnumProcedureReturnsExpectedPaths() {
        try (Session session = driver.session()) {
            session.writeTransaction(tx -> {
                tx.run("UNWIND range(1,60) AS i CREATE (:Node {number:i, name: 'N' + toString(i)});");
                tx.run("UNWIND range(1,60) AS i UNWIND [1,2,3,10,20] AS k WITH i, ((i-1 + k) % 60) + 1 AS j MATCH (a:Node {number:i}), (b:Node {number:j}) CREATE (a)-[:REL]->(b);");
                tx.run("UNWIND range(1,30) AS i MATCH (a:Node {number:i}), (b:Node {number:60 - i + 1}) CREATE (a)-[:CROSS]->(b), (b)-[:CROSS]->(a);");
                return null;
            });

            session.run(
                    "MATCH (source)-[]->(target) WITH gds.graph.project('testGraph', source, target, {}, {inverseIndexedRelationshipTypes: ['*']}) as g return g.graphName");

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
                        "CALL master.pathenum($graphName, $params) YIELD source, results, paths RETURN source, results, paths",
                        Map.of("graphName", "testGraph", "params", algParams)).single();

                long source = r.get("source").asLong();
                List<List<Long>> results = r.get("results").asList(t -> t.asList(n -> Long.parseLong(n.toString())));

                assertTrue(source == src);

                assertEquals(EXPECTED_DFS_RESULTS[k], results.size(), "Results size should match for k:" + k);
            }

            algParams.put("timeoutDuration", 1L);
            algParams.put("k", 10);

            Record r = session.run(
                    "CALL master.pathenum($graphName, $params) YIELD source, startTime, endTime, timedOut RETURN source, startTime, endTime, timedOut",
                    Map.of("graphName", "testGraph", "params", algParams)).single();

            long source = r.get("source").asLong();
            boolean timedOut = r.get("timedOut").asBoolean();

            long startTime = r.get("startTime").asLong();
            long endTime = r.get("endTime").asLong();

            System.out.print("Algorithm used " + (((double) (endTime - startTime)) / 1000000000) + " seconds\n");

            assertTrue(source == src);

            assertTrue(timedOut);

            session.run("CALL gds.graph.drop('testGraph') YIELD graphName");
        }
    }

    @Test
    public void PathEnumJoinProcedureReturnsExpectedPaths() {
        try (Session session = driver.session()) {
            session.writeTransaction(tx -> {
                tx.run("UNWIND range(1,60) AS i CREATE (:Node {number:i, name: 'N' + toString(i)});");
                tx.run("UNWIND range(1,60) AS i UNWIND [1,2,3,10,20] AS k WITH i, ((i-1 + k) % 60) + 1 AS j MATCH (a:Node {number:i}), (b:Node {number:j}) CREATE (a)-[:REL]->(b);");
                tx.run("UNWIND range(1,30) AS i MATCH (a:Node {number:i}), (b:Node {number:60 - i + 1}) CREATE (a)-[:CROSS]->(b), (b)-[:CROSS]->(a);");
                return null;
            });

            session.run(
                    "MATCH (source)-[]->(target) WITH gds.graph.project('testGraph', source, target, {}, {inverseIndexedRelationshipTypes: ['*']}) as g return g.graphName");

            Record record = session
                    .run("MATCH (a:Node {number:1}), (f:Node {number:59}) RETURN id(a) AS a_id, id(f) AS f_id")
                    .single();

            long src = record.get("a_id").asLong();
            long trg = record.get("f_id").asLong();

            Map<String, Object> algParams = new HashMap<>();
            algParams.put("sourceNode", src);
            algParams.put("targetNode", trg);
            algParams.put("runJoin", true);

            for (int k = 1; k < 7; k++) {
                algParams.put("k", k);

                Record r = session.run(
                        "CALL master.pathenum($graphName, $params) YIELD source, results, paths, nodeTimestamps, startTime, endTime RETURN source, results, paths, nodeTimestamps, startTime, endTime",
                        Map.of("graphName", "testGraph", "params", algParams)).single();

                long source = r.get("source").asLong();
                List<List<Long>> results = r.get("results").asList(t -> t.asList(n -> Long.parseLong(n.toString())));

                long startTime = r.get("startTime").asLong();
                long endTime = r.get("endTime").asLong();

                System.out.print("Algorithm used " + (((double) (endTime - startTime)) / 1000000000) + " seconds\n");

                assertTrue(source == src);

                assertEquals(EXPECTED_DFS_RESULTS[k], results.size(), "Results size should match for k:" + k);

                for (List<Long> path : results) {
                    assertEquals(src, path.getFirst());
                    assertEquals(trg, path.getLast());
                }
            }

            algParams.put("timeoutDuration", 1L);
            algParams.put("k", 10);

            Record r = session.run(
                    "CALL master.pathenum($graphName, $params) YIELD source, startTime, endTime, timedOut RETURN source, startTime, endTime, timedOut",
                    Map.of("graphName", "testGraph", "params", algParams)).single();

            long source = r.get("source").asLong();
            boolean timedOut = r.get("timedOut").asBoolean();

            long startTime = r.get("startTime").asLong();
            long endTime = r.get("endTime").asLong();

            System.out.print("Algorithm used " + (((double) (endTime - startTime)) / 1000000000) + " seconds\n");

            assertTrue(source == src);

            assertTrue(timedOut);

            session.run("CALL gds.graph.drop('testGraph') YIELD graphName");
        }
    }

}
