package com.shootingstar.listener;

import com.shootingstar.ShootingStarPlugin;
import com.shootingstar.manager.StarEventManager;
import com.shootingstar.model.ActiveStar;
import com.shootingstar.util.LootBuilder;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.CompassMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.List;

/**
 * Handles the Star Compass: a craftable compass that points to the active star's impact site.
 *
 * Craft recipe (shapeless):
 *   1× Star Fragment + 1× Compass → Star Compass
 *
 * The recipe is registered in ShootingStarPlugin via registerCompassRecipe().
 */
public class CompassListener implements Listener {

    private static final String COMPASS_KEY = "star_compass";

    private final ShootingStarPlugin plugin;
    private final StarEventManager   eventManager;

    public CompassListener(ShootingStarPlugin plugin, StarEventManager eventManager) {
        this.plugin       = plugin;
        this.eventManager = eventManager;
        registerCompassRecipe();
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        if (event.getAction() != Action.RIGHT_CLICK_AIR
         && event.getAction() != Action.RIGHT_CLICK_BLOCK) return;

        ItemStack held = player.getInventory().getItemInMainHand();
        if (!isStarCompass(held)) return;

        if (!player.hasPermission("shootingstar.compass")) {
            eventManager.getMessageUtil().send(player, "no-permission");
            return;
        }

        ActiveStar star = eventManager.getActiveStar();
        if (star == null) {
            eventManager.getMessageUtil().send(player, "no-active-star");
            return;
        }

        Location target = star.getPhase() == com.shootingstar.model.ActiveStar.Phase.FALLING
                ? star.getTargetLocation()
                : star.getTargetLocation();

        // Point the compass at the target
        if (held.getItemMeta() instanceof CompassMeta meta) {
            meta.setLodestone(target);
            meta.setLodestoneTracked(false);
            held.setItemMeta(meta);
        }

        int distance = (int) player.getLocation().distance(target);
        eventManager.getMessageUtil().send(player, "compass-pointing",
            "x", String.valueOf(target.getBlockX()),
            "z", String.valueOf(target.getBlockZ()),
            "distance", String.valueOf(distance)
        );

        event.setCancelled(true); // prevent block interact passthrough
    }

    // ─────────────────────────────────────────────────────────
    // Recipe registration
    // ─────────────────────────────────────────────────────────

    private void registerCompassRecipe() {
        NamespacedKey key = new NamespacedKey(plugin, "star_compass_recipe");

        // Build the Star Compass item
        ItemStack compass = new ItemStack(Material.COMPASS);
        CompassMeta meta  = (CompassMeta) compass.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text("★ Star Compass").color(NamedTextColor.AQUA));
            meta.lore(List.of(
                Component.text("Points to the nearest fallen star.")
                    .color(NamedTextColor.GRAY),
                Component.text("Right-click to update direction.")
                    .color(NamedTextColor.DARK_GRAY)
            ));
            meta.getPersistentDataContainer().set(
                new NamespacedKey(plugin, COMPASS_KEY), PersistentDataType.BYTE, (byte) 1);
            compass.setItemMeta(meta);
        }

        // Shapeless: 1 Star Fragment + 1 Compass
        org.bukkit.inventory.ShapelessRecipe recipe =
            new org.bukkit.inventory.ShapelessRecipe(key, compass);
        recipe.addIngredient(Material.COMPASS);
        // We use NETHER_STAR as the material for Star Fragment (with PDC check)
        recipe.addIngredient(Material.NETHER_STAR);

        try {
            plugin.getServer().addRecipe(recipe);
        } catch (Exception e) {
            plugin.getLogger().warning("Could not register Star Compass recipe: " + e.getMessage());
        }
    }

    public boolean isStarCompass(ItemStack item) {
        if (item == null || item.getType() != Material.COMPASS || !item.hasItemMeta()) return false;
        NamespacedKey key = new NamespacedKey(plugin, COMPASS_KEY);
        return item.getItemMeta().getPersistentDataContainer().has(key, PersistentDataType.BYTE);
    }
}
