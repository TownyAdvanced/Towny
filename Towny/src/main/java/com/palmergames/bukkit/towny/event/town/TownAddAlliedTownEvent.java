package com.palmergames.bukkit.towny.event.town;

import com.palmergames.bukkit.towny.event.CancellableTownyEvent;
import com.palmergames.bukkit.towny.object.Town;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

public class TownAddAlliedTownEvent extends CancellableTownyEvent {
	private static final HandlerList HANDLER_LIST = new HandlerList();

	private final Town town;
	private final Town newAlly;

	public TownAddAlliedTownEvent(Town town, Town newAlly) {
		this.town = town;
		this.newAlly = newAlly;
		setCancelMessage("A town alliance was cancelled by another plugin.");
	}

	/**
	 * @return town Town which is receiving a new ally.
	 */
	public Town getTown() {
		return town;
	}

	/**
	 * @return newAlly Town which is being added as a new ally.
	 */
	public Town getNewAlly() {
		return newAlly;
	}

	public static HandlerList getHandlerList() {
		return HANDLER_LIST;
	}

	@NotNull
	@Override
	public HandlerList getHandlers() {
		return HANDLER_LIST;
	}
}
