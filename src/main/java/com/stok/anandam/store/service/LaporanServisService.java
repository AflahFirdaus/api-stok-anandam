package com.stok.anandam.store.service;

import com.stok.anandam.store.core.postgres.model.StatusServis;
import com.stok.anandam.store.core.postgres.repository.TransaksiServisRepository;
import com.stok.anandam.store.dto.LaporanServisKeuanganResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class LaporanServisService {

    private final TransaksiServisRepository transaksiRepository;

    public LaporanServisKeuanganResponse getLaporanKeuanganServis(LocalDate start, LocalDate end) {
        var list = transaksiRepository.findByStatusTerkiniAndCreatedAtBetween(
                StatusServis.SUDAH_DIAMBIL, start.atStartOfDay(), end.atTime(23, 59, 59));

        BigDecimal omzet = list.stream()
                .map(t -> t.getBiayaFinal() != null ? t.getBiayaFinal() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal modal = list.stream()
                .map(t -> t.getModalSparepart() != null ? t.getModalSparepart() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return LaporanServisKeuanganResponse.builder()
                .periode(start + " sampai " + end)
                .totalTransaksiServis(list.size())
                .totalOmzetServis(omzet)
                .totalModalSparepart(modal)
                .labaBersihServis(omzet.subtract(modal))
                .build();
    }
}