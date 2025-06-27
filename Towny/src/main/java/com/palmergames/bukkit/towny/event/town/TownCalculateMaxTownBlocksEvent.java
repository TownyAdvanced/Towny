package com.palmergames.bukkit.towny.event.town;

import com.google.common.base.Preconditions;
import com.palmergames.bukkit.towny.object.Town;

import org.bukkit.Bukkit;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

/**
 * Called when the maximum amount of townblocks is being calculated for a town, after all bonuses and limits have been applied.
 */
public class TownCalculateMaxTownBlocksEvent extends Event {
	private static final HandlerList HANDLER_LIST = new HandlerList();

	private final Town town;
	private int townBlockCount;

	@ApiStatus.Internal
	public TownCalculateMaxTownBlocksEvent(final Town town, final int townBlockCount) {
		super(!Bukkit.getServer().isPrimaryThread());
		this.town = town;
		this.townBlockCount = townBlockCount;
	}

	public Town getTown() {
		return town;
	}

	public int getTownBlockCount() {
		return townBlockCount;
	}

	public void setTownBlockCount(int townBlockCount) {
		Preconditions.checkArgument(townBlockCount >= 0, "townBlockCount must be >= 0, got %s", townBlockCount);
		this.townBlockCount = townBlockCount;
	}

	public static HandlerList getHandlerList() {
		return HANDLER_LIST;
	}

	@Override
	public @NotNull HandlerList getHandlers() {
		return HANDLER_LIST;
	}
}
