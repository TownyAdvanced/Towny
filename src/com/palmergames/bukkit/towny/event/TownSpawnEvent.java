package com.palmergames.bukkit.towny.event;

import com.palmergames.bukkit.towny.exceptions.NotRegisteredException;
import com.palmergames.bukkit.towny.object.Town;
import com.palmergames.bukkit.towny.object.WorldCoord;
import org.bukkit.Location;
import org.bukkit.entity.Player;

/**
 * An event called when town spawns occur.
 * 
 * Deprecated as of 0.96.3.4, use {@link com.palmergames.bukkit.towny.event.teleport.TownSpawnEvent} instead.
 * @author Suneet Tipirneni (Siris)
 */
@Deprecated
public class TownSpawnEvent extends SpawnEvent {
	
	Town fromTown;
	private Town toTown;
	
	public TownSpawnEvent(Player player, Location from, Location to) {
		super(player, from, to);

		try {
			fromTown = WorldCoord.parseWorldCoord(from).getTownBlock().getTown();
		} catch (NotRegisteredException ignored) {}
		
		try {
			toTown = WorldCoord.parseWorldCoord(to).getTownBlock().getTown();
		} catch (NotRegisteredException ignored) {}
		
	}

	/**
	 * Gets the town that the player is teleporting to.
	 * 
	 * @return The Town being teleported to.
	 */
	public Town getToTown() {
		return toTown;
	}

	/**
	 * Gets the town being teleported from.
	 * 
	 * @return null if the player was not standing in a townblock, the town they were standing in otherwise.
	 */
	public Town getFromTown() {
		return fromTown;
	}
}
