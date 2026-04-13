package com.stok.anandam.store.core.postgres.repository;

import com.stok.anandam.store.core.postgres.model.RequestDelivery;
import com.stok.anandam.store.core.postgres.model.Role;
import com.stok.anandam.store.core.postgres.model.enums.RequestDeliveryStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RequestDeliveryRepository extends JpaRepository<RequestDelivery, Long> {

    Optional<RequestDelivery> findByNomorRequest(String nomorRequest);

    List<RequestDelivery> findAllByOrderByCreatedAtDesc();

    List<RequestDelivery> findByStatusOrderByCreatedAtDesc(RequestDeliveryStatus status);

    @Query("SELECT r FROM RequestDelivery r JOIN r.creator c WHERE c.role = :role ORDER BY r.createdAt DESC")
    List<RequestDelivery> findByCreatorRoleOrderByCreatedAtDesc(@Param("role") Role role);

    @Query("SELECT r FROM RequestDelivery r JOIN r.creator c WHERE c.role IN :roles ORDER BY r.createdAt DESC")
    List<RequestDelivery> findByCreatorRoleIn(@Param("roles") List<Role> roles);

    Optional<RequestDelivery> findTopByOrderByCreatedAtDesc();
}
