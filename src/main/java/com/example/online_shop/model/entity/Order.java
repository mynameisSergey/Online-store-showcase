package com.example.online_shop.model.entity;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("orders")
public class Order {
    @Id
    private Long id;
    @Column("total_sum")
    private BigDecimal totalSum;

}
