package com.example.online_shop.model.entity;

import lombok.*;
import org.springframework.data.relational.core.mapping.Table;

@Data
@Builder
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
@Table("users")
public class User {
    private String login;
    private String password;
    private String roles;
}
