package com.palmergames.bukkit.towny.event.plot.group;

import com.palmergames.bukkit.towny.object.PlotGroup;
import com.palmergames.bukkit.towny.object.TownBlock;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

/**
 * Called when a plot group is created.
 */
public class PlotGroupCreatedEvent extends PlotGroupAddEvent {
	public PlotGroupCreatedEvent(PlotGroup group, TownBlock townBlock, Player player) {
		super(group, townBlock, player);
	}

	/**
	 * @return The initial townblock that this plot group is being created with.
	 */
	@Override
	@NotNull
	public TownBlock getTownBlock() {
		return super.getTownBlock();
	}
}
