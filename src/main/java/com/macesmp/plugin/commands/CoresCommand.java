package com.macesmp.plugin.commands;

import com.macesmp.plugin.MaceSMP;
import com.macesmp.plugin.managers.CoreManager;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;

public class CoresCommand implements CommandExecutor {

    private final MaceSMP plugin;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("MMM d, HH:mm");

    public CoresCommand(MaceSMP plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            return showOwnBalance(sender);
        }

        switch (args[0].toLowerCase()) {
            case "balance", "bal" -> {
                if (args.length >= 2) {
                    return showOtherBalance(sender, Bukkit.getOfflinePlayer(args[1]));
                }
                return showOwnBalance(sender);
            }
            case "give" -> {
                return handleGive(sender, args);
            }
            case "set" -> {
                return handleSet(sender, args);
            }
            case "reload" -> {
                if (!hasNode(sender, "macesmp.command.reload")) {
                    plugin.getMessageManager().send(sender, "no-permission");
                    return true;
                }
                plugin.reloadAll();
                plugin.getMessageManager().send(sender, "reload-success");
                return true;
            }
            case "history", "logs" -> {
                return handleHistory(sender, args);
            }
            default -> {
                sender.sendMessage(plugin.getMessageManager().color(
                        "&cUsage: /cores [balance|give|set|reload|history] [player] [amount]"));
                return true;
            }
        }
    }

    private boolean handleGive(CommandSender sender, String[] args) {
        if (!isHardAuthorized(sender, "macesmp.command.give")) {
            plugin.getMessageManager().send(sender, "no-permission");
            return true;
        }
        if (args.length < 3) {
            sender.sendMessage(plugin.getMessageManager().color("&cUsage: /cores give <player> <amount>"));
            return true;
        }
        Player target = Bukkit.getPlayerExact(args[1]);
        if (target == null) {
            plugin.getMessageManager().send(sender, "player-not-found");
            return true;
        }
        Integer amount = parsePositiveInt(args[2]);
        if (amount == null) {
            plugin.getMessageManager().send(sender, "invalid-amount");
            return true;
        }
        plugin.getCoreManager().addBalance(target.getUniqueId(), amount, "ADMIN_GIVE", "By " + sender.getName());
        plugin.getMessageManager().send(sender, "given-cores", Map.of(
                "amount", String.valueOf(amount), "player", target.getName()));
        plugin.getMessageManager().send(target, "received-cores", Map.of("amount", String.valueOf(amount)));
        plugin.getScoreboardManager().updateFor(target);
        return true;
    }

    private boolean handleSet(CommandSender sender, String[] args) {
        if (!isHardAuthorized(sender, "macesmp.command.set")) {
            plugin.getMessageManager().send(sender, "no-permission");
            return true;
        }
        if (args.length < 3) {
            sender.sendMessage(plugin.getMessageManager().color("&cUsage: /cores set <player> <amount>"));
            return true;
        }
        Player target = Bukkit.getPlayerExact(args[1]);
        if (target == null) {
            plugin.getMessageManager().send(sender, "player-not-found");
            return true;
        }
        Integer amount = parsePositiveInt(args[2]);
        if (amount == null) {
            plugin.getMessageManager().send(sender, "invalid-amount");
            return true;
        }
        plugin.getCoreManager().setBalance(target.getUniqueId(), amount);
        plugin.getMessageManager().send(sender, "set-cores", Map.of(
                "amount", String.valueOf(amount), "player", target.getName()));
        plugin.getScoreboardManager().updateFor(target);
        return true;
    }

    private boolean handleHistory(CommandSender sender, String[] args) {
        if (!hasNode(sender, "macesmp.command.history")) {
            plugin.getMessageManager().send(sender, "no-permission");
            return true;
        }
        if (args.length < 2) {
            sender.sendMessage(plugin.getMessageManager().color("&cUsage: /cores history <player>"));
            return true;
        }
        OfflinePlayer target = Bukkit.getOfflinePlayer(args[1]);
        List<CoreManager.HistoryEntry> entries = plugin.getCoreManager().getHistory(target.getUniqueId());

        String name = target.getName() != null ? target.getName() : args[1];
        sender.sendMessage(plugin.getMessageManager().color("&8&m                                        "));
        sender.sendMessage(plugin.getMessageManager().color("&bCores history for &f" + name + " &7(last " + entries.size() + ")"));
        if (entries.isEmpty()) {
            sender.sendMessage(plugin.getMessageManager().color("&7No history recorded."));
        }
        for (CoreManager.HistoryEntry entry : entries) {
            String sign = entry.amount() >= 0 ? "&a+" : "&c";
            String time = dateFormat.format(new Date(entry.timestamp()));
            String line = "&7[" + time + "] " + sign + entry.amount() + " &7(" + entry.type() + ")"
                    + (entry.note().isEmpty() ? "" : " &8- &7" + entry.note())
                    + " &8-> &f" + entry.balanceAfter();
            sender.sendMessage(plugin.getMessageManager().color(line));
        }
        sender.sendMessage(plugin.getMessageManager().color("&8&m                                        "));
        return true;
    }

    private boolean showOwnBalance(CommandSender sender) {
        if (!sender.hasPermission("macesmp.cores")) {
            plugin.getMessageManager().send(sender, "no-permission");
            return true;
        }
        if (!(sender instanceof Player player)) {
            sender.sendMessage(plugin.getMessageManager().color("&cConsole has no core balance. Use /cores balance <player>."));
            return true;
        }
        int balance = plugin.getCoreManager().getBalance(player.getUniqueId());
        plugin.getMessageManager().send(sender, "balance-self", Map.of("amount", String.valueOf(balance)));
        return true;
    }

    private boolean showOtherBalance(CommandSender sender, OfflinePlayer target) {
        if (!hasNode(sender, "macesmp.cores.others")) {
            plugin.getMessageManager().send(sender, "no-permission");
            return true;
        }
        if (target == null || (target.getName() == null && !target.hasPlayedBefore())) {
            plugin.getMessageManager().send(sender, "player-not-found");
            return true;
        }
        int balance = plugin.getCoreManager().getBalance(target.getUniqueId());
        String name = target.getName() != null ? target.getName() : "Unknown";
        plugin.getMessageManager().send(sender, "balance-other", Map.of(
                "amount", String.valueOf(balance), "player", name));
        return true;
    }

    /** True if the sender has the specific node OR the catch-all macesmp.admin. */
    private boolean hasNode(CommandSender sender, String node) {
        return sender.hasPermission(node) || sender.hasPermission("macesmp.admin");
    }

    /**
     * Used for /cores give and /cores set specifically, since those move real
     * currency. Requires the permission node AND, for a real player, actual
     * server-operator status - so a misconfigured permission grant alone can't
     * let a non-staff player move Cores. Console is always trusted.
     */
    private boolean isHardAuthorized(CommandSender sender, String node) {
        if (!hasNode(sender, node)) return false;
        return !(sender instanceof Player player) || player.isOp();
    }

    private Integer parsePositiveInt(String input) {
        try {
            int value = Integer.parseInt(input);
            return value < 0 ? null : value;
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
