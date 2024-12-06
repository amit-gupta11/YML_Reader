package utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

public class DataExtractor {

    static String folderPath = PropertyReader.getProperty("yml.path");
    static String labelsKey = PropertyReader.getProperty("labels.key");
    static String requestsKey = PropertyReader.getProperty("requests.key");
    static String replicasKey = PropertyReader.getProperty("replicas.key");
    static int serialNumber = 1;
    static ObjectMapper yamlMapper = new ObjectMapper(new YAMLFactory());
    static Map<String, List<Map<String, String>>> appData = new HashMap<>();
    private static final List<String> outputStrings = new ArrayList<>();

    public static File[] fileSetup() {
        File folder = new File(folderPath);
        File[] yamlFiles = folder.listFiles((dir, name) -> name.endsWith(".yaml") || name.endsWith(".yml"));

        if (yamlFiles == null || yamlFiles.length == 0) {
            System.out.println("No YAML files found in the specified folder.");
        }
        return yamlFiles;
    }

    public static Object extractData(Map<String, Object> yamlData, String keyPath) {
        try {
            for (String key : keyPath.split("\\.")) {
                if (key.contains("[")) {
                    String listKey = key.substring(0, key.indexOf("["));
                    int index = Integer.parseInt(key.substring(key.indexOf("[") + 1, key.indexOf("]")));
                    yamlData = (Map<String, Object>) ((List<?>) yamlData.get(listKey)).get(index);
                } else {
                    Object value = yamlData.get(key);
                    if (value instanceof Map) yamlData = (Map<String, Object>) value;
                    else return value;
                }
            }
            return yamlData;
        } catch (Exception e) {
            System.err.println("Error extracting data for key path: " + keyPath);
            e.printStackTrace();
            return null;
        }
    }

    public static Object getKeyValue(Map<String, Object> requestData, String key) {
        return extractData(requestData, key);
    }

    public static double calculateClusterUsage(double cores) {
        int totalCore = Integer.parseInt(PropertyReader.getProperty("rancher.cores"));
        return (cores / totalCore ) * 100;
    }

    public static double calculateCoreUsage(String cpuValue, int replicas) {
        if (cpuValue == null) cpuValue = "0";
        double cpuInCores = cpuValue.endsWith("m")
                ? Double.parseDouble(cpuValue.replace("m", "")) * replicas / 1000
                : Integer.parseInt(cpuValue) * replicas;
        return cpuInCores;
    }

    public static String getCPUValue(Map<String, Object> request) {
        Object value = extractData(request, "cpu");
        try {
            if (value instanceof String && !((String) value).endsWith("m")) {
                return Integer.parseInt((String) value) * 1000 + "m";
            } else if (value instanceof Integer) {
                return ((Integer) value * 1000) + "m";
            }
        } catch (NumberFormatException e) {
            System.err.println("Invalid CPU value: " + value);
        }
        return value != null ? value.toString() : "0m";
    }

