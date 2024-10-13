package com.repository;

import com.model.Order;

import java.sql.*;
import java.time.Instant;

public class OrderRepository {
    private final ConnectionManager connectionManager;

    public OrderRepository(ConnectionManager connectionManager) {
        this.connectionManager = connectionManager;
    }

    public void save(Order order, int executedQuantity, boolean isPartial) {
        String query = "INSERT INTO executed_orders (symbol, type, price, quantity, executed_at, is_partial) VALUES (?, ?, ?, ?, ?, ?)";
        Connection conn = null;

        try {
            conn = connectionManager.getConnection(); // Pega uma conexão do pool
            try (PreparedStatement pstmt = conn.prepareStatement(query)) {
                pstmt.setString(1, order.getSymbol());
                pstmt.setString(2, order.getType().name());
                pstmt.setDouble(3, order.getPrice());
                pstmt.setInt(4, executedQuantity);
                pstmt.setTimestamp(5, Timestamp.from(Instant.now()));
                pstmt.setBoolean(6, isPartial);
                pstmt.executeUpdate();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            if (conn != null) {
                connectionManager.releaseConnection(conn); // Devolve a conexão ao pool
            }
        }
    }
}
