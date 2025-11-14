package com.landother.landotherplugin;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.logging.Level;

public class PlayerData {

    private final LandOtherPlugin plugin;
    private final Map<UUID, PlayerInfo> playerDataMap;
    private File dataFolder;

    public PlayerData(LandOtherPlugin plugin) {
        this.plugin = plugin;
        this.playerDataMap = new HashMap<>();
        this.dataFolder = new File(plugin.getDataFolder(), "playerdata");


        if (!dataFolder.exists()) {
            dataFolder.mkdirs();
        }


        loadAllPlayerData();
    }


    public static class PlayerInfo {
        private final UUID uuid;
        private String playerName;
        private boolean hasClaimedItem;
        private ItemStack claimedItem;
        private long claimTime;

        public PlayerInfo(UUID uuid, String playerName) {
            this.uuid = uuid;
            this.playerName = playerName;
            this.hasClaimedItem = false;
            this.claimedItem = null;
            this.claimTime = 0;
        }

        public UUID getUuid() {
            return uuid;
        }

        public String getPlayerName() {
            return playerName;
        }

        public void setPlayerName(String playerName) {
            this.playerName = playerName;
        }

        public boolean hasClaimedItem() {
            return hasClaimedItem;
        }

        public void setHasClaimedItem(boolean hasClaimedItem) {
            this.hasClaimedItem = hasClaimedItem;
        }

        public ItemStack getClaimedItem() {
            return claimedItem;
        }

        public void setClaimedItem(ItemStack claimedItem) {
            this.claimedItem = claimedItem;
            this.claimTime = System.currentTimeMillis();
        }

        public long getClaimTime() {
            return claimTime;
        }

        public void setClaimTime(long claimTime) {
            this.claimTime = claimTime;
        }
    }


    public boolean hasPlayerClaimedItem(UUID playerUuid) {
        PlayerInfo info = playerDataMap.get(playerUuid);
        return info != null && info.hasClaimedItem();
    }


    public void setPlayerClaimedItem(UUID playerUuid, ItemStack item) {
        PlayerInfo info = playerDataMap.computeIfAbsent(playerUuid,
            k -> new PlayerInfo(playerUuid, "Unknown"));

        info.setHasClaimedItem(true);
        info.setClaimedItem(item);


        savePlayerData(playerUuid);
    }


    public ItemStack getPlayerClaimedItem(UUID playerUuid) {
        PlayerInfo info = playerDataMap.get(playerUuid);
        return info != null ? info.getClaimedItem() : null;
    }


    public PlayerInfo getPlayerInfo(UUID playerUuid) {
        return playerDataMap.get(playerUuid);
    }


    public boolean resetPlayerClaimStatus(UUID playerUuid) {
        PlayerInfo info = playerDataMap.get(playerUuid);
        if (info != null && info.hasClaimedItem()) {
            info.setHasClaimedItem(false);
            info.setClaimedItem(null);
            info.setClaimTime(0);

            savePlayerData(playerUuid);
            return true;
        }
        return false;
    }


    public int getClaimedPlayerCount() {
        return (int) playerDataMap.values().stream()
            .filter(PlayerInfo::hasClaimedItem)
            .count();
    }


    public boolean loadPlayerDataById(UUID playerId) {
        return playerDataMap.containsKey(playerId);
    }


    public List<UUID> getAllClaimedPlayers() {
        List<UUID> claimedPlayers = new ArrayList<>();
        for (PlayerInfo info : playerDataMap.values()) {
            if (info.hasClaimedItem()) {
                claimedPlayers.add(info.getUuid());
            }
        }
        return claimedPlayers;
    }


    public void savePlayerData(UUID playerUuid) {
        PlayerInfo info = playerDataMap.get(playerUuid);
        if (info == null) {
            return;
        }

        File playerFile = new File(dataFolder, playerUuid.toString() + ".yml");
        FileConfiguration playerConfig = new YamlConfiguration();


        playerConfig.set("uuid", info.getUuid().toString());
        playerConfig.set("playerName", info.getPlayerName());
        playerConfig.set("hasClaimedItem", info.hasClaimedItem());
        playerConfig.set("claimTime", info.getClaimTime());


        if (info.hasClaimedItem() && info.getClaimedItem() != null) {
            playerConfig.set("claimedItem", info.getClaimedItem());
        }

        try {
            playerConfig.save(playerFile);
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "无法保存玩家数据: " + playerUuid, e);
        }
    }


    public void loadPlayerData(UUID playerUuid) {
        File playerFile = new File(dataFolder, playerUuid.toString() + ".yml");
        if (!playerFile.exists()) {
            return;
        }

        FileConfiguration playerConfig = YamlConfiguration.loadConfiguration(playerFile);

        try {
            UUID uuid = UUID.fromString(playerConfig.getString("uuid"));
            String playerName = playerConfig.getString("playerName", "Unknown");
            boolean hasClaimedItem = playerConfig.getBoolean("hasClaimedItem", false);
            long claimTime = playerConfig.getLong("claimTime", 0);

            PlayerInfo info = new PlayerInfo(uuid, playerName);
            info.setHasClaimedItem(hasClaimedItem);
            info.setClaimTime(claimTime);


            if (hasClaimedItem && playerConfig.contains("claimedItem")) {
                ItemStack claimedItem = playerConfig.getItemStack("claimedItem");
                info.setClaimedItem(claimedItem);
            }

            playerDataMap.put(playerUuid, info);
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "无法加载玩家数据: " + playerUuid, e);
        }
    }


    public void loadAllPlayerData() {
        if (!dataFolder.exists() || !dataFolder.isDirectory()) {
            return;
        }

        File[] playerFiles = dataFolder.listFiles((dir, name) -> name.endsWith(".yml"));
        if (playerFiles == null) {
            return;
        }

        for (File playerFile : playerFiles) {
            String fileName = playerFile.getName();
            String uuidString = fileName.substring(0, fileName.length() - 4);

            try {
                UUID playerUuid = UUID.fromString(uuidString);
                loadPlayerData(playerUuid);
            } catch (IllegalArgumentException e) {
                plugin.getLogger().warning("无效的玩家数据文件名: " + fileName);
            }
        }
    }


    public void saveAllPlayerData() {
        for (UUID playerUuid : playerDataMap.keySet()) {
            savePlayerData(playerUuid);
        }
    }


    public boolean deletePlayerData(UUID playerUuid) {
        playerDataMap.remove(playerUuid);

        File playerFile = new File(dataFolder, playerUuid.toString() + ".yml");
        if (playerFile.exists()) {
            return playerFile.delete();
        }

        return false;
    }


    public Map<UUID, PlayerInfo> getAllPlayerData() {
        return new HashMap<>(playerDataMap);
    }


    public File getDataFolder() {
        return dataFolder;
    }
}