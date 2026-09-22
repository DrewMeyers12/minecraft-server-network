package me.quantumnetwork.tk.quantumvelocitycore.players;

import lombok.Getter;
import me.quantumnetwork.tk.ChatType;
import me.quantumnetwork.tk.Rank;
import me.quantumnetwork.tk.quantumvelocitycore.party.Party;

@Getter
public class NetworkProfile {

    private Rank rank;
    private Party party;
    private ChatType chatChannel;

    public void setRank(Rank rank) {
        this.rank = rank;
    }

    public void setParty(Party party) {
        this.party = party;
    }

    public void setChatChannel(ChatType chatChannel) {
        this.chatChannel = chatChannel;
    }

}