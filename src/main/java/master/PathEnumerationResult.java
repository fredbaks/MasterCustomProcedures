package master;

import org.neo4j.graphdb.Path;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class PathEnumerationResult {
    public Long source;
    public List<List<Long>> results = new ArrayList<>();
    public Map<String, Long> nodeTimestamps = new HashMap<>();
    public List<Path> paths = new ArrayList<>();
    public Long startTime;
    public Long endTime;
}
