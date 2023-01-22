package com.palmergames.bukkit.towny.event;

import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.towny.object.Town;
import com.palmergames.bukkit.towny.object.TownBlock;
import org.bukkit.event.HandlerList;

public class TownBlockSettingsPreChangeEvent extends CancellableTownyEvent {

	private static final HandlerList handlers = new HandlerList();
	private Resident r;
	private Town t;
	private TownBlock tb;
	private boolean isCancelled = false;

	public TownBlockSettingsPreChangeEvent(Resident r) {
		this.r = r;
	}

	public TownBlockSettingsPreChangeEvent(Town t) {
		this.t = t;
	}

	public TownBlockSettingsPreChangeEvent(TownBlock tb) {
		this.tb = tb;
	}

	public Resident getResident() {
		return r;
	}

	public Town getTown() {
		return t;
	}

	public TownBlock getTownBlock() {
		return tb;
	}

	@Override
	public boolean isCancelled() {
		return isCancelled;
	}

	@Override
	public void setCancelled(boolean b) {
		this.isCancelled = b;
	}

	@Override
	public HandlerList getHandlers() {
		return handlers;
	}

	public static HandlerList getHandlerList() {
		return handlers;
	}
}