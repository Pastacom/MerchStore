package org.pastacom.MerchStore.controller;

import org.pastacom.MerchStore.service.UserInfoService;
import org.pastacom.MerchStore.dto.InfoResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class InfoController {
    private final UserInfoService userInfoService;

    @Autowired
    public InfoController(UserInfoService userInfoService) {
        this.userInfoService = userInfoService;
    }

    @GetMapping("/info")
    public ResponseEntity<InfoResponse> getInfo(@AuthenticationPrincipal UserDetails userDetails) {
        String username = userDetails.getUsername();
        InfoResponse response = userInfoService.getUserInfo(username);
        return ResponseEntity.ok(response);
    }
}
