package com.shootingstar.manager;

import com.shootingstar.ShootingStarPlugin;
import com.shootingstar.model.ActiveStar;
import com.shootingstar.model.StarVariant;
import com.shootingstar.util.LocationFinder;
import com.shootingstar.util.LootBuilder;
import com.shootingstar.util.MessageUtil;
import com.shootingstar.util.ParticleUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.*;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.*;
import org.bukkit.block.Chest;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;

public class StarEventManager {

    private final ShootingStarPlugin plugin;
    private final LootBuilder        lootBuilder;
    private final MessageUtil        messages;
    private final Random             random = new Random();

    private ActiveStar activeStar;

    private final Map<Location, Chest> spawnedChests    = new HashMap<>();
    private final Set<Location>        pvpZoneLocations = new HashSet<>();

    public StarEventManager(ShootingStarPlugin plugin) {
        this.plugin      = plugin;
        this.lootBuilder = new LootBuilder(plugin);
        this.messages    = new MessageUtil(plugin.getConfig());
    }

    // ── Public API ──────────────────────────────────────────

    public boolean isEventActive()          { return activeStar != null; }
    public ActiveStar getActiveStar()       { return activeStar; }
    public MessageUtil getMessageUtil()     { return messages; }

    public boolean isInPvpZone(Location loc) {
        if (activeStar == null || activeStar.getPhase() == ActiveStar.Phase.DONE) return false;
        if (!plugin.getConfig().getBoolean("impact-pvp-zone", true)) return false;
        double radius = plugin.getConfig().getDouble("pvp-radius", 24);
        Location impact = activeStar.getTargetLocation();
        return loc.getWorld() != null
            && loc.getWorld().equals(impact.getWorld())
            && loc.distanceSquared(impact) <= radius * radius;
    }

    /** Starts a random-variant event. */
    public boolean startEvent() {
        if (activeStar != null) return false;
        return startEventWithVariant(pickVariant());
    }

    /** Starts an event forcing a specific variant. */
    public boolean startEventWithVariant(StarVariant variant) {
        if (activeStar != null) return false;

        String worldName = plugin.getConfig().getString("world", "world");
        World world = Bukkit.getWorld(worldName);
        if (world == null) {
            plugin.getLogger().severe("World '" + worldName + "' not found!");
            return false;
        }

        int radius  = plugin.getConfig().getInt("spawn-radius", 5000);
        int minDist = plugin.getConfig().getInt("min-spawn-distance", 100);
        Location land = LocationFinder.findLandLocation(world, radius, minDist);
        if (land == null) {
            plugin.getLogger().warning("Could not find a valid land location after 30 attempts.");
            return false;
        }

        activeStar = new ActiveStar(variant, land);

        int earlyMinutes = plugin.getConfig().getInt("early-warning-minutes", 3);
        if (earlyMinutes > 0) {
            messages.broadcast("early-warning", "minutes", String.valueOf(earlyMinutes));
        }

        messages.broadcast("broadcast",
            "variant", variant.getDisplayName(),
            "x", String.valueOf(land.getBlockX()),
            "z", String.valueOf(land.getBlockZ())
        );

        beginFall(activeStar);
        return true;
    }

    /** Admin-triggered: spawn at a specific location with random variant. */
    public boolean startEventAt(Location loc) {
        if (activeStar != null) return false;
        StarVariant variant = pickVariant();
        loc.setY(loc.getWorld().getHighestBlockYAt(loc));
        activeStar = new ActiveStar(variant, loc);
        messages.broadcast("broadcast",
            "variant", variant.getDisplayName(),
            "x", String.valueOf(loc.getBlockX()),
            "z", String.valueOf(loc.getBlockZ())
        );
        beginFall(activeStar);
        return true;
    }

    /** Cancels and cleans up the current event. */
    public void cancelCurrentEvent() {
        if (activeStar == null) return;
        cancelTask(activeStar.getFallTaskId());
        cancelTask(activeStar.getUnlockTaskId());
        cancelTask(activeStar.getDespawnTaskId());
        if (activeStar.getMarker() != null && !activeStar.getMarker().isDead()) {
            activeStar.getMarker().remove();
        }
        pvpZoneLocations.clear();
        spawnedChests.clear();
        activeStar = null;
    }

    // ── Fall animation ──────────────────────────────────────

