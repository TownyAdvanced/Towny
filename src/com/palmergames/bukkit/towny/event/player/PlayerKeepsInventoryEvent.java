package com.palmergames.bukkit.towny.event.player;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.Nullable;

import com.palmergames.bukkit.towny.TownyAPI;
import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.towny.object.Town;

public class PlayerKeepsInventoryEvent extends Event implements Cancellable {

	private static final HandlerList handlers = new HandlerList();
	private boolean isCancelled = false;
	private final Player player;
	private final Location location;

	/**
	 * An event thrown after a PlayerDeathEvent at HIGHEST priority.
	 * Thrown when Towny would opt to keep someone's inventory and clear the drops.
	 * 
	 * @param player Player who has died.
	 * @param location Location where the player died.
	 */
	public PlayerKeepsInventoryEvent(Player player, Location location) {
		super(!Bukkit.getServer().isPrimaryThread());
		this.player = player;
		this.location = location;
	}

	@Override
	public boolean isCancelled() {
		return isCancelled;
	}

	/**
	 * Set to true in order to prevent Towny from keeping the inventory.
	 */
	@Override
	public void setCancelled(boolean cancel) {
		isCancelled = cancel;
	}

	@Override
	public HandlerList getHandlers() {
		return handlers;
	}

	public static HandlerList getHandlerList() {
		return handlers;
	}

	/**
	 * @return the player whose inventory is going to be kept.
	 */
	public Player getPlayer() {
		return player;
	}

	/**
	 * @return resident Resident or null of the player.
	 */
	public Resident getResident() {
		return TownyAPI.getInstance().getResident(player);
	}

	/**
	 * @return the location where the player died.
	 */
	public Location getLocation() {
		return location;
	}

	/**
	 * @return town Town or null, if the player died in the wilderness.
	 */
	@Nullable
	public Town getTownOrNull() {
		return TownyAPI.getInstance().getTown(location);
	}

	/**
	 * @return true when the Player has died in the wilderness.
	 */
	public boolean isWilderness() {
		return TownyAPI.getInstance().isWilderness(location);
	}
}
