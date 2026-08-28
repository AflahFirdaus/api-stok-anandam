package com.stok.anandam.store.controller;

import com.stok.anandam.store.annotation.LogActivity;
import com.stok.anandam.store.core.postgres.model.enums.StatusJadwal;
import com.stok.anandam.store.core.postgres.model.enums.TipeTugas;
import com.stok.anandam.store.dto.*;
import com.stok.anandam.store.service.PenjadwalanService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping(value = {"/api/v1/penjadwalan", "/api/v1/penjadwalan/"})
@RequiredArgsConstructor
public class PenjadwalanController {

    private final PenjadwalanService penjadwalanService;

    // Endpoint untuk nampilin list tugas
    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public WebResponse<List<PenjadwalanResponse>> getListTugas(
            @RequestParam(name = "tipe", required = false) TipeTugas tipe,
            @RequestParam(name = "status", required = false) StatusJadwal status,
            java.security.Principal principal) {
        return penjadwalanService.getListTugas(tipe, status, principal.getName());
    }

    @GetMapping(path = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public WebResponse<PenjadwalanResponse> getTugasDetail(
            @PathVariable(name = "id") Long id,
            java.security.Principal principal) {
        return penjadwalanService.getTugasDetail(id, principal.getName());
    }

    @LogActivity("User melakukan update data penjadwalan")
    @RequestMapping(
            path = "/{id}",
            method = {RequestMethod.PUT, RequestMethod.POST},
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public WebResponse<String> update(
            @PathVariable(name = "id") Long id,
            @RequestBody @Validated UpdatePenjadwalanRequest request,
            java.security.Principal principal) {
        return penjadwalanService.updatePenjadwalan(id, request, principal.getName());
    }

    @LogActivity("User menyelesaikan tugas")
    @PostMapping(path = "/{id}/selesai", consumes = MediaType.MULTIPART_FORM_DATA_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public WebResponse<String> selesaikanTugas(
            @PathVariable(name = "id") Long id,
            @RequestParam(value = "photo", required = true) MultipartFile photo,
            @RequestParam(value = "namaPenerima", required = false) String namaPenerima,
            @RequestParam(value = "catatanOperasional", required = false) String catatanOperasional,
            java.security.Principal principal) {
        return penjadwalanService.selesaikanTugas(id, photo, namaPenerima, catatanOperasional, principal.getName());
    }

    /**
     * GET /api/v1/penjadwalan/{id}/photo
     * Sajikan foto bukti tugas penjadwalan secara aman (butuh JWT).
     */
    @GetMapping(path = "/{id}/photo")
    public ResponseEntity<Resource> getPenjadwalanPhoto(
            @PathVariable("id") Long id) {
        try {
            String fileName = penjadwalanService.getPenjadwalanPhotoFileName(id);
            if (fileName == null || fileName.isBlank()) {
                return ResponseEntity.notFound().build();
            }
            java.nio.file.Path filePath = java.nio.file.Paths.get("uploads/penjadwalan/" + fileName);
            if (!java.nio.file.Files.exists(filePath)) {
                filePath = java.nio.file.Paths.get("uploads/memos/" + fileName);
            }
            if (!java.nio.file.Files.exists(filePath)) {
                filePath = java.nio.file.Paths.get("uploads/" + fileName);
            }
            if (!java.nio.file.Files.exists(filePath)) {
                return ResponseEntity.notFound().build();
            }
            Resource resource = new UrlResource(filePath.toUri());
            String contentType = "image/jpeg";
            try {
                String probed = java.nio.file.Files.probeContentType(filePath);
                if (probed != null) contentType = probed;
            } catch (Exception ignored) {}
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_TYPE, contentType)
                    .header(HttpHeaders.CACHE_CONTROL, "private, max-age=3600")
                    .body(resource);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Gagal membaca foto: " + e.getMessage());
        }
    }

    @LogActivity("User memulai tugas penjadwalan")
    @PutMapping(path = "/{id}/mulai", produces = MediaType.APPLICATION_JSON_VALUE)
    public WebResponse<String> mulaiTugas(
            @PathVariable(name = "id") Long id,
            java.security.Principal principal) {
        return penjadwalanService.mulaiTugas(id, principal.getName());
    }

    @LogActivity("Marketing/Gudang membuat request pengambilan barang")
    @PostMapping(
            path = "/manual",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public WebResponse<String> createManual(
            @RequestBody @Validated CreateManualJadwalRequest request,
            java.security.Principal principal) {
        return penjadwalanService.createManualRequest(request, principal.getName());
    }

    @LogActivity("Marketing/Gudang membuat request pengambilan barang")
    @PostMapping(
            path = "/pengambilan",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public WebResponse<String> createPengambilan(
            @RequestBody @Validated CreatePengambilanRequest request,
            java.security.Principal principal) {
        return penjadwalanService.createPengambilan(request, principal.getName());
    }

    @LogActivity("User melakukan konfirmasi pengiriman via jadwal")
    @PostMapping(
            path = "/{id}/konfirmasi-kirim",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public WebResponse<String> konfirmasiKirimViaJadwal(
            @PathVariable(name = "id") Long id,
            @RequestBody @Validated KonfirmasiKirimRequest request,
            java.security.Principal principal) {
        return penjadwalanService.konfirmasiKirimJadwal(id, request, principal.getName());
    }

    @LogActivity("User melakukan bulk penjadwalan")
    @PostMapping(
            path = "/bulk",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public WebResponse<String> createBulkPenjadwalan(
            @RequestBody @Validated BulkPenjadwalanRequest request,
            java.security.Principal principal) {
        return penjadwalanService.createBulkPenjadwalan(request, principal.getName());
    }

    @LogActivity("User membuat batch pengiriman ekspedisi")
    @PostMapping(
            path = "/batch-drop-off",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public WebResponse<String> createBatchDropOff(
            @RequestBody @Validated BatchDropOffRequest request,
            java.security.Principal principal) {
        return penjadwalanService.createBatchDropOff(request, principal.getName());
    }

    @LogActivity("Driver memulai tugas pengiriman secara bulk")
    @PutMapping(path = "/bulk/mulai", produces = MediaType.APPLICATION_JSON_VALUE)
    public WebResponse<String> bulkMulaiTugas(
            @RequestBody List<Long> ids,
            java.security.Principal principal) {
        return penjadwalanService.bulkMulaiTugas(ids, principal.getName());
    }

    @LogActivity("Driver menyelesaikan tugas pengiriman secara bulk")
    @PutMapping(
            path = "/bulk/selesai",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public WebResponse<String> bulkSelesaikanTugas(
            @RequestParam("ids") List<Long> ids,
            @RequestParam("photo") MultipartFile photo,
            @RequestParam("nama_penerima") String namaPenerima,
            @RequestParam(value = "catatan_operasional", required = false) String catatanOperasional,
            java.security.Principal principal) {
        return penjadwalanService.bulkSelesaikanTugas(ids, photo, namaPenerima, catatanOperasional, principal.getName());
    }
}