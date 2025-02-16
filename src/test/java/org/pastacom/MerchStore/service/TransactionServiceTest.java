package org.pastacom.MerchStore.service;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.pastacom.MerchStore.dto.SendCoinRequest;
import org.pastacom.MerchStore.exception.InsufficientFundsException;
import org.pastacom.MerchStore.exception.SelfTransactionException;
import org.pastacom.MerchStore.exception.UserNotFoundException;
import org.pastacom.MerchStore.model.User;
import org.pastacom.MerchStore.repository.TransactionRepository;
import org.pastacom.MerchStore.repository.UserRepository;

import org.junit.jupiter.api.extension.ExtendWith;

import java.util.stream.Stream;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TransactionServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private TransactionRepository transactionRepository;

    @InjectMocks
    private TransactionService transactionService;

    static Stream<Object[]> provideSendCoinsParams() {
        return Stream.of(
                new Object[]{"senderUser", 100, "receiverUser", 50, null, 50, 150},
                new Object[]{"senderUser", 100, "senderUser", 50, SelfTransactionException.class, 100, 100},
                new Object[]{"unknownUser", 100, "receiverUser", 50, UserNotFoundException.class, 100, 100},
                new Object[]{"senderUser", 100, "unknownUser", 50, UserNotFoundException.class, 100, 100},
                new Object[]{"senderUser", 30, "receiverUser", 50, InsufficientFundsException.class, 30, 100}
        );
    }

    @ParameterizedTest
    @MethodSource("provideSendCoinsParams")
    void testSendCoins(String fromUser, int senderBalance, String toUser, int amount,
                       Class<? extends RuntimeException> expectedException, int expectedSenderBalance,
                       int expectedReceiverBalance) {
        SendCoinRequest request = new SendCoinRequest(toUser, amount);

        if (expectedException != SelfTransactionException.class) {
            if (fromUser.equals("senderUser")) {
                when(userRepository.findByUsername("senderUser"))
                        .thenReturn(new User("senderUser", 100));
                if (toUser.equals("receiverUser")) {
                    when(userRepository.findByUsername("receiverUser"))
                            .thenReturn(new User("receiverUser", 100));
                } else {
                    when(userRepository.findByUsername(fromUser))
                            .thenThrow(new UserNotFoundException("Receiver not found"));
                }
            } else {
                when(userRepository.findByUsername(fromUser)).thenThrow(new UserNotFoundException("Sender not found"));
            }
        }

        if (expectedException == InsufficientFundsException.class) {
            User sender = new User(fromUser, senderBalance);
            when(userRepository.findByUsername(fromUser)).thenReturn(sender);
        }

        if (expectedException != null) {
            assertThrows(expectedException, () -> transactionService.sendCoins(fromUser, request));
        } else {
            assertDoesNotThrow(() -> transactionService.sendCoins(fromUser, request));
            verify(userRepository).updateBalance(any(User.class), eq(expectedSenderBalance));
            verify(userRepository).updateBalance(any(User.class), eq(expectedReceiverBalance));
            verify(transactionRepository).saveTransaction(any(), any(), eq(amount));
        }
    }
}