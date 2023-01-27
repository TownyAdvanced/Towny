package com.palmergames.bukkit.towny.event;

import com.palmergames.bukkit.towny.object.TownBlock;
import com.palmergames.bukkit.towny.object.TownyPermission;
import com.palmergames.bukkit.towny.object.TownyPermissionChange;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

public class TownBlockPermissionChangeEvent extends CancellableTownyEvent {

	private static final HandlerList handlers = new HandlerList();
	private static final TownyPermission test = new TownyPermission();
	private final TownBlock townBlock;
	private final String changes;
	private boolean isCancelled = false;

	public TownBlockPermissionChangeEvent(@NotNull TownBlock townBlock, @NotNull String changes) {
		this.townBlock = townBlock;
		this.changes = changes;
	}

	public TownBlockPermissionChangeEvent(@NotNull TownBlock townBlock, @NotNull TownyPermissionChange change) {
		this(townBlock, parseChanges(change));
	}

	public TownBlock getTownBlock() {
		return townBlock;
	}

	public String parseChanges() {
		return changes;
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
	
	private static String parseChanges(TownyPermissionChange change) {
		test.reset();
		test.change(change);
		return test.toString();
	}
}