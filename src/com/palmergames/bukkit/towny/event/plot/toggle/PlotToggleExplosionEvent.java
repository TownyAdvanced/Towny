package com.palmergames.bukkit.towny.event.plot.toggle;

import com.palmergames.bukkit.towny.object.Town;
import org.bukkit.entity.Player;

public class PlotToggleExplosionEvent extends PlotToggleEvent {

	public PlotToggleExplosionEvent(Town town, Player player, boolean futureState) {
		super(town, player, futureState);
	}
}
