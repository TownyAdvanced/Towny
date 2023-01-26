package com.palmergames.bukkit.towny.event;

import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.towny.object.Town;
import com.palmergames.bukkit.towny.object.TownBlock;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

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

	public @Nullable Resident getResident() {
		return r;
	}

	public @Nullable Town getTown() {
		return t;
	}

	public @Nullable TownBlock getTownBlock() {
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
	public @NotNull HandlerList getHandlers() {
		return handlers;
	}

	public static HandlerList getHandlerList() {
		return handlers;
	}
}