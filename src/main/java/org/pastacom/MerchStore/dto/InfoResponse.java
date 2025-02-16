package org.pastacom.MerchStore.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@AllArgsConstructor
public class InfoResponse {
    private int coins;
    private List<Item> inventory;
    private CoinHistory coinHistory;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Item {
        private String type;
        private int quantity;
    }

    public static class Transaction {}

    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    public static class SendingTransaction extends Transaction {
        private String toUser;
        private int amount;
    }

    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    public static class ReceivingTransaction extends Transaction {
        private String fromUser;
        private int amount;
    }

    @Getter
    @AllArgsConstructor
    public static class CoinHistory {
        private final List<ReceivingTransaction> received;
        private final List<SendingTransaction> sent;
    }
}
