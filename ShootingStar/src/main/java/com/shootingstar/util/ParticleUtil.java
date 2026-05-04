package com.shootingstar.util;

import com.shootingstar.model.StarVariant;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;

import java.util.Random;

/**
 * Centralised particle spawning helpers for falling trail, core glow, and impact burst.
 */
public class ParticleUtil {

    private static final Random RANDOM = new Random();

    private ParticleUtil() {}

    /**
     * Spawns the per-tick trail particles as the star falls.
     */
    public static void spawnTrail(Location loc, StarVariant variant) {
        World world = loc.getWorld();
        if (world == null) return;

        // Coloured dust trail
        Particle.DustOptions trailDust = new Particle.DustOptions(variant.getTrailColor(), 1.8f);
        world.spawnParticle(Particle.DUST, loc, 12, 0.25, 0.25, 0.25, 0, trailDust);

        // Bright white core
        Particle.DustOptions coreDust = new Particle.DustOptions(variant.getCoreColor(), 2.5f);
        world.spawnParticle(Particle.DUST, loc, 6, 0.1, 0.1, 0.1, 0, coreDust);

        // END_ROD for a sharp glowing needle
        world.spawnParticle(Particle.END_ROD, loc, 3, 0.15, 0.15, 0.15, 0.02);

        // Variant-specific extra particles
        if (variant == StarVariant.RAINBOW) {
            // Cycle through HSB-derived colours each tick for rainbow effect
            Color random = Color.fromRGB(RANDOM.nextInt(256), RANDOM.nextInt(256), RANDOM.nextInt(256));
            Particle.DustOptions rainbowDust = new Particle.DustOptions(random, 1.5f);
            world.spawnParticle(Particle.DUST, loc, 8, 0.4, 0.4, 0.4, 0, rainbowDust);
        } else {
            world.spawnParticle(variant.getExtraParticle(), loc, 4, 0.2, 0.2, 0.2, 0.01);
        }

        // Smoke tail above the star
        world.spawnParticle(Particle.CAMPFIRE_COSY_SMOKE, loc.clone().add(0, 1, 0), 3, 0.3, 0.3, 0.3, 0.01);
    }

    /**
     * Massive burst on impact.
     */
    public static void spawnImpact(Location loc, StarVariant variant) {
        World world = loc.getWorld();
        if (world == null) return;

        // Shockwave ring of coloured dust
        for (int i = 0; i < 72; i++) {
            double angle = Math.toRadians(i * 5);
            double radius = 4 + RANDOM.nextDouble() * 2;
            Location ring = loc.clone().add(
                    Math.cos(angle) * radius, 0.1, Math.sin(angle) * radius);
            Particle.DustOptions dust = new Particle.DustOptions(variant.getTrailColor(), 2.0f);
            world.spawnParticle(Particle.DUST, ring, 3, 0.1, 0.1, 0.1, 0, dust);
        }

        // Central burst upward
        world.spawnParticle(Particle.EXPLOSION, loc, 3, 0.5, 0.5, 0.5, 0.1);
        world.spawnParticle(Particle.FIREWORK, loc, 80, 0.8, 0.8, 0.8, 0.3);
        world.spawnParticle(Particle.END_ROD,  loc, 40, 1.0, 1.0, 1.0, 0.4);
        world.spawnParticle(Particle.FLASH,    loc, 1,  0,   0,   0,   0);

        // Falling debris
        world.spawnParticle(Particle.ASH, loc.clone().add(0, 2, 0), 60, 2, 2, 2, 0.05);

        if (variant == StarVariant.RAINBOW) {
            for (int i = 0; i < 5; i++) {
                Color c = Color.fromRGB(RANDOM.nextInt(256), RANDOM.nextInt(256), RANDOM.nextInt(256));
                world.spawnParticle(Particle.DUST,
                        loc.clone().add((RANDOM.nextDouble() - 0.5) * 4,
                                RANDOM.nextDouble() * 3,
                                (RANDOM.nextDouble() - 0.5) * 4),
                        20, 0.5, 0.5, 0.5, 0,
                        new Particle.DustOptions(c, 2.0f));
            }
        }
    }

    /**
     * Gentle ambient glow around a landed chest.
     */
    public static void spawnChestAmbient(Location loc, StarVariant variant) {
        World world = loc.getWorld();
        if (world == null) return;
        Particle.DustOptions dust = new Particle.DustOptions(variant.getCoreColor(), 1.2f);
        world.spawnParticle(Particle.DUST, loc.clone().add(0.5, 1.2, 0.5), 4, 0.4, 0.3, 0.4, 0, dust);
        world.spawnParticle(Particle.END_ROD, loc.clone().add(0.5, 1.5, 0.5), 1, 0.3, 0.3, 0.3, 0.01);
    }
}
