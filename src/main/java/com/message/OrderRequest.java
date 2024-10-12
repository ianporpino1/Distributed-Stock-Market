package com.message;

public class OrderRequest implements Message {
    
    private String operation;
    private String symbol;
    private int quantity;
    private double price;

    public OrderRequest(String operation, String symbol, int quantity, double price) {
        this.operation = operation;
        this.symbol = symbol;
        this.quantity = quantity;
        this.price = price;
    }

    public String getOperation() {
        return operation;
    }

    public String getSymbol() {
        return symbol;
    }

    public int getQuantity() {
        return quantity;
    }

    public double getPrice() {
        return price;
    }

    @Override
    public String toString() {
        return String.format("1|ORDER_REQUEST|%s;%s;%d;%.2f", operation, symbol, quantity, price);
    }

    public static OrderRequest fromString(String message) {
        String[] parts = message.split("\\|");

        if (parts.length != 3) {
            throw new IllegalArgumentException("Invalid message format.");
        }

        String[] dataParts = parts[2].split(";");

        if (dataParts.length != 4) {
            throw new IllegalArgumentException("Invalid order format.");
        }

        String operation = dataParts[0];
        String symbol = dataParts[1];
        int quantity = Integer.parseInt(dataParts[2]);
        double price = Double.parseDouble(dataParts[3].replace(",", "."));

        return new OrderRequest(operation, symbol, quantity, price);
    }

    @Override
    public int getSenderId() {
        return 0;
    }
    
    

   
}
