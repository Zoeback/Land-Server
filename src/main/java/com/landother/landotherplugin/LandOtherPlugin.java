package com.landother.landotherplugin;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class LandOtherPlugin extends JavaPlugin implements Listener, TabCompleter {

    private ItemManager itemManager;
    private PlayerData playerData;
    private KitManager kitManager;
    private EnchantLimitListener enchantLimitListener;
    private FileConfiguration config;
    private File configFile;
    private Inventory publicInventory;
    private String inventoryTitle;
    private Map<UUID, Inventory> playerInventories = new HashMap<>();

    @Override
    public void onEnable() {
        saveDefaultConfig();
        loadConfig();

        itemManager = new ItemManager(this);
        playerData = new PlayerData(this);
        kitManager = new KitManager(this);
        enchantLimitListener = new EnchantLimitListener(this);


        createPublicInventory();


        getServer().getPluginManager().registerEvents(this, this);
        getServer().getPluginManager().registerEvents(enchantLimitListener, this);

        getLogger().info("LandOtherPlugin 已启用");
    }

    @Override
    public void onDisable() {
        if (playerData != null) {
            playerData.saveAllPlayerData();
        }
        getLogger().info("LandOtherPlugin 已禁用");
    }

    private void loadConfig() {
        configFile = new File(getDataFolder(), "config.yml");
        if (!configFile.exists()) {
            saveDefaultConfig();
        }
        config = YamlConfiguration.loadConfiguration(configFile);
    }

    private void createPublicInventory() {
        String title = ChatColor.translateAlternateColorCodes('&',
            config.getString("inventory.title", "&6&l公用物品领取"));
        int size = config.getInt("inventory.size", 27);

        this.inventoryTitle = title;
        publicInventory = Bukkit.createInventory(null, size, title);


        if (config.contains("items")) {
            List<Map<?, ?>> items = config.getMapList("items");
            for (int i = 0; i < items.size() && i < size; i++) {
                Map<?, ?> itemData = items.get(i);
                ItemStack item = createItemFromConfig(itemData);
                if (item != null) {
                    publicInventory.setItem(i, item);
                }
            }
        }
    }

    private ItemStack createItemFromConfig(Map<?, ?> itemData) {
        try {

            String materialName = null;
            if (itemData.containsKey("type")) {
                materialName = (String) itemData.get("type");
            } else if (itemData.containsKey("material")) {
                materialName = (String) itemData.get("material");
            }

            if (materialName == null) {
                getLogger().warning("物品配置缺少类型信息: " + itemData);
                return null;
            }

            Material material = Material.valueOf(materialName.toUpperCase());
            int amount = itemData.containsKey("amount") ? (Integer) itemData.get("amount") : 1;
            String name = null;


            if (itemData.containsKey("display-name")) {
                name = (String) itemData.get("display-name");
            } else if (itemData.containsKey("name")) {
                name = (String) itemData.get("name");
            }

            ItemStack item = new ItemStack(material, amount);
            ItemMeta meta = item.getItemMeta();

            if (name != null) {
                meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', name));
            }


            if (itemData.containsKey("meta")) {
                Map<?, ?> metaData = (Map<?, ?>) itemData.get("meta");
                loadItemMeta(meta, metaData);
            }

            item.setItemMeta(meta);
            return item;
        } catch (Exception e) {
            getLogger().warning("无法创建物品: " + e.getMessage());
            return null;
        }
    }

    private void loadItemMeta(ItemMeta meta, Map<?, ?> metaData) {
        try {

            if (metaData.containsKey("display-name")) {
                String displayName = (String) metaData.get("display-name");
                meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', displayName));
            }


            if (metaData.containsKey("lore")) {
                List<String> lore = (List<String>) metaData.get("lore");
                meta.setLore(lore);
            }


            if (metaData.containsKey("enchants")) {
                List<Map<String, Object>> enchants = (List<Map<String, Object>>) metaData.get("enchants");
                for (Map<String, Object> enchantData : enchants) {
                    String enchantName = (String) enchantData.get("name");
                    int level = (Integer) enchantData.get("level");
                    Enchantment enchantment = Enchantment.getByKey(NamespacedKey.minecraft(enchantName.toLowerCase()));
                    if (enchantment != null) {
                        meta.addEnchant(enchantment, level, true);
                    }
                }
            }


            if (metaData.containsKey("custom-model-data")) {
                int customModelData = (Integer) metaData.get("custom-model-data");
                meta.setCustomModelData(customModelData);
            }


            if (metaData.containsKey("flags")) {
                List<String> flags = (List<String>) metaData.get("flags");
                for (String flagName : flags) {
                    try {
                        ItemFlag flag = ItemFlag.valueOf(flagName);
                        meta.addItemFlags(flag);
                    } catch (IllegalArgumentException e) {
                        getLogger().warning("无效的物品标志: " + flagName);
                    }
                }
            }


            if (metaData.containsKey("unbreakable")) {
                boolean unbreakable = (Boolean) metaData.get("unbreakable");
                meta.setUnbreakable(unbreakable);
            }
        } catch (Exception e) {
            getLogger().warning("加载物品元数据失败: " + e.getMessage());
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "只有玩家才能使用这个命令");
            return true;
        }

        Player player = (Player) sender;


        if (command.getName().equalsIgnoreCase("publicitems") || command.getName().equalsIgnoreCase("pi")) {

            if (!player.hasPermission("landother.publicitems")) {
                player.sendMessage(ChatColor.RED + "你没有权限使用这个命令");
                return true;
            }

            openPublicInventory(player);
            return true;
        }


        if (command.getName().equalsIgnoreCase("kit")) {
            if (args.length == 0) {
                player.sendMessage(ChatColor.RED + "用法: /kit <name>");
                return true;
            }

            String kitName = args[0];
            if (kitManager != null) {
                kitManager.openKit(player, kitName);
            } else {
                player.sendMessage(ChatColor.RED + "Kit系统未启用");
            }
            return true;
        }

        if (command.getName().equalsIgnoreCase("landother") || command.getName().equalsIgnoreCase("lo")) {

            if (!player.hasPermission("landother.admin")) {
                player.sendMessage(ChatColor.RED + "你没有权限使用这个命令");
                return true;
            }


            if (args.length == 0) {
                player.sendMessage(ChatColor.GOLD + "=== LandOtherPlugin 管理命令 ===");
                player.sendMessage(ChatColor.YELLOW + "/landother help - 显示此帮助信息");
                player.sendMessage(ChatColor.YELLOW + "/landother reload - 重载配置");
                player.sendMessage(ChatColor.YELLOW + "/landother reload kit - 重载kit配置");
                player.sendMessage(ChatColor.YELLOW + "/landother stats - 查看统计信息");
                player.sendMessage(ChatColor.YELLOW + "/landother reset <玩家> - 重置玩家领取状态");
                player.sendMessage(ChatColor.YELLOW + "/landother reset kit <玩家> [kit名称] - 重置玩家kit状态");
                player.sendMessage(ChatColor.YELLOW + "/landother additem - 添加手上的物品到公用列表");
                player.sendMessage(ChatColor.YELLOW + "/landother createkit <name> - 创建新的kit");
                player.sendMessage(ChatColor.YELLOW + "/landother deletekit <name> - 删除指定的kit");
                player.sendMessage(ChatColor.YELLOW + "/landother listkits - 列出所有可用的kit");
                player.sendMessage(ChatColor.YELLOW + "/landother enchantlimit <玩家/all> [等级/vanillaMax/reset] - 设置玩家附魔等级限制");
                player.sendMessage(ChatColor.GOLD + "=== 玩家命令 ===");
                player.sendMessage(ChatColor.YELLOW + "/publicitems 或 /pi - 打开公用物品领取界面");
                player.sendMessage(ChatColor.YELLOW + "/kit - 打开kit领取界面");
                player.sendMessage(ChatColor.YELLOW + "/landother 或 /lo - 打开管理界面（需要权限）");
                return true;
            }

            if (args[0].equalsIgnoreCase("reload")) {
                if (args.length >= 2 && args[1].equalsIgnoreCase("kit")) {

                    if (kitManager != null) {
                        kitManager.reloadKits();
                        player.sendMessage(ChatColor.GREEN + "Kit配置已重载");
                    } else {
                        player.sendMessage(ChatColor.RED + "Kit管理器未初始化");
                    }
                } else {

                    reloadConfig();
                    loadConfig();
                    createPublicInventory();
                    if (kitManager != null) {
                        kitManager.reloadKits();
                    }
                    player.sendMessage(ChatColor.GREEN + "配置已重载");
                }
                return true;
            }

            if (args[0].equalsIgnoreCase("stats")) {
                int claimedPlayers = playerData.getClaimedPlayerCount();
                int claimedItems = itemManager.getClaimedItemCount();
                player.sendMessage(ChatColor.GOLD + "=== 统计信息 ===");
                player.sendMessage(ChatColor.YELLOW + "已领取玩家数: " + ChatColor.GREEN + claimedPlayers);
                player.sendMessage(ChatColor.YELLOW + "已领取物品数: " + ChatColor.GREEN + claimedItems);
                return true;
            }

            if (args[0].equalsIgnoreCase("reset") && args.length >= 2) {
                String target = args[1];


                if ("kit".equalsIgnoreCase(target) && args.length >= 3) {

                    String playerName = args[2];
                    Player targetPlayer = getServer().getPlayer(playerName);

                    if (targetPlayer == null) {
                        player.sendMessage(ChatColor.RED + "玩家 " + playerName + " 不在线");
                        return true;
                    }

                    if (args.length >= 4) {

                        String kitName = args[3];
                        if (kitManager != null && kitManager.hasKit(kitName)) {
                            boolean success = kitManager.resetPlayerKitClaim(kitName, targetPlayer.getUniqueId());
                            if (success) {
                                player.sendMessage(ChatColor.GREEN + "已重置玩家 " + playerName + " 的 kit: " + kitName);
                                targetPlayer.sendMessage(ChatColor.GREEN + "管理员已重置你的 kit: " + kitName);
                            } else {
                                player.sendMessage(ChatColor.YELLOW + "玩家 " + playerName + " 没有领取过 kit: " + kitName);
                            }
                        } else {
                            player.sendMessage(ChatColor.RED + "Kit '" + kitName + "' 不存在");
                        }
                    } else {

                        if (kitManager != null) {
                            int resetCount = kitManager.resetPlayerAllKitClaims(targetPlayer.getUniqueId());
                            if (resetCount > 0) {
                                player.sendMessage(ChatColor.GREEN + "已重置玩家 " + playerName + " 的所有 kit (" + resetCount + " 个)");
                                targetPlayer.sendMessage(ChatColor.GREEN + "管理员已重置你的所有 kit");
                            } else {
                                player.sendMessage(ChatColor.YELLOW + "玩家 " + playerName + " 没有领取过任何 kit");
                            }
                        } else {
                            player.sendMessage(ChatColor.RED + "Kit管理器未初始化");
                        }
                    }
                    return true;
                }


                String playerName = target;
                Player targetPlayer = getServer().getPlayer(playerName);
                if (targetPlayer == null) {
                    player.sendMessage(ChatColor.RED + "玩家 " + playerName + " 不在线");
                    return true;
                }

                boolean success = playerData.resetPlayerClaimStatus(targetPlayer.getUniqueId());
                if (success) {
                    player.sendMessage(ChatColor.GREEN + "已重置玩家 " + playerName + " 的领取状态");
                    targetPlayer.sendMessage(ChatColor.GREEN + "你的领取状态已被管理员重置");
                } else {
                    player.sendMessage(ChatColor.YELLOW + "玩家 " + playerName + " 没有领取记录");
                }
                return true;
            }

            if (args[0].equalsIgnoreCase("additem")) {

                ItemStack itemInHand = player.getInventory().getItemInMainHand();

                if (itemInHand == null || itemInHand.getType() == Material.AIR) {
                    player.sendMessage(ChatColor.RED + "你必须手持一个物品才能添加");
                    return true;
                }


                List<Map<?, ?>> itemsList = getConfig().getMapList("items");
                if (itemsList == null) {
                    itemsList = new ArrayList<>();
                }


                Map<String, Object> newItem = new HashMap<>();
                newItem.put("type", itemInHand.getType().name());
                newItem.put("amount", itemInHand.getAmount());


                if (itemInHand.hasItemMeta()) {
                    Map<String, Object> metaData = new HashMap<>();
                    ItemMeta meta = itemInHand.getItemMeta();

                    if (meta.hasDisplayName()) {
                        metaData.put("display-name", meta.getDisplayName());
                    }

                    if (meta.hasLore()) {
                        metaData.put("lore", new ArrayList<>(meta.getLore()));
                    }

                    if (meta.hasEnchants()) {
                        List<Map<String, Object>> enchants = new ArrayList<>();
                        for (Map.Entry<Enchantment, Integer> enchant : meta.getEnchants().entrySet()) {
                            Map<String, Object> enchantData = new HashMap<>();
                            enchantData.put("name", enchant.getKey().getKey().getKey());
                            enchantData.put("level", enchant.getValue());
                            enchants.add(enchantData);
                        }
                        metaData.put("enchants", enchants);
                    }


                    if (meta.hasCustomModelData()) {
                        metaData.put("custom-model-data", meta.getCustomModelData());
                    }


                    if (!meta.getItemFlags().isEmpty()) {
                        List<String> flags = new ArrayList<>();
                        for (ItemFlag flag : meta.getItemFlags()) {
                            flags.add(flag.name());
                        }
                        metaData.put("flags", flags);
                    }


                    if (meta.isUnbreakable()) {
                        metaData.put("unbreakable", true);
                    }

                    newItem.put("meta", metaData);
                }


                itemsList.add(newItem);


                getConfig().set("items", itemsList);
                saveConfig();


                createPublicInventory();

                player.sendMessage(ChatColor.GREEN + "成功将手上的物品添加到公用物品列表");
                player.sendMessage(ChatColor.YELLOW + "物品: " + itemInHand.getType().name() + " x" + itemInHand.getAmount());

                if (itemInHand.hasItemMeta()) {
                    player.sendMessage(ChatColor.GRAY + "已保存物品的元数据");
                }

                return true;
            }

            if (args[0].equalsIgnoreCase("createkit") && args.length >= 2) {
                String kitName = args[1].toLowerCase();

                if (kitManager != null) {
                    boolean success = kitManager.createKit(kitName, player);
                    if (success) {
                        player.sendMessage(ChatColor.GREEN + "成功创建kit: " + kitName);
                        player.sendMessage(ChatColor.YELLOW + "配置文件已生成在: plugins/LandOtherPlugin/kit/" + kitName + ".yml");
                        player.sendMessage(ChatColor.GRAY + "编辑配置文件后使用 /lo reload kit 来重载");
                    } else {
                        player.sendMessage(ChatColor.RED + "创建kit失败，可能是已存在或权限不足");
                    }
                } else {
                    player.sendMessage(ChatColor.RED + "Kit管理器未初始化");
                }
                return true;
            }

            if (args[0].equalsIgnoreCase("deletekit") && args.length >= 2) {
                String kitName = args[1].toLowerCase();

                if (kitManager != null) {
                    File kitFile = new File(getDataFolder(), "kit/" + kitName + ".yml");
                    if (kitFile.exists() && kitFile.delete()) {
                        kitManager.reloadKits();
                        player.sendMessage(ChatColor.GREEN + "成功删除kit: " + kitName);
                    } else {
                        player.sendMessage(ChatColor.RED + "删除kit失败，可能是kit不存在或文件错误");
                    }
                } else {
                    player.sendMessage(ChatColor.RED + "Kit管理器未初始化");
                }
                return true;
            }

            if (args[0].equalsIgnoreCase("listkits")) {
                if (kitManager != null) {
                    Set<String> kitNames = kitManager.getKitNames();
                    if (kitNames.isEmpty()) {
                        player.sendMessage(ChatColor.YELLOW + "当前没有任何kit");
                    } else {
                        player.sendMessage(ChatColor.GOLD + "=== 可用Kit列表 ===");
                        for (String kitName : kitNames) {
                            player.sendMessage(ChatColor.YELLOW + "- " + kitName);
                        }
                        player.sendMessage(ChatColor.GRAY + "共 " + kitNames.size() + " 个kit");
                    }
                } else {
                    player.sendMessage(ChatColor.RED + "Kit管理器未初始化");
                }
                return true;
            }

            if (args[0].equalsIgnoreCase("enchantlimit")) {

                if (!sender.hasPermission("landother.enchantlimit")) {
                    sender.sendMessage("§c你没有权限使用这个命令");
                    return true;
                }

                if (enchantLimitListener == null) {
                    sender.sendMessage("§c附魔限制功能未启用");
                    return true;
                }

                if (args.length < 2) {
                    sender.sendMessage("§c用法: /landother enchantlimit <玩家/all> [等级/vanillaMax/reset]");
                    return true;
                }

                String target = args[1];


                if (args.length >= 3 && args[2].equalsIgnoreCase("vanillaMax")) {
                    getConfig().set("enchantment-limits.limit-to-vanilla-max-level", true);
                    saveConfig();
                    enchantLimitListener.loadConfig();

                    if (target.equalsIgnoreCase("all")) {
                        sender.sendMessage("§a已为§e所有玩家§a启用§e限制到原版最高附魔等级§a功能");
                        for (Player p : getServer().getOnlinePlayers()) {
                            p.sendMessage("§e[系统] §a附魔等级限制已更新为§e原版最高等级");
                        }
                    } else {
                        sender.sendMessage("§a已为玩家§e" + target + "§a启用§e限制到原版最高附魔等级§a功能");
                        Player targetPlayer = getServer().getPlayer(target);
                        if (targetPlayer != null && targetPlayer.isOnline()) {
                            targetPlayer.sendMessage("§e[系统] §a你的附魔等级限制已更新为§e原版最高等级");
                        }
                    }
                    return true;
                }


                if (args.length >= 3 && args[2].equalsIgnoreCase("reset")) {
                    getConfig().set("enchantment-limits.limit-to-vanilla-max-level", false);
                    getConfig().set("enchantment-limits.player-limits", new HashMap<>());
                    saveConfig();
                    enchantLimitListener.loadConfig();

                    if (target.equalsIgnoreCase("all")) {
                        sender.sendMessage("§a已为§e所有玩家§a§e重置§a附魔等级限制功能");
                        sender.sendMessage("§a已关闭限制到原版最高附魔等级功能");
                        sender.sendMessage("§a已清空所有玩家的个人附魔等级限制");
                        for (Player p : getServer().getOnlinePlayers()) {
                            p.sendMessage("§e[系统] §a附魔等级限制已§e重置§a，恢复为默认设置");
                        }
                    } else {
                        sender.sendMessage("§a已为玩家§e" + target + "§a§e重置§a附魔等级限制功能");
                        sender.sendMessage("§a已关闭限制到原版最高附魔等级功能");
                        sender.sendMessage("§a已清空该玩家的个人附魔等级限制");
                        Player targetPlayer = getServer().getPlayer(target);
                        if (targetPlayer != null && targetPlayer.isOnline()) {
                            targetPlayer.sendMessage("§e[系统] §a你的附魔等级限制已§e重置§a，恢复为默认设置");
                        }
                    }
                    return true;
                }

                int maxLevel = 255;

                if (args.length >= 3) {
                    try {
                        maxLevel = Integer.parseInt(args[2]);
                        if (maxLevel < 1 || maxLevel > 32767) {
                            sender.sendMessage("§c等级必须在1-32767之间");
                            return true;
                        }
                    } catch (NumberFormatException e) {
                        sender.sendMessage("§c无效的等级数字");
                        return true;
                    }
                }

                if (target.equalsIgnoreCase("all")) {

                    int count = 0;
                    for (Player p : getServer().getOnlinePlayers()) {
                        enchantLimitListener.setPlayerEnchantLimit(p.getName(), maxLevel);
                        count++;
                    }
                    sender.sendMessage("§a已为 §e" + count + " §a名玩家设置附魔等级限制为 §e" + maxLevel);


                    for (Player p : getServer().getOnlinePlayers()) {
                        p.sendMessage("§e[系统] §a你的附魔等级限制已被设置为 §e" + maxLevel);
                    }
                } else {

                    enchantLimitListener.setPlayerEnchantLimit(target, maxLevel);
                    sender.sendMessage("§a已为玩家 §e" + target + " §a设置附魔等级限制为 §e" + maxLevel);


                    Player targetPlayer = getServer().getPlayer(target);
                    if (targetPlayer != null && targetPlayer.isOnline()) {
                        targetPlayer.sendMessage("§e[系统] §a你的附魔等级限制已被设置为 §e" + maxLevel);
                    }
                }
                return true;
            }
        }

        return false;
    }

    private void openPublicInventory(Player player) {

        if (playerData.hasPlayerClaimedItem(player.getUniqueId())) {
            player.sendMessage(ChatColor.RED + "你已经领取过物品了，每人只能领取一次");
            return;
        }


        Inventory personalInventory = Bukkit.createInventory(null, publicInventory.getSize(),
            inventoryTitle);


        for (int i = 0; i < publicInventory.getSize(); i++) {
            ItemStack item = publicInventory.getItem(i);
            if (item != null && !itemManager.isItemClaimed(item)) {
                personalInventory.setItem(i, item.clone());
            }
        }

        playerInventories.put(player.getUniqueId(), personalInventory);
        player.openInventory(personalInventory);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;

        Player player = (Player) event.getWhoClicked();
        Inventory inventory = event.getInventory();


        if (kitManager != null && kitManager.isKitInventory(player.getUniqueId())) {
            event.setCancelled(true);

            if (event.getCurrentItem() == null || event.getCurrentItem().getType() == Material.AIR) return;

            boolean handled = kitManager.handleKitClick(player, event.getSlot());
            if (handled) {
                return;
            }
        }


        if (!playerInventories.containsValue(inventory)) return;

        event.setCancelled(true);

        if (event.getCurrentItem() == null || event.getCurrentItem().getType() == Material.AIR) return;


        if (playerData.hasPlayerClaimedItem(player.getUniqueId())) {
            player.sendMessage(ChatColor.RED + "你已经领取过物品了");
            player.closeInventory();
            return;
        }

        ItemStack clickedItem = event.getCurrentItem();


        if (itemManager.isItemClaimed(clickedItem)) {
            player.sendMessage(ChatColor.RED + "这个物品已经被其他玩家领取了");
            player.closeInventory();
            return;
        }


        if (itemManager.claimItem(clickedItem, player.getUniqueId())) {
            playerData.setPlayerClaimedItem(player.getUniqueId(), clickedItem);
            player.getInventory().addItem(clickedItem.clone());
            player.sendMessage(ChatColor.GREEN + "成功领取物品: " +
                (clickedItem.hasItemMeta() && clickedItem.getItemMeta().hasDisplayName() ?
                clickedItem.getItemMeta().getDisplayName() : clickedItem.getType().name()));
            player.closeInventory();


            updateAllInventories();
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (event.getPlayer() instanceof Player) {
            Player player = (Player) event.getPlayer();


            playerInventories.remove(player.getUniqueId());


            if (kitManager != null) {
                kitManager.removePlayerKitData(player.getUniqueId());
            }
        }
    }

    private void updateAllInventories() {
        for (Map.Entry<UUID, Inventory> entry : playerInventories.entrySet()) {
            Player player = Bukkit.getPlayer(entry.getKey());
            if (player != null && player.isOnline()) {
                Inventory inventory = entry.getValue();


                for (int i = 0; i < publicInventory.getSize(); i++) {
                    ItemStack item = publicInventory.getItem(i);
                    if (item != null && !itemManager.isItemClaimed(item)) {
                        inventory.setItem(i, item.clone());
                    } else {
                        inventory.setItem(i, null);
                    }
                }
            }
        }
    }

    public ItemManager getItemManager() {
        return itemManager;
    }

    public PlayerData getPlayerData() {
        return playerData;
    }

    public EnchantLimitListener getEnchantLimitListener() {
        return enchantLimitListener;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();

        if (!(sender instanceof Player)) {
            return completions;
        }

        Player player = (Player) sender;


        if (!player.hasPermission("landother.admin")) {
            return completions;
        }

        if (args.length == 1) {

            String[] commands = {"help", "reload", "reset", "stats", "additem", "createkit", "deletekit", "listkits", "enchantlimit"};
            for (String cmd : commands) {
                if (cmd.startsWith(args[0].toLowerCase())) {
                    completions.add(cmd);
                }
            }
        } else if (args.length == 2) {

            if (args[0].equalsIgnoreCase("reset")) {

                completions.add("kit");

                for (Player onlinePlayer : getServer().getOnlinePlayers()) {
                    if (onlinePlayer.getName().toLowerCase().startsWith(args[1].toLowerCase())) {
                        completions.add(onlinePlayer.getName());
                    }
                }
            } else if (args[0].equalsIgnoreCase("deletekit")) {

                if (kitManager != null) {
                    for (String kitName : kitManager.getKitNames()) {
                        if (kitName.toLowerCase().startsWith(args[1].toLowerCase())) {
                            completions.add(kitName);
                        }
                    }
                }
                if ("<name>".startsWith(args[1].toLowerCase())) {
                    completions.add("<name>");
                }
            } else if (args[0].equalsIgnoreCase("createkit")) {

                if ("<name>".startsWith(args[1].toLowerCase())) {
                    completions.add("<name>");
                }
            } else if (args[0].equalsIgnoreCase("enchantlimit")) {

                completions.add("all");

                for (Player onlinePlayer : getServer().getOnlinePlayers()) {
                    if (onlinePlayer.getName().toLowerCase().startsWith(args[1].toLowerCase())) {
                        completions.add(onlinePlayer.getName());
                    }
                }
            }
        } else if (args.length == 3) {

            if (args[0].equalsIgnoreCase("reset") && args[1].equalsIgnoreCase("kit")) {

                for (Player onlinePlayer : getServer().getOnlinePlayers()) {
                    if (onlinePlayer.getName().toLowerCase().startsWith(args[2].toLowerCase())) {
                        completions.add(onlinePlayer.getName());
                    }
                }
            } else if (args[0].equalsIgnoreCase("enchantlimit") &&
                      !args[1].equalsIgnoreCase("vanillaMax") && !args[1].equalsIgnoreCase("reset")) {

                completions.add("vanillaMax");
                completions.add("reset");
                String[] levels = {"1", "5", "10", "50", "100", "255", "1000", "10000"};
                for (String level : levels) {
                    if (level.startsWith(args[2])) {
                        completions.add(level);
                    }
                }
            }
        } else if (args.length == 4) {

            if (args[0].equalsIgnoreCase("reset") && args[1].equalsIgnoreCase("kit")) {

                if (kitManager != null) {
                    for (String kitName : kitManager.getKitNames()) {
                        if (kitName.toLowerCase().startsWith(args[3].toLowerCase())) {
                            completions.add(kitName);
                        }
                    }
                }
            }
        }

        return completions;
    }
}