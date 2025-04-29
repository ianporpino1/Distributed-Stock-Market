package com.strategy;


import com.message.Message;
import com.message.OrderRequest;
import com.message.OrderResponse;
import com.model.Order;
import com.server.MessageHandler;
import com.server.OrderHandler;

import java.io.IOException;
import java.net.InetSocketAddress;


public interface CommunicationStrategy {
    void sendMessage(Message message, int nodeId);
    void startListening(int port, MessageHandler handler, OrderHandler orderHandler) throws IOException;
    OrderResponse forwardOrder(OrderRequest orderRequest, InetSocketAddress clientAddress, int serverId);
}

