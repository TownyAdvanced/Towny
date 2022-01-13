package com.palmergames.bukkit.towny.event.player;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import com.palmergames.bukkit.towny.object.Translatable;

public class PlayerDeniedBedUseEvent extends Event implements Cancellable {

	private static final HandlerList handlers = new HandlerList();
	private Translatable denialMessage;
	private boolean isCancelled = false;
	private final Player player;
	private final Location location;
	private final boolean consideredEnemy;
	
	public PlayerDeniedBedUseEvent(Player player, Location location, boolean consideredEnemy, Translatable denialMessage) {
		super(!Bukkit.getServer().isPrimaryThread());
		this.player = player;
		this.location = location;
		this.consideredEnemy = consideredEnemy;
		this.setDenialMessage(denialMessage);
	}

	@Override
	public boolean isCancelled() {
		return isCancelled;
	}

	/**
	 * Set to true in order to allow the bed use.
	 */
	@Override
	public void setCancelled(boolean cancel) {
		isCancelled = cancel;
	}

	/**
	 * @return the denialMessage shown when a player cannot use the bed.
	 */
	public Translatable getDenialMessage() {
		return denialMessage;
	}

	/**
	 * @param cancelMessage the cancelMessage to set
	 */
	public void setDenialMessage(Translatable cancelMessage) {
		this.denialMessage = cancelMessage;
	}

	@Override
	public HandlerList getHandlers() {
		return handlers;
	}
	
	public static HandlerList getHandlerList() {
		return handlers;
	}

	/**
	 * @return the player being denied the use of a bed by Towny.
	 */
	public Player getPlayer() {
		return player;
	}

	/**
	 * @return the location of the bed.
	 */
	public Location getLocation() {
		return location;
	}

	/**
	 * When false this is being denied because the player is in neither a
	 * personally-owned plot, nor an Inn plot.
	 * 
	 * @return True when this beduse is being denied because the Player is an enemy
	 *         or outlaw at the bed's location.
	 */
	public boolean isConsideredEnemy() {
		return consideredEnemy;
	}

}
