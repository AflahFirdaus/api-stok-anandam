package com.stok.anandam.store.exception;

import com.stok.anandam.store.dto.ApiErrorResponse;
import com.stok.anandam.store.dto.WebResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.time.format.DateTimeParseException;
import java.util.Map;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    // 1. Handle Data Tidak Ditemukan (404)
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<WebResponse<ApiErrorResponse>> handleResourceNotFound(ResourceNotFoundException ex, HttpServletRequest request) {
        ApiErrorResponse error = new ApiErrorResponse(
                HttpStatus.NOT_FOUND.value(),
                "Not Found",
                ex.getMessage(),
                request.getRequestURI()
        );
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(WebResponse.<ApiErrorResponse>builder()
                .status(HttpStatus.NOT_FOUND.value())
                .message(ex.getMessage())
                .data(error)
                .paging(null)
                .build());
    }

    // 2. Handle Error Database (Contoh: Duplicate Entry, Kolom Kepanjangan) (409)
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<WebResponse<ApiErrorResponse>> handleDataIntegrity(DataIntegrityViolationException ex, HttpServletRequest request) {
        // Ambil pesan root cause biar lebih jelas (tapi tetap aman)
        String message = "Terjadi konflik data database. Cek constraint atau panjang karakter.";
        if (ex.getRootCause() != null) {
            message = ex.getRootCause().getMessage();
        }

        ApiErrorResponse error = new ApiErrorResponse(
                HttpStatus.CONFLICT.value(),
                "Database Conflict",
                message,
                request.getRequestURI()
        );
        return ResponseEntity.status(HttpStatus.CONFLICT).body(WebResponse.<ApiErrorResponse>builder()
                .status(HttpStatus.CONFLICT.value())
                .message(message)
                .data(error)
                .paging(null)
                .build());
    }

    // 3. Handle Tipe Data Salah di URL (Contoh: ?page=abc padahal harus int) (400)
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<WebResponse<ApiErrorResponse>> handleTypeMismatch(MethodArgumentTypeMismatchException ex, HttpServletRequest request) {
        String message = String.format("Parameter '%s' harus berupa tipe '%s'", 
                ex.getName(), ex.getRequiredType().getSimpleName());
        
        ApiErrorResponse error = new ApiErrorResponse(
                HttpStatus.BAD_REQUEST.value(),
                "Bad Request",
                message,
                request.getRequestURI()
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(WebResponse.<ApiErrorResponse>builder()
                .status(HttpStatus.BAD_REQUEST.value())
                .message(message)
                .data(error)
                .paging(null)
                .build());
    }

    // 4. Handle Parameter Kurang (400)
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<WebResponse<ApiErrorResponse>> handleMissingParams(MissingServletRequestParameterException ex, HttpServletRequest request) {
        String message = "Parameter wajib '" + ex.getParameterName() + "' tidak ditemukan.";
        ApiErrorResponse error = new ApiErrorResponse(
                HttpStatus.BAD_REQUEST.value(),
                "Missing Parameter",
                message,
                request.getRequestURI()
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(WebResponse.<ApiErrorResponse>builder()
                .status(HttpStatus.BAD_REQUEST.value())
                .message(message)
                .data(error)
                .paging(null)
                .build());
    }

    // 5. Handle Argument tidak valid dari service (400)
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<WebResponse<ApiErrorResponse>> handleIllegalArgument(IllegalArgumentException ex, HttpServletRequest request) {
        ApiErrorResponse error = new ApiErrorResponse(
                HttpStatus.BAD_REQUEST.value(),
                "Bad Request",
                ex.getMessage(),
                request.getRequestURI()
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(WebResponse.<ApiErrorResponse>builder()
                .status(HttpStatus.BAD_REQUEST.value())
                .message(ex.getMessage())
                .data(error)
                .paging(null)
                .build());
    }

    // 5a. Format tanggal tidak valid (startDate/endDate) (400)
    @ExceptionHandler(DateTimeParseException.class)
    public ResponseEntity<WebResponse<ApiErrorResponse>> handleDateTimeParse(DateTimeParseException ex, HttpServletRequest request) {
        String message = "Format tanggal tidak valid. Gunakan yyyy-MM-dd (contoh: 2024-01-15).";
        ApiErrorResponse error = new ApiErrorResponse(
                HttpStatus.BAD_REQUEST.value(),
                "Bad Request",
                message,
                request.getRequestURI()
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(WebResponse.<ApiErrorResponse>builder()
                .status(HttpStatus.BAD_REQUEST.value())
                .message(message)
                .data(error)
                .paging(null)
                .build());
    }

    // 5b. Handle Invalid Refresh Token (401)
    @ExceptionHandler(InvalidRefreshTokenException.class)
    public ResponseEntity<WebResponse<ApiErrorResponse>> handleInvalidRefreshToken(InvalidRefreshTokenException ex, HttpServletRequest request) {
        ApiErrorResponse error = new ApiErrorResponse(
                HttpStatus.UNAUTHORIZED.value(),
                "Unauthorized",
                ex.getMessage(),
                request.getRequestURI()
        );
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(WebResponse.<ApiErrorResponse>builder()
                .status(HttpStatus.UNAUTHORIZED.value())
                .message(ex.getMessage())
                .data(error)
                .paging(null)
                .build());
    }

    // 6. Handle Validasi DTO (Bean Validation) (400)
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<WebResponse<ApiErrorResponse>> handleValidation(MethodArgumentNotValidException ex, HttpServletRequest request) {
        Map<String, String> fieldErrors = ex.getBindingResult().getFieldErrors().stream()
                .collect(Collectors.toMap(
                        err -> err.getField(),
                        err -> err.getDefaultMessage() != null ? err.getDefaultMessage() : "Invalid",
                        (a, b) -> a
                ));
        String message = fieldErrors.isEmpty() ? "Validasi gagal" : fieldErrors.values().iterator().next();
        ApiErrorResponse error = new ApiErrorResponse(
                HttpStatus.BAD_REQUEST.value(),
                "Validation Failed",
                message,
                request.getRequestURI(),
                fieldErrors
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(WebResponse.<ApiErrorResponse>builder()
                .status(HttpStatus.BAD_REQUEST.value())
                .message(message)
                .data(error)
                .paging(null)
                .build());
    }

    // 7. Handle Semua Error Lain yang Tidak Terduga (500)
    @ExceptionHandler(Exception.class)
    public ResponseEntity<WebResponse<ApiErrorResponse>> handleGlobalException(Exception ex, HttpServletRequest request) {
        log.error("Unexpected error at {}: {}", request.getRequestURI(), ex.getMessage(), ex);

        String message = "Terjadi kesalahan internal pada server. Silakan hubungi admin.";
        ApiErrorResponse error = new ApiErrorResponse(
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                "Internal Server Error",
                message,
                request.getRequestURI()
        );
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(WebResponse.<ApiErrorResponse>builder()
                .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                .message(message)
                .data(error)
                .paging(null)
                .build());
    }
}