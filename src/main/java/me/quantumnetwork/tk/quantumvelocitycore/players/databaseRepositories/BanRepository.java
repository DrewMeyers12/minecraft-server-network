package me.quantumnetwork.tk.quantumvelocitycore.players.databaseRepositories;

import com.velocitypowered.api.proxy.Player;
import me.quantumnetwork.tk.quantumvelocitycore.QuantumVelocityCore;
import me.quantumnetwork.tk.quantumvelocitycore.players.utils.Ban;
import me.quantumnetwork.tk.quantumvelocitycore.utils.DBManager;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class BanRepository {

    private final QuantumVelocityCore plugin;
    private final DBManager db;

    public BanRepository() {
        this.plugin = QuantumVelocityCore.getInstance();
        this.db = plugin.dbManager;
    }

    public CompletableFuture<Boolean> banPlayer(
            UUID uuid,
            String name,
            String reason,
            String bannedBy,
            LocalDateTime unbanDate
    ) {
        return CompletableFuture.supplyAsync(() -> {

            String query = """
                INSERT INTO bannedplayers
                (PlayerUUID, PlayerName, BanDate, BanReason, BannedBy, UnbanDate)
                VALUES (?, ?, ?, ?, ?, ?)
                ON DUPLICATE KEY UPDATE
                
                BanDate = VALUES(BanDate),
                BanReason = VALUES(BanReason),
                BannedBy = VALUES(BannedBy),
                UnbanDate = VALUES(UnbanDate);
                """;

            try (PreparedStatement stmt = plugin.dbManager.connection.prepareStatement(query)) {

                stmt.setString(1, uuid.toString());
                stmt.setString(2, name);
                stmt.setTimestamp(3, Timestamp.valueOf(LocalDateTime.now()));
                stmt.setString(4, reason);
                stmt.setString(5, bannedBy);

                if (unbanDate != null) {
                    stmt.setTimestamp(6, Timestamp.valueOf(unbanDate));
                } else {
                    stmt.setNull(6, Types.TIMESTAMP);
                }

                stmt.executeUpdate();
                return true;

            } catch (SQLException e) {
                plugin.getLogger().error("Failed to ban {} ({})", name, uuid, e);
                return false;
            }
        });
    }

    public CompletableFuture<Ban> getBan(UUID uuid) {

        return CompletableFuture.supplyAsync(() -> {

            String sql = """
                SELECT BanReason, UnbanDate
                FROM bannedplayers
                WHERE PlayerUUID = ?
                """;

            try (PreparedStatement statement = db.getConnection().prepareStatement(sql)) {

                statement.setString(1, uuid.toString());

                try (ResultSet rs = statement.executeQuery()) {

                    // Player isn't banned
                    if (!rs.next()) {
                        return new Ban(false, false, 0, null);
                    }

                    String reason = rs.getString("BanReason");
                    Timestamp unbanDate = rs.getTimestamp("UnbanDate");

                    // Permanent ban
                    if (unbanDate == null) {
                        return new Ban(true, true, 0, reason);
                    }

                    long timeRemaining = unbanDate.getTime() - System.currentTimeMillis();

                    // Ban has expired
                    if (timeRemaining <= 0) {

                        String deleteSql = """
                            DELETE FROM bannedplayers
                            WHERE PlayerUUID = ?
                            """;

                        try (PreparedStatement delete = db.getConnection().prepareStatement(deleteSql)) {
                            delete.setString(1, uuid.toString());
                            delete.executeUpdate();
                        }

                        return new Ban(false, false, 0, null);
                    }

                    // Active temporary ban
                    return new Ban(true, false, timeRemaining, reason);

                }

            } catch (SQLException e) {
                throw new RuntimeException(e);
            }

        }, db.getExecutor());
    }

    public CompletableFuture<Boolean> pardon(String name) {

        plugin.getLogger().info("Pardoning player {}", name);

        return db.fetchPlayerUUIDAsync(name).thenCompose(uuid ->
                CompletableFuture.supplyAsync(() -> {

                    String query = "DELETE FROM bannedplayers WHERE PlayerUUID = ?";

                    try (PreparedStatement statement =
                                 plugin.dbManager.connection.prepareStatement(query)) {

                        statement.setString(1, uuid.toString());

                        int rowsAffected = statement.executeUpdate();

                        if (rowsAffected > 0) {
                            plugin.getLogger().info("Player {} has been pardoned.", name);
                            return true;
                        } else {
                            plugin.getLogger().warn("Failed to pardon player {}. Check your spelling or try again.", name);
                            return false;
                        }

                    } catch (SQLException e) {
                        plugin.getLogger().error("Error executing SQL query.", e);
                        return false;
                    }

                }, db.getExecutor())
        );
    }

}