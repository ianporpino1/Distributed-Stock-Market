package com.strategy;

import com.patterns.Message;
import com.server.MessageHandler;
import com.server.OrderHandler;

import java.net.InetSocketAddress;

public interface CommunicationStrategy {
    void sendMessage(Message message, InetSocketAddress recipient);
    void startListening(int port, MessageHandler handler, OrderHandler orderHandler);
}

