package com.example.online_shop.service;

import com.example.online_shop.mapper.ItemInOrderMapper;
import com.example.online_shop.mapper.OrderMapper;
import com.example.online_shop.model.dto.OrderDto;
import com.example.online_shop.model.entity.ItemInOrder;
import com.example.online_shop.model.entity.Order;
import com.example.online_shop.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class OrderService {
    private final OrderRepository orderRepository;
    private final OrderMapper orderMapper;
    private final CartService cartService;
    private final ItemInOrderService itemInOrderService;
    private final ItemInOrderMapper itemInOrderMapper;

    @Transactional
    public Mono<Long> buy() {
        return orderRepository.save(orderMapper.toOrder(OrderDto.builder()
                        .totalSum(cartService.getCart().getTotal())
                        .items(cartService.getCart().getItems().values().stream().toList())
                        .build()))
                .log()
                .map(Order::getId)
                .log()
                .doOnNext(orderId -> {
                    itemInOrderMapper.toItemInOrderList(cartService.getCart().getItems().values().stream().toList())
                            .forEach(item -> {
                                item.setOrderId(orderId);
                                itemInOrderService.save(item);
                            });
                });
    }

    public Flux<OrderDto> getOrders() {
        return itemInOrderService.getItems()
                .groupBy(ItemInOrder::getOrderId)
                .flatMap(Flux::collectList)
                .map(items -> OrderDto.builder()
                        .items(itemInOrderMapper.toItemDtoList(items))
                        .id(items.getFirst().getOrderId())
                        .totalSum(items.stream()
                                .map(item -> item.getPrice().multiply(BigDecimal.valueOf(item.getCount())))
                                .reduce(BigDecimal.ZERO, BigDecimal::add))
                        .build())
                .sort();
    }

    public Mono<OrderDto> getOrderById(Long orderId) {
        return itemInOrderService.getItemInOrderByOrderId(orderId)
                .groupBy(ItemInOrder::getOrderId)
                .flatMap(Flux::collectList)
                .map(items -> OrderDto.builder()
                        .items(itemInOrderMapper.toItemDtoList(items))
                        .id(items.getFirst().getOrderId())
                        .totalSum(items.stream()
                                .map(item -> item.getPrice().multiply(BigDecimal.valueOf(item.getCount())))
                                .reduce(BigDecimal.ZERO, BigDecimal::add))
                        .build())
                .next();
    }

}