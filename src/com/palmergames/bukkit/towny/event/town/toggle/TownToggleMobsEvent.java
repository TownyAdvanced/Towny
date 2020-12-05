package com.palmergames.bukkit.towny.event.town.toggle;

import org.bukkit.command.CommandSender;
import com.palmergames.bukkit.towny.object.Town;

public class TownToggleMobsEvent extends TownToggleEvent {

	private final boolean state;
	
	public TownToggleMobsEvent(CommandSender sender, Town town, boolean admin) {
		super(sender, town, admin);
		state = town.hasMobs();
	}

	/**
	 * @return the toggle's new state.
	 */
	public boolean getNewState() {
		return state;
	}
	
}
