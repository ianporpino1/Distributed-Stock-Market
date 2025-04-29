package com.message;


public class OrderResponse implements Message{
    
    
    private final String responseMessage;
    private int senderId;
    public OrderResponse(String message) {
        this.responseMessage = message;
    }

    public String getResponseMessage() {
        return responseMessage;
    }
    
    public String toString() {
        return String.format("1|ORDER_RESPONSE|%s", responseMessage);
    }

    public static OrderResponse fromString(String message) {
        String[] parts = message.split("\\|");

        if (parts.length != 3) {
            throw new IllegalArgumentException("Invalid message format.");
        }
        String responseMessage = parts[2];
        
        return new OrderResponse(responseMessage);
    }
    

    @Override
    public int getSenderId() {
        return 0;
    }

   
}
