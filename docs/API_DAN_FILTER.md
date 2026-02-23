# Daftar API & Filter

## Keamanan & Filter Global

- **JwtFilter**: Semua request ke `/api/**` (kecuali login, refresh, swagger, actuator) memerlukan header `Authorization: Bearer <token>`.
- **Path tanpa auth**: `/api/v1/auth/login`, `/api/v1/auth/refresh`, `/swagger-ui/**`, `/v3/api-docs/**`, `OPTIONS /api/**` (CORS preflight).
- **Role ADMIN**: Hanya `/api/v1/users/**` yang memerlukan role `ADMIN`.

---

## Daftar API

### Auth
| Method | Path | Auth | Keterangan |
|--------|------|------|------------|
| POST | `/api/v1/auth/login` | Tidak | Login, dapat access + refresh token |
| POST | `/api/v1/auth/refresh` | Tidak | Tukar refresh token → access token baru |
| GET | `/api/v1/auth/me` | Ya (JWT) | Data user yang login |

### Users (ADMIN only)
| Method | Path | Auth | Filter/Pagination |
|--------|------|------|-------------------|
| GET | `/api/v1/users` | ADMIN | `page`, `size` |
| POST | `/api/v1/users` | ADMIN | - |
| PUT | `/api/v1/users/{id}` | ADMIN | - |
| DELETE | `/api/v1/users/{id}` | ADMIN | - |

### Activity Logs
| Method | Path | Auth | Filter |
|--------|------|------|--------|
| GET | `/api/v1/activity-logs` | JWT | `page`, `size`, `sortBy`, `direction`, **`username`**, **`action`** (bisa kombinasi) |
| GET | `/api/v1/activity-logs/{id}` | JWT | - |

**SortBy** yang diizinkan: `id`, `timestamp`, `username`, `action`, `details`, `ipAddress`.

### Purchases
| Method | Path | Auth | Filter |
|--------|------|------|--------|
| GET | `/api/v1/purchases` | JWT | `page`, `size`, `sortBy`, `dir`, **`startDate`**, **`endDate`**, **`search`** (par_name, item_name, dll) |

Format tanggal: `yyyy-MM-dd`.

### Sales
| Method | Path | Auth | Filter |
|--------|------|------|--------|
| GET | `/api/v1/sales` | JWT | `page`, `size`, `sortBy`, `direction`, **`startDate`**, **`endDate`**, **`empCode`**, **`search`** |

### Stock
| Method | Path | Auth | Filter |
|--------|------|------|--------|
| GET | `/api/v1/stock` atau `/api/v1/stocks` | JWT | `page`, `size`, `sortBy`, `direction`, **`search`** (item code atau nama) |
| GET | `/api/v1/stock/{id}` | JWT | - |

### TKDN
| Method | Path | Auth | Filter |
|--------|------|------|--------|
| GET | `/api/v1/tkdn` | JWT | `page`, `size`, `sortBy`, `direction`, **`isTkdn`** (true/false), **`kategori`**, **`search`** (nama, no merek, kategori) |
| GET | `/api/v1/tkdn/{id}` | JWT | - |

### Canvasing
| Method | Path | Auth | Filter |
|--------|------|------|--------|
| GET | `/api/v1/canvasing` | JWT | `page`, `size`, `sortBy`, `direction`, **`search`** (nama instansi, kabupaten, kecamatan) |

### Data Canvasing
| Method | Path | Auth | Filter |
|--------|------|------|--------|
| POST | `/api/v1/data-canvasing` | JWT | Body: DataCanvasingRequest |
| GET | `/api/v1/data-canvasing` | JWT | `page`, `size`, `sortBy`, `direction`, **`startDate`**, **`endDate`**, **`search`** (nama instansi) |

### Serial Number (SN)
| Method | Path | Auth | Filter & Sort |
|--------|------|------|----------------|
| GET | `/api/sn/masuk` | JWT | Lihat bawah |
| GET | `/api/sn/keluar` | JWT | Lihat bawah |

**Filter (semua optional):**
- **`search`** – Pencarian global (sn, doc_id, user, item_name)
- **`docId`** – Filter nomor dokumen (partial)
- **`user`** – Filter nama user/par_name (partial)
- **`itemName`** – Filter nama barang (partial)
- **`sn`** – Filter serial number (partial)
- **`startDate`**, **`endDate`** – Rentang tanggal (format `yyyy-MM-dd`)

**Sort:** **`sortBy`** = `tanggal` \| `docId` \| `user` \| `itemName` \| `sn`, **`direction`** = `asc` \| `desc` (default: `tanggal`, `desc`).

**Pagination:** `page`, `size`.

### Dashboard
| Method | Path | Auth |
|--------|------|------|
| GET | `/api/v1/dashboard/summary` | JWT |

### Migration (butuh `app.mysql.enabled=true`)
| Method | Path | Auth |
|--------|------|------|
| POST | `/api/v1/migration/purchase` | JWT |
| POST | `/api/v1/migration/sales` | JWT |
| POST | `/api/v1/migration/stock` | JWT |
| POST | `/api/v1/migration/canvasing` | JWT |
| POST | `/api/v1/migration/tkdn` | JWT |
| POST | `/api/v1/migration/sn` | JWT |

---

## Ringkasan Perubahan (Pengecekan & Optimalisasi)

1. **ItemSnService**: Filter tanggal pakai kolom asli (`p.doc_date` / `s.doc_date`), bukan alias `tanggal`, agar valid di MySQL.
2. **JwtFilter**: Path publik (login, refresh, swagger, actuator) tidak lagi melalui JWT parsing → lebih ringan dan aman.
3. **SecurityConfig**: OPTIONS `/api/**` di-permit untuk CORS preflight; penjelasan matcher API diperjelas.
4. **WebConfig (CORS)**: Origin eksplisit (localhost + 127.0.0.1), method termasuk PATCH, `exposedHeaders("Authorization")`, `allowCredentials(true)` untuk frontend yang pakai kredensial.
5. **ActivityLogController**: `sortBy` dibatasi ke field yang diizinkan agar aman dan konsisten.
6. **GlobalExceptionHandler**: Penanganan `DateTimeParseException` untuk parameter tanggal invalid → response 400 dengan pesan format `yyyy-MM-dd`.

Semua API di atas dapat dijalankan dengan JWT (kecuali yang permitAll). Semua filter query (tanggal, search, kategori, dll.) terhubung ke service/repository dan berfungsi optimal.
