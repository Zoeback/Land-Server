package com.landother.landotherplugin;

import org.bukkit.configuration.file.FileConfiguration;

import java.util.*;

public class Kit {

    private final String name;
    private final String title;
    private final int size;
    private final String permission;
    private final boolean oneTime;
    private final int cooldown;
    private final List<KitItem> items;
    private final Map<UUID, Long> claimedPlayers = new HashMap<>();
    private final Map<UUID, Long> cooldownPlayers = new HashMap<>();
    private final int confirmSlot;
    private final int cancelSlot;

    public Kit(String name, FileConfiguration config) {
        this.name = name;
        this.title = config.getString("title", "&6&l" + name + " &f礼包");
        this.size = config.getInt("size", 27);
        this.permission = config.getString("permission", "landother.kit." + name);
        this.oneTime = config.getBoolean("one-time", true);
        this.cooldown = config.getInt("cooldown", 0);
        this.confirmSlot = config.getInt("confirm-slot", 22);
        this.cancelSlot = config.getInt("cancel-slot", 23);

        this.items = new ArrayList<>();
        if (config.contains("items")) {
            List<Map<?, ?>> itemList = config.getMapList("items");
            for (Map<?, ?> itemData : itemList) {
                items.add(new KitItem(itemData));
            }
        }
    }

    public String getName() {
        return name;
    }

    public String getTitle() {
        return title;
    }

    public int getSize() {
        return size;
    }

    public String getPermission() {
        return permission;
    }

    public boolean isOneTime() {
        return oneTime;
    }

    public int getCooldown() {
        return cooldown;
    }

    public List<KitItem> getItems() {
        return new ArrayList<>(items);
    }

    public KitItem getItem(int slot) {
        for (KitItem item : items) {
            if (item.getSlot() == slot) {
                return item;
            }
        }
        return null;
    }

    public boolean hasClaimed(UUID playerId) {
        return claimedPlayers.containsKey(playerId);
    }

    public void claim(UUID playerId) {
        long currentTime = System.currentTimeMillis();

        if (oneTime) {
            claimedPlayers.put(playerId, currentTime);
        }

        if (cooldown > 0) {
            cooldownPlayers.put(playerId, currentTime + (cooldown * 1000L));
        }
    }

    public boolean hasCooldown(UUID playerId) {
        if (!cooldownPlayers.containsKey(playerId)) {
            return false;
        }

        long cooldownEnd = cooldownPlayers.get(playerId);
        return System.currentTimeMillis() < cooldownEnd;
    }

    public long getRemainingCooldown(UUID playerId) {
        if (!cooldownPlayers.containsKey(playerId)) {
            return 0;
        }

        long cooldownEnd = cooldownPlayers.get(playerId);
        long remaining = (cooldownEnd - System.currentTimeMillis()) / 1000L;
        return Math.max(0, remaining);
    }

    public int getConfirmSlot() {
        return confirmSlot;
    }

    public int getCancelSlot() {
        return cancelSlot;
    }


    public boolean resetPlayerClaim(UUID playerId) {
        boolean resetClaimed = claimedPlayers.remove(playerId) != null;
        boolean resetCooldown = cooldownPlayers.remove(playerId) != null;
        return resetClaimed || resetCooldown;
    }
}