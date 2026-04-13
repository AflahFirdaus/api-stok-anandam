import psycopg2
import requests
import time
import sys

# DATABASE CONFIGURATION
DB_CONFIG = {
    "dbname": "stok-anandam-local",
    "user": "postgres",
    "password": "123",
    "host": "localhost",
    "port": "5432",
    "sslmode": "prefer"
}

NOMINATIM_URL = "https://nominatim.openstreetmap.org/search"
HEADERS = {
    "User-Agent": "StokAnandam_Geocoding_Final/2.1 (aflah.dev@gmail.com)"
}

DIY_CENTER_LAT = "-7.8191000"
DIY_CENTER_LON = "110.2031000"

def get_coordinates(desa_raw, kec_raw, kab_raw):
    # 1. Standarisasi nama
    desa_std = desa_raw.strip().title()
    kec = kec_raw.strip().title()
    kab = kab_raw.strip().title()
    if "Kota" in kab or "Yogyakarta" in kab:
        kab = "Kota Yogyakarta"

    # 2. Buat versi tanpa spasi untuk desa (Contoh: "Bangun Kerto" -> "Bangunkerto")
    desa_no_space = desa_std.replace(" ", "")

    # 3. Daftar strategi (Urutan: Spesifik -> Tanpa Spasi -> Umum)
    strategies = [
        f"{desa_std}, {kec}, {kab}, DIY",           # Versi asli (Spesifik)
        f"{desa_no_space}, {kec}, {kab}, DIY",      # Versi tanpa spasi (Spesifik)
        f"Desa {desa_std}, {kec}, DIY",             # Pakai keyword 'Desa'
        f"Desa {desa_no_space}, {kec}, DIY",        # Pakai keyword 'Desa' + tanpa spasi
        f"{desa_std}, DIY",                         # Simple
        f"{desa_no_space}, DIY"                     # Simple + tanpa spasi
    ]

    # Hilangkan duplikat query jika desa_std == desa_no_space
    strategies = list(dict.fromkeys(strategies))

    for query in strategies:
        try:
            print(f"   Searching: {query}...")
            params = {"q": query, "format": "json", "limit": 1}
            response = requests.get(NOMINATIM_URL, params=params, headers=HEADERS, timeout=10)
            data = response.json()

            if data:
                lat = data[0]["lat"]
                lon = data[0]["lon"]
                
                # Cek agar tidak terjebak di titik pusat DIY
                if lat[:7] == DIY_CENTER_LAT[:7] and lon[:7] == DIY_CENTER_LON[:7]:
                    continue 
                
                return lat, lon
        except Exception as e:
            print(f"   Error API: {e}")
        
        # Jeda antar sub-query (OSM sangat ketat)
        time.sleep(1.2) 
    
    return None, None

def main():
    conn = None
    try:
        conn = psycopg2.connect(**DB_CONFIG)
        conn.set_client_encoding('UTF8')
        cur = conn.cursor()

        cur.execute("""
            SELECT id, desa_kelurahan, kecamatan, kabupaten_kota 
            FROM kode_pos_diy 
            WHERE latitude IS NULL OR longitude IS NULL
        """)
        records = cur.fetchall()
        total = len(records)
        print(f"--- Memulai Geocoding {total} data (Update: Anti-Spasi) ---")

        for index, (id_val, desa, kec, kab) in enumerate(records, 1):
            print(f"[{index}/{total}] Processing ID {id_val}: {desa}...")
            
            lat, lon = get_coordinates(desa, kec, kab)

            if lat and lon:
                cur.execute("""
                    UPDATE kode_pos_diy 
                    SET latitude = %s, longitude = %s 
                    WHERE id = %s
                """, (lat, lon, id_val))
                conn.commit()
                print(f"   ✓ SUCCESS: {lat}, {lon}")
            else:
                print(f"   × FAILED: Lokasi tidak ditemukan.")
            
            time.sleep(0.5)

        cur.close()
        print("\n--- Selesai! ---")

    except Exception as e:
        print(f"Database Error: {e}")
    finally:
        if conn: conn.close()

if __name__ == "__main__":
    main()