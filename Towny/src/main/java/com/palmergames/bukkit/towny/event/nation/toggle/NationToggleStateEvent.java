package com.palmergames.bukkit.towny.event.nation.toggle;

import com.palmergames.bukkit.towny.object.Nation;
import org.bukkit.command.CommandSender;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

abstract class NationToggleStateEvent extends NationToggleEvent {
	private static final HandlerList HANDLER_LIST = new HandlerList();

	private final boolean oldState;
	private final boolean newState;

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
	
	public static HandlerList getHandlerList() {
		return HANDLER_LIST;
	}

	@Override
	public @NotNull HandlerList getHandlers() {
		return HANDLER_LIST;
	}
}
