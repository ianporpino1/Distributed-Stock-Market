package com.message;


import java.io.Serializable;
import java.util.Arrays;

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

    public OrderRequest(String orderMessage) {
        this.parse(orderMessage);
    }

    private void parse(String orderMessage) {
        String[] parts = orderMessage.split(":", 2);
        if (parts.length != 2) {
            System.out.println("Formato de ordem inválido: " + orderMessage);
        }

        String[] orderDetails = parts[1].split(";");
        if (orderDetails.length != 4) {
            System.out.println("Formato de ordem inválido: " + Arrays.toString(orderDetails));
        }

        operation = orderDetails[0].trim();
        symbol = orderDetails[1].trim();

        try {
            quantity = Integer.parseInt(orderDetails[2].trim());
            price = Double.parseDouble(orderDetails[3].trim());
        } catch (NumberFormatException e) {
            System.out.println("Erro ao analisar quantidade ou preço: " + e.getMessage());
        }
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
