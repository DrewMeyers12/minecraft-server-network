package me.quantumnetwork.tk.quantumvelocitycore.commands;

import com.mojang.brigadier.tree.LiteralCommandNode;
import com.velocitypowered.api.command.BrigadierCommand;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.proxy.Player;
import me.quantumnetwork.tk.quantumvelocitycore.QuantumVelocityCore;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import static com.mojang.brigadier.Command.SINGLE_SUCCESS;
import static com.mojang.brigadier.arguments.StringArgumentType.string;

public class MatchmakeCommand {

    public static BrigadierCommand createCommand(QuantumVelocityCore instance) {

        LiteralCommandNode<CommandSource> node = BrigadierCommand
                .literalArgumentBuilder("matchmake")
                .then(BrigadierCommand.requiredArgumentBuilder("game", string())
                        .executes(ctx -> {
                            Player player = requirePlayer(ctx.getSource());
                            if (player == null) return SINGLE_SUCCESS;

                            String game = ctx.getArgument("game", String.class);
                            instance.matchmakingManager.findServerWithAvailableSlots(player, game);

                            return SINGLE_SUCCESS;
                        })
                )
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


}
