package com.message;

import java.io.Serializable;

public class OrderResponse implements Message, Serializable {
    private static final long serialVersionUID = 1L;
    
    private String responseMessage;
    private int senderId;
    public OrderResponse(String message) {
        this.responseMessage = message;
    }

    public String getResponseMessage() {
        return responseMessage;
    }

    @Override
    public int getSenderId() {
        return 0;
    }

   
}