    private void beginFall(ActiveStar star) {
        int startY  = plugin.getConfig().getInt("fall-start-y", 350);
        int targetY = star.getTargetLocation().getBlockY();

        // Use exact duration in seconds so it's always consistent
        int fallDurationSeconds = plugin.getConfig().getInt("fall-duration-seconds", 180);
        long fallDurationTicks  = fallDurationSeconds * 20L;

        double startX = star.getTargetLocation().getX();
        double startZ = star.getTargetLocation().getZ();
        World  world  = star.getTargetLocation().getWorld();

        double driftX = (random.nextDouble() - 0.5) * 80;
        double driftZ = (random.nextDouble() - 0.5) * 80;

        final long[] tick = {0};

        BukkitRunnable fallTask = new BukkitRunnable() {
            @Override
            public void run() {
                if (activeStar == null || activeStar.getPhase() != ActiveStar.Phase.FALLING) {
                    cancel();
                    return;
                }

                tick[0]++;
                double t = Math.min((double) tick[0] / fallDurationTicks, 1.0);

                double curX = startX + driftX * (1 - t);
                double curY = startY  - (startY - targetY) * t;
                double curZ = startZ + driftZ * (1 - t);

                Location cur = new Location(world, curX, curY, curZ);
                star.setCurrentLocation(cur);

                ParticleUtil.spawnTrail(cur, star.getVariant());

                if (t >= 1.0) {
                    cancel();
                    onImpact(star);
                }
            }
        };

        int taskId = fallTask.runTaskTimer(plugin, 0L, 1L).getTaskId();
        star.setFallTaskId(taskId);
    }

    // ── Impact ──────────────────────────────────────────────

    private void onImpact(ActiveStar star) {
        star.setPhase(ActiveStar.Phase.LANDED);
        Location impact = star.getTargetLocation();
        World    world  = impact.getWorld();

        ParticleUtil.spawnImpact(impact, star.getVariant());
        world.playSound(impact, Sound.ENTITY_GENERIC_EXPLODE,        SoundCategory.AMBIENT, 4f, 0.6f);
        world.playSound(impact, Sound.BLOCK_BEACON_ACTIVATE,         SoundCategory.AMBIENT, 2f, 1.2f);
        world.playSound(impact, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, SoundCategory.AMBIENT, 3f, 0.8f);
        world.strikeLightningEffect(impact);

        scorchGround(impact, star.getVariant());

        if (star.getVariant() == StarVariant.RED) {
            spawnGuardMobs(impact);
        }

        messages.broadcast("impact",
            "x", String.valueOf(impact.getBlockX()),
            "z", String.valueOf(impact.getBlockZ())
        );

        if (plugin.getConfig().getBoolean("impact-pvp-zone", true)) {
            messages.broadcast("pvp-enabled");
        }

        int unlockDelay = plugin.getConfig().getInt("chest-unlock-delay", 15) * 20;
        BukkitRunnable unlockTask = new BukkitRunnable() {
            @Override
            public void run() {
                if (activeStar == null) return;
                openLootChest(star);
            }
        };
        star.setUnlockTaskId(unlockTask.runTaskLater(plugin, unlockDelay).getTaskId());

        BukkitRunnable glowTask = new BukkitRunnable() {
            @Override
            public void run() {
                if (activeStar == null || activeStar.getPhase() == ActiveStar.Phase.DONE) {
                    cancel();
                    return;
                }
                ParticleUtil.spawnChestAmbient(impact, star.getVariant());
            }
        };
        glowTask.runTaskTimer(plugin, 5L, 10L);
    }

    private void openLootChest(ActiveStar star) {
        star.setPhase(ActiveStar.Phase.CHEST_OPEN);
        Location impact   = star.getTargetLocation();
        World    world    = impact.getWorld();

        // Place chest 1 block above the impact point so it's not buried in scorched ground
        Location chestLoc = impact.clone().add(0, 1, 0);
        chestLoc.getBlock().setType(Material.CHEST);

        if (chestLoc.getBlock().getState() instanceof Chest chest) {
            List<ItemStack> loot = lootBuilder.buildLoot(star.getVariant());
            int[] slots = randomSlots(loot.size(), 27);
            for (int i = 0; i < loot.size() && i < slots.length; i++) {
                chest.getInventory().setItem(slots[i], loot.get(i));
            }
            spawnedChests.put(chestLoc, chest);
        }

        world.playSound(impact, Sound.BLOCK_CHEST_OPEN,         SoundCategory.BLOCKS,  2f, 1f);
        world.playSound(impact, Sound.BLOCK_BEACON_POWER_SELECT, SoundCategory.AMBIENT, 1.5f, 1.5f);

        messages.broadcast("chest-unlocked",
            "x", String.valueOf(impact.getBlockX()),
            "z", String.valueOf(impact.getBlockZ())
        );

        int despawnDelay = plugin.getConfig().getInt("chest-despawn-delay", 90) * 20;
        BukkitRunnable despawnTask = new BukkitRunnable() {
            @Override
            public void run() { despawnChest(star); }
        };
        star.setDespawnTaskId(despawnTask.runTaskLater(plugin, despawnDelay).getTaskId());
    }

