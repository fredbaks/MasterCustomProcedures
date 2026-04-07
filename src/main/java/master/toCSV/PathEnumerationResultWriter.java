package master.toCSV;

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

/**
 * Writes the output of any {@link PathEnumerationResult} (CDfs, BCDfs,
 * JoinBCDfs, PathEnum) to a CSV file using {@link RandomAccessFile}.
 *
 * <p>
 * The file is created in the {@code output/} directory at the project root.
 * The filename follows the format:
 * {@code <algorithmName>-<graphName>-<timestamp>.csv}
 * where the timestamp is formatted as {@code yyyy-MM-dd'T'HH-mm-ss} (colons
 * replaced with dashes for Windows filesystem compatibility).
 *
 * <p>
 * The CSV contains one row per unique node first discovered during the
 * traversal, sorted by discovery timestamp ascending:
 *
 * <pre>
 * nodeId,firstDiscoveredAt
 * 42,1742906600123456789
 * 17,1742906600456789123
 * </pre>
 *
 * <p>
 * Usage:
 *
 * <pre>{@code
 * new CDfsResultWriter(cdfsResult, "CDFS", "myGraph");
 * new CDfsResultWriter(bcdfsResult, "BCDFS", "myGraph");
 * new CDfsResultWriter(joinResult, "JOINBCDFS", "myGraph");
 * new CDfsResultWriter(pathEnumResult, "PATHENUM", "myGraph");
 * }</pre>
 */
public class PathEnumerationResultWriter {

    private static final DateTimeFormatter TIMESTAMP_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH-mm-ss");

    private static final String OUTPUT_DIR_NAME = "output";
    private static final String CSV_HEADER = "nodeId,firstDiscoveredAt\n";

    private final String filePath;

    /**
     * Creates a {@code CDfsResultWriter}, immediately writes the result to disk,
     * and closes the file.
     *
     * @param result        the {@link PathEnumerationResult} to write
     * @param algorithmName prefix used in the filename (e.g. {@code "CDFS"})
     * @param graphName     the graph projection name, used in the output filename
     * @throws IOException if the file cannot be created or written
     */
    public PathEnumerationResultWriter(PathEnumerationResult result, String algorithmName, String graphName,
            int hopLimit)
            throws IOException {
        String outputDir = System.getProperty("user.dir") + File.separator + OUTPUT_DIR_NAME;

        File dir = new File(outputDir);
        if (!dir.exists()) {
            dir.mkdirs();
        }

        String timestamp = LocalDateTime.now().format(TIMESTAMP_FORMATTER);
        String fileName = algorithmName + "-" + hopLimit + "-" + graphName + "-" + timestamp + ".csv";
        this.filePath = outputDir + File.separator + fileName;

        write(result);
    }

    /**
     * Returns the absolute path of the file that was written.
     *
     * @return absolute path string
     */
    public String getFilePath() {
        return filePath;
    }

    private void write(PathEnumerationResult result) throws IOException {
        try (RandomAccessFile raf = new RandomAccessFile(filePath, "rw")) {
            // Truncate any pre-existing content (e.g. if file already exists)
            raf.setLength(0);

            raf.writeBytes(CSV_HEADER);

            List<Map.Entry<String, Long>> entries = new ArrayList<>(result.nodeTimestamps.entrySet());
            entries.sort(Comparator.comparingLong(Map.Entry::getValue));

            for (Map.Entry<String, Long> entry : entries) {
                raf.writeBytes(entry.getKey() + "," + entry.getValue() + "\n");
            }
        }
    }
}
