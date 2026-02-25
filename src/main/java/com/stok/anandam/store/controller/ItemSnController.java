package com.stok.anandam.store.controller;

import com.stok.anandam.store.dto.ItemSerialNumberResponse;
import com.stok.anandam.store.dto.WebResponse;
import com.stok.anandam.store.service.ItemSnService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/sn")
public class ItemSnController {

        @Autowired
        private ItemSnService itemSnService;

        /**
         * GET /api/sn/masuk
         * Filter: search (global), docId, user, itemName, sn, startDate, endDate
         * Sort: sortBy = tanggal | docId | user | itemName | sn, direction = asc | desc
         * Pagination: page, size
         */
        @GetMapping("/masuk")
        public ResponseEntity<WebResponse<List<ItemSerialNumberResponse>>> getSnMasuk(
                        @RequestParam(name = "search", required = false) String search,
                        @RequestParam(name = "docId", required = false) String docId,
                        @RequestParam(name = "user", required = false) String user,
                        @RequestParam(name = "itemName", required = false) String itemName,
                        @RequestParam(name = "sn", required = false) String sn,
                        @RequestParam(name = "startDate", required = false) String startDate,
                        @RequestParam(name = "endDate", required = false) String endDate,
                        @RequestParam(name = "sortBy", defaultValue = "tanggal") String sortBy,
                        @RequestParam(name = "direction", defaultValue = "desc") String direction,
                        @RequestParam(name = "size", defaultValue = "10") int size,
                        @RequestParam(name = "page", defaultValue = "0") int page) {

                List<ItemSerialNumberResponse> data = itemSnService.getSnData(
                                "MASUK", search, docId, user, itemName, sn, startDate, endDate, sortBy, direction, size,
                                page * size);
                return ResponseEntity.ok(WebResponse.<List<ItemSerialNumberResponse>>builder()
                                .status(200)
                                .message("Success Fetch SN Masuk")
                                .data(data)
                                .paging(null)
                                .build());
        }

        /**
         * GET /api/sn/keluar
         * Filter: search (global), docId, user, itemName, sn, startDate, endDate
         * Sort: sortBy = tanggal | docId | user | itemName | sn, direction = asc | desc
         * Pagination: page, size
         */
        @GetMapping("/keluar")
        public ResponseEntity<WebResponse<List<ItemSerialNumberResponse>>> getSnKeluar(
                        @RequestParam(name = "search", required = false) String search,
                        @RequestParam(name = "docId", required = false) String docId,
                        @RequestParam(name = "user", required = false) String user,
                        @RequestParam(name = "itemName", required = false) String itemName,
                        @RequestParam(name = "sn", required = false) String sn,
                        @RequestParam(name = "startDate", required = false) String startDate,
                        @RequestParam(name = "endDate", required = false) String endDate,
                        @RequestParam(name = "sortBy", defaultValue = "tanggal") String sortBy,
                        @RequestParam(name = "direction", defaultValue = "desc") String direction,
                        @RequestParam(name = "size", defaultValue = "10") int size,
                        @RequestParam(name = "page", defaultValue = "0") int page) {

                List<ItemSerialNumberResponse> data = itemSnService.getSnData(
                                "KELUAR", search, docId, user, itemName, sn, startDate, endDate, sortBy, direction,
                                size, page * size);
                return ResponseEntity.ok(WebResponse.<List<ItemSerialNumberResponse>>builder()
                                .status(200)
                                .message("Success Fetch SN Keluar")
                                .data(data)
                                .paging(null)
                                .build());
        }
}