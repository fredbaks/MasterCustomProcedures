package master;

import static org.junit.jupiter.api.Assertions.fail;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.neo4j.driver.AuthTokens;
import org.neo4j.driver.Driver;
import org.neo4j.driver.GraphDatabase;
import org.neo4j.driver.Session;
import org.testcontainers.containers.BindMode;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.neo4j.Neo4jContainer;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.utility.MountableFile;

public class TestSetup {

    protected Driver driver;

    protected static int[] EXPECTED_DFS_RESULTS = { 0, 0, 1, 4, 16, 75, 554 };

    @Container
    private static Neo4jContainer neo4jContainer = new Neo4jContainer(DockerImageName.parse("neo4j:2025.10.1"))
            .withoutAuthentication() // Disable password
            .withFileSystemBind(System.getProperty("user.dir") + "\\testlogs", "/logs", BindMode.READ_WRITE)
            .withFileSystemBind(System.getProperty("user.dir") + "\\testoutput", "/var/lib/neo4j/output",
                    BindMode.READ_WRITE)
            .withCopyFileToContainer(
                    MountableFile.forHostPath(System.getProperty("user.dir") + "\\conf\\user-logs.xml"),
                    "/var/lib/neo4j/conf/user-logs.xml")
            .withCopyFileToContainer(
                    MountableFile.forHostPath(
                            System.getProperty("user.dir") + "\\plugins\\neo4j-graph-data-science-2.23.0.jar"),
                    "/var/lib/neo4j/plugins/neo4j-graph-data-science-2.23.0.jar")
            .withCopyFileToContainer(
                    MountableFile.forHostPath(
                            System.getProperty("user.dir") + "\\plugins\\master-procedures-0.0.1.jar"),
                    "/var/lib/neo4j/plugins/master-procedures-0.0.1.jar")
            .withEnv("NEO4J_dbms_security_procedures_unrestricted", "gds.*,master.*")
            .withEnv("NEO4J_dbms_security_procedures_allowlist", "gds.*,master.*")
            .withReuse(true);

    @BeforeAll
    public void start() {
        neo4jContainer.start();
    }

    @AfterAll
    public void shutdown() {
        neo4jContainer.close();
    }

    @BeforeEach
    void testSetup() {
        newSession();
        dropAll();
    }

    @AfterEach
    void testCleanup() {
        stopSession();
    }

    private void newSession() {
        String boltUrl = neo4jContainer.getBoltUrl();
        try {
            this.driver = GraphDatabase.driver(boltUrl, AuthTokens.none());
        } catch (Exception e) {
            fail(e.getMessage());
        }
    }

    private void stopSession() {
        if (this.driver != null) {
            try {
                this.driver.close();
            } catch (Exception ignored) {
            }
        }
    }

    private void dropAll() {
        executeCypher("MATCH (n) DETACH DELETE n");
    }

    private void executeCypher(String statement) {
        try (Driver driver = GraphDatabase.driver(neo4jContainer.getBoltUrl());
                Session session = driver.session()) {
            driver.session().run(statement, Collections.emptyMap());
        }
    }
}
