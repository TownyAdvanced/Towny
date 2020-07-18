package com.palmergames.bukkit.towny.event;

import org.bukkit.Bukkit;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import com.palmergames.bukkit.towny.object.Nation;

public class NationPreRenameEvent extends Event implements Cancellable {

	private static final HandlerList handlers = new HandlerList();
	private final String oldName;
	private final String newName;
	private final Nation nation;
	private boolean isCancelled = false;

	@Override
	public HandlerList getHandlers() {
		return handlers;
	}

	public static HandlerList getHandlerList() {
		return handlers;
	}

	public NationPreRenameEvent(Nation nation, String newName) {
		super(!Bukkit.getServer().isPrimaryThread());
		this.oldName = nation.getName();
		this.nation = nation;
		this.newName = newName;
	}

	/**
	 *
	 * @return the old nation name.
	 */
	public String getOldName() {
		return oldName;
	}
	/**
	 * 
	 * @return the new nation name.
	 */
	public String getNewName() {
		return newName;
	}

	/**
	 *
	 * @return the nation with it's changed name
	 */
	public Nation getNation() {
		return this.nation;
	}

	@Override
	public boolean isCancelled() {
		return isCancelled;
	}

	@Override
	public void setCancelled(boolean cancel) {
		this.isCancelled = cancel;
	}
}
