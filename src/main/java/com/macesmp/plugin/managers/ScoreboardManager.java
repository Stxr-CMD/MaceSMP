package com.macesmp.plugin.managers;

import com.macesmp.plugin.MaceSMP;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Sidebar scoreboard showing each player's live Cores and kills.
 * If you'd rather use a separate scoreboard plugin, set scoreboard.enabled
 * to false and use the %macesmp_cores% / %macesmp_kills% placeholders instead.
 */
public class ScoreboardManager {

    private final MaceSMP plugin;
    private final Set<UUID> disabled = new HashSet<>();
    private BukkitTask task;

    public ScoreboardManager(MaceSMP plugin) {
        this.plugin = plugin;
    }

    public void startUpdating() {
        if (!plugin.getConfig().getBoolean("scoreboard.enabled", true)) {
            return;
        }
        long interval = plugin.getConfig().getLong("scoreboard.update-interval-ticks", 20L);
        task = plugin.getServer().getScheduler().runTaskTimer(plugin, () -> {
            for (Player player : plugin.getServer().getOnlinePlayers()) {
                if (!disabled.contains(player.getUniqueId())) {
                    updateFor(player);
                }
            }
        }, 0L, interval);
    }

    public void stopUpdating() {
        if (task != null) {
            task.cancel();
            task = null;
        }
    }

    public void reload() {
        stopUpdating();
        startUpdating();
    }

    public boolean toggle(Player player) {
        if (disabled.contains(player.getUniqueId())) {
            disabled.remove(player.getUniqueId());
            updateFor(player);
            return true; // now enabled
        } else {
            disabled.add(player.getUniqueId());
            clearFor(player);
            return false; // now disabled
        }
    }

    public void updateFor(Player player) {
        if (!plugin.getConfig().getBoolean("scoreboard.enabled", true)) return;
        if (disabled.contains(player.getUniqueId())) return;

        Scoreboard board = player.getScoreboard();
        // Give the player their own scoreboard instance if they're on the server's default/shared one
        if (board == plugin.getServer().getScoreboardManager().getMainScoreboard()) {
            board = plugin.getServer().getScoreboardManager().getNewScoreboard();
            player.setScoreboard(board);
        }

        Objective objective = board.getObjective("macesmp");
        String title = color(plugin.getConfig().getString("scoreboard.title", "&b&lMACESMP"));
        if (objective == null) {
            objective = board.registerNewObjective("macesmp", "dummy", title);
            objective.setDisplaySlot(DisplaySlot.SIDEBAR);
        } else if (!objective.getDisplayName().equals(title)) {
            objective.setDisplayName(title);
        }

        // Clear old entries before re-writing, since line content/order changes every tick
        for (String entry : new HashSet<>(board.getEntries())) {
            board.resetScores(entry);
        }

        List<String> lines = plugin.getConfig().getStringList("scoreboard.lines");
        int cores = plugin.getCoreManager().getBalance(player);
        int kills = plugin.getCoreManager().getKills(player.getUniqueId());

        int score = lines.size();
        Set<String> usedLines = new HashSet<>();
        for (String rawLine : lines) {
            String line = color(rawLine
                    .replace("{player}", player.getName())
                    .replace("{cores}", String.valueOf(cores))
                    .replace("{kills}", String.valueOf(kills))
                    .replace("{icon}", plugin.getMessageManager().icon()));

            // Scoreboards can't have two identical entries; pad blank/duplicate lines with invisible color codes
            while (line.length() > 40) {
                line = line.substring(0, 40);
            }
            while (usedLines.contains(line)) {
                line = line + ChatColor.RESET;
            }
            usedLines.add(line);

            objective.getScore(line).setScore(score);
            score--;
        }
    }

    public void clearFor(Player player) {
        Scoreboard main = plugin.getServer().getScoreboardManager().getMainScoreboard();
        player.setScoreboard(main);
    }

    private String color(String text) {
        return ChatColor.translateAlternateColorCodes('&', text == null ? "" : text);
    }
}
