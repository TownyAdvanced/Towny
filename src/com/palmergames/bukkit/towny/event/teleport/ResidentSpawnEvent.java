package com.palmergames.bukkit.towny.event.teleport;

import com.palmergames.bukkit.towny.event.SpawnEvent;
import com.palmergames.bukkit.towny.object.Town;
import com.palmergames.bukkit.towny.object.WorldCoord;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

/**
 * An event called when /res spawn occurs.
 * 
 * @author LlmDl
 */
public class ResidentSpawnEvent extends SpawnEvent {
	
	Town fromTown;
	private Town toTown;
	
	public ResidentSpawnEvent(Player player, Location from, Location to) {
		super(player, from, to);
		
		fromTown = WorldCoord.parseWorldCoord(from).getTownOrNull();
		toTown = WorldCoord.parseWorldCoord(to).getTownOrNull();
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
	@Nullable
	public Town getFromTown() {
		return fromTown;
	}
}
