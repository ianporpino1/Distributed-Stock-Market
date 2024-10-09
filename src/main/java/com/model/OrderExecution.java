package com.model;

public class OrderExecution {
    private final Order order;
    private final int executedQuantity;
    private final double executedPrice;

    public OrderExecution(Order order, int executedQuantity, double executedPrice) {
        this.order = order;
        this.executedQuantity = executedQuantity;
        this.executedPrice = executedPrice;
    }

    public Order getOrder() {
        return order;
    }

    public int getExecutedQuantity() {
        return executedQuantity;
    }

    public double getExecutedPrice() {
        return executedPrice;
    }

    public boolean isPartialExecution() {
        return executedQuantity < order.getQuantity();
    }
}