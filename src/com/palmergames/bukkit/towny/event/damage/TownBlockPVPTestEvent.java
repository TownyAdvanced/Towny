package com.palmergames.bukkit.towny.event.damage;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import com.palmergames.bukkit.towny.object.TownBlock;

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


	public TownBlock getTownBlock() {
		return townBlock;
	}


	public boolean isPvp() {
		return pvp;
	}


	public void setPvp(boolean pvp) {
		this.pvp = pvp;
	}

}
