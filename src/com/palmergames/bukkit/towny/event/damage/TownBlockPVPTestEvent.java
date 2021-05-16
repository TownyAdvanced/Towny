package com.palmergames.bukkit.towny.event.damage;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.Nullable;

import com.palmergames.bukkit.towny.object.Town;
import com.palmergames.bukkit.towny.object.TownBlock;

/**
 * An event thrown when Towny will determine the PVP status of 
 * a townblock, or plot, in a town.
 * 
 * @author LlmDl
 */
public class TownBlockPVPTestEvent extends Event {

	private static final HandlerList handlers = new HandlerList();
	private final TownBlock townBlock;
	private boolean pvp;
	
	public TownBlockPVPTestEvent(TownBlock townBlock, boolean pvp) {
		this.townBlock = townBlock;
		this.setPvp(pvp);
	}
	
	
	public static HandlerList getHandlerList() {
		return handlers;
	}

	public HandlerList getHandlers() {
		return handlers;
	}

	/**
	 * @return the TownBlock which is having its pvp status decided.
	 */
	public TownBlock getTownBlock() {
		return townBlock;
	}

	/**
	 * @return the Town where this test is made, should never return null.
	 */
	@Nullable
	public Town getTown() {
		return townBlock.getTownOrNull();
	}
	
	/**
	 * @return true if the townblock has PVP on.
	 */
	public boolean isPvp() {
		return pvp;
	}

	/**
	 * Sets the pvp status and outcome of the event.
	 * @param pvp whether the event will result in PVP being on or off in the townblock.
	 */
	public void setPvp(boolean pvp) {
		this.pvp = pvp;
	}

}
