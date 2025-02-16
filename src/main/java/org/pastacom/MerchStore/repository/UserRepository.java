package org.pastacom.MerchStore.repository;

import org.pastacom.MerchStore.dto.InfoResponse;
import org.pastacom.MerchStore.exception.InsufficientFundsException;
import org.pastacom.MerchStore.exception.UserNotFoundException;
import org.pastacom.MerchStore.model.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class UserRepository {
    private final JdbcTemplate jdbcTemplate;

    @Autowired
    public UserRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    private final RowMapper<User> userRowMapper = (rs, rowNum) ->
            new User(UUID.fromString(rs.getString("id")), rs.getString("username"), rs.getString("password_hash"), rs.getInt("balance"));

    public Optional<User> findById(UUID id) {
        return jdbcTemplate.query("SELECT * FROM users WHERE id = ?", userRowMapper, id).stream().findFirst();
    }

    @Cacheable(value = "users", key = "#username", unless = "#result == null")
    public User findByUsername(String username) throws UserNotFoundException {
        Optional<User> result =  jdbcTemplate.query("SELECT * FROM users WHERE username = ?",
                userRowMapper, username).stream().findFirst();

        if (result.isEmpty())
        {
            throw new UserNotFoundException("Can't find user with username " + username);
        }

        return result.get();
    }

    public void saveUser(User user) {
        jdbcTemplate.update("INSERT INTO users (id, username, password_hash, balance) VALUES (?, ?, ?, ?)",
                user.getId(), user.getUsername(), user.getPasswordHash(), user.getBalance());
    }

    @CacheEvict(value = "users", key = "#user.getUsername()")
    public void updateBalance(User user, int newBalance) throws InsufficientFundsException {
        if (newBalance < 0)
        {
            throw new InsufficientFundsException("User's balance can't be negative");
        }

        jdbcTemplate.update(
                "UPDATE users SET balance = ? WHERE id = ?",
                newBalance, user.getId());
    }

    @Cacheable(value = "inventory", key = "#user.getId()", cacheManager = "cacheManagerWithTTL")
    public List<InfoResponse.Item> getInventory(User user) {
        return jdbcTemplate.query(
                """
                SELECT pro.item_name, COUNT(pur.id) as quantity
                FROM purchases pur
                JOIN products pro ON pur.product_id = pro.id
                WHERE pur.user_id = ?
                GROUP BY pro.item_name
                """,
                (rs, rowNum) -> new InfoResponse.Item(rs.getString("item_name"), rs.getInt("quantity")),
                user.getId()
        );
    }

    @Cacheable(value = "receivedTransactions", key = "#user.getId()", cacheManager = "cacheManagerWithTTL")
    public List<InfoResponse.ReceivingTransaction> getReceivingTransactions(User user) {
        return jdbcTemplate.query(
            """
            SELECT u.username AS sender, t.amount
            FROM transactions t
            JOIN users u ON t.sender_id = u.id
            WHERE t.receiver_id = ?
            """,
            (rs, rowNum) -> new InfoResponse.ReceivingTransaction(rs.getString("sender"),
            rs.getInt("amount")), user.getId());
    }

    @Cacheable(value = "sentTransactions", key = "#user.getId()", cacheManager = "cacheManagerWithTTL")
    public List<InfoResponse.SendingTransaction> getSendingTransactions(User user) {
        return jdbcTemplate.query(
            """
            SELECT u.username AS receiver, t.amount
            FROM transactions t
            JOIN users u ON t.receiver_id = u.id
            WHERE t.sender_id = ?
            """,
            (rs, rowNum) -> new InfoResponse.SendingTransaction(rs.getString("receiver"),
            rs.getInt("amount")), user.getId());
    }
}
