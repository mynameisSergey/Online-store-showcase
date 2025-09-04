package com.example.online_shop.repository;

import com.example.online_shop.model.entity.ItemInCart;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface ItemInCartRepository extends R2dbcRepository<ItemInCart, Long> {
    Mono<Void> deleteByLoginIgnoreCase(String login);

    Mono<Void> deleteByItemIdAndLoginIgnoreCase(Long itemId, String login);

    Mono<ItemInCart> getByItemIdAndLoginIgnoreCase(Long itemId, String Login);

    Flux<ItemInCart> getByLoginIgnoreCase(String login);
}
