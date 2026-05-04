package com.shootingstar.util;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;

import java.util.Random;

/**
 * Picks a suitable ground location for the star to land on.
 * Avoids ocean, lava lakes, and out-of-border positions.
 */
public class LocationFinder {

    private static final int MAX_ATTEMPTS = 30;
    private static final Random RANDOM    = new Random();

    private LocationFinder() {}

    /**
     * Tries up to MAX_ATTEMPTS times to find a non-ocean, non-lava,
     * non-void ground block within the specified radius.
     *
     * @param world       target world
     * @param radius      half-side of the square spawn area from 0,0
     * @param minDistance minimum distance from 0,0
     * @return a valid ground Location, or null if none found
     */
    public static Location findLandLocation(World world, int radius, int minDistance) {
        for (int attempt = 0; attempt < MAX_ATTEMPTS; attempt++) {
            int x = randomCoord(radius, minDistance);
            int z = randomCoord(radius, minDistance);

            // Check world border
            if (!world.getWorldBorder().isInside(new Location(world, x, 64, z))) continue;

            int y = world.getHighestBlockYAt(x, z);
            if (y <= world.getMinHeight()) continue;

            Location loc = new Location(world, x, y, z);
            Material surface = loc.getBlock().getType();

            // Skip undesirable surfaces
            if (isOcean(surface) || isLava(surface)) continue;

            return loc;
        }
        return null;
    }

    private static int randomCoord(int radius, int minDist) {
        int mag = minDist + RANDOM.nextInt(radius - minDist);
        return RANDOM.nextBoolean() ? mag : -mag;
    }

    private static boolean isOcean(Material m) {
        return m == Material.WATER || m == Material.SEAGRASS
            || m == Material.KELP_PLANT || m == Material.KELP
            || m == Material.TALL_SEAGRASS;
    }

    private static boolean isLava(Material m) {
        return m == Material.LAVA;
    }
}
