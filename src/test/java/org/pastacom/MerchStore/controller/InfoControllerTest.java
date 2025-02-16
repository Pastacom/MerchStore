package org.pastacom.MerchStore.controller;

import org.junit.jupiter.api.Test;
import org.pastacom.MerchStore.dto.InfoResponse;
import org.pastacom.MerchStore.service.UserInfoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

import java.util.Collections;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

@SpringBootTest(classes = InfoController.class)
@AutoConfigureMockMvc
@EnableWebMvc
@Import(GlobalExceptionHandler.class)
public class InfoControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserInfoService userInfoService;

    @Test
    @WithMockUser(username = "testUser")
    public void testGetInfoSuccess() throws Exception {
        InfoResponse.Item item = new InfoResponse.Item("itemType", 10);
        InfoResponse.CoinHistory coinHistory = new InfoResponse.CoinHistory(
                Collections.singletonList(new InfoResponse.ReceivingTransaction("sender", 100)),
                Collections.singletonList(new InfoResponse.SendingTransaction("receiver", 50))
        );
        InfoResponse infoResponse = new InfoResponse(200, Collections.singletonList(item), coinHistory);

        when(userInfoService.getUserInfo("testUser")).thenReturn(infoResponse);

        mockMvc.perform(get("/api/info"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.coins").value(200))
                .andExpect(jsonPath("$.inventory[0].type").value("itemType"))
                .andExpect(jsonPath("$.inventory[0].quantity").value(10))
                .andExpect(jsonPath("$.coinHistory.received[0].fromUser").value("sender"))
                .andExpect(jsonPath("$.coinHistory.received[0].amount").value(100))
                .andExpect(jsonPath("$.coinHistory.sent[0].toUser").value("receiver"))
                .andExpect(jsonPath("$.coinHistory.sent[0].amount").value(50));
    }

    @Test
    public void testGetInfoUnauthorized() throws Exception {
        mockMvc.perform(get("/api/info"))
                .andExpect(status().isUnauthorized());
    }
}
