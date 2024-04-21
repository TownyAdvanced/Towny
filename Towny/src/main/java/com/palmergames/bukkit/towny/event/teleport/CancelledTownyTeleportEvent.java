package com.palmergames.bukkit.towny.event.teleport;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;
import com.palmergames.bukkit.towny.object.Resident;

/**
 * An event thrown by Towny when a player that was supposed to teleport to the
 * /res, /town, or /nation spawn, but the action was cancelled. This can be due
 * to Movement, Damage or an Unknown source (when cancelled via the TownyAPI
 * class.
 * 
 * @since 0.100.2.2
 * @author LlmDl
 */
public class CancelledTownyTeleportEvent extends Event {

	private static final HandlerList handlers = new HandlerList();
	private final Resident resident;
	private final Location location;
	private final double teleportCost;
	private final CancelledTeleportReason reason;

	public CancelledTownyTeleportEvent(Resident resident, Location location, double teleportCost, CancelledTeleportReason reason) {
		super(!Bukkit.isPrimaryThread());
		this.resident = resident;
		this.location = location;
		this.teleportCost = teleportCost;
		this.reason = reason;
	}

	/**
	 * @return Resident which is not going to teleport.
	 */
	public Resident getResident() {
		return resident;
	}

	/**
	 * @return Location that resident was going to go to.
	 */
	public Location getLocation() {
		return location;
	}

	/**
	 * @return the amount of money which was refunded to the player.
	 */
	public double getTeleportCost() {
		return teleportCost;
	}

	/**
	 * @return the {@link CancelledTeleportReason} that the resident will not teleport.
	 *         When cancelled via the TownyAPI class, this will return UNKNOWN.
	 */
	public CancelledTeleportReason getReason() {
		return reason;
	}

	@NotNull
	@Override
	public HandlerList getHandlers() {
		return handlers;
	}

	public static HandlerList getHandlerList() {
		return handlers;
	}

	public enum CancelledTeleportReason {
		MOVEMENT, DAMAGE, UNKNOWN
	}
}
