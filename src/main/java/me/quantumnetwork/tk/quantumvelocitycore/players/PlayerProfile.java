package me.quantumnetwork.tk.quantumvelocitycore.players;

import lombok.Getter;
import net.kyori.adventure.text.Component;

import java.awt.*;
import java.util.UUID;

@Getter
public class PlayerProfile {

    private final UUID uuid;
    private final String username;

    private final NetworkProfile networkProfile = new NetworkProfile();

    public PlayerProfile(UUID uuid, String username) {
        this.uuid = uuid;
        this.username = username;
    }

    public Component getDisplayName() {
        return networkProfile.getRank()
                .getPrefix()
                .append(Component.text(username, networkProfile.getRank().getColor()));
    }

}

