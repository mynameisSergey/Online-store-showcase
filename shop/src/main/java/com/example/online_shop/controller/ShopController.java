package com.example.online_shop.controller;

import com.example.online_shop.model.dto.CartDto;
import com.example.online_shop.model.dto.ItemCreateDto;
import com.example.online_shop.model.dto.ItemDto;
import com.example.online_shop.model.dto.ItemsWithPagingDto;
import com.example.online_shop.service.CartService;
import com.example.online_shop.service.ItemService;
import com.example.online_shop.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PostAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.Principal;
import java.util.function.Function;

@Controller
@RequiredArgsConstructor
public class ShopController {
    private final ItemService itemService;
    private final OrderService orderService;
    private final CartService cartService;
    private final PaymentsService paymentsService;
    private final UserService userService;

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
    public Mono<String> getItems(Model model, Principal principal,
                                 @RequestParam(defaultValue = "", name = "search") String search,
                                 @RequestParam(defaultValue = "NO", name = "sort") String sort,
                                 @RequestParam(defaultValue = "1", name = "pageNumber") int pageNumber,
                                 @RequestParam(defaultValue = "10", name = "pageSize") int pageSize) {

        model.addAttribute("items", itemService.getItems(search, sort, pageNumber, pageSize,
                principal == null ? "" : principal.getName()));
        model.addAttribute("search", search);
        model.addAttribute("sort", sort);
        model.addAttribute("paging", itemService.getPaging(search, sort, pageNumber, pageSize));
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
    public Mono<String> getChart(Model model, Principal principal) {
        return cartService.getCart(principal == null ? "" : principal.getName())
                .doOnNext(cart -> model.addAttribute("items", cart.getItems().values()))
                .doOnNext(cart -> model.addAttribute("total", cart.getTotal()))
                .doOnNext(cart -> model.addAttribute("empty", cart.isEmpty()))
                .zipWith(paymentsService.getBalance().onErrorReturn(BigDecimal.valueOf(-1)).log(), (cart, balance) ->
                        model.addAttribute("canBuy", balance.compareTo(cart.getTotal()) >= 0))
                .map(cart -> "cart");
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
                .onErrorComplete() // игнор ошибки
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
    public Mono<String> getItem(@PathVariable("id") Long id, Model model, Principal principal) {
        return itemService.getItemDtoById(id, principal == null ? "" : principal.getName())
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
                .map(itemDto -> "redirect:/items/" + id);
    }

    /**
     * POST "/buy" - купить товары в корзине (выполняет покупку товаров в корзине и очищает ее)
     *
     * @return редирект на "/orders/{id}?newOrder=true"
     */
    @PostMapping("/buy")
    public Mono<String> buy(Principal principal) {
        return orderService.buy(principal == null ? "" : principal.getName())
                .map(id -> "redirect:/orders/" + id + "?newOrder=true")
                .onErrorReturn("redirect:/error?message="
                        + URLEncoder.encode("Недостаточно средств. Пополните счёт и повторите попытку"));
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
    @GetMapping("/admin/items/add")
    @PostAuthorize("hasRole('ADMIN')")
    public Mono<String> addItemPage() {
        return Mono.just("add-item");
    }

    /**
     * POST "/main/items" - добавление товара
     *
     * @param item "multipart/form-data"
     * @return редирект на "/items/{id}"
     */
    @PostMapping("/admin/items/add")
    @PostAuthorize("hasRole('ADMIN')")
    public Mono<String> addItem(@ModelAttribute("item") Mono<ItemCreateDto> item) {
        return itemService.saveItem(item)
                .map(itemDto -> "redirect:/items/" + itemDto.getId());
    }


    @GetMapping("/signup")
    public Mono<String> addUserPage() {
        return Mono.just("add-user");
    }

    /**
     * POST "/signup" - создание аккаунта
     *
     * @param "login"    - название товара
     * @param "password" - текст товара
     * @return редирект на форму логина "/login"
     */

    @PostMapping("/signup")
    public Mono<String> addUser(@ModelAttribute("user") Mono<NewUserDto> user) throws UnsupportedEncodingException {
        return userService.addUser(user)
                .log()
                .onErrorReturn("redirect:/error?message="
                        + URLEncoder.encode("пользователь с таким логином уже существует", StandardCharsets.UTF_8))
                .map(login -> "redirect:/login");
    }

    /**
     * GET "/error" - страница сообщения об ошибке
     *
     * @return шаблон "error.html"
     */
    @GetMapping("/error")
    public Mono<String> getError(Model model,
                                 @RequestParam(defaultValue = "Повторите операцию позже", name = "message") String message)
            throws UnsupportedEncodingException {
        model.addAttribute("message", URLDecoder.decode(message, StandardCharsets.UTF_8));
        return Mono.just("error");
    }

    private Mono<ItemDto> inspectRequest(Long id, ServerWebExchange exchange) {
        return exchange.getFormData()
                .map(MultiValueMap::toSingleValueMap)
                .map(map -> map.get("action"))
                .zipWith(exchange.getPrincipal().map(Principal::getName), (action, login)
                        -> itemService.actionWithItemInCart(id, action, login))
                .flatMap(Function.identity());
    }
}