package master.dfs;

import org.neo4j.gds.api.Graph;
import org.neo4j.gds.collections.ha.HugeLongArray;
import org.neo4j.logging.Log;

public class Dfs {

    public static HugeLongArray computeDfs(Graph graph, long source, Log log) {

        HugeLongArray result = HugeLongArray.newArray(graph.nodeCount());

        log.debug("Started dfs");
        DfsVisitor traverse = new DfsVisitor(graph, source, log);

        int counter = 0;
        while (traverse.hasNext()) {
            log.debug("On " + counter + " iteration");
            result.set(counter, traverse.next());
            counter++;
        }

        log.debug("Stopped after " + counter + " iterations");

        log.debug("Returning " + result.toString());

        return result;
    }
}
