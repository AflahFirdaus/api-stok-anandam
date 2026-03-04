package com.stok.anandam.store.controller;

import com.stok.anandam.store.core.postgres.model.OldItemSerialNumber;
import com.stok.anandam.store.core.postgres.model.OldPurchase;
import com.stok.anandam.store.core.postgres.model.OldSales;
import com.stok.anandam.store.dto.PagingResponse;
import com.stok.anandam.store.dto.SalesSummaryResponse;
import com.stok.anandam.store.dto.WebResponse;
import com.stok.anandam.store.service.OldDataService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/old-data")
public class OldDataController {

        @Autowired
        private OldDataService oldDataService;

        @GetMapping("/sales")
        public ResponseEntity<WebResponse<SalesSummaryResponse<OldSales>>> getAllSales(
                        @RequestParam(name = "page", defaultValue = "0") int page,
                        @RequestParam(name = "size", defaultValue = "100") int size,
                        @RequestParam(name = "sortBy", defaultValue = "docDate") String sortBy,
                        @RequestParam(name = "direction", defaultValue = "desc") String direction,
                        @RequestParam(name = "startDate", required = false) String startDate,
                        @RequestParam(name = "endDate", required = false) String endDate,
                        @RequestParam(name = "empCode", required = false) String empCode,
                        @RequestParam(name = "search", required = false) String search) {
                SalesSummaryResponse<OldSales> data = oldDataService.getSales(
                                page, size, sortBy, direction, startDate, endDate, empCode, search);

                PagingResponse paging = PagingResponse.builder()
                                .currentPage(page)
                                .totalPage(data.getTotalPages())
                                .size(size)
                                .totalItem(data.getTotalElements())
                                .build();

                return ResponseEntity.ok(WebResponse.<SalesSummaryResponse<OldSales>>builder()
                                .status(200)
                                .message("Success Fetch Old Sales Data")
                                .data(data)
                                .paging(paging)
                                .build());
        }

        @GetMapping("/purchase")
        public ResponseEntity<WebResponse<SalesSummaryResponse<OldPurchase>>> getAllPurchase(
                        @RequestParam(name = "page", defaultValue = "0") int page,
                        @RequestParam(name = "size", defaultValue = "100") int size,
                        @RequestParam(name = "sortBy", defaultValue = "docDate") String sortBy,
                        @RequestParam(name = "direction", defaultValue = "desc") String direction,
                        @RequestParam(name = "startDate", required = false) String startDate,
                        @RequestParam(name = "endDate", required = false) String endDate,
                        @RequestParam(name = "search", required = false) String search) {
                SalesSummaryResponse<OldPurchase> data = oldDataService.getPurchase(
                                page, size, sortBy, direction, startDate, endDate, search);

                PagingResponse paging = PagingResponse.builder()
                                .currentPage(page)
                                .totalPage(data.getTotalPages())
                                .size(size)
                                .totalItem(data.getTotalElements())
                                .build();

                return ResponseEntity.ok(WebResponse.<SalesSummaryResponse<OldPurchase>>builder()
                                .status(200)
                                .message("Success Fetch Old Purchase Data")
                                .data(data)
                                .paging(paging)
                                .build());
        }

        @GetMapping("/item-sn")
        public ResponseEntity<WebResponse<List<OldItemSerialNumber>>> getAllItemSn(
                        @RequestParam(name = "page", defaultValue = "0") int page,
                        @RequestParam(name = "size", defaultValue = "100") int size,
                        @RequestParam(name = "sortBy", defaultValue = "tanggal") String sortBy,
                        @RequestParam(name = "direction", defaultValue = "desc") String direction,
                        @RequestParam(name = "type", required = false) String type,
                        @RequestParam(name = "search", required = false) String search,
                        @RequestParam(name = "startDate", required = false) String startDate,
                        @RequestParam(name = "endDate", required = false) String endDate) {
                Page<OldItemSerialNumber> pageResult = oldDataService.getItemSn(
                                page, size, sortBy, direction, type, search, startDate, endDate);

                PagingResponse paging = PagingResponse.builder()
                                .currentPage(page)
                                .totalPage(pageResult.getTotalPages())
                                .size(size)
                                .totalItem(pageResult.getTotalElements())
                                .build();

                return ResponseEntity.ok(WebResponse.<List<OldItemSerialNumber>>builder()
                                .status(200)
                                .message("Success Fetch Old Item SN Data")
                                .data(pageResult.getContent())
                                .paging(paging)
                                .build());
        }

        @GetMapping("/employee-codes")
        public ResponseEntity<WebResponse<List<String>>> getEmployeeCodes() {
                List<String> codes = oldDataService.getEmployeeCodes();
                return ResponseEntity.ok(WebResponse.<List<String>>builder()
                                .status(200)
                                .message("Success fetch employee codes")
                                .data(codes)
                                .build());
        }

        @GetMapping("/meta")
        public ResponseEntity<WebResponse<java.util.Map<String, Object>>> getDateRangeMeta() {
                java.util.Map<String, Object> meta = oldDataService.getDateRangeMeta();
                return ResponseEntity.ok(WebResponse.<java.util.Map<String, Object>>builder()
                                .status(200)
                                .message("Success fetch date range meta")
                                .data(meta)
                                .build());
        }
}
