package org.pastacom.MerchStore.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.pastacom.MerchStore.dto.AuthRequest;
import org.pastacom.MerchStore.dto.AuthResponse;
import org.pastacom.MerchStore.dto.RefreshRequest;
import org.pastacom.MerchStore.service.AuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(classes = AuthController.class)
@AutoConfigureMockMvc
@EnableWebMvc
@Import(GlobalExceptionHandler.class)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AuthService authService;

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
    }

    @Test
    @WithMockUser
    void testAuthorizeUserSuccessfully() throws Exception {
        AuthRequest authRequest = new AuthRequest("testUser", "testPassword");
        AuthResponse authResponse = new AuthResponse("newAccessToken", "newRefreshToken");

        when(authService.authenticate(any())).thenReturn(authResponse);

        mockMvc.perform(post("/api/auth")
                        .with(SecurityMockMvcRequestPostProcessors.csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(authRequest)))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.accessToken").value("newAccessToken"))
                        .andExpect(jsonPath("$.refreshToken").value("newRefreshToken"));

        verify(authService).authenticate(any());
    }

    @Test
    @WithMockUser
    void testAuthorizeUserUsernameIsEmpty() throws Exception {
        AuthRequest authRequest = new AuthRequest("", "testPassword");

        mockMvc.perform(post("/api/auth")
                        .with(SecurityMockMvcRequestPostProcessors.csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(authRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors").value("No username was provided"));

        verify(authService, never()).authenticate(any());
    }

    @Test
    @WithMockUser
    void testAuthorizeUserPasswordIsEmpty() throws Exception {
        AuthRequest authRequest = new AuthRequest("testUser", "");

        mockMvc.perform(post("/api/auth")
                        .with(SecurityMockMvcRequestPostProcessors.csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(authRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors").value("No password was provided"));

        verify(authService, never()).authenticate(any());
    }

    @Test
    @WithMockUser
    void testRefreshTokenSuccessfully() throws Exception {
        RefreshRequest refreshRequest = new RefreshRequest("oldRefreshToken");
        AuthResponse authResponse = new AuthResponse("newAccessToken", "newRefreshToken");

        when(authService.refreshToken(any())).thenReturn(authResponse);

        mockMvc.perform(post("/api/refresh")
                        .with(SecurityMockMvcRequestPostProcessors.csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(refreshRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("newAccessToken"))
                .andExpect(jsonPath("$.refreshToken").value("newRefreshToken"));

        verify(authService).refreshToken(any());
    }

    @Test
    @WithMockUser
    void testRefreshTokenIsEmpty() throws Exception {
        RefreshRequest refreshRequest = new RefreshRequest("");

        mockMvc.perform(post("/api/refresh")
                        .with(SecurityMockMvcRequestPostProcessors.csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(refreshRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors").value("No refresh token was provided"));

        verify(authService, never()).refreshToken(any());
    }
}
