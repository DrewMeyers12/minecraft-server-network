package me.quantumnetwork.tk.quantumvelocitycore.commands;

import com.mojang.brigadier.tree.LiteralCommandNode;
import com.velocitypowered.api.command.BrigadierCommand;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.proxy.Player;
import me.quantumnetwork.tk.quantumvelocitycore.QuantumVelocityCore;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import java.util.Optional;
import java.util.UUID;

import static com.mojang.brigadier.Command.SINGLE_SUCCESS;
import static com.mojang.brigadier.arguments.StringArgumentType.greedyString;
import static com.mojang.brigadier.arguments.StringArgumentType.string;

public final class PartyCommand {

    public static BrigadierCommand createCommand(QuantumVelocityCore instance) {

        LiteralCommandNode<CommandSource> node = BrigadierCommand
                .literalArgumentBuilder("party")

                // /party create
                .then(BrigadierCommand.literalArgumentBuilder("create")
                        .executes(ctx -> {
                            Player player = requirePlayer(ctx.getSource());
                            if (player == null) return SINGLE_SUCCESS;
                            instance.partyManager.createParty(player);
                            return SINGLE_SUCCESS;
                        })
                )

                // /party disband
                .then(BrigadierCommand.literalArgumentBuilder("disband")
                        .executes(ctx -> {
                            Player player = requirePlayer(ctx.getSource());
                            if (player == null) return SINGLE_SUCCESS;
                            instance.partyManager.disbandParty(player);
                            return SINGLE_SUCCESS;
                        })
                )

                // /party leave
                .then(BrigadierCommand.literalArgumentBuilder("leave")
                        .executes(ctx -> {
                            Player player = requirePlayer(ctx.getSource());
                            if (player == null) return SINGLE_SUCCESS;
                            instance.partyManager.leaveParty(player);
                            return SINGLE_SUCCESS;
                        })
                )

                // /party list
                .then(BrigadierCommand.literalArgumentBuilder("list")
                        .executes(ctx -> {
                            Player player = requirePlayer(ctx.getSource());
                            if (player == null) return SINGLE_SUCCESS;
                            instance.partyManager.listParty(player);
                            return SINGLE_SUCCESS;
                        })
                )

                // /party warp
                .then(BrigadierCommand.literalArgumentBuilder("warp")
                        .executes(ctx -> {
                            Player player = requirePlayer(ctx.getSource());
                            if (player == null) return SINGLE_SUCCESS;
                            instance.partyManager.warpParty(player);
                            return SINGLE_SUCCESS;
                        })
                )

                // /party invite
                .then(BrigadierCommand.literalArgumentBuilder("invite")
                        .then(BrigadierCommand.requiredArgumentBuilder("target", string())
                        .executes(ctx -> {
                            Player player = requirePlayer(ctx.getSource());
                            if (player == null) return SINGLE_SUCCESS;
                            String targetName = ctx.getArgument("target", String.class);

                            Optional<Player> optionalTarget = instance.getProxy().getPlayer(targetName);

                            if (optionalTarget.isEmpty()) {
                                player.sendMessage(Component.text(targetName + " is not online.",  NamedTextColor.RED));
                                return SINGLE_SUCCESS;
                            }

                            Player target = optionalTarget.get();

                            instance.partyManager.invitePlayer(player, target);
                            return SINGLE_SUCCESS;
                        }))
                )

                // /party kick
                .then(BrigadierCommand.literalArgumentBuilder("kick")
                        .then(BrigadierCommand.requiredArgumentBuilder("target", string())
                        .executes(ctx -> {
                            Player player = requirePlayer(ctx.getSource());
                            if (player == null) return SINGLE_SUCCESS;
                            String targetName = ctx.getArgument("target", String.class);

                            Optional<Player> optionalTarget = instance.getProxy().getPlayer(targetName);

                            if (optionalTarget.isEmpty()) {
                                player.sendMessage(Component.text(targetName + " is not online.",  NamedTextColor.RED));
                                return SINGLE_SUCCESS;
                            }

                            Player target = optionalTarget.get();

                            instance.partyManager.kickPlayer(player, target, targetName);
                            return SINGLE_SUCCESS;
                        }))
                )

                // /party join
                .then(BrigadierCommand.literalArgumentBuilder("join")
                        .then(BrigadierCommand.requiredArgumentBuilder("target", string())
                        .executes(ctx -> {
                            Player player = requirePlayer(ctx.getSource());
                            if (player == null) return SINGLE_SUCCESS;
                            String targetName = ctx.getArgument("target", String.class);

                            Optional<Player> optionalTarget = instance.getProxy().getPlayer(targetName);

                            if (optionalTarget.isEmpty()) {
                                optionalTarget = instance.getProxy().getPlayer(UUID.fromString(targetName));
                            }

                            if (optionalTarget.isEmpty()) {
                                player.sendMessage(Component.text(targetName + " is not online.",  NamedTextColor.RED));
                                return SINGLE_SUCCESS;
                            }

                            Player target = optionalTarget.get();

                            instance.partyManager.acceptInvite(player, target);
                            return SINGLE_SUCCESS;
                        }))
                )

                // /party promote
                .then(BrigadierCommand.literalArgumentBuilder("promote")
                        .then(BrigadierCommand.requiredArgumentBuilder("target", string())
                        .executes(ctx -> {
                            Player player = requirePlayer(ctx.getSource());
                            if (player == null) return SINGLE_SUCCESS;

                            String targetName = ctx.getArgument("target", String.class);

                            Optional<Player> optionalTarget = instance.getProxy().getPlayer(targetName);

                            if (optionalTarget.isEmpty()) {
                                player.sendMessage(Component.text(targetName + " is not online.",  NamedTextColor.RED));
                                return SINGLE_SUCCESS;
                            }

                            Player target = optionalTarget.get();

                            instance.partyManager.promotePlayer(player, target);
                            return SINGLE_SUCCESS;
                        }))
                )

                // /party message
                .then(BrigadierCommand.literalArgumentBuilder("chat")
                        .then(BrigadierCommand.requiredArgumentBuilder("message", greedyString())
                                .executes(ctx -> {
                                    Player player = requirePlayer(ctx.getSource());
                                    if (player == null) return SINGLE_SUCCESS;
                                    instance.partyManager.handlePartyChat(player, ctx.getArgument("message", String.class));
                                    return SINGLE_SUCCESS;
                                }))
                )

                // /party <player>
                .then(BrigadierCommand.requiredArgumentBuilder(
                        "target",
                        string()
                ).executes(ctx -> {
                    Player player = requirePlayer(ctx.getSource());
                    if (player == null) return SINGLE_SUCCESS;

                    String targetName = ctx.getArgument("target", String.class);

                    Optional<Player> optionalTarget = instance.getProxy().getPlayer(targetName);

                    if (optionalTarget.isEmpty()) {
                        player.sendMessage(Component.text(targetName + " is not online.",  NamedTextColor.RED));
                        return SINGLE_SUCCESS;
                    }

                    Player target = optionalTarget.get();
                    instance.partyManager.invitePlayer(player, target);
                    return SINGLE_SUCCESS;
                }))






                .build();

        return new BrigadierCommand(node);
    }

    //Literally just the alias for party chat /pc chat <message>
    public static BrigadierCommand createPartyChatAlias(QuantumVelocityCore instance) {

        LiteralCommandNode<CommandSource> node = BrigadierCommand
                .literalArgumentBuilder("partychat")
                        .then(BrigadierCommand.requiredArgumentBuilder("message", greedyString())
                                .executes(ctx -> {
                                    Player player = requirePlayer(ctx.getSource());
                                    if (player == null) return SINGLE_SUCCESS;
                                    instance.partyManager.handlePartyChat(player, ctx.getArgument("message", String.class));
                                    return SINGLE_SUCCESS;
                                }))
                .build();

        return new BrigadierCommand(node);
    }

    private static Player requirePlayer(CommandSource source) {
        if (source instanceof Player ) {
            return (Player) source;
        } else {
            source.sendMessage(Component.text("This command can only be executed by a player.", NamedTextColor.RED));
            return null;
        }
    }


    //Commands needed
    // create, disband, invite, join/accept, kick/remove, leave, promote, list, warp, chat/pc, help
}
