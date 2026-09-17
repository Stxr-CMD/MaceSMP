package com.macesmp.plugin.managers;

import com.macesmp.plugin.MaceSMP;
import com.macesmp.plugin.gui.ShopHolder;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.bukkit.inventory.meta.ItemMeta;

import java.io.File;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

/**
 * Loads shop.yml and builds the shop GUI.
 *
 * Two kinds of entries:
 *  - "categories": click the category icon, it opens a sub-menu of items.
 *  - "direct-items": buyable icons placed straight on the main menu, no
 *    sub-menu - click once and it buys.
 */
public class ShopManager {

    private final MaceSMP plugin;
    private final Map<String, ShopCategory> categories = new LinkedHashMap<>();
    private final Map<Integer, ShopItem> directItems = new LinkedHashMap<>();

    public ShopManager(MaceSMP plugin) {
        this.plugin = plugin;
        load();
    }

    public void reload() {
        categories.clear();
        directItems.clear();
        load();
    }

    private void load() {
        File shopFile = new File(plugin.getDataFolder(), "shop.yml");
        if (!shopFile.exists()) {
            plugin.saveResource("shop.yml", false);
        }
        YamlConfiguration cfg = YamlConfiguration.loadConfiguration(shopFile);

        ConfigurationSection catsSection = cfg.getConfigurationSection("categories");
        if (catsSection != null) {
            for (String catId : catsSection.getKeys(false)) {
                ConfigurationSection cat = catsSection.getConfigurationSection(catId);
                if (cat == null) continue;

                Material icon = parseMaterial(cat.getString("icon", "CHEST"), Material.CHEST);
                String displayName = color(cat.getString("display-name", catId));
                int slot = cat.getInt("slot", 0);
                ShopCategory category = new ShopCategory(catId, icon, displayName, slot);

                for (Map<?, ?> raw : cat.getMapList("items")) {
                    category.addItem(parseItem(raw));
                }
                categories.put(catId, category);
            }
        }

        List<Map<?, ?>> directList = cfg.getMapList("direct-items");
        for (Map<?, ?> raw : directList) {
            ShopItem item = parseItem(raw);
            if (item.getSlot() >= 0) {
                directItems.put(item.getSlot(), item);
            } else {
                plugin.getLogger().warning("A direct-item in shop.yml is missing 'slot', skipping it.");
            }
        }

        if (categories.isEmpty() && directItems.isEmpty()) {
            plugin.getLogger().warning("shop.yml has no categories or direct-items - the shop will be empty.");
        }
        plugin.getLogger().info("Loaded " + categories.size() + " categories and " + directItems.size() + " direct-buy items.");
    }

    private ShopItem parseItem(Map<?, ?> raw) {
        Material material = parseMaterial(String.valueOf(raw.get("material")), Material.STONE);
        Object nameObj = raw.get("name");
        String name = color(nameObj != null ? String.valueOf(nameObj) : material.name());
        int price = raw.get("price") instanceof Number n ? n.intValue() : 0;
        int amount = raw.get("amount") instanceof Number n ? n.intValue() : 1;
        int slot = raw.get("slot") instanceof Number n ? n.intValue() : -1;
        String command = raw.get("command") != null ? String.valueOf(raw.get("command")) : null;

        List<String> lore = new ArrayList<>();
        if (raw.get("lore") instanceof List<?> loreList) {
            for (Object line : loreList) {
                lore.add(color(String.valueOf(line))
                        .replace("{price}", String.valueOf(price))
                        .replace("{icon}", plugin.getMessageManager().icon()));
            }
        }

        return new ShopItem(material, name, lore, price, amount, command, parseEnchants(raw.get("enchant")), slot);
    }

    /**
     * "wind_burst:1" or "sharpness:5,unbreaking:3" -> real Enchantment + level pairs.
     * Bad entries are skipped with a warning instead of breaking the whole item.
     */
    private List<Map.Entry<Enchantment, Integer>> parseEnchants(Object raw) {
        List<Map.Entry<Enchantment, Integer>> result = new ArrayList<>();
        if (raw == null) return result;

        for (String part : String.valueOf(raw).split(",")) {
            String trimmed = part.trim();
            if (trimmed.isEmpty()) continue;

            String[] pieces = trimmed.split(":");
            String key = pieces[0].trim().toLowerCase();
            int level = pieces.length > 1 ? parseIntSafe(pieces[1].trim(), 1) : 1;

            Enchantment enchantment = Registry.ENCHANTMENT.get(NamespacedKey.minecraft(key));
            if (enchantment == null) {
                plugin.getLogger().warning("Unknown enchantment '" + key + "' in shop.yml, skipping.");
                continue;
            }
            result.add(new AbstractMap.SimpleEntry<>(enchantment, level));
        }
        return result;
    }

