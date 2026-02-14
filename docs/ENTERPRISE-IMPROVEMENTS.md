# Rekomendasi Peningkatan ke Tingkat Enterprise

Dokumen ini berisi hasil pengecekan project dan rekomendasi agar aplikasi siap untuk standar enterprise (keamanan, maintainability, observability, dan operasional).

---

## 1. Keamanan (Security)

### 1.1 Rahasia jangan di file properties
**Masalah:** Password database dan JWT secret ada di `application.properties` dan berisiko ikut ter-commit.

**Rekomendasi:**
- Gunakan **environment variable** atau **Spring Cloud Config** / Vault.
- Buat `application.properties` hanya untuk konfigurasi non-rahasia; untuk rahasia gunakan:
  ```properties
  spring.datasource.pg.password=${DB_PASSWORD}
  jwt.secret=${JWT_SECRET}
  ```
- Tambahkan `application-example.properties` (tanpa nilai rahasia) sebagai template, dan masukkan `application.properties` ke `.gitignore` jika berisi rahasia.

### 1.2 CORS production
**Masalah:** `WebConfig` memakai `allowedOrigins("*")` dan `allowCredentials(false)` — untuk production sebaiknya daftar origin eksplisit.

**Rekomendasi:**
- Di production pakai daftar domain jelas, misalnya:
  ```java
  .allowedOrigins("https://app.company.com", "https://admin.company.com")
  ```
- Ambil daftar origin dari config/env, jangan hardcode `"*"` di production.

### 1.3 Rate limiting & brute-force
**Masalah:** Endpoint login tidak dibatasi; rentan brute-force.

**Rekomendasi:**
- Tambah **rate limiting** (mis. Bucket4j, atau filter custom) untuk `/api/auth/login`.
- Pertimbangkan lockout setelah N percobaan gagal (simpan di cache/DB).

### 1.4 CSRF
**Masalah:** CSRF disabled di semua chain. Untuk API-only (JWT) ini umum, tapi kalau ada form/session harus dipertimbangkan.

**Rekomendasi:** Tetap matikan CSRF hanya untuk API yang murni JWT; pastikan tidak ada endpoint state-changing yang mengandalkan cookie/session tanpa proteksi lain.

---

## 2. Konfigurasi (Configuration)

### 2.1 Profile (dev / staging / prod)
**Masalah:** Hanya satu `application.properties`; tidak ada pemisah environment.

**Rekomendasi:**
- Pakai profile: `application-dev.properties`, `application-prod.properties`.
- Aktifkan lewat env: `SPRING_PROFILES_ACTIVE=prod`.
- Di prod: matikan `spring.jpa.show-sql`, set `ddl-auto=validate` atau `none`, dan atur logging level.

### 2.2 Externalized config
**Rekomendasi:**
- Semua URL DB, secret, feature flag ambil dari env atau config server.
- Jangan hardcode IP/host di properties; gunakan placeholder + env.

---

## 3. Validasi Input (Validation)

### 3.1 Bean Validation pada DTO
**Masalah:** Request DTO (mis. `LoginUserRequest`, `UserRequest`) tidak pakai `@Valid` dan tidak ada anotasi validasi.

**Rekomendasi:**
- Tambah dependency (sudah ada `spring-boot-starter-validation`).
- Di DTO:
  ```java
  public class LoginUserRequest {
      @NotBlank(message = "Username wajib")
      private String username;
      @NotBlank(message = "Password wajib")
      private String password;
  }
  ```
- Di controller: `public ... login(@Valid @RequestBody LoginUserRequest request)`.
- Tambah handler di `GlobalExceptionHandler` untuk `MethodArgumentNotValidException` dan kembalikan 400 + daftar error field.

---

## 4. Logging

### 4.1 Ganti System.out / System.err
**Masalah:** Banyak `System.out.println` / `System.err.println` di MigrationService, DataSeeder, JwtFilter, ActivityLogAspect.

**Rekomendasi:**
- Pakai **SLF4J** di semua class:
  ```java
  private static final Logger log = LoggerFactory.getLogger(MigrationService.class);
  log.info("=== START MIGRASI PURCHASE ===");
  log.error("Error processing row: {}", e.getMessage());
  ```
- Di production, atur level per package (mis. `logging.level.com.stok.anandam=INFO`).

### 4.2 Jangan printStackTrace di global handler
**Masalah:** `GlobalExceptionHandler.handleGlobalException` memanggil `ex.printStackTrace()`.

**Rekomendasi:**
- Ganti dengan `log.error("Unexpected error", ex)` agar masuk ke log framework (dan bisa dikirim ke log aggregator).
- Jangan expose detail exception ke response body; tetap kembalikan pesan generik ke client.

---

## 5. API Design

### 5.1 Versioning API
**Masalah:** Semua endpoint di `/api/...` tanpa versi.

**Rekomendasi:**
- Prefix versi: `/api/v1/auth/login`, `/api/v1/users`, dll.
- Nanti bila ada breaking change bisa tambah `/api/v2/...` tanpa mengganggu client lama.

### 5.2 OpenAPI / Swagger
**Masalah:** Tidak ada dokumentasi API terstruktur.

**Rekomendasi:**
- Tambah **springdoc-openapi** (atau Springfox) dan expose UI di `/swagger-ui.html`.
- Dokumentasikan request/response dan error code; berguna untuk tim frontend dan integrasi.

### 5.3 Konsistensi error response
**Masalah:** Beberapa path melempar `RuntimeException` (mis. refresh token tidak ditemukan) tanpa melalui DTO error standar.

