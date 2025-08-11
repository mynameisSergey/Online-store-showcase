package com.example.online_shop.service;

import com.example.online_shop.mapper.ItemInOrderMapper;
import com.example.online_shop.mapper.OrderMapper;
import com.example.online_shop.model.dto.CartDto;
import com.example.online_shop.model.dto.ItemDto;
import com.example.online_shop.model.dto.OrderDto;
import com.example.online_shop.model.dto.OrderItemDetailDto;
import com.example.online_shop.model.entity.ItemInOrder;
import com.example.online_shop.model.entity.Order;
import com.example.online_shop.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class OrderService {
    private final OrderRepository orderRepository;
    private final OrderMapper orderMapper;
    private final CartService cartService;
    private final ItemInOrderService itemInOrderService;
    private final ItemInOrderMapper itemInOrderMapper;
    private final JdbcTemplate jdbcTemplate;

    private final static String SQL_ORDER = """
            SELECT o.total_sum, io.image_path, io.description, io.title, io.price, io.count, io.item_id id
            FROM orders o
                 LEFT JOIN items_in_order io on o.id = io.order_id
            WHERE o.id = ?
            """;

    private final static String SQL_ORDERS = """
            SELECT o.id, o.total_sum, io.title, io.price, io.count, io.item_id
            FROM orders o
                LEFT JOIN items_in_order io on o.id = io.order_id
            """;

    @Transactional
    public Long buy() {
        CartDto cartCopy = cartService.getCart();
        if (!cartCopy.isEmpty()) {
            OrderDto orderDto = OrderDto.builder()
                    .totalSum(cartCopy.getTotal())
                    .items(cartCopy.getItems().values().stream().toList())
                    .build();
            Order order = orderRepository.save(orderMapper.toOrder(orderDto));
            List<ItemInOrder> items = itemInOrderMapper.toItemInOrderList(cartCopy.getItems().values().stream().toList());
            items.forEach(item -> item.setOrder(order));
            itemInOrderService.saveItemsInOrder(items);
            cartService.clearCart();
            return order.getId();
        } else return 0L;
    }

    public List<OrderDto> getOrders() {
        List<OrderItemDetailDto> itemsDetails = jdbcTemplate.query(SQL_ORDERS,
                (rs, rowNum) -> OrderItemDetailDto.builder()
                        .title(rs.getString("title"))
                        .price(rs.getBigDecimal("price") == null ? BigDecimal.valueOf(0) : rs.getBigDecimal("price"))
                        .count(rs.getInt("count"))
                        .orderId(rs.getLong("id"))
                        .totalSum(rs.getBigDecimal("total_sum"))
                        .build());
        List<Long> orderIds = itemsDetails.stream().map(OrderItemDetailDto::getOrderId).distinct().toList();
        List<OrderDto> orders = orderIds.stream().map(orderId -> OrderDto.builder()
                .id(orderId)
                .totalSum(itemsDetails.stream()
                        .filter(item -> orderId.equals(item.getOrderId()))
                        .findFirst()
                        .get().getTotalSum())
                .items(itemInOrderMapper.toItemDtoList(itemsDetails.stream()
                        .filter(item -> orderId.equals(item.getOrderId()))
                        .toList()))
                .build()).toList();
        return orders;
    }

    public OrderDto getOrderById(Long orderId) {
        BigDecimal totalSum = BigDecimal.valueOf(0);
        ItemDto item = new ItemDto();
        List<ItemDto> items = List.of(item);
        List<OrderItemDetailDto> itemsDetails = jdbcTemplate.query(SQL_ORDER,
                (rs, rowNum) -> OrderItemDetailDto.builder()
                        .title(rs.getString("title"))
                        .price(rs.getBigDecimal("price") == null ? BigDecimal.valueOf(0) : rs.getBigDecimal("price"))
                        .count(rs.getInt("count"))
                        .id(rs.getLong("id"))
                        .imagePath(rs.getString("image_path"))
                        .totalSum(rs.getBigDecimal("total_sum"))
                        .build(), orderId);
        if (!itemsDetails.isEmpty()) {
            totalSum = itemsDetails.getFirst().getTotalSum();
            items = itemInOrderMapper.toItemDtoList(itemsDetails);
        }
        return OrderDto.builder()
                .id(orderId)
                .items(items)
                .totalSum(totalSum)
                .build();
    }

}
