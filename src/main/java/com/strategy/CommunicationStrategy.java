package com.strategy;


import com.message.Message;
import com.server.MessageHandler;
import com.server.OrderHandler;

import java.io.IOException;


public interface CommunicationStrategy {
    void sendMessage(Message message, int nodeId);
    void startListening(int port, MessageHandler handler, OrderHandler orderHandler) throws IOException;
}

