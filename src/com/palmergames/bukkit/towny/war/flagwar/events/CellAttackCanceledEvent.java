package com.palmergames.bukkit.towny.war.flagwar.events;

import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import com.palmergames.bukkit.towny.war.flagwar.CellUnderAttack;

/** @deprecated for removal in a future release. Please use <a href="https://github.com/TownyAdvanced/Flagwar">the FlagWar plugin</a> for continued support. */
@Deprecated
public class CellAttackCanceledEvent extends Event implements Cancellable{

	private static final HandlerList handlers = new HandlerList();
	private boolean cancelled = false;

	@Override
	public HandlerList getHandlers() {

		return handlers;
	}

	public static HandlerList getHandlerList() {

		return handlers;
	}

	//////////////////////////////
	private CellUnderAttack cell;

	public CellAttackCanceledEvent(CellUnderAttack cell) {

		super();
		this.cell = cell;

	}

	public CellUnderAttack getCell() {

		return cell;
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