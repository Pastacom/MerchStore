package org.pastacom.MerchStore.service;

import org.pastacom.MerchStore.dto.AuthRequest;
import org.pastacom.MerchStore.dto.AuthResponse;
import org.pastacom.MerchStore.dto.RefreshRequest;
import org.pastacom.MerchStore.exception.UnauthorizedException;
import org.pastacom.MerchStore.exception.UserNotFoundException;
import org.pastacom.MerchStore.model.RefreshToken;
import org.pastacom.MerchStore.model.User;
import org.pastacom.MerchStore.repository.RefreshTokenRepository;
import org.pastacom.MerchStore.repository.UserRepository;
import org.pastacom.MerchStore.util.JwtUtil;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Service
public class AuthService {
    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtUtil jwtUtil;
    private final PasswordEncoder passwordEncoder;

    static final int STARTING_BALANCE = 1000;

    @Autowired
    public AuthService(UserRepository userRepository, RefreshTokenRepository refreshTokenRepository,
                       JwtUtil jwtUtil, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.jwtUtil = jwtUtil;
        this.passwordEncoder = passwordEncoder;
    }

    public AuthResponse authenticate(AuthRequest request) throws UnauthorizedException {
        try {
            User user = userRepository.findByUsername(request.getUsername());
            return verifyPassword(user, request.getPassword());
        } catch (UserNotFoundException e) {
            return registerNewUser(request.getUsername(), request.getPassword());
        }
    }

    private AuthResponse verifyPassword(User user, String password) throws UnauthorizedException {
        if (passwordEncoder.matches(password, user.getPasswordHash())) {
            String token = jwtUtil.generateToken(user.getUsername());
            Optional<RefreshToken> foundToken = refreshTokenRepository.findByUserId(user.getId());
            String refreshToken;
            if (foundToken.isPresent() && foundToken.get().getExpiresAt().isAfter(Instant.now())) {
                refreshToken = foundToken.get().getToken();
            } else {
                foundToken.ifPresent(value -> refreshTokenRepository.deleteByTokenId(value.getId()));
                refreshToken = jwtUtil.generateRefreshToken(user.getUsername());
                RefreshToken refreshTokenModel = new RefreshToken(UUID.randomUUID(), user.getId(),
                        refreshToken, jwtUtil.extractExpiration(refreshToken));
                refreshTokenRepository.save(refreshTokenModel);
            }
            return new AuthResponse(token, refreshToken);
        }
        throw new UnauthorizedException("Invalid password provided for user " + user.getUsername());
    }

    private AuthResponse registerNewUser(String username, String password) {
        String hashedPassword = passwordEncoder.encode(password);
        User newUser = new User(UUID.randomUUID(), username, hashedPassword, STARTING_BALANCE);
        userRepository.saveUser(newUser);
        String token = jwtUtil.generateToken(username);
        String refreshToken = jwtUtil.generateRefreshToken(username);
        RefreshToken refreshTokenModel = new RefreshToken(UUID.randomUUID(), newUser.getId(),
                refreshToken, jwtUtil.extractExpiration(refreshToken));
        refreshTokenRepository.save(refreshTokenModel);
        return new AuthResponse(token, refreshToken);
    }

    public AuthResponse refreshToken(RefreshRequest request) throws UnauthorizedException {
        final String refreshToken = request.getRefreshToken();

        if (!jwtUtil.validateJwtToken(refreshToken)) {
            throw new UnauthorizedException("Invalid refresh token provided");
        }

        RefreshToken foundToken = refreshTokenRepository.findByToken(refreshToken);
        if (!foundToken.getExpiresAt().isAfter(Instant.now()))
        {
            throw new UnauthorizedException("Refresh token has expired");
        }

        Optional<User> user = userRepository.findById(foundToken.getUserId());

        if (user.isEmpty())
        {
            refreshTokenRepository.deleteByTokenId(foundToken.getId());
            throw new UnauthorizedException("Refresh token is associated with other user and will be blacklisted.");
        }

        String token = jwtUtil.generateToken(user.get().getUsername());
        String newRefreshToken = jwtUtil.generateRefreshToken(user.get().getUsername());
        RefreshToken refreshTokenModel = new RefreshToken(UUID.randomUUID(), user.get().getId(),
                newRefreshToken, jwtUtil.extractExpiration(newRefreshToken));
        refreshTokenRepository.deleteByTokenId(foundToken.getId());
        refreshTokenRepository.save(refreshTokenModel);
        return new AuthResponse(token, newRefreshToken);
    }
}
