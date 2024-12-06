package utils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.*;
import java.util.Iterator;
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
    public static void jsonToCsv(){
        String inputFile = "target/result/app_config_json.txt"; // input .txt file containing JSON data
        String outputFile = "target/result/app_config_json.csv"; // output .csv file
        try {
            String jsonData = readFile(inputFile);
            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode jsonArray = objectMapper.readTree(jsonData);
            writeCsv(jsonArray, outputFile);
            System.out.println("Conversion complete. Output saved to " + outputFile);
        } catch(IOException e) {
            e.printStackTrace();
        }
    }

    private static String readFile(String fileName) throws IOException {
        StringBuilder content = new StringBuilder();
        try (BufferedReader br = new BufferedReader(new FileReader(fileName))) {
            String line;
            while ((line = br.readLine()) != null) {
                content.append(line);
            }
        }
        return content.toString();
    }

    private static void writeCsv(JsonNode jsonArray, String outputFile) throws IOException {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(outputFile))) {
            Iterator<String> fieldNames = jsonArray.get(0).fieldNames();
            StringBuilder header = new StringBuilder();
            while (fieldNames.hasNext()) {
                if (header.length() > 0) {
                    header.append(",");
                }
                header.append(fieldNames.next());
            }
            writer.write(header.toString());
            writer.newLine();
            for (JsonNode jsonNode : jsonArray) {
                StringBuilder row = new StringBuilder();
                Iterator<String> fields = jsonNode.fieldNames();
                while (fields.hasNext()) {
                    String fieldName = fields.next();
                    if (row.length() > 0) {
                        row.append(",");
                    }
                    row.append(jsonNode.get(fieldName).asText());
                }
                writer.write(row.toString());
                writer.newLine();
            }
        }
    }
}
