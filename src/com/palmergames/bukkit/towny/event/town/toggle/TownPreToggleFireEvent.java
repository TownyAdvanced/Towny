package com.palmergames.bukkit.towny.event.town.toggle;

import org.bukkit.command.CommandSender;
import com.palmergames.bukkit.towny.object.Town;

public class TownPreToggleFireEvent extends TownPreToggleEvent {

	private final boolean state;
	
	public TownPreToggleFireEvent(CommandSender sender, Town town, boolean admin) {
		super(sender, town, admin);
		state = town.isFire();
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
