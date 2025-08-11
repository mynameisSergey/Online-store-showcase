package com.example.online_shop.service;

import com.example.online_shop.model.dto.CartDto;
import com.example.online_shop.model.dto.ItemDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class CartService {
    private final CartDto cart;

    public int getItemCountInCart(Long itemId) { // СЧИТАЕТ КОЛИЧЕСТВО ТОВАРА В КОРЗИНЕ ПО АЙДИ
        if (cart.getItems().containsKey(itemId)) return cart.getItems().get(itemId).getCount();
        return 0;
    }

    public void clearCart() {
        cart.setItems(new HashMap<>());
        cart.setTotal(BigDecimal.valueOf(0));
        cart.setEmpty(true);
    }

    public CartDto getCart() {
        return cart;
    }

    public Map<Long, ItemDto> getItemsInCart() {
        return cart.getItems();
    }

    public void refresh(Map<Long, ItemDto> itemsInCart, ItemDto itemInCart) {
        cart.setItems(itemsInCart);
        cart.setEmpty(itemsInCart.size() == 0);
        if (itemsInCart.size() > 0) cart.setTotal(cart.getTotal().add(itemInCart.getPrice()));
        else cart.setTotal(BigDecimal.valueOf(0));

    }
}
