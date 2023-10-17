package com.hillayes.executors.scheduler.helpers;

import org.hsqldb.jdbc.JDBCDataSource;

import javax.sql.DataSource;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.function.Consumer;

public class TestDatasource {
    public static DataSource initDatabase() {
        JDBCDataSource dataSource = new JDBCDataSource();

        dataSource.setURL("jdbc:hsqldb:mem:schedule_testing");
        dataSource.setUser("sa");
        dataSource.setPassword("");

        doWithConnection(dataSource, c -> {
            try (Statement statement = c.createStatement()) {
                String createTables = readFile("/hsql_tables.sql");
                statement.execute(createTables);
            } catch (Exception e) {
                throw new RuntimeException("Failed to create tables", e);
            }
        });

        return dataSource;
    }

    private static String readFile(String resource) throws IOException {
        try (BufferedReader in = new BufferedReader(new InputStreamReader(TestDatasource.class.getResourceAsStream(resource)))) {
            String line;
            StringBuilder createTables = new StringBuilder();
            while ((line = in.readLine()) != null) {
                createTables.append(line);
            }
            return createTables.toString();
        }
    }

    private static void doWithConnection(DataSource dataSource, Consumer<Connection> consumer) {
        try (Connection connection = dataSource.getConnection()) {
            consumer.accept(connection);
        } catch (SQLException e) {
            throw new RuntimeException("Error getting connection from datasource.", e);
        }
    }
}
