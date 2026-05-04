package com.shootingstar.listener;

import com.shootingstar.ShootingStarPlugin;
import com.shootingstar.manager.StarEventManager;
import com.shootingstar.model.ActiveStar;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.player.PlayerInteractEvent;

/**
 * Prevents the loot chest from being broken or exploded while the event is active.
 * Also handles messaging when a player opens the chest.
 */
public class LootChestListener implements Listener {

    private final ShootingStarPlugin plugin;
    private final StarEventManager   eventManager;

    public LootChestListener(ShootingStarPlugin plugin, StarEventManager eventManager) {
        this.plugin       = plugin;
        this.eventManager = eventManager;
    }

    /** Block breaking the star chest during the event. */
    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        if (!isStarChest(event.getBlock().getLocation())) return;
        event.setCancelled(true);
        event.getPlayer().sendMessage(
            net.kyori.adventure.text.Component.text("This chest is protected by cosmic energy!")
                .color(net.kyori.adventure.text.format.NamedTextColor.GOLD)
        );
    }

    /** Block explosion (e.g. from a red-star mob) destroying the chest. */
    @EventHandler
    public void onBlockExplode(BlockExplodeEvent event) {
        event.blockList().removeIf(b -> isStarChest(b.getLocation()));
    }

    /** Play a special sound when a player opens the star chest. */
    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        if (event.getClickedBlock() == null) return;
        if (event.getClickedBlock().getType() != Material.CHEST) return;

        if (!isStarChest(event.getClickedBlock().getLocation())) return;

        ActiveStar star = eventManager.getActiveStar();
        if (star == null || star.getPhase() != ActiveStar.Phase.CHEST_OPEN) return;

        Player player = event.getPlayer();
        if (!player.hasPermission("shootingstar.loot")) {
            event.setCancelled(true);
            eventManager.getMessageUtil().send(player, "no-permission");
            return;
        }

        // Play a celebratory sound for the first time opening
        player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, SoundCategory.MASTER, 1f, 1f);
    }

    private boolean isStarChest(Location loc) {
        ActiveStar star = eventManager.getActiveStar();
        if (star == null) return false;
        Location impact = star.getTargetLocation();
        return impact.getWorld() != null
            && loc.getWorld() != null
            && impact.getWorld().equals(loc.getWorld())
            && impact.getBlockX() == loc.getBlockX()
            && impact.getBlockY() == loc.getBlockY()
            && impact.getBlockZ() == loc.getBlockZ();
    }
}
