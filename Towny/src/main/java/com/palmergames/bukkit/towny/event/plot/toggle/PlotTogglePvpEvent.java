package com.palmergames.bukkit.towny.event.plot.toggle;

import com.palmergames.bukkit.towny.object.TownBlock;
import org.bukkit.entity.Player;

public class PlotTogglePvpEvent extends PlotToggleEvent {
	
	public PlotTogglePvpEvent(TownBlock townBlock, Player player, boolean futureState) {
		super(townBlock, player, futureState);
	}
}
