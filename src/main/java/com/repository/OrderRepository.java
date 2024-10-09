package com.repository;

import com.model.Order;

import java.sql.*;
import java.time.Instant;

public class OrderRepository {
    private final String url = "jdbc:postgresql://localhost:5433/matching_orders";
    private final String user = "postgres";
    private final String password = "postgres";

    private Connection connect() throws SQLException {
        return DriverManager.getConnection(url, user, password);
    }

    public void save(Order order, int executedQuantity, boolean isPartial) {
        String query = "INSERT INTO executed_orders (symbol, type, price, quantity, executed_at, is_partial) VALUES (?, ?, ?, ?, ?, ?)";

        try (Connection conn = connect();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setString(1, order.getSymbol());
            pstmt.setString(2, order.getType().name());
            pstmt.setDouble(3, order.getPrice());
            pstmt.setInt(4, executedQuantity);
            pstmt.setTimestamp(5, Timestamp.from(Instant.now()));
            pstmt.setBoolean(6, isPartial);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}