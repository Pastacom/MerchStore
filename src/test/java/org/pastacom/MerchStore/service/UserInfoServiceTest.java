package org.pastacom.MerchStore.service;

import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.pastacom.MerchStore.dto.InfoResponse;
import org.pastacom.MerchStore.exception.UserNotFoundException;
import org.pastacom.MerchStore.model.User;
import org.pastacom.MerchStore.repository.UserRepository;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserInfoServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserInfoService userInfoService;

    @Test
    void testGetUserInfo_Success() {
        String username = "validUser";
        int balance = 100;

        when(userRepository.findByUsername(username))
                .thenReturn(new User(username, balance));

        InfoResponse response = assertDoesNotThrow(() -> userInfoService.getUserInfo(username));

        assertEquals(balance, response.getCoins());
        assertTrue(response.getInventory().isEmpty());
        assertTrue(response.getCoinHistory().getReceived().isEmpty());
        assertTrue(response.getCoinHistory().getSent().isEmpty());
    }

    @Test
    void testGetUserInfo_UserNotFound() {
        String username = "invalidUser";

        when(userRepository.findByUsername(username)).thenThrow(new UserNotFoundException("User not found"));
        assertThrows(UserNotFoundException.class, () -> userInfoService.getUserInfo(username));
    }
}
