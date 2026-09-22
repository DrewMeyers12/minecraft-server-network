package me.quantumnetwork.tk.quantumvelocitycore.commands;

import com.mojang.brigadier.tree.LiteralCommandNode;
import com.velocitypowered.api.command.BrigadierCommand;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.proxy.Player;
import me.quantumnetwork.tk.ChatType;
import me.quantumnetwork.tk.quantumvelocitycore.QuantumVelocityCore;
import me.quantumnetwork.tk.quantumvelocitycore.players.PlayerProfile;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import static com.mojang.brigadier.Command.SINGLE_SUCCESS;

public final class ChatCommand {

     public static BrigadierCommand createCommand(QuantumVelocityCore instance) {

            LiteralCommandNode<CommandSource> node = BrigadierCommand
                    .literalArgumentBuilder("chat")

                    // /chat global | g | all
                    .then(BrigadierCommand.literalArgumentBuilder("global")
                            .executes(ctx -> setChannel(ctx.getSource(), instance, ChatType.GLOBAL)))
                    .then(BrigadierCommand.literalArgumentBuilder("g")
                            .executes(ctx -> setChannel(ctx.getSource(), instance, ChatType.GLOBAL)))
                    .then(BrigadierCommand.literalArgumentBuilder("all")
                            .executes(ctx -> setChannel(ctx.getSource(), instance, ChatType.GLOBAL)))

                    // /chat party | p
                    .then(BrigadierCommand.literalArgumentBuilder("party")
                            .executes(ctx -> setChannel(ctx.getSource(), instance, ChatType.PARTY)))
                    .then(BrigadierCommand.literalArgumentBuilder("p")
                            .executes(ctx -> setChannel(ctx.getSource(), instance, ChatType.PARTY)))

                    // fallback: /chat
                    .executes(ctx -> {
                        ctx.getSource().sendMessage(Component.text(
                                "Usage: /chat <global | party>",
                                NamedTextColor.RED
                        ));
                        return SINGLE_SUCCESS;
                    })

                    .build();

            return new BrigadierCommand(node);
        }

    private static int setChannel(CommandSource source, QuantumVelocityCore instance, ChatType channel) {
         if (requirePlayer(source) == null) return SINGLE_SUCCESS;

         Player player = requirePlayer(source);
         PlayerProfile profile = instance.playerManager.get(player.getUniqueId());
         profile.getNetworkProfile().setChatChannel(channel);
         player.sendMessage(Component.text(
                 "Chat type set to " + channel.name(),
                 NamedTextColor.GRAY
            ));
        instance.redisManager.updatePlayerState(profile);
        return SINGLE_SUCCESS;
    }

    private static Player requirePlayer(CommandSource source) {
        if (source instanceof Player ) {
            return (Player) source;
        } else {
            source.sendMessage(Component.text("This command can only be executed by a player.", NamedTextColor.RED));
            return null;
        }
    }
}
