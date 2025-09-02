package com.example.online_shop.mapper;

import com.example.online_shop.model.dto.OrderDto;
import com.example.online_shop.model.entity.Order;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class OrderMapper {

    private final ModelMapper mapper;

    public Order toOrder(OrderDto orderDto) {
        log.debug("Start toOrder: orderDto={}, order={}", orderDto, mapper.map(orderDto, Order.class));
        return mapper.map(orderDto, Order.class);
    }
}
