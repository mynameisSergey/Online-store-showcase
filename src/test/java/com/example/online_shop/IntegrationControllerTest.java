package com.example.online_shop;

import com.example.online_shop.controller.ShopController;
import com.example.online_shop.model.dto.ItemCreateDto;
import com.example.online_shop.model.dto.ItemDto;
import com.example.online_shop.model.dto.OrderDto;
import com.example.online_shop.model.entity.Item;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@AutoConfigureMockMvc
public class IntegrationControllerTest extends OnlineShopApplicationTests {
    @Autowired
    private MockMvc mockMvc;
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
        mockMvc.perform(get("/main/items"))
                .andExpect(status().isOk())
                .andExpect(content().contentType("text/html;charset=UTF-8"))
                .andExpect(view().name("main"))
                .andExpect(model().attributeExists("items"))
                .andExpect(model().attributeExists("paging"));
    }

    @Test
    void testGetPageableItems() throws Exception {
        mockMvc.perform(get("/main/items")
                        .param("search", "")
                        .param("sort", "NO")
                        .param("pageNumber", "1")
                        .param("pageSize", "5"))
                .andExpect(status().isOk())
                .andExpect(content().contentType("text/html;charset=UTF-8"))
                .andExpect(view().name("main"))
                .andExpect(model().attributeExists("items"))
                .andExpect(model().attributeExists("paging"))
                .andExpect(model().attributeExists("search"))
                .andExpect(model().attributeExists("sort"));
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
        mockMvc.perform(get("/cart/items"))
                .andExpect(status().isOk())
                .andExpect(content().contentType("text/html;charset=UTF-8"))
                .andExpect(view().name("cart"))
                .andExpect(model().attributeExists("items"))
                .andExpect(model().attributeExists("total"))
                .andExpect(model().attributeExists("empty"));
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
        ItemDto itemDto = getLastItem().orElse(new ItemDto());
        mockMvc.perform(post("/main/items/" + itemDto.getId())
                        .contentType("application/x-www-form-urlencoded")
                        .queryParam("action", "plus")
                        .flashAttr("item", itemDto))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/main/items"));
    }

    /*
     * POST "/items/{id}" - изменить количество товара в корзине
     *
     * @param id     товара
     * @param action значение из перечисления PLUS|MINUS|DELETE (PLUS - добавить один товар, MINUS - удалить один товар, DELETE - удалить товар из корзины)
     * @return "redirect:/items/"
     */
    @Test
    void testChangeItemsCountInCartWhenInItem() throws Exception {
        ItemDto itemDto = getLastItem().orElse(new ItemDto());
        mockMvc.perform(post("/items/" + itemDto.getId())
                        .contentType("application/x-www-form-urlencoded")
                        .queryParam("action", "plus")
                        .flashAttr("item", itemDto))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/items/" + itemDto.getId()));
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
        Item item = getAnyItem().orElse(new Item());
        mockMvc.perform(get("/items/" + item.getId()))
                .andExpect(status().isOk())
                .andExpect(content().contentType("text/html;charset=UTF-8"))
                .andExpect(view().name("item"))
                .andExpect(model().attributeExists("item"));
    }

    /*
     * POST "/buy" - купить товары в корзине (выполняет покупку товаров в корзине и очищает ее)
     *
     * @return редирект на "/orders/{id}?newOrder=true"
     */

    @Test
    void testBuy() throws Exception {
        OrderDto orderDto = getLastOrder().orElse(new OrderDto());
        Long lastId = orderDto.getId();
        mockMvc.perform(post("buy")
                        .contentType("application/x-www-form-urlencoded")
                        .flashAttr("order", orderDto))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/orders/" + (lastId + 1) + "?newOrder=true"));
    }

    /*
     * GET "/orders" - список заказов
     *
     * @param model "orders" -List<Order> - список заказов:
     * @return "orders.html"
     */
    @Test
    void tstGetOrders() throws Exception {
        mockMvc.perform(get("/orders"))
                .andExpect(status().isOk())
                .andExpect(content().contentType("text/html;charset=UTF-8"))
                .andExpect(view().name("orders"))
                .andExpect(model().attributeExists("orders"));
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
        OrderDto orderDto = getLastOrder().orElse(new OrderDto());
        mockMvc.perform(get("/orders/" + orderDto.getId()))
                .andExpect(status().isOk())
                .andExpect(content().contentType("text/html;charset=UTF-8"))
                .andExpect(view().name("order"))
                .andExpect(model().attributeExists("order"));
    }

    /*
     * GET "/main/items/add" - страница добавления товара
     *
     * @return "add-item.html"
     */
    @Test
    void testAddItemPage() throws Exception {
        mockMvc.perform(get("/main/items/add"))
                .andExpect(status().isOk())
                .andExpect(content().contentType("text/html;charset=UTF-8"))
                .andExpect(view().name("add-item"));
    }

    /*
     * POST "/main/items" - добавление товара
     * @param item "multipart/form-data"
     * @return редирект на "/items/{id}"
     */
    @Test
    void testAddItem() throws Exception {
        ItemDto itemDto = getLastItem().orElse(new ItemDto());
        Long lastId = itemDto.getId();
        ItemCreateDto item = ItemCreateDto.builder()
                .title(itemDto.getTitle())
                .price(itemDto.getPrice())
                .description(itemDto.getDescription())
                .build();
        mockMvc.perform(post("/main/items")
                        .contentType("application/x-www-form-urlencoded")
                        .flashAttr("item", item))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/items/" + (lastId + 1)));

    }
}
