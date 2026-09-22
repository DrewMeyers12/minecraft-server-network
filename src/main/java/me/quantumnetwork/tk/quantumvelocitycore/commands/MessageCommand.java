package me.quantumnetwork.tk.quantumvelocitycore.commands;

import com.mojang.brigadier.tree.LiteralCommandNode;
import com.velocitypowered.api.command.BrigadierCommand;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.proxy.Player;
import me.quantumnetwork.tk.quantumvelocitycore.QuantumVelocityCore;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import static com.mojang.brigadier.Command.SINGLE_SUCCESS;
import static com.mojang.brigadier.arguments.StringArgumentType.greedyString;
import static com.mojang.brigadier.arguments.StringArgumentType.string;

public final class MessageCommand {

    public static BrigadierCommand createCommand(QuantumVelocityCore instance) {

        LiteralCommandNode<CommandSource> node = BrigadierCommand
                .literalArgumentBuilder("message")
                .then(BrigadierCommand.requiredArgumentBuilder("player", string())
                        .then(BrigadierCommand.requiredArgumentBuilder("message", greedyString())
                                .executes(ctx -> {

                                    CommandSource source = ctx.getSource();
                                    String targetName = ctx.getArgument("player", String.class);
                                    String message = ctx.getArgument("message", String.class);

                                    instance.getProxy().getPlayer(targetName).ifPresentOrElse(target -> {

                                        if (source instanceof Player sender) {
                                            // Player → Player
                                            instance.utils.sendMessageToPlayer(sender, target.getUsername(), message);
                                        } else {
                                            // Console → Player
                                            instance.utils.sendConsoleMessageToPlayer(target.getUsername(), message);
                                        }

                                    }, () -> source.sendMessage(Component.text(
                                            "Player is not online!",
                                            NamedTextColor.RED
                                    )));

                                    return SINGLE_SUCCESS;
                                })
                        )
                ).build();

        return new BrigadierCommand(node);
    }
}
