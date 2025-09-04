package com.example.online_shop.service;

import com.example.online_shop.enumiration.ECartAction;
import com.example.online_shop.mapper.ItemInCartMapper;
import com.example.online_shop.model.dto.CartDto;
import com.example.online_shop.model.dto.ItemDto;
import com.example.online_shop.model.entity.ItemInCart;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class CartService {
    private final ItemInCartService itemInCartService;
    private final ItemInCartMapper itemInCartMapper;

    public Mono<Void> clearCart(String login) {
        return itemInCartService.deleteByLogin(login);
    }

    public Mono<CartDto> getCart(String login) {
        log.info("Start getCart: login={}", login);
        return itemInCartService.getByLogin(login)
                .log()
                .map(itemsInCartList -> CartDto.builder()
                        .items(itemsInCartList.stream()
                                .map(itemInCartMapper::toItemInCartDto)
                                .map(itemInCartMapper::toItemDto)
                                .collect(Collectors.toMap(ItemDto::getId, item -> item)))
                        .empty(itemsInCartList.isEmpty())
                        .login(login)
                        .total(itemsInCartList.stream()
                                .map(item -> item.getPrice().multiply(BigDecimal.valueOf(item.getCount())))
                                .reduce(BigDecimal.ZERO, BigDecimal::add))
                        .build());
    }

    public Mono<Integer> getItemCountInCart(Long itemId, String login) { // СЧИТАЕТ КОЛИЧЕСТВО ТОВАРА В КОРЗИНЕ ПО АЙДИ
        return login == null || login.isBlank() ? Mono.just(0) :
                itemInCartService.getByItemIdAndLogin(itemId, login).log()
                        .map(ItemInCart::getCount).log();
    }

    public Mono<Map<Long, ItemDto>> getItemsInCart(String login) {
        return getCart(login).map(CartDto::getItems);
    }

    public Mono<ItemDto> refresh(ItemInCart itemDto, String action, String login) {
        log.info("Start refresh: itemDto={}, action={}, login={}", itemDto, action, login);
        switch (ECartAction.valueOf(action.toUpperCase())) {
            case PLUS -> itemDto.setCount(itemDto.getCount() + 1);
            case MINUS -> {
                if (itemDto.getCount() >= 1) itemDto.setCount(itemDto.getCount() - 1);
            }
            case DELETE -> itemDto.setCount(0);
        }
        if (itemDto.getCount() == 0)
            return itemInCartService.removeItemFromCart(itemDto.getItemId(), login)
                    .then(Mono.defer(() -> Mono.just(itemDto)
                            .map(itemInCartMapper::toItemDto)
                            .map(item -> {
                                item.setCount(0);
                                return item;
                            })));
        return itemInCartService.changeItemCountInCart(itemDto, action, login)
                .map(itemInCartMapper::toItemDto);
    }
}
