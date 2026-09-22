package me.quantumnetwork.tk.quantumvelocitycore.players;

import com.velocitypowered.api.proxy.Player;
import me.quantumnetwork.tk.ChatType;
import me.quantumnetwork.tk.Rank;
import me.quantumnetwork.tk.quantumvelocitycore.QuantumVelocityCore;
import me.quantumnetwork.tk.quantumvelocitycore.players.databaseRepositories.BanRepository;
import me.quantumnetwork.tk.quantumvelocitycore.players.databaseRepositories.NetworkRepository;
import me.quantumnetwork.tk.quantumvelocitycore.players.databaseRepositories.WhitelistRepository;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class PlayerManager {

    private final QuantumVelocityCore instance;

    //Repos
    public NetworkRepository networkRepository;
    public BanRepository banRepository;
    public WhitelistRepository whitelistRepository;

    public PlayerManager() {
        this.instance = QuantumVelocityCore.getInstance();

        networkRepository = new NetworkRepository();
        banRepository = new BanRepository();
        whitelistRepository = new WhitelistRepository();
    }

    public CompletableFuture<PlayerProfile> load(Player player) {

        if (instance.getProxy().getConfiguration().isOnlineMode()) {

            return networkRepository.loadPlayer(player.getUniqueId())
                    .thenApply(profile -> {

                        if (profile == null) {
                            profile = new PlayerProfile(player.getUniqueId(), player.getUsername());
                            profile.getNetworkProfile().setRank(Rank.DEFAULT);
                            profile.getNetworkProfile().setChatChannel(ChatType.GLOBAL);
                        }
                        players.put(player.getUniqueId(), profile);

                        return profile;

                    });
        } else {

                PlayerProfile profile =
                        new PlayerProfile(player.getUniqueId(), player.getUsername());

                profile.getNetworkProfile().setRank(Rank.DEFAULT);

                profile.getNetworkProfile().setChatChannel(ChatType.GLOBAL);

                instance.getLogger().info("Information was not loaded from the database for " + player.getUsername() + " because the server is in OFFLINE MODE!");

                return CompletableFuture.completedFuture(profile);


            }
        }

    public void disconnectPlayer(Player player) {
        instance.redisManager.removePlayerFromRedis(player.getUniqueId());
        instance.partyManager.leaveParty(player);
        players.values().removeIf(profile -> profile.getUuid().equals(player.getUniqueId()));
    }


    private final Map<UUID, PlayerProfile> players = new HashMap<>();

    public PlayerProfile get(UUID uuid) {
        return players.get(uuid);
    }

    public void removeAllPlayersFromParties() {
        for (PlayerProfile profile : players.values()) {
            profile.getNetworkProfile().setParty(null);
        }
    }

    public Collection<PlayerProfile> getAllPlayers() {
        return players.values();
    }

    public void clear() {
        for (PlayerProfile profile : players.values()) {
            profile.getNetworkProfile().setParty(null);
            players.remove(profile.getUuid());
        }
    }



}

