package com.example.online_shop.model.dto;

import com.example.online_shop.enumiration.EUserRole;
import org.springframework.security.core.GrantedAuthority;

public class RoleDto implements GrantedAuthority {
    private final EUserRole role;

    public RoleDto(EUserRole role) {
        this.role = role;
    }

    @Override
    public String getAuthority() {
        return role.name();
    }
}
