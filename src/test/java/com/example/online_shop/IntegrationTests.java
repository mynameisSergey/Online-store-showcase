package com.example.online_shop;

import com.example.online_shop.enumiration.ECartAction;
import com.example.online_shop.model.dto.*;
import com.example.online_shop.model.entity.Item;
import com.example.online_shop.repository.ItemRepository;
import com.example.online_shop.repository.OrderRepository;
import com.example.online_shop.service.CartService;
import com.example.online_shop.service.ItemService;
import com.example.online_shop.service.OrderService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;

public class IntegrationTests extends OnlineShopApplicationTests {
    @Autowired
    private OrderService orderService;
    @Autowired
    private ItemService itemService;
    @Autowired
    private CartService cartService;
    @Autowired
    private OrderRepository orderRepository;
    @Autowired
    private ItemRepository itemRepository;

    @ParameterizedTest
    @ValueSource(strings = {"NO", "ALPHA", "PRICE"})
    void testGetItemsCheckSort(String sort) throws Exception {
        ItemsWithPagingDto items = itemService.getItems(null, sort, 1, 10);
        assertNotNull(items);
        assertNotNull(items.getItems());
        assertNotNull(items.getPaging());

        if ("ALPHA".equalsIgnoreCase(sort)) {
            assertEquals(jdbcTemplate.queryForObject("SELECT min(title) FROM items", String.class),
                    items.getItems().getFirst().getFirst().getTitle());
        } else if ("PRICE".equalsIgnoreCase(sort)) {
            assertEquals(jdbcTemplate.queryForObject("SELECT min(price) FROM items", BigDecimal.class),
                    items.getItems().getFirst().getFirst().getPrice());
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "Товар №1"})
    void testGetItemsCheckSearch(String search) throws Exception {
        ItemsWithPagingDto items = itemService.getItems(null, "NO", 1, 10);
        assertNotNull(items);
        assertNotNull(items.getItems());
        assertNotNull(items.getPaging());

        if (search != null && !search.isBlank()) {
            boolean found = items.getItems().stream().flatMap(List::stream).anyMatch(item -> item.getTitle().contains(search));
            assertTrue(found);
        } else {
            assertTrue(items.getItems().size() <= itemRepository.count());
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
        Item item = getAnyItem().orElse(new Item());
        ItemDto itemDto = itemService.getItemDtoById(item.getId());

        assertNotNull(itemDto);
        assertNotNull(itemDto.getId());
        assertEquals(item.getId(), itemDto.getId());
        assertEquals(item.getTitle(), itemDto.getTitle());
        assertEquals(item.getDescription(), itemDto.getDescription());
        assertEquals(item.getPrice(), itemDto.getPrice());
        assertEquals(imagePath + item.getId(), itemDto.getImagePath());
    }

    @Test
    void testBuy() throws Exception {
        OrderDto orderDto = getLastOrder().orElse(new OrderDto());
        Long lastId = orderDto.getId();
        Collection<Long> currentItemsInCart = cart.getItems().keySet();
        Long newOrderId = orderService.buy();

        assertNotNull(newOrderId);
        assertEquals(lastId + 1, newOrderId);
        List<Long> itemsInOrder = jdbcTemplate.query("SELECT item_id FROM items_in_order WHERE order_id = ?",
                (rs, i) -> rs.getLong("item_id"),
                newOrderId);
        assertEquals(currentItemsInCart.stream().sorted().toArray(), itemsInOrder.stream().sorted().toArray());
    }

    @Test
    void testGetOrders() throws Exception {
        List<OrderDto> orders = orderService.getOrders();
        assertNotNull(orders);
        assertNotNull(orders.getFirst().getId());
        assertNotNull(orders.getFirst().getTotalSum());
        assertNotNull(orders.getFirst().getItems());
        assertEquals(orders.size(), orderRepository.count());
    }

    @Test
    void testGetOrder() throws Exception {
        OrderDto orderDtoFromDb = getLastOrder().orElse(new OrderDto());
        OrderDto orderDto = orderService.getOrderById(orderDtoFromDb.getId());

        assertNotNull(orderDto);
        assertNotNull(orderDto.getId());
        assertEquals(orderDtoFromDb.getId(), orderDto.getId());
        assertEquals(orderDtoFromDb.getTotalSum(), orderDto.getTotalSum());
    }

    @Test
    void testAddItem() throws Exception {
        ItemDto itemDto = getLastItem().orElse(new ItemDto());
        Long lastId = itemDto.getId();
        ItemCreateDto itemCreateDto = ItemCreateDto.builder()
                .title(itemDto.getTitle())
                .price(itemDto.getPrice())
                .description(itemDto.getDescription())
                .build();
        try {
            MultipartFile picture = new MockMultipartFile("myblogdb.png",
                    Files.readAllBytes(new File("myblogdb.png").toPath()));
            itemCreateDto.setImage(picture);
        } catch (IOException ignore) {
        }
        ItemDto itemCreated = itemService.saveItem(itemCreateDto);

        assertNotNull(itemCreated);
        assertNotNull(itemCreated.getId());
        assertEquals(lastId + 1, itemCreated.getId());
        assertEquals(itemCreateDto.getTitle(), itemCreated.getTitle());
        assertEquals(itemCreateDto.getDescription(), itemCreated.getDescription());
        assertEquals(itemCreateDto.getPrice(), itemCreated.getPrice());
        assertEquals(imagePath + (lastId + 1), itemCreated.getImagePath());
    }

    @ParameterizedTest
    @ValueSource(strings = {"PLUS", "minus", "DEletE"})
    void testChangeItemCountInCart(String acrion) throws Exception {
        addItemInCart();
        int itemsInCartCnt = cart.getItems().values().stream().mapToInt(ItemDto::getCount).sum();

        ItemDto itemDto = getLastItem().orElse(new ItemDto());
        Long lastId = itemDto.getId();
        itemService.actionWithItemInCart(lastId, acrion);

        int itemsInCartAfterActionCnt = cart.getItems().values().stream().mapToInt(ItemDto::getCount).sum();
        switch (ECartAction.valueOf(acrion.toUpperCase())) {
            case PLUS -> {
                assertTrue(cart.getItems().containsKey(lastId));
                assertEquals(itemsInCartCnt + 1, itemsInCartAfterActionCnt);
            }
            case MINUS -> assertEquals(itemsInCartCnt == 0 ? 0 : itemsInCartCnt - 1, itemsInCartAfterActionCnt);
            case DELETE -> assertFalse(cart.getItems().containsKey(lastId));
        }
    }

    @Test
    void testClearCart() throws Exception {
        addItemInCart();
        assertFalse(cartService.getCart().isEmpty());
        cartService.clearCart();
        assertTrue(cartService.getCart().isEmpty());
        addItemInCart();
    }
}
