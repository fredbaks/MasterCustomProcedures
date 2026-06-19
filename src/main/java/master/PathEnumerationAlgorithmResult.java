package master;

public class PathEnumerationAlgorithmResult {
    public long[] paths;
    public int stride;
    public long[] timestamps;
    public boolean timedOut;

    public PathEnumerationAlgorithmResult(long[] paths, int stride, long[] timestamps, boolean timedOut) {
        this.paths = paths;
        this.stride = stride;
        this.timestamps = timestamps;
        this.timedOut = timedOut;
    }

    public void release() {
        paths = null;
        timestamps = null;
    }
}