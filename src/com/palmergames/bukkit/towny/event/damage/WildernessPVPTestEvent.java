package com.palmergames.bukkit.towny.event.damage;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import com.palmergames.bukkit.towny.object.TownyWorld;

/**
 * An event thrown when Towny will determine the PVP status of 
 * the wilderness surrounding town-owned land.
 * 
 * @author LlmDl
 */
public class WildernessPVPTestEvent extends Event {

	private static final HandlerList handlers = new HandlerList();
	private final TownyWorld world;
	private boolean pvp;
	
	public WildernessPVPTestEvent(TownyWorld world, boolean pvp) {
		this.world = world;
		this.setPvp(pvp);
	}
	
	public static HandlerList getHandlerList() {
		return handlers;
	}

	public HandlerList getHandlers() {
		return handlers;
	}

	/**
	 * @return the TownyWorld in which the PVP test is occuring.
	 */
	public TownyWorld getWorld() {
		return world;
	}

	/**
	 * @return whether PVP is on or off in the wilderness.
	 */
	public boolean isPvp() {
		return pvp;
	}

	/**
	 * Sets the pvp status and outcome of the event.
	 * @param pvp whether the event will result in PVP being on or off in the wilderness.
	 */
	public void setPvp(boolean pvp) {
		this.pvp = pvp;
	}

}
