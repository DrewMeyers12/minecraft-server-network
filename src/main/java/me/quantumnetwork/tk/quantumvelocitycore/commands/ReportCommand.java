package me.quantumnetwork.tk.quantumvelocitycore.commands;

import com.mojang.brigadier.tree.LiteralCommandNode;
import com.velocitypowered.api.command.BrigadierCommand;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.proxy.Player;
import me.quantumnetwork.tk.quantumvelocitycore.QuantumVelocityCore;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static com.mojang.brigadier.Command.SINGLE_SUCCESS;
import static com.mojang.brigadier.arguments.StringArgumentType.greedyString;
import static com.mojang.brigadier.arguments.StringArgumentType.word;

public class ReportCommand {


    private static final String WEBHOOK_URL =
            "https://discord.com/api/webhooks/1450960601512673414/Gf3g5lPzHAmYPsWXvD9g8MkIKLeEfqotXsr_WeYWgHfEneLR10RFLSavYOVObRMO8_mp";

    private static final int RED = 15158332;
    private static final int ORANGE = 15105570;
    private static final int GREEN = 3066993;

    private static final String PLAYER_REPORT_TAG = "1250173869260542054";
    private static final String BUG_REPORT_TAG = "1250173896330711051";


    //Rate Limit
    private static final long REPORT_COOLDOWN_MS = 2 * 60 * 1000; // 2 minutes
    private static final Map<UUID, Long> lastReportTime = new HashMap<>();


    public static boolean canReport(UUID playerUUID) {
        long currentTime = System.currentTimeMillis();
        if (lastReportTime.containsKey(playerUUID)) {
            long lastTime = lastReportTime.get(playerUUID);
            if (currentTime - lastTime < REPORT_COOLDOWN_MS) {
                return false; // Still in cooldown
            }
        }
        lastReportTime.put(playerUUID, currentTime);
        return true; // Can report
    }


    public static BrigadierCommand createCommand(QuantumVelocityCore in) {

        LiteralCommandNode<CommandSource> node = BrigadierCommand
                .literalArgumentBuilder("report")
                .requires(source -> source instanceof Player)
                // /report player <player> <reason>
                .then(
                        BrigadierCommand.literalArgumentBuilder("player")
                                .then(
                                        BrigadierCommand.requiredArgumentBuilder("target", word())
                                                .then(
                                                        BrigadierCommand.requiredArgumentBuilder("reason", greedyString())
                                                                .executes(context -> {


                                                                    Player reporter = (Player) context.getSource();
                                                                    String target = context.getArgument("target", String.class);
                                                                    String reason = context.getArgument("reason", String.class);

                                                                    if (!canReport(reporter.getUniqueId())) {
                                                                        reporter.sendMessage(Component.text("You can only submit a report every 2 minutes. Please wait before reporting again.", NamedTextColor.RED));
                                                                        return SINGLE_SUCCESS;
                                                                    }

                                                                    sendEmbed(
                                                                            "Player Report - " + target,
                                                                            "Player Report",
                                                                            RED,
                                                                            new String[][]{
                                                                                    {"Reporter", reporter.getUsername()},
                                                                                    {"Target", target},
                                                                                    {"Reason", reason}
                                                                            },
                                                                            new String[]{PLAYER_REPORT_TAG}
                                                                    );
                                                                    reporter.sendMessage(Component.text("Thank you for reporting this player! It has been submitted to the discord server for review.", NamedTextColor.GREEN));

                                                                    return SINGLE_SUCCESS;
                                                                })
                                                )
                                )
                )

                // /report bug <description>
                .then(
                        BrigadierCommand.literalArgumentBuilder("bug")
                                .then(
                                        BrigadierCommand.requiredArgumentBuilder("description", greedyString())
                                                .executes(context -> {
                                                    Player reporter = (Player) context.getSource();
                                                    String desc = context.getArgument("description", String.class);

                                                    if (!canReport(reporter.getUniqueId())) {
                                                        reporter.sendMessage(Component.text("You can only submit a report every 2 minutes. Please wait before reporting again.", NamedTextColor.RED));
                                                        return SINGLE_SUCCESS;
                                                    }
                                                    sendEmbed(
                                                            "Bug Report - " + reporter.getUsername(),
                                                            "Bug Report",
                                                            ORANGE,
                                                            new String[][]{
                                                                    {"Reporter", reporter.getUsername()},
                                                                    {"Description", desc},

                                                            },
                                                        new String[]{BUG_REPORT_TAG}
                                                    );
                                                    reporter.sendMessage(Component.text("Thank you for reporting the bug! It has been submitted to the discord server for review.", NamedTextColor.GREEN));
                                                    return SINGLE_SUCCESS;
                                                })
                                )
                )
                .build();

        return new BrigadierCommand(node);
    }

    // ===================== DISCORD WEBHOOK =====================

    private static void sendEmbed(
            String threadName,
            String title,
            int color,
            String[][] fields,
            String[] tagIds
    ) {
        HttpURLConnection conn = null;

        try {
            URL url = new URL(WEBHOOK_URL);
            conn = (HttpURLConnection) url.openConnection();

            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setRequestProperty("User-Agent", "QuantumVelocityCore");
            conn.setDoOutput(true);

            StringBuilder json = new StringBuilder();
            json.append("{");
            json.append("\"thread_name\":\"").append(escape(threadName)).append("\",");

            json.append("\"applied_tags\":[");
            for (int i = 0; i < tagIds.length; i++) {
                json.append("\"").append(tagIds[i]).append("\"");
                if (i < tagIds.length - 1) json.append(",");
            }
            json.append("],");

            json.append("\"embeds\":[{");
            json.append("\"title\":\"").append(escape(title)).append("\",");
            json.append("\"color\":").append(color).append(",");
            json.append("\"fields\":[");

            for (int i = 0; i < fields.length; i++) {
                json.append("{")
                        .append("\"name\":\"").append(escape(fields[i][0])).append("\",")
                        .append("\"value\":\"").append(escape(fields[i][1])).append("\",")
                        .append("\"inline\":false")
                        .append("}");
                if (i < fields.length - 1) json.append(",");
            }

            json.append("]}]}");

            try (OutputStream os = conn.getOutputStream()) {
                os.write(json.toString().getBytes(StandardCharsets.UTF_8));
            }

            if (conn.getResponseCode() >= 400 && conn.getErrorStream() != null) {
                String err = new String(conn.getErrorStream().readAllBytes(), StandardCharsets.UTF_8);
                System.err.println("[DiscordWebhook] " + err);
            }

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (conn != null) conn.disconnect();
        }
    }

    private static String escape(String text) {
        return text
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "");
    }
}
