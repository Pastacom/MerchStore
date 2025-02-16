package org.pastacom.MerchStore.controller;

import org.junit.jupiter.api.Test;
import org.pastacom.MerchStore.exception.UserNotFoundException;
import org.pastacom.MerchStore.service.StoreService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

import static org.mockito.Mockito.*;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = StoreController.class)
@AutoConfigureMockMvc
@EnableWebMvc
@Import(GlobalExceptionHandler.class)
public class StoreControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private StoreService storeService;

    @Test
    @WithMockUser(username = "testUser")
    public void testBuyProductSuccess() throws Exception {
        doNothing().when(storeService).buyItem("testUser", "itemType");

        mockMvc.perform(get("/api/buy/itemType"))
                .andExpect(status().isOk());

        verify(storeService).buyItem("testUser", "itemType");
    }

    @Test
    @WithMockUser(username = "testUser")
    public void testBuyProductNoItem() throws Exception {
        doThrow(new UserNotFoundException("User not found"))
                .when(storeService).buyItem("testUser", "blah");

        mockMvc.perform(get("/api/buy/blah"))
                .andExpect(status().isNotFound());

        verify(storeService).buyItem("testUser", "blah");
    }

    @Test
    public void testBuyProductUnauthorized() throws Exception {
        mockMvc.perform(get("/api/buy/itemType")).andExpect(status().isUnauthorized());
        verify(storeService, never()).buyItem(anyString(), anyString());
    }
}
