package master;

import it.unimi.dsi.fastutil.longs.LongBigArrayBigList;

public class PathEnumerationAlgorithmResult {
    public LongBigArrayBigList paths;
    public int stride;
    public long[] timestamps;
    public boolean timedOut;

    public PathEnumerationAlgorithmResult(LongBigArrayBigList paths, int stride, long[] timestamps, boolean timedOut) {
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