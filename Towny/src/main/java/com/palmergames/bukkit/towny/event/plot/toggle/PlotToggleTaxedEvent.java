package com.palmergames.bukkit.towny.event.plot.toggle;

import org.bukkit.entity.Player;

import com.palmergames.bukkit.towny.object.TownBlock;

public class PlotToggleTaxedEvent extends PlotToggleEvent {

	public PlotToggleTaxedEvent(TownBlock townBlock, Player player, boolean futureState) {
		super(townBlock, player, futureState);
	}
}
