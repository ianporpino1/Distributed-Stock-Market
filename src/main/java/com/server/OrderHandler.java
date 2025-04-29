package com.server;

import com.message.OrderRequest;
import com.message.OrderResponse;

import java.net.InetSocketAddress;

public interface OrderHandler {
    OrderResponse handleOrder(OrderRequest orderRequest, InetSocketAddress sender);
}
