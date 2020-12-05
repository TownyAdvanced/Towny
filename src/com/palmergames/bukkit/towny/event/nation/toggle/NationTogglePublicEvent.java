package com.palmergames.bukkit.towny.event.nation.toggle;

import org.bukkit.command.CommandSender;
import com.palmergames.bukkit.towny.object.Nation;

public class NationTogglePublicEvent extends NationToggleEvent {

	private final boolean state;
	
	public NationTogglePublicEvent(CommandSender sender, Nation nation, boolean admin) {
		super(sender, nation, admin);
		state = nation.isNeutral();
	}
	
	/**
	 * @return the toggle's new state.
	 */
	public boolean getNewState() {
		return state;
	}

}
