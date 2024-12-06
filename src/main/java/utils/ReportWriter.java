package utils;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

public class ReportWriter {
    public static void createDataFile(List<String> outputStrings) {
        String dataFile = "target/result/app_config_json.txt";
        File outputFile = new File(dataFile);
        outputFile.getParentFile().mkdirs();

        try (FileWriter writer = new FileWriter(outputFile)) {
            writer.write("[");
            int size = outputStrings.size();
            for (int i = 0; i < size; i++) {
                writer.write(outputStrings.get(i)); // Write the line
                if (i < size - 1) { // Add a comma and newline if not the last line
                    writer.write("," + System.lineSeparator());
                }
            }
            writer.write("]");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
