package com.example.online_shop.repository;

import com.example.online_shop.model.entity.Order;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;

public interface OrderRepository extends ReactiveCrudRepository<Order, Long> {
}
