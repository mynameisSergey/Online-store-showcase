package com.example.online_shop.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CartDto {
    @Builder.Default
    private Map<Long, ItemDto> items = new HashMap<>();
    @Builder.Default
    private BigDecimal total = BigDecimal.valueOf(0);
    @Builder.Default
    private boolean empty = true;
}
