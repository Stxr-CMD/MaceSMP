package com.macesmp.plugin.listeners;

import com.macesmp.plugin.MaceSMP;
import com.macesmp.plugin.gui.ShopHolder;
import com.macesmp.plugin.managers.ShopCategory;
import com.macesmp.plugin.managers.ShopItem;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Handles clicks in the shop GUI: opening categories, buying direct-buy
 * items, navigating back, and buying items inside a category.
 * All clicks/drags in shop inventories are cancelled so nothing can be
 * physically moved in/out, and a per-player lock stops double-purchases
 * from a rapid double click.
 */
public class ShopClickListener implements Listener {

    private final MaceSMP plugin;
    private final Set<UUID> purchaseLock = new HashSet<>();

    public ShopClickListener(MaceSMP plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onDrag(InventoryDragEvent event) {
        if (event.getInventory().getHolder() instanceof ShopHolder) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        InventoryHolder rawHolder = event.getInventory().getHolder();
        if (!(rawHolder instanceof ShopHolder holder)) {
            return; // not one of our GUIs, ignore completely
        }

        // Always cancel clicks inside our shop GUIs so players can't take the icons
        event.setCancelled(true);

        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType() == Material.AIR) {
            return;
        }

        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }

        if (holder.getType() == ShopHolder.Type.MAIN_MENU) {
            ShopItem direct = plugin.getShopManager().getDirectItemAt(event.getSlot());
            if (direct != null) {
                purchase(player, direct);
                return;
            }
            handleMainMenuClick(player, clicked);
        } else if (holder.getType() == ShopHolder.Type.CATEGORY) {
            handleCategoryClick(player, holder, event, clicked);
        }
    }

    private void handleMainMenuClick(Player player, ItemStack clicked) {
        for (Map.Entry<String, ShopCategory> entry : plugin.getShopManager().getCategories().entrySet()) {
            ShopCategory category = entry.getValue();
            if (category.getIcon() == clicked.getType()
                    && clicked.hasItemMeta()
                    && clicked.getItemMeta().hasDisplayName()
                    && clicked.getItemMeta().getDisplayName().equals(category.getDisplayName())) {
                plugin.getShopManager().openCategory(player, entry.getKey());
                return;
            }
        }
    }

    private void handleCategoryClick(Player player, ShopHolder holder, InventoryClickEvent event, ItemStack clicked) {
        Inventory inv = event.getInventory();

        // Back button is centered in the bottom nav row - see ShopManager#openCategory
        int backSlot = inv.getSize() - 5;
        if (event.getSlot() == backSlot && clicked.getType() == Material.ARROW) {
            plugin.getShopManager().openMainMenu(player);
            return;
        }

        if (event.getSlot() >= inv.getSize() - 9) {
            return; // clicked somewhere else in the bottom nav row (including filler panes)
        }

        ShopItem item = plugin.getShopManager().getItemAt(holder.getCategoryId(), event.getSlot());
        if (item == null) {
            return;
        }

        purchase(player, item);
    }

    private void purchase(Player player, ShopItem item) {
        UUID uuid = player.getUniqueId();

        // Anti-dupe: refuse to process a second purchase for this player while
        // one is already being handled (blocks double-click/duplicate-packet abuse).
        if (purchaseLock.contains(uuid)) {
            return;
        }
        purchaseLock.add(uuid);

        try {
            if (!plugin.getCoreManager().hasBalance(uuid, item.getPrice())) {
                plugin.getMessageManager().send(player, "not-enough-cores");
                return;
            }

            boolean withdrawn = plugin.getCoreManager().removeBalance(uuid, item.getPrice(), "PURCHASE", ChatColor.stripColor(item.getName()));
            if (!withdrawn) {
                plugin.getMessageManager().send(player, "not-enough-cores");
                return;
            }

            if (item.hasCommand()) {
                String cmd = item.getCommand().replace("{player}", player.getName());
                plugin.getServer().dispatchCommand(plugin.getServer().getConsoleSender(), cmd);
            } else {
                ItemStack stack = plugin.getShopManager().buildStack(item);
                Map<Integer, ItemStack> overflow = player.getInventory().addItem(stack);
                for (ItemStack leftover : overflow.values()) {
                    player.getWorld().dropItem(player.getLocation(), leftover);
                }
            }

            plugin.getMessageManager().send(player, "purchase-success", Map.of(
                    "item", ChatColor.stripColor(item.getName()),
                    "price", String.valueOf(item.getPrice())
            ));

            plugin.getScoreboardManager().updateFor(player);
        } finally {
            purchaseLock.remove(uuid);
        }
    }
}
