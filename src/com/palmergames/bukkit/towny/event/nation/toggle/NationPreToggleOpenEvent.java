package com.palmergames.bukkit.towny.event.nation.toggle;

import org.bukkit.entity.Player;

import com.palmergames.bukkit.towny.object.Nation;

public class NationPreToggleOpenEvent extends NationPreToggleEvent {

	private final boolean state;
	
	public NationPreToggleOpenEvent(Player player, Nation nation) {
		super(player, nation);
		state = nation.isOpen();
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
