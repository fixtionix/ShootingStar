package com.shootingstar.model;

import org.bukkit.Color;
import org.bukkit.Particle;

public enum StarVariant {

    NORMAL("Normal", "golden",
            Color.fromRGB(255, 200, 50),  Color.fromRGB(255, 240, 180), Particle.FLAME),
    BLUE("Blue", "blue",
            Color.fromRGB(50,  150, 255), Color.fromRGB(180, 220, 255), Particle.FALLING_WATER),
    RED("Red", "red",
            Color.fromRGB(220, 50,  50),  Color.fromRGB(255, 180, 180), Particle.FLAME),
    RAINBOW("Rainbow", "rainbow",
            Color.fromRGB(200, 100, 255), Color.fromRGB(255, 255, 255), Particle.TOTEM_OF_UNDYING);

    private final String displayName;
    private final String adjective;
    private final Color  trailColor;
    private final Color  coreColor;
    private final Particle extraParticle;

    StarVariant(String displayName, String adjective,
                Color trailColor, Color coreColor, Particle extraParticle) {
        this.displayName   = displayName;
        this.adjective     = adjective;
        this.trailColor    = trailColor;
        this.coreColor     = coreColor;
        this.extraParticle = extraParticle;
    }

    public String   getDisplayName()     { return displayName; }
    public String   getAdjective()       { return adjective; }
    public Color    getTrailColor()      { return trailColor; }
    public Color    getCoreColor()       { return coreColor; }
    public Particle getExtraParticle()   { return extraParticle; }
}
