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
public class ItemInCartDto {
    private Long id;
    private String title;
    private int count;
    private BigDecimal price;
    private String description;
    private String imagePath;
    private String login;
    private Long itemId;
}
