package com.stok.anandam.store.core.postgres.repository;

import com.stok.anandam.store.core.postgres.model.StatusServis;
import com.stok.anandam.store.core.postgres.model.TransaksiServis;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Specification builder untuk filter fleksibel pada TransaksiServis.
 * Mendukung kombinasi filter status, search (noServis & namaPelanggan),
 * dan filter masa garansi.
 */
public class TransaksiServisSpecification {

    /**
     * Mencari berdasarkan noServis (LIKE %search%) ATAU namaPelanggan (LIKE %search%).
     */
    public static Specification<TransaksiServis> searchByNoServisOrNamaPelanggan(String search) {
        return (root, query, criteriaBuilder) -> {
            if (search == null || search.trim().isEmpty()) {
                return criteriaBuilder.conjunction(); // no filter
            }

            String pattern = "%" + search.trim().toLowerCase() + "%";

            // LIKE pada noServis
            Predicate noServisLike = criteriaBuilder.like(
                    criteriaBuilder.lower(root.get("noServis")), pattern);

            // LIKE pada namaPelanggan (join ke tabel pelanggan)
            Predicate namaPelangganLike = criteriaBuilder.like(
                    criteriaBuilder.lower(
                            root.join("pelanggan", JoinType.LEFT).get("namaPelanggan")),
                    pattern);

            return criteriaBuilder.or(noServisLike, namaPelangganLike);
        };
    }

    /**
     * Filter berdasarkan status terkini.
     */
    public static Specification<TransaksiServis> hasStatus(StatusServis status) {
        return (root, query, criteriaBuilder) -> {
            if (status == null) {
                return criteriaBuilder.conjunction();
            }
            return criteriaBuilder.equal(root.get("statusTerkini"), status);
        };
    }

    /**
     * Filter garansi aktif:
     * statusTerkini = SUDAH_DIAMBIL AND tglBatasGaransi IS NOT NULL AND tglBatasGaransi > sekarang
     */
    public static Specification<TransaksiServis> isGaransiAktif(LocalDateTime sekarang) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            predicates.add(criteriaBuilder.equal(root.get("statusTerkini"), StatusServis.SUDAH_DIAMBIL));
            predicates.add(criteriaBuilder.isNotNull(root.get("tglBatasGaransi")));
            predicates.add(criteriaBuilder.greaterThan(root.get("tglBatasGaransi"), sekarang));

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }

    /**
     * Filter garansi expired:
     * statusTerkini = SUDAH_DIAMBIL AND tglBatasGaransi IS NOT NULL AND tglBatasGaransi <= sekarang
     */
    public static Specification<TransaksiServis> isGaransiExpired(LocalDateTime sekarang) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            predicates.add(criteriaBuilder.equal(root.get("statusTerkini"), StatusServis.SUDAH_DIAMBIL));
            predicates.add(criteriaBuilder.isNotNull(root.get("tglBatasGaransi")));
            predicates.add(criteriaBuilder.lessThanOrEqualTo(root.get("tglBatasGaransi"), sekarang));

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }
}