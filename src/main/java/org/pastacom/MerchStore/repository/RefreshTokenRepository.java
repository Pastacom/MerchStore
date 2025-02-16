package org.pastacom.MerchStore.repository;

import org.pastacom.MerchStore.exception.UnauthorizedException;
import org.pastacom.MerchStore.model.RefreshToken;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Optional;
import java.util.UUID;

@Repository
public class RefreshTokenRepository {
    private final JdbcTemplate jdbcTemplate;

    @Autowired
    public RefreshTokenRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void save(RefreshToken refreshToken) {
        String sql = "INSERT INTO refresh_tokens (id, user_id, token, expires_at) VALUES (?, ?, ?, ?)";
        jdbcTemplate.update(sql,
                refreshToken.getId(),
                refreshToken.getUserId(),
                refreshToken.getToken(),
                Timestamp.from(refreshToken.getExpiresAt())
        );
    }

    public RefreshToken findByToken(String token) throws UnauthorizedException {
        String sql = "SELECT * FROM refresh_tokens WHERE token = ?";
        Optional<RefreshToken> result = jdbcTemplate.query(sql, this::mapRowToRefreshToken, token).stream().findFirst();
        if (result.isEmpty()) {
            throw new UnauthorizedException("Invalid refresh token provided");
        }
        return result.get();
    }

    public Optional<RefreshToken> findByUserId(UUID userId) {
        String sql = "SELECT * FROM refresh_tokens WHERE user_id = ?";
        return jdbcTemplate.query(sql, this::mapRowToRefreshToken, userId).stream().findFirst();
    }

    public void deleteByTokenId(UUID id) {
        String sql = "DELETE FROM refresh_tokens WHERE id = ?";
        jdbcTemplate.update(sql, id);
    }

    private RefreshToken mapRowToRefreshToken(ResultSet rs, int rowNum) throws SQLException {
        return new RefreshToken(UUID.fromString(rs.getString("id")),
                UUID.fromString(rs.getString("user_id")), rs.getString("token"),
                rs.getTimestamp("expires_at").toInstant());
    }
}
