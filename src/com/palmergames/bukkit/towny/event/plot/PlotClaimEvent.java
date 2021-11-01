package com.palmergames.bukkit.towny.event.plot;

import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.towny.object.TownBlock;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class PlotClaimEvent extends Event {
	private static final HandlerList handlers = new HandlerList();
	private final Resident oldResident, newResident;
	private final TownBlock townBlock;

	public PlotClaimEvent(Resident oldResident, Resident newResident, TownBlock townBlock) {
		this.oldResident = oldResident;
		this.newResident = newResident;
		this.townBlock = townBlock;
	}

	@Nullable
	public Resident getOldResident() {
		return oldResident;
	}

	@Nullable
	public Resident getNewResident() {
		return newResident;
	}

	public TownBlock getTownBlock() {
		return townBlock;
	}

	@Override
	public @NotNull HandlerList getHandlers() {
		return handlers;
	}

	public static HandlerList getHandlerList() {
		return handlers;
	}
}
