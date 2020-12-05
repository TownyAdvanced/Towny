package com.palmergames.bukkit.towny.event.nation.toggle;

import org.bukkit.entity.Player;

import com.palmergames.bukkit.towny.object.Nation;

public class NationTogglePublicEvent extends NationToggleEvent {

	private final boolean state;
	
	public NationTogglePublicEvent(Player player, Nation nation) {
		super(player, nation);
		state = nation.isNeutral();
	}
	
	/**
	 * @return the toggle's new state.
	 */
	public boolean getNewState() {
		return state;
	}

}
