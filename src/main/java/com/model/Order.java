package com.model;

import java.time.Instant;

public class Order {
    private String symbol;
    private OrderType type;
    private OrderStatus status;
    private double price;
    private int quantity;
    private Instant receivedAt;

    public Order(String symbol, OrderType type, int quantity, double price) {
        this.symbol = symbol;
        this.type = type;
        this.price = price;
        this.quantity = quantity;
        this.receivedAt = Instant.now();
        this.status = OrderStatus.PENDING;
    }

    public String getSymbol() {
        return symbol;
    }

    public OrderType getType() {
        return type;
    }

    public double getPrice() {
        return price;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public Instant getReceivedAt() {
        return receivedAt;
    }

    public OrderStatus getStatus() {
        return status;
    }

    public void setStatus(OrderStatus status) {
        this.status = status;
    }
}
