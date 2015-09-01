package com.libon.jdbc;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Objects;
import java.util.Optional;
import java.util.Properties;
import java.util.logging.ConsoleHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;
import com.google.common.base.Stopwatch;

public class SimpleJdbcCheck {


    public static void main(String[] args) throws Exception {
        if(args.length != 1) throw new IllegalArgumentException("property file path required");
        Properties properties = properties(args[0]);
        enableOracleDiagnosability(properties);
        testConnection(properties);
    }

    private static void enableOracleDiagnosability(Properties properties) {
        // Diagnosability OJDBC http://docs.oracle.com/cd/B28359_01/java.111/b31224/diagnose.htm
        if(!Objects.equals(properties.getProperty("diagnosability"), "true")) return;

        System.setProperty("oracle.jdbc.Trace", "true");

        // Load a properties file from class path that way can't be achieved with java.util.logging.config.file
        /*
        final LogManager logManager = LogManager.getLogManager();
        try (final InputStream is = getClass().getResourceAsStream("/logging.properties")) {
            logManager.readConfiguration(is);
        }
        */

        // Programmatic configuration
        System.setProperty("java.util.logging.SimpleFormatter.format",
                           "%1$tY-%1$tm-%1$td %1$tH:%1$tM:%1$tS.%1$tL %4$-7s [%3$s] (%2$s) %5$s %6$s%n");

        /*
        .level=SEVERE
        oracle.jdbc.level=INFO
        oracle.jdbc.handlers=java.util.logging.ConsoleHandler
        java.util.logging.ConsoleHandler.level=INFO
        java.util.logging.ConsoleHandler.formatter=java.util.logging.SimpleFormatter
        */
        Logger.getGlobal().setLevel(Level.SEVERE);

        final ConsoleHandler consoleHandler = new ConsoleHandler();
        consoleHandler.setLevel(Level.FINEST);
        consoleHandler.setFormatter(new SimpleFormatter());

        final Logger app = Logger.getLogger("oracle.jdbc");
        app.setLevel(Level.FINEST);
        app.addHandler(consoleHandler);

    }

    private static void testConnection(Properties properties) throws ClassNotFoundException {
        // Connect via TNSNAMES.ora http://docs.oracle.com/cd/B19306_01/java.102/b14355/urls.htm#BEIDIJCE
        // TNSNAMES http://docs.oracle.com/cd/B28359_01/network.111/b28317/tnsnames.htm
        Optional.ofNullable(properties.getProperty("oracle.net.tns_admin"))
                .ifPresent(v -> System.setProperty("oracle.net.tns_admin", v));
        String dbURL = Optional.ofNullable(properties.getProperty("url"))
                               .orElseThrow(() -> new IllegalArgumentException("missing url property"));

        Class.forName("oracle.jdbc.OracleDriver");

        Connection conn = null;
        Statement stmt = null;

        try {
            conn = establishConnection(properties, dbURL);

            stmt = conn.createStatement();

            Stopwatch stmtWatch = Stopwatch.createStarted();
            ResultSet rs = stmt.executeQuery(properties.getProperty("query"));
            System.out.println("Executed statement took : " + stmtWatch.stop());

            if (rs.next()) {
                System.out.println(rs.getString(1));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        finally {
            if (stmt != null) try { stmt.close(); } catch (Exception e) {}
            if (conn != null) try { conn.close(); } catch (Exception e) {}
        }
    }

    private static Connection establishConnection(Properties properties, String dbURL) throws SQLException {
        Stopwatch connWatch = Stopwatch.createStarted();
        Connection conn = DriverManager.getConnection(dbURL, properties.getProperty("user"), properties.getProperty("password"));
        System.out.println("Connection established took : " + connWatch.stop());
        return conn;
    }

    private static Properties properties(String propertyFilePath) {
        try(FileInputStream fileInput = new FileInputStream(new File(propertyFilePath))) {
            Properties properties = new Properties();
            properties.load(fileInput);
            properties.entrySet()
                      .forEach(entry -> System.out.println(entry.getKey() + ": " + entry.getValue()));

            return properties;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
