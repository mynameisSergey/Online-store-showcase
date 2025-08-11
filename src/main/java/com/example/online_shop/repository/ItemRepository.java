package com.example.online_shop.repository;

import com.example.online_shop.model.entity.Item;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ItemRepository extends JpaRepository<Item, Long> {
    Page<Item> getItemsByTitleLike(String search, Pageable page);
}
