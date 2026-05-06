package com.shootingstar.util;

import com.shootingstar.model.StarVariant;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;

import java.util.Random;

public class ParticleUtil {

    private static final Random RANDOM = new Random();

    private ParticleUtil() {}

    public static void spawnTrail(Location loc, StarVariant variant) {
        World world = loc.getWorld();
        if (world == null) return;

        Particle.DustOptions trailDust = new Particle.DustOptions(variant.getTrailColor(), 3.0f);
        world.spawnParticle(Particle.DUST, loc, 20, 0.5, 0.5, 0.5, 0, trailDust, true);

        Particle.DustOptions coreDust = new Particle.DustOptions(variant.getCoreColor(), 4.0f);
        world.spawnParticle(Particle.DUST, loc, 10, 0.2, 0.2, 0.2, 0, coreDust, true);

        world.spawnParticle(Particle.END_ROD, loc, 8, 0.3, 0.3, 0.3, 0.05, null, true);

        if (variant == StarVariant.RAINBOW) {
            Color random = Color.fromRGB(RANDOM.nextInt(256), RANDOM.nextInt(256), RANDOM.nextInt(256));
            Particle.DustOptions rainbowDust = new Particle.DustOptions(random, 2.5f);
            world.spawnParticle(Particle.DUST, loc, 12, 0.6, 0.6, 0.6, 0, rainbowDust, true);
        } else {
            world.spawnParticle(variant.getExtraParticle(), loc, 6, 0.3, 0.3, 0.3, 0.02, null, true);
        }

        world.spawnParticle(Particle.CAMPFIRE_COSY_SMOKE, loc.clone().add(0, 1, 0), 5, 0.5, 0.5, 0.5, 0.02, null, true);
    }

    public static void spawnImpact(Location loc, StarVariant variant) {
        World world = loc.getWorld();
        if (world == null) return;

        for (int i = 0; i < 72; i++) {
            double angle = Math.toRadians(i * 5);
            double radius = 4 + RANDOM.nextDouble() * 2;
            Location ring = loc.clone().add(
                    Math.cos(angle) * radius, 0.1, Math.sin(angle) * radius);
            Particle.DustOptions dust = new Particle.DustOptions(variant.getTrailColor(), 2.0f);
            world.spawnParticle(Particle.DUST, ring, 3, 0.1, 0.1, 0.1, 0, dust, true);
        }

        world.spawnParticle(Particle.EXPLOSION, loc, 3, 0.5, 0.5, 0.5, 0.1, null, true);
        world.spawnParticle(Particle.FIREWORK, loc, 80, 0.8, 0.8, 0.8, 0.3, null, true);
        world.spawnParticle(Particle.END_ROD,  loc, 40, 1.0, 1.0, 1.0, 0.4, null, true);
        world.spawnParticle(Particle.FLASH,    loc, 1,  0,   0,   0,   0,   null, true);
        world.spawnParticle(Particle.ASH, loc.clone().add(0, 2, 0), 60, 2, 2, 2, 0.05, null, true);

        if (variant == StarVariant.RAINBOW) {
            for (int i = 0; i < 5; i++) {
                Color c = Color.fromRGB(RANDOM.nextInt(256), RANDOM.nextInt(256), RANDOM.nextInt(256));
                world.spawnParticle(Particle.DUST,
                        loc.clone().add((RANDOM.nextDouble() - 0.5) * 4,
                                RANDOM.nextDouble() * 3,
                                (RANDOM.nextDouble() - 0.5) * 4),
                        20, 0.5, 0.5, 0.5, 0,
                        new Particle.DustOptions(c, 2.0f), true);
            }
        }
    }

    public static void spawnChestAmbient(Location loc, StarVariant variant) {
        World world = loc.getWorld();
        if (world == null) return;
        Particle.DustOptions dust = new Particle.DustOptions(variant.getCoreColor(), 1.5f);
        world.spawnParticle(Particle.DUST, loc.clone().add(0.5, 1.2, 0.5), 6, 0.5, 0.4, 0.5, 0, dust, true);
        world.spawnParticle(Particle.END_ROD, loc.clone().add(0.5, 1.5, 0.5), 2, 0.4, 0.4, 0.4, 0.02, null, true);
    }
}
