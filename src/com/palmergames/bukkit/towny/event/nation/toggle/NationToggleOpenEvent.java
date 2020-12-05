package com.palmergames.bukkit.towny.event.nation.toggle;

import org.bukkit.command.CommandSender;
import com.palmergames.bukkit.towny.object.Nation;

public class NationToggleOpenEvent extends NationToggleEvent {

	private final boolean state;
	
	public NationToggleOpenEvent(CommandSender sender, Nation nation, boolean admin) {
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
