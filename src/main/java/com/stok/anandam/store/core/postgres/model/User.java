package com.stok.anandam.store.core.postgres.model;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nama;

    @Column(unique = true, nullable = false)
    private String username;

    @Column(nullable = false)
    private String password; // Nanti ini akan diisi password yang sudah di-encrypt (BCrypt)

    @Enumerated(EnumType.STRING) // Agar tersimpan sebagai tulisan "ADMIN", bukan angka 0
    @Column(nullable = false)
    private Role role;
}
