# Pembaruan API Backend → Panduan Integrasi Flutter (Stok Anandam)

Dokumen ini menjelaskan **semua pembaruan API** dan cara mengintegrasikannya di **Flutter**. Backend telah dicek (linter bersih); pastikan frontend mengikuti struktur dan perilaku di bawah.

---

## 1. Base URL & Auth

- **Base URL:** `http://<host>:<port>` (mis. `http://localhost:8080` atau URL server production).
- **Prefix:** Sebagian besar API memakai `/api/v1/...`. Pengecualian: **Serial Number** memakai `/api/sn` (tanpa `v1`).
- **Header untuk endpoint terproteksi:**  
  `Authorization: Bearer <access_token>`

---

## 2. Struktur Response Seragam (Penting)

**Semua** response API (sukses dan error) mengikuti bentuk yang sama agar Flutter bisa parse satu model.

### 2.1 Bentuk Umum (WebResponse)

```json
{
  "status": 200,
  "message": "Success ...",
  "data": { ... },
  "paging": { ... } | null
}
```

| Field     | Tipe    | Keterangan |
|----------|---------|------------|
| `status` | int     | Kode HTTP (200, 201, 400, 401, 404, 500, dll). |
| `message`| string  | Pesan untuk user (bisa ditampilkan di UI). |
| `data`   | object / array / null | Payload. Untuk list: **array**. Untuk error: object detail error. |
| `paging` | object / null | Metadata pagination; **selalu ada** (bisa `null`). Untuk list dengan paging, **selalu object**. |

### 2.2 Paging (jika ada)

```json
{
  "currentPage": 0,
  "totalPage": 5,
  "size": 10,
  "totalItem": 48
}
```

- **Tanpa pagination** (detail, options, summary): `paging` = `null`.
- **Dengan pagination**: gunakan `paging.currentPage` untuk tampilan “halaman ke-X” dan untuk request halaman berikutnya.

### 2.3 Error Response

Bentuk tetap **sama**: `status`, `message`, `data`, `paging`.

- `status`: 400, 401, 404, 409, 500, dll.
- `message`: pesan singkat untuk user (baik untuk snackbar/toast).
- `data`: object detail error (untuk log/debug), isi contoh:

```json
{
  "timestamp": "2024-01-15T10:30:00",
  "status": 404,
  "error": "Not Found",
  "message": "Stock not found with id: 999",
  "path": "/api/v1/stock/999",
  "fieldErrors": {}
}
```

- Validasi (400): `fieldErrors` bisa berisi `{ "fieldName": "pesan error" }`.
- **Di Flutter:** selalu cek `response.status >= 400` untuk anggap error; tampilkan `response.message`; untuk form validation pakai `response.data?.fieldErrors`.

---

## 3. Perilaku Pagination Setelah Search/Filter

Agar list tidak kosong setelah user mengubah search/filter:

- Jika **total hasil** berkurang (mis. dari 100 jadi 5) dan user masih di **halaman besar** (mis. 5), backend **otomatis mengembalikan halaman pertama** (page 0) beserta data.
- `paging.currentPage` akan disesuaikan (bisa jadi 0).
- **Rekomendasi di Flutter:** saat user mengubah **search** atau **filter**, kirim ulang request dengan **page = 0** (jangan pertahankan nomor halaman lama).
- Jika `paging.totalItem == 0` dan `data` array kosong, tampilkan pesan: *"Tidak ada data yang cocok."*

---

## 4. Daftar Endpoint & Pembaruan

### 4.1 Stock (Stok)

| Method | Path | Keterangan |
|--------|------|------------|
| GET | `/api/v1/stock` atau `/api/v1/stocks` | List stok (paging + filter). |
| GET | `/api/v1/stock/summary-by-category` | Ringkasan per kategori (flat). |
| GET | `/api/v1/stock/summary-by-category/hierarchy` | **Baru.** Ringkasan 2 level (parent + children). |
| GET | `/api/v1/stock/{id}` | Detail stok by ID. |

**GET list stok – query params:**

