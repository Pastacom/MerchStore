package org.pastacom.MerchStore.controller;

import org.pastacom.MerchStore.exception.InvalidParametersException;
import org.pastacom.MerchStore.service.StoreService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class StoreController {
    private final StoreService storeService;

    @Autowired
    public StoreController(StoreService storeService) {
        this.storeService = storeService;
    }

    @GetMapping("/buy/{item}")
    public ResponseEntity<?> buyProduct(@AuthenticationPrincipal UserDetails userDetails, @PathVariable String item)
            throws InvalidParametersException {

        if (item == null || item.trim().isEmpty()) {
            throw new InvalidParametersException("No item type was provided");
        }

        String username = userDetails.getUsername();
        storeService.buyItem(username, item);
        return ResponseEntity.ok().build();
    }
}