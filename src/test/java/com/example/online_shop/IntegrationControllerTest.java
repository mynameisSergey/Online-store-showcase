package com.example.online_shop;

import com.example.online_shop.controller.ShopController;
import com.example.online_shop.model.dto.ItemCreateDto;
import com.example.online_shop.model.dto.OrderDto;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@Slf4j
@AutoConfigureWebTestClient
public class IntegrationControllerTest extends OnlineShopApplicationTests {
    @Autowired
    private WebTestClient webTestClient;
    @Autowired
    private ShopController shopController;

    @Test
    void testController() {
        assertNotNull(shopController);
    }

    /*
     * GET "/main/items" -список всех товаров плиткой на главной странице
     * Параметры:
     * search - строка с поиском по названию/описанию товара (по умолчанию, пустая строка - все товары)
     * sort - сортировка. Перечисление NO, ALPHA, PRICE (по умолчанию, NO - не использовать сортировку)
     * pageSize - максимальное число товаров на странице (по умолчанию, 10)
     * pageNumber - номер текущей страницы (по умолчанию, 1)
     * Возвращает: шаблон "main.html"
     * используется модель для заполнения шаблона:
     * "items" - List<List<Item>> - список товаров по N в ряд (id, title, description, imagePath, count, price)
     * "search" - строка поиска (по умолчанию, пустая строка - все товары)
     * "sort" - сортировка. Перечисление NO, ALPHA, PRICE (по умолчанию, NO - не использовать сортировку)
     * "paging":
     * "pageNumber" - номер текущей страницы (по умолчанию, 1)
     * "pageSize" - максимальное число товаров на странице (по умолчанию, 10)
     * "hasNext" - можно ли пролистнуть вперед
     * "hasPrevious" - можно ли пролистнуть назад
     */

    @Test
    void testGetItems() throws Exception {
        webTestClient.get()
                .uri("/main/items")
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentType(MediaType.TEXT_HTML)
                .expectBody(String.class).consumeWith(response -> {
                    String body = response.getResponseBody();
                    assertNotNull(body);
                    assertTrue(body.contains("<title>Витрина товаров</title>"));
                });
    }

    /*
     * GET "/cart/items" - список товаров в корзине
     *
     * @param model "items" - List<Item> - список товаров в корзине (id, title, description, imgPath, count, price)
     *              "total" - суммарная стоимость заказа
     *              "empty" - trueб если в корзину не добавлен ни один товар
     * @return шаблон "cart.html"
     */
    @Test
    void testGetItemsInCart() throws Exception {
        webTestClient.get()
                .uri("/cart/items")
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentType(MediaType.TEXT_HTML)
                .expectBody(String.class).consumeWith(response -> {
                    String body = response.getResponseBody();
                    assertNotNull(body);
                    assertTrue(body.contains("<title>Корзина товаров</title>"));
                });

    }

    /*
     * POST "/main/items/{id}" - изменить количество товара в корзине
     *
     * @param id     товара
     * @param action значение из перечисления PLUS|MINUS|DELETE (добавить товар, удалить один товар, удалить товар из корзины)
     * @return редирект на "/main/items"
     */
    @Test
    void testChangeItemsCountInCartWhenInItems() throws Exception {
        getLastItem().publishOn(Schedulers.boundedElastic()).doOnNext(itemDto -> {
            webTestClient.post()
                    .uri("/main/items/" + itemDto.getId())
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .bodyValue("action=MINUS")
                    .exchange()
                    .expectStatus().is3xxRedirection()
                    .expectHeader().valueEquals("Location", "/main/items");
        }).subscribe();
    }

    /*
     * POST "/items/{id}" - изменить количество товара в корзине
     *
     * @param id     товара
     * @param action значение из перечисления PLUS|MINUS|DELETE (PLUS - добавить один товар, MINUS - удалить один товар, DELETE - удалить товар из корзины)
     * @return "redirect:/items/"
     */
    @Test
    void testChangeItemsCountInCartWhenInCart() throws Exception {
        getLastItem().publishOn(Schedulers.boundedElastic()).doOnNext(itemDto -> {
            webTestClient.post()
                    .uri("/items/" + itemDto.getId())
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .bodyValue("action=PLUS")
                    .exchange()
                    .expectStatus().is3xxRedirection()
                    .expectHeader().valueEquals("Location", "/items");
        }).subscribe();
    }

