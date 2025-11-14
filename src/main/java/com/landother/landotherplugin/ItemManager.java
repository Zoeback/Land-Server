package com.landother.landotherplugin;

import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;

import java.util.*;

public class ItemManager {

    private final LandOtherPlugin plugin;
    private final Map<String, UUID> claimedItems;
    private final Map<UUID, List<String>> playerClaimedItems;

    public ItemManager(LandOtherPlugin plugin) {
        this.plugin = plugin;
        this.claimedItems = new HashMap<>();
        this.playerClaimedItems = new HashMap<>();


        loadClaimedItems();
    }


    public boolean isItemClaimed(ItemStack item) {
        String itemId = getItemIdentifier(item);
        return claimedItems.containsKey(itemId);
    }


    public boolean claimItem(ItemStack item, UUID playerUuid) {
        if (item == null || playerUuid == null) {
            return false;
        }

        String itemId = getItemIdentifier(item);


        if (claimedItems.containsKey(itemId)) {
            return false;
        }


        if (playerClaimedItems.containsKey(playerUuid) && !playerClaimedItems.get(playerUuid).isEmpty()) {
            return false;
        }


        claimedItems.put(itemId, playerUuid);


        playerClaimedItems.computeIfAbsent(playerUuid, k -> new ArrayList<>()).add(itemId);


        saveClaimedItems();

        return true;
    }


    private String getItemIdentifier(ItemStack item) {
        if (item == null) {
            return "null";
        }

        StringBuilder identifier = new StringBuilder();
        identifier.append(item.getType().name());
        identifier.append("_");
        identifier.append(item.getAmount());


        if (item.hasItemMeta()) {
            ItemMeta meta = item.getItemMeta();

            if (meta.hasDisplayName()) {
                identifier.append("_name:");
                identifier.append(meta.getDisplayName().replaceAll("[^a-zA-Z0-9]", "_"));
            }

            if (meta.hasLore()) {
                identifier.append("_lore:");
                for (String loreLine : meta.getLore()) {
                    identifier.append(loreLine.replaceAll("[^a-zA-Z0-9]", "_"));
                    identifier.append("|");
                }
            }

            if (meta.hasEnchants()) {
                identifier.append("_enchants:");
                for (Map.Entry<Enchantment, Integer> enchant : meta.getEnchants().entrySet()) {
                    identifier.append(enchant.getKey().getKey().getKey());
                    identifier.append("_");
                    identifier.append(enchant.getValue());
                    identifier.append(";");
                }
            }

            if (meta.hasCustomModelData()) {
                identifier.append("_cmd:");
                identifier.append(meta.getCustomModelData());
            }

            if (meta.isUnbreakable()) {
                identifier.append("_unbreakable");
            }

            if (!meta.getItemFlags().isEmpty()) {
                identifier.append("_flags:");
                for (ItemFlag flag : meta.getItemFlags()) {
                    identifier.append(flag.name());
                    identifier.append(";");
                }
            }
        }

        return identifier.toString();
    }


    public UUID getItemClaimer(ItemStack item) {
        String itemId = getItemIdentifier(item);
        return claimedItems.get(itemId);
    }


    public List<String> getPlayerClaimedItems(UUID playerUuid) {
        return playerClaimedItems.getOrDefault(playerUuid, new ArrayList<>());
    }


    public boolean resetItemClaim(ItemStack item) {
        String itemId = getItemIdentifier(item);
        UUID playerUuid = claimedItems.remove(itemId);

        if (playerUuid != null) {
            List<String> playerItems = playerClaimedItems.get(playerUuid);
            if (playerItems != null) {
                playerItems.remove(itemId);
                if (playerItems.isEmpty()) {
                    playerClaimedItems.remove(playerUuid);
                }
            }

            saveClaimedItems();
            return true;
        }

        return false;
    }


    public boolean resetPlayerClaims(UUID playerUuid) {
        List<String> playerItems = playerClaimedItems.remove(playerUuid);

        if (playerItems != null) {
            for (String itemId : playerItems) {
                claimedItems.remove(itemId);
            }

            saveClaimedItems();
            return true;
        }

        return false;
    }


    public int getClaimedItemCount() {

        return claimedItems.size();
    }


    public int getClaimerCount() {
        return playerClaimedItems.size();
    }


    private void saveClaimedItems() {
        plugin.getConfig().set("claimed_items", new HashMap<>(claimedItems));
        plugin.getConfig().set("player_claimed_items", new HashMap<>(playerClaimedItems));
        plugin.saveConfig();
    }


    @SuppressWarnings("unchecked")
    private void loadClaimedItems() {
        if (plugin.getConfig().contains("claimed_items")) {
            Map<String, Object> claimedItemsData = plugin.getConfig().getConfigurationSection("claimed_items").getValues(false);
            for (Map.Entry<String, Object> entry : claimedItemsData.entrySet()) {
                if (entry.getValue() instanceof String) {
                    try {
                        UUID playerUuid = UUID.fromString((String) entry.getValue());
                        claimedItems.put(entry.getKey(), playerUuid);
                    } catch (IllegalArgumentException e) {
                        plugin.getLogger().warning("无效的UUID格式: " + entry.getValue());
                    }
                }
            }
        }

        if (plugin.getConfig().contains("player_claimed_items")) {
            Map<String, Object> playerClaimedData = plugin.getConfig().getConfigurationSection("player_claimed_items").getValues(false);
            for (Map.Entry<String, Object> entry : playerClaimedData.entrySet()) {
                if (entry.getValue() instanceof List) {
                    try {
                        UUID playerUuid = UUID.fromString(entry.getKey());
                        List<String> items = (List<String>) entry.getValue();
                        playerClaimedItems.put(playerUuid, new ArrayList<>(items));
                    } catch (IllegalArgumentException e) {
                        plugin.getLogger().warning("无效的玩家UUID格式: " + entry.getKey());
                    }
                }
            }
        }
    }


    public Map<String, UUID> getAllClaimedItems() {
        return new HashMap<>(claimedItems);
    }


    public Map<UUID, List<String>> getAllPlayerClaimedItems() {
        return new HashMap<>(playerClaimedItems);
    }
}