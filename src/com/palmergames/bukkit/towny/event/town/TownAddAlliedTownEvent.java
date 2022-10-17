package com.palmergames.bukkit.towny.event.town;

import com.palmergames.bukkit.towny.event.CancellableTownyEvent;
import com.palmergames.bukkit.towny.object.Town;

public class TownAddAlliedTownEvent extends CancellableTownyEvent {

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
}
