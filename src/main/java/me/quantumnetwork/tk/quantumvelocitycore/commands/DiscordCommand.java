package me.quantumnetwork.tk.quantumvelocitycore.commands;

import com.mojang.brigadier.tree.LiteralCommandNode;
import com.velocitypowered.api.command.BrigadierCommand;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.proxy.Player;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.NamedTextColor;

import static com.mojang.brigadier.Command.SINGLE_SUCCESS;

public final class DiscordCommand {

    public static BrigadierCommand createCommand() {

        LiteralCommandNode<CommandSource> node = BrigadierCommand
                .literalArgumentBuilder("discord")
                        .executes(ctx -> {
                            Player player = requirePlayer(ctx.getSource());
                            if (player == null) return SINGLE_SUCCESS;

                            Component discordLink = Component.text("Join our discord at ", NamedTextColor.GREEN)
                                    .append(Component.text("https://discord.gg/XF4G7tS3vq", NamedTextColor.LIGHT_PURPLE))
                                    .hoverEvent(Component.text("Click to join our discord", NamedTextColor.WHITE))
                                    .clickEvent(ClickEvent.openUrl("https://discord.gg/XF4G7tS3vq"));


                            player.sendMessage(discordLink);
                            return SINGLE_SUCCESS;
                        })
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
