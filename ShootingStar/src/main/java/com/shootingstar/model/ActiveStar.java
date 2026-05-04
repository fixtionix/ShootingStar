package com.shootingstar.model;

import org.bukkit.Location;
import org.bukkit.entity.ArmorStand;

/**
 * Holds the runtime state of a currently-falling or landed shooting star.
 */
public class ActiveStar {

    public enum Phase { FALLING, LANDED, CHEST_OPEN, DONE }

    private final StarVariant  variant;
    private final Location     targetLocation;    // ground impact point
    private Location           currentLocation;   // current position of the falling entity

    private ArmorStand         marker;            // invisible entity used for position reference
    private org.bukkit.entity.Item chestItem;     // dropped chest item on the ground

    private Phase phase = Phase.FALLING;

    // task IDs so we can cancel them
    private int fallTaskId      = -1;
    private int unlockTaskId    = -1;
    private int despawnTaskId   = -1;

    public ActiveStar(StarVariant variant, Location targetLocation) {
        this.variant        = variant;
        this.targetLocation = targetLocation.clone();
        this.currentLocation = targetLocation.clone().add(0, 200, 0);
    }

    // --- Getters / Setters ---

    public StarVariant getVariant()              { return variant; }
    public Location    getTargetLocation()       { return targetLocation.clone(); }
    public Location    getCurrentLocation()      { return currentLocation.clone(); }
    public void        setCurrentLocation(Location l) { this.currentLocation = l.clone(); }

    public ArmorStand  getMarker()               { return marker; }
    public void        setMarker(ArmorStand a)   { this.marker = a; }

    public org.bukkit.entity.Item getChestItem() { return chestItem; }
    public void setChestItem(org.bukkit.entity.Item i) { this.chestItem = i; }

    public Phase getPhase()             { return phase; }
    public void  setPhase(Phase p)      { this.phase = p; }

    public int   getFallTaskId()        { return fallTaskId; }
    public void  setFallTaskId(int id)  { this.fallTaskId = id; }

    public int   getUnlockTaskId()      { return unlockTaskId; }
    public void  setUnlockTaskId(int id){ this.unlockTaskId = id; }

    public int   getDespawnTaskId()     { return despawnTaskId; }
    public void  setDespawnTaskId(int id){ this.despawnTaskId = id; }
}
