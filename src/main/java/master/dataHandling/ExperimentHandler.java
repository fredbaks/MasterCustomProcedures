package master.dataHandling;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.RandomAccessFile;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Scanner;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import org.neo4j.driver.Driver;
import org.neo4j.driver.Record;

import com.carrotsearch.hppc.BitSet;

import master.neo4j.CypherConnector;
import master.neo4j.Neo4jConnector;

public class ExperimentHandler {

    private String projectionName;
    private int hoplimit;

    private static final String OUTPUT_DIR_NAME = "source-target-pairs";
    private static final String OUTPUT_DIR = System.getProperty("user.dir") + File.separator + OUTPUT_DIR_NAME;

    private static final String[] ALGORITHMS = { "cdfs", "bcdfs", "joinbcdfs", "pathenum" };
    private static final Integer[] K_VALUES = { 3, 4, 5 };
    private static final String[] DATASETS = { "bio-grid-yeast", "com-amazon", "reactome" };

    private final int SOURCE_TARGET_PAIRS = 1000;

    private int TASK_COUNT;

    private Driver driver;

    private ArrayList<ArrayList<Long>> sourceTargetPairs;

    public static void main(String[] args) {
        if (args.length == 0) {
            System.err.println("Creates random pairs of source target on given projection.");
            System.exit(1);
        }

        if (args[0].equals("single")) {
            String datasetName = args[1];
            int hopLimit = Integer.parseInt(args[2]);
            boolean isDataSetLoaded = Boolean.parseBoolean(args[3]);

            try (Driver driver = Neo4jConnector.createDriver()) {
                new ExperimentHandler(driver).runExperiment(datasetName, hopLimit, isDataSetLoaded);
            }
        } else if (args[0].equals("multiple")) {
            ArrayList<String> datasets = new ArrayList<String>();

            if (args.length == 1) {
                for (String dataset : DATASETS) {
                    datasets.add(dataset);
                }

            } else {
                for (String arg : args) {
                    if (arg.equals("multiple")) {
                        continue;
                    }

                    datasets.add(arg);
                }
            }

            boolean isDataSetLoaded = false;

            for (String dataset : datasets) {
                isDataSetLoaded = false;
                for (Integer k : K_VALUES) {
                    try (Driver driver = Neo4jConnector.createDriver()) {
                        new ExperimentHandler(driver).runExperiment(dataset, k, isDataSetLoaded);
                    }
                    isDataSetLoaded = true;
                }
            }
        }
    }

    private ExperimentHandler(Driver driver) {
        this.driver = driver;
    };

    private void printProgressBar(int completed, int total) {
        int barWidth = 50;
        double fraction = (double) completed / total;
        int filled = (int) (fraction * barWidth);
        StringBuilder bar = new StringBuilder("\r[");
        for (int i = 0; i < barWidth; i++) {
            bar.append(i < filled ? '#' : ' ');
        }
        int percent = (int) (fraction * 100);
        bar.append(String.format("] %3d%% (%d/%d)", percent, completed, total));
        System.out.print(bar);
        if (completed == total) {
            System.out.println();
        }
    }

    public boolean writeRandomPairs() {

        HashSet<ArrayList<Long>> sourceTargetPairs = new HashSet<ArrayList<Long>>();

        int nodeCount = 0;

        Record record = CypherConnector.runQuery(driver, "MATCH (a) RETURN COUNT(a) as nodeCount", true).get(0);
        nodeCount = record.get("nodeCount").asInt();

        BitSet expendedSources = new BitSet();

        Random random = new Random();

        System.out.println("Finding source target pairs");
        printProgressBar(0, SOURCE_TARGET_PAIRS);

        while (sourceTargetPairs.size() < SOURCE_TARGET_PAIRS) {
            printProgressBar(sourceTargetPairs.size(), SOURCE_TARGET_PAIRS);
            long source = random.nextLong(1, nodeCount);

            if (expendedSources.get(source)) {
                continue;
            }

            try {
                String queryString = String.format(
                        "MATCH (source {id:'%d'}) CALL master.bfstree('%s', {sourceNode: source, k: %d}) YIELD result RETURN result",
                        source, projectionName, hoplimit);

                Record dfsRecord = CypherConnector.runQuery(driver, queryString, true).get(0);

                Map<String, Long> dfsResult = dfsRecord.get("result").asMap(v -> v.asLong());

                List<Long> resultNodes = dfsResult.entrySet().stream().filter(e -> e.getValue() == hoplimit)
                        .map(Map.Entry::getKey).map(Long::parseLong).collect(java.util.stream.Collectors.toList());
                java.util.Collections.shuffle(resultNodes, random);

                if (resultNodes.isEmpty()) {
                    continue;
                }

                boolean added = false;
                for (Long nodeId : resultNodes) {
                    Long target = CypherConnector.getNodeIdProperty(driver, nodeId);

                    ArrayList<Long> pair = new ArrayList<Long>();
                    pair.add(source);
                    pair.add(target);

                    if (!sourceTargetPairs.contains(pair)) {
                        sourceTargetPairs.add(pair);
                        added = true;
                        break;
                    }
                }

                if (!added) {
                    expendedSources.set(source);
                }

                if (expendedSources.cardinality() == nodeCount) {
                    return false;
                }
            } catch (Exception e) {
                System.out.println("Something went wrong: " + e.getMessage());
                e.printStackTrace();
                continue;
            }
        }

        File dir = new File(OUTPUT_DIR);
        if (!dir.exists()) {
            dir.mkdirs();
        }

        String filePath = getFilePath(projectionName, hoplimit);

        try (RandomAccessFile raf = new RandomAccessFile(filePath, "rw")) {
            raf.setLength(0);

            for (ArrayList<Long> pair : sourceTargetPairs) {
                raf.writeBytes(pair.get(0) + "," + pair.get(1) + "\n");
            }
        } catch (Exception e) {
            System.err.println(
                    "Something went wrong while writing pairs to file: " + e.getMessage() + "\n" + e.getStackTrace());
            return false;
        }

        return true;
    }

