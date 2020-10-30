package com.palmergames.bukkit.towny.event;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;

/**
 * 
 * @author LlmDl
 */
public interface TownyActionEvent {

	/**
	 * @return Whether the event has been cancelled.
	 */
	public boolean isCancelled();

	/**
	 * @param boolean Set the event to cancelled.
	 */
	public void setCancelled(boolean cancelled);

	/**
	 * @return null or the message shown to players when their action is cancelled.
	 */
	public String getMessage();

	/**
	 * @param message Message shown to players when their action is cancelled.
	 */
	public void setMessage(String message);

	/**
	 * @return Material of the block being used in the event.
	 */
	public Material getMaterial();
	
	/**
	 * @return Location of the event.
	 */
	public Location getLocation();

	/**
	 * @return player involved in the event.
	 */
	public Player getPlayer();
	
}
