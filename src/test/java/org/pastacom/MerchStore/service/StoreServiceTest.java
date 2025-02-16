package org.pastacom.MerchStore.service;

import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.pastacom.MerchStore.exception.InsufficientFundsException;
import org.pastacom.MerchStore.exception.ProductNotFoundException;
import org.pastacom.MerchStore.exception.UserNotFoundException;
import org.pastacom.MerchStore.model.Product;
import org.pastacom.MerchStore.model.User;
import org.pastacom.MerchStore.repository.StoreRepository;
import org.pastacom.MerchStore.repository.UserRepository;

import java.util.UUID;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StoreServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private StoreRepository storeRepository;

    @InjectMocks
    private StoreService storeService;

    static Stream<Object[]> provideBuyItemParams() {
        return Stream.of(
                new Object[]{"testUser", 100, "book", 50, null, 50},
                new Object[]{"unknownUser", 100, "book", 50, UserNotFoundException.class, 100},
                new Object[]{"testUser", 100, "unknownItem", 50, ProductNotFoundException.class, 100},
                new Object[]{"testUser", 30, "book", 50, InsufficientFundsException.class, 30}
        );
    }

    @ParameterizedTest
    @MethodSource("provideBuyItemParams")
    void testBuyItem(String username, int balance, String itemName, int price,
                                    Class<? extends RuntimeException> expectedException, int expectedBalance) {
        User user = new User(username, balance);
        Product product = new Product(UUID.randomUUID(), itemName, price);

        if (expectedException == UserNotFoundException.class) {
            when(userRepository.findByUsername(username)).thenThrow(new UserNotFoundException("User not found"));
        } else {
            when(userRepository.findByUsername(username)).thenReturn(user);
        }

        if (expectedException == ProductNotFoundException.class) {
            when(storeRepository.findByItemName(itemName)).thenThrow(new ProductNotFoundException("Product not found"));
        } else if (expectedException == null || expectedException == InsufficientFundsException.class) {
            when(storeRepository.findByItemName(itemName)).thenReturn(product);
        }

        if (expectedException != null) {
            assertThrows(expectedException, () -> storeService.buyItem(username, itemName));
        } else {
            storeService.buyItem(username, itemName);
            verify(userRepository).updateBalance(any(), eq(expectedBalance));
            verify(storeRepository).savePurchase(user.getId(), product.getId(), price);
        }
    }
}
