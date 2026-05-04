package com.shootingstar.listener;

import com.shootingstar.ShootingStarPlugin;
import com.shootingstar.manager.StarEventManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

/**
 * Enforces or allows PvP within the impact zone radius based on config.
 */
public class PvpZoneListener implements Listener {

    private final ShootingStarPlugin plugin;
    private final StarEventManager   eventManager;

    public PvpZoneListener(ShootingStarPlugin plugin, StarEventManager eventManager) {
        this.plugin       = plugin;
        this.eventManager = eventManager;
    }

    @EventHandler
    public void onEntityDamage(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player attacker)) return;
        if (!(event.getEntity()  instanceof Player victim))   return;

        boolean inZone = eventManager.isInPvpZone(attacker.getLocation())
                      || eventManager.isInPvpZone(victim.getLocation());

        if (!inZone) return;

        // PvP is explicitly enabled in this zone — allow it even on PvP-off servers
        // (We do nothing here; just don't cancel. On servers where PvP is globally off,
        //  this event fires after the server's own check, so we ensure it proceeds.)
        event.setCancelled(false);

        // Notify attacker once they enter PvP zone (avoid spamming)
        if (!attacker.hasMetadata("ss_pvp_notified")) {
            attacker.sendMessage(Component.text("⚔ You are in the PvP impact zone!")
                    .color(NamedTextColor.RED));
            attacker.setMetadata("ss_pvp_notified",
                    new org.bukkit.metadata.FixedMetadataValue(plugin, true));
        }
    }
}