    private int parseIntSafe(String s, int fallback) {
        try {
            return Integer.parseInt(s);
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    private Material parseMaterial(String name, Material fallback) {
        try {
            return Material.valueOf(name.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            plugin.getLogger().log(Level.WARNING, "Unknown material '" + name + "' in shop.yml, using " + fallback);
            return fallback;
        }
    }

    private String color(String text) {
        return ChatColor.translateAlternateColorCodes('&', text);
    }

    public Map<String, ShopCategory> getCategories() {
        return categories;
    }

    public ShopItem getDirectItemAt(int slot) {
        return directItems.get(slot);
    }

    private ItemStack filler() {
        ItemStack pane = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        ItemMeta meta = pane.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(" ");
            pane.setItemMeta(meta);
        }
        return pane;
    }

    /**
     * Builds the ItemStack for a shop entry, enchants included. Used for both
     * the preview icon and the actual item handed over on purchase, so what
     * you see is exactly what you get.
     */
    public ItemStack buildStack(ShopItem item) {
        ItemStack stack = new ItemStack(item.getMaterial(), Math.max(1, item.getAmount()));
        ItemMeta meta = stack.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(item.getName());
            meta.setLore(item.getLore());
            stack.setItemMeta(meta);
        }

        if (!item.getEnchants().isEmpty()) {
            ItemMeta enchantMeta = stack.getItemMeta();
            if (enchantMeta instanceof EnchantmentStorageMeta bookMeta) {
                for (Map.Entry<Enchantment, Integer> entry : item.getEnchants()) {
                    bookMeta.addStoredEnchant(entry.getKey(), entry.getValue(), true);
                }
                stack.setItemMeta(bookMeta);
            } else if (enchantMeta != null) {
                for (Map.Entry<Enchantment, Integer> entry : item.getEnchants()) {
                    enchantMeta.addEnchant(entry.getKey(), entry.getValue(), true);
                }
                stack.setItemMeta(enchantMeta);
            }
        }

        return stack;
    }

    public void openMainMenu(Player player) {
        int rows = plugin.getConfig().getInt("shop.rows", 6);
        String title = color(plugin.getConfig().getString("shop.title", "&8Core Shop"));

        ShopHolder holder = new ShopHolder(ShopHolder.Type.MAIN_MENU, null);
        Inventory inv = plugin.getServer().createInventory(holder, rows * 9, title);
        holder.setInventory(inv);

        ItemStack border = filler();
        for (int i = 0; i < inv.getSize(); i++) {
            inv.setItem(i, border);
        }

        for (ShopCategory category : categories.values()) {
            ItemStack icon = new ItemStack(category.getIcon());
            ItemMeta meta = icon.getItemMeta();
            if (meta != null) {
                meta.setDisplayName(category.getDisplayName());
                meta.setLore(List.of(ChatColor.GRAY + "Click to browse this category"));
                icon.setItemMeta(meta);
            }
            if (category.getSlot() >= 0 && category.getSlot() < inv.getSize()) {
                inv.setItem(category.getSlot(), icon);
            }
        }

        for (Map.Entry<Integer, ShopItem> entry : directItems.entrySet()) {
            if (entry.getKey() >= 0 && entry.getKey() < inv.getSize()) {
                inv.setItem(entry.getKey(), buildStack(entry.getValue()));
            }
        }

        player.openInventory(inv);
    }

    public void openCategory(Player player, String categoryId) {
        ShopCategory category = categories.get(categoryId);
        if (category == null) return;

        String title = color("&8Core Shop &7- " + ChatColor.stripColor(category.getDisplayName()));
        ShopHolder holder = new ShopHolder(ShopHolder.Type.CATEGORY, categoryId);
        int rowsNeeded = Math.max(2, Math.min(6, (int) Math.ceil((category.getItems().size() + 9) / 9.0)));
        Inventory inv = plugin.getServer().createInventory(holder, rowsNeeded * 9, title);
        holder.setInventory(inv);

        ItemStack border = filler();
        for (int i = 0; i < inv.getSize(); i++) {
            inv.setItem(i, border);
        }

        int slot = 0;
        for (ShopItem item : category.getItems()) {
            if (slot >= inv.getSize() - 9) break;
            inv.setItem(slot, buildStack(item));
            slot++;
        }

        ItemStack back = new ItemStack(Material.ARROW);
        ItemMeta backMeta = back.getItemMeta();
        if (backMeta != null) {
            backMeta.setDisplayName(ChatColor.YELLOW + "« Back");
            back.setItemMeta(backMeta);
        }
        inv.setItem(inv.getSize() - 5, back);

        player.openInventory(inv);
    }

    public ShopItem getItemAt(String categoryId, int slot) {
        ShopCategory category = categories.get(categoryId);
        if (category == null || slot < 0 || slot >= category.getItems().size()) return null;
        return category.getItems().get(slot);
    }
}
