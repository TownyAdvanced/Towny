package com.palmergames.bukkit.towny.event.town.toggle;

import org.bukkit.command.CommandSender;
import com.palmergames.bukkit.towny.object.Town;

public class TownPreTogglePVPEvent extends TownPreToggleEvent {

	private final boolean state;
	
	public TownPreTogglePVPEvent(CommandSender sender, Town town, boolean admin) {
		super(sender, town, admin);
		state = town.isPVP();
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
