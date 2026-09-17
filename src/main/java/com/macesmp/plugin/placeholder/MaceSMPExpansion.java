package com.macesmp.plugin.placeholder;

import com.macesmp.plugin.MaceSMP;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;

/**
 * Registers %macesmp_cores% and %macesmp_kills% so any external scoreboard,
 * tab list, or holograms plugin reads the live, correct value straight from
 * MaceSMP's own data - no separate/duplicate tracking, no desync.
 */
public class MaceSMPExpansion extends PlaceholderExpansion {

    private final MaceSMP plugin;

    public MaceSMPExpansion(MaceSMP plugin) {
        this.plugin = plugin;
    }

    @Override
    public @NotNull String getIdentifier() {
        return "macesmp";
    }

    @Override
    public @NotNull String getAuthor() {
        return "MaceSMP";
    }

    @Override
    public @NotNull String getVersion() {
        return plugin.getDescription().getVersion();
    }

    @Override
    public boolean persist() {
        return true; // keep the expansion registered across /papi reload
    }

    @Override
    public String onRequest(OfflinePlayer player, @NotNull String params) {
        if (player == null) {
            return "";
        }
        return switch (params.toLowerCase()) {
            case "cores" -> String.valueOf(plugin.getCoreManager().getBalance(player.getUniqueId()));
            case "kills" -> String.valueOf(plugin.getCoreManager().getKills(player.getUniqueId()));
            default -> null;
        };
    }
}
