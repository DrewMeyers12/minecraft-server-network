package me.quantumnetwork.tk.quantumvelocitycore.servers;

import lombok.Getter;
import lombok.Setter;
import me.quantumnetwork.tk.ServerType;

import java.util.UUID;

public class Server {

    @Getter
    ServerType serverType;

    @Getter
    UUID uuid;

    @Getter
    @Setter
    String name;

    @Getter
    @Setter
    int serverNumber;

    @Getter
    @Setter
    int maxPlayers;


    public Server(ServerType serverType, UUID uuid, int maxPlayers) {
        this.serverType = serverType;
        this.uuid = uuid;
        this.maxPlayers = maxPlayers;
    }


}
