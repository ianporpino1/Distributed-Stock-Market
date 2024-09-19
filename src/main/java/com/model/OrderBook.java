package com.model;

import java.util.*;

public class OrderBook {
    private final PriorityQueue<Order> buyOrders;
    private final PriorityQueue<Order> sellOrders;

    public OrderBook() {
        this.buyOrders = new PriorityQueue<>((o1, o2) -> {
            int priceComparison = Double.compare(o2.getPrice(), o1.getPrice());
            if (priceComparison == 0) {
                return o1.getReceivedAt().compareTo(o2.getReceivedAt());
            }
            return priceComparison;
        });
        this.sellOrders = new PriorityQueue<>((o1, o2) -> {
            int priceComparison = Double.compare(o1.getPrice(), o2.getPrice());
            if (priceComparison == 0) {
                return o1.getReceivedAt().compareTo(o2.getReceivedAt());
            }
            return priceComparison;
        });
    }

    public synchronized void addOrder(Order order) {
        if (order.getType() == OrderType.BUY) {
            buyOrders.add(order);
        } else if (order.getType() == OrderType.SELL) {
            sellOrders.add(order);
        }
        matchOrders();
    }

    private void matchOrders() {
        while (!buyOrders.isEmpty() && !sellOrders.isEmpty()) {
            Order buyOrder = buyOrders.peek();
            Order sellOrder = sellOrders.peek();

            if (buyOrder.getPrice() >= sellOrder.getPrice()) {
                int quantity = Math.min(buyOrder.getQuantity(), sellOrder.getQuantity());
                buyOrder.setQuantity(buyOrder.getQuantity() - quantity);
                sellOrder.setQuantity(sellOrder.getQuantity() - quantity);

                if (buyOrder.getQuantity() == 0) buyOrders.poll();
                if (sellOrder.getQuantity() == 0) sellOrders.poll();
            } else {
                break;
            }
        }
    }

    public synchronized List<Order> getBuyOrders() {
        return new ArrayList<>(buyOrders);
    }

    public synchronized List<Order> getSellOrders() {
        return new ArrayList<>(sellOrders);
    }
}

