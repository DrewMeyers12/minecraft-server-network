package me.quantumnetwork.tk.quantumvelocitycore.party;

import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.ServerConnection;
import me.quantumnetwork.tk.ChatType;
import me.quantumnetwork.tk.quantumvelocitycore.QuantumVelocityCore;
import me.quantumnetwork.tk.quantumvelocitycore.players.PlayerManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;

import java.util.Iterator;
import java.util.Optional;
import java.util.UUID;

public class PartyManager {

    public PlayerManager playerManager;

    private final ProxyServer proxy;

    private final QuantumVelocityCore instance = QuantumVelocityCore.getInstance();

    public PartyManager(PlayerManager playerManager ) {
//        this.playerParties = new HashMap<>();
        this.proxy = instance.getProxy();
        this.playerManager = playerManager;
    }


    public Party getParty(Player player) {
        return playerManager.get(player.getUniqueId()).getNetworkProfile().getParty();

//        if (proxy.getPlayer(player).isPresent()) {
//            return playerManager.getOrCreate(proxy.getPlayer(player).get()).getParty();
////            return playerParties.get(proxy.getPlayer(player).get().getUsername());
//        } else {
//            return playerManager.get(proxy.getPlayer(player)).getParty();
//            return playerParties.get(proxy.getPlayer(player).get().getUsername());
//        }
    }

    public void createParty(Player leader) {

        if (leader == null) {
            instance.getLogger().error("ERROR 1" + "\n" + "Player attempting to create a party was null!");
            return;
        }

        if (getParty(leader) != null) {
            leader.sendMessage(Component.text("You are already in a party", NamedTextColor.RED));
            return;
        }


        Party party = new Party(leader.getUniqueId());
        playerManager.get(leader.getUniqueId()).getNetworkProfile().setParty(party);
        leader.sendMessage(Component.text("------------------------------------------------" + "\n", NamedTextColor.BLUE)
                .append(Component.text("You have created a party!" + "\n", NamedTextColor.YELLOW ))
                .append(Component.text("------------------------------------------------", NamedTextColor.BLUE)));
    }

    public void handlePartyChat(Player player, String message) {
        if (playerManager.get(player.getUniqueId()).getNetworkProfile().getParty() == null) {
            if (playerManager.get(player.getUniqueId()).getNetworkProfile().getChatChannel().equals(ChatType.PARTY)) {
                proxy.getPlayer(player.getUsername()).ifPresent(p -> p.sendMessage(Component.text("You are not in a party!" + "\n" + "Your chat channel has automatically been set to global!", NamedTextColor.RED)));
                playerManager.get(player.getUniqueId()).getNetworkProfile().setChatChannel(ChatType.GLOBAL);
            } else {
                proxy.getPlayer(player.getUsername()).ifPresent(p -> p.sendMessage(Component.text("You are not in a party!", NamedTextColor.RED)));

            }
            return;
        }

        Party party = playerManager.get(player.getUniqueId()).getNetworkProfile().getParty();


        TextComponent finalMessage = Component.text("Party > ", TextColor.color(255,182,193))
                .append(playerManager.get(player.getUniqueId()).getNetworkProfile().getRank().getPrefix())
                .append(Component.text( player.getUsername(), playerManager.get(player.getUniqueId()).getNetworkProfile().getRank().getColor())
                        .append(Component.text( ": " + message, NamedTextColor.WHITE)));




        for (UUID member : party.getMembers()) {
            proxy.getPlayer(member).ifPresent(p -> p.sendMessage(finalMessage));
        }

    }


