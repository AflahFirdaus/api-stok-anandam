package com.stok.anandam.store.service;

import com.stok.anandam.store.core.postgres.model.User;
import com.stok.anandam.store.core.postgres.repository.UserRepository;
import com.stok.anandam.store.dto.UserRequest;
import com.stok.anandam.store.dto.UserResponse;
import com.stok.anandam.store.exception.ResourceNotFoundException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    // UPDATE: Menggunakan Page<UserResponse> dan menerima parameter page & size
    public Page<UserResponse> getAllUsers(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        
        Page<User> usersPage = userRepository.findAll(pageable);
        
        // Konversi Page<User> ke List<UserResponse>
        List<UserResponse> userResponses = usersPage.getContent().stream()
                .map(this::toUserResponse)
                .collect(Collectors.toList());

        // Bungkus kembali menjadi Page
        return new PageImpl<>(userResponses, pageable, usersPage.getTotalElements());
    }

    public UserResponse createUser(UserRequest request) {
        if (request.getPassword() == null || request.getPassword().isBlank()) {
            throw new IllegalArgumentException("Password wajib diisi saat membuat user");
        }
        if (userRepository.findByUsername(request.getUsername()).isPresent()) {
            throw new RuntimeException("Username sudah terpakai!");
        }
        User user = new User();
        user.setNama(request.getNama());
        user.setUsername(request.getUsername());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRole(request.getRole());
        User savedUser = userRepository.save(user);
        return toUserResponse(savedUser);
    }

    public UserResponse updateUser(Long id, UserRequest request) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User dengan ID " + id + " tidak ditemukan"));

        if (!user.getUsername().equals(request.getUsername())) {
            if (userRepository.existsByUsername(request.getUsername())) {
                throw new DataIntegrityViolationException("Username '" + request.getUsername() + "' sudah digunakan user lain");
            }
        }

        user.setNama(request.getNama());
        user.setUsername(request.getUsername());
        user.setRole(request.getRole());

        if (request.getPassword() != null && !request.getPassword().isBlank()) {
            user.setPassword(passwordEncoder.encode(request.getPassword()));
        }
        
        userRepository.save(user);
        return toUserResponse(user);
    }

    public void deleteUser(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User dengan ID " + id + " tidak ditemukan"));

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String currentUsername = auth.getName();

        if (user.getUsername().equals(currentUsername)) {
            throw new DataIntegrityViolationException("Anda tidak dapat menghapus akun sendiri yang sedang aktif!");
        }

        userRepository.delete(user);
    }

    private UserResponse toUserResponse(User user) {
        UserResponse response = new UserResponse();
        response.setId(user.getId());
        response.setNama(user.getNama());
        response.setUsername(user.getUsername());
        response.setRole(user.getRole());
        return response;
    }
}