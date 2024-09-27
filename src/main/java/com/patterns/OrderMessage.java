package com.patterns;

public class OrderMessage {
    private String operation;
    private String symbol;
    private int quantity;
    private double price;

    @Override
    public String toString() {
        return "ORDER:" + operation + ";" + symbol + ";" + quantity + ";" + price;
    }
}
