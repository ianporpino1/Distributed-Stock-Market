package com.patterns;

import java.io.Serializable;

public class OrderMessage implements Serializable {
    private String operation;
    private String symbol;
    private int quantity;
    private double price;

    public OrderMessage(String orderType, String stockSymbol, int quantity, double price) {
        this.operation = orderType;
        this.symbol = stockSymbol;
        this.quantity = quantity;
        this.price = price;
    }


    public String getOperation() {
        return operation;
    }

    public void setOperation(String operation) {
        this.operation = operation;
    }

    public String getSymbol() {
        return symbol;
    }

    public void setSymbol(String symbol) {
        this.symbol = symbol;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public double getPrice() {
        return price;
    }

    public void setPrice(double price) {
        this.price = price;
    }

    @Override
    public String toString() {
        return "ORDER:" + operation + ";" + symbol + ";" + quantity + ";" + price;
    }
}
