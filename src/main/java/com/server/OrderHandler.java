package com.server;

import com.patterns.OrderMessage;

import java.net.InetSocketAddress;

public interface OrderHandler {
    void handleOrder(OrderMessage orderMessage, InetSocketAddress sender);
}
