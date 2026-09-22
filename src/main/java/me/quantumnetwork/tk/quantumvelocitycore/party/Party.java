package me.quantumnetwork.tk.quantumvelocitycore.party;

import lombok.Getter;
import lombok.Setter;

import java.util.*;

@Getter
public class Party {
    @Setter
    private UUID leader; // Store player UUID or name
    private final Set<UUID> members;

    private final Set<UUID> invites;

    // Constructor, getters, setters, and methods for party management
    public Party(UUID leader) {
        this.leader = leader;
        this.members = new HashSet<>();
        this.members.add(leader);
        this.invites = new HashSet<>();
    }

    public void addMember(UUID member) {
        members.add(member);
    }

    public void removeMember(UUID member) {
        members.remove(member);
    }

    public boolean isLeader(UUID player) {
        return player.equals(leader);
    }

    public boolean isMember(UUID player) {
        return members.contains(player);
    }

    public void addInvite(UUID player) {
        invites.add(player);
    }
    public void removeInvite(UUID player) {
        invites.remove(player);
    }


    public void disband() {
        members.clear();
        invites.clear();
    }



}
