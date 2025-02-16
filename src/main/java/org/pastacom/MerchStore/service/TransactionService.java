package org.pastacom.MerchStore.service;

import org.pastacom.MerchStore.dto.SendCoinRequest;
import org.pastacom.MerchStore.exception.InsufficientFundsException;
import org.pastacom.MerchStore.exception.SelfTransactionException;
import org.pastacom.MerchStore.exception.UserNotFoundException;
import org.pastacom.MerchStore.model.User;
import org.pastacom.MerchStore.repository.TransactionRepository;
import org.pastacom.MerchStore.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TransactionService {

    private final UserRepository userRepository;
    private final TransactionRepository transactionRepository;

    @Autowired
    public TransactionService(UserRepository userRepository, TransactionRepository transactionRepository) {
        this.userRepository = userRepository;
        this.transactionRepository = transactionRepository;
    }

    @Transactional
    public void sendCoins(String username, SendCoinRequest request) throws SelfTransactionException,
            UserNotFoundException, InsufficientFundsException{
        if (request.getToUser().equals(username))
        {
            throw new SelfTransactionException("You can't send coins to yourself");
        }

        User sender = userRepository.findByUsername(username);

        User receiver = userRepository.findByUsername(request.getToUser());

        int amount = request.getAmount();

        if (sender.getBalance() < amount)
        {
            throw new InsufficientFundsException("There are not enough coins to fulfill this transaction");
        }

        userRepository.updateBalance(sender, sender.getBalance()-amount);
        userRepository.updateBalance(receiver, receiver.getBalance()+amount);
        transactionRepository.saveTransaction(sender.getId(), receiver.getId(), amount);
    }
}
