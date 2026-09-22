package me.quantumnetwork.tk.quantumvelocitycore.commands;

import com.mojang.brigadier.tree.LiteralCommandNode;
import com.velocitypowered.api.command.BrigadierCommand;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.proxy.Player;
import me.quantumnetwork.tk.Rank;
import me.quantumnetwork.tk.quantumvelocitycore.QuantumVelocityCore;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import static com.mojang.brigadier.Command.SINGLE_SUCCESS;
import static com.mojang.brigadier.arguments.StringArgumentType.greedyString;
import static com.mojang.brigadier.arguments.StringArgumentType.word;

public final class KickCommand {

    public static BrigadierCommand createCommand(QuantumVelocityCore in) {

        LiteralCommandNode<CommandSource> node = BrigadierCommand
                .literalArgumentBuilder("kick")
                .requires(source -> {
                    if (!(source instanceof Player player)) return true; // console bypass
                    Rank rank = in.playerManager.get(player.getUniqueId()).getNetworkProfile().getRank();
                    return rank.canRunCommand(Rank.ADMIN);
                })
                .then(
                        BrigadierCommand.requiredArgumentBuilder("player", word())
                                // /kick <player>
                                .executes(context -> {
                                    String target = context.getArgument("player", String.class);

                                    in.getProxy().getPlayer(target).ifPresentOrElse(targetP -> targetP.disconnect(Component.text(
                                            "You have been kicked from the server.",
                                            NamedTextColor.RED
                                    )), () -> context.getSource().sendMessage(Component.text(
                                            "Player is not online!",
                                            NamedTextColor.RED
                                    )));
                                    return SINGLE_SUCCESS;
                                })
                                // /kick <player> <reason>
                                .then(
                                        BrigadierCommand.requiredArgumentBuilder("reason", greedyString())
                                                .executes(context -> {
                                                    String target = context.getArgument("player", String.class);
                                                    String reason = context.getArgument("reason", String.class);

                                                    in.getProxy().getPlayer(target).ifPresentOrElse(targetP -> targetP.disconnect(Component.text(
                                                            reason, NamedTextColor.RED
                                                    )), () -> context.getSource().sendMessage(Component.text(
                                                            "Player is not online!",
                                                            NamedTextColor.RED
                                                    )));

                                                    return SINGLE_SUCCESS;
                                                })
                                )
                )
                .build();

        return new BrigadierCommand(node);
    }
}
