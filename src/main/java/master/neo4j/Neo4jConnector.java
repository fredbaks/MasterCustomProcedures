package master.neo4j;

import org.neo4j.driver.AuthToken;
import org.neo4j.driver.AuthTokens;
import org.neo4j.driver.Config;
import org.neo4j.driver.Driver;
import org.neo4j.driver.GraphDatabase;

import io.github.cdimascio.dotenv.Dotenv;

public class Neo4jConnector {

    private static final Dotenv DOTENV = Dotenv.configure().ignoreIfMissing().load();

    public static Driver createDriver() {
        String boltUrl = envOrDefault("NEO4J_BOLT_URL", "bolt://localhost:7687");
        AuthToken auth = parseAuth(envOrDefault("NEO4J_AUTH", "neo4j/neo4j"));
        Config config = Config.builder().withoutEncryption().build();
        System.out.println("Connecting to " + boltUrl + " ...");
        return GraphDatabase.driver(boltUrl, auth, config);
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
