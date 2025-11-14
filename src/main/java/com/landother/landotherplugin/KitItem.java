package com.landother.landotherplugin;

import java.util.List;
import java.util.Map;

public class KitItem {

    private final int slot;
    private final String material;
    private final int amount;
    private final String name;
    private final List<String> lore;
    private final String action;

    public KitItem(Map<?, ?> itemData) {
        this.slot = (Integer) itemData.get("slot");
        this.material = (String) itemData.get("material");
        this.amount = itemData.containsKey("amount") ? (Integer) itemData.get("amount") : 1;
        this.name = (String) itemData.get("name");
        this.lore = (List<String>) itemData.get("lore");
        this.action = (String) itemData.get("action");
    }

    public int getSlot() {
        return slot;
    }

    public String getMaterial() {
        return material;
    }

    public int getAmount() {
        return amount;
    }

    public String getName() {
        return name;
    }

    public List<String> getLore() {
        return lore;
    }

    public String getAction() {
        return action;
    }
}