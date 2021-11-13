package com.palmergames.bukkit.towny.event.plot.changeowner;

import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.towny.object.TownBlock;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public abstract class PlotChangeOwnerEvent extends Event {
	private static final HandlerList handlers = new HandlerList();
	private final Resident oldResident, newResident;
	private final TownBlock townBlock;

	public PlotChangeOwnerEvent(@Nullable Resident oldResident, @Nullable Resident newResident, @NotNull TownBlock townBlock) {
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

	@NotNull
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
