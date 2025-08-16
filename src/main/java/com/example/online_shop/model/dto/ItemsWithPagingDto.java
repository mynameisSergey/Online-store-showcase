package com.example.online_shop.model.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ItemsWithPagingDto {
    private List<List<ItemDto>> items;
    private PagingParametersDto paging;
}
