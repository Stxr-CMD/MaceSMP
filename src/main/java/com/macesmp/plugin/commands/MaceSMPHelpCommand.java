package com.macesmp.plugin.commands;

import com.macesmp.plugin.MaceSMP;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

/**
 * /macesmp (alias /macecore) - always shows the full command list.
 * This exists specifically so typing the command alone gives players/staff
 * a menu instead of nothing.
 */
public class MaceSMPHelpCommand implements CommandExecutor {

    private final MaceSMP plugin;

    public MaceSMPHelpCommand(MaceSMP plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        String c = ChatColor.translateAlternateColorCodes('&', "&8&m                                        ");
        sender.sendMessage(c);
        sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "&b&lMaceSMP &7- v" + plugin.getDescription().getVersion()));
        sender.sendMessage("");
        line(sender, "/cores", "Check your Cores balance");
        if (sender.hasPermission("macesmp.cores.others") || sender.hasPermission("macesmp.admin")) {
            line(sender, "/cores balance <player>", "Check someone else's balance");
        }
        if (sender.hasPermission("macesmp.command.give") || sender.hasPermission("macesmp.admin")) {
            line(sender, "/cores give <player> <amount>", "Give Cores");
        }
        if (sender.hasPermission("macesmp.command.set") || sender.hasPermission("macesmp.admin")) {
            line(sender, "/cores set <player> <amount>", "Set a balance exactly");
        }
        if (sender.hasPermission("macesmp.command.reload") || sender.hasPermission("macesmp.admin")) {
            line(sender, "/cores reload", "Reload config.yml + shop.yml");
        }
        if (sender.hasPermission("macesmp.command.history") || sender.hasPermission("macesmp.admin")) {
            line(sender, "/cores history <player>", "View a player's Cores transaction log");
        }
        line(sender, "/coreshop", "Open the Core Shop");
        line(sender, "/scoreboard", "Toggle the built-in scoreboard");
        sender.sendMessage(c);
        return true;
    }

    private void line(CommandSender sender, String cmd, String description) {
        sender.sendMessage(ChatColor.translateAlternateColorCodes('&',
                "&e" + cmd + " &8- &7" + description));
    }
}
