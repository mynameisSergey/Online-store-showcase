package com.example.online_shop.model.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "items_in_order")
public class ItemInOrder {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "items_in_order_generator")
    @SequenceGenerator(name = "items_in_order_generator", sequenceName = "items_in_order_seq", allocationSize = 1)
    private Long id;
    private String title;
    private int count;
    private BigDecimal price;
    private String description;
    @Column(name = "image_path")
    private String imagePath;
    @ManyToOne
    @JoinColumn(name = "order_id", nullable = false, referencedColumnName = "id")
    private Order order;
    @Column(name = "item_id")
    private Long itemId;

}
