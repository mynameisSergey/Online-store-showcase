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
public class OrderDto implements Comparable<OrderDto> {
    private Long id;
    private List<ItemDto> items;
    private BigDecimal totalSum;

    @Override
    public int compareTo(OrderDto o) {
        return Long.compare(o.id, this.id);

    }
}
