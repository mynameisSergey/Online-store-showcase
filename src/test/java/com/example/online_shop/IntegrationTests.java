package com.example.online_shop;

import com.example.online_shop.enumiration.ECartAction;
import com.example.online_shop.model.dto.*;
import com.example.online_shop.model.entity.Item;
import com.example.online_shop.service.CartService;
import com.example.online_shop.service.ItemService;
import com.example.online_shop.service.OrderService;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;

import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

@Slf4j
public class IntegrationTests extends OnlineShopApplicationTests {
    @Autowired
    private OrderService orderService;
    @Autowired
    private ItemService itemService;
    @Autowired
    private CartService cartService;

    @ParameterizedTest
    @ValueSource(strings = {"NO", "ALPHA", "PRICE"})
    void testGetItemsCheckSort(String sort) throws Exception {
        itemService.getItems(null, sort, 1, 10)
                .doOnNext(itemRes -> {
                    assertNotNull(itemRes);
                    assertNotNull(itemRes.getItems());
                    assertNotNull(itemRes.getPaging());
                }).subscribe();

        var sortSelect = databaseClient.sql("select min(title) as title from items")
                .map((row, metadata) -> row.get("title", String.class))
                .one();
        var priceSelect = databaseClient.sql("select min(price) as price from items")
                .map((row, metadata) -> row.get("price", BigDecimal.class))
                .one();
        switch (sort.toUpperCase()) {
            case "ALPHA" -> itemService.getItems(null, sort, 1, 10)
                    .zipWith(sortSelect, (items, fromDb) -> {
                        assertEquals(fromDb, items.getItems().getFirst().getFirst().getTitle());
                        return items;
                    }).subscribe();
            case "PRICE" -> itemService.getItems(null, sort, 1, 10)
                    .zipWith(priceSelect, (items, fromDb) -> {
                        assertEquals(fromDb, items.getItems().getFirst().getFirst().getPrice());
                        return items;
                    }).subscribe();
            default -> System.out.println("No sort");
        }
    }

    @Test
    void testGetItemsInCart() throws Exception {
        CartDto cartItems = cartService.getCart();
        assertArrayEquals(cart.getItems().keySet().toArray(), cartItems.getItems().keySet().toArray());
        assertArrayEquals(cart.getItems().values().toArray(), cartItems.getItems().values().toArray());
    }

    @Test
    void testGetItem() throws Exception {
        Mono<Item> item = getAnyItem();
        item.flatMap(itemFromDb -> itemService.getItemDtoById(itemFromDb.getId()))
                .zipWith(item, (itemDto, anyItem) -> {

                    assertNotNull(itemDto);
                    assertNotNull(itemDto.getId());
                    assertEquals(anyItem.getId(), itemDto.getId());
                    assertEquals(anyItem.getTitle(), itemDto.getTitle());
                    assertEquals(anyItem.getDescription(), itemDto.getDescription());
                    assertEquals(anyItem.getPrice(), itemDto.getPrice());
                    assertEquals(imagePath + anyItem.getId(), itemDto.getImagePath());
                    return itemDto;
                })
                .subscribe();
    }

    @Test
    void testBuy() throws Exception {
        Mono<OrderDto> orderDtoFromDb = getLastOrder().log();
        orderDtoFromDb
                .flatMap(lastOrder -> addItemInCart())
                .log()
                .flatMap(cart -> orderService.buy())
                .log()
                .zipWith(orderDtoFromDb, (newOrderId, orderDto) -> {
                    assertNotNull(newOrderId);
                    assertEquals(orderDto.getId() + 1, newOrderId);
                    return newOrderId;
                })
                .map(orderId -> databaseClient.sql("SELECT item_id FROM items_in_order WHERE order_id = :orderId")
                        .bind("orderId", orderId)
                        .map((rs, i) -> rs.get("item_id", Long.class))
                        .all())
                .log()
                .publishOn(Schedulers.boundedElastic())
                .subscribe();
    }

    @Test
    void testGetOrders() throws Exception {
        orderService.getOrders()
                .doOnNext(order -> {
                    assertNotNull(order);
                    assertNotNull(order.getId());
                    assertNotNull(order.getItems());
                    assertNotNull(order.getTotalSum());
                }).subscribe();
    }

    @Test
    void testGetOrder() throws Exception {
        Mono<OrderDto> orderDtoFromDb = getLastOrder();
        orderDtoFromDb
                .map(orderDto -> orderService.getOrderById(orderDto.getId())
                        .zipWith(orderDtoFromDb, (orderFromDb, order) -> {

                            assertNotNull(order);
                            assertNotNull(order.getId());
                            assertEquals(orderFromDb.getId(), order.getId());
                            assertEquals(orderFromDb.getTotalSum(), order.getTotalSum());
                            return order;
                        }))
                .subscribe();
    }

    @Test
    void testAddItem() throws Exception {

        Mono<ItemCreateDto> itemCreateDto = getLastItem()
                .map(itemDto -> ItemCreateDto.builder()
                        .title(itemDto.getTitle())
                        .price(itemDto.getPrice())
                        .description(itemDto.getDescription())
                        .build())
                .log();
        itemService.saveItem(itemCreateDto)
                .zipWith(itemCreateDto, (item, dto) -> {

                    assertNotNull(item);
                    assertNotNull(item.getId());
                    assertEquals(dto.getTitle(), item.getTitle());
                    assertEquals(dto.getDescription(), item.getDescription());
                    assertEquals(dto.getPrice(), item.getPrice());
                    assertEquals(imagePath + item.getId(), item.getImagePath());
                    return dto;
                })
                .log()
                .subscribe();
    }

    @ParameterizedTest
    @ValueSource(strings = {"PLUS", "minus", "DeLeTe"})
    void testChangeItemCountInCart(String action) throws Exception {
        int itemsInCartCnt = cart.getItems().values().stream().mapToInt(ItemDto::getCount).sum();
        switch(ECartAction.valueOf(action.toUpperCase())) {
            case PLUS -> addItemInCart()
                    .flatMap(cartDto -> itemService.actionWithItemInCart(cartDto.getItems().values().stream().findFirst().get().getId(), action))
                    .subscribe(itemDto -> {
                        assertTrue(cart.getItems().containsKey(itemDto.getId()));
                        assertEquals(itemsInCartCnt + 2, cart.getItems().values().stream().mapToInt(ItemDto::getCount).sum());
                    });
            case MINUS -> addItemInCart()
                    .flatMap(cart -> itemService.actionWithItemInCart(cart.getItems().values().stream().findFirst().get().getId(), action))
                    .subscribe(itemDto ->
                            assertEquals(itemsInCartCnt == 0 ? 0 : itemsInCartCnt - 2, cart.getItems().values().stream().mapToInt(ItemDto::getCount).sum()));
            case DELETE -> addItemInCart()
                    .flatMap(cart -> itemService.actionWithItemInCart(cart.getItems().values().stream().findFirst().get().getId(), action))
                    .subscribe(itemDto -> assertFalse(cart.getItems().containsKey(itemDto.getId())));
        }
    }

    @Test
    void testClearCart() throws Exception {
        cartService.clearCart();
        assertTrue(cartService.getCart().isEmpty());
    }
}
