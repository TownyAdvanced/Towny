package com.palmergames.bukkit.towny.event.town.toggle;

import org.bukkit.entity.Player;

import com.palmergames.bukkit.towny.object.Town;

public class TownPreToggleMobsEvent extends TownPreToggleEvent {

	private final boolean state;
	
	public TownPreToggleMobsEvent(Player player, Town town) {
		super(player, town);
		state = town.hasMobs();
	}

	/**
	 * @return the current toggle's state.
	 */
	public boolean getCurrentState() {
		return state;
	}
	
	/**
	 * @return the future state of the toggle after the event.
	 */
	public boolean getFutureState() {
		return !state;
	}

}
