package me.quantumnetwork.tk.quantumvelocitycore.matchmaking;

import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ServerConnection;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import me.quantumnetwork.tk.ServerType;
import me.quantumnetwork.tk.quantumvelocitycore.QuantumVelocityCore;
import me.quantumnetwork.tk.quantumvelocitycore.party.Party;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import java.util.*;

public class MatchmakingManager {

    QuantumVelocityCore instance;

    public MatchmakingManager() {
        instance = QuantumVelocityCore.getInstance();
    }


    //This system needs to take into account two parties trying to connect to the same server/matchmake at the same time.
    // In order to do this, I need to reserve the server within redis.
    // For now this should work fine though

    public void findServerWithAvailableSlots(Player player, String game) {

        ArrayList<RegisteredServer> matchmakingServers = new ArrayList<>();
        RegisteredServer[] targetServer = {null};
        RegisteredServer[] backupServer = {null};

        int targetServerMaxCapacity = 0;
        int backupServerMaxCapacity = 0;

        Collection<RegisteredServer> servers = instance.getProxy().getAllServers();

        // Filter by game prefix
        for (RegisteredServer server : servers) {
            String serverName = server.getServerInfo().getName().toLowerCase();

            if (serverName.startsWith(game.toLowerCase())
                    && serverName.length() > game.length()
                    && Character.isDigit(serverName.charAt(game.length()))) {

                matchmakingServers.add(server);
            }
        }

        if (matchmakingServers.isEmpty()) {
            player.sendMessage(Component.text("No servers available for the specified game", NamedTextColor.RED));
            return;
        }

        // Sort by current player count
        matchmakingServers.sort(Comparator.comparingInt(s -> s.getPlayersConnected().size()));

        Set<String> serverIds = instance.redisManager.get().smembers("servers:" + game);

        instance.getLogger().info("Found " + serverIds.size() + " servers for game: " + game);
        instance.getLogger().info("Matchmaking servers: " + matchmakingServers);

        for (int i = matchmakingServers.size() - 1; i >= 0; i--) {

            RegisteredServer server = matchmakingServers.get(i);

            String key = "server:" + server.getServerInfo().getName();
            Map<String, String> data = instance.redisManager.get().hgetall(key);

            if (data == null || data.isEmpty()) continue;

            int maxCapacity = Integer.parseInt(data.getOrDefault("maximum_players", "0"));
            String gameState = data.get("game_state");

            int currentPlayers = server.getPlayersConnected().size();

            if (currentPlayers < maxCapacity && "waiting_for_players".equals(gameState)) {
                targetServer[0] = server;
                targetServerMaxCapacity = maxCapacity;
                break;
            }

            if (currentPlayers < maxCapacity
                    && "game_started".equals(gameState)
                    && backupServer[0] == null) {
                backupServer[0] = server;
                backupServerMaxCapacity = maxCapacity;
            }
        }

        if (targetServer[0] == null) {
            targetServer[0] = backupServer[0];
            targetServerMaxCapacity = backupServerMaxCapacity;
        }

        if (targetServer[0] == null) {
            player.sendMessage(Component.text("No servers available for the specified game", NamedTextColor.RED));
            return;
        }

        // Party logic (unchanged)
        if (instance.playerManager.get(player.getUniqueId()).getNetworkProfile().getParty() != null) {

            Party party = instance.playerManager.get(player.getUniqueId()).getNetworkProfile().getParty();

            if (party.getLeader().equals(player.getUniqueId())) {

                if (targetServer[0].getPlayersConnected().size()
                        + (party.getMembers().size() - 1)
                        <= targetServerMaxCapacity) {

                    for (UUID member : party.getMembers()) {
                        Optional<Player> partyMember = instance.getProxy().getPlayer(member);
                        partyMember.ifPresent(value ->
                                value.createConnectionRequest(targetServer[0]).connect()
                        );
                    }
                }
            }
            return;
        }

        player.createConnectionRequest(targetServer[0]).connect();
    }


    public RegisteredServer findLobby(Player player) {
        Optional<ServerConnection> currentServer = player.getCurrentServer();

        instance.getLogger().info("Finding lobby for player " + player.getUsername() + ", current server: " + (currentServer.isPresent() ? currentServer.get().getServerInfo().getName() : "none"));
        // Should use the servermanager to find the server uuid and then the server type

        if (currentServer.isPresent() && currentServer.get().getServerInfo().getName().toLowerCase().contains("lobby")) {
            player.sendMessage(Component.text("You are already in a lobby", NamedTextColor.YELLOW));
            return currentServer.get().getServer();
        }

        Collection<RegisteredServer> servers = instance.getProxy().getAllServers();
        int highestPlayerCount = -1;
        RegisteredServer targetServer = null;

        for (RegisteredServer server : servers) {
            if (instance.serverManager.getServerType(server.getServerInfo().getName()) == ServerType.LOBBY
                    && server.getPlayersConnected().size() > highestPlayerCount) {
                highestPlayerCount = server.getPlayersConnected().size();
                targetServer = server;
            }
        }
        return targetServer;
    }

    public int getMaxPlayers(RegisteredServer server) {
        return instance.serverManager.serversByName.get(server.getServerInfo().getName()).getMaxPlayers();
    }
}
