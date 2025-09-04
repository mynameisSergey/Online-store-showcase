package com.example.online_shop.model.entity;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.math.BigDecimal;

@Data
@Builder
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
@Table("items_in_cart")
public class ItemInCart {
    @Id
    private Long id;
    private String title;
    private int count;
    private BigDecimal price;
    private String description;
    @Column("image_path")
    private String imagePath;
    private String login;
    @Column("item_id")
    private Long itemId;
}
