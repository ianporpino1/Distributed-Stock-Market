package com.message;


import java.io.Serializable;

public class OrderRequest implements Serializable, Message {
    int senderId;
    private String operation;
    private String symbol;
    private int quantity;
    private double price;

    public OrderRequest(int senderId, String operation, String symbol, int quantity, double price) {
        this.senderId = senderId;
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
        return "ORDER:" + operation + ";" + symbol + ";" + quantity + ";" + price;
    }
    
    @Override
    public int getSenderId() {
        return 0;
    }
    
    

   
}
