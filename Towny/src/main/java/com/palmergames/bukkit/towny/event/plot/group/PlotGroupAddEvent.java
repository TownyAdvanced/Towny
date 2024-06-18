package com.palmergames.bukkit.towny.event.plot.group;

import com.palmergames.bukkit.towny.event.CancellableTownyEvent;
import com.palmergames.bukkit.towny.object.PlotGroup;
import com.palmergames.bukkit.towny.object.TownBlock;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

/**
 * Called when a townblock is added into a plot group
 */
public class PlotGroupAddEvent extends CancellableTownyEvent {
	private static final HandlerList HANDLER_LIST = new HandlerList();
	private final PlotGroup plotGroup;
	private final TownBlock townBlock;
	private final Player player;
	
	public PlotGroupAddEvent(final PlotGroup group, final TownBlock townBlock, final Player player) {
		this.plotGroup = group;
		this.townBlock = townBlock;
		this.player = player;
	}
	
	@NotNull
	public PlotGroup getPlotGroup() {
		return plotGroup;
	}
	
	@NotNull
	public TownBlock getTownBlock() {
		return townBlock;
	}
	
	@NotNull
	public Player getPlayer() {
		return player;
	}
	
	@NotNull
	public static HandlerList getHandlerList() {
		return HANDLER_LIST;
	}

	@NotNull
	@Override
	public HandlerList getHandlers() {
		return HANDLER_LIST;
	}
}
