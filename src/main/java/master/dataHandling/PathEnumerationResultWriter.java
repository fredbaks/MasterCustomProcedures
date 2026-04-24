package master.dataHandling;

import master.PathEnumerationResult;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

public class PathEnumerationResultWriter {

    private static final DateTimeFormatter TIMESTAMP_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH-mm");

    private static final String OUTPUT_DIR_NAME = "output";
    private static final String CSV_HEADER = "nodeId,firstDiscoveredAt\n";

    private String csvComments;

    private final String filePath;

    public PathEnumerationResultWriter(PathEnumerationResult result, String algorithmName, String graphName,
            long hopLimit, long sourceNode, long targetNode)
            throws IOException {
        String outputDir = System.getProperty("user.dir") + File.separator + OUTPUT_DIR_NAME + File.separator
                + String.format("%s-k_%d", graphName, hopLimit);

        File dir = new File(outputDir);
        if (!dir.exists()) {
            dir.mkdirs();
        }

        String timestamp = LocalDateTime.now().format(TIMESTAMP_FORMATTER);
        String fileName = algorithmName + "-k_" + hopLimit + "-" + graphName + "-source_" + sourceNode + "-target_"
                + targetNode + "-" + timestamp + ".csv";
        this.filePath = outputDir + File.separator + fileName;

        csvComments = String.format(
                "#Algorithm: %s\n#GraphName: %s\n#HopLimit: %d\n#PathCount: %d\n#StartTime: %d\n#EndTime: %d\n#TotalTime: %d\n#SourceNode: %d\n#TargetNode: %d\n#NodeCount: %d\n#timedOut: %s\n",
                algorithmName,
                graphName, hopLimit, result.paths.size(), result.startTime, result.endTime,
                result.endTime - result.startTime, sourceNode,
                targetNode,
                result.nodeTimestamps.entrySet().size(), result.timedOut);

        write(result);
    }

    public String getFilePath() {
        return filePath;
    }

    private void write(PathEnumerationResult result) throws IOException {
        try (RandomAccessFile raf = new RandomAccessFile(filePath, "rw")) {
            // Truncate any pre-existing content (e.g. if file already exists)
            raf.setLength(0);

            raf.writeBytes(csvComments);

            raf.writeBytes(CSV_HEADER);

            List<Map.Entry<String, Long>> entries = new ArrayList<>(result.nodeTimestamps.entrySet());
            entries.sort(Comparator.comparingLong(Map.Entry::getValue));

            for (Map.Entry<String, Long> entry : entries) {
                raf.writeBytes(entry.getKey() + "," + entry.getValue() + "\n");
            }
        }
    }
}
