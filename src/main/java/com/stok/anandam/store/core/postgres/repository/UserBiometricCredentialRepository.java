package com.stok.anandam.store.core.postgres.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.stok.anandam.store.core.postgres.model.UserBiometricCredential;

import java.util.Optional;

@Repository
public interface UserBiometricCredentialRepository extends JpaRepository<UserBiometricCredential, Long> {
    Optional<UserBiometricCredential> findByDeviceIdAndActiveTrue(String deviceId);
    Optional<UserBiometricCredential> findByDeviceId(String deviceId);
}