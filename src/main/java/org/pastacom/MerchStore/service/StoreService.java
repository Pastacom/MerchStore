package org.pastacom.MerchStore.service;

import org.pastacom.MerchStore.exception.InsufficientFundsException;
import org.pastacom.MerchStore.exception.ProductNotFoundException;
import org.pastacom.MerchStore.exception.UserNotFoundException;
import org.pastacom.MerchStore.model.Product;
import org.pastacom.MerchStore.model.User;
import org.pastacom.MerchStore.repository.StoreRepository;
import org.pastacom.MerchStore.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class StoreService {

    private final UserRepository userRepository;
    private final StoreRepository storeRepository;

    @Autowired
    public StoreService(UserRepository userRepository, StoreRepository storeRepository) {
        this.userRepository = userRepository;
        this.storeRepository = storeRepository;
    }

    @Transactional
    public void buyItem(String username, String item) throws UserNotFoundException, ProductNotFoundException,
            InsufficientFundsException {
        User user = userRepository.findByUsername(username);

        int balance = user.getBalance();

        Product product = storeRepository.findByItemName(item);

        if (product.getPrice() > balance)
        {
            throw new InsufficientFundsException("There are not enough coins to fulfill this purchase");
        }

        userRepository.updateBalance(user, balance-product.getPrice());
        storeRepository.savePurchase(user.getId(), product.getId(), product.getPrice());

    }
}
