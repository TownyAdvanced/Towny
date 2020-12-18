package com.palmergames.bukkit.towny.event.plot.toggle;

import com.palmergames.bukkit.towny.object.Town;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

public abstract class PlotToggleEvent extends Event implements Cancellable {

	private static final HandlerList handlers = new HandlerList();
	private final Town town;
	private final boolean futureState;
	private final Player player;
	private boolean isCancelled = false;

	public PlotToggleEvent(Town town, Player player, boolean futureState) {
		this.town = town;
		this.player = player;
		this.futureState = futureState;
	}

	@Override
	public boolean isCancelled() {
		return isCancelled;
	}

	@Override
	public void setCancelled(boolean isCancelled) {
		this.isCancelled = isCancelled;
	}

	@NotNull
	@Override
	public HandlerList getHandlers() {
		return handlers;
	}

	public static HandlerList getHandlerList() {
		return handlers;
	}

	public Town getTown() {
		return town;
	}

	public boolean getFutureState() {
		return futureState;
	}

	public Player getPlayer() {
		return player;
	}
	
}
