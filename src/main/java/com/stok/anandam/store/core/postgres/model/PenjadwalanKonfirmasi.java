package com.stok.anandam.store.core.postgres.model;

import com.stok.anandam.store.core.postgres.model.enums.StatusJadwal;
import com.stok.anandam.store.core.postgres.model.enums.TipeTugas;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction; // Gunakan @Where("deleted_at IS NULL") jika Anda pakai Hibernate 5
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.annotations.NotFound;
import org.hibernate.annotations.NotFoundAction;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.hibernate.proxy.HibernateProxy;


import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "penjadwalan_konfirmasi", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"memo_id", "tipe_tugas", "deleted_at"}),
    @UniqueConstraint(columnNames = {"request_delivery_id", "tipe_tugas", "deleted_at"})
})
// Implementasi Soft Delete otomatis
@SQLDelete(sql = "UPDATE penjadwalan_konfirmasi SET deleted_at = CURRENT_TIMESTAMP WHERE id = ?")
@SQLRestriction("deleted_at IS NULL") // Penting: Ubah ke @Where(clause = "deleted_at IS NULL") jika memakai Spring Boot 2.x / Hibernate 5
public class PenjadwalanKonfirmasi {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "memo_id")
    private Memo memo; 

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "request_delivery_id")
    private RequestDelivery requestDelivery;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "penjadwalan_memos",
        joinColumns = @JoinColumn(name = "penjadwalan_id"),
        inverseJoinColumns = @JoinColumn(name = "memo_id")
    )
    @Builder.Default
    private List<Memo> manifestMemos = new ArrayList<>();

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "penjadwalan_requests",
        joinColumns = @JoinColumn(name = "penjadwalan_id"),
        inverseJoinColumns = @JoinColumn(name = "request_delivery_id")
    )
    @Builder.Default
    private List<RequestDelivery> manifestRequests = new ArrayList<>();

    @Enumerated(EnumType.STRING)
    @Column(name = "tipe_tugas", nullable = false)
    private TipeTugas tipeTugas;

    @Column(name = "personel_id")
    private Long personelId;

    @Column(name = "tanggal_jadwal")
    private LocalDate tanggalJadwal;

    @Builder.Default
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "items_dikirim", columnDefinition = "jsonb")
    private java.util.List<java.util.Map<String, Object>> itemsDikirim = new java.util.ArrayList<>();

    @Column(name = "alamat_lengkap", columnDefinition = "text")
    private String alamatLengkap;

    @Column(name = "alamat_maps")
    private String alamatMaps;

    @Column(name = "id_kodepos")
    private Integer idKodepos;

    @Column(name = "latitude")
    private Double latitude;

    @Column(name = "longitude")
    private Double longitude;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "id_kodepos", insertable = false, updatable = false)
    @NotFound(action = NotFoundAction.IGNORE)
    private Kodepos kodepos;



    @Column(name = "estimasi_waktu")
    private String estimasiWaktu; 

    @Enumerated(EnumType.STRING)
    @Column(name = "status_jadwal")
    private StatusJadwal statusJadwal;

    @Column(name = "foto_bukti")
    private String fotoBukti;

    @Column(columnDefinition = "text")
    private String catatan; 

    @Column(name = "nama_penerima")
    private String namaPenerima;

    @Column(name = "catatan_operasional", columnDefinition = "text")
    private String catatanOperasional;
    @Builder.Default
    @Column(name = "is_urgen")
    private Boolean isUrgen = false;

    @Builder.Default
    @Column(name = "is_expedition_outlet")
    private Boolean isExpeditionOutlet = false;

    @Column(name = "manual_customer_name")
    private String manualCustomerName;

    @Column(name = "manual_no_hp")
    private String manualNoHp;

    @Column(name = "marketing_name")
    private String marketingName;

    @Column(name = "kecamatan")
    private String kecamatan;

    @Column(name = "desa_kelurahan")
    private String desaKelurahan;

    @Column(name = "kabupaten_kota")
    private String kabupatenKota;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    // --- CUSTOM EQUALS & HASHCODE UNTUK JPA ---
    // Mencegah error pada proxy Hibernate dan memastikan entitas dibandingkan hanya berdasarkan ID-nya.
    
    @Override
    public final boolean equals(Object o) {
        if (this == o) return true;
        if (o == null) return false;
        Class<?> oEffectiveClass = o instanceof HibernateProxy ? ((HibernateProxy) o).getHibernateLazyInitializer().getPersistentClass() : o.getClass();
        Class<?> thisEffectiveClass = this instanceof HibernateProxy ? ((HibernateProxy) this).getHibernateLazyInitializer().getPersistentClass() : this.getClass();
        if (thisEffectiveClass != oEffectiveClass) return false;
        PenjadwalanKonfirmasi that = (PenjadwalanKonfirmasi) o;
        return getId() != null && Objects.equals(getId(), that.getId());
    }

    @Override
    public final int hashCode() {
        return this instanceof HibernateProxy ? ((HibernateProxy) this).getHibernateLazyInitializer().getPersistentClass().hashCode() : getClass().hashCode();
    }
}