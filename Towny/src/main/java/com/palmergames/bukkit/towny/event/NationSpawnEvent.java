package com.palmergames.bukkit.towny.event;

import com.palmergames.bukkit.towny.object.Nation;
import com.palmergames.bukkit.towny.object.TownBlock;
import com.palmergames.bukkit.towny.object.WorldCoord;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * An event called when nation spawns occur.
 * 
 * @author Suneet Tipirneni (Siris)
 */
public class NationSpawnEvent extends SpawnEvent {
	private static final HandlerList HANDLER_LIST = new HandlerList();
	
	private Nation toNation;
	private Nation fromNation;

	/**
	 * Called when a player is teleported to a nation.
	 * 
	 * @param player The player being teleported.
	 * @param from The location the player is teleporting from.
	 * @param to The location the player is going to.
	 */
	public NationSpawnEvent(Player player, Location from, Location to, double cost, boolean cancelled, String cancelMessage) {
		super(player, from, to, cost);
		
		TownBlock fromTownBlock = WorldCoord.parseWorldCoord(from).getTownBlockOrNull();
		TownBlock toTownBlock = WorldCoord.parseWorldCoord(to).getTownBlockOrNull();
		
		if (fromTownBlock != null)
			fromNation = fromTownBlock.getTownOrNull().getNationOrNull();
		if (toTownBlock != null)
			toNation = toTownBlock.getTownOrNull().getNationOrNull();
		
		setCancelled(cancelled);
		setCancelMessage(cancelMessage);
	}

	/**
	 * Gets the nation that the player to spawning to.
	 *
	 * @return The nation being spawned to.
	 */
	public Nation getToNation() {
		return toNation;
	}

	/**
	 * Gets the nation the player is spawning from.
	 * 
	 * @return null if the player is not standing in a nation owned townblock, the nation otherwise.
	 */
	@Nullable
	public Nation getFromNation() {
		return fromNation;
	}

	public static HandlerList getHandlerList() {
		return HANDLER_LIST;
	}

	@NotNull
	@Override
	public HandlerList getHandlers() {
		return HANDLER_LIST;
	}
}