| Param | Tipe | Default | Keterangan |
|-------|------|---------|------------|
| page | int | 0 | Halaman (0-based). |
| size | int | 10 | Jumlah per halaman. |
| sortBy | string | "itemName" | Field: itemName, itemCode, kategoriNama, kategoriItemcode, finalStok, grandTotal, warehouse, id. |
| direction | string | "asc" | "asc" atau "desc". |
| search | string | - | Cari di nama item & kode item. |
| **kategori** | string | - | **Baru.** Filter by kategori (kategori_itemcode). |
| **warehouse** | string | - | **Baru.** Filter by warehouse. |

**Response list:** `data` = array of Stock, `paging` = object.

**GET summary-by-category:**  
- Query: `groupBy` = "kategori_itemcode" (default) atau "kategori_nama".  
- `data`: array `{ nama, stok, presentase }`, baris terakhir `nama: "TOTAL"`, `presentase: 100`.

**GET summary-by-category/hierarchy (baru):**  
- `data`: array item dengan struktur:
  - `nama`: string (nama parent atau sub)
  - `stok`: number (parent = jumlah dari children)
  - `presentase`: number
  - `children`: array of object yang sama (sub kategori); kosong `[]` untuk child/standalone.
- Contoh: BRANDED (parent) → children: PCAIO, PCBU, PCMINI. Baris terakhir tetap TOTAL.

---

### 4.2 Sales (Penjualan)

| Method | Path | Keterangan |
|--------|------|------------|
| GET | `/api/v1/sales` | List penjualan (paging + filter). |
| GET | `/api/v1/sales/employee-codes` | Daftar kode karyawan (untuk dropdown filter). |

**GET list sales – query params:**

| Param | Tipe | Default | Keterangan |
|-------|------|---------|------------|
| page | int | 0 | |
| size | int | 10 | |
| sortBy | string | "docDate" | |
| direction | string | "desc" | |
| startDate | string | - | Format yyyy-MM-dd. |
| endDate | string | - | Format yyyy-MM-dd. |
| empCode | string | - | Filter by kode karyawan. |
| search | string | - | Pencarian. |

**Response list:**  
`data`: object `{ totalGrandSum, content: List<Sales>, totalPages, totalElements }`.  
`paging`: object (currentPage, totalPage, size, totalItem).

---

### 4.3 Purchase (Pembelian)

| Method | Path | Keterangan |
|--------|------|------------|
| GET | `/api/v1/purchases` | List pembelian (paging + filter). |

**Query params:** page, size, sortBy, dir (bukan direction), startDate, endDate, search.

**Response list:**  
`data`: object `{ totalGrandSum, content: List<Purchase>, totalPages, totalElements }`.  
`paging`: object.

---

### 4.4 Canvasing (Master Instansi/Toko)

| Method | Path | Keterangan |
|--------|------|------------|
| GET | `/api/v1/canvasing` | List canvasing (paging + filter). |
| GET | `/api/v1/canvasing/options` | Options ringan (id, namaInstansi) untuk dropdown. |

**GET list – query params:**

| Param | Tipe | Default | Keterangan |
|-------|------|---------|------------|
| page | int | 0 | |
| size | int | 10 | |
| sortBy | string | "namaInstansi" | |
| direction | string | "asc" | |
| search | string | - | Nama instansi, kabupaten, kecamatan. |
| **kategori** | string | - | **Baru.** Filter kategori. |
| **provinsi** | string | - | **Baru.** Filter provinsi. |

**GET options:** query `search`, `limit` (default 50). `data` = array `{ id, namaInstansi }`, `paging` = null.

---

### 4.5 Data Canvasing (Kunjungan)

| Method | Path | Keterangan |
|--------|------|------------|
| POST | `/api/v1/data-canvasing` | Tambah data kunjungan. |
| GET | `/api/v1/data-canvasing` | List kunjungan (paging + filter). |

**GET list – query params:**

| Param | Tipe | Default | Keterangan |
|-------|------|---------|------------|
| page | int | 0 | |
| size | int | 10 | |
| sortBy | string | "tanggal" | |
| direction | string | "desc" | |
| startDate | string | - | yyyy-MM-dd. |
| endDate | string | - | yyyy-MM-dd. |
| search | string | - | Nama instansi (relasi). |
| **canvasingId** | long | - | **Baru.** Filter by ID toko/instansi. |
| **canvasVisit** | string | - | **Baru.** Filter tipe: "Canvas" / "Visit". |

