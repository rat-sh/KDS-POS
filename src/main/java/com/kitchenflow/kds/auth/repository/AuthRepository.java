package com.kitchenflow.kds.auth.repository;

import com.kitchenflow.kds.auth.api.AuthModels.UserView;
import com.kitchenflow.kds.common.security.ActorDirectory;
import com.kitchenflow.kds.common.security.ApiActor;
import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class AuthRepository implements ActorDirectory {
    private final JdbcTemplate jdbc;

    public AuthRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public Optional<UserRow> findByUsername(String username) {
        return jdbc.query("SELECT id, username, password_hash, role, active, version FROM app_user WHERE lower(username) = lower(?)",
                (rs, row) -> new UserRow(rs.getLong("id"), rs.getString("username"), rs.getString("password_hash"),
                        rs.getString("role"), rs.getBoolean("active"), rs.getLong("version")), username)
                .stream().findFirst();
    }

    public Optional<UserRow> lockValidRefresh(String tokenHash) {
        return jdbc.query("""
                SELECT u.id, u.username, u.role, u.active, u.version
                FROM refresh_token r JOIN app_user u ON u.id = r.user_id
                WHERE r.token_hash = ? AND r.revoked_at IS NULL AND r.expires_at > now()
                FOR UPDATE OF r
                """, (rs, row) -> new UserRow(rs.getLong("id"), rs.getString("username"), null,
                rs.getString("role"), rs.getBoolean("active"), rs.getLong("version")), tokenHash)
                .stream().findFirst();
    }

    public void rotateRefresh(String tokenHash) {
        jdbc.update("UPDATE refresh_token SET revoked_at = now() WHERE token_hash = ?", tokenHash);
    }

    public void revokeRefresh(String tokenHash) {
        jdbc.update("UPDATE refresh_token SET revoked_at = now() WHERE token_hash = ? AND revoked_at IS NULL", tokenHash);
    }

    public void insertRefresh(long userId, String tokenHash, Timestamp expiresAt) {
        jdbc.update("INSERT INTO refresh_token (user_id, token_hash, expires_at) VALUES (?, ?, ?)", userId, tokenHash, expiresAt);
    }

    public UserView findUserView(long userId) {
        return jdbc.query("SELECT id, username, role, active, version FROM app_user WHERE id = ?",
                (rs, row) -> new UserView(rs.getLong("id"), rs.getString("username"), rs.getString("role"),
                        rs.getBoolean("active"), stationIds(rs.getLong("id")), rs.getLong("version")), userId)
                .stream().findFirst().orElse(null);
    }

    @Override
    public ApiActor findActiveActor(long id) {
        return jdbc.query("SELECT id, username, role, version FROM app_user WHERE id = ? AND active = true",
                (rs, row) -> new ApiActor(rs.getLong("id"), rs.getString("username"), rs.getString("role"), rs.getLong("version")), id)
                .stream().findFirst().orElse(null);
    }

    public int userCount() {
        Integer count = jdbc.queryForObject("SELECT count(*) FROM app_user", Integer.class);
        return count == null ? 0 : count;
    }

    public void createBootstrapAdmin(String username, String displayName, String passwordHash) {
        jdbc.update("INSERT INTO app_user (username, display_name, password_hash, role) VALUES (?, ?, ?, 'ADMIN')",
                username, displayName, passwordHash);
    }

    private List<Long> stationIds(long userId) {
        return jdbc.queryForList("SELECT station_id FROM user_station WHERE user_id = ? ORDER BY station_id", Long.class, userId);
    }

    public record UserRow(long id, String username, String passwordHash, String role, boolean active, long version) {}
}
