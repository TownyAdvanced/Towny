package com.palmergames.bukkit.towny.event.plot;

import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.towny.object.TownBlock;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class PlotPreClaimEvent extends Event implements Cancellable {
	private static final HandlerList handlers = new HandlerList();
	private boolean cancelled;
	private String cancelMessage = "";
	private final Resident oldResident, newResident;
	private final TownBlock townBlock;
	
	public PlotPreClaimEvent(Resident oldResident, Resident newResident, TownBlock townBlock) {
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
	
	@Override
	public boolean isCancelled() {
		return cancelled;
	}
	
	@Override
	public void setCancelled(boolean cancelled) {
		this.cancelled = cancelled;
	}
	
	public String getCancelMessage() {
		return cancelMessage;
	}
	
	public void setCancelMessage(String cancelMessage) {
		this.cancelMessage = cancelMessage;
	}
}
