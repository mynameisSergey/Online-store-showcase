package com.example.online_shop.service;

import com.example.online_shop.model.entity.ItemInCart;
import com.example.online_shop.repository.ItemInCartRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ItemInCartService {
    private final ItemInCartRepository itemInCartRepository;

    @CacheEvict(cacheNames = "itemsInCart", key = "#login") //будет очищен кеш с именем "itemsInCart" по ключу = login.
    public Mono<Void> deleteByLogin(String login) {
        log.info("Start deleteByLogin: login={}", login);
        return itemInCartRepository.deleteByLoginIgnoreCase(login).then();
    }

    public Mono<ItemInCart> getByItemIdAndLogin(Long itemId, String login) {
        log.info("Start getByItemIdAndLogin: itemId={}, login={}", itemId, login);
        return itemInCartRepository.getByItemIdAndLoginIgnoreCase(itemId, login).log();
    }

    public Mono<Integer> getCountByItemIdAndLogin(Long itemId, String login) {
        log.info("Start getByItemIdAndLogin: itemId={}, login={}", itemId, login);
        return itemInCartRepository.getByItemIdAndLoginIgnoreCase(itemId, login).map(ItemInCart::getCount);
    }

    @Cacheable(cacheNames = "itemsInCart", key = "#login")
    public Mono<List<ItemInCart>> getByLogin(String login) {
        return itemInCartRepository.getByLoginIgnoreCase(login).collectList();
    }

    @CacheEvict(cacheNames = "itemsInCart", key = "#login")
    public Mono<ItemInCart> changeItemCountInCart(ItemInCart itemInCart, String action, String login) {
        return Mono.just(itemInCart).flatMap(itemInCartRepository::save);
    }

    @CacheEvict(cacheNames = "itemsInCart", key = "#login")
    public Mono<Void> removeItemFromCart(Long itemId, String login) {
        return itemInCartRepository.deleteByItemIdAndLoginIgnoreCase(itemId, login).log();
    }
}
