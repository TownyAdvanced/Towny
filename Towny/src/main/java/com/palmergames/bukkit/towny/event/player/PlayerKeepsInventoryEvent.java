package com.palmergames.bukkit.towny.event.player;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.jetbrains.annotations.Nullable;

import com.palmergames.bukkit.towny.TownyAPI;
import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.towny.object.Town;

public class PlayerKeepsInventoryEvent extends Event implements Cancellable {

	private static final HandlerList handlers = new HandlerList();
	private boolean isCancelled = false;
	private final Player player;
	private final Location location;
	private final PlayerDeathEvent event;

	/**
	 * An event thrown after a PlayerDeathEvent at HIGHEST priority.
	 * Thrown when Towny would opt to keep someone's inventory and clear the drops.
	 * 
	 * @param event PlayerDeathEvent
	 */
	public PlayerKeepsInventoryEvent(PlayerDeathEvent event) {
		super(!Bukkit.getServer().isPrimaryThread());
		this.player = event.getEntity();
		this.location = event.getEntity().getLocation();
		this.event = event;
	}

	/**
	 * An event thrown after a PlayerDeathEvent at HIGHEST priority.
	 * Thrown when Towny would opt to keep someone's inventory and clear the drops.
	 * 
	 * @param event PlayerDeathEvent
	 * @param keepInventory Default state of the event.
	 */
	public PlayerKeepsInventoryEvent(PlayerDeathEvent event, boolean keepInventory) {
		super(!Bukkit.getServer().isPrimaryThread());
		this.player = event.getEntity();
		this.location = event.getEntity().getLocation();
		this.event = event;
		this.isCancelled = !keepInventory;
	}

	/**
	 * When true, players will not keep their inventory.
	 */
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
	 * @return the PlayerDeathEvent that killed the player.
	 */
	public PlayerDeathEvent getPlayerDeathEvent() {
		return event;
	}

	/**
	 * @return Resident that killed the player, if they died to another player, or null.
	 */
	@Nullable
	public Resident getKiller() {
		if (player.getLastDamageCause() instanceof EntityDamageByEntityEvent event) {
			Entity attackerEntity = event.getDamager();
			if (attackerEntity instanceof Projectile projectile
				&& projectile.getShooter() instanceof Player player)
				return TownyAPI.getInstance().getResident(player);
			else if (attackerEntity instanceof Player player)
				return TownyAPI.getInstance().getResident(player);
		}
		return null;
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
