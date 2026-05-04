package com.shootingstar;

import com.shootingstar.command.ShootingStarCommand;
import com.shootingstar.listener.CompassListener;
import com.shootingstar.listener.LootChestListener;
import com.shootingstar.listener.PvpZoneListener;
import com.shootingstar.manager.EventScheduler;
import com.shootingstar.manager.StarEventManager;
import org.bukkit.plugin.java.JavaPlugin;

public final class ShootingStarPlugin extends JavaPlugin {

    private StarEventManager eventManager;
    private EventScheduler eventScheduler;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        eventManager  = new StarEventManager(this);
        eventScheduler = new EventScheduler(this, eventManager);

        // Register listeners
        getServer().getPluginManager().registerEvents(new LootChestListener(this, eventManager), this);
        getServer().getPluginManager().registerEvents(new PvpZoneListener(this, eventManager), this);
        getServer().getPluginManager().registerEvents(new CompassListener(this, eventManager), this);

        // Register command
        ShootingStarCommand cmd = new ShootingStarCommand(this, eventManager, eventScheduler);
        getCommand("shootingstar").setExecutor(cmd);
        getCommand("shootingstar").setTabCompleter(cmd);

        // Start the scheduler
        eventScheduler.scheduleNext();

        getLogger().info("ShootingStar enabled! Stars will fall. ★");
    }

    @Override
    public void onDisable() {
        if (eventScheduler != null) eventScheduler.cancel();
        if (eventManager != null)  eventManager.cancelCurrentEvent();
        getLogger().info("ShootingStar disabled.");
    }

    public StarEventManager getEventManager() { return eventManager; }
    public EventScheduler getEventScheduler() { return eventScheduler; }
}
