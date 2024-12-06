package utils;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

public class PropertyReader {
    private static final Properties properties;
    static {
        properties = new Properties();
        try (FileInputStream fileInput = new FileInputStream("src/main/resources/app.properties")) {
            properties.load(fileInput);
        } catch (IOException e) {
            System.err.println("Failed to load properties file: " + e.getMessage());
        }
    }

    public static String getProperty(String key) {
        return properties.getProperty(key);
    }

}
