package com.stok.anandam.store.core.postgres.repository;

import com.stok.anandam.store.core.postgres.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    // Mencari user berdasarkan username (untuk login)
    Optional<User> findByUsername(String username);

    boolean existsByUsername(String username);
}