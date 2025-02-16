package org.pastacom.MerchStore.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@AllArgsConstructor
@NoArgsConstructor
public class SendCoinRequest {
    private String toUser;

    private int amount;
}
