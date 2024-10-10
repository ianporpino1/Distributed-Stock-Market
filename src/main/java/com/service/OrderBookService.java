package com.service;

import com.model.Order;
import com.model.OrderBook;
import com.model.OrderExecution;
import com.repository.OrderRepository;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class OrderBookService {
    private final ConcurrentHashMap<String, OrderBook> orderBooks;
    private final OrderRepository orderRepository;
    private final ExecutorService executorService;

    public OrderBookService(OrderRepository orderRepository) {
        this.orderBooks = new ConcurrentHashMap<>();
        this.orderRepository = orderRepository;
        this.executorService = Executors.newFixedThreadPool(10);
    }

    private OrderBook getOrderBook(String symbol) {
        return orderBooks.computeIfAbsent(symbol, k -> new OrderBook());
    }

    void addOrder(Order order) {
        OrderBook orderBook = getOrderBook(order.getSymbol());
        orderBook.addOrder(order);

        List<OrderExecution> executedOrders = orderBook.matchOrders();

        if (!executedOrders.isEmpty()) {
            executorService.submit(() -> {
                for (OrderExecution executedOrder : executedOrders) {
                    orderRepository.save(executedOrder.getOrder(), executedOrder.getExecutedQuantity(), executedOrder.isPartialExecution());
                }
            });
        }
    }
    
}

