package com.stok.anandam.store.service;

import com.stok.anandam.store.dto.AddressResponse;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.ArrayList;
import java.util.List;

@Service
public class LocationService {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public LocationService(RestTemplate restTemplate, ObjectMapper objectMapper) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
    }

    public List<AddressResponse> searchAddress(String query) {
        List<AddressResponse> resultList = new ArrayList<>();

        // 1. URL Builder yang aman dari spasi atau karakter aneh (URL Encoding)
        String url = UriComponentsBuilder.fromUriString("https://photon.komoot.io/api/")
                .queryParam("q", query)
                .queryParam("limit", 5)
                .toUriString();

        // 2. Set Header untuk Keamanan (User-Agent wajib diisi nama app Anda)
        HttpHeaders headers = new HttpHeaders();
        headers.set("User-Agent", "BackendApp_Springboot/1.0");
        HttpEntity<String> entity = new HttpEntity<>(headers);

        try {
            // 3. Tembak API Photon
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                // 4. Parsing JSON kotor menjadi rapi
                JsonNode root = objectMapper.readTree(response.getBody());
                JsonNode features = root.path("features");

                for (JsonNode feature : features) {
                    JsonNode properties = feature.path("properties");
                    JsonNode geometry = feature.path("geometry").path("coordinates");

                    // Ambil properti
                    String name = properties.path("name").asText("");
                    String street = properties.path("street").asText("");
                    String city = properties.path("city").asText("");
                    String state = properties.path("state").asText("");
                    String postcode = properties.path("postcode").asText("Tidak tersedia");

                    // Rangkai alamat lengkap (mengabaikan yang kosong)
                    String fullAddress = String.join(", ", 
                        List.of(street, city, state).stream().filter(s -> !s.isEmpty()).toList()
                    );

                    // Ambil koordinat [longitude, latitude] dari GeoJSON
                    Double lon = geometry.get(0) != null ? geometry.get(0).asDouble() : 0.0;
                    Double lat = geometry.get(1) != null ? geometry.get(1).asDouble() : 0.0;

                    resultList.add(new AddressResponse(name, fullAddress, postcode, lat, lon));
                }
            }
        } catch (Exception e) {
            // Log error di server, tapi jangan biarkan aplikasi meledak
            System.err.println("Gagal memanggil API Lokasi: " + e.getMessage());
        }

        return resultList;
    }
}