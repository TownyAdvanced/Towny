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
	private Resident resident;
	private Location teleportLocation;

	public SuccessfulTownyTeleportEvent(Resident resident, Location loc) {
		super(!Bukkit.isPrimaryThread());
		this.resident = resident;
		this.teleportLocation = loc;
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

}
