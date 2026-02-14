package com.stok.anandam.store.core.postgres.repository;

import com.stok.anandam.store.core.postgres.model.RefreshToken;
import com.stok.anandam.store.core.postgres.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {
    Optional<RefreshToken> findByToken(String token);

    // Untuk fitur Logout (hapus token)
    @Modifying
    int deleteByUser(User user);

    Optional<RefreshToken> findByUser(User user);
}