package org.pastacom.MerchStore.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
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
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
public class AuthServiceTest {

    @InjectMocks
    private AuthService authService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private PasswordEncoder passwordEncoder;

    private User user;

    @BeforeEach
    void setUp() {
        user = new User(UUID.randomUUID(), "testUser", "hashedPassword", 1000);
    }

    @Test
    void testRegisterNewUser() {
        AuthRequest authRequest = new AuthRequest("testUser", "rawPassword");

        String accessToken = "testToken";
        String refreshToken = "testRefreshToken";

        when(userRepository.findByUsername(authRequest.getUsername()))
                .thenThrow(new UserNotFoundException("User not found"));
        when(passwordEncoder.encode(authRequest.getPassword())).thenReturn("hashedPassword");
        when(jwtUtil.generateToken(authRequest.getUsername())).thenReturn(accessToken);
        when(jwtUtil.generateRefreshToken(authRequest.getUsername())).thenReturn(refreshToken);

        AuthResponse response = authService.authenticate(authRequest);

        assertNotNull(response);
        assertEquals(accessToken, response.getAccessToken());
        assertEquals(refreshToken, response.getRefreshToken());
    }

    static Stream<Object[]> provideAuthParams() {
        User user = new User(UUID.randomUUID(), "testUser", "hashedPassword", 1000);
        RefreshToken validRefreshToken = new RefreshToken(UUID.randomUUID(), user.getId(), "validRefreshToken",
                Instant.now().plusSeconds(3600));
        RefreshToken expiredRefreshToken = new RefreshToken(UUID.randomUUID(), user.getId(), "expiredRefreshToken",
                Instant.now().minusSeconds(3600));

        return Stream.of(
                new Object[]{user, Optional.of(validRefreshToken), true, false},
                new Object[]{user, Optional.of(expiredRefreshToken), true, true},
                new Object[]{user, Optional.empty(), false, true}
        );
    }

    @ParameterizedTest
    @MethodSource("provideAuthParams")
    void testAuthenticate(User user, Optional<RefreshToken> refreshToken,
                              boolean passwordMatches, boolean shouldGenerateNewRefreshToken) {

        String password = "rawPassword";
        String newAccessToken = "newAccessToken";

        when(userRepository.findByUsername(user.getUsername())).thenReturn(user);
        when(passwordEncoder.matches(password, user.getPasswordHash()))
                .thenReturn(passwordMatches);

        AuthRequest request = new AuthRequest(user.getUsername(), password);

        if (!passwordMatches) {
            assertThrows(UnauthorizedException.class, () -> authService.authenticate(request));
        } else {
            when(refreshTokenRepository.findByUserId(user.getId())).thenReturn(refreshToken);
            when(jwtUtil.generateToken(anyString())).thenReturn(newAccessToken);

            if (shouldGenerateNewRefreshToken) {
                when(jwtUtil.generateRefreshToken(anyString())).thenReturn("newRefreshToken");
                when(jwtUtil.extractExpiration(anyString())).thenReturn(Instant.now().plusSeconds(3600));
            }

            AuthResponse response = authService.authenticate(new AuthRequest(user.getUsername(), password));

            assertNotNull(response);
            assertEquals(newAccessToken, response.getAccessToken());

            if (shouldGenerateNewRefreshToken) {
                verify(jwtUtil).generateRefreshToken(user.getUsername());
                verify(refreshTokenRepository).save(any());

                if (refreshToken.isPresent()) {
                    verify(refreshTokenRepository).deleteByTokenId(refreshToken.get().getId());
                    assertNotEquals(response.getRefreshToken(), refreshToken.get().getToken());
                }
            } else {
                verify(refreshTokenRepository, never()).save(any());

                assertEquals(response.getRefreshToken(), refreshToken.isPresent() ? refreshToken.get().getToken() : "");
            }
        }
    }

    @Test
    void testRefreshToken_Success() {
        String oldRefreshToken = "oldRefreshToken";
        String newAccessToken = "newAccessToken";
        String newRefreshToken = "newRefreshToken";
        Instant expiresAt = Instant.now().plusSeconds(3600);

        RefreshRequest request = new RefreshRequest(oldRefreshToken);
        RefreshToken foundToken = new RefreshToken(UUID.randomUUID(), user.getId(), oldRefreshToken, expiresAt);

        when(jwtUtil.validateJwtToken(oldRefreshToken)).thenReturn(true);
        when(refreshTokenRepository.findByToken(oldRefreshToken)).thenReturn(foundToken);
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
        when(jwtUtil.generateToken(user.getUsername())).thenReturn(newAccessToken);
        when(jwtUtil.generateRefreshToken(user.getUsername())).thenReturn(newRefreshToken);
        when(jwtUtil.extractExpiration(newRefreshToken)).thenReturn(expiresAt);

        AuthResponse response = authService.refreshToken(request);

        assertNotNull(response);
        assertEquals(newAccessToken, response.getAccessToken());
        assertEquals(newRefreshToken, response.getRefreshToken());

        verify(refreshTokenRepository).deleteByTokenId(foundToken.getId());
        verify(refreshTokenRepository).save(any());
    }

    @Test
    void testRefreshToken_InvalidToken() {
        String invalidRefreshToken = "invalidRefreshToken";
        RefreshRequest request = new RefreshRequest(invalidRefreshToken);

        when(jwtUtil.validateJwtToken(invalidRefreshToken)).thenReturn(false);

        assertThrows(UnauthorizedException.class, () -> authService.refreshToken(request));

        verifyNoInteractions(refreshTokenRepository);
        verifyNoInteractions(userRepository);
    }

    @Test
    void testRefreshToken_ExpiredToken() {
        String oldRefreshToken = "expiredRefreshToken";
        RefreshRequest request = new RefreshRequest(oldRefreshToken);
        Instant expiredAt = Instant.now().minusSeconds(3600);

        RefreshToken foundToken = new RefreshToken(UUID.randomUUID(), user.getId(), oldRefreshToken, expiredAt);

        when(jwtUtil.validateJwtToken(oldRefreshToken)).thenReturn(true);
        when(refreshTokenRepository.findByToken(oldRefreshToken)).thenReturn(foundToken);

        assertThrows(UnauthorizedException.class, () -> authService.refreshToken(request));

        verify(refreshTokenRepository).findByToken(oldRefreshToken);
        verifyNoInteractions(userRepository);
    }

    @Test
    void testRefreshToken_UserNotFound() {
        String oldRefreshToken = "validRefreshToken";
        UUID userId = UUID.randomUUID();
        Instant expiresAt = Instant.now().plusSeconds(3600);

        RefreshRequest request = new RefreshRequest(oldRefreshToken);
        RefreshToken foundToken = new RefreshToken(UUID.randomUUID(), userId, oldRefreshToken, expiresAt);

        when(jwtUtil.validateJwtToken(oldRefreshToken)).thenReturn(true);
        when(refreshTokenRepository.findByToken(oldRefreshToken)).thenReturn(foundToken);
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        assertThrows(UnauthorizedException.class, () -> authService.refreshToken(request));

        verify(refreshTokenRepository).deleteByTokenId(foundToken.getId());
    }
}
