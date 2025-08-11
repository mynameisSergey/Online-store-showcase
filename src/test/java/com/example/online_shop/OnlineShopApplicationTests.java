package com.example.online_shop;

import com.example.online_shop.model.dto.CartDto;
import com.example.online_shop.model.dto.ItemDto;
import com.example.online_shop.model.dto.OrderDto;
import com.example.online_shop.model.entity.Item;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.util.HashMap;
import java.util.List;
import java.util.Optional;

@SpringBootTest
@ActiveProfiles("test")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class OnlineShopApplicationTests {

    @Autowired
    protected JdbcTemplate jdbcTemplate;
    @Autowired
    protected CartDto cart;
    protected Long maxItemId = 0L;
    protected Long maxOrderId = 0L;

    @Value("${shop.image.path}")
    protected String imagePath;

    @BeforeAll
    public void createData() {
        jdbcTemplate.update("insert into items(title, description, price) values('Товар №1', 'Тестовый товар номер один', 10)");
        jdbcTemplate.update("insert into items(title, description, price) values('Товар №2','Тестовый товар номер два', 20)");
        jdbcTemplate.update("insert into orders(total_sum) values(2200)");
        maxOrderId = jdbcTemplate.queryForObject("select coalesce(max(id),0) from orders", Long.class);
        maxItemId = jdbcTemplate.queryForObject("select coalesce(max(id),0) from items", Long.class);
        jdbcTemplate.update("insert into items_in_order(title, description, price, count, item_id, order_id) values('Товар №1', 'Тестовый товар номер один', 10, 2, ?, ?)", maxItemId, maxOrderId);
        addItemInCart();
    }

    @AfterAll
    public void tearDownData() {
        jdbcTemplate.update("delete from items_in_order");
        jdbcTemplate.update("delete from items");
        jdbcTemplate.update("delete from orders");
    }

    protected void addItemInCart() { // доавляет товар в корзину
        ItemDto item = getLastItem().get();
        item.setCount(1);
        cart.setEmpty(false);
        cart.setItems(new HashMap<Long, ItemDto>() {{
            put(item.getId(), item);
        }});
        cart.setTotal(item.getPrice());
    }

    protected Optional<ItemDto> getLastItem() { // ищет последний товар из таблицы
        String sql = "select id, title, description, price, image from items order by id desc limit 1";

        List<ItemDto> items = jdbcTemplate.query(sql,
                (rs, rowNum) -> ItemDto.builder()
                        .id(rs.getLong("id"))
                        .title(rs.getString("title"))
                        .description(rs.getString("description"))
                        .price(rs.getBigDecimal("price"))
                        .imagePath(imagePath + rs.getLong("id"))
                        .build());

        return items.stream().findFirst();
    }

    protected Optional<OrderDto> getLastOrder() { //ищет последний заказ в таблице
        String sql = "select id, total_sum from orders order by id desc limit 1";

        List<OrderDto> orders = jdbcTemplate.query(sql, (rs, rowNum) -> OrderDto.builder()
                .id(rs.getLong("id"))
                .totalSum(rs.getBigDecimal("total_sum"))
                .build());

        return orders.stream().findFirst();
    }

    protected Optional<Item> getAnyItem() { //получает любой товар из БД
        String sql = "select id, title, description, price, image from items limit 1";

        List<Item> items = jdbcTemplate.query(sql, (rs, rowNum) ->
                Item.builder()
                        .id(rs.getLong("id"))
                        .title(rs.getString("title"))
                        .description(rs.getString("description"))
                        .price(rs.getBigDecimal("price"))
                        .image(rs.getBytes("image"))
                        .build()
        );
        return items.stream().findFirst();
    }

}