    private static String getFilePath(String projectionName, int hoplimit) {
        String fileName = projectionName + "-k_" + hoplimit + ".csv";
        String filePath = OUTPUT_DIR + File.separator + fileName;
        return filePath;
    }

    public boolean loadSourceTargetPairs() {

        String filePath = getFilePath(projectionName, hoplimit);

        Path pairPath = Paths.get(filePath);
        if (!Files.exists(pairPath)) {
            System.out.println("Found no file with path: " + pairPath + ", creating from stratch");

            boolean wasFound = writeRandomPairs();

            if (!wasFound) {
                return false;
            }
        }

        sourceTargetPairs = new ArrayList<ArrayList<Long>>();

        try (Scanner scanner = new Scanner(new File(filePath))) {
            while (scanner.hasNextLine()) {
                String[] line = scanner.nextLine().split(",");
                ArrayList<Long> pair = new ArrayList<Long>();
                pair.add(Long.parseLong(line[0]));
                pair.add(Long.parseLong(line[1]));
                sourceTargetPairs.add(pair);
            }
        } catch (FileNotFoundException e) {
            System.out.println("File not created by writeRandomPairs: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
        return true;
    }

    public void runExperiment(String dataset, int hopLimit, boolean isDataSetLoaded) {

        this.projectionName = dataset;
        this.hoplimit = hopLimit;

        if (!isDataSetLoaded) {
            System.out.println("Loading dataset " + dataset);
            try {
                CsvLoader.loadDataset(driver, dataset);
            } catch (IllegalArgumentException e) {
                System.err.print(e.getMessage());
                System.exit(1);
            }
        } else {
            System.out.println("Dataset is already loaded, skipping");
        }

        System.out.println("Creating cypher projection");
        CypherConnector.createProjection(driver, dataset, true);

        System.out.println("Reading source pair data");
        boolean wasLoaded = loadSourceTargetPairs();

        if (!wasLoaded) {
            System.out.println("Not enough source-target pairs were found");
            return;
        }

        TASK_COUNT = ALGORITHMS.length * sourceTargetPairs.size();

        AtomicInteger completedTasks = new AtomicInteger(0);

        System.out.println("Running queries");
        printProgressBar(0, TASK_COUNT);

        List<Callable<Void>> tasks = new ArrayList<Callable<Void>>();

        for (String algorithm : ALGORITHMS) {
            for (ArrayList<Long> pair : sourceTargetPairs) {
                Callable<Void> task = new Callable<Void>() {
                    @Override
                    public Void call() throws Exception {
                        CypherConnector.runPathEnumeration(driver, pair.get(0), pair.get(1), algorithm, dataset,
                                hopLimit);
                        printProgressBar(completedTasks.incrementAndGet(), TASK_COUNT);
                        return null;
                    }
                };

                tasks.add(task);

            }
        }

        ExecutorService executor = Executors.newFixedThreadPool(8);

        try {
            executor.invokeAll(tasks);
        } catch (InterruptedException e) {
            System.err.println("Something happened during execution of algorithms: " + e.getMessage());
            e.printStackTrace();
        } finally {
            executor.shutdown();
        }

    }
}
