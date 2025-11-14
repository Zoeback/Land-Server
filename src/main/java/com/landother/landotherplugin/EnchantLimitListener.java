package com.landother.landotherplugin;

import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.enchantment.EnchantItemEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryMoveItemEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerPickupItemEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.inventory.AnvilInventory;
import org.bukkit.inventory.EnchantingInventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;


public class EnchantLimitListener implements Listener {

    private final LandOtherPlugin plugin;
    private final Map<UUID, Map<Enchantment, Integer>> playerEnchantLimits;

    public EnchantLimitListener(LandOtherPlugin plugin) {
        this.plugin = plugin;
        this.playerEnchantLimits = new HashMap<>();
        loadConfig();
    }


    public void loadConfig() {
        FileConfiguration config = plugin.getConfig();


        if (!config.getBoolean("enchantment-limits.enabled", true)) {
            return;
        }

        playerEnchantLimits.clear();


        if (config.contains("enchantment-limits.player-limits")) {
            for (String playerName : config.getConfigurationSection("enchantment-limits.player-limits").getKeys(false)) {
                int maxLevel = config.getInt("enchantment-limits.player-limits." + playerName, 255);



                UUID placeholderUUID = UUID.nameUUIDFromBytes(playerName.getBytes());
                Map<Enchantment, Integer> limits = new HashMap<>();
                limits.put(null, maxLevel);
                playerEnchantLimits.put(placeholderUUID, limits);
            }
        }
    }


