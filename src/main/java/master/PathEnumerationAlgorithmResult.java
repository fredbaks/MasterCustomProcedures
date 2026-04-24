package master;

import java.util.HashMap;

import org.neo4j.gds.collections.ha.HugeLongArray;

public class PathEnumerationAlgorithmResult {
    public HashMap<HugeLongArray, Long> results;
    public boolean timedOut;

    public PathEnumerationAlgorithmResult(HashMap<HugeLongArray, Long> results, boolean timedOut) {
        this.results = results;
        this.timedOut = timedOut;
    }
}