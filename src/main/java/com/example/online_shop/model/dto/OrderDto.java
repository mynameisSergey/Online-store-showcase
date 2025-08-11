package com.example.online_shop.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class OrderDto {
    private Long id;
    private List<ItemDto> items;
    @Builder.Default
    private BigDecimal totalSum = BigDecimal.valueOf(0);
}
