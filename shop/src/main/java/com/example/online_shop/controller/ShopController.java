package com.example.online_shop.controller;

import com.example.online_shop.model.dto.CartDto;
import com.example.online_shop.model.dto.ItemCreateDto;
import com.example.online_shop.model.dto.ItemDto;
import com.example.online_shop.model.dto.ItemsWithPagingDto;
import com.example.online_shop.service.CartService;
import com.example.online_shop.service.ItemService;
import com.example.online_shop.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.function.Function;

@Controller
@RequiredArgsConstructor
public class ShopController {
    private final ItemService itemService;
    private final OrderService orderService;
    private final CartService cartService;

    /**
     * GET "/" — редирект на "/main/items".
     */

    @GetMapping("/")
    public Mono<String> redirectItems() {
        return Mono.just("redirect:/main/items");
    }

    /**
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

    @GetMapping("/main/items")
    public Mono<String> getItems(Model model,
                                 @RequestParam(defaultValue = "", name = "search") String search,
                                 @RequestParam(defaultValue = "NO", name = "sort") String sort,
                                 @RequestParam(defaultValue = "1", name = "pageNumber") int pageNumber,
                                 @RequestParam(defaultValue = "10", name = "pageSize") int pageSize) {

        Mono<ItemsWithPagingDto> items = itemService.getItems(search, sort, pageNumber, pageSize);
        model.addAttribute("items", items.map(ItemsWithPagingDto::getItems));
        model.addAttribute("search", search);
        model.addAttribute("sort", sort);
        model.addAttribute("paging", items.map(ItemsWithPagingDto::getPaging));
        return Mono.just("main");

    }

    /*
     * POST "/main/items/{id}" - изменить количество товара в корзине
     *
     * @param id     товара
     * @param action значение из перечисления PLUS|MINUS|DELETE (добавить товар, удалить один товар, удалить товар из корзины)
     * @return редирект на "/main/items"
     */
    @PostMapping("/main/items/{id}")
    public Mono<String> changeItemCount(@PathVariable("id") Long id,
                                        ServerWebExchange exchange) {

        return inspectRequest(id, exchange)
                .map(itemDto -> "redirect:/main/items");
    }

    /**
     * GET "/cart/items" - список товаров в корзине
     *
     * @param model "items" - List<Item> - список товаров в корзине (id, title, description, imgPath, count, price)
     *              "total" - суммарная стоимость заказа
     *              "empty" - true если в корзину не добавлен ни один товар
     * @return шаблон "cart.html"
     */
    @GetMapping("/cart/items")
    public Mono<String> getChart(Model model) {
        CartDto cartCopy = cartService.getCart();
        model.addAttribute("items", cartCopy.getItems().values());
        model.addAttribute("total", cartCopy.getTotal());
        model.addAttribute("empty", cartCopy.isEmpty());
        return Mono.just("cart");

    }

    /*
     * POST "/cart/items/{id}" - изменить количество товара в корзине
     *
     * @param id     товара
     * @param action значение из перечисления PLUS|MINUS|DELETE (PLUS - добавить один товар, MINUS - удалить один товар,
     *               DELETE - удалить товар из корзины)
     * @return "redirect:/cart/items"
     */

    @PostMapping("/cart/items/{id}")
    public Mono<String> changeItemCountInCart(@PathVariable("id") Long id,
                                              ServerWebExchange exchange) {
        return inspectRequest(id, exchange)
                .map(itemDto -> "redirect:/cart/items");
    }

    /**
     * GET "/items/{id}" - карточка товара
     *
     * @param id    товара
     * @param model "item" товар(id, title, description, imgPath, count, price)
     * @return "item"
     */
    @GetMapping("/items/{id}")
    public Mono<String> getItem(@PathVariable("id") Long id, Model model) {
        return itemService.getItemDtoById(id)
                .doOnNext(item -> model.addAttribute("item", item))
                .map(order -> "item");
    }

    /*
     * POST "/items/{id}" - изменить количество товара в корзине
     *
     * @param id     товара
     * @param action значение из перечисления PLUS|MINUS|DELETE (PLUS - добавить один товар, MINUS - удалить один товар, DELETE - удалить товар из корзины)
     * @return "redirect:/items/"
     */
    @PostMapping("/items/{id}")
    public Mono<String> changeItemsCount(@PathVariable("id") Long id,
                                         ServerWebExchange exchange) {
        return inspectRequest(id, exchange)
                .map(itemDto -> "redirect:/items/" + itemDto.getId());
    }

    /**
     * POST "/buy" - купить товары в корзине (выполняет покупку товаров в корзине и очищает ее)
     *
     * @return редирект на "/orders/{id}?newOrder=true"
     */
    @PostMapping("/buy")
    public Mono<String> buy() {
        return orderService.buy()
                .map(id -> "redirect:/orders/" + id + "?newOrder=true");
    }

    /**
     * GET "/orders" - список заказов
     *
     * @param model "orders" -List<Order> - список заказов:
     * @return "orders.html"
     */
    @GetMapping("/orders")
    public Mono<String> getOrders(Model model) {
        model.addAttribute("orders", orderService.getOrders());
        return Mono.just("orders");
    }

    /**
     * GET "/orders/{id}" - карточка заказа
     *
     * @param model    "order" - заказ, "items" - List<Item> - список товаров в заказе (id, title, description, imgPath, count, price)
     * @param id       идентификатор заказа
     * @param newOrder true, если переход со страницы оформления заказа (по умолчанию, false)
     * @return "order.html"
     */
    @GetMapping("/orders/{id}")
    public Mono<String> getOrder(Model model, @PathVariable("id") Long id,
                                 @RequestParam(name = "newOrder", defaultValue = "false") boolean newOrder) {
        model.addAttribute("newOrder", newOrder);
        model.addAttribute("order", orderService.getOrderById(id));
        return Mono.just("order");
    }

    /**
     * GET "/items/image/{id}" - эндпоинт, возвращающий набор байт картинки поста
     *
     * @param id идентификатор товара
     * @return картинка в байтах
     */
    @GetMapping("/items/image/{id}")
    @ResponseBody
    public Mono<byte[]> getImage(@PathVariable("id") Long id) {
        return itemService.getImage(id);
    }

    /**
     * GET "/main/items/add" - страница добавления товара
     *
     * @return "add-item.html"
     */
    @GetMapping("/main/items/add")
    public Mono<String> addItemPage() {
        return Mono.just("add-item");
    }

    /**
     * POST "/main/items" - добавление товара
     *
     * @param item "multipart/form-data"
     * @return редирект на "/items/{id}"
     */
    @PostMapping("/main/items")
    public Mono<String> addItem(@ModelAttribute("item") Mono<ItemCreateDto> item) {
        return itemService.saveItem(item)
                .map(itemDto -> "redirect:/items/" + itemDto.getId());
    }



    private Mono<ItemDto> inspectRequest(Long id, ServerWebExchange exchange) {
        return exchange.getFormData()
                .map(MultiValueMap::toSingleValueMap)
                .map(map -> {
                    return map.get("action");
                })
                .map(action -> itemService.actionWithItemInCart(id, action))
                .flatMap(Function.identity());
    }
}