    private void despawnChest(ActiveStar star) {
        star.setPhase(ActiveStar.Phase.DONE);
        Location impact   = star.getTargetLocation();
        World    world    = impact.getWorld();

        // Chest was placed 1 block above impact
        Location chestLoc = impact.clone().add(0, 1, 0);
        if (chestLoc.getBlock().getType() == Material.CHEST) {
            if (chestLoc.getBlock().getState() instanceof Chest chest) {
                for (ItemStack item : chest.getInventory().getContents()) {
                    if (item != null) world.dropItemNaturally(chestLoc, item);
                }
                chest.getInventory().clear();
            }
            chestLoc.getBlock().setType(Material.AIR);
        }

        spawnedChests.remove(chestLoc);
        pvpZoneLocations.clear();

        world.playSound(impact, Sound.ENTITY_ITEM_BREAK, SoundCategory.BLOCKS, 1f, 0.8f);
        messages.broadcast("chest-despawned",
            "x", String.valueOf(impact.getBlockX()),
            "z", String.valueOf(impact.getBlockZ())
        );

        activeStar = null;
    }

    // ── Helpers ─────────────────────────────────────────────

    private StarVariant pickVariant() {
        ConfigurationSection variants = plugin.getConfig().getConfigurationSection("variants");
        int totalWeight = 0;
        Map<StarVariant, Integer> weights = new EnumMap<>(StarVariant.class);

        for (StarVariant v : StarVariant.values()) {
            int w = 0;
            if (variants != null) {
                ConfigurationSection sec = variants.getConfigurationSection(v.name());
                if (sec != null) w = sec.getInt("weight", 0);
            }
            if (w <= 0) continue;
            weights.put(v, w);
            totalWeight += w;
        }

        if (totalWeight == 0) return StarVariant.NORMAL;

        int roll = random.nextInt(totalWeight);
        int cumulative = 0;
        for (Map.Entry<StarVariant, Integer> entry : weights.entrySet()) {
            cumulative += entry.getValue();
            if (roll < cumulative) return entry.getKey();
        }
        return StarVariant.NORMAL;
    }

    private void scorchGround(Location impact, StarVariant variant) {
        World world  = impact.getWorld();
        int   radius = 3;
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                if (dx * dx + dz * dz > radius * radius) continue;
                Location bl = impact.clone().add(dx, 0, dz);
                Material m  = bl.getBlock().getType();
                if (m == Material.GRASS_BLOCK || m == Material.DIRT) {
                    bl.getBlock().setType(Material.COARSE_DIRT);
                } else if (m == Material.STONE || m == Material.DEEPSLATE) {
                    if (random.nextInt(3) == 0) bl.getBlock().setType(Material.COBBLESTONE);
                }
            }
        }
    }

    private void spawnGuardMobs(Location impact) {
        World world = impact.getWorld();
        ConfigurationSection redConfig = plugin.getConfig().getConfigurationSection("loot-tables.red");
        if (redConfig == null) return;

        int count = redConfig.getInt("mob-count", 6);
        List<String> mobNames = redConfig.getStringList("mobs");
        if (mobNames.isEmpty()) mobNames = List.of("ZOMBIE", "SKELETON");

        for (int i = 0; i < count; i++) {
            String mobName = mobNames.get(random.nextInt(mobNames.size()));
            try {
                EntityType type  = EntityType.valueOf(mobName.toUpperCase());
                double angle     = random.nextDouble() * Math.PI * 2;
                double dist      = 3 + random.nextDouble() * 4;
                Location spawn   = impact.clone().add(Math.cos(angle) * dist, 1, Math.sin(angle) * dist);
                world.spawnEntity(spawn, type);
            } catch (IllegalArgumentException ignored) {
                plugin.getLogger().warning("Unknown mob in config: " + mobName);
            }
        }
    }

    private int[] randomSlots(int count, int max) {
        List<Integer> all = new ArrayList<>();
        for (int i = 0; i < max; i++) all.add(i);
        Collections.shuffle(all, random);
        int[] result = new int[Math.min(count, max)];
        for (int i = 0; i < result.length; i++) result[i] = all.get(i);
        return result;
    }

    private void cancelTask(int id) {
        if (id != -1) {
            try { plugin.getServer().getScheduler().cancelTask(id); } catch (Exception ignored) {}
        }
    }
}
