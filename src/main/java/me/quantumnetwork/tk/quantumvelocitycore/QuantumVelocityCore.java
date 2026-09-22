package me.quantumnetwork.tk.quantumvelocitycore;

import com.google.inject.Inject;
import com.velocitypowered.api.event.EventTask;
import com.velocitypowered.api.event.ResultedEvent;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.command.CommandExecuteEvent;
import com.velocitypowered.api.event.connection.DisconnectEvent;
import com.velocitypowered.api.event.connection.LoginEvent;
import com.velocitypowered.api.event.player.PlayerChatEvent;
import com.velocitypowered.api.event.player.PlayerChooseInitialServerEvent;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.event.proxy.ProxyShutdownEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import lombok.Getter;
import me.quantumnetwork.tk.ChatType;
import me.quantumnetwork.tk.ServerType;
import me.quantumnetwork.tk.quantumvelocitycore.commands.CommandManager;
import me.quantumnetwork.tk.Rank;
import me.quantumnetwork.tk.States;
import me.quantumnetwork.tk.quantumvelocitycore.matchmaking.MatchmakingManager;
import me.quantumnetwork.tk.quantumvelocitycore.party.PartyManager;
import me.quantumnetwork.tk.quantumvelocitycore.players.PlayerManager;
import me.quantumnetwork.tk.quantumvelocitycore.players.PlayerProfile;
import me.quantumnetwork.tk.quantumvelocitycore.players.utils.Ban;
import me.quantumnetwork.tk.quantumvelocitycore.redis.RedisManager;
import me.quantumnetwork.tk.quantumvelocitycore.servers.ServerManager;
import me.quantumnetwork.tk.quantumvelocitycore.utils.DBManager;
import me.quantumnetwork.tk.quantumvelocitycore.utils.Utils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.slf4j.Logger;

import java.io.File;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@Plugin(
        id = "quantumvelocitycore",
        name = "QuantumVelocityCore",
        version = "1.0"
)
public class QuantumVelocityCore {


    @Getter
    private final Logger logger;

    @Getter
    private final ProxyServer proxy;

    public PlayerManager playerManager;

    public PartyManager partyManager;

    public CommandManager commandManager;

    @Getter
    private static QuantumVelocityCore instance;

    public Utils utils;

    public DBManager dbManager;

    public boolean whitelist = false;

    public RedisManager redisManager;

    public MatchmakingManager matchmakingManager;

    public States currentState = States.OFFLINE;

    public ServerType serverType = ServerType.PROXY;

    public String serverUUID = new File(".").getAbsoluteFile().getParentFile().getName();

    public ServerManager serverManager;

    @Inject
    public QuantumVelocityCore(ProxyServer proxy, Logger logger) {
        this.logger = logger;
        this.proxy = proxy;
        instance = this;

        logger.info("QuantumCore initializing...");

        redisManager = new RedisManager();
    }


    @Subscribe
    public void onProxyInitialization(ProxyInitializeEvent event) {

        dbManager = new DBManager();
        playerManager = new PlayerManager();
        partyManager = new PartyManager(playerManager);
        utils = new Utils();
        matchmakingManager = new MatchmakingManager();
        serverManager = new ServerManager(this);


        getLogger().info(proxy.getAllServers().toString());

        this.commandManager = new CommandManager(instance);

        redisManager.init();
        redisManager.updateServerState(States.GAME_STARTED);

        redisManager.initServersInRedis();

    }

    @Subscribe
    public void listenToCommand(CommandExecuteEvent e) {
        if (e.getCommand().startsWith("server")  || e.getCommand().startsWith("velocity") && e.getCommandSource() instanceof Player) {
            Rank rank = playerManager.get(((Player) e.getCommandSource()).getUniqueId()).getNetworkProfile().getRank();
            if (rank.canRunCommand(Rank.ADMIN)) {
                e.setResult(CommandExecuteEvent.CommandResult.denied());
                e.getCommandSource().sendMessage(Component.text("You do not have permission to use this command", NamedTextColor.RED));
            }
        }
    }


    @Subscribe
    public void proxyShutDown(ProxyShutdownEvent e) {

        logger.info("Proxy shutting down... starting cleanup");

        // 1. Stop gameplay systems first
        playerManager.removeAllPlayersFromParties();
        playerManager.clear();

        // 2. Flush Redis
        CompletableFuture<Void> redisFuture = redisManager.clearData();

        try {
            redisFuture
                    .orTimeout(5, TimeUnit.SECONDS)
                    .whenComplete((res, ex) -> {

                        if (ex != null) {
                            logger.error("Redis cleanup failed or timed out", ex);
                        }

                        redisManager.shutdown();

                        // 3. Always close DB AFTER Redis attempt
                        try {
                            dbManager.getConnection().close();
                        } catch (Exception dbEx) {
                            logger.error("Error closing database connection", dbEx);
                        }

                        logger.info("Shutdown cleanup completed");
                    })
                    .join(); // ensures JVM doesn't exit early

        } catch (Exception e1) {
            logger.error("Fatal error during shutdown sequence", e1);

            // fallback DB close (VERY important)
            try {
                dbManager.connection.close();
            } catch (Exception ignored) {}
        }
    }


