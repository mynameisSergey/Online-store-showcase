package com.example.online_shop;

import com.example.online_shop.model.dto.ItemDto;
import com.example.online_shop.model.dto.ItemInCartDto;
import com.example.online_shop.model.dto.OrderDto;
import com.example.online_shop.model.entity.Item;
import com.example.online_shop.repository.ItemInCartRepository;
import com.example.online_shop.repository.ItemRepository;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.context.ApplicationContext;
import org.springframework.r2dbc.core.DatabaseClient;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.util.function.Function;

@Slf4j
@SpringBootTest
@ActiveProfiles("test")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@AutoConfigureWebTestClient
class ShopApplicationTests {
    @Autowired
    protected DatabaseClient databaseClient;
    @Autowired
    protected ItemRepository itemRepository;
    @Autowired
    protected ItemInCartRepository itemInCartRepository;
    @Autowired
    protected ApplicationContext context;

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
                        	delete from items_in_order;
                        	delete from items_in_cart;
                        	delete from items;
                        	delete from orders;
                        	delete from users;
                            insert into users(login, password, roles) values('user', '$2a$10$jurTwq7mqMVbqAbvHuLtHeuInZsfSVj58hwms7qeDDS2YMAUwyHLe', 'ROLE_USER');
                        	insert into users(login, password, roles) values('admin', '$2a$10$1uqPC2fATNy7VynsRKsPQeoEl0gs09HrbukCiqoXcaHSFDf9iDLxG', 'ROLE_ADMIN');
                        	insert into items(title, description, price) values('Товар№1', 'Тестовый товар номер один', 10);
                        	insert into items(title, description, price) values('Товар№2', 'Тестовый товар номер два', 20) returning id into item_id;
                        	insert into orders(total_sum) values(2200) returning id into order_id;
                        	insert into items_in_order(title, description, price, count, item_id, order_id) values('Товар№1', 'Тестовый товар номер один', 10, 2, item_id, order_id);
                        	insert into items_in_cart(title, description, price, count, item_id, login, image_path) values('Товар№1', 'Тестовый товар номер один', 10, 2, item_id, 'user', 'http://localhost:8084/items/image/'||item_id);
                        END $$;
                        """).fetch()
                .rowsUpdated()
                .subscribe();
        itemRepository.findAll().subscribe(System.out::println);
        itemInCartRepository.findAll().subscribe(System.out::println);
    }

    @AfterAll
    public void tearDownData() {
        System.out.println("Start tearDownData");
/*
		databaseClient.sql("""
			DO $$
			BEGIN
			delete from items_in_order;
			delete from items_in_cart;
			delete from items;
			delete from orders;
			delete from users;
			END $$;
    		""").fetch().rowsUpdated()
				.log()
				.subscribe();*/
    }

    protected Mono<ItemDto> getLastItem() {
        String sql = """
                with last_item as (select last_value(i.id) over () max_id, i.* from items i)
                select last_item.id, last_item.title, last_item.description, last_item.price, last_item.image
                from last_item
                where last_item.id = last_item.max_id
                """;
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

    protected Mono<Item> getAnyItem() {
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

    protected Mono<OrderDto> getLastOrder() {
        String sql = """ 
                			with last_order as (select last_value(o.id) over () max_id, o.* from orders o)
                select last_order.id, last_order.total_sum, last_order.login
                from last_order
                where last_order.id = last_order.max_id
                """;
        return databaseClient.sql(sql)
                .map((row, rowNum) -> OrderDto.builder()
                        .id(row.get("id", Long.class))
                        .totalSum(row.get("total_sum", BigDecimal.class))

                        .build())
                .one();
    }

    protected Mono<ItemInCartDto> addItemInCart() {
        System.out.println("Start addItemInCart");
        return databaseClient.sql("""
                         			DO $$
                        DECLARE
                        	item_id numeric := 0;
                        BEGIN
                        	select max(id) from items into item_id;
                        	merge into items_in_cart as c
                        		using (select 'Товар№1' title, 'Тестовый товар номер один' description, 10 price, 1 count, item_id item_id, '' image_path, 'user' login) as i
                        		on c.login = i.login and c.item_id = i.item_id
                        		when not matched then
                        			insert(title, description, price, count, item_id, image_path, login)
                        			values(i.title, i.description, i.price, i.count, i.item_id, i.image_path, i.login)
                        		when matched then
                        			update set count=i.count+1;
                        END $$;
                        """)
                .fetch()
                .one()
                .thenReturn(getItemsInCart()
                        .collectList()
                        .map(items -> items.stream().findFirst().get()))
                .flatMap(Function.identity());
    }

    protected Flux<ItemInCartDto> getItemsInCart() {
        System.out.println("Start addItemInCart");
        return databaseClient.sql("""
                        	select id, title, description, price, count, item_id, login, image_path from items_in_cart where login = 'user';
                        """).map((row, rowNum) -> ItemInCartDto.builder()
                        .id(row.get("id", Long.class))
                        .title(row.get("title", String.class))
                        .description(row.get("description", String.class))
                        .imagePath(row.get("image_path", String.class))
                        .count(row.get("count", Integer.class) == null ? 0 : row.get("count", Integer.class))
                        .price(row.get("price", BigDecimal.class))
                        .itemId(row.get("item_id", Long.class))
                        .login(row.get("login", String.class))
                        .build())
                .all();
    }
}