package org.pastacom.MerchStore.repository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public class TransactionRepository {
    private final JdbcTemplate jdbcTemplate;

    @Autowired
    private CacheManager cacheManager;

    @Autowired
    public TransactionRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void saveTransaction(UUID sender_id, UUID receiver_id, int amount) {
        Cache transactionsCache = cacheManager.getCache("receivedTransactions");
        if (transactionsCache != null) {
            transactionsCache.evict(receiver_id);
        }

        transactionsCache = cacheManager.getCache("sentTransactions");
        if (transactionsCache != null) {
            transactionsCache.evict(sender_id);
        }

        jdbcTemplate.update(
                "INSERT INTO transactions (sender_id, receiver_id, amount) VALUES (?, ?, ?)",
                sender_id, receiver_id, amount
        );
    }
}
