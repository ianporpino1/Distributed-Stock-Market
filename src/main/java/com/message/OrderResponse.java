package com.message;

import java.io.Serializable;

public class OrderResponse implements Message, Serializable {
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
