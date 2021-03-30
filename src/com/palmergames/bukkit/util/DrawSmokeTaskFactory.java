package com.palmergames.bukkit.util;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

/**
 * @author Chris H (Zren / Shade)
 *         Date: 4/15/12
 */
public class DrawSmokeTaskFactory {

    /**
     * Send to the smoke effect to the player.
	 * 
	 * @param player - {@link Player} to send smoke effect
     * @return {@link LocationRunnable}
     */
    public static LocationRunnable sendToPlayer(final Player player) {
        return new LocationRunnable() {
            Vector offset = new Vector(0.5, 1.5, 0.5);

            @Override
            public void run(Location loc) {
            	// Considering changing from Smoke to this Green coloured particle. It is difficult to see however.
            	// Colours could be changed to differentiate between friendly and enemy lands, but we'd have to be passing colour data.
            	player.spawnParticle(Particle.SMOKE_NORMAL, loc.add(offset), 5,0,0,0,0);
            }
        };
    }
}