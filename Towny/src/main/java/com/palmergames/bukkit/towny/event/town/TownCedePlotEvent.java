package com.palmergames.bukkit.towny.event.town;

import com.palmergames.bukkit.towny.event.CancellableTownyEvent;
import com.palmergames.bukkit.towny.object.Town;
import com.palmergames.bukkit.towny.object.TownBlock;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

public class TownCedePlotEvent extends CancellableTownyEvent {
	private static final HandlerList HANDLER_LIST = new HandlerList();

	private final Town townGivingPlot;
	private final Town townGainingPlot;
	private final TownBlock townBlock;

	public TownCedePlotEvent(Town townGivingPlot, Town townGainingPlot, TownBlock townBlock) {
		this.townGivingPlot = townGivingPlot;
		this.townGainingPlot = townGainingPlot;
		this.townBlock = townBlock;
	}

	public Town getTownGivingPlot() {
		return townGivingPlot;
	}

	public Town getTownReceivingPlot() {
		return townGainingPlot;
	}

	public TownBlock getTownBlock() {
		return townBlock;
	}

	public static HandlerList getHandlerList() {
		return HANDLER_LIST;
	}

	@NotNull
	@Override
	public HandlerList getHandlers() {
		return HANDLER_LIST;
	}
}
