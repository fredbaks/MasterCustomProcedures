package master.dataHandling;

import io.github.cdimascio.dotenv.Dotenv;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.Path;
import org.neo4j.driver.AuthToken;
import org.neo4j.driver.AuthTokens;
import org.neo4j.driver.Config;
import org.neo4j.driver.Driver;
import org.neo4j.driver.GraphDatabase;
import org.neo4j.driver.Session;

public class CsvLoader {

    private static final String CSV_DIR = "CSV";

    private static final Dotenv DOTENV = Dotenv.configure().ignoreIfMissing().load();

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

    static Driver createDriver() {
        String boltUrl = envOrDefault("NEO4J_BOLT_URL", "bolt://localhost:7687");
        AuthToken auth = parseAuth(envOrDefault("NEO4J_AUTH", "neo4j/neo4j"));
        Config config = Config.builder().withoutEncryption().build();
        System.out.println("Connecting to " + boltUrl + " ...");
        return GraphDatabase.driver(boltUrl, auth, config);
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

    private static String envOrDefault(String name, String defaultValue) {
        String value = System.getenv(name);
        if (value != null && !value.isBlank())
            return value;
        value = DOTENV.get(name);
        if (value != null && !value.isBlank())
            return value;
        return defaultValue;
    }

    private static AuthToken parseAuth(String auth) {
        if (auth == null || auth.equalsIgnoreCase("none") || !auth.contains("/")) {
            return AuthTokens.none();
        }
        int slash = auth.indexOf('/');
        return AuthTokens.basic(auth.substring(0, slash), auth.substring(slash + 1));
    }
}
