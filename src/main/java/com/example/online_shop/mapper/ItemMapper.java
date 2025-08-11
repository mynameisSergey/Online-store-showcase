package com.example.online_shop.mapper;

import com.example.online_shop.model.dto.ItemCreateDto;
import com.example.online_shop.model.dto.ItemDto;
import com.example.online_shop.model.entity.Item;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class ItemMapper {

    private final ModelMapper mapper;

    @Value("${shop.image.path}")
    private String imagePath;

    public ItemDto toDto(Item item) {
        ItemDto dto = mapper.map(item, ItemDto.class);
        dto.setImagePath(imagePath + dto.getId());
        return dto;
    }

    public List<ItemDto> toListDto(Page<Item> entities) {
        return entities.stream().map(this::toDto).toList();
    }

    public Item toItem(ItemCreateDto dto) {
        Item item = mapper.map(dto, Item.class);
        if (item != null) {
            try {
                if (dto.getImage() != null && !dto.getImage().isEmpty())
                    item.setImage(dto.getImage().getBytes());
            } catch (IOException e) {
                return item;
            }
        }
        return item;

    }
}
