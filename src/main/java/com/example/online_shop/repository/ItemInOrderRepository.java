package com.example.online_shop.repository;

import com.example.online_shop.model.entity.ItemInOrder;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;

public interface ItemInOrderRepository extends ReactiveCrudRepository<ItemInOrder, Long> {
    Flux<ItemInOrder> getItemInOrderByOrderId(Long orderId);
}
