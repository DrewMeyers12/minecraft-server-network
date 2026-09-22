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

import java.util.concurrent.CompletableFuture;

import static com.mojang.brigadier.arguments.StringArgumentType.string;

public class PardonCommand {

    private static QuantumVelocityCore instance;

    public static BrigadierCommand createCommand(QuantumVelocityCore plugin) {
        instance = plugin;

        LiteralCommandNode<CommandSource> node =
                BrigadierCommand.literalArgumentBuilder("pardon")
                        .requires(source -> {
                            if (!(source instanceof Player player)) return true; // console bypass
                            Rank playerRank = instance.playerManager.get(player.getUniqueId()).getNetworkProfile().getRank(); // your method to get current rank
                            return playerRank.canRunCommand(Rank.ADMIN); // the rank required to run this command
                        })

                        // /pardon <player>
                        .then(BrigadierCommand.requiredArgumentBuilder("player", string())
                                .executes(PardonCommand::execute)
                        )
                        .build();

        return new BrigadierCommand(node);
    }

    private static int execute(CommandContext<CommandSource> ctx) {
        CommandSource source = ctx.getSource();
        String playerName = StringArgumentType.getString(ctx, "player");

        instance.playerManager.banRepository.pardon(playerName)
                .thenAccept(success -> {
                    if (success) {
                        source.sendMessage(Component.text(
                                playerName + " has been pardoned.",
                                NamedTextColor.GREEN
                        ));
                    } else {
                        source.sendMessage(Component.text(
                                "Failed to pardon " + playerName + ". Check spelling or try again.",
                                NamedTextColor.RED
                        ));
                    }
                })
                .exceptionally(ex -> {
                    instance.getLogger().error("Pardon command failed", ex);
                    source.sendMessage(Component.text(
                            "An error occurred while executing the command.",
                            NamedTextColor.RED
                    ));
                    return null;
                });

        return 1;
    }



}
