package com.example.online_shop.service;

import com.example.online_shop.model.entity.ItemInOrder;
import com.example.online_shop.repository.ItemInOrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ItemInOrderService {
    private final ItemInOrderRepository itemInOrderRepository;

    public void saveItemsInOrder(List<ItemInOrder> items) {
        itemInOrderRepository.saveAll(items);
    }
}
