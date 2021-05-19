package com.palmergames.bukkit.towny.event;

import com.palmergames.bukkit.towny.object.Town;
import com.palmergames.bukkit.towny.object.TownBlock;
import com.palmergames.bukkit.towny.object.WorldCoord;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

/**
 * An event called when town spawns occur.
 * 
 * @author Suneet Tipirneni (Siris)
 */
public class TownSpawnEvent extends SpawnEvent {
	
	Town fromTown;
	private Town toTown;
	
	public TownSpawnEvent(Player player, Location from, Location to) {
		super(player, from, to);

		TownBlock fromTownBlock = WorldCoord.parseWorldCoord(from).getTownBlockOrNull();
		TownBlock toTownBlock = WorldCoord.parseWorldCoord(to).getTownBlockOrNull();
		
		if (fromTownBlock != null)
			fromTown = fromTownBlock.getTownOrNull();
		if (toTownBlock != null)
			toTown = WorldCoord.parseWorldCoord(to).getTownBlockOrNull().getTownOrNull();
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
