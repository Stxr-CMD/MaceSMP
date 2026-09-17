package com.macesmp.plugin.commands;

import com.macesmp.plugin.MaceSMP;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class CoreShopCommand implements CommandExecutor {

    private final MaceSMP plugin;

    public CoreShopCommand(MaceSMP plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Only players can open the core shop.");
            return true;
        }
        if (!player.hasPermission("macesmp.shop")) {
            plugin.getMessageManager().send(player, "no-permission");
            return true;
        }
        plugin.getShopManager().openMainMenu(player);
        return true;
    }
}
