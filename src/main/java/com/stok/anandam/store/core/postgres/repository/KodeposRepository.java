package com.stok.anandam.store.core.postgres.repository;

import com.stok.anandam.store.core.postgres.model.Kodepos;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface KodeposRepository extends JpaRepository<Kodepos, Integer> {
    List<Kodepos> findByKodePosContainingOrDesaKelurahanContainingIgnoreCase(String kodePos, String desaKelurahan);
    
    Optional<Kodepos> findFirstByKodePos(String kodePos);
}
