package org.pastacom.MerchStore.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;
import org.pastacom.MerchStore.dto.SendCoinRequest;
import org.pastacom.MerchStore.exception.InvalidParametersException;
import org.pastacom.MerchStore.service.TransactionService;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(classes = TransactionController.class)
@AutoConfigureMockMvc
@EnableWebMvc
@Import(GlobalExceptionHandler.class)
public class TransactionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private TransactionService transactionService;

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
    }

    @Test
    @WithMockUser(username = "testUser")
    public void testSendCoinsSuccess() throws Exception {
        SendCoinRequest request = new SendCoinRequest("receiverUser", 10);

        doNothing().when(transactionService).sendCoins(any(), any());

        mockMvc.perform(post("/api/sendCoin")
                        .with(SecurityMockMvcRequestPostProcessors.csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                        .andExpect(status().isOk());

        verify(transactionService).sendCoins(any(), any());
    }

    @Test
    @WithMockUser(username = "testUser")
    public void testSendCoinsNoReceiver() throws Exception {
        SendCoinRequest request = new SendCoinRequest("", 10);

        mockMvc.perform(post("/api/sendCoin")
                        .with(SecurityMockMvcRequestPostProcessors.csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                        .andExpect(status().isBadRequest())
                        .andExpect(jsonPath("$.errors")
                                .value("No receiver of the coins was provided"));

        verify(transactionService, never()).sendCoins(any(), any());
    }

    @Test
    @WithMockUser(username = "testUser")
    public void testSendCoinsNoAmount() throws Exception {
        SendCoinRequest request = new SendCoinRequest("receiverUser", 0);

        mockMvc.perform(post("/api/sendCoin")
                        .with(SecurityMockMvcRequestPostProcessors.csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                        .andExpect(status().isBadRequest())
                        .andExpect(jsonPath("$.errors").value("No coins amount was provided"));

        verify(transactionService, never()).sendCoins(any(), any());
    }

    @Test
    public void testBuyProductUnauthorized() throws Exception {
        SendCoinRequest request = new SendCoinRequest("receiverUser", 10);

        mockMvc.perform(post("/api/sendCoin")
                        .with(SecurityMockMvcRequestPostProcessors.csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                        .andExpect(status().isUnauthorized());

        verify(transactionService, never()).sendCoins(any(), any());
    }
}