    public static void getAppConfig() {
        String totalCPU = PropertyReader.getProperty("rancher.cores");
        String totalMemory = PropertyReader.getProperty("rancher.memory");
        for (File yamlFile : fileSetup()) {
            System.out.println("Processing file " + serialNumber + " : " + yamlFile.getName());
            serialNumber++;
            try {
                Map<String, Object> yamlData = yamlMapper.readValue(yamlFile, Map.class);
                Object labels = extractData(yamlData, labelsKey);
                Object request = extractData(yamlData, requestsKey);
                Object replicas = extractData(yamlData, replicasKey);
                Object cpuUsage = getKeyValue((Map<String, Object>) request, "cpu");

                String appName = (String) getKeyValue((Map<String, Object>) labels, "app");
                String env = (String) getKeyValue((Map<String, Object>) labels, "environment");
                String cpu = getCPUValue((Map<String, Object>) request);
                String tempMemory = (String) getKeyValue((Map<String, Object>) request, "memory");
                String memory = convertMemoryToGi(tempMemory);
                double cores= calculateCoreUsage((String) cpuUsage, (Integer) replicas);
                String clusterUsage =  String.format("%.1f", calculateClusterUsage(cores)) + "%";

                String cpuPercentage = String.format("%.2f", getPercentage(cpu, totalCPU)) + "%";
                String memoryPercentage = String.format("%.2f", getPercentage(memory, totalMemory)) + "%";
                String formattedString = String.format("{\"app\" : \"%s\",\"env\" : \"%s\",\"cpu\" : \"%s\",\"memory\" : \"%s\",\"cpuPercentage\" : \"%s\",\"memoryPercentage\" : \"%s\",\"cores\" : \"%s\",\"clusterUsage\" : \"%s\"}",
                        appName, env, cpu, memory, cpuPercentage, memoryPercentage,cores,clusterUsage);

                outputStrings.add(formattedString);

                System.out.printf("App Name: %s, Environment Name: %s, CPU Requested: %s, Memory Requested: %s%n", appName, env, cpu, memory);
                System.out.printf("Replicas: %s%n", replicas);
                System.out.printf("Cores : %s%n", calculateCoreUsage((String) cpuUsage, (Integer) replicas));
                System.out.printf("Cluster : %s%n", clusterUsage);
                System.out.printf("CPU usage : %s%n", cpuPercentage);
                System.out.printf("Memory usage : %s%n", memoryPercentage);
                System.out.println("-----------------------------------------------------------------------------------------");


            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
        ReportWriter.createDataFile(outputStrings);
        ReportWriter.jsonToCsv();

    }

    public static void getDuplicateUsage() {
        for (File yamlFile : fileSetup()) {
            serialNumber++;
            try {
                Map<String, Object> yamlData = yamlMapper.readValue(yamlFile, Map.class);
                Object labels = extractData(yamlData, labelsKey);
                Object request = extractData(yamlData, requestsKey);
                String appName = (String) getKeyValue((Map<String, Object>) labels, "app");
                String env = (String) getKeyValue((Map<String, Object>) labels, "environment");
                String cpu = getCPUValue((Map<String, Object>) request);
                String memory = (String) getKeyValue((Map<String, Object>) request, "memory");

                Map<String, String> appAttributes = new HashMap<>();
                appAttributes.put("env", env);
                appAttributes.put("cpu", cpu);
                appAttributes.put("memory", memory);
                appData.computeIfAbsent(appName, k -> new ArrayList<>()).add(appAttributes);

            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }

        String filePath = PropertyReader.getProperty("output.path");
        File outputFile = new File(filePath);
        outputFile.getParentFile().mkdirs();

        try (FileWriter writer = new FileWriter(filePath)) {

            appData.forEach((appName, attributesList) -> {
                Map<String, Set<String>> envByCpuMemory = new HashMap<>();
                attributesList.forEach(attr -> envByCpuMemory
                        .computeIfAbsent(attr.get("cpu") + "," + attr.get("memory"), k -> new HashSet<>())
                        .add(attr.get("env")));

                if (envByCpuMemory.size() == 1) {
                    // All environments have the same CPU and memory
                    String commonCpuMemKey = envByCpuMemory.keySet().iterator().next();
                    String output = "This App " + appName + " has the same CPU: " + commonCpuMemKey.split(",")[0] +
                            " & Memory: " + commonCpuMemKey.split(",")[1] +
                            " for all environments: " + String.join(", ", envByCpuMemory.get(commonCpuMemKey)) + "\n" +
                            "-----------------------------------------------------------------------------------------\n";
                    try {
                        writer.write(output);
                        System.out.print(output);
                    } catch (IOException e) {
                        System.err.println("An error occurred while writing to the file: " + e.getMessage());
                    }
                }
            });

            System.out.println("Data successfully saved to the file at: " + filePath);

        } catch (IOException e) {
            throw new RuntimeException("An error occurred while opening or writing to the file: " + e.getMessage());
        }
    }


    public static Double getPercentage(String value, String totalValue) {
        double modifiedValue = Double.parseDouble(value.replace("m", "").replace("Gi", ""));
        if (value.endsWith("m")) {
            return (modifiedValue / (Double.parseDouble(totalValue) * 1000)) * 100;
        }
        return (modifiedValue / Double.parseDouble(totalValue)) * 100;
    }


    public static String convertMemoryToGi(String value) {
        if (value.endsWith("Mi")) {
            int modifiedValue = Integer.parseInt(value.replace("Mi", ""));
            Double output = modifiedValue / 1024.0;
            return output + "Gi";
        }
        return value;
    }
}
