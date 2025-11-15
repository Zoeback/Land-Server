package com.landother.landotherplugin;

import org.bukkit.enchantments.Enchantment;
import java.util.HashMap;
import java.util.Map;


public class VanillaEnchantMaxLevel {

    private static final Map<Enchantment, Integer> VANILLA_MAX_LEVELS = new HashMap<>();

    static {

        VANILLA_MAX_LEVELS.put(Enchantment.PROTECTION_ENVIRONMENTAL, 4);
        VANILLA_MAX_LEVELS.put(Enchantment.PROTECTION_FIRE, 4);
        VANILLA_MAX_LEVELS.put(Enchantment.PROTECTION_FALL, 4);
        VANILLA_MAX_LEVELS.put(Enchantment.PROTECTION_EXPLOSIONS, 4);
        VANILLA_MAX_LEVELS.put(Enchantment.PROTECTION_PROJECTILE, 4);
        VANILLA_MAX_LEVELS.put(Enchantment.THORNS, 3);
        VANILLA_MAX_LEVELS.put(Enchantment.OXYGEN, 3);
        VANILLA_MAX_LEVELS.put(Enchantment.WATER_WORKER, 1);
        VANILLA_MAX_LEVELS.put(Enchantment.DEPTH_STRIDER, 3);
        VANILLA_MAX_LEVELS.put(Enchantment.FROST_WALKER, 2);


        VANILLA_MAX_LEVELS.put(Enchantment.DAMAGE_ALL, 5);
        VANILLA_MAX_LEVELS.put(Enchantment.DAMAGE_UNDEAD, 5);
        VANILLA_MAX_LEVELS.put(Enchantment.DAMAGE_ARTHROPODS, 5);
        VANILLA_MAX_LEVELS.put(Enchantment.KNOCKBACK, 2);
        VANILLA_MAX_LEVELS.put(Enchantment.FIRE_ASPECT, 2);
        VANILLA_MAX_LEVELS.put(Enchantment.LOOT_BONUS_MOBS, 3);
        VANILLA_MAX_LEVELS.put(Enchantment.SWEEPING_EDGE, 3);


        VANILLA_MAX_LEVELS.put(Enchantment.DIG_SPEED, 5);
        VANILLA_MAX_LEVELS.put(Enchantment.SILK_TOUCH, 1);
        VANILLA_MAX_LEVELS.put(Enchantment.DURABILITY, 3);
        VANILLA_MAX_LEVELS.put(Enchantment.LOOT_BONUS_BLOCKS, 3);
        VANILLA_MAX_LEVELS.put(Enchantment.LUCK, 3);
        VANILLA_MAX_LEVELS.put(Enchantment.LURE, 3);


        VANILLA_MAX_LEVELS.put(Enchantment.ARROW_DAMAGE, 5);
        VANILLA_MAX_LEVELS.put(Enchantment.ARROW_KNOCKBACK, 2);
        VANILLA_MAX_LEVELS.put(Enchantment.ARROW_FIRE, 1);
        VANILLA_MAX_LEVELS.put(Enchantment.ARROW_INFINITE, 1);


        VANILLA_MAX_LEVELS.put(Enchantment.IMPALING, 5);
        VANILLA_MAX_LEVELS.put(Enchantment.RIPTIDE, 3);
        VANILLA_MAX_LEVELS.put(Enchantment.LOYALTY, 3);
        VANILLA_MAX_LEVELS.put(Enchantment.CHANNELING, 1);


        VANILLA_MAX_LEVELS.put(Enchantment.MULTISHOT, 1);
        VANILLA_MAX_LEVELS.put(Enchantment.QUICK_CHARGE, 3);
        VANILLA_MAX_LEVELS.put(Enchantment.PIERCING, 4);


        VANILLA_MAX_LEVELS.put(Enchantment.MENDING, 1);
        VANILLA_MAX_LEVELS.put(Enchantment.BINDING_CURSE, 1);
        VANILLA_MAX_LEVELS.put(Enchantment.VANISHING_CURSE, 1);
        VANILLA_MAX_LEVELS.put(Enchantment.SOUL_SPEED, 3);
    }


    public static int getVanillaMaxLevel(Enchantment enchantment) {
        if (enchantment == null) {
            return 1;
        }
        return VANILLA_MAX_LEVELS.getOrDefault(enchantment, 1);
    }


    public static boolean exceedsVanillaMaxLevel(Enchantment enchantment, int level) {
        int vanillaMax = getVanillaMaxLevel(enchantment);
        return level > vanillaMax;
    }


    public static Map<Enchantment, Integer> getAllVanillaMaxLevels() {
        return new HashMap<>(VANILLA_MAX_LEVELS);
    }
}