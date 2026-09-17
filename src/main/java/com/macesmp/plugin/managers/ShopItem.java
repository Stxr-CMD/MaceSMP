package com.macesmp.plugin.managers;

import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;

import java.util.List;
import java.util.Map;

/**
 * A single purchasable entry inside a shop category.
 */
public class ShopItem {

    private final Material material;
    private final String name;
    private final List<String> lore;
    private final int price;
    private final int amount;
    private final String command; // nullable
    private final List<Map.Entry<Enchantment, Integer>> enchants; // empty if none
    private final int slot; // only used for direct-buy items placed straight in the main menu; -1 = unused

    public ShopItem(Material material, String name, List<String> lore, int price, int amount,
                     String command, List<Map.Entry<Enchantment, Integer>> enchants, int slot) {
        this.material = material;
        this.name = name;
        this.lore = lore;
        this.price = price;
        this.amount = amount;
        this.command = command;
        this.enchants = enchants;
        this.slot = slot;
    }

    public Material getMaterial() {
        return material;
    }

    public String getName() {
        return name;
    }

    public List<String> getLore() {
        return lore;
    }

    public int getPrice() {
        return price;
    }

    public int getAmount() {
        return amount;
    }

    public boolean hasCommand() {
        return command != null && !command.isEmpty();
    }

    public String getCommand() {
        return command;
    }

    public List<Map.Entry<Enchantment, Integer>> getEnchants() {
        return enchants;
    }

    public int getSlot() {
        return slot;
    }
}
