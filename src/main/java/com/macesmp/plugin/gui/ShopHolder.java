package com.macesmp.plugin.gui;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

/**
 * Marks an Inventory as belonging to MaceSMP's shop so the click listener
 * can safely identify it and ignore all other inventories on the server.
 */
public class ShopHolder implements InventoryHolder {

    public enum Type {
        MAIN_MENU,
        CATEGORY
    }

    private final Type type;
    private final String categoryId; // null when type == MAIN_MENU
    private Inventory inventory;

    public ShopHolder(Type type, String categoryId) {
        this.type = type;
        this.categoryId = categoryId;
    }

    public Type getType() {
        return type;
    }

    public String getCategoryId() {
        return categoryId;
    }

    public void setInventory(Inventory inventory) {
        this.inventory = inventory;
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }
}
