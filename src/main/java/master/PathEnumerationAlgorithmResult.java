package master;

import java.util.List;

import org.neo4j.gds.collections.ha.HugeLongArray;

public class PathEnumerationAlgorithmResult {
    public List<HugeLongArray> paths;
    public List<Long> timestamps;
    public boolean timedOut;

    public PathEnumerationAlgorithmResult(List<HugeLongArray> paths, List<Long> timestamps, boolean timedOut) {
        this.paths = paths;
        this.timestamps = timestamps;
        this.timedOut = timedOut;
    }

    public void release() {
        paths = null;
        timestamps = null;
    }
}