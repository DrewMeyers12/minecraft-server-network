package me.quantumnetwork.tk.quantumvelocitycore.players.utils;

public record Ban(
        boolean banned,
        boolean permanent,
        long timeRemaining,
        String reason
) {}
