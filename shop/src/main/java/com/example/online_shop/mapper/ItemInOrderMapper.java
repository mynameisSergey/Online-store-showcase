package com.example.online_shop.mapper;

import com.example.online_shop.model.dto.ItemDto;
import com.example.online_shop.model.entity.ItemInOrder;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

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

    public ItemDto toDto(ItemInOrder item) {
        ItemDto itemInOrder = mapper.map(item, ItemDto.class);
        itemInOrder.setId(item.getItemId());
        return itemInOrder;
    }

    public Flux<ItemDto> toItemInOrderListFromFlux(Flux<ItemInOrder> entities) {
        return entities.map(this::toDto);
    }

    public List<ItemDto> toItemDtoList(List<ItemInOrder> entities) {
        return entities.stream().map(this::toDto).toList();
    }

    public List<ItemInOrder> toItemInOrderList(List<ItemDto> entities) {
        return entities.stream().map(this::toItemInOrder).toList();
    }

}
