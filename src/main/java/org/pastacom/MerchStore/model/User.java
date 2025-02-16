package org.pastacom.MerchStore.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class User {

    private UUID id;

    private String username;

    private String passwordHash;

    private int balance;

    public User(String username, int balance) {
        this.username = username;
        this.balance = balance;
    }

    public User(String username, String passwordHash) {
        this.username = username;
        this.passwordHash = passwordHash;
    }
}
