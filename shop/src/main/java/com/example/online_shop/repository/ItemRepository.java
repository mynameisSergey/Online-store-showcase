package com.example.online_shop.repository;

import com.example.online_shop.model.entity.Item;
import org.springframework.data.domain.Pageable;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import reactor.core.publisher.Flux;

public interface ItemRepository extends R2dbcRepository<Item, Long> {
    Flux<Item> getItemsByTitleLike(String search, Pageable page);
    Flux<Item> findBy(Pageable pageable);
}
