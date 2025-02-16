package org.pastacom.MerchStore.service;

import org.pastacom.MerchStore.dto.InfoResponse;
import org.pastacom.MerchStore.exception.UserNotFoundException;
import org.pastacom.MerchStore.model.User;
import org.pastacom.MerchStore.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UserInfoService {
    private final UserRepository userRepository;

    @Autowired
    public UserInfoService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public InfoResponse getUserInfo(String username) throws UserNotFoundException {
        User user = userRepository.findByUsername(username);

        List<InfoResponse.Item> inventory = userRepository.getInventory(user);

        List<InfoResponse.ReceivingTransaction> received = userRepository.getReceivingTransactions(user);

        List<InfoResponse.SendingTransaction> sent = userRepository.getSendingTransactions(user);

        return new InfoResponse(user.getBalance(), inventory, new InfoResponse.CoinHistory(received, sent));
    }
}
