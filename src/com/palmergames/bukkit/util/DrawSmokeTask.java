package com.palmergames.bukkit.util;

import org.bukkit.Effect;
import org.bukkit.Location;
import org.bukkit.block.BlockFace;
import org.bukkit.util.Vector;

/**
 * @author Chris H (Zren / Shade)
 *         Date: 4/15/12
 */
public class DrawSmokeTask implements LocationRunnable {
    BlockFace smokeDirection = BlockFace.UP;
    Vector offset = new Vector(0.5, 0.5, 0.5);

    @Override
    public void run(Location loc) {
        loc.getWorld().playEffect(loc.add(offset), Effect.SMOKE, smokeDirection);
    }

    public static final DrawSmokeTask DEFAULT = new DrawSmokeTask();
}