**Rekomendasi:**
- Gunakan custom exception (mis. `InvalidRefreshTokenException`) dan tangani di `GlobalExceptionHandler`, kembalikan format yang sama dengan `ApiErrorResponse`.
- Hindari `throw new RuntimeException(...)` di layer controller/service untuk error bisnis.

---

## 6. Monitoring & Operasional

### 6.1 Spring Boot Actuator
**Masalah:** Tidak ada endpoint health/metrics bawaan.

**Rekomendasi:**
- Tambah dependency: `spring-boot-starter-actuator`.
- Expose minimal: `management.endpoints.web.exposure.include=health,info` (prod jangan expose `*`).
- Health check dipakai load balancer/K8s untuk liveness/readiness.
- Nanti bisa tambah metrics (Prometheus) dan tracing (Sleuth/Micrometer).

### 6.2 Health per komponen
**Rekomendasi:**
- Aktifkan health detail (DB, disk) hanya lewat config:
  ```properties
  management.endpoint.health.show-details=when_authorized
  ```
- Pastikan health endpoint tidak bocorkan info sensitif.

---

## 7. Testing

### 7.1 Test coverage
**Masalah:** Hanya `StoreApplicationTests` (contextLoads); tidak ada unit test service atau integration test API.

**Rekomendasi:**
- **Unit test:** Service (AuthService, UserService, dll) dengan Mockito; mock repository.
- **Integration test:** Test REST endpoint dengan `@SpringBootTest` + `MockMvc` atau `TestRestTemplate`; gunakan profile `test` dan DB in-memory (H2) jika memungkinkan.
- Target coverage wajar untuk critical path (login, user CRUD, migrasi) > 70%.

### 7.2 Profile test
**Rekomendasi:**
- Buat `application-test.properties` (H2 atau Testcontainers) agar test tidak bergantung ke DB nyata.

---

## 8. Kualitas Kode

### 8.1 Dependency injection
**Masalah:** Banyak `@Autowired` di field; prefer constructor injection untuk immutability dan testability.

**Rekomendasi:**
- Gunakan constructor injection (bisa pakai `@RequiredArgsConstructor` Lombok) untuk dependency wajib.
- Lebih mudah di-test dan dependency jelas di satu tempat.

### 8.2 Repository
**Masalah:** `RefreshTokenRepository.deleteByUser(User user)` tidak punya `@Query`; derived delete mungkin tidak sesuai dengan nama method.

**Rekomendasi:**
- Tambah `@Query("DELETE FROM RefreshToken r WHERE r.user = :user")` dan `@Modifying` agar jelas dan konsisten dengan JPA.

---

## 9. Database

### 9.1 DDL di production
**Masalah:** `spring.jpa.hibernate.ddl-auto=update` bisa mengubah schema otomatis; risk untuk production.

**Rekomendasi:**
- Production: pakai `ddl-auto=validate` (atau `none`) dan kelola schema lewat **migration script** (Flyway/Liquibase).
- Dev: `update` masih bisa dipakai; pastikan prod punya migration yang teruji.

### 9.2 Show SQL
**Masalah:** `spring.jpa.show-sql=true` di semua environment; boros I/O dan bisa bocorkan data di log.

**Rekomendasi:**
- Matikan di prod; nyalakan hanya di dev lewat profile, atau pakai `logging.level.org.hibernate.SQL=DEBUG` saat perlu.

### 9.3 Connection pool
**Rekomendasi:**
- Atur HikariCP (max pool size, timeout, maxLifetime) lewat config sesuai beban; kurangi warning "Failed to validate connection" bila ada.

---

## 10. Dokumentasi & Repo

### 10.1 README
**Masalah:** Tidak ada README di root project.

**Rekomendasi:**
- Tambah README: cara build, run, set env (DB, JWT), profile (dev/prod), dan cara jalankan test.
- Sertakan contoh request/response untuk endpoint utama (atau link ke Swagger).

### 10.2 Changelog / release
**Rekomendasi:**
- Versioning jelas di `pom.xml` (hindari SNAPSHOT untuk release production).
- Changelog atau release notes untuk setiap rilis agar tim dan audit jelas.

---

## Prioritas Implementasi (disarankan)

| Prioritas | Item | Effort | Dampak |
|-----------|------|--------|--------|
| P0 | Rahasia pakai env variable | Kecil | Keamanan tinggi |
| P0 | Validasi input (@Valid + handler) | Kecil | Keamanan & stabilitas |
| P0 | Logging pakai SLF4J, hapus printStackTrace | Kecil | Observability |
| P1 | Profile dev/prod + application-*.properties | Sedang | Ops & keamanan |
| P1 | Actuator health (dan info) | Kecil | Ops & deployment |
| P1 | GlobalExceptionHandler pakai logger | Kecil | Observability |
| P2 | API versioning (/api/v1/) | Sedang | Maintainability |
| P2 | OpenAPI/Swagger | Sedang | Dokumentasi & integrasi |
| P2 | Unit + integration test | Besar | Kualitas & refactor aman |
| P2 | ddl-auto=validate + Flyway/Liquibase | Sedang | Stabilitas DB |
| P3 | Rate limiting login | Sedang | Keamanan |
| P3 | Constructor injection & README | Kecil–sedang | Maintainability |

---

Dengan langkah di atas, project akan lebih siap untuk deploy production dan standar enterprise (keamanan, konfigurasi, logging, testing, dan operasional).
