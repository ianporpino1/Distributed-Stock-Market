package com.message;

public interface Message {
    int getSenderId();
    String toString();

    static Message fromString(String message) {
        String[] parts = message.split("\\|");
        String operation = parts[1];

        return switch (operation) {
            case "ORDER_REQUEST" -> OrderRequest.fromString(message);
            case "ORDER_RESPONSE" -> OrderResponse.fromString(message);
            case "VOTE_REQUEST" -> RequestVoteMessage.fromString(message);
            case "VOTE_RESPONSE" -> VoteResponseMessage.fromString(message);
            case "HEARTBEAT" -> HeartbeatMessage.fromString(message);
            default -> throw new IllegalArgumentException("Operação desconhecida: " + operation);
        };
    }
}
