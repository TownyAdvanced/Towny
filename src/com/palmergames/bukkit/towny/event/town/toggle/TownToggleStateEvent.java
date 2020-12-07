package com.palmergames.bukkit.towny.event.town.toggle;

import com.palmergames.bukkit.towny.object.Town;
import org.bukkit.command.CommandSender;

abstract class TownToggleStateEvent extends TownToggleEvent {
	private boolean newState, currState;
	
	TownToggleStateEvent(CommandSender sender, Town town, boolean admin, boolean currState, boolean newState) {
		super(sender, town, admin);
		this.currState = currState;
		this.newState = newState;
	}

	/**
	 * @return the current toggle's state.
	 */
	public boolean getCurrentState() {
		return currState;
	}

	/**
	 * @return the future state of the toggle after the event.
	 */
	public boolean getFutureState() {
		return newState;
	}
}
