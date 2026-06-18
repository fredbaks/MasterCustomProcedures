package master;

import java.util.List;

import org.neo4j.gds.collections.ha.HugeLongArray;

public class PathEnumerationAlgorithmResult {
    public List<HugeLongArray> paths;
    public long[] timestamps;
    public boolean timedOut;

    public PathEnumerationAlgorithmResult(List<HugeLongArray> paths, long[] timestamps, boolean timedOut) {
        this.paths = paths;
        this.timestamps = timestamps;
        this.timedOut = timedOut;
    }

    public void release() {
        paths = null;
        timestamps = null;
    }
}