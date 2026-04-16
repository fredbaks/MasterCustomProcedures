package master.dataHandling;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.neo4j.driver.Driver;
import org.neo4j.driver.Session;

import master.neo4j.Neo4jConnector;

public class CsvLoader extends Neo4jConnector {

    private static final String CSV_DIR = "CSV";

    public static void main(String[] args) {
        if (args.length == 0) {
            System.err.println("Usage: DataLoader <name>");
            System.err.println("  Loads CSV/<name>.csv into the Neo4j database.");
            System.exit(1);
        }

        String name = args[0];
        Path csvPath = Paths.get(CSV_DIR, name + ".csv");

        if (!Files.exists(csvPath)) {
            System.err.println("File not found: " + csvPath.toAbsolutePath());
            System.exit(1);
        }

        try (Driver driver = createDriver()) {
            loadDataset(driver, name);
        }

        System.out.println("Done.");
    }

    public static void loadDataset(Driver driver, String name) {
        Path csvPath = Paths.get(CSV_DIR, name + ".csv");
        if (!Files.exists(csvPath)) {
            throw new IllegalArgumentException(
                    "File not found: " + csvPath.toAbsolutePath());
        }

        clearDatabase(driver);

        String filename = name + ".csv";
        System.out.println("Loading " + filename + " ...");
        long[] counts = loadEdgeCsv(driver, filename);
        System.out.printf("%s loaded: %d nodes created, %d relationships created.%n",
                filename, counts[0], counts[1]);
    }

    static void clearDatabase(Driver driver) {
        System.out.println("Clearing database ...");
        try (Session session = driver.session()) {
            session.run(
                    "CALL { MATCH (n) DETACH DELETE n } IN TRANSACTIONS OF 10000 ROWS").consume();
        }
        System.out.println("Database cleared.");
    }

    private static long[] loadEdgeCsv(Driver driver, String filename) {
        String cypher = """
                LOAD CSV WITH HEADERS FROM 'file:///%s' AS row
                CALL {
                    WITH row
                    MERGE (a:Node {id: row.START_ID})
                    MERGE (b:Node {id: row.END_ID})
                    CREATE (a)-[:EDGE]->(b)
                } IN TRANSACTIONS OF 1000 ROWS
                """.formatted(filename);

        try (Session session = driver.session()) {
            var summary = session.run(cypher).consume();
            var counters = summary.counters();
            return new long[] { counters.nodesCreated(), counters.relationshipsCreated() };
        }
    }
}
