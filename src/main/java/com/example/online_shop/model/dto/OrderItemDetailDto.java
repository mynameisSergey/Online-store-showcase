package com.example.online_shop.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class OrderItemDetailDto {
    private Long orderId;
    private Long id;
    private String title;
    private String description;
    private String imagePath;
    private int count;
    @Builder.Default
    private BigDecimal price = BigDecimal.valueOf(0);
    @Builder.Default
    private BigDecimal totalSum = BigDecimal.valueOf(0);

}
