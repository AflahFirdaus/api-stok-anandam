package com.stok.anandam.store.controller;

import com.stok.anandam.store.core.postgres.model.Shbj;
import com.stok.anandam.store.dto.PagingResponse;
import com.stok.anandam.store.dto.WebResponse;
import com.stok.anandam.store.service.ShbjService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/shbj")
public class ShbjController {

    @Autowired
    private ShbjService shbjService;

    // GET /api/v1/shbj?page=0&size=10&search=amplas
    @GetMapping
    public ResponseEntity<WebResponse<List<Shbj>>> getAllShbj(
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "10") int size,
            @RequestParam(name = "sortBy", defaultValue = "id") String sortBy,
            @RequestParam(name = "direction", defaultValue = "asc") String direction,
            @RequestParam(name = "search", required = false) String search) {

        Page<Shbj> pageResult = shbjService.getShbjData(page, size, sortBy, direction, search);

        // Jika halaman kosong karena page terlalu besar, lempar ke page 0 (opsional, meniru logic SalesController)
        if (pageResult.getContent().isEmpty() && pageResult.getTotalElements() > 0 && page > 0) {
            pageResult = shbjService.getShbjData(0, size, sortBy, direction, search);
            page = 0;
        }

        // Susun PagingResponse
        PagingResponse paging = PagingResponse.builder()
                .currentPage(page)
                .totalPage(pageResult.getTotalPages())
                .size(size)
                .totalItem(pageResult.getTotalElements())
                .build();

        // Kembalikan Response
        return ResponseEntity.ok(WebResponse.<List<Shbj>>builder()
                .status(200)
                .message("Success Fetch SHBJ Data")
                .data(pageResult.getContent())
                .paging(paging)
                .build());
    }

    // GET /api/v1/shbj/categories (Bonus endpoint jika Anda butuh filter per kategori)
    @GetMapping("/categories")
    public ResponseEntity<WebResponse<List<String>>> getCategories() {
        List<String> categories = shbjService.getKategoriList();
        
        return ResponseEntity.ok(WebResponse.<List<String>>builder()
                .status(200)
                .message("Success Fetch Categories")
                .data(categories)
                .build());
    }
}
