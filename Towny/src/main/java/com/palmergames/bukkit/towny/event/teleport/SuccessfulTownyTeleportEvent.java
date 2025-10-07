package com.palmergames.bukkit.towny.event.teleport;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;
import com.palmergames.bukkit.towny.object.Resident;

/**
 * Thrown when Towny teleports a player after they have used /res, /town, or
 * /nation spawn.
 * 
 * @author LlmDl
 * @since 0.100.2.12
 */
public class SuccessfulTownyTeleportEvent extends Event {

	private static final HandlerList handlers = new HandlerList();
	private final Resident resident;
	private final Location teleportLocation;
	private final Location priorLocation;
	private final double teleportCost;

	public SuccessfulTownyTeleportEvent(Resident resident, Location loc, double cost, Location priorLocation) {
		super(!Bukkit.isPrimaryThread());
		this.resident = resident;
		this.teleportLocation = loc;
		this.teleportCost = cost;
		this.priorLocation = priorLocation;
	}

	@NotNull
	@Override
	public HandlerList getHandlers() {
		return handlers;
	}

	public static HandlerList getHandlerList() {
		return handlers;
	}

	public Resident getResident() {
		return resident;
	}

	public Location getTeleportLocation() {
		return teleportLocation;
	}
	
	/**
	 * @return The location the player was at prior to being teleported.
	 */
	public Location getPriorLocation() { return priorLocation; }

	/**
	 * @return The price that the player paid to teleport.
	 */
	public double getTeleportCost() {
		return this.teleportCost;
	}
}
