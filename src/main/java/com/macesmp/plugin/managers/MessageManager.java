package com.macesmp.plugin.managers;

import com.macesmp.plugin.MaceSMP;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Map;

/**
 * Loads message strings from config.yml and handles color codes + placeholders.
 */
public class MessageManager {

    private final MaceSMP plugin;

    public MessageManager(MaceSMP plugin) {
        this.plugin = plugin;
    }

    private String raw(String key) {
        String msg = plugin.getConfig().getString("messages." + key, "");
        return msg == null ? "" : msg;
    }

    public String color(String text) {
        return ChatColor.translateAlternateColorCodes('&', text);
    }

    /** The Cores icon (e.g. "♦"), color-translated, read fresh from config each time so /cores reload picks it up. */
    public String icon() {
        return color(plugin.getConfig().getString("cores.icon", "&e♦"));
    }

    public String get(String key) {
        String prefix = raw("prefix");
        return color(prefix + raw(key)).replace("{icon}", icon());
    }

    public String get(String key, Map<String, String> placeholders) {
        String message = get(key);
        for (Map.Entry<String, String> entry : placeholders.entrySet()) {
            message = message.replace("{" + entry.getKey() + "}", entry.getValue());
        }
        return message;
    }

    public void send(CommandSender sender, String key) {
        sender.sendMessage(get(key));
    }

    public void send(CommandSender sender, String key, Map<String, String> placeholders) {
        sender.sendMessage(get(key, placeholders));
    }

    /**
     * Sends a message above the hotbar (action bar) instead of in chat.
     * Used for things like the kill-reward "+10 Cores" popup.
     * Deliberately skips the chat prefix since it would look cluttered there.
     */
    public void sendActionBar(Player player, String key, Map<String, String> placeholders) {
        String message = color(raw(key)).replace("{icon}", icon());
        for (Map.Entry<String, String> entry : placeholders.entrySet()) {
            message = message.replace("{" + entry.getKey() + "}", entry.getValue());
        }
        player.sendActionBar(LegacyComponentSerializer.legacySection().deserialize(message));
    }
}
