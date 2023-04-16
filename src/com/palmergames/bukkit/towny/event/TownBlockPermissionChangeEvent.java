package com.palmergames.bukkit.towny.event;

import com.palmergames.bukkit.towny.object.TownBlock;
import com.palmergames.bukkit.towny.object.TownyPermissionChange;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

public class TownBlockPermissionChangeEvent extends CancellableTownyEvent {

	private static final HandlerList handlers = new HandlerList();
	private final TownBlock townBlock;
	private final TownyPermissionChange change;
	private boolean isCancelled = false;

	public TownBlockPermissionChangeEvent(@NotNull TownBlock townBlock, @NotNull TownyPermissionChange change) {
		this.townBlock = townBlock;
		this.change = change;
	}

	public TownBlock getTownBlock() {
		return townBlock;
	}

	public TownyPermissionChange getChange() {
		return change;
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