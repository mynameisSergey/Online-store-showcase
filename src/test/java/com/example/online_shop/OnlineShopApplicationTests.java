package com.example.online_shop;

import com.example.online_shop.model.dto.CartDto;
import com.example.online_shop.model.dto.ItemDto;
import com.example.online_shop.model.dto.OrderDto;
import com.example.online_shop.model.entity.Item;
import com.example.online_shop.repository.ItemRepository;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.springframework.r2dbc.core.DatabaseClient;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.util.HashMap;

@SpringBootTest
@ActiveProfiles("test")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class OnlineShopApplicationTests {
    @Autowired
    protected DatabaseClient databaseClient;
    @Autowired
    protected ItemRepository itemRepository;
    @Autowired
    protected CartDto cart;

    @Value("${shop.image.path}")
    protected String imagePath;

    @BeforeAll
    public void createData() {
        System.out.println("Start createData");
        databaseClient.sql("""
                        DO $$
                        DECLARE
                        	item_id numeric := 0;
                        	order_id numeric := 0;
                        BEGIN
                        	insert into items(title, description, price) values('Товар№1', 'Тестовый товар номер один', 10);
                        	insert into items(title, description, price) values('Товар№2', 'Тестовый товар номер два', 20) returning id into item_id;
                        	insert into orders(total_sum) values(2200) returning id into order_id;
                        	insert into items_in_order(title, description, price, count, item_id, order_id) values('Товар№1', 'Тестовый товар номер один', 10, 2, item_id, order_id);
                        END $$;
                        """).fetch()
                .rowsUpdated()
                .subscribe();
        itemRepository.findAll().subscribe(System.out::println);
    }

    @AfterAll
    public void tearDownData() {
        System.out.println("Start tearDownData");

        databaseClient.sql("""
                        DO $$
                        BEGIN
                        delete from items_in_order;
                        delete from items;
                        delete from orders;
                        END $$;
                        """).fetch().rowsUpdated()
                .log()
                .subscribe();
    }

    protected Mono<CartDto> addItemInCart() {
        System.out.println("Start addItemInCart");
        return getLastItem().log().map(itemDto -> {
            itemDto.setCount(1);
            return itemDto;
        }).log().map(itemDto -> {
            cart.setEmpty(false);
            cart.setItems(new HashMap<Long, ItemDto>() {{
                put(itemDto.getId(), itemDto);
            }});
            cart.setTotal(itemDto.getPrice());
            return cart;
        }).log();
    }

    protected Mono<ItemDto> getLastItem() { // ищет последний товар из таблицы
        String sql = "select id, title, description, price, image from items order by id desc limit 1";

        return databaseClient.sql(sql)
                .map((row, metadata) -> ItemDto.builder()
                        .id(row.get("id", Long.class))
                        .title(row.get("title", String.class))
                        .description(row.get("description", String.class))
                        .price(row.get("price", BigDecimal.class))
                        .imagePath(imagePath + row.get("id", Long.class))
                        .build())
                .one()
                .log();
    }

    protected Mono<OrderDto> getLastOrder() { //ищет последний заказ в таблице
        String sql = "select id, total_sum from orders order by id desc limit 1";

        return databaseClient.sql(sql)
                .map((row, rowNum) -> OrderDto.builder()
                        .id(row.get("id", Long.class))
                        .totalSum(row.get("total_sum", BigDecimal.class))
                        .build())
                .one();
    }


    protected Mono<Item> getAnyItem() { //получает любой товар из БД
        String sql = "select id, title, description, price, image from items limit 1";
        return databaseClient.sql(sql)
                .map((row, metadata) -> Item.builder()
                        .id(row.get("id", Long.class))
                        .title(row.get("title", String.class))
                        .description(row.get("description", String.class))
                        .price(row.get("price", BigDecimal.class))
                        .image(row.get("image", byte[].class))
                        .build())
                .one();
    }
}




