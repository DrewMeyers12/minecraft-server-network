package me.quantumnetwork.tk.quantumvelocitycore.players.databaseRepositories;

import com.velocitypowered.api.proxy.Player;
import me.quantumnetwork.tk.ChatType;
import me.quantumnetwork.tk.Rank;
import me.quantumnetwork.tk.quantumvelocitycore.QuantumVelocityCore;
import me.quantumnetwork.tk.quantumvelocitycore.players.PlayerProfile;
import me.quantumnetwork.tk.quantumvelocitycore.players.utils.RankUpdateResult;
import me.quantumnetwork.tk.quantumvelocitycore.utils.DBManager;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class NetworkRepository {

    private final QuantumVelocityCore plugin;
    private final DBManager db;

    public NetworkRepository() {
        this.plugin = QuantumVelocityCore.getInstance();
        this.db = plugin.dbManager;
    }

    public CompletableFuture<PlayerProfile> loadPlayer(UUID uuid) {
        return CompletableFuture.supplyAsync(() -> {

            String sql = "SELECT * FROM playerinfo WHERE uuid = ?";

            try (PreparedStatement statement =
                         db.getConnection().prepareStatement(sql)) {

                statement.setString(1, uuid.toString());

                ResultSet rs = statement.executeQuery();

                if (!rs.next())
                    return null;

                PlayerProfile profile =
                        new PlayerProfile(uuid, rs.getString("name"));

                profile.getNetworkProfile().setRank(
                        Rank.valueOf(rs.getString("playerRank"))
                );

                profile.getNetworkProfile().setChatChannel(
                        ChatType.valueOf(rs.getString("chatChannel"))
                );

                return profile;

            } catch (SQLException e) {

                plugin.getLogger().error("Failed to load player.", e);

                return null;

            }
        }, db.getExecutor());

    }



    public CompletableFuture<Void> savePlayer(PlayerProfile profile) {

        if (!plugin.getProxy().getConfiguration().isOnlineMode()) {
            plugin.getLogger().info("Player information for " + profile.getUsername() + " was not saved because the server is in OFFLINE MODE!");
            return CompletableFuture.completedFuture(null);
        }

        return CompletableFuture.runAsync(() -> {

            String sql = """
        INSERT INTO playerinfo (uuid, name, playerRank, chatChannel)
        VALUES (?, ?, ?, ?)
        ON DUPLICATE KEY UPDATE
            name = VALUES(name),
            chatChannel = VALUES(chatChannel);
        """;

            try (PreparedStatement statement =
                         db.getConnection().prepareStatement(sql)) {

                statement.setString(1, profile.getUuid().toString());
                statement.setString(2, profile.getUsername());
                statement.setString(3, profile.getNetworkProfile().getRank().name());
                statement.setString(4, profile.getNetworkProfile().getChatChannel().name());

                statement.executeUpdate();

            } catch (SQLException e) {

                plugin.getLogger().error("Failed to save player.", e);

            }

        }, db.getExecutor());

    }

    //The ONLY thing that gets saved directly to the database without a full save.
    public CompletableFuture<RankUpdateResult> updateRank(UUID uuid, Rank rank) {

        return CompletableFuture.supplyAsync(() -> {

            String sql = """
                UPDATE playerinfo
                SET playerRank = ?
                WHERE uuid = ?
                """;

            try (PreparedStatement statement =
                 plugin.dbManager.connection.prepareStatement(sql)) {


                statement.setString(1, rank.name());
                statement.setString(2, uuid.toString());

                int updated = statement.executeUpdate();


                if (updated == 0) {
                    return new RankUpdateResult(
                            false,
                            false,
                            null
                    );
                }


                return new RankUpdateResult(
                        true,
                        true,
                        null
                );


            } catch (SQLException e) {

                return new RankUpdateResult(
                        false,
                        false,
                        e.getMessage()
                );
            }

        }, db.getExecutor());
    }


}
