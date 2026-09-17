package com.macesmp.plugin.listeners;

import com.macesmp.plugin.MaceSMP;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Rewards cores on PvP kills. Same-IP kills never pay out (blocks obvious
 * alt farming), and killing the same person over and over is capped: after
 * a few rewarded kills on one victim, no more cores until a cooldown passes.
 */
public class PlayerKillListener implements Listener {

    private final MaceSMP plugin;

    // key = "killerUUID|victimUUID"
    private final Map<String, Long> windowStart = new HashMap<>();
    private final Map<String, Integer> windowCount = new HashMap<>();

    public PlayerKillListener(MaceSMP plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player victim = event.getEntity();
        Player killer = victim.getKiller();

        if (killer == null || killer.equals(victim)) {
            return;
        }

        if (isSameIp(killer, victim)) {
            return; // no reward, no kill counted - obvious alt farming
        }

        if (!allowedByKillLimit(killer, victim)) {
            return; // hit the same-victim limit, waiting on cooldown
        }

        int reward = plugin.getConfig().getInt("cores.kill-reward", 10);
        plugin.getCoreManager().addBalance(killer.getUniqueId(), reward, "KILL", "Killed " + victim.getName());
        plugin.getCoreManager().addKill(killer.getUniqueId());
        announceReward(killer, victim, reward);

        if (plugin.getConfig().getBoolean("cores.victim-loses-cores", false)) {
            int loss = plugin.getConfig().getInt("cores.victim-loss-amount", 10);
            int actuallyLost = Math.min(loss, plugin.getCoreManager().getBalance(victim.getUniqueId()));
            plugin.getCoreManager().removeBalance(victim.getUniqueId(), actuallyLost, "DIED", "Killed by " + killer.getName());
            victim.sendMessage(plugin.getMessageManager().get("victim-loss", Map.of(
                    "amount", String.valueOf(actuallyLost),
                    "killer", killer.getName()
            )));
        }

        plugin.getScoreboardManager().updateFor(killer);
        plugin.getScoreboardManager().updateFor(victim);
    }

    private boolean isSameIp(Player killer, Player victim) {
        if (!plugin.getConfig().getBoolean("cores.anti-grind.block-same-ip", true)) {
            return false;
        }
        if (killer.getAddress() == null || victim.getAddress() == null) {
            return false;
        }
        return killer.getAddress().getAddress().getHostAddress()
                .equals(victim.getAddress().getAddress().getHostAddress());
    }

    /**
     * Lets a killer be rewarded for killing the same victim up to
     * max-kills-per-victim-window times, then blocks further rewards until
     * same-victim-window-minutes has passed since the window started.
     */
    private boolean allowedByKillLimit(Player killer, Player victim) {
        int maxKills = plugin.getConfig().getInt("cores.anti-grind.max-kills-per-victim-window", 5);
        long windowMinutes = plugin.getConfig().getLong("cores.anti-grind.same-victim-window-minutes", 5);
        if (maxKills <= 0 || windowMinutes <= 0) {
            return true; // feature disabled
        }

        String key = killer.getUniqueId() + "|" + victim.getUniqueId();
        long now = System.currentTimeMillis();
        long windowMillis = TimeUnit.MINUTES.toMillis(windowMinutes);

        Long start = windowStart.get(key);
        if (start == null || (now - start) > windowMillis) {
            // no active window, or the old one expired - start a fresh one
            windowStart.put(key, now);
            windowCount.put(key, 1);
            return true;
        }

        int count = windowCount.getOrDefault(key, 0);
        if (count >= maxKills) {
            return false;
        }
        windowCount.put(key, count + 1);
        return true;
    }

    private void announceReward(Player killer, Player victim, int reward) {
        boolean useActionBar = plugin.getConfig().getBoolean("cores.use-actionbar-for-kill-reward", false);
        Map<String, String> placeholders = Map.of("amount", String.valueOf(reward), "victim", victim.getName());
        if (useActionBar) {
            plugin.getMessageManager().sendActionBar(killer, "kill-reward-actionbar", placeholders);
        } else {
            killer.sendMessage(plugin.getMessageManager().get("kill-reward", placeholders));
        }
    }
}
