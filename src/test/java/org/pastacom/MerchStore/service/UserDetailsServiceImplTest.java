package org.pastacom.MerchStore.service;

import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.pastacom.MerchStore.exception.UserNotFoundException;
import org.pastacom.MerchStore.model.User;
import org.pastacom.MerchStore.repository.UserRepository;
import org.springframework.security.core.userdetails.UserDetails;

import org.junit.jupiter.api.extension.ExtendWith;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserDetailsServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserDetailsServiceImpl userDetailsService;

    @Test
    void testLoadUserByUsername_Success() {
        String username = "validUser";
        String passwordHash = "hashedPassword";

        when(userRepository.findByUsername(username))
                .thenReturn(new User(username, passwordHash));

        UserDetails userDetails = userDetailsService.loadUserByUsername(username);

        assertEquals(username, userDetails.getUsername());
        assertEquals(passwordHash, userDetails.getPassword());
        assertTrue(userDetails.getAuthorities().stream()
                .anyMatch(grantedAuthority -> grantedAuthority.getAuthority().equals("USER")));
    }

    @Test
    void testLoadUserByUsername_UserNotFound() {
        String username = "invalidUser";

        when(userRepository.findByUsername(username)).thenThrow(new UserNotFoundException("User not found"));
        assertThrows(UserNotFoundException.class, () -> userDetailsService.loadUserByUsername(username));
    }
}
