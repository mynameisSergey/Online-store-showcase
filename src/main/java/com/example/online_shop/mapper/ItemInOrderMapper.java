package com.example.online_shop.mapper;

import com.example.online_shop.model.dto.ItemDto;
import com.example.online_shop.model.dto.OrderItemDetailDto;
import com.example.online_shop.model.entity.ItemInOrder;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class ItemInOrderMapper {

    private final ModelMapper mapper;

    public ItemInOrder toItemInOrder(ItemDto item) {
        ItemInOrder itemInOrder = mapper.map(item, ItemInOrder.class);
        itemInOrder.setId(null);
        itemInOrder.setItemId(item.getId());
        return itemInOrder;
    }

    public List<ItemInOrder> toItemInOrderList(List<ItemDto> entities) {
        return entities.stream().map(this::toItemInOrder).toList();
    }

    public ItemDto toItemInOrder(OrderItemDetailDto item) {
        return mapper.map(item, ItemDto.class);
    }

    public List<ItemDto> toItemDtoList(List<OrderItemDetailDto> entities) {
        return entities.stream().map(this::toItemInOrder).toList();
    }
}
