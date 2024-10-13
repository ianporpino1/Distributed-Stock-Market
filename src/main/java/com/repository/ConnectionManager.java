package com.repository;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class ConnectionManager {
    private final String url = "jdbc:postgresql://localhost:5433/matching_orders";
    private final String user = "postgres";
    private final String password = "postgres";
    private final List<Connection> connectionPool = new ArrayList<>();
    private final int MAX_CONNECTIONS = 10;

    public ConnectionManager() throws SQLException {
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        for (int i = 0; i < MAX_CONNECTIONS; i++) {
            connectionPool.add(createConnection());
        }
    }

    private Connection createConnection() throws SQLException {
        return DriverManager.getConnection(url, user, password);
    }

    public synchronized Connection getConnection() throws SQLException {
        if (!connectionPool.isEmpty()) {
            return connectionPool.removeLast();
        }
        throw new SQLException("No available connections");
    }

    public synchronized void releaseConnection(Connection connection) {
        if (connectionPool.size() < MAX_CONNECTIONS) {
            connectionPool.add(connection); // Devolve a conexão ao pool
        }
    }

    public void closeAllConnections() {
        for (Connection connection : connectionPool) {
            try {
                if (connection != null && !connection.isClosed()) {
                    connection.close();
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }
}
