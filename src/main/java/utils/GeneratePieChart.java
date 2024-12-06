package utils;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartUtils;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.labels.StandardPieSectionLabelGenerator;
import org.jfree.chart.plot.PiePlot;
import org.jfree.chart.ui.RectangleInsets;
import org.jfree.data.general.DefaultPieDataset;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class GeneratePieChart {
    private static final String DEFAULT_FILE_PATH = "target/result/app_config_json.txt";
    private static final String DEFAULT_OUTPUT_PATH = "target/charts/";

    public void generateChart() {

        try {
            // Read and parse the JSON file
            List<Map<String, String>> resourceData = readResourceData();

            // Summarize data for all environments
            Map<String, Double> appCpuTotals = new HashMap<>();
            Map<String, Double> appMemoryTotals = new HashMap<>();
            Map<String, Double> appClusterTotals = new HashMap<>();

            aggregateResourceData(resourceData, appCpuTotals, appMemoryTotals,appClusterTotals);

            // Ensure the output directory exists
            createOutputDirectory();

            // Create pie charts for each environment and summary charts for all environments
            createChartsForEnvironments(resourceData);
            createSummaryCharts(appCpuTotals, appMemoryTotals,appClusterTotals);

        } catch (IOException e) {
            System.err.println("Error: " + e.getMessage());
        }
    }

    private static List<Map<String, String>> readResourceData() throws IOException {
        ObjectMapper objectMapper = new ObjectMapper();
        return objectMapper.readValue(new File(GeneratePieChart.DEFAULT_FILE_PATH), new TypeReference<>() {
        });
    }

    private static void aggregateResourceData(List<Map<String, String>> resourceData, Map<String, Double> appCpuTotals, Map<String, Double> appMemoryTotals,Map<String, Double> appClusterTotals) {
        for (Map<String, String> entry : resourceData) {
            String appName = entry.get("app");
            double cpuPercentage = parsePercentage(entry.get("cpuPercentage"));
            double memoryPercentage = parsePercentage(entry.get("memoryPercentage"));
            double clusterPercentage = parsePercentage(entry.get("clusterUsage"));

            appCpuTotals.merge(appName, cpuPercentage, Double::sum);
            appMemoryTotals.merge(appName, memoryPercentage, Double::sum);
            appClusterTotals.merge(appName, clusterPercentage, Double::sum);
        }
    }

    private static void createOutputDirectory() throws IOException {
        Path outputPath = Paths.get(GeneratePieChart.DEFAULT_OUTPUT_PATH);
        if (!Files.exists(outputPath)) {
            Files.createDirectories(outputPath);
        }
    }

    private static void createChartsForEnvironments(List<Map<String, String>> resourceData) {
        Map<String, List<Map<String, String>>> envGroupedData = resourceData.stream()
                .collect(Collectors.groupingBy(entry -> entry.get("env")));

        envGroupedData.forEach((env, envData) -> {
            createPieChartPerEnv(env, "cpuPercentage", "CPU Usage", envData);
            createPieChartPerEnv(env, "memoryPercentage", "Memory Usage", envData);
        });
    }

    private static void createSummaryCharts(Map<String, Double> appCpuTotals, Map<String, Double> appMemoryTotals,Map<String, Double> appClusterTotals) {
        createPieChart("cpuPercentage", "CPU Usage for All Environments", appCpuTotals);
        createPieChart("memoryPercentage", "Memory Usage for All Environments", appMemoryTotals);
        createPieChart("clusterUsage", "Cluster Usage for All Environments", appClusterTotals);
    }

    private static void createPieChart(String key, String chartTitle, Map<String, Double> data) {
        try {
            DefaultPieDataset dataset = new DefaultPieDataset();
            data.forEach(dataset::setValue);
            double total = data.values().stream().mapToDouble(Double::doubleValue).sum();
            JFreeChart chart = ChartFactory.createPieChart(
                    chartTitle + " - Total: " + String.format("%.2f", total) + "%",
                    dataset,
                    true,
                    true,
                    false
            );

            customizePieChart(chart);
            String fileName = key + "_pie_chart_" + "All Environments".replaceAll("[^a-zA-Z0-9]", "_") + ".png";
            ChartUtils.saveChartAsPNG(Paths.get(GeneratePieChart.DEFAULT_OUTPUT_PATH, fileName).toFile(), chart, 1400, 1000);
            System.out.println(chartTitle + " chart for '" + "All Environments" + "' saved at: " + GeneratePieChart.DEFAULT_OUTPUT_PATH);

        } catch (IOException e) {
            System.err.println("Error creating pie chart for '" + "All Environments" + "': " + e.getMessage());
        }
    }


    private static void createPieChartPerEnv(String envOrTitle, String key, String chartTitle, List<Map<String, String>> envData) {
        try {
            DefaultPieDataset dataset = new DefaultPieDataset();
            double total = 0.0; // Variable to hold the total of all apps

            for (Map<String, String> entry : envData) {
                String appName = entry.get("app");
                double percentage = parsePercentage(entry.get(key));
                dataset.setValue(appName + " (" + percentage + "%)", percentage);
                total += percentage; // Add to the total
            }

            // Create and customize the chart
            JFreeChart chart = ChartFactory.createPieChart(
                    chartTitle + " (" + envOrTitle + " Environment) - Total: " + String.format("%.2f", total) + "%",
                    dataset,
                    true,
                    true,
                    false
            );
            customizePieChart(chart);

            // Save the chart as PNG
            String fileName = key + "_pie_chart_" + envOrTitle.replaceAll("[^a-zA-Z0-9]", "_") + ".png";
            ChartUtils.saveChartAsPNG(Paths.get(GeneratePieChart.DEFAULT_OUTPUT_PATH, fileName).toFile(), chart, 1400, 1000);
            System.out.println(chartTitle + " chart for '" + envOrTitle + "' saved at: " + GeneratePieChart.DEFAULT_OUTPUT_PATH);

        } catch (IOException e) {
            System.err.println("Error creating pie chart for environment '" + envOrTitle + "': " + e.getMessage());
        }
    }

    private static double parsePercentage(String percentage) {
        try {
            return Double.parseDouble(percentage.replace("%", "").trim());
        } catch (NumberFormatException e) {
            return 0.0;
        }
    }

    private static void customizePieChart(JFreeChart chart) {
        PiePlot plot = (PiePlot) chart.getPlot();
        plot.setLabelGenerator(new StandardPieSectionLabelGenerator(
                "{0}: {1}%",
                new DecimalFormat("0.00"),
                new DecimalFormat("0.00")
        ));
        plot.setLabelFont(new Font("Arial", Font.BOLD, 12));
        plot.setLabelGap(0.02);
        plot.setSimpleLabels(false);
        plot.setInsets(new RectangleInsets(5.0, 5.0, 5.0, 5.0));
        plot.setLabelPaint(Color.BLACK);
        plot.setMaximumLabelWidth(0.20);
        plot.setInteriorGap(0.01);
        plot.setCircular(true);
    }
}
