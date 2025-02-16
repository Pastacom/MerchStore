package org.pastacom.MerchStore.repository;

import org.pastacom.MerchStore.exception.ProductNotFoundException;
import org.pastacom.MerchStore.model.Product;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public class StoreRepository {
    private final JdbcTemplate jdbcTemplate;

    @Autowired
    public StoreRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Cacheable(value = "products", key = "#itemName", cacheManager = "cacheManagerWithTTL")
    public Product findByItemName(String itemName) throws ProductNotFoundException {
        Optional<Product> result = jdbcTemplate.query(
                "SELECT id, price FROM products WHERE item_name = ?",
                (rs, rowNum) -> new Product(
                        UUID.fromString(rs.getString("id")), itemName,
                        rs.getInt("price")
                ),
                itemName
        ).stream().findFirst();

        if (result.isEmpty())
        {
            throw new ProductNotFoundException("Can't find product of type " + itemName);
        }

        return result.get();
    }

    @CacheEvict(value = "inventory", key = "#userId")
    public void savePurchase(UUID userId, UUID productId, int productPrice) {
        jdbcTemplate.update(
                "INSERT INTO purchases (user_id, product_id, price) VALUES (?, ?, ?)",
                userId, productId, productPrice
        );
    }
}
