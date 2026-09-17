package com.macesmp.plugin.managers;

import org.bukkit.Material;

import java.util.ArrayList;
import java.util.List;

/**
 * A category shown as a single icon in the shop's main menu (e.g. "Blocks", "PvP Gear").
 */
public class ShopCategory {

    private final String id;
    private final Material icon;
    private final String displayName;
    private final int slot;
    private final List<ShopItem> items = new ArrayList<>();

    public ShopCategory(String id, Material icon, String displayName, int slot) {
        this.id = id;
        this.icon = icon;
        this.displayName = displayName;
        this.slot = slot;
    }

    public String getId() {
        return id;
    }

    public Material getIcon() {
        return icon;
    }

    public String getDisplayName() {
        return displayName;
    }

    public int getSlot() {
        return slot;
    }

    public List<ShopItem> getItems() {
        return items;
    }

    public void addItem(ShopItem item) {
        items.add(item);
    }
}
