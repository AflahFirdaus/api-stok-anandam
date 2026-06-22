# Presence & Real-Time Activity Tracking System

## Backend Implementation Complete ✅

### Files Created/Modified

| File | Description |
|------|-------------|
| `config/WebSocketConfig.java` | **Modified** - Endpoint changed to `/ws-connect` |
| `config/RedisConfig.java` | **New** - RedisTemplate with Jackson JSON serializer |
| `config/WebSocketEventListener.java` | **New** - SessionConnectEvent & SessionDisconnectEvent handlers |
| `controller/UserActivityController.java` | **New** - @MessageMapping("/user-action") receiver |
| `service/UserSessionService.java` | **New** - Redis CRUD operations for user sessions |
| `service/AdminBroadcasterService.java` | **New** - @Scheduled broadcaster every 2s to /topic/admin-dashboard |
| `dto/UserSessionDto.java` | **New** - JSON model: userId, name, status, currentAction, lastActive |
| `pom.xml` | **Modified** - Added spring-boot-starter-data-redis & jackson-datatype-jsr310 |

---

## 🔴 Frontend (Flutter) Implementation Summary

Berikut adalah spesifikasi teknis yang harus diimplementasikan di Flutter:

---

### 1. Setup STOMP/WebSocket Dependency

Tambahkan ke `pubspec.yaml`:

```yaml
dependencies:
  stomp_dart_client: ^1.0.0
  # atau
  web_socket_channel: ^2.4.0  # alternatif manual
```

---

### 2. Service: StompService / WebsocketService

Buat service singleton untuk mengelola koneksi STOMP.

#### a. Connect (dipanggil setelah login)
```dart
// Endpoint: ws://YOUR_SERVER:9099/ws-connect
// Kirim headers userId & name saat CONNECT frame

StompClient(
  config: StompConfig(
    url: 'ws://192.168.1.176:9099/ws-connect',
    // custom headers yang akan dibaca backend
    onConnect: (frame) {
      // Kirim frame CONNECT dengan header
      frame.headers['userId'] = user.id;       // WAJIB
      frame.headers['name'] = user.fullName;    // WAJIB
    },
  ),
);
```

**Catatan Penting:**
- Header `userId` dan `name` WAJIB dikirim saat koneksi
- Backend membaca header ini untuk menyimpan session ke Redis
- Jika tidak dikirim, session tidak akan tercatat

#### b. Subscribe ke Admin Dashboard (halaman admin saja)
```dart
stompClient.subscribe(
  destination: '/topic/admin-dashboard',
  callback: (frame) {
    // Parsing JSON array
    final List<dynamic> sessions = jsonDecode(frame.body);
    // Update UI (daftar user online & aktivitas mereka)
  },
);
```

---

### 3. Kirim User Action (setiap pindah halaman)

Buat mekanisme untuk mengirim action setiap user berpindah halaman.

```dart
void sendUserAction({
  required String userId,
  required String currentAction,
}) {
  stompClient.send(
    destination: '/app/user-action',
    body: jsonEncode({
      'userId': userId,
      'currentAction': currentAction, // Contoh: "Halaman Penjualan", "Halaman Stok"
      // 'metadata': {}, // opsional
    }),
  );
}
```

**Kapan dipanggil:**
- Di setiap halaman `initState()` → kirim action sesuai halaman
- Gunakan `WidgetsBindingObserver` untuk deteksi app lifecycle (resume/pause)
- Contoh integrasi:

```dart
class SalesPage extends StatefulWidget { ... }

class _SalesPageState extends State<SalesPage> with WidgetsBindingObserver {
  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addObserver(this);
    _sendAction('Halaman Penjualan');
  }

  @override
  void dispose() {
    WidgetsBinding.instance.removeObserver(this);
    super.dispose();
  }

  @override
  void didChangeAppLifecycleState(AppLifecycleState state) {
    if (state == AppLifecycleState.resumed) {
      _sendAction('Halaman Penjualan');
    } else if (state == AppLifecycleState.paused) {
      // Aplikasi di-minimize → action akan timeout oleh Redis TTL 5 menit
    }
  }

  void _sendAction(String action) {
    stompService.sendAction(userId: user.id, currentAction: action);
  }
}
```

---

### 4. Pengelolaan Session & Reconnect

```dart
// Reconnect otomatis saat koneksi terputus
StompClient(
  config: StompConfig(
    url: 'ws://.../ws-connect',
    reconnectDelay: 3000, // reconnect setiap 3 detik
    // Headers untuk CONNECT frame (cara spesifik library)
    stompConnectHeaders: {
      'userId': userId,
      'name': userName,
    },
  ),
);
```

---

### 5. Format Data dari Backend

**Output dari `/topic/admin-dashboard` (setiap 2 detik):**
```json
[
  {
    "userId": "1",
    "name": "Aflah Firdaus",
    "status": "online",
    "currentAction": "Halaman Penjualan",
    "lastActive": "2026-06-19T09:00:00Z"
  },
  {
    "userId": "2",
    "name": "Admin",
    "status": "offline",
    "currentAction": "idle",
    "lastActive": "2026-06-19T08:55:00Z"
  }
]
```

---

### 6. Arsitektur yang Disarankan

```
lib/
  features/
    presence/
      services/
        stomp_service.dart          # STOMP connection manager
        presence_repository.dart     # Data layer
      bloc/
        active_users_bloc.dart       # BLoC untuk daftar user aktif
        active_users_event.dart
        active_users_state.dart
      screens/
        admin_dashboard_screen.dart  # Admin melihat daftar user & aktivitas
      widgets/
        user_activity_tile.dart      # Widget per-item user
```

---

### 7. Contoh Implementasi `StompService`

```dart
class StompService {
  static final StompService _instance = StompService._internal();
  factory StompService() => _instance;
  StompService._internal();

  StompClient? _client;
  final _userActivityController = StreamController<UserSession>.broadcast();
  
  Stream<UserSession> get userActivityStream => _userActivityController.stream;

  void connect({
    required String userId,
    required String name,
    required String wsUrl,
  }) {
    _client = StompClient(
      config: StompConfig(
        url: wsUrl,
        stompConnectHeaders: {
          'userId': userId,
          'name': name,
        },
        onConnect: (frame) {
          // Subscribe ke admin dashboard
          _client!.subscribe(
            destination: '/topic/admin-dashboard',
            callback: (frame) {
              final sessions = (jsonDecode(frame.body!) as List)
                .map((e) => UserSession.fromJson(e))
                .toList();
              // Emit ke stream / bloc
            },
          );
        },
        onDisconnect: (frame) {
          // Handle disconnect
        },
        reconnectDelay: 3000,
      ),
    );
    _client!.activate();
  }

  void sendAction({required String userId, required String currentAction}) {
    _client?.send(
      destination: '/app/user-action',
      body: jsonEncode({
        'userId': userId,
        'currentAction': currentAction,
      }),
    );
  }

  void disconnect() {
    _client?.deactivate();
  }
}
```

---

### 8. REST Endpoint untuk Cek Status (Alternatif)

Jika perlu REST API untuk polling (fallback), bisa ditambahkan ke backend:

```
GET /api/v1/presence/active-users  → List<UserSessionDto>
```

Tapi disarankan menggunakan WebSocket karena real-time.

---

### 9. Environment Variables (Backend)

Pastikan Redis server berjalan dan dikonfigurasi di `application.properties`:

```properties
# Redis Configuration
spring.redis.host=${REDIS_HOST:localhost}
spring.redis.port=${REDIS_PORT:6379}
```

Redis harus di-install di server (atau gunakan Redis Cloud / Docker).

---

### 10. Flow Diagram

```
FLUTTER APP                          BACKEND (Spring Boot)              REDIS
    |                                      |                            |
    |-- CONNECT /ws-connect --------------->|                            |
    |   (headers: userId, name)             |                            |
    |                                      |-- SAVE user_session:xxx -->|
    |                                      |   (TTL: 5 menit)           |
    |                                      |                            |
    |-- SEND /app/user-action ------------>|                            |
    |   {userId, currentAction}            |                            |
    |                                      |-- UPDATE user_session:xxx->|
    |                                      |   (reset TTL 5 menit)      |
    |                                      |                            |
    |                                      |  (Scheduler @ 2 detik)     |
    |                                      |-- GET ALL user_session:* ->|
    |                                      |<- List<UserSessionDto> ----|
    |                                      |                            |
    |<-- BROADCAST /topic/admin-dashboard --|                            |
    |   (array of user sessions)           |                            |
    |                                      |                            |
    |-- DISCONNECT ----------------------->|                            |
    |                                      |-- DELETE user_session:xxx->|
```

---

### Yang Perlu Disiapkan untuk Development:

1. **Install Redis** di lokal (atau Docker): `docker run -p 6379:6379 redis`
2. **Tambahkan env** `REDIS_HOST=localhost` di environment
3. **Testing** menggunakan `stomp-dart-client` atau `wscat` dari command line
4. **Debugging**: log backend dengan level `TRACE` untuk melihat detail broadcast

Ada pertanyaan lebih lanjut atau butuh bantuan implementasi salah satu bagian?