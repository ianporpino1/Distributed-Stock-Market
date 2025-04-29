package com.service;

import com.message.OrderResponse;
import com.model.Order;
import com.model.OrderStatus;

public class MatchingEngine {
    public OrderBookService orderBookService;
    
    public MatchingEngine(OrderBookService orderBookService) {
        this.orderBookService = orderBookService;
    }
    
    public OrderResponse processOrder(Order order) {
        if (order == null) {
            throw new IllegalArgumentException("A ordem não pode ser nula.");
        }
        if (order.getQuantity() <= 0) {
            throw new IllegalArgumentException("A quantidade deve ser maior que zero.");
        }
        if (order.getPrice() <= 0) {
            throw new IllegalArgumentException("O preço deve ser maior que zero.");
        }
        OrderStatus status = orderBookService.addOrder(order);
        
        return new OrderResponse(status.toString());
    }
    
}
