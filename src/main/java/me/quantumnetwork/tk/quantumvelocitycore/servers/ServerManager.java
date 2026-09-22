package me.quantumnetwork.tk.quantumvelocitycore.servers;

import com.google.gson.JsonObject;
import com.velocitypowered.api.proxy.server.ServerInfo;
import me.quantumnetwork.tk.ServerType;
import me.quantumnetwork.tk.quantumvelocitycore.QuantumVelocityCore;

import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ServerManager {

    QuantumVelocityCore plugin;

    public ServerManager(QuantumVelocityCore plugin) {
        this.plugin = plugin;
    }

    public Map<UUID, Server> serversByUuid = new HashMap<>();

    public Map<String, Server> serversByName = new HashMap<>();




    public void registerServer(JsonObject json) {

        UUID server_uuid = UUID.fromString(json.get("server_uuid").getAsString());
        String server_address = json.get("server_address").getAsString();
        String server_port = json.get("server_port").getAsString();
        ServerType serverType = ServerType.valueOf(json.get("server_type").getAsString());
        int maxPlayers = json.get("max_players").getAsInt();

        InetSocketAddress address = new InetSocketAddress(server_address, Integer.parseInt(server_port));

        Server server = new Server(serverType, server_uuid, maxPlayers);
        ServerNameAllocation serverNameAllocation = plugin.utils.generateServerName(server.serverType);

        server.setName(serverNameAllocation.name());
        server.setServerNumber(serverNameAllocation.number());

        serversByUuid.put(server_uuid, server);
        serversByName.put(server.getName(), server);


        plugin.getProxy().registerServer(new ServerInfo(server.getName(), address));

        plugin.getLogger().info("Registered server with uuid {}, address {}, and name {}", server_uuid, address, server.getName());
        plugin.getLogger().info("Current registered servers: {}", plugin.getProxy().getAllServers());
    }


    public ServerType getServerType(String serverName) {
        return serversByName.get(serverName).serverType;
    }
}
