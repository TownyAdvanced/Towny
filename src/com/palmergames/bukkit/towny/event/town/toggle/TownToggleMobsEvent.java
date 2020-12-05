package com.palmergames.bukkit.towny.event.town.toggle;

import org.bukkit.entity.Player;

import com.palmergames.bukkit.towny.object.Town;

public class TownToggleMobsEvent extends TownToggleEvent {

	private final boolean state;
	
	public TownToggleMobsEvent(Player player, Town town) {
		super(player, town);
		state = town.hasMobs();
	}

	/**
	 * @return the toggle's new state.
	 */
	public boolean getNewState() {
		return state;
	}
	
}