    @Subscribe
    public void playerChooseInitialServerEvent(PlayerChooseInitialServerEvent event) {
        RegisteredServer target = matchmakingManager.findLobby(event.getPlayer());

        if (target != null) {
            event.setInitialServer(target);
        } else {
            logger.warn("No lobby found for {}", event.getPlayer().getUsername());
            getProxy().getPlayer(event.getPlayer().getUsername()).ifPresent(player -> player.disconnect(Component.text("No lobby server available. Please try again later.", NamedTextColor.RED)));
        }
    }


    @Subscribe
    public void playerChatEvent(PlayerChatEvent e ) {

        if (e.getMessage().equalsIgnoreCase("party")) {
            instance.getLogger().info("==== PARTY DEBUG DUMP ====");

            for (PlayerProfile profile : playerManager.getAllPlayers()) {
                if (profile != null && profile.getNetworkProfile().getParty() != null) {
                    instance.getLogger().info("Player={} Party={}", profile.getUsername(), profile.getNetworkProfile().getParty());
                }
            }
            instance.getLogger().info("==== END PARTY DUMP ====");
        }

        if (e.getMessage().equalsIgnoreCase("profile")) {
            instance.getLogger().info("==== PROFILE DEBUG DUMP ====");

            for (PlayerProfile profile : playerManager.getAllPlayers()) {
                instance.getLogger().info("Player={} Rank={} ChatChannel={} Party={}", profile.getUsername(), profile.getNetworkProfile().getRank(), profile.getNetworkProfile().getChatChannel(), profile.getNetworkProfile().getParty());
            }
            instance.getLogger().info("==== PROFILE PARTY DUMP ====");
        }

        Player player = e.getPlayer();
        String message = e.getMessage();
        PlayerProfile profile = playerManager.get(player.getUniqueId());

        if (playerManager.get(player.getUniqueId()).getNetworkProfile().getChatChannel().equals(ChatType.PARTY)) {
            partyManager.handlePartyChat(player, message);
        } else {


            e.getPlayer().getCurrentServer().ifPresent(serverConnection -> {
                Component formattedMessage = profile.getDisplayName()
                        .append(Component.text(": " + message, NamedTextColor.WHITE));

                serverConnection.getServer()
                        .getPlayersConnected()
                        .forEach(p -> p.sendMessage(formattedMessage));
            });

        }
    }






    @Subscribe
    public EventTask onLogin(LoginEvent e) {

        Player player = e.getPlayer();
        UUID uuid = player.getUniqueId();

        if (proxy.getPlayer(uuid).isPresent()) {
            e.setResult(ResultedEvent.ComponentResult.denied(
                    Component.text("You are already on the server!", NamedTextColor.RED)
            ));
            return EventTask.resumeWhenComplete(CompletableFuture.completedFuture(null));
        }

        CompletableFuture<Ban> banFuture = playerManager.banRepository.getBan(uuid);

        CompletableFuture<Boolean> whitelistFuture = whitelist
                ? playerManager.whitelistRepository.isWhitelisted(uuid)
                : CompletableFuture.completedFuture(true);

        CompletableFuture<PlayerProfile> profileFuture = playerManager.load(player);


        CompletableFuture<Void> combined = CompletableFuture.allOf(
                banFuture, whitelistFuture, profileFuture
        ).thenAccept(v -> {

            Ban ban = banFuture.join();

            if (ban.banned()) {
                if (ban.permanent()) {
                    e.setResult(ResultedEvent.ComponentResult.denied(
                            Component.text("You have been permanently banned!", NamedTextColor.RED)
                                    .append(Component.newline())
                                    .append(Component.text("Reason: " + ban.reason(), NamedTextColor.RED))));
                } else {
                    e.setResult(ResultedEvent.ComponentResult.denied(
                            Component.text("You have been banned for " + utils.formatTime(ban.timeRemaining()), NamedTextColor.RED)));
                }
                return;
            }

            boolean whitelisted = whitelistFuture.join();
            if (!whitelisted) {
                e.setResult(ResultedEvent.ComponentResult.denied(
                        Component.text("You are not whitelisted.", NamedTextColor.RED)));
                return;
            }

            redisManager.updatePlayerState(profileFuture.join());
            getLogger().info("{} successfully logged in", player.getUsername());

        }).exceptionally(ex -> {
            getLogger().error("Error while processing login", ex);
            e.setResult(ResultedEvent.ComponentResult.denied(
                    Component.text("An error occurred while logging in.", NamedTextColor.RED)));
            return null;
        });



        return EventTask.resumeWhenComplete(combined);
    }



    //When the user leaves the proxy, it removes the player from the party and removes the player's rank from the playerRanks map
    @Subscribe
    public void playerDisconnectEvent(DisconnectEvent event) {
        if (event.getPlayer() == null) {
            return;
        }

        PlayerProfile profile = playerManager.get(event.getPlayer().getUniqueId());

        if (playerManager.get(event.getPlayer().getUniqueId()) == null) {
            return;
        }

        playerManager.networkRepository.savePlayer(profile);
        playerManager.disconnectPlayer(event.getPlayer());


    }



}
