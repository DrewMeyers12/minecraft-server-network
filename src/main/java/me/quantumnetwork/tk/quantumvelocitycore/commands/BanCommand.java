package me.quantumnetwork.tk.quantumvelocitycore.commands;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.tree.LiteralCommandNode;
import com.velocitypowered.api.command.BrigadierCommand;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.proxy.Player;
import me.quantumnetwork.tk.Rank;
import me.quantumnetwork.tk.quantumvelocitycore.QuantumVelocityCore;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static com.mojang.brigadier.arguments.StringArgumentType.string;

public class BanCommand {

    private QuantumVelocityCore instance;

    public BrigadierCommand createCommand(QuantumVelocityCore in) {
        instance = in;

        LiteralCommandNode<CommandSource> node =
                BrigadierCommand.literalArgumentBuilder("ban")
                        .requires(source -> {
                            if (!(source instanceof Player player)) return true; // console bypass
                            Rank playerRank = instance.playerManager.get(player.getUniqueId()).getNetworkProfile().getRank(); // your method to get current rank
                            return playerRank.canRunCommand(Rank.ADMIN); // the rank required to run this command
                        })

                        // /ban <player> <duration> [reason...]
                        .then(BrigadierCommand.requiredArgumentBuilder("player", string())
                                .then(BrigadierCommand.requiredArgumentBuilder("duration", string())
                                        .executes(ctx -> execute(ctx, null))
                                        .then(BrigadierCommand.requiredArgumentBuilder(
                                                "reason", StringArgumentType.greedyString()
                                        ).executes(ctx -> execute(
                                                ctx,
                                                StringArgumentType.getString(ctx, "reason")
                                        )))
                                )
                        )
                        .build();

        return new BrigadierCommand(node);
    }


    private int execute(CommandContext<CommandSource> ctx, String reasonOverride) {
        CommandSource source = ctx.getSource();

        String bannerName = (source instanceof Player player)
                ? player.getUsername()
                : "CONSOLE";

        String playerName = StringArgumentType.getString(ctx, "player");
        String duration = StringArgumentType.getString(ctx, "duration");
        String reason = (reasonOverride == null)
                ? "No reason provided"
                : reasonOverride;

        CompletableFuture<UUID> uuidFuture;

        if (!isValidBanDuration(duration)) {
            source.sendMessage(Component.text("Invalid duration: " + duration + ". Please use format p, perm, or 3d, 10h, 30m", NamedTextColor.RED));
            return 1;
        }

        LocalDateTime unbanDate = calculateUnbanDate(LocalDateTime.now(), duration);

        Optional<Player> onlinePlayer = instance.getProxy().getPlayer(playerName);

        if (onlinePlayer.isPresent()) {
            uuidFuture = CompletableFuture.completedFuture(onlinePlayer.get().getUniqueId());
        } else {
            uuidFuture = instance.dbManager.fetchPlayerUUIDAsync(playerName);
        }

        uuidFuture.thenCompose(uuid -> {

            if (uuid == null) {
                source.sendMessage(Component.text(
                        "Player not found.",
                        NamedTextColor.RED));
                return CompletableFuture.completedFuture(false);
            }

            return instance.playerManager.banRepository.banPlayer(
                    uuid,
                    playerName,
                    reason,
                    bannerName,
                    unbanDate
            );

        }).thenAccept(success -> {

            if (!success) {
                source.sendMessage(Component.text(
                        "Failed to ban player.",
                        NamedTextColor.RED));
                return;
            }

            // Disconnect if they're online
            instance.getProxy().getScheduler().buildTask(instance, () ->
                    instance.getProxy().getPlayer(playerName).ifPresent(player ->
                            player.disconnect(Component.text(
                                    "You have been banned: " + reason,
                                    NamedTextColor.RED
                            ))
                    )
            ).schedule();

            source.sendMessage(Component.text(
                    "Successfully banned " + playerName,
                    NamedTextColor.GREEN
            ));
        });

        return 1;
    }

    private boolean isValidBanDuration(String duration) {

        if (duration == null) {
            return false;
        }

        if (duration.equalsIgnoreCase("p")
                || duration.equalsIgnoreCase("perm")
                || duration.equalsIgnoreCase("permanent")) {
            return true;
        }

        return duration.matches("\\d+[dhm]");
    }

    private LocalDateTime calculateUnbanDate(LocalDateTime now, String duration) {
        if (duration.equalsIgnoreCase("perm")
                || duration.equalsIgnoreCase("permanent")
                || duration.equalsIgnoreCase("p")) {
            return null;
        }

        char unit = duration.charAt(duration.length() - 1);
        int value = Integer.parseInt(duration.substring(0, duration.length() - 1));

        return switch (unit) {
            case 'd' -> now.plusDays(value);
            case 'h' -> now.plusHours(value);
            case 'm' -> now.plusMinutes(value);
            default -> throw new IllegalArgumentException();
        };
    }
}
