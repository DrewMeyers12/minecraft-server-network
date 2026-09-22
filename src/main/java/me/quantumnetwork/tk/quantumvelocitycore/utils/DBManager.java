package me.quantumnetwork.tk.quantumvelocitycore.utils;

import lombok.Getter;
import me.quantumnetwork.tk.quantumvelocitycore.QuantumVelocityCore;
import org.json.JSONObject;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.UUID;
import java.util.concurrent.*;

public class DBManager {

    QuantumVelocityCore instance;

    @Getter
    public Connection connection;

    @Getter
    public final Executor executor = Executors.newCachedThreadPool();


    public DBManager() {
        instance = QuantumVelocityCore.getInstance();

        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            connection = DriverManager.getConnection(url, user, password);
            System.out.println("Connected to the database");
            scheduleKeepAlive();
        } catch (ClassNotFoundException | SQLException e) {
            instance.getLogger().error("Error connecting to backend database", e);
            instance.getProxy().shutdown();
        }
    }


    public void scheduleKeepAlive() {
        instance.getProxy().getScheduler().buildTask(instance, () -> {
            try (Statement stmt = connection.createStatement()) {
                stmt.executeQuery("SELECT 1");
            } catch (SQLException e) {
                // handle reconnection
                instance.getLogger().error("SEVERE: Lost connection to the database", e);
            }
        }).repeat(5, TimeUnit.MINUTES).schedule();
    }


    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final ExecutorService httpExecuter = Executors.newCachedThreadPool();

    public CompletableFuture<UUID> fetchPlayerUUIDAsync(String playerName) {

        instance.getLogger().info("Fetching UUID for player " + playerName);
        //Call the api and return the uuid
        return CompletableFuture.supplyAsync(() -> {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://api.mojang.com/users/profiles/minecraft/" + playerName))
                    .GET()
                    .build();

            try {
                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

                instance.getLogger().info("Response status code: " + response.statusCode());
                if (response.statusCode() == 200) {
                    JSONObject jsonResponse = new JSONObject(response.body());
                    instance.getLogger().info("UUID: " + jsonResponse.getString("id"));
                    instance.getLogger().info(instance.utils.insertDashUUID(jsonResponse.getString("id")).toString());
                    return instance.utils.insertDashUUID(jsonResponse.getString("id"));
                } else if (response.statusCode() == 204 || response.body().contains("Couldn't find any profile with name")) {
                    return null;
                } else {
                    return null;
                }
            } catch (IOException | InterruptedException e) {
                instance.getLogger().error("Error fetching UUID from Mojang API: {}", e.getMessage());
                throw new CompletionException(e);
            }
        });


    }


}
