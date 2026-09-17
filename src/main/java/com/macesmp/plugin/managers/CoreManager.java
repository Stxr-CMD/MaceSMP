package com.macesmp.plugin.managers;

import com.macesmp.plugin.MaceSMP;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.io.File;
import java.io.IOException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

/**
 * Player Cores balances, kill counts, and a transaction history for each player.
 * Everything's cached in memory and written to cores.yml.
 */
public class CoreManager {

    public record HistoryEntry(long timestamp, String type, int amount, int balanceAfter, String note) {
        String encode() {
            return timestamp + "~" + type + "~" + amount + "~" + balanceAfter + "~" + note;
        }

        static HistoryEntry decode(String line) {
            String[] parts = line.split("~", 5);
            return new HistoryEntry(Long.parseLong(parts[0]), parts[1],
                    Integer.parseInt(parts[2]), Integer.parseInt(parts[3]), parts[4]);
        }
    }

    private static final int MAX_HISTORY_PER_PLAYER = 50;

    private final MaceSMP plugin;
    private final File file;
    private final YamlConfiguration data;

    private final Map<UUID, Integer> balances = new ConcurrentHashMap<>();
    private final Map<UUID, Integer> kills = new ConcurrentHashMap<>();
    private final Map<UUID, Deque<HistoryEntry>> history = new ConcurrentHashMap<>();

    private volatile boolean dirty = false;
    private BukkitTask autoSaveTask;

    public CoreManager(MaceSMP plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "cores.yml");
        if (!file.exists()) {
            plugin.getDataFolder().mkdirs();
            try {
                file.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().log(Level.SEVERE, "Could not create cores.yml", e);
            }
        }
        this.data = YamlConfiguration.loadConfiguration(file);
        loadAll();
    }

    private void loadAll() {
        if (data.isConfigurationSection("balances")) {
            for (String key : data.getConfigurationSection("balances").getKeys(false)) {
                try {
                    balances.put(UUID.fromString(key), data.getInt("balances." + key));
                } catch (IllegalArgumentException ignored) {
                }
            }
        }
        if (data.isConfigurationSection("kills")) {
            for (String key : data.getConfigurationSection("kills").getKeys(false)) {
                try {
                    kills.put(UUID.fromString(key), data.getInt("kills." + key));
                } catch (IllegalArgumentException ignored) {
                }
            }
        }
        if (data.isConfigurationSection("history")) {
            for (String key : data.getConfigurationSection("history").getKeys(false)) {
                try {
                    UUID uuid = UUID.fromString(key);
                    Deque<HistoryEntry> deque = new ArrayDeque<>();
                    for (String line : data.getStringList("history." + key)) {
                        try {
                            deque.addLast(HistoryEntry.decode(line));
                        } catch (Exception ignored) {
                        }
                    }
                    history.put(uuid, deque);
                } catch (IllegalArgumentException ignored) {
                }
            }
        }
    }

    /** Writes everything to disk. Called on shutdown and periodically while dirty. */
    public synchronized void saveAll() {
        for (Map.Entry<UUID, Integer> entry : balances.entrySet()) {
            data.set("balances." + entry.getKey(), entry.getValue());
        }
        for (Map.Entry<UUID, Integer> entry : kills.entrySet()) {
            data.set("kills." + entry.getKey(), entry.getValue());
        }
        for (Map.Entry<UUID, Deque<HistoryEntry>> entry : history.entrySet()) {
            List<String> encoded = new ArrayList<>();
            for (HistoryEntry h : entry.getValue()) {
                encoded.add(h.encode());
            }
            data.set("history." + entry.getKey(), encoded);
        }
        try {
            data.save(file);
            dirty = false;
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Could not save cores.yml", e);
        }
    }

    private void markDirty() {
        dirty = true;
    }

    /** Saves every 5 seconds if something changed, instead of once per single transaction. */
    public void startAutoSave() {
        autoSaveTask = Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, () -> {
            if (dirty) saveAll();
        }, 100L, 100L);
    }

    public void stopAutoSave() {
        if (autoSaveTask != null) {
            autoSaveTask.cancel();
            autoSaveTask = null;
        }
    }

    public int getBalance(UUID uuid) {
        return balances.getOrDefault(uuid, plugin.getConfig().getInt("cores.starting-balance", 0));
    }

    public int getBalance(Player player) {
        return getBalance(player.getUniqueId());
    }

    // Balance-mutating methods are synchronized on this instance so two things
    // touching a balance at the same tick can't race each other into a lost
    // update or a spend approved against a stale value.

    public synchronized void setBalance(UUID uuid, int amount) {
        balances.put(uuid, Math.max(amount, 0));
        markDirty();
    }

    public void addBalance(UUID uuid, int amount) {
        addBalance(uuid, amount, "ADJUST", "");
    }

    public synchronized void addBalance(UUID uuid, int amount, String type, String note) {
        if (amount <= 0) return;
        balances.merge(uuid, amount, Integer::sum);
        markDirty();
        logHistory(uuid, type, amount, note);
    }

    public boolean removeBalance(UUID uuid, int amount) {
        return removeBalance(uuid, amount, "ADJUST", "");
    }

    /** @return true if the player had enough and the withdrawal went through */
    public synchronized boolean removeBalance(UUID uuid, int amount, String type, String note) {
        int current = getBalance(uuid);
        if (current < amount) return false;
        setBalance(uuid, current - amount);
        logHistory(uuid, type, -amount, note);
        return true;
    }

    public boolean hasBalance(UUID uuid, int amount) {
        return getBalance(uuid) >= amount;
    }

    public int getKills(UUID uuid) {
        return kills.getOrDefault(uuid, 0);
    }

    public void addKill(UUID uuid) {
        kills.put(uuid, getKills(uuid) + 1);
        markDirty();
    }

    private void logHistory(UUID uuid, String type, int amount, String note) {
        Deque<HistoryEntry> deque = history.computeIfAbsent(uuid, k -> new ArrayDeque<>());
        deque.addFirst(new HistoryEntry(System.currentTimeMillis(), type, amount, getBalance(uuid), note));
        while (deque.size() > MAX_HISTORY_PER_PLAYER) {
            deque.removeLast();
        }
        markDirty();
    }

    /** Newest first. */
    public List<HistoryEntry> getHistory(UUID uuid) {
        return new ArrayList<>(history.getOrDefault(uuid, new ArrayDeque<>()));
    }

    public Map<UUID, Integer> getAllBalances() {
        return new HashMap<>(balances);
    }
}
