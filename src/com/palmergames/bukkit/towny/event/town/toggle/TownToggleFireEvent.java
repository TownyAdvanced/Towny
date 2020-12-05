package com.palmergames.bukkit.towny.event.town.toggle;

import org.bukkit.command.CommandSender;
import com.palmergames.bukkit.towny.object.Town;

public class TownToggleFireEvent extends TownToggleEvent {

	private final boolean state;
	
	public TownToggleFireEvent(CommandSender sender, Town town, boolean admin) {
		super(sender, town, admin);
		state = town.isFire();
	}

	/**
	 * @return the toggle's new state.
	 */
	public boolean getNewState() {
		return state;
	}
	
}
