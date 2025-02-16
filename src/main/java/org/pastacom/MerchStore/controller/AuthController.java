package org.pastacom.MerchStore.controller;

import org.pastacom.MerchStore.dto.AuthRequest;
import org.pastacom.MerchStore.dto.AuthResponse;
import org.pastacom.MerchStore.dto.RefreshRequest;

import org.pastacom.MerchStore.exception.InvalidParametersException;
import org.pastacom.MerchStore.service.AuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class AuthController {

    private final AuthService authService;

    @Autowired
    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/auth")
    public ResponseEntity<AuthResponse> authorizeUser(@RequestBody AuthRequest request)
            throws InvalidParametersException {
        if (request.getUsername() == null || request.getUsername().trim().isEmpty()) {
            throw new InvalidParametersException("No username was provided");
        }

        if (request.getPassword() == null || request.getPassword().trim().isEmpty()) {
            throw new InvalidParametersException("No password was provided");
        }

        return ResponseEntity.ok(authService.authenticate(request));
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refreshToken(@RequestBody RefreshRequest request)
            throws InvalidParametersException  {

        if (request.getRefreshToken() == null || request.getRefreshToken().trim().isEmpty()) {
            throw new InvalidParametersException("No refresh token was provided");
        }

        return ResponseEntity.ok(authService.refreshToken(request));
    }
}
