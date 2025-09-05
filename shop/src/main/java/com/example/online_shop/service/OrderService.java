package com.example.online_shop.service;

import com.example.online_shop.mapper.ItemInOrderMapper;
import com.example.online_shop.mapper.OrderMapper;
import com.example.online_shop.model.dto.OrderDto;
import com.example.online_shop.model.entity.ItemInOrder;
import reactor.core.publisher.Mono;
import com.example.online_shop.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderService {
    private final OrderRepository orderRepository;
    private final OrderMapper orderMapper;
    private final CartService cartService;
    private final ItemInOrderService itemInOrderService;
    private final ItemInOrderMapper itemInOrderMapper;
    private final PaymentsService paymentsService;

    @Transactional
    public Mono<Long> buy(String login) {
        log.info("Start buy: login={}", login);

        return cartService.getCart(login).flatMap(cart -> paymentsService.createPayment(cart.getTotal()).flatMap(paymentRes -> {
                    if (!paymentRes) {
                        return Mono.error(new RuntimeException("Payment failed"));
                    }

                    OrderDto orderDto = OrderDto.builder().totalSum(cart.getTotal()).items(cart.getItems()
                                    .values()
                                    .stream()
                                    .toList())
                            .login(login)
                            .build();

                    return orderRepository.save(orderMapper.toOrder(orderDto)).flatMap(order -> {
                        List<ItemInOrder> itemsInOrder = itemInOrderMapper.toItemInOrderList(orderDto.getItems());
                        itemsInOrder.forEach(item -> item.setOrderId(order.getId()));

                        // сохраняем все items реактивно и ждем завершения
                        return Flux.fromIterable(itemsInOrder).flatMap(itemInOrderService::save).thenReturn(order.getId());
                    }).flatMap(orderId -> cartService.clearCart(login).thenReturn(orderId));
                })
        );
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
                .flatMap(groupedFlux -> groupedFlux.collectList())
                .map(items -> OrderDto.builder()
                        .items(itemInOrderMapper.toItemDtoList(items))
                        .id(items.get(0).getOrderId())
                        .totalSum(items.stream()
                                .map(item -> item.getPrice()
                                        .multiply(BigDecimal.valueOf(item.getCount())))
                                .reduce(BigDecimal.ZERO, BigDecimal::add))
                        .build())
                .next();
    }
}