package me.quantumnetwork.tk.quantumvelocitycore.players.utils;

public record RankUpdateResult(
        boolean success,
        boolean playerExists,
        String error
){}