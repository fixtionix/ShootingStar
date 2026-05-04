package com.shootingstar.manager;

import com.shootingstar.ShootingStarPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Random;

/**
 * Handles the automatic timed scheduling of star events.
 * After each event ends (or is cancelled) scheduleNext() should be called.
 */
public class EventScheduler {

    private final ShootingStarPlugin plugin;
    private final StarEventManager   eventManager;
    private final Random             random = new Random();

    private int scheduledTaskId = -1;

    public EventScheduler(ShootingStarPlugin plugin, StarEventManager eventManager) {
        this.plugin       = plugin;
        this.eventManager = eventManager;
    }

    /** Schedule the next star event using the configured min/max interval. */
    public void scheduleNext() {
        cancel(); // clear any existing timer

        int minSeconds = plugin.getConfig().getInt("event-interval.min", 1800);
        int maxSeconds = plugin.getConfig().getInt("event-interval.max", 5400);

        int range = Math.max(1, maxSeconds - minSeconds);
        int delaySeconds = minSeconds + random.nextInt(range);
        long delayTicks  = delaySeconds * 20L;

        plugin.getLogger().info(String.format(
            "[ShootingStar] Next star event in %.1f minutes.", delaySeconds / 60.0));

        BukkitRunnable task = new BukkitRunnable() {
            @Override
            public void run() {
                boolean started = eventManager.startEvent();
                if (started) {
                    // Re-schedule after a sensible cooldown (despawn + buffer)
                    int despawn  = plugin.getConfig().getInt("chest-despawn-delay", 90);
                    int unlock   = plugin.getConfig().getInt("chest-unlock-delay", 15);
                    int fallTime = plugin.getConfig().getInt("fall-start-y", 280) *
                                   plugin.getConfig().getInt("fall-speed-ticks", 2);
                    int buffer   = 60;
                    long afterEventTicks = ((long) despawn + unlock + (fallTime / 20) + buffer) * 20L;

                    new BukkitRunnable() {
                        @Override
                        public void run() {
                            scheduleNext();
                        }
                    }.runTaskLater(plugin, afterEventTicks);
                } else {
                    // Something went wrong — retry in 60s
                    plugin.getLogger().warning("[ShootingStar] Could not start event, retrying in 60s.");
                    new BukkitRunnable() {
                        @Override
                        public void run() { scheduleNext(); }
                    }.runTaskLater(plugin, 1200L);
                }
            }
        };

        scheduledTaskId = task.runTaskLater(plugin, delayTicks).getTaskId();
    }

    /** Cancel the pending scheduled task (does not cancel a running event). */
    public void cancel() {
        if (scheduledTaskId != -1) {
            plugin.getServer().getScheduler().cancelTask(scheduledTaskId);
            scheduledTaskId = -1;
        }
    }

    public boolean isScheduled() { return scheduledTaskId != -1; }
}
