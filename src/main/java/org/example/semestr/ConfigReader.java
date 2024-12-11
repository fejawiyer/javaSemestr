package org.example.semestr;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class ConfigReader {

    private final String host;
    private final int port;

    private static ConfigReader reader;

    private ConfigReader() {
        Properties properties = new Properties();
        InputStream input = this.getClass()
                .getClassLoader()
                .getResourceAsStream("application.properties");
        try {
            properties.load(input);

            host = properties.getProperty("server.host");
            port = Integer.parseInt(properties.getProperty("server.port"));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public String getHost() {
        return host;
    }

    public int getPort() {
        return port;
    }

    public static ConfigReader getInstance() {
        if (reader == null)
            reader = new ConfigReader();

        return reader;
    }
}