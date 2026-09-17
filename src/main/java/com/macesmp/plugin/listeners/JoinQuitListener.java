package com.macesmp.plugin.listeners;

import com.macesmp.plugin.MaceSMP;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

public class JoinQuitListener implements Listener {

    private final MaceSMP plugin;

    public JoinQuitListener(MaceSMP plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        // Give the new player their scoreboard immediately instead of waiting for the next tick cycle
        plugin.getScoreboardManager().updateFor(event.getPlayer());
    }
}
