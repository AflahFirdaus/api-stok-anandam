package com.stok.anandam.store.dto;

import com.stok.anandam.store.core.postgres.model.Role;
import lombok.Data;

@Data
public class UserResponse {
    private Long id;
    private String nama;
    private String username;
    private Role role;

}