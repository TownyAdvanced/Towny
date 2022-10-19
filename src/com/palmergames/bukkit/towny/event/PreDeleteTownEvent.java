package com.palmergames.bukkit.towny.event;

import com.palmergames.bukkit.towny.object.Town;

public class PreDeleteTownEvent extends CancellableTownyEvent {
	private final String townName;
	private final Town town;

	public PreDeleteTownEvent(Town town) {
		this.townName = town.getName();
		this.town = town;
	}

	/**
	 * @return the deleted town name.
	 */
	public String getTownName() {
		return townName;
	}

	/**
	 * @return the deleted town object.
	 */
	public Town getTown() {
		return town;
	}
}