    /**
     * This method is only used when a player leaves the server (via the disconnect event) to ensure that the player name and related information is accessible.
     * @param player Object of the player who left the party
     */
    public void leaveParty(Player player) {
    instance.getLogger().info("Player Name Unchecked: " + player);
    if (player == null) {
        instance.getLogger().error("ERROR" + "\n" + "Player was 'null' while trying to leave server!");
        return;
    }

    Party party = playerManager.get(player.getUniqueId()).getNetworkProfile().getParty();
    if (party == null) {
        player.sendMessage(Component.text("You are not in a party", NamedTextColor.RED));
        return;
    }

        instance.getLogger().info("Party Members: {}", party.getMembers());

    // Remove the player from the party
    party.removeMember(player.getUniqueId());
//    playerParties.remove(player1.getUsername());
    playerManager.get(player.getUniqueId()).getNetworkProfile().setParty(null);

    // Check if the party should be disbanded
    if (party.getMembers().size() <= 1) {
        // Notify all remaining members (if any) and disband the party
        for (UUID member : party.getMembers()) {
            proxy.getPlayer(member).ifPresent(p -> {
                p.sendMessage(
                        Component.text("------------------------------------------------\n", NamedTextColor.BLUE)
                                .append(Component.text("The party has been disbanded as everyone has left!\n", NamedTextColor.YELLOW))
                                .append(Component.text("------------------------------------------------", NamedTextColor.BLUE))
                );

                playerManager.get(p.getUniqueId())
                        .getNetworkProfile()
                        .setParty(null);
            });
        }
        party.disband();
    } else {
        // Notify remaining members that the player has left
        for (UUID member : party.getMembers()) {
            if (!member.equals(player.getUniqueId())) {
                proxy.getPlayer(member).ifPresent(p -> p.sendMessage(
                        Component.text("------------------------------------------------\n", NamedTextColor.BLUE)
                                .append(playerManager.get(player.getUniqueId()).getDisplayName())
                                .append(Component.text(" has left the party\n", NamedTextColor.YELLOW))
                                .append(Component.text("------------------------------------------------", NamedTextColor.BLUE))
                ));
            }
        }

        // Assign a new leader if necessary
        if (party.isLeader(player.getUniqueId())) {
            findNewLeader(party);
        }
    }

    player.sendMessage(Component.text("You have left the party!", NamedTextColor.YELLOW));
}



    private void findNewLeader(Party party) {
        if (party.getMembers().size() <= 1) {
            // If there's only one member in the party or none, disband the party
            disbandParty(party);
            return;
        }

        // Find the new leader from the existing members
        UUID newLeader = party.getMembers().stream()
                .filter(member -> !member.equals(party.getLeader()))
                .findFirst()
                .orElse(null);

        if (newLeader != null) {
            party.setLeader(newLeader);
            for (UUID member : party.getMembers()) {
                proxy.getPlayer(member).ifPresent(p -> p.sendMessage(Component.text("------------------------------------------------" + "\n", NamedTextColor.BLUE)
                        .append(Component.text("The party leader has left the party ", NamedTextColor.YELLOW))
                                .append(playerManager.get(newLeader).getNetworkProfile().getRank().getPrefix())
                        .append(Component.text( " is the new party leader" + "\n", NamedTextColor.YELLOW))
                        .append(Component.text("------------------------------------------------", NamedTextColor.BLUE))));

            }
        } else {
            // If no new leader is found, disband the party
            disbandParty(party);
        }
    }

    public void disbandParty(Player player) {
        if (player == null) {
            instance.getLogger().error("ERROR" + "\n" + "Player attempting to disband party was null");
            return;
        }

        Party party = playerManager.get(player.getUniqueId()).getNetworkProfile().getParty();
        if (party == null) {
            player.sendMessage(Component.text("You are not in a party", NamedTextColor.RED));
            return;
        }
        if (!party.isLeader(player.getUniqueId())) {
            player.sendMessage(Component.text("You are not the party leader", NamedTextColor.RED));
            return;
        }

        for (UUID member : party.getMembers()) {
            proxy.getPlayer(member).ifPresent(player1 -> player1.sendMessage(Component.text("The party has been disbanded!", NamedTextColor.YELLOW)));
//            playerParties.remove(member);
            proxy.getPlayer(member).ifPresent(player1 -> playerManager.get(player1.getUniqueId()).getNetworkProfile().setParty(null));
        }
        party.disband();
    }

    public void disbandParty(Party party) {
        if (party == null) {
            instance.getLogger().error("ERROR" + "\n" + "Player attempting to disband party was null");
            return;
        }

        for (UUID member : party.getMembers()) {
            proxy.getPlayer(member).ifPresent(player1 -> player1.sendMessage(Component.text("The party has been disbanded!", NamedTextColor.YELLOW)));
//            playerParties.remove(member);
            proxy.getPlayer(member).ifPresent(player1 -> playerManager.get(player1.getUniqueId()).getNetworkProfile().setParty(null));
        }
        party.disband();
    }



    public void invitePlayer(Player inviter, Player invitee) {

        instance.getLogger().info("Inviter: {} Invitee: {}", inviter, invitee);



        if (inviter == null) {
            instance.getLogger().info("ERROR 4");
            return;
        }

//        Party party = playerParties.get(inviter1.getUsername());
        Party party = playerManager.get(inviter.getUniqueId()).getNetworkProfile().getParty();

        if (party != null && !party.isLeader(inviter.getUniqueId())) {
            inviter.sendMessage(Component.text("You are not the party leader!", NamedTextColor.RED));
            return;
        }

        if (inviter.getUniqueId().equals(invitee.getUniqueId())) {
            inviter.sendMessage(Component.text("You cannot invite yourself to a party!", NamedTextColor.RED));
            return;
        }



        if (getParty(inviter) == null) {
            createParty(inviter);
            party = playerManager.get(inviter.getUniqueId()).getNetworkProfile().getParty();
        }


        if (party.isMember(invitee.getUniqueId())) {
            inviter.sendMessage(Component.text(invitee.getUsername() + " is already in the party", NamedTextColor.RED));
            return;
        }
        if (party.getInvites().contains(invitee.getUniqueId())) {
            inviter.sendMessage(Component.text(invitee.getUsername() + " has already been invited", NamedTextColor.YELLOW));
            return;
        }



        for (UUID member : party.getMembers()) {
            proxy.getPlayer(member).ifPresent(player -> player.sendMessage(Component.text("------------------------------------------------" + "\n", NamedTextColor.BLUE)
                    .append(playerManager.get(inviter.getUniqueId()).getDisplayName())
                    .append(Component.text(" invited ", NamedTextColor.YELLOW))
                    .append(playerManager.get(invitee.getUniqueId()).getDisplayName())
                    .append(Component.text(" to the party! " + "\n", NamedTextColor.YELLOW))
                    .append(Component.text("------------------------------------------------", NamedTextColor.BLUE))));
        }
        party.addInvite(invitee.getUniqueId());

        // Make the invitee receive a clickable message that runs a /party join PlayerName command

            Component message = Component.text()
                    .append(Component.text("------------------------------------------------" + "\n", NamedTextColor.BLUE))
                    .append(playerManager.get(inviter.getUniqueId()).getDisplayName())
                    .append(Component.text(" has invited you to join their party! ", NamedTextColor.YELLOW))
                    .append(Component.text("Click here to join!" + "\n", NamedTextColor.GOLD)).clickEvent(ClickEvent.runCommand("/party join " + inviter.getUniqueId())).hoverEvent(Component.text("Click to join " + inviter.getUsername() + "'s party", NamedTextColor.WHITE))
                    .append(Component.text("------------------------------------------------", NamedTextColor.BLUE))
                    .build();

            invitee.sendMessage(message);


    }


    public void acceptInvite(Player invitee, Player inviter) {

        if (inviter == null) {
            instance.getLogger().info("ERROR 5");
            return;
        }

        if (invitee == null) {
            instance.getLogger().info("ERROR 6");
            return;
        }

        instance.getLogger().info("Inviter: {} Invitee: {}", inviter, invitee);

        // Check if the invitee is already in a party
        if (playerManager.get(invitee.getUniqueId()).getNetworkProfile().getParty() != null) {
            invitee.sendMessage(Component.text("You are already in a party", NamedTextColor.RED));
            return;
        }

        Party party = playerManager.get(inviter.getUniqueId()).getNetworkProfile().getParty();

        // Check if the inviter is in a party and if the invitee has been invited
        if (party == null || !party.getInvites().contains(invitee.getUniqueId())) {
                    invitee.sendMessage(Component.text("You have not been invited to any parties", NamedTextColor.RED));
            return;
        }

        // Add invitee to the party
        party.addMember(invitee.getUniqueId());
        party.removeInvite(invitee.getUniqueId());
        playerManager.get(invitee.getUniqueId()).getNetworkProfile().setParty(party);

        // Notify the invitee
        invitee.sendMessage(Component.text("------------------------------------------------\n", NamedTextColor.BLUE)
                .append(Component.text("You have joined ", NamedTextColor.YELLOW)
                        .append(playerManager.get(inviter.getUniqueId()).getDisplayName())
                        .append(Component.text("'s party!\n"))
                        .append(Component.text("------------------------------------------------", NamedTextColor.BLUE))));

        // Notify all other party members
        for (UUID member : party.getMembers()) {
            if (!member.equals(invitee.getUniqueId())) {
                proxy.getPlayer(member).ifPresent(player ->
                        player.sendMessage(Component.text("------------------------------------------------\n", NamedTextColor.BLUE)
                                .append(playerManager.get(invitee.getUniqueId()).getDisplayName())
                                .append(Component.text(" has joined the party\n", NamedTextColor.YELLOW))
                                .append(Component.text("------------------------------------------------", NamedTextColor.BLUE))
                        )
                );
            }
        }
    }


    public void listParty(Player player) {

        if (player == null) {
            instance.getLogger().info("ERROR 7" + "\n" + "Player attempting to list the party was null!");
            return;

        }

        Party party = playerManager.get(player.getUniqueId()).getNetworkProfile().getParty();
        if (party == null) {
            player.sendMessage(Component.text("You are not in a party", NamedTextColor.RED));
            return;
        }

                TextComponent message = Component.text("------------------------------------------------" + "\n", NamedTextColor.BLUE)
                        .append(Component.text("Party Members (" + party.getMembers().size() + "): " + "\n" + "\n", NamedTextColor.GOLD))
                        .append(Component.text("Party Leader: ", NamedTextColor.YELLOW))
                        .append(playerManager.get(party.getLeader()).getDisplayName())
                        .append(Component.newline())
                        .append(Component.text("Party Members: " , NamedTextColor.YELLOW));

        Iterator<UUID> iterator = party.getMembers().iterator();

        while (iterator.hasNext()) {
            UUID member = iterator.next();

            message = message.append(playerManager.get(member).getDisplayName());

            if (iterator.hasNext()) {
                message = message.append(Component.text(", ", NamedTextColor.YELLOW));
            }
        }

        message = message.append(Component.newline());

        message = message.append(Component.newline());
        message = message.append(Component.text("------------------------------------------------" , NamedTextColor.BLUE));
        TextComponent finalMessage = message;
        player.sendMessage(finalMessage);
    }



    public void warpParty(Player leader) {

        if (leader == null) {
            instance.getLogger().info("ERROR 8" +  "\n" + "Player attempting to warp party was null!");
            return;
        }

        if (playerManager.get(leader.getUniqueId()).getNetworkProfile().getParty() == null) {
            leader.sendMessage(Component.text("You are not in a party", NamedTextColor.RED));
            return;
        }

        if (!playerManager.get(leader.getUniqueId()).getNetworkProfile().getParty().isLeader(leader.getUniqueId())) {
            leader.sendMessage(Component.text("You are not the party leader", NamedTextColor.RED));
            return;
        }
        
        Party party = playerManager.get(leader.getUniqueId()).getNetworkProfile().getParty();

        //Add the isPresent() check
        Optional<ServerConnection> server = leader.getCurrentServer();


        int maxPlayers;

        server.ifPresent(s -> maxPlayers = instance.matchmakingManager.getMaxPlayers(s.getServer()));


        if (((party.getMembers().size() - 1) + server.get().getServer().getPlayersConnected().size()) > maxPlayers) {
            leader.sendMessage(Component.text("The party is too large to warp", NamedTextColor.RED));
            return;
        }


        leader.sendMessage(Component.text("Attempting to warp party members!", NamedTextColor.GRAY));
        for (UUID member : party.getMembers()) {
            if (!member.equals(leader.getUniqueId())) {


                //This method is bad. Should check for party size I think
                if (proxy.getPlayer(member).isPresent()) {
                    if (!proxy.getPlayer(member).get().getCurrentServer().get().getServerInfo().getName().equals(server.get().getServerInfo().getName())) {
                        proxy.getPlayer(member).get().createConnectionRequest(server.get().getServer()).fireAndForget();
                    }
                }
            }
        }


    }


    //Needs a ton of testing.
    public void promotePlayer(Player player, Player target) {

        if (player == null) {
            instance.getLogger().info("ERROR 9" +  "\n" + "Player attempting to promote party was null");
            return;
        }

        if (playerManager.get(player.getUniqueId()).getNetworkProfile().getParty() == null) {
            player.sendMessage(Component.text("You are not in a party", NamedTextColor.RED));
            return;
        }


        Party party = playerManager.get(player.getUniqueId()).getNetworkProfile().getParty();

        if (!party.isLeader(player.getUniqueId())) {
            player.sendMessage(Component.text("You are not the party leader", NamedTextColor.RED));
            return;
        }

        if (target == null) {
            player.sendMessage(Component.text(target + " is not online", NamedTextColor.RED));
            return;
        }

        if (target.getUniqueId().equals(player.getUniqueId())) {
            player.sendMessage(Component.text("You are already the party leader", NamedTextColor.RED));
            return;
        }

        if (!party.isMember(target.getUniqueId())) {
            player.sendMessage(Component.text(target.getUsername() + " is not in the party", NamedTextColor.RED));
            return;
        }

        party.setLeader(target.getUniqueId());

        for (UUID member : party.getMembers()) {
            if (!member.equals(target.getUniqueId())) {
                proxy.getPlayer(member).ifPresent(player1 -> player1.sendMessage(Component.text("------------------------------------------------" + "\n", NamedTextColor.BLUE)
                        .append(playerManager.get(player.getUniqueId()).getDisplayName())                        .append(Component.text(" has promoted ", NamedTextColor.YELLOW))
                        .append(playerManager.get(target.getUniqueId()).getDisplayName())
                        .append(Component.text(" to party leader! " + "\n", NamedTextColor.YELLOW))
                        .append(Component.text("------------------------------------------------", NamedTextColor.BLUE))));
            } else {
                proxy.getPlayer(member).ifPresent(player1 -> player1.sendMessage(Component.text("------------------------------------------------" + "\n", NamedTextColor.BLUE)
                        .append(Component.text("You have been promoted to party leader! " + "\n", NamedTextColor.YELLOW))
                        .append(Component.text("------------------------------------------------", NamedTextColor.BLUE))));
            }
        }

    }


    public void kickPlayer(Player player, Player target, String allegedName) {

        if (player == null) {
            instance.getLogger().info("ERROR 9"  +  "\n" + "Player attempting to kick party was null");
            return;
        }

        if (playerManager.get(player.getUniqueId()).getNetworkProfile().getParty() == null) {
            player.sendMessage(Component.text("You are not in a party", NamedTextColor.RED));
            return;
        }


//        Party party = playerParties.get(commandExecuter1.getUsername());
        Party party = playerManager.get(player.getUniqueId()).getNetworkProfile().getParty();

        if (!party.isLeader(player.getUniqueId())) {
            player.sendMessage(Component.text("You are not the party leader", NamedTextColor.RED));
            return;
        }


        if (target == null) {
            player.sendMessage(Component.text( allegedName + " is not online", NamedTextColor.RED));
            return;
        }

        if (target.getUniqueId().equals(player.getUniqueId())) {
            player.sendMessage(Component.text("You cannot kick yourself!", NamedTextColor.RED));
            return;
        }

        if (!party.isMember(target.getUniqueId())) {
            player.sendMessage(Component.text(target.getUsername() + " is not in the party", NamedTextColor.RED));
            return;
        }

        //Actual leave party code
        party.removeMember(target.getUniqueId());
//        playerParties.remove(target1.getUsername());
        playerManager.get(target.getUniqueId()).getNetworkProfile().setParty(null);

        //This is unchecked and probably doesn't work well or at all. It needs to be looked at later
        if (party.getMembers().size() == 1) {
            for (UUID member : party.getMembers()) {
                proxy.getPlayer(member).ifPresent(p -> p.sendMessage(Component.text("------------------------------------------------" + "\n", NamedTextColor.BLUE)
                        .append(Component.text("The party has been disbanded as everyone has left!" + "\n", NamedTextColor.YELLOW ))
                        .append(Component.text("------------------------------------------------", NamedTextColor.BLUE))));
//                playerParties.remove(member);
                playerManager.get(proxy.getPlayer(member).get().getUniqueId()).getNetworkProfile().setParty(null);
            }
            target.sendMessage(Component.text("You have been kicked from the party!", NamedTextColor.YELLOW));
            party.disband();
            return;
        }


        for (UUID member : party.getMembers()) {
            if (!member.equals(target.getUniqueId())) {
                proxy.getPlayer(member).ifPresent(p -> p.sendMessage(Component.text("------------------------------------------------" + "\n", NamedTextColor.BLUE)
                        .append(playerManager.get(target.getUniqueId()).getDisplayName())
                        .append(Component.text( " has been removed from the party!" + "\n", NamedTextColor.YELLOW))
                        .append(Component.text("------------------------------------------------", NamedTextColor.BLUE))));
            }
        }

    }
}