    @EventHandler
    public void onEnchantItem(EnchantItemEvent event) {
        if (!plugin.getConfig().getBoolean("enchantment-limits.enabled", true)) {
            return;
        }

        Player player = event.getEnchanter();
        Map<Enchantment, Integer> enchantments = event.getEnchantsToAdd();

        boolean modified = false;
        Map<Enchantment, Integer> toRemove = new HashMap<>();
        Map<Enchantment, Integer> toAdd = new HashMap<>();

        for (Map.Entry<Enchantment, Integer> entry : enchantments.entrySet()) {
            Enchantment enchant = entry.getKey();
            int level = entry.getValue();
            int maxLevel = getMaxEnchantLevel(player, enchant);

            if (level > maxLevel) {
                toRemove.put(enchant, level);
                toAdd.put(enchant, maxLevel);
                modified = true;
            }
        }

        if (modified) {

            for (Map.Entry<Enchantment, Integer> entry : toRemove.entrySet()) {
                enchantments.remove(entry.getKey());
            }


            for (Map.Entry<Enchantment, Integer> entry : toAdd.entrySet()) {
                enchantments.put(entry.getKey(), entry.getValue());
            }

            player.sendMessage(ChatColor.YELLOW + "附魔等级已被限制为最高 " + getMaxEnchantLevel(player, null) + " 级");
        }
    }


    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!plugin.getConfig().getBoolean("enchantment-limits.enabled", true)) {
            return;
        }

        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }

        if (event.getInventory().getType() != InventoryType.ANVIL) {
            return;
        }

        Player player = (Player) event.getWhoClicked();
        AnvilInventory anvil = (AnvilInventory) event.getInventory();


        if (event.getRawSlot() != 2) {
            return;
        }

        ItemStack result = anvil.getItem(2);
        if (result == null || result.getType() == org.bukkit.Material.AIR) {
            return;
        }


        if (result.hasItemMeta() && result.getItemMeta().hasEnchants()) {
            ItemMeta meta = result.getItemMeta();
            Map<Enchantment, Integer> enchantments = meta.getEnchants();

            boolean modified = false;
            Map<Enchantment, Integer> newEnchantments = new HashMap<>();

            for (Map.Entry<Enchantment, Integer> entry : enchantments.entrySet()) {
                Enchantment enchant = entry.getKey();
                int level = entry.getValue();
                int maxLevel = getMaxEnchantLevel(player, enchant);

                if (level > maxLevel) {
                    newEnchantments.put(enchant, maxLevel);
                    modified = true;
                } else {
                    newEnchantments.put(enchant, level);
                }
            }

            if (modified) {

                event.setCancelled(true);


                ItemStack newResult = result.clone();
                ItemMeta newMeta = newResult.getItemMeta();


                for (Enchantment enchant : newMeta.getEnchants().keySet()) {
                    newMeta.removeEnchant(enchant);
                }


                for (Map.Entry<Enchantment, Integer> entry : newEnchantments.entrySet()) {
                    newMeta.addEnchant(entry.getKey(), entry.getValue(), true);
                }

                newResult.setItemMeta(newMeta);


                anvil.setItem(2, newResult);

                player.sendMessage(ChatColor.YELLOW + "附魔等级已被限制为最高 " + getMaxEnchantLevel(player, null) + " 级");
            }
        }
    }


    private boolean checkAndFixItemEnchantments(ItemStack item, Player player) {
        if (item == null || item.getType() == org.bukkit.Material.AIR) {
            return false;
        }

        if (!item.hasItemMeta()) {
            return false;
        }

        ItemMeta meta = item.getItemMeta();
        if (!meta.hasEnchants()) {
            return false;
        }

        Map<Enchantment, Integer> enchantments = meta.getEnchants();
        boolean modified = false;
        Map<Enchantment, Integer> newEnchantments = new HashMap<>();

        for (Map.Entry<Enchantment, Integer> entry : enchantments.entrySet()) {
            Enchantment enchant = entry.getKey();
            int level = entry.getValue();
            int maxLevel = getMaxEnchantLevel(player, enchant);

            if (level > maxLevel) {
                newEnchantments.put(enchant, maxLevel);
                modified = true;
            } else {
                newEnchantments.put(enchant, level);
            }
        }

        if (modified) {

            for (Enchantment enchant : meta.getEnchants().keySet()) {
                meta.removeEnchant(enchant);
            }


            for (Map.Entry<Enchantment, Integer> entry : newEnchantments.entrySet()) {
                meta.addEnchant(entry.getKey(), entry.getValue(), true);
            }

            item.setItemMeta(meta);
            player.sendMessage(ChatColor.YELLOW + "物品附魔等级已被限制为最高 " + getMaxEnchantLevel(player, null) + " 级");
        }

        return modified;
    }


    @EventHandler
    public void onPlayerPickupItem(PlayerPickupItemEvent event) {
        if (!plugin.getConfig().getBoolean("enchantment-limits.enabled", true)) {
            return;
        }

        Player player = event.getPlayer();
        ItemStack item = event.getItem().getItemStack();

        if (checkAndFixItemEnchantments(item, player)) {

            event.getItem().setItemStack(item);
        }
    }


    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        if (!plugin.getConfig().getBoolean("enchantment-limits.enabled", true)) {
            return;
        }

        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }

        Player player = (Player) event.getWhoClicked();
        ItemStack item = event.getOldCursor();

        if (checkAndFixItemEnchantments(item, player)) {
            event.setCursor(item);
        }
    }


    @EventHandler
    public void onInventoryMoveItem(InventoryMoveItemEvent event) {
        if (!plugin.getConfig().getBoolean("enchantment-limits.enabled", true)) {
            return;
        }

        ItemStack item = event.getItem();


        if (item != null && item.hasItemMeta() && item.getItemMeta().hasEnchants()) {
            ItemMeta meta = item.getItemMeta();
            Map<Enchantment, Integer> enchantments = meta.getEnchants();
            boolean modified = false;
            Map<Enchantment, Integer> newEnchantments = new HashMap<>();

            for (Map.Entry<Enchantment, Integer> entry : enchantments.entrySet()) {
                Enchantment enchant = entry.getKey();
                int level = entry.getValue();
                int maxLevel = plugin.getConfig().getInt("enchantment-limits.default-max-level", 255);

                if (level > maxLevel) {
                    newEnchantments.put(enchant, maxLevel);
                    modified = true;
                } else {
                    newEnchantments.put(enchant, level);
                }
            }

            if (modified) {

                for (Enchantment enchant : meta.getEnchants().keySet()) {
                    meta.removeEnchant(enchant);
                }


                for (Map.Entry<Enchantment, Integer> entry : newEnchantments.entrySet()) {
                    meta.addEnchant(entry.getKey(), entry.getValue(), true);
                }

                item.setItemMeta(meta);
                event.setItem(item);
            }
        }
    }


    @EventHandler
    public void onInventoryOpen(InventoryOpenEvent event) {
        if (!plugin.getConfig().getBoolean("enchantment-limits.enabled", true)) {
            return;
        }

        if (!(event.getPlayer() instanceof Player)) {
            return;
        }

        Player player = (Player) event.getPlayer();


        for (ItemStack item : player.getInventory().getContents()) {
            checkAndFixItemEnchantments(item, player);
        }


        if (event.getInventory() != null) {
            for (ItemStack item : event.getInventory().getContents()) {
                checkAndFixItemEnchantments(item, player);
            }
        }
    }


    @EventHandler
    public void onPlayerCommand(PlayerCommandPreprocessEvent event) {
        if (!plugin.getConfig().getBoolean("enchantment-limits.enabled", true)) {
            return;
        }

        String message = event.getMessage().toLowerCase().trim();

        Player player = event.getPlayer();


         if (!message.startsWith("/give ") && !message.startsWith("minecraft:give ") &&
             !message.startsWith("/enchant ") && !message.startsWith("minecraft:enchant ")) {
             return;
         }


         if (message.startsWith("/enchant ") || message.startsWith("minecraft:enchant ")) {
             String[] enchantArgs = message.split(" ");
             if (enchantArgs.length >= 4) {
                 try {
                     int level = Integer.parseInt(enchantArgs[3]);
                     int maxLevel = getMaxEnchantLevel(player, null);
                     if (level > maxLevel) {
                         event.setCancelled(true);
                         player.sendMessage(ChatColor.RED + "附魔等级超过限制！最高允许 " + maxLevel + " 级");
                         return;
                     }
                 } catch (NumberFormatException e) {

                 }
             }
         }
        String[] args = message.split(" ");

        if (args.length < 3) {
            return;
        }


        String itemId = args[2];
        int amount = 1;

        if (args.length >= 4) {
            try {
                amount = Integer.parseInt(args[3]);
            } catch (NumberFormatException e) {
                amount = 1;
            }
        }


        if (args.length >= 5 && args[4].startsWith("{")) {

            plugin.getServer().getScheduler().runTaskLater(plugin, () -> {

                for (ItemStack item : player.getInventory().getContents()) {
                    if (item != null && !item.getType().equals(org.bukkit.Material.AIR)) {
                        if (checkAndFixItemEnchantments(item, player)) {

                            player.sendMessage(ChatColor.YELLOW + "检测到通过命令给予的高等级附魔物品，已自动修正为最高 " +
                                             getMaxEnchantLevel(player, null) + " 级");
                        }
                    }
                }
            }, 2L);
        } else {

            plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                for (ItemStack item : player.getInventory().getContents()) {
                    if (item != null && !item.getType().equals(org.bukkit.Material.AIR)) {
                        checkAndFixItemEnchantments(item, player);
                    }
                }
            }, 2L);
        }
    }
    private int getMaxEnchantLevel(Player player, Enchantment enchantment) {
        FileConfiguration config = plugin.getConfig();


        String playerName = player.getName();
        if (config.contains("enchantment-limits.player-limits." + playerName)) {
            return config.getInt("enchantment-limits.player-limits." + playerName, 255);
        }


        if (enchantment != null) {
            String enchantKey = enchantment.getKey().getKey().toUpperCase();
            if (config.contains("enchantment-limits.specific-enchantments." + enchantKey)) {
                return config.getInt("enchantment-limits.specific-enchantments." + enchantKey, 255);
            }
        }


        return config.getInt("enchantment-limits.default-max-level", 255);
    }


    public boolean setPlayerEnchantLimit(String playerName, int maxLevel) {
        if (maxLevel < 1 || maxLevel > 32767) {
            return false;
        }

        plugin.getConfig().set("enchantment-limits.player-limits." + playerName, maxLevel);
        plugin.saveConfig();
        loadConfig();
        return true;
    }


    public boolean removePlayerEnchantLimit(String playerName) {
        plugin.getConfig().set("enchantment-limits.player-limits." + playerName, null);
        plugin.saveConfig();
        loadConfig();
        return true;
    }


    public Set<String> getLimitedPlayers() {
        if (!plugin.getConfig().contains("enchantment-limits.player-limits")) {
            return new HashSet<>();
        }
        return plugin.getConfig().getConfigurationSection("enchantment-limits.player-limits").getKeys(false);
    }


    public int getPlayerEnchantLimit(String playerName) {
        return plugin.getConfig().getInt("enchantment-limits.player-limits." + playerName,
               plugin.getConfig().getInt("enchantment-limits.default-max-level", 255));
    }


    public boolean setAllPlayersEnchantLimit(int maxLevel) {
        if (maxLevel < 1 || maxLevel > 32767) {
            return false;
        }


        for (Player player : plugin.getServer().getOnlinePlayers()) {
            plugin.getConfig().set("enchantment-limits.player-limits." + player.getName(), maxLevel);
        }

        plugin.saveConfig();
        loadConfig();
        return true;
    }
}