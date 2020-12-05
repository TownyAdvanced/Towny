package com.palmergames.bukkit.towny.event.town.toggle;

import org.bukkit.entity.Player;

import com.palmergames.bukkit.towny.object.Town;

public class TownToggleFireEvent extends TownToggleEvent {

	private final boolean state;
	
	public TownToggleFireEvent(Player player, Town town) {
		super(player, town);
		state = town.isFire();
	}

	/**
	 * @return the toggle's new state.
	 */
	public boolean getNewState() {
		return state;
	}
	
}
