package com.landother.landotherplugin;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class KitManager {

    private final LandOtherPlugin plugin;
    private final Map<String, Kit> kits = new HashMap<>();
    private final Map<UUID, KitInventoryData> playerKitInventories = new HashMap<>();
    private File kitsFolder;

    public KitManager(LandOtherPlugin plugin) {
        this.plugin = plugin;
        initializeKitsFolder();
        loadAllKits();
    }

    private void initializeKitsFolder() {
        kitsFolder = new File(plugin.getDataFolder(), "kit");
        if (!kitsFolder.exists()) {
            kitsFolder.mkdirs();
        }
    }

    public boolean createKit(String kitName, Player creator) {
        if (kits.containsKey(kitName)) {
            return false;
        }

        File kitFile = new File(kitsFolder, kitName + ".yml");
        if (kitFile.exists()) {
            return false;
        }

        try {

            FileConfiguration kitConfig = new YamlConfiguration();


            kitConfig.set("name", kitName);
            kitConfig.set("title", "&6&l" + kitName + " &f礼包");
            kitConfig.set("size", 27);
            kitConfig.set("confirm-slot", 22);
            kitConfig.set("cancel-slot", 31);
            kitConfig.set("permission", "landother.kit." + kitName);
            kitConfig.set("one-time", true);
            kitConfig.set("cooldown", 0);


            List<Map<String, Object>> items = new ArrayList<>();


            Map<String, Object> exampleItem1 = new HashMap<>();
            exampleItem1.put("slot", 10);
            exampleItem1.put("material", "DIAMOND_SWORD");
            exampleItem1.put("amount", 1);
            exampleItem1.put("name", "&b&l传说之剑");
            List<String> lore1 = Arrays.asList("&7一把锋利的剑", "&e右键使用");
            exampleItem1.put("lore", lore1);
            items.add(exampleItem1);

            Map<String, Object> exampleItem2 = new HashMap<>();
            exampleItem2.put("slot", 12);
            exampleItem2.put("material", "GOLDEN_APPLE");
            exampleItem2.put("amount", 5);
            exampleItem2.put("name", "&6&l金苹果");
            items.add(exampleItem2);


            Map<String, Object> confirmButton = new HashMap<>();
            confirmButton.put("slot", 22);
            confirmButton.put("material", "EMERALD_BLOCK");
            confirmButton.put("amount", 1);
            confirmButton.put("name", "&a&l✔ 确认领取");
            List<String> confirmLore = Arrays.asList("&7点击确认领取此礼包", "&c注意：领取后无法退回！");
            confirmButton.put("lore", confirmLore);
            confirmButton.put("action", "confirm");
            items.add(confirmButton);


            Map<String, Object> cancelButton = new HashMap<>();
            cancelButton.put("slot", 23);
            cancelButton.put("material", "REDSTONE_BLOCK");
            cancelButton.put("amount", 1);
            cancelButton.put("name", "&c&l✖ 取消");
            List<String> cancelLore = Arrays.asList("&7点击取消领取");
            cancelButton.put("lore", cancelLore);
            cancelButton.put("action", "cancel");
            items.add(cancelButton);

            kitConfig.set("items", items);


            kitConfig.save(kitFile);


            Kit newKit = new Kit(kitName, kitConfig);
            kits.put(kitName, newKit);

            return true;

        } catch (IOException e) {
            plugin.getLogger().severe("创建kit文件失败: " + e.getMessage());
            return false;
        }
    }

    public void reloadKits() {
        kits.clear();
        loadAllKits();
        playerKitInventories.clear();
        plugin.getLogger().info("已重载所有kit配置");
    }

    private void loadAllKits() {
        if (!kitsFolder.exists()) {
            return;
        }

        File[] kitFiles = kitsFolder.listFiles((dir, name) -> name.endsWith(".yml"));
        if (kitFiles != null) {
            for (File kitFile : kitFiles) {
                try {
                    String kitName = kitFile.getName().replace(".yml", "");
                    FileConfiguration kitConfig = YamlConfiguration.loadConfiguration(kitFile);

                    Kit kit = new Kit(kitName, kitConfig);
                    kits.put(kitName, kit);

                } catch (Exception e) {
                    plugin.getLogger().warning("加载kit文件失败: " + kitFile.getName() + " - " + e.getMessage());
                }
            }
        }
    }

    public boolean openKit(Player player, String kitName) {
        Kit kit = kits.get(kitName.toLowerCase());
        if (kit == null) {
            player.sendMessage(ChatColor.RED + "Kit '" + kitName + "' 不存在!");
            return false;
        }


        if (!player.hasPermission(kit.getPermission())) {
            player.sendMessage(ChatColor.RED + "你没有权限使用这个kit!");
            return false;
        }


        if (kit.hasCooldown(player.getUniqueId())) {
            long remainingTime = kit.getRemainingCooldown(player.getUniqueId());
            player.sendMessage(ChatColor.RED + "Kit冷却中，还需等待: " + formatTime(remainingTime));
            return false;
        }


        if (kit.isOneTime() && kit.hasClaimed(player.getUniqueId())) {
            player.sendMessage(ChatColor.RED + "你已经领取过这个kit了!");
            return false;
        }


        Inventory kitInventory = Bukkit.createInventory(null, kit.getSize(), kit.getTitle());


        for (KitItem kitItem : kit.getItems()) {
            ItemStack item = createKitItem(kitItem);
            kitInventory.setItem(kitItem.getSlot(), item);
        }


        playerKitInventories.put(player.getUniqueId(), new KitInventoryData(kitName, kitInventory));

        player.openInventory(kitInventory);
        return true;
    }

    private ItemStack createKitItem(KitItem kitItem) {
        ItemStack item = new ItemStack(Material.valueOf(kitItem.getMaterial()), kitItem.getAmount());

        if (kitItem.getName() != null || kitItem.getLore() != null) {
            ItemMeta meta = item.getItemMeta();

            if (kitItem.getName() != null) {
                meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', kitItem.getName()));
            }

            if (kitItem.getLore() != null) {
                List<String> coloredLore = new ArrayList<>();
                for (String loreLine : kitItem.getLore()) {
                    coloredLore.add(ChatColor.translateAlternateColorCodes('&', loreLine));
                }
                meta.setLore(coloredLore);
            }

            item.setItemMeta(meta);
        }

        return item;
    }

    public boolean handleKitClick(Player player, int slot) {
        KitInventoryData data = playerKitInventories.get(player.getUniqueId());
        if (data == null) {
            return false;
        }

        Kit kit = kits.get(data.getKitName());
        if (kit == null) {
            return false;
        }

        KitItem clickedItem = kit.getItem(slot);
        if (clickedItem == null) {
            return true;
        }

        String action = clickedItem.getAction();
        if ("confirm".equals(action)) {

            return giveKitToPlayer(player, kit);
        } else if ("cancel".equals(action)) {

            player.closeInventory();
            player.sendMessage(ChatColor.YELLOW + "已取消领取kit!");
            return true;
        }

        return true;
    }

    private boolean giveKitToPlayer(Player player, Kit kit) {
        List<ItemStack> itemsToGive = new ArrayList<>();

        for (KitItem kitItem : kit.getItems()) {
            if (kitItem.getAction() == null || kitItem.getAction().isEmpty()) {

                ItemStack item = createKitItem(kitItem);
                itemsToGive.add(item);
            }
        }


        if (!hasInventorySpace(player, itemsToGive)) {
            player.sendMessage(ChatColor.RED + "背包空间不足，无法领取kit!");
            return false;
        }


        for (ItemStack item : itemsToGive) {
            player.getInventory().addItem(item);
        }


        kit.claim(player.getUniqueId());

        player.closeInventory();
        player.sendMessage(ChatColor.GREEN + "成功领取kit: " + kit.getName());

        return true;
    }

    private boolean hasInventorySpace(Player player, List<ItemStack> items) {
        int requiredSlots = 0;
        for (ItemStack item : items) {
            requiredSlots += (int) Math.ceil((double) item.getAmount() / item.getMaxStackSize());
        }

        return player.getInventory().firstEmpty() != -1 || getEmptySlots(player) >= requiredSlots;
    }

    private int getEmptySlots(Player player) {
        int emptySlots = 0;
        for (ItemStack item : player.getInventory().getContents()) {
            if (item == null || item.getType() == Material.AIR) {
                emptySlots++;
            }
        }
        return emptySlots;
    }

    private String formatTime(long seconds) {
        if (seconds < 60) {
            return seconds + "秒";
        } else if (seconds < 3600) {
            return (seconds / 60) + "分" + (seconds % 60) + "秒";
        } else {
            return (seconds / 3600) + "时" + ((seconds % 3600) / 60) + "分";
        }
    }

    public void removePlayerKitData(UUID playerId) {
        playerKitInventories.remove(playerId);
    }

    public boolean isKitInventory(UUID playerId) {
        return playerKitInventories.containsKey(playerId);
    }

    public boolean hasKit(String kitName) {
        return kits.containsKey(kitName.toLowerCase());
    }

    public Set<String> getKitNames() {
        return new HashSet<>(kits.keySet());
    }


    public boolean resetPlayerKitClaim(String kitName, UUID playerId) {
        Kit kit = kits.get(kitName.toLowerCase());
        if (kit == null) {
            return false;
        }

        return kit.resetPlayerClaim(playerId);
    }


    public int resetPlayerAllKitClaims(UUID playerId) {
        int resetCount = 0;
        for (Kit kit : kits.values()) {
            if (kit.resetPlayerClaim(playerId)) {
                resetCount++;
            }
        }
        return resetCount;
    }
}