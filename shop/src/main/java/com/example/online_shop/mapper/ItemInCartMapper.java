package com.example.online_shop.mapper;

import com.example.online_shop.model.dto.ItemDto;
import com.example.online_shop.model.dto.ItemInCartDto;
import com.example.online_shop.model.entity.ItemInCart;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class ItemInCartMapper {
    private final ModelMapper mapper;

    public ItemDto toItemDto(ItemInCartDto itemInCartDto) {
        Long itemId = itemInCartDto.getItemId();
        ItemDto itemDto = mapper.map(itemInCartDto, ItemDto.class);
        itemDto.setId(itemId);
        return itemDto;
    }

    public ItemDto toItemDto(ItemInCart item) {
        Long itemId = item.getItemId();
        ItemDto itemDto = mapper.map(item, ItemDto.class);
        itemDto.setId(itemId);
        return itemDto;
    }

    public List<ItemDto> toItemDto(List<ItemInCartDto> items) {
        return items.stream().map(this::toItemDto).toList();
    }

    public ItemInCartDto toItemInCartDto(ItemInCart item) {
        return mapper.map(item, ItemInCartDto.class);
    }

    public ItemInCart toItemInCart(ItemDto itemDto) {
        ItemInCart itemInCart = mapper.map(itemDto, ItemInCart.class);
        itemInCart.setId(null);
        itemInCart.setItemId(itemDto.getId());
        return itemInCart;
    }
}
