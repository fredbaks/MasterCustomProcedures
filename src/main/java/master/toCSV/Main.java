package master.toCSV;

public class Main {

    public static void main(String[] args) {
        if (args.length < 1) {
            System.out.println("Usage: java Main <filePath>");
            System.out.println("Example: java Main /path/to/file.edges");
            return;
        }

        String filePath = args[0];
        String[] filePathArray = filePath.split("\\.");
        String fileType = filePathArray[filePathArray.length - 1];

        System.out.println("Converting file: " + filePath);
        new FileToCsv(filePath, fileType);
        System.out.println("Done!");
    }
}