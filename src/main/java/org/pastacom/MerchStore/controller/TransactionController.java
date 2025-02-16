package org.pastacom.MerchStore.controller;

import org.pastacom.MerchStore.dto.SendCoinRequest;
import org.pastacom.MerchStore.exception.InvalidParametersException;
import org.pastacom.MerchStore.service.TransactionService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class TransactionController {
    private final TransactionService transactionService;

    @Autowired
    public TransactionController(TransactionService transactionService) {
        this.transactionService = transactionService;
    }

    @PostMapping("/sendCoin")
    public ResponseEntity<?> sendCoins(@AuthenticationPrincipal UserDetails userDetails,
                                       @RequestBody SendCoinRequest request) throws InvalidParametersException {

        if (request.getToUser() == null || request.getToUser().trim().isEmpty()) {
            throw new InvalidParametersException("No receiver of the coins was provided");
        }

        if (request.getAmount() == 0) {
            throw new InvalidParametersException("No coins amount was provided");
        }

        String username = userDetails.getUsername();
        transactionService.sendCoins(username, request);
        return ResponseEntity.ok().build();
    }
}