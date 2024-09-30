package com.patterns;

import java.io.Serializable;

public class Request implements Serializable {
    public enum RequestType {
        MESSAGE,
        ORDER,
        RESPONSE
    }
    private RequestType type;
    private Message message;
    private OrderMessage order;
    private String responseContent;

    public Request(Message message) {
        this.type = RequestType.MESSAGE;
        this.message = message;
    }

    public Request(OrderMessage order) {
        this.type = RequestType.ORDER;
        this.order = order;
    }

    public Request(String responseContent) {
        this.type = RequestType.RESPONSE;
        this.responseContent = responseContent;
    }

    public RequestType getType() {
        return type;
    }

    public Message getMessage() {
        return message;
    }

    public OrderMessage getOrder() {
        return order;
    }

    public String getResponseContent() {
        return responseContent;
    }
}