**Response list:** `data` = array of DataCanvasing (bukan object Page), `paging` = object.

---

### 4.6 TKDN

| Method | Path | Keterangan |
|--------|------|------------|
| GET | `/api/v1/tkdn` | List TKDN (banyak filter). |
| GET | `/api/v1/tkdn/categories` | Daftar kategori unik (dropdown). |
| GET | `/api/v1/tkdn/filter-options` | Alternatif: `{ "kategori": ["A","B",...] }`. |
| GET | `/api/v1/tkdn/{id}` | Detail TKDN. |

**GET list – query params:**  
page, size, sortBy, direction, isTkdn, kategori, search, processor, ram, ssd, hdd, vga, layar, os.

**Response list:** `data` = array of Tkdn, `paging` = object.

---

### 4.7 User

| Method | Path | Keterangan |
|--------|------|------------|
| GET | `/api/v1/users` | List user (paging + filter). |
| POST | `/api/v1/users` | Create user. |
| PUT | `/api/v1/users/{id}` | Update user. |
| DELETE | `/api/v1/users/{id}` | Hapus user. |

**GET list – query params:**

| Param | Tipe | Default | Keterangan |
|-------|------|---------|------------|
| page | int | 0 | |
| size | int | 10 | |
| **search** | string | - | **Baru.** Cari nama & username. |
| **role** | string | - | **Baru.** Filter: ADMIN, SUPERVISOR, MARKETING, GUDANG, NOTA, DELIVERY, TEKNISI. |

**Response list:** `data` = array of UserResponse, `paging` = object.

---

### 4.8 Activity Log

| Method | Path | Keterangan |
|--------|------|------------|
| GET | `/api/v1/activity-logs` | List log (filter username, action). |
| GET | `/api/v1/activity-logs/{id}` | Detail log. |

**GET list:** page, size, sortBy, direction, username, action. `data` = array, `paging` = object.

---

### 4.9 Serial Number (path tanpa v1)

| Method | Path | Keterangan |
|--------|------|------------|
| GET | `/api/sn/masuk` | List SN masuk. |
| GET | `/api/sn/keluar` | List SN keluar. |

**Query params (keduanya):** search, docId, user, itemName, sn, startDate, endDate, sortBy (default "tanggal"), direction (default "desc"), size (default 10), page (default 0).  
**Response:** `data` = array, `paging` = null (endpoint ini belum mengembalikan paging object; bisa tetap pakai response model yang sama).

---

### 4.10 Dashboard

| Method | Path | Keterangan |
|--------|------|------------|
| GET | `/api/v1/dashboard/summary` | Summary dashboard. |

**Response:** `data` = object DashboardResponse, `paging` = null.

---

### 4.11 Auth

| Method | Path | Keterangan |
|--------|------|------------|
| POST | `/api/v1/auth/login` | Login → accessToken, refreshToken. |
| POST | `/api/v1/auth/refresh` | Refresh token. |
| GET | `/api/v1/auth/me` | User saat ini (perlu Bearer). |

Semua response: `data` object, `paging` = null.

---

### 4.12 Migration

| Method | Path | Keterangan |
|--------|------|------------|
| POST | `/api/v1/migration/purchase` | Trigger migrasi purchase. |
| POST | `/api/v1/migration/sales` | Trigger migrasi sales. |
| POST | `/api/v1/migration/stock` | Trigger migrasi stock. |
| POST | `/api/v1/migration/canvasing` | Trigger migrasi canvasing. |
| POST | `/api/v1/migration/tkdn` | Trigger migrasi TKDN. |
| POST | `/api/v1/migration/sn` | Trigger migrasi SN. |

Response: `data` string (mis. "Processing..."), `paging` = null.

---

## 5. Model Flutter yang Disarankan

### 5.1 Response wrapper (generic)

