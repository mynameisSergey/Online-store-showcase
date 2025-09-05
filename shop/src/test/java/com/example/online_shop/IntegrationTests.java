package com.example.online_shop;

import com.example.online_shop.mapper.ItemInCartMapper;
import com.example.online_shop.model.dto.*;
import com.example.online_shop.model.entity.Item;
import com.example.online_shop.service.CartService;
import com.example.online_shop.service.ItemService;
import com.example.online_shop.service.OrderService;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;

import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.math.BigDecimal;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.*;

@Slf4j
public class IntegrationTests extends OnlineShopApplicationTests {
    @Autowired
    private OrderService orderService;
    @Autowired
    private ItemService itemService;
    @Autowired
    private CartService cartService;
    @Autowired
    private ItemInCartMapper itemInCartMapper;

    @ParameterizedTest
    @ValueSource(strings = {"NO", "ALPHA", "PRICE"})
    void testGetItemsCheckSort(String sort) throws Exception {
        itemService.getItems(null, sort, 1, 10, "user")
                .doOnNext(Assertions::assertNotNull).subscribe();

        var sortSelect = databaseClient.sql("select min(title) as title from items")
                .map((row, metadata) -> row.get("title", String.class))
                .one();
        var priceSelect = databaseClient.sql("select min(price) as price from items")
                .map((row, metadata) -> row.get("price", BigDecimal.class))
                .one();
        switch (sort.toUpperCase()) {
            case "ALPHA" -> itemService.getItems(null, sort, 1, 10, "user")
                    .zipWith(sortSelect, (items, fromDb) -> {
                        assertEquals(fromDb, items.getFirst().getFirst().getTitle());
                        return items;
                    }).subscribe();
            case "PRICE" -> itemService.getItems(null, sort, 1, 10, "user")
                    .zipWith(priceSelect, (items, fromDb) -> {
                        assertEquals(fromDb, items.getFirst().getFirst().getPrice());
                        return items;
                    }).subscribe();
            default -> System.out.println("No sort");
        }
    }

    @Test
    void testGetItemsInCart() throws Exception {
        addItemInCart()
                .flatMap(cart -> cartService.getCart("user"))
                .log()
                .zipWith(getItemsInCart().collectList(), (cart, items) -> {
                    assertArrayEquals(itemInCartMapper.toItemDto(items).toArray(), cart.getItems().values().toArray());
                    return cart;
                })
                .subscribe();
    }

    @Test
    void testGetItem() throws Exception {
        Mono<Item> item = getAnyItem();
        item.flatMap(itemFromDb -> itemService.getItemDtoById(itemFromDb.getId(), "user"))
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
                .flatMap(cart -> orderService.buy("user"))
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
    void testChangeItemCountInCart(String action) {
        addItemInCart()
                .map(itemInCartDto -> itemService.actionWithItemInCart(itemInCartDto.getItemId(), action, "user"))
                .flatMap(Function.identity())
                .zipWith(cartService.getItemsInCart("user"), (itemDto, itemMap) -> {
                    assertEquals(itemMap.get(0).getCount(), itemDto.getCount());
                    return itemDto.getCount();
                }).subscribe();
    }

    @Test
    void testClearCart() throws Exception {
        cartService.clearCart("user")
                .then(Mono.defer(() -> cartService.getCart("user")))
                .doOnNext(emptyCart -> assertTrue(emptyCart.isEmpty()))
                .subscribe();
    }
}
