package me.quantumnetwork.tk.quantumvelocitycore.redis;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.velocitypowered.api.proxy.Player;
import io.lettuce.core.RedisClient;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.sync.RedisCommands;
import io.lettuce.core.pubsub.RedisPubSubAdapter;
import io.lettuce.core.pubsub.RedisPubSubListener;
import io.lettuce.core.pubsub.StatefulRedisPubSubConnection;
import io.lettuce.core.pubsub.api.sync.RedisPubSubCommands;
import me.quantumnetwork.tk.quantumvelocitycore.QuantumVelocityCore;
import me.quantumnetwork.tk.RedisMessageType;
import me.quantumnetwork.tk.States;
import me.quantumnetwork.tk.quantumvelocitycore.players.PlayerProfile;
import me.quantumnetwork.tk.quantumvelocitycore.servers.Server;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public class RedisManager {

    private QuantumVelocityCore plugin;

    private RedisClient client;

    private StatefulRedisConnection<String, String> commandConnection;
    private RedisCommands<String, String> redis;

    private StatefulRedisPubSubConnection<String, String> pubSubConnection;
    private RedisPubSubCommands<String, String> pubSub;

    private final Map<RedisMessageType, Consumer<JsonObject>> handlers = new HashMap<>();

    public void init() {

        try {
            plugin = QuantumVelocityCore.getInstance();

            client = RedisClient.create("redisConnectionAccountRedacted");

            // Normal Redis commands
            try {
                commandConnection = client.connect();
            } catch (Throwable e) {
                plugin.getLogger().error("Error connecting to Redis", e);
                plugin.getProxy().shutdown();
                return;
            }

            redis = commandConnection.sync();

            // Pub/Sub connection
            pubSubConnection = client.connectPubSub();
            pubSubConnection.addListener(listener);

            pubSub = pubSubConnection.sync();

            registerHandlers();

            //This is hardcoded for now. I need to figure out a way to change this
            pubSub.subscribe("velocity");

            startRedisUpdater();
        } catch (Throwable e) {
            plugin.getLogger().error("Error initializing RedisManager", e);
            plugin.getProxy().shutdown();
        }
    }

    private void registerHandlers() {
        handlers.put(RedisMessageType.REGISTER, this::registerServer);
        handlers.put(RedisMessageType.UNREGISTER, this::unregisterServer);
        handlers.put(RedisMessageType.MATCHMAKE, this::matchmake);
        handlers.put(RedisMessageType.LOBBY_REQUEST, this::handleLobbyRequest);
    }


    private void startRedisUpdater() {

        plugin.getProxy().getScheduler().buildTask(plugin, () -> {

            updateServerState(plugin.currentState);

        }).repeat(5, TimeUnit.SECONDS).schedule();
    }


    public void shutdown() {

        if (pubSubConnection != null)
            pubSubConnection.close();

        if (commandConnection != null)
            commandConnection.close();

        if (client != null)
            client.shutdown();
    }

    public RedisCommands<String, String> get() {
        return redis;
    }


    public void updateServerState(States gameState) {

        plugin.currentState = gameState;

        CompletableFuture.runAsync(() -> {
            try {


                Map<String, String> data = new HashMap<>();

                data.put("server_uuid", plugin.serverUUID);
                data.put("server_type", plugin.serverType.name());
                data.put("game_state", gameState.getName());
                data.put("player_count", String.valueOf(plugin.getProxy().getPlayerCount()));
                data.put("maximum_players", String.valueOf(plugin.getProxy().getConfiguration().getShowMaxPlayers()));
                data.put("server_address", plugin.getProxy().getBoundAddress().getHostName());
                data.put("server_port", String.valueOf(plugin.getProxy().getBoundAddress().getPort()));

                data.put("last_update",
                        String.valueOf(System.currentTimeMillis()));

                String key = "server:" + plugin.serverUUID;

                redis.hset(key, data);

                // ttl 15 seconds. If it doesn't update within that time, the server is dead and should be removed from the redis stack
                redis.expire(key, 15);

                // optional: index set for matchmaking
                redis.sadd("servers:all", plugin.serverUUID);

            } catch (Exception e) {
                plugin.getLogger().error("Error updating server state (Redis)", e);
                plugin.getProxy().shutdown();
            }
        }, plugin.dbManager.executor);
    }

    public void updatePlayerState(PlayerProfile profile) {
        CompletableFuture.runAsync(() -> {

            Map<String, String> data = new HashMap<>();

            data.put("uuid", profile.getUuid().toString());
            data.put("name", profile.getUsername());

            data.put("rank", profile.getNetworkProfile().getRank().name());
            data.put("chatChannel", profile.getNetworkProfile().getChatChannel().name());

            data.put("last_update",
                    String.valueOf(System.currentTimeMillis()));

            String key = "player:" + profile.getUuid();

            redis.hset(key, data);
        }, plugin.dbManager.executor);
    }


    public CompletableFuture<Void> clearData() {
        return CompletableFuture.runAsync(() -> {
            try {
                redis.del("server:" + plugin.serverUUID);
                redis.srem("servers:all",  plugin.serverUUID);

                for (Player player : plugin.getProxy().getAllPlayers()) {
                    PlayerProfile profile = plugin.playerManager.get(player.getUniqueId());
                    redis.del("player:" + profile.getUuid());
                }
            } catch (Exception e) {
                plugin.getLogger().error("Error updating server state (Redis)", e);
            }
        }, plugin.dbManager.executor);
    }

    public void removePlayerFromRedis(UUID uuid) {
        CompletableFuture.runAsync(() -> {
            try {
                redis.del("player:" + uuid);
            } catch (Exception e) {
                plugin.getLogger().error("Error updating server state (Redis)", e);
            }
        }, plugin.dbManager.executor);
    }


    public CompletableFuture<Void> publish(String channel, JsonObject json) {
        return CompletableFuture.runAsync(() ->
                        redis.publish(channel, json.toString()),
                plugin.dbManager.executor
        );
    }


    private final RedisPubSubListener<String, String> listener = new RedisPubSubAdapter<>() {

        @Override
        public void message(String channel, String message) {

            try {

                JsonObject json = JsonParser.parseString(message).getAsJsonObject();

                if (!json.has("type")) {
                    plugin.getLogger().warn("Redis message missing type field");
                    return;
                }

                String type = json.get("type").getAsString();

                RedisMessageType typeEnum = RedisMessageType.valueOf(type);

                Consumer<JsonObject> handler = handlers.get(typeEnum);

                if (handler != null) {
                    handler.accept(json);
                } else {
                    plugin.getLogger().warn("Unknown Redis message type: {}", type);
                }

            } catch (Exception e) {
                plugin.getLogger().error("Failed to process Redis message. \n{}\n", message, e);
            }
        }
    };

    // Handlers


    public void registerServer(JsonObject json) {
        plugin.serverManager.registerServer(json);
    }


    public void unregisterServer(JsonObject json) {

        String server_uuid = json.get("server_uuid").getAsString();

        Server server = plugin.serverManager.serversByUuid.get(UUID.fromString(server_uuid));
        try {
            plugin.getProxy().getServer(String.valueOf(server.getUuid())).ifPresent(registeredServer -> plugin.getProxy().unregisterServer(registeredServer.getServerInfo()));
            plugin.getLogger().info("Unregistered server {}", server_uuid);

        } catch (Exception e) {
            plugin.getLogger().error("Error unregistering server {}", server_uuid, e);
        }
    }



    private void matchmake(JsonObject jsonObject) {

        String playerName = jsonObject.get("player_name").getAsString();
        String gameName = jsonObject.get("game_name").getAsString();

        plugin.getLogger().info("Received matchmake request for player {} for game {}", playerName, gameName);


        plugin.matchmakingManager.findServerWithAvailableSlots(plugin.getProxy().getPlayer(playerName).orElse(null), gameName);
    }


    private void handleLobbyRequest(JsonObject jsonObject) {

        String playerName = jsonObject.get("player_name").getAsString();
        String playerUUID = jsonObject.get("player_uuid").getAsString();

        plugin.getProxy().getPlayer(playerName).ifPresent(player -> plugin.matchmakingManager.findLobby(player));
    }


    public void initServersInRedis() {

        Set<String> servers = redis.smembers("servers:all");

        plugin.getLogger().info("Found {} servers in Redis", servers.size());
        plugin.getLogger().info("Servers: {}", servers);

        for (String serverName : servers) {

            Map<String, String> data = redis.hgetall("server:" + serverName);

            plugin.getLogger().info(data.toString());

            if (data.isEmpty()) {
                redis.srem("servers:all", serverName);
                continue;
            }

            plugin.getLogger().info(data.toString());

            JsonObject json = new JsonObject();

            for (Map.Entry<String, String> entry : data.entrySet()) {
                json.addProperty(entry.getKey(), entry.getValue());
            }

            plugin.serverManager.registerServer(json);
        }

    }
}