    /*
     * GET "/items/{id}" - карточка товара
     *
     * @param id    товара
     * @param model "item" товар(id, title, description, imgPath, count, price)
     * @return "item"
     */
    @Test
    void testGetItem() throws Exception {
        getAnyItem().publishOn(Schedulers.boundedElastic()).doOnNext(itemDto ->
                webTestClient.get()
                        .uri("/items/" + itemDto.getId())
                        .exchange()
                        .expectStatus().isOk()
                        .expectHeader().contentType(MediaType.TEXT_HTML)
                        .expectBody(String.class).consumeWith(response -> {
                            String body = response.getResponseBody();
                            assertNotNull(body);
                            assertTrue(body.contains("<title>Витрина товаров</title>"));
                        })).subscribe();
    }

    /*
     * POST "/buy" - купить товары в корзине (выполняет покупку товаров в корзине и очищает ее)
     *
     * @return редирект на "/orders/{id}?newOrder=true"
     */

    @Test
    void testBuy() throws Exception {
        getLastOrder().publishOn(Schedulers.boundedElastic()).doOnNext(orderDto ->
                        webTestClient.post()
                                .uri("/buy")
                                .bodyValue(OrderDto.builder()
                                        .totalSum(orderDto.getTotalSum())
                                        .items(orderDto.getItems())
                                        .build())
                                .exchange()
                                .expectStatus().is3xxRedirection()
                                .expectHeader().valueEquals("Location", "/orders/"
                                        + (orderDto.getId() + 1) + "?newOrder=true"))
                .log()
                .subscribe();
    }

    /*
     * GET "/orders" - список заказов
     *
     * @param model "orders" -List<Order> - список заказов:
     * @return "orders.html"
     */
    @Test
    void tstGetOrders() throws Exception {
        webTestClient.get()
                .uri("/orders")
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentType(MediaType.TEXT_HTML)
                .expectBody(String.class).consumeWith(response -> {
                    String body = response.getResponseBody();
                    assertNotNull(body);
                    assertTrue(body.contains("<title>Заказы</title>"));
                });
    }

    /*
     * GET "/orders/{id}" - карточка заказа
     *
     * @param model    "order" - заказ, "items" - List<Item> - список товаров в заказе (id, title, description, imgPath, count, price)
     * @param id       идентификатор заказа
     * @param newOrder true, если переход со страницы оформления заказа (по умолчанию, false)
     * @return "order.html"
     */
    @Test
    void testGetOrder() throws Exception {
        getLastOrder().publishOn(Schedulers.boundedElastic()).doOnNext(orderDto ->
                        webTestClient.get()
                                .uri("/orders/" + orderDto.getId())
                                .exchange()
                                .expectStatus().isOk()
                                .expectHeader().contentType(MediaType.TEXT_HTML)
                                .expectBody(String.class).consumeWith(response -> {
                                    String body = response.getResponseBody();
                                    assertNotNull(body);
                                    assertTrue(body.contains("<title>Заказ</title>"));
                                }))
                .subscribe();

    }

    /*
     * GET "/main/items/add" - страница добавления товара
     *
     * @return "add-item.html"
     */
    @Test
    void testAddItemPage() throws Exception {
        webTestClient.get()
                .uri("/main/items/add")
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentType(MediaType.TEXT_HTML)
                .expectBody(String.class).consumeWith(response -> {
                    String body = response.getResponseBody();
                    assertNotNull(body);
                    assertTrue(body.contains("<h3>Изображение</h3>"));
                });
    }

    /*
     * POST "/main/items" - добавление товара
     * @param item "multipart/form-data"
     * @return редирект на "/items/{id}"
     */
    @Test
    void testAddItem() throws Exception {
        getLastItem().map(itemDto ->
                        Mono.just(ItemCreateDto.builder()
                                        .title(itemDto.getTitle() + "_NEW")
                                        .price(itemDto.getPrice())
                                        .description(itemDto.getDescription() + "_NEW")
                                        .build())
                                .publishOn(Schedulers.boundedElastic())
                                .doOnNext(item -> webTestClient.post()
                                        .uri("/main/items")
                                        .bodyValue(item)
                                        .exchange()
                                        .expectStatus().is3xxRedirection()
                                        .expectHeader().valueEquals("Location", "/items/" + (itemDto.getId() + 1))))
                .subscribe();
    }
}