```dart
class WebResponse<T> {
  final int status;
  final String message;
  final T? data;
  final PagingResponse? paging;

  WebResponse({
    required this.status,
    required this.message,
    this.data,
    this.paging,
  });

  factory WebResponse.fromJson(
    Map<String, dynamic> json,
    T Function(dynamic)? fromJsonT,
  ) {
    return WebResponse(
      status: json['status'] as int,
      message: json['message'] as String? ?? '',
      data: json['data'] != null && fromJsonT != null
          ? fromJsonT(json['data'])
          : json['data'] as T?,
      paging: json['paging'] != null
          ? PagingResponse.fromJson(json['paging'] as Map<String, dynamic>)
          : null,
    );
  }

  bool get isSuccess => status >= 200 && status < 300;
}

class PagingResponse {
  final int currentPage;
  final int totalPage;
  final int size;
  final int totalItem;

  PagingResponse({
    required this.currentPage,
    required this.totalPage,
    required this.size,
    required this.totalItem,
  });

  factory PagingResponse.fromJson(Map<String, dynamic> json) {
    return PagingResponse(
      currentPage: (json['currentPage'] as num?)?.toInt() ?? 0,
      totalPage: (json['totalPage'] as num?)?.toInt() ?? 0,
      size: (json['size'] as num?)?.toInt() ?? 10,
      totalItem: (json['totalItem'] as num?)?.toInt() ?? 0,
    );
  }
}
```

### 5.2 Error detail (untuk form validation / debug)

```dart
class ApiErrorData {
  final String? timestamp;
  final int? status;
  final String? error;
  final String? message;
  final String? path;
  final Map<String, String>? fieldErrors;

  ApiErrorData.fromJson(Map<String, dynamic> json)
      : timestamp = json['timestamp'] as String?,
        status = json['status'] as int?,
        error = json['error'] as String?,
        message = json['message'] as String?,
        path = json['path'] as String?,
        fieldErrors = json['fieldErrors'] != null
            ? Map<String, String>.from(
                (json['fieldErrors'] as Map).map(
                  (k, v) => MapEntry(k.toString(), v.toString()),
                ),
              )
            : null;
}
```

### 5.3 Stock summary hierarchy (untuk tampilan 2 level)

```dart
class StockSummaryRow {
  final String nama;
  final num stok;
  final num presentase;
  final List<StockSummaryRow> children;

  StockSummaryRow({
    required this.nama,
    required this.stok,
    required this.presentase,
    this.children = const [],
  });

  factory StockSummaryRow.fromJson(Map<String, dynamic> json) {
    return StockSummaryRow(
      nama: json['nama'] as String? ?? '',
      stok: (json['stok'] as num?) ?? 0,
      presentase: (json['presentase'] as num?) ?? 0,
      children: (json['children'] as List<dynamic>?)
              ?.map((e) => StockSummaryRow.fromJson(e as Map<String, dynamic>))
              .toList() ??
          [],
    );
  }
}
```

---

## 6. Ringkasan Pembaruan untuk Flutter

1. **Response seragam**  
   Selalu parse ke `WebResponse<T>`. Cek `status` untuk sukses/error; tampilkan `message` untuk user; gunakan `paging` bila ada (bisa null).

2. **Paging**  
   Untuk list: gunakan `paging.currentPage`, `paging.totalPage`, `paging.totalItem`. Saat user mengubah search/filter, **reset page ke 0** sebelum request.

3. **Error**  
   Jika `status >= 400`, anggap error; tampilkan `message`. Untuk validasi form, baca `data` sebagai `ApiErrorData` dan gunakan `fieldErrors`.

4. **Endpoint baru**  
   - `GET /api/v1/stock/summary-by-category/hierarchy` → data ringkasan 2 level (parent + children). Gunakan untuk tabel bertingkat.

5. **Filter baru**  
   - Stock: `kategori`, `warehouse`  
   - Canvasing: `kategori`, `provinsi`  
   - Data Canvasing: `canvasingId`, `canvasVisit`  
   - User: `search`, `role`

6. **Data list**  
   - Data Canvasing list: `data` langsung array (bukan object dengan `content`).  
   - Sales/Purchase list: `data` object dengan `content` (array), `totalGrandSum`, `totalPages`, `totalElements`.

7. **Angka**  
   Backend memakai `BigDecimal`; di JSON bisa number atau string. Di Flutter parse aman (mis. `num.tryParse` atau konversi dari `dynamic`).

Gunakan dokumen ini sebagai acuan utama integrasi Flutter dengan backend Stok Anandam terbaru.
