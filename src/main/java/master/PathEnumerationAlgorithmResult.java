package master;

import java.util.List;

import org.neo4j.gds.collections.ha.HugeLongArray;

import com.carrotsearch.hppc.LongLongHashMap;

public class PathEnumerationAlgorithmResult {
    // public List<HugeLongArray> paths;
    // public List<Long> timestamps;
    public boolean timedOut;
    public LongLongHashMap nodeTimestamps;
    public long pathCount;

    public PathEnumerationAlgorithmResult(
            // List<HugeLongArray> paths, List<Long> timestamps,
            LongLongHashMap nodeTimestamps, long pathCount, boolean timedOut) {
        // this.paths = paths;
        // this.timestamps = timestamps;
        this.timedOut = timedOut;
        this.nodeTimestamps = nodeTimestamps;
        this.pathCount = pathCount;
    }
}