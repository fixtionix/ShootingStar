package com.shootingstar.util;

import com.shootingstar.ShootingStarPlugin;
import com.shootingstar.model.StarVariant;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * Reads loot-tables from config and produces a randomised list of ItemStacks.
 */
public class LootBuilder {

    private static final Random RANDOM = new Random();

    /** PDC key that marks this item as a Star Fragment (custom item). */
    public static final String STAR_FRAGMENT_KEY = "star_fragment";

    private final ShootingStarPlugin plugin;

    public LootBuilder(ShootingStarPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Builds the loot list for the given variant, honouring config chances and amounts.
     */
    public List<ItemStack> buildLoot(StarVariant variant) {
        String tableKey = plugin.getConfig()
                .getString("variants." + variant.name() + ".loot-table", "normal");
        ConfigurationSection table = plugin.getConfig()
                .getConfigurationSection("loot-tables." + tableKey);

        if (table == null) {
            plugin.getLogger().warning("Missing loot-table: " + tableKey);
            return Collections.emptyList();
        }

        int minItems = table.getInt("min-items", 2);
        int maxItems = table.getInt("max-items", 4);
        int targetCount = minItems + RANDOM.nextInt(Math.max(1, maxItems - minItems + 1));

        List<ConfigurationSection> pool = new ArrayList<>();
        List<?> items = table.getList("items");
        if (items != null) {
            for (Object entry : items) {
                if (entry instanceof ConfigurationSection cs) pool.add(cs);
            }
        }

        // Also support the YAML-list-of-maps style Bukkit deserialises
        ConfigurationSection itemsSec = table.getConfigurationSection("items");
        if (itemsSec != null) {
            for (String key : itemsSec.getKeys(false)) {
                ConfigurationSection cs = itemsSec.getConfigurationSection(key);
                if (cs != null) pool.add(cs);
            }
        }

        Collections.shuffle(pool);

        List<ItemStack> result = new ArrayList<>();
        for (ConfigurationSection entry : pool) {
            if (result.size() >= targetCount) break;
            double chance = entry.getDouble("chance", 1.0);
            if (RANDOM.nextDouble() > chance) continue;

            String matName = entry.getString("material", "");
            int amount     = entry.getInt("amount", 1);

            ItemStack stack = buildItem(matName, amount);
            if (stack != null) result.add(stack);
        }

        // Always guarantee at least one Star Fragment
        boolean hasFragment = result.stream().anyMatch(s -> isStarFragment(s));
        if (!hasFragment) result.add(createStarFragment(1));

        return result;
    }

    private ItemStack buildItem(String materialName, int amount) {
        // Custom item: star fragment
        if (materialName.equalsIgnoreCase("STAR_FRAGMENT")) {
            return createStarFragment(amount);
        }

        Material mat = Material.matchMaterial(materialName);
        if (mat == null) {
            plugin.getLogger().warning("Unknown material in loot table: " + materialName);
            return null;
        }

        ItemStack stack = new ItemStack(mat, amount);

        // Add a random enchantment to books
        if (mat == Material.ENCHANTED_BOOK) {
            applyRandomEnchantment(stack);
        }

        return stack;
    }

    private ItemStack createStarFragment(int amount) {
        ItemStack frag = new ItemStack(Material.NETHER_STAR, amount);
        ItemMeta meta  = frag.getItemMeta();
        if (meta != null) {
            meta.displayName(net.kyori.adventure.text.Component.text("★ Star Fragment")
                    .color(net.kyori.adventure.text.format.NamedTextColor.GOLD));
            meta.lore(List.of(
                net.kyori.adventure.text.Component.text("A fragment from a fallen star.")
                    .color(net.kyori.adventure.text.format.NamedTextColor.GRAY),
                net.kyori.adventure.text.Component.text("Use to craft a Star Compass.")
                    .color(net.kyori.adventure.text.format.NamedTextColor.DARK_GRAY)
            ));
            NamespacedKey key = new NamespacedKey(plugin, STAR_FRAGMENT_KEY);
            meta.getPersistentDataContainer().set(key, PersistentDataType.BYTE, (byte) 1);
            frag.setItemMeta(meta);
        }
        return frag;
    }

    public boolean isStarFragment(ItemStack stack) {
        if (stack == null || !stack.hasItemMeta()) return false;
        NamespacedKey key = new NamespacedKey(plugin, STAR_FRAGMENT_KEY);
        return stack.getItemMeta().getPersistentDataContainer()
                .has(key, PersistentDataType.BYTE);
    }

    private void applyRandomEnchantment(ItemStack book) {
        // Apply a random "safe" enchant to a book
        Enchantment[] enchants = {
            Enchantment.SHARPNESS, Enchantment.PROTECTION,
            Enchantment.EFFICIENCY, Enchantment.FORTUNE,
            Enchantment.SILK_TOUCH, Enchantment.MENDING,
            Enchantment.UNBREAKING, Enchantment.LOOTING
        };
        Enchantment chosen = enchants[RANDOM.nextInt(enchants.length)];
        int level = 1 + RANDOM.nextInt(chosen.getMaxLevel());
        book.addUnsafeEnchantment(chosen, level);
    }
}
