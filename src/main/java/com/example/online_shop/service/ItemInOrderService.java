package com.example.online_shop.service;

import com.example.online_shop.model.entity.ItemInOrder;
import com.example.online_shop.repository.ItemInOrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class ItemInOrderService {
    private final ItemInOrderRepository itemInOrderRepository;

    public Mono<ItemInOrder> save(ItemInOrder item) {
        return itemInOrderRepository.save(item);
    }

    public Flux<ItemInOrder> getItemInOrderByOrderId(Long orderId) {
        return itemInOrderRepository.getItemInOrderByOrderId(orderId);
    }

    public Flux<ItemInOrder> getItems() {
        return itemInOrderRepository.findAll();
    }
}
