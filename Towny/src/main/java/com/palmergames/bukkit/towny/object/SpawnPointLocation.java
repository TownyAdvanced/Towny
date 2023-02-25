package com.palmergames.bukkit.towny.object;

import com.palmergames.bukkit.towny.TownyAPI;
import org.bukkit.Location;

/**
 * A worldname, x, y, z location used to validate SpawnPoints. 
 */
public class SpawnPointLocation extends Position {
	
	public SpawnPointLocation(Location loc) {
		super(TownyAPI.getInstance().getTownyWorld(loc.getWorld()), loc.getX(), loc.getY(), loc.getZ(), 0F, 0F);
	}

	private SpawnPointLocation(Position position) {
		super(position.world(), position.blockX(), position.blockY(), position.blockZ(), 0F, 0F);
	}

	/**
	 * @deprecated Deprecated as of TODO insert version, please use {@link #world()} instead.
	 */
	@Deprecated
	public String getWorld() {
		return world().getName();
	}

	public int getX() {
		return blockX();
	}

	public int getY() {
		return blockY();
	}

	public int getZ() {
		return blockZ();
	}
	
	public static SpawnPointLocation parseSpawnPointLocation(Location loc) {
		return parsePos(Position.ofLocation(loc)); // ofLocation has a few extra validations that this ctor doesn't
	}
	
	public static SpawnPointLocation parsePos(Position position) {
		return new SpawnPointLocation(position);
	}
	
	public String toString() {
		return world().getName() + "," + blockX() + "," + blockY() + "," + blockZ();  
	}
	
	@Override
	public boolean equals(Object other) {
		if (this == other) return true;
		if (!(other instanceof SpawnPointLocation loc)) return false;
		
		return world().getName().equalsIgnoreCase(loc.world().getName()) && blockX() == loc.blockX() && blockY() == loc.blockY() && blockZ() == loc.blockZ();
	}
}
