package com.macesmp.plugin.commands;

import com.macesmp.plugin.MaceSMP;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class ScoreboardCommand implements CommandExecutor {

    private final MaceSMP plugin;

    public ScoreboardCommand(MaceSMP plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Only players can toggle the scoreboard.");
            return true;
        }
        boolean nowEnabled = plugin.getScoreboardManager().toggle(player);
        plugin.getMessageManager().send(player, nowEnabled ? "scoreboard-on" : "scoreboard-off");
        return true;
    }
}
