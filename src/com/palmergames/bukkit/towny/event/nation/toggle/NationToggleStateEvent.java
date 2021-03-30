package com.palmergames.bukkit.towny.event.nation.toggle;

import com.palmergames.bukkit.towny.object.Nation;
import org.bukkit.command.CommandSender;

abstract class NationToggleStateEvent extends NationToggleEvent {

	private boolean oldState, newState;

	public NationToggleStateEvent(CommandSender sender, Nation nation, boolean admin, boolean oldState, boolean newState) {
		super(sender, nation, admin);
		this.oldState = oldState;
		this.newState = newState;
	}

	/**
	 * @return the current toggle's state.
	 */
	public boolean getCurrentState() {
		return oldState;
	}

	/**
	 * @return the future state of the toggle after the event.
	 */
	public boolean getFutureState() {
		return newState;
	}
	
}
