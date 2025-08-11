package com.example.online_shop.model.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class ItemsWithPagingDto {
    List<List<ItemDto>> items;
    private PagingParametersDto paging;
}
