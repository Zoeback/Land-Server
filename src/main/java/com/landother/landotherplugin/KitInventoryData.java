package com.landother.landotherplugin;

import org.bukkit.inventory.Inventory;

import java.util.UUID;

public class KitInventoryData {

    private final String kitName;
    private final Inventory inventory;

    public KitInventoryData(String kitName, Inventory inventory) {
        this.kitName = kitName;
        this.inventory = inventory;
    }

    public String getKitName() {
        return kitName;
    }

    public Inventory getInventory() {
        return inventory;
    }
}