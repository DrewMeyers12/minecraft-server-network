package me.quantumnetwork.tk.quantumvelocitycore.players.databaseRepositories;

import me.quantumnetwork.tk.quantumvelocitycore.QuantumVelocityCore;
import me.quantumnetwork.tk.quantumvelocitycore.utils.DBManager;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class WhitelistRepository {

    private final QuantumVelocityCore plugin;
    private final DBManager db;

    public WhitelistRepository() {
        this.plugin = QuantumVelocityCore.getInstance();
        this.db = plugin.dbManager;
    }


    public CompletableFuture<Boolean> isWhitelisted(UUID uuid) {

        return CompletableFuture.supplyAsync(() -> {

            String sql = """
                    SELECT 1
                    FROM whitelist
                    WHERE PlayerUUID = ?
                    """;

            try (PreparedStatement statement =
                         db.getConnection().prepareStatement(sql)) {

                statement.setString(1, uuid.toString());

                ResultSet rs = statement.executeQuery();

                return rs.next();

            } catch (SQLException e) {
                throw new RuntimeException(e);
            }

        }, db.getExecutor());

    }
}
