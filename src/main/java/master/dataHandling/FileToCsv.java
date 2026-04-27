package master.dataHandling;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

public class FileToCsv {

    public static void main(String[] args) {
        if (args.length < 1) {
            System.out.println("A path is required.");
            return;
        }

        String filePath = args[0];
        String[] filePathArray = filePath.split("\\.");
        String fileType = filePathArray[filePathArray.length - 1];

        System.out.println("Converting file: " + filePath);
        new FileToCsv(filePath, fileType);
        System.out.println("Done!");
    }

    private String filePath;
    private String fileName;

    private String outputPath = "CSV\\";

    private int BUFFER_SIZE = 1024;

    public FileToCsv(String filePath, String fileType) {
        System.out.println(filePath + ", " + fileType);
        this.filePath = filePath;

        String[] filePathArray = filePath.split("\\\\");
        filePathArray = filePathArray[filePathArray.length - 1].split("\\.");
        this.fileName = filePathArray[filePathArray.length - 2];
        System.out.println(this.fileName);

        Path path = Paths.get(outputPath + fileName + ".csv");

        File outputDir = new File(outputPath);
        if (!outputDir.exists()) {
            outputDir.mkdirs();
        }

        try (FileChannel channel = FileChannel.open(path, StandardOpenOption.TRUNCATE_EXISTING,
                StandardOpenOption.WRITE, StandardOpenOption.CREATE)) {
            if (fileType.equals("edges")) {
                EdgesFileToCsv(channel);
            } else if (fileType.equals("txt")) {
                TxtEdgesFileToCsv(channel);
            }
        } catch (IOException e) {
            System.out.println("An error occured");
            e.printStackTrace();
        }
    }

    private void EdgesFileToCsv(FileChannel writeChannel) {

        Path path = Paths.get(filePath);

        try (FileChannel readChannel = FileChannel.open(path, StandardOpenOption.READ)) {

            ByteBuffer readBuffer = ByteBuffer.allocate(BUFFER_SIZE);
            StringBuilder leftover = new StringBuilder();
            boolean headerWritten = false;

            while (readChannel.read(readBuffer) > 0) {
                readBuffer.flip();
                String chunk = new String(readBuffer.array(), 0, readBuffer.limit(), "UTF-8");
                readBuffer.clear();

                chunk = leftover + chunk;
                leftover.setLength(0);

                String[] lines = chunk.split("\\r?\\n", -1);

                leftover.append(lines[lines.length - 1]);

                if (!headerWritten) {
                    String header = "START_ID,END_ID,TYPE\n";
                    writeChannel.write(ByteBuffer.wrap(header.getBytes("UTF-8")));
                    headerWritten = true;
                }

                StringBuilder csvChunk = new StringBuilder();
                for (int i = 0; i < lines.length - 1; i++) {
                    String line = lines[i].trim();
                    if (line.startsWith("%") || line.isEmpty()) {
                        continue;
                    }
                    String[] parts = line.split(",");
                    if (parts.length == 2) {
                        csvChunk.append(parts[0].trim())
                                .append(",")
                                .append(parts[1].trim())
                                .append(",EDGE\n");
                    }
                }

                if (csvChunk.length() > 0) {
                    writeChannel.write(ByteBuffer.wrap(csvChunk.toString().getBytes("UTF-8")));
                }
            }

            String remaining = leftover.toString().trim();
            if (!remaining.isEmpty() && !remaining.startsWith("%")) {
                String[] parts = remaining.split(",");
                if (parts.length == 2) {
                    String lastLine = parts[0].trim() + "," + parts[1].trim() + ",EDGE\n";
                    writeChannel.write(ByteBuffer.wrap(lastLine.getBytes("UTF-8")));
                }
            }

        } catch (IOException e) {
            System.out.println("An error occurred.");
            e.printStackTrace();
        }
    }

    private void TxtEdgesFileToCsv(FileChannel writeChannel) {

        Path path = Paths.get(filePath);

        try (FileChannel readChannel = FileChannel.open(path, StandardOpenOption.READ)) {

            ByteBuffer readBuffer = ByteBuffer.allocate(BUFFER_SIZE);
            StringBuilder leftover = new StringBuilder();
            boolean headerWritten = false;

            while (readChannel.read(readBuffer) > 0) {
                readBuffer.flip();
                String chunk = new String(readBuffer.array(), 0, readBuffer.limit(), "UTF-8");
                readBuffer.clear();

                chunk = leftover + chunk;
                leftover.setLength(0);

                String[] lines = chunk.split("\\r?\\n", -1);

                leftover.append(lines[lines.length - 1]);

                if (!headerWritten) {
                    String header = "START_ID,END_ID,TYPE\n";
                    writeChannel.write(ByteBuffer.wrap(header.getBytes("UTF-8")));
                    headerWritten = true;
                }

                StringBuilder csvChunk = new StringBuilder();
                for (int i = 0; i < lines.length - 1; i++) {
                    String line = lines[i].trim();
                    if (line.startsWith("#") || line.isEmpty()) {
                        continue;
                    }
                    String[] parts = line.split("\\t");
                    if (parts.length == 2) {
                        csvChunk.append(parts[0].trim())
                                .append(",")
                                .append(parts[1].trim())
                                .append(",EDGE\n");
                    }
                }

                if (csvChunk.length() > 0) {
                    writeChannel.write(ByteBuffer.wrap(csvChunk.toString().getBytes("UTF-8")));
                }
            }

            String remaining = leftover.toString().trim();
            if (!remaining.isEmpty() && !remaining.startsWith("#")) {
                String[] parts = remaining.split("\\t");
                if (parts.length == 2) {
                    String lastLine = parts[0].trim() + "," + parts[1].trim() + ",EDGE\n";
                    writeChannel.write(ByteBuffer.wrap(lastLine.getBytes("UTF-8")));
                }
            }

        } catch (IOException e) {
            System.out.println("An error occurred.");
            e.printStackTrace();
        }
    }
}
