package com.palmergames.bukkit.util;

import org.bukkit.Effect;
import org.bukkit.Location;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

/**
 * @author Chris H (Zren / Shade)
 *         Date: 4/15/12
 */
public class DrawSmokeTaskFactory {
	public static final LocationRunnable SEND_TO_WORLD = sendToWorld();

    /**
     * Send to all players in the world the location is in.
     * @return
     */
    private static LocationRunnable sendToWorld() {
        return new LocationRunnable() {
            BlockFace smokeDirection = BlockFace.UP;
            Vector offset = new Vector(0.5, 0.5, 0.5);

            @Override
            public void run(Location loc) {
                loc.getWorld().playEffect(loc.add(offset), Effect.SMOKE, smokeDirection);
            }
        };
    }

    /**
     * Send to the smoke effect to the player.
     * @return LocationRunnable
     */
    public static LocationRunnable sendToPlayer(final Player player) {
        return new LocationRunnable() {
            BlockFace smokeDirection = BlockFace.UP;
            Vector offset = new Vector(0.5, 0.5, 0.5);

            @Override
            public void run(Location loc) {
            	// Considering changing from Smoke to this Green coloured particle. It is difficult to see however.
            	// Colours could be changed to differentiate between friendly and enemy lands, but we'd have to be passing colour data.
            	//
                // player.spawnParticle(Particle.REDSTONE, loc.add(offset), 0, Float.MIN_VALUE, 255, 0, 1);
            	
            	player.playEffect(loc.add(offset), Effect.SMOKE, smokeDirection);
            }
        };
    }
}