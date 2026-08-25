package com.stok.anandam.store.controller;

import com.stok.anandam.store.annotation.LogActivity;
import com.stok.anandam.store.dto.WebResponse;
import com.stok.anandam.store.service.MemoService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import org.springframework.web.multipart.MultipartFile;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/memos")
@RequiredArgsConstructor
public class MemoController {

    private final MemoService memoService;

    @LogActivity("User memperbarui nomor resi memo")
    @PutMapping(
            path = "/{memoId}/resi",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public WebResponse<String> updateResi(
            @PathVariable(name = "memoId") UUID memoId,
            @RequestBody @Validated com.stok.anandam.store.dto.UpdateResiRequest request,
            java.security.Principal principal) {
        
        return memoService.updateResi(memoId, request, principal.getName());
    }

    @LogActivity("User mengubah status tracking memo")
    @PutMapping(
            path = "/{memoId}/status",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public WebResponse<String> updateStatus(
            @PathVariable(name = "memoId") UUID memoId,
            @RequestBody @Validated com.stok.anandam.store.dto.UpdateMemoStatusRequest request,
            java.security.Principal principal) {
        
        return memoService.updateMemoStatus(memoId, request, principal.getName());
    }

    @LogActivity("Marketing membuat memo penjualan baru")
    @PostMapping(
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public WebResponse<String> createMemo(
            @RequestBody @Validated com.stok.anandam.store.dto.CreateMemoRequest request,
            java.security.Principal principal) {
        
        return memoService.createMemo(request, principal.getName());
    }

    @LogActivity("Marketing memperbarui draft memo penjualan")
    @PutMapping(
            path = "/{memoId}",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public WebResponse<String> updateMemo(
            @PathVariable(name = "memoId") UUID memoId,
            @RequestBody @Validated com.stok.anandam.store.dto.CreateMemoRequest request,
            java.security.Principal principal) {
        
        return memoService.updateMemo(memoId, request, principal.getName());
    }

    @GetMapping(
            path = "/{memoId}",
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public WebResponse<com.stok.anandam.store.dto.MemoDetailResponse> getMemoDetail(
            @PathVariable(name = "memoId") UUID memoId,
            java.security.Principal principal) {
        return memoService.getMemoDetail(memoId, principal.getName());
    }

    @GetMapping(
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public WebResponse<java.util.List<com.stok.anandam.store.dto.MemoDetailResponse>> getListMemo(
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "statuses", required = false) java.util.List<String> statuses,
            java.security.Principal principal) {

        // Multi-status filter (digunakan oleh halaman pengiriman agar tidak tarik semua memo)
        if (statuses != null && !statuses.isEmpty()) {
            java.util.List<com.stok.anandam.store.core.postgres.model.enums.MemoStatus> memoStatuses =
                statuses.stream()
                    .map(s -> com.stok.anandam.store.core.postgres.model.enums.MemoStatus.fromName(s))
                    .filter(s -> s != null)
                    .collect(java.util.stream.Collectors.toList());
            return memoService.getListMemoByStatuses(memoStatuses, principal.getName());
        }

        com.stok.anandam.store.core.postgres.model.enums.MemoStatus memoStatus = 
            com.stok.anandam.store.core.postgres.model.enums.MemoStatus.fromName(status);
            
        return memoService.getListMemoByStatus(memoStatus, principal.getName());
    }

    @GetMapping(
            path = "/counts",
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public WebResponse<java.util.Map<String, Long>> getMemoCounts(java.security.Principal principal) {
        return memoService.getMemoCounts(principal.getName());
    }

    /**
     * @deprecated Scheduling is now integrated into the memo creation flow or removed.
     */
    @Deprecated
    @PostMapping("/{memoId}/penjadwalan")
    public WebResponse<String> createPenjadwalan(
            @PathVariable("memoId") UUID memoId,
            @RequestBody com.stok.anandam.store.dto.CreatePenjadwalanRequest request,
            java.security.Principal principal) {
        
        return memoService.addPenjadwalan(memoId, request, principal.getName());
    }

    @LogActivity("User melakukan konfirmasi pengiriman barang")
    @PostMapping(
            path = "/{memoId}/konfirmasi-kirim",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public WebResponse<String> konfirmasiKirim(
            @PathVariable(name = "memoId") UUID memoId,
            @RequestParam("itemsJson") String itemsJson,
            @RequestParam("photo") MultipartFile photo,
            java.security.Principal principal) {
        
        return memoService.konfirmasiKirim(memoId, itemsJson, photo, principal.getName());
    }

    @LogActivity("Marketing membooking stok barang (Pending)")
    @PostMapping(
            path = "/pending",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public WebResponse<String> createPendingMemo(
            @RequestBody @Validated com.stok.anandam.store.dto.CreateMemoRequest request,
            java.security.Principal principal) {
        
        return memoService.createPendingMemo(request, principal.getName());
    }

    @LogActivity("Menyetujui booking stok")
    @PutMapping(path = "/pending/{id}/approve", produces = MediaType.APPLICATION_JSON_VALUE)
    public WebResponse<String> approvePending(@PathVariable("id") UUID id, java.security.Principal principal) {
        return memoService.approvePendingMemo(id, principal.getName());
    }

    @LogActivity("Menolak booking stok")
    @PutMapping(path = "/pending/{id}/reject", produces = MediaType.APPLICATION_JSON_VALUE)
    public WebResponse<String> rejectPending(
            @PathVariable("id") UUID id, 
            @RequestParam(value = "alasan", required = false) String alasan,
            java.security.Principal principal) {
        return memoService.rejectPendingMemo(id, principal.getName(), alasan != null ? alasan : "Ditolak tanpa alasan");
    }

    @LogActivity("Melepas/Memulihkan booking stok")
    @PutMapping(path = "/pending/{id}/release", produces = MediaType.APPLICATION_JSON_VALUE)
    public WebResponse<String> releasePending(@PathVariable("id") UUID id, java.security.Principal principal) {
        return memoService.releasePendingMemo(id, principal.getName());
    }

    @LogActivity("Lanjutkan booking menjadi memo")
    @PutMapping(path = "/pending/{id}/continue", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public WebResponse<String> continuePendingBooking(
            @PathVariable("id") UUID id,
            @RequestBody @Validated com.stok.anandam.store.dto.UpdateMemoTypeRequest request,
            java.security.Principal principal) {
        return memoService.continueToMemo(id, request, principal.getName());
    }

    @LogActivity("Menyelesaikan booking pending")
    @PutMapping(path = "/pending/{id}/finish-pending", produces = MediaType.APPLICATION_JSON_VALUE)
    public WebResponse<String> finishPendingBooking(@PathVariable("id") UUID id, java.security.Principal principal) {
        return memoService.finishPendingMemo(id, principal.getName());
    }

    @LogActivity("Marketing memfinalisasi memo (Kirim ke Gudang)")
    @PutMapping(path = "/{memoId}/finalize", produces = MediaType.APPLICATION_JSON_VALUE)
    public WebResponse<String> finalizeMemo(@PathVariable("memoId") UUID memoId, java.security.Principal principal) {
        return memoService.finalizeMemo(memoId, principal.getName());
    }

    @LogActivity("Gudang memperbarui catatan item")
    @PutMapping(path = "/items/{itemId}/catatan", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public WebResponse<String> updateItemCatatan(
            @PathVariable("itemId") Long itemId,
            @RequestBody @Validated com.stok.anandam.store.dto.UpdateItemCatatanRequest request,
            java.security.Principal principal) {
        return memoService.updateItemCatatan(itemId, request, principal.getName());
    }

    @LogActivity("User memperbarui status item memo")
    @PutMapping(path = "/{memoId}/items/{itemId}/status", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public WebResponse<String> updateItemStatus(
            @PathVariable("memoId") UUID memoId,
            @PathVariable("itemId") Long itemId,
            @RequestBody @Validated com.stok.anandam.store.dto.UpdateItemStatusRequest request,
            java.security.Principal principal) {
        return memoService.updateItemStatus(memoId, itemId, request, principal.getName());
    }

    @LogActivity("Gudang menyelesaikan proses picking (Ready di Rak Keluar)")
    @PutMapping(path = "/{memoId}/gudang-finish", produces = MediaType.APPLICATION_JSON_VALUE)
    public WebResponse<String> finishGudangProcess(
            @PathVariable("memoId") UUID memoId,
            java.security.Principal principal) {
        return memoService.finishWarehouseProcess(memoId, principal.getName());
    }

    @LogActivity("Gudang menyelesaikan proses pembuatan Invoice/Nota")
    @PutMapping(path = "/{memoId}/invoice-finish", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public WebResponse<String> finishInvoicingProcess(
            @PathVariable("memoId") UUID memoId,
            @RequestBody @Validated com.stok.anandam.store.dto.FinishNotaRequest request,
            java.security.Principal principal) {
        return memoService.finishInvoicingProcess(memoId, request, principal.getName());
    }

    @LogActivity("Gudang memilih jalur Pengiriman (Menunggu Driver/Teknisi)")
    @PutMapping(path = "/{memoId}/delivery-route", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public WebResponse<String> confirmDeliveryRoute(
            @PathVariable("memoId") UUID memoId,
            @RequestBody @Validated com.stok.anandam.store.dto.FinishGudangRequest request,
            java.security.Principal principal) {
        return memoService.confirmDeliveryRoute(memoId, request, principal.getName());
    }

    @LogActivity("User melakukan serah terima langsung (Pickup/Batal Kirim)")
    @PutMapping(
            path = "/{memoId}/pickup-route",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public WebResponse<String> confirmPickupRoute(
            @PathVariable(name = "memoId") UUID memoId,
            @RequestParam("photo") MultipartFile photo,
            java.security.Principal principal) {
        return memoService.confirmPickupRoute(memoId, photo, principal.getName());
    }
    
    @LogActivity("User melakukan konfirmasi final serah terima (Admin/Marketing)")
    @PutMapping(path = "/{memoId}/pickup-final", produces = MediaType.APPLICATION_JSON_VALUE)
    public WebResponse<String> confirmPickupFinal(
            @PathVariable("memoId") UUID memoId,
            java.security.Principal principal) {
        return memoService.confirmPickupFinal(memoId, principal.getName());
    }

    @LogActivity("Gudang melaporkan kendala fisik barang")
    @PutMapping(path = "/{memoId}/report-issue", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public WebResponse<String> reportPhysicalIssue(
            @PathVariable("memoId") UUID memoId,
            @RequestBody @Validated com.stok.anandam.store.dto.UpdateMemoStatusRequest request,
            java.security.Principal principal) {
        return memoService.reportPhysicalIssue(memoId, request, principal.getName());
    }

    @LogActivity("Admin menyelesaikan memo secara paksa (Audit)")
    @PutMapping(path = "/{memoId}/force-complete", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public WebResponse<String> forceComplete(
            @PathVariable("memoId") UUID memoId,
            @RequestBody @Validated com.stok.anandam.store.dto.UpdateMemoStatusRequest request,
            java.security.Principal principal) {
        return memoService.forceComplete(memoId, request, principal.getName());
    }

    @LogActivity("Teknisi menyelesaikan proses teknisi")
    @PutMapping(path = "/{memoId}/technician-finish", produces = MediaType.APPLICATION_JSON_VALUE)
    public WebResponse<String> finishTechnicianProcess(@PathVariable("memoId") UUID memoId, java.security.Principal principal) {
        return memoService.finishTechnicianProcess(memoId, principal.getName());
    }
    @PutMapping("/{memoId}/complete")
    public WebResponse<String> completeMemo(
            @PathVariable("memoId") UUID memoId,
            java.security.Principal principal) {
        return memoService.completeMemo(memoId, principal.getName());
    }

    @LogActivity("Driver menyelesaikan pengiriman")
    @PutMapping(
            path = "/{memoId}/delivery-finish",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public WebResponse<String> finishDeliveryProcess(
            @PathVariable("memoId") UUID memoId,
            @RequestParam("photo") MultipartFile photo,
            @RequestParam(value = "catatan", required = false) String catatan,
            java.security.Principal principal) {
        return memoService.finishDeliveryProcess(memoId, photo, catatan, principal.getName());
    }

    @LogActivity("User mengunggah foto bukti (evidence) tanpa mengubah status")
    @PutMapping(
            path = "/{memoId}/photo-evidence",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public WebResponse<String> uploadEvidencePhoto(
            @PathVariable("memoId") UUID memoId,
            @RequestParam("photo") MultipartFile photo,
            java.security.Principal principal) {
        return memoService.uploadEvidencePhoto(memoId, photo, principal.getName());
    }

    @LogActivity("Melakukan duplikat dan revisi memo")
    @PostMapping(path = "/{memoId}/duplicate-revision", produces = MediaType.APPLICATION_JSON_VALUE)
    public WebResponse<com.stok.anandam.store.dto.MemoDetailResponse> duplicateRevision(
            @PathVariable("memoId") UUID memoId,
            java.security.Principal principal) {
        return memoService.duplicateRevision(memoId, principal.getName());
    }

    @LogActivity("Melakukan duplikat header memo")
    @PostMapping(path = "/{memoId}/duplicate-header", produces = MediaType.APPLICATION_JSON_VALUE)
    public WebResponse<com.stok.anandam.store.dto.MemoDetailResponse> duplicateHeader(
            @PathVariable("memoId") UUID memoId,
            java.security.Principal principal) {
        return memoService.duplicateHeader(memoId, principal.getName());
    }

    @LogActivity("Membatalkan draft memo hasil duplikasi/revisi")
    @DeleteMapping(path = "/{memoId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public WebResponse<String> deleteMemo(
            @PathVariable("memoId") UUID memoId,
            java.security.Principal principal) {
        return memoService.deleteMemo(memoId, principal.getName());
    }

    @LogActivity("Retry auto-match JL dari data MyBiz")
    @PutMapping(path = "/{memoId}/retry-auto-jl", produces = MediaType.APPLICATION_JSON_VALUE)
    public WebResponse<String> retryAutoMatchJl(
            @PathVariable("memoId") UUID memoId,
            java.security.Principal principal) {
        return memoService.retryAutoMatchJl(memoId, principal.getName());
    }

    @LogActivity("Bulk retry auto-match JL dari data MyBiz")
    @PutMapping(path = "/retry-auto-jl-bulk", produces = MediaType.APPLICATION_JSON_VALUE)
    public WebResponse<String> retryAutoMatchJlBulk(java.security.Principal principal) {
        return memoService.retryAutoMatchJlBulk(principal.getName());
    }

    @GetMapping(
            path = "/search/by-resi",
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public WebResponse<java.util.List<com.stok.anandam.store.dto.MemoDetailResponse>> searchByResi(
            @RequestParam("resi") String resi,
            java.security.Principal principal) {
        return memoService.searchByResi(resi, principal.getName());
    }

    @GetMapping(
            path = "/search/by-order-id",
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public WebResponse<java.util.List<com.stok.anandam.store.dto.MemoDetailResponse>> searchByOrderId(
            @RequestParam("orderId") String orderId,
            java.security.Principal principal) {
        return memoService.searchByOrderId(orderId, principal.getName());
    }

    @GetMapping(
            path = "/search/by-barcode",
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public WebResponse<java.util.List<com.stok.anandam.store.dto.MemoDetailResponse>> searchByBarcode(
            @RequestParam("code") String code,
            java.security.Principal principal) {
        return memoService.searchByBarcode(code, principal.getName());
    }
}
