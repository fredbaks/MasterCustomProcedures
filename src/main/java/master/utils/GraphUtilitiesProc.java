package master.utils;

import static org.neo4j.procedure.Mode.WRITE;

import java.util.Map;

import org.neo4j.procedure.Name;
import org.neo4j.procedure.Procedure;

public class GraphUtilitiesProc extends master.Procedure {

    @Procedure(name = "master.testGraphUtils", mode = WRITE)
    public void stream() {
        GraphUtilities graphUtilities = new GraphUtilities(tx, log);

        Long nodeId = graphUtilities.addNode("temp", Map.of());

    }
}
