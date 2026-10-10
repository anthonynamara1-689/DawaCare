package com.mycompany.dawacare.util;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

public class DBConnection {
    private static final Properties props = new Properties();

    static {
        // 1. A db.properties in the current folder (next to the jar) wins.
        // 2. Otherwise use the one inside src/main/resources.
        Path external = Path.of("db.properties");
        try (InputStream in = Files.exists(external)
                ? Files.newInputStream(external)
                : DBConnection.class.getResourceAsStream("/db.properties")) {
            if (in == null) {
                throw new IllegalStateException("db.properties not found. Copy db.properties.example "
                        + "to db.properties and fill in your MySQL details.");
            }
            props.load(in);
        } catch (IOException e) {
            throw new RuntimeException("Cannot load db.properties", e);
        }
    }

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(
            props.getProperty("db.url"),
            props.getProperty("db.user"),
            props.getProperty("db.password"));
    }
}