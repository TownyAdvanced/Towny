package com.palmergames.bukkit.towny.event.nation.toggle;

import org.bukkit.entity.Player;

import com.palmergames.bukkit.towny.object.Nation;

public class NationPreToggleNeutralEvent extends NationPreToggleEvent {

	private final boolean state;
	
	public NationPreToggleNeutralEvent(Player player, Nation nation) {
		super(player, nation);
		state = nation.isNeutral();
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
