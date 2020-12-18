package com.palmergames.bukkit.towny.event.plot.toggle;

import com.palmergames.bukkit.towny.object.Town;
import org.bukkit.entity.Player;

public class PlotToggleMobsEvent extends PlotToggleEvent{

	public PlotToggleMobsEvent(Town town, Player player, boolean futureState) {
		super(town, player, futureState);
	}
	
}
