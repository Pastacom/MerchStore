package org.pastacom.MerchStore.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class RefreshToken {
    private UUID id;
    private UUID userId;
    private String token;
    private Instant expiresAt;
}
