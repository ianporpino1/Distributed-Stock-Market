package com.patterns;

public class Response extends Message{
    private String responseContent; 

    public Response(MessageType type, String responseContent) {
        super(type);
        this.responseContent = responseContent;
    }

    public String getResponseContent() {
        return responseContent;
    }

    public void setResponseContent(String responseContent) {
        this.responseContent = responseContent;
    }

    @Override
    public String toString() {
        return "Response{" +
                "type=" + getType() +
                ", generation=" + getGeneration() +
                ", senderId=" + getSenderId() +
                ", leaderId=" + getLeaderId() +
                ", responseContent='" + responseContent + '\'' +
                '}';
    }
}
