package com.palmergames.bukkit.towny.war.flagwar.events;

import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import com.palmergames.bukkit.towny.war.flagwar.CellUnderAttack;

public class CellWonEvent extends Event implements Cancellable {

	private static final HandlerList handlers = new HandlerList();
	public boolean cancelled = false;

	@Override
	public HandlerList getHandlers() {

		return handlers;
	}

	public static HandlerList getHandlerList() {

		return handlers;
	}

	//////////////////////////////

	private CellUnderAttack cellAttackData;

	public CellWonEvent(CellUnderAttack cellAttackData) {

		super();
		this.cellAttackData = cellAttackData;
	}

	public CellUnderAttack getCellAttackData() {

		return cellAttackData;
	}
	
	@Override
    public boolean isCancelled() {
        return cancelled;
    }
	
     @Override
    public void setCancelled(boolean cancel) {
        this.cancelled = cancel;
    }
}