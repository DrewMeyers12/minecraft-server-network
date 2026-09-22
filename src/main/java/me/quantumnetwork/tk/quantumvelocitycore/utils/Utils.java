package me.quantumnetwork.tk.quantumvelocitycore.utils;

import com.velocitypowered.api.proxy.Player;
import me.quantumnetwork.tk.ServerType;
import me.quantumnetwork.tk.quantumvelocitycore.QuantumVelocityCore;
import me.quantumnetwork.tk.quantumvelocitycore.servers.Server;
import me.quantumnetwork.tk.quantumvelocitycore.servers.ServerNameAllocation;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class Utils {

    QuantumVelocityCore instance = QuantumVelocityCore.getInstance();


    public void sendMessageToPlayer(Player player, String target, String message) {

        Player target1;
        if (instance.getProxy().getPlayer(target).isPresent()) {
            target1 = instance.getProxy().getPlayer(target).get();

            target1.sendMessage(Component.text("From ", NamedTextColor.LIGHT_PURPLE)
                    .append(instance.playerManager.get(player.getUniqueId()).getNetworkProfile().getRank().getPrefix())
                    .append(Component.text( player.getUsername(), instance.playerManager.get(player.getUniqueId()).getNetworkProfile().getRank().getColor()).append(Component.text(": " + message, NamedTextColor.GRAY))));

            player.sendMessage(Component.text("To ", NamedTextColor.LIGHT_PURPLE)
                    .append(instance.playerManager.get(target1.getUniqueId()).getNetworkProfile().getRank().getPrefix())
                    .append(Component.text( target1.getUsername(), instance.playerManager.get(target1.getUniqueId()).getNetworkProfile().getRank().getColor())
                            .append(Component.text(": " + message, NamedTextColor.GRAY))));

        } else {
            player.sendMessage(Component.text("Player is offline!", NamedTextColor.RED));
        }
    }

    public void sendConsoleMessageToPlayer(String target, String message) {

        Player target1;
        if (instance.getProxy().getPlayer(target).isPresent()) {
            target1 = instance.getProxy().getPlayer(target).get();
            target1.sendMessage(Component.text("From ", NamedTextColor.LIGHT_PURPLE).append(Component.text("CONSOLE", NamedTextColor.RED)).append(Component.text(": " + message, NamedTextColor.GRAY)));
        }
    }


    public ServerNameAllocation generateServerName(ServerType type) {

        Set<Integer> used = new HashSet<>();

        for (Server server : instance.serverManager.serversByUuid.values()) {

            if (server.getServerType() != type)
                continue;

            used.add(server.getServerNumber());

        }

        int number = 1;

        while (used.contains(number))
            number++;

        String name = type + "-" + number;

        return new ServerNameAllocation(number, name);
    }

    public String formatTime(long milliseconds) {
        long seconds = milliseconds / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;
        long days = hours / 24;

        StringBuilder formattedTime = new StringBuilder();
        if (days > 0) {
            formattedTime.append(days).append("d ");
        }
        if (hours % 24 > 0) {
            formattedTime.append(hours % 24).append("h ");
        }
        if (minutes % 60 > 0) {
            formattedTime.append(minutes % 60).append("m ");
        }
        if (seconds % 60 > 0) {
            formattedTime.append(seconds % 60).append("s");
        }
        return formattedTime.toString().trim();
    }

    public UUID insertDashUUID(String uuid) {
        StringBuilder sb = new StringBuilder(uuid);
        sb.insert(8, "-");
        sb = new StringBuilder(sb.toString());
        sb.insert(13, "-");
        sb = new StringBuilder(sb.toString());
        sb.insert(18, "-");
        sb = new StringBuilder(sb.toString());
        sb.insert(23, "-");

        return UUID.fromString(String.valueOf(sb));
    }

}
