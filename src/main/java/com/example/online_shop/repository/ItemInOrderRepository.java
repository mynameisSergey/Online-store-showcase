package com.example.online_shop.repository;

import com.example.online_shop.model.entity.ItemInOrder;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ItemInOrderRepository extends JpaRepository<ItemInOrder, Long> {
}
