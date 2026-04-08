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

    private static final DateTimeFormatter TIMESTAMP_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH-mm-ss");

    private static final String OUTPUT_DIR_NAME = "output";
    private static final String CSV_HEADER = "nodeId,firstDiscoveredAt\n";

    private String csvComments;

    private final String filePath;

    public PathEnumerationResultWriter(PathEnumerationResult result, String algorithmName, String graphName,
            long hopLimit, long startTime, long endTime, long sourceNode, long targetNode)
            throws IOException {
        String outputDir = System.getProperty("user.dir") + File.separator + OUTPUT_DIR_NAME + File.separator
                + String.format("%s-source_%d-target_%d", graphName, sourceNode, targetNode);

        File dir = new File(outputDir);
        if (!dir.exists()) {
            dir.mkdirs();
        }

        String timestamp = LocalDateTime.now().format(TIMESTAMP_FORMATTER);
        String fileName = algorithmName + "-" + hopLimit + "-" + graphName + "-" + timestamp + ".csv";
        this.filePath = outputDir + File.separator + fileName;

        csvComments = String.format(
                "#Algorithm: %s\n#GraphName: %s\n#HopLimit: %d\n#StartTime: %d\n#EndTime: %d\n#SourceNode: %d\n#TargetNode: %d\n#NodeCount: %d\n",
                algorithmName,
                graphName, hopLimit, startTime, endTime, sourceNode, targetNode,
                result.nodeTimestamps.entrySet().size());

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
