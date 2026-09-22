package me.quantumnetwork.tk.quantumvelocitycore.commands;

import com.mojang.brigadier.tree.LiteralCommandNode;
import com.velocitypowered.api.command.BrigadierCommand;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.proxy.Player;
import me.quantumnetwork.tk.Rank;
import me.quantumnetwork.tk.quantumvelocitycore.QuantumVelocityCore;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import java.util.concurrent.CompletableFuture;

import static com.mojang.brigadier.Command.SINGLE_SUCCESS;
import static com.mojang.brigadier.arguments.StringArgumentType.word;


public final class RankCommand {

    public static BrigadierCommand createCommand(QuantumVelocityCore instance) {
        LiteralCommandNode<com.velocitypowered.api.command.CommandSource> node = BrigadierCommand
                .literalArgumentBuilder("rank")
                .requires(source -> {
                    if (!(source instanceof Player player)) return true; // console bypass
                    Rank playerRank = instance.playerManager.get(player.getUniqueId()).getNetworkProfile().getRank(); // your method to get current rank
                    return playerRank.canRunCommand(Rank.ADMIN); // the rank required to run this command
                })
                .then(BrigadierCommand.requiredArgumentBuilder("player", word())
                        .then(BrigadierCommand.requiredArgumentBuilder("rank", word())
                                .suggests((ctx, builder) -> {
                                    // Tab completion for rank names
                                    for (Rank r : Rank.values()) {
                                        builder.suggest(r.toString());
                                    }
                                    return builder.buildFuture();
                                })
                                .executes(context -> {
                                    String targetName = context.getArgument("player", String.class);
                                    String rankName = context.getArgument("rank", String.class).toUpperCase();
                                    var source = context.getSource();

                                    // Validate rank
                                    boolean isValidRank = false;
                                    for (Rank r : Rank.values()) {
                                        if (r.toString().equals(rankName)) {
                                            isValidRank = true;
                                            break;
                                        }
                                    }

                                    if (!isValidRank) {
                                        source.sendMessage(Component.text("Invalid rank. Please use a valid rank.", NamedTextColor.RED));
                                        return SINGLE_SUCCESS;
                                    }


                                    updateRank(targetName, source, rankName);

                                    return SINGLE_SUCCESS;
                                })
                        )
                )
                .build();

        return new BrigadierCommand(node);
    }


    public static void updateRank(String name, CommandSource source, String rankName) {
        QuantumVelocityCore instance = QuantumVelocityCore.getInstance();

        instance.dbManager.fetchPlayerUUIDAsync(name)
                .thenCompose(uuid -> {
                    if (uuid == null) {
                        source.sendMessage(Component.text(
                                "Player " + name + " was not found.",
                                NamedTextColor.YELLOW));
                        return CompletableFuture.completedFuture(null);
                    }

                    return instance.playerManager.networkRepository
                            .updateRank(uuid, Rank.valueOf(rankName));
                })
                .thenAccept(result -> {

                    if (result == null) {
                        return; // UUID wasn't found
                    }

                    if (!result.success()) {

                        if (!result.playerExists()) {
                            source.sendMessage(Component.text(
                                    "Player " + name + " was not in the database.",
                                    NamedTextColor.YELLOW));
                            return;
                        }

                        source.sendMessage(Component.text(
                                "An error occurred while updating the rank.",
                                NamedTextColor.RED));
                        return;
                    }

                    source.sendMessage(Component.text(
                            "Player " + name + "'s rank has been updated to " + rankName,
                            NamedTextColor.GREEN));
                })
                .exceptionally(ex -> {
                    source.sendMessage(Component.text(
                            "An unexpected error occurred.",
                            NamedTextColor.RED));
                    ex.printStackTrace();
                    return null;
                });
    }

}