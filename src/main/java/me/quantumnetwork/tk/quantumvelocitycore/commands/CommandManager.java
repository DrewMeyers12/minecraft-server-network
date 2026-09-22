package me.quantumnetwork.tk.quantumvelocitycore.commands;

import me.quantumnetwork.tk.quantumvelocitycore.QuantumVelocityCore;

public class CommandManager {

    public CommandManager(QuantumVelocityCore instance) {

        instance.getProxy().getCommandManager().register(
                instance.getProxy().getCommandManager().metaBuilder("rank")
                        .plugin(instance)
                        .build(),
                RankCommand.createCommand(instance));


        //Both Commands are party chats

        instance.getProxy().getCommandManager().register(
                instance.getProxy().getCommandManager().metaBuilder("party")
                .plugin(instance)
                        .aliases("p")
                .build(),
                 PartyCommand.createCommand(instance));

        instance.getProxy().getCommandManager().register(
                instance.getProxy().getCommandManager().metaBuilder("partychat")
                        .plugin(instance)
                        .aliases("pc")
                        .build(),
                PartyCommand.createPartyChatAlias(instance));

        //Both Commands are party chats

        instance.getProxy().getCommandManager().register(
                instance.getProxy().getCommandManager().metaBuilder("discord")
                        .plugin(instance)
                        .build(),
                DiscordCommand.createCommand());

        instance.getProxy().getCommandManager().register(
                instance.getProxy().getCommandManager().metaBuilder("message")
                        .plugin(instance)
                        .aliases("msg", "m", "tell")
                        .build(),
                MessageCommand.createCommand(instance));

        instance.getProxy().getCommandManager().register(
                instance.getProxy().getCommandManager().metaBuilder("lobby")
                        .plugin(instance)
                        .aliases("l", "hub")
                        .build(),
                LobbyCommand.createCommand(instance));

        instance.getProxy().getCommandManager().register(
                instance.getProxy().getCommandManager().metaBuilder("kick")
                        .plugin(instance)
                        .build(),
                KickCommand.createCommand(instance));

        instance.getProxy().getCommandManager().register(
                instance.getProxy().getCommandManager().metaBuilder("ban")
                        .plugin(instance)
                        .build(),
                new BanCommand().createCommand(instance));

        instance.getProxy().getCommandManager().register(
                instance.getProxy().getCommandManager().metaBuilder("pardon")
                        .plugin(instance)
                        .aliases("unban")
                        .build(),
                PardonCommand.createCommand(instance));

        instance.getProxy().getCommandManager().register(
                instance.getProxy().getCommandManager().metaBuilder("chat")
                        .plugin(instance)
                        .build(),
                ChatCommand.createCommand(instance));

        instance.getProxy().getCommandManager().register(
                instance.getProxy().getCommandManager().metaBuilder("report")
                        .plugin(instance)
                        .build(),
                ReportCommand.createCommand(instance));

        instance.getLogger().info("Registered Rank and party commands.");
    }

    //other commands to add, report and suggest, pardon, whitelist
}

