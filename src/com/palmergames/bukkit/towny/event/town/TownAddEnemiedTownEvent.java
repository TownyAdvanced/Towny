package com.palmergames.bukkit.towny.event.town;

import com.palmergames.bukkit.towny.event.CancellableTownyEvent;
import com.palmergames.bukkit.towny.object.Town;

public class TownAddEnemiedTownEvent extends CancellableTownyEvent {
	
	private final Town town;
	private final Town newEnemy;
	
	public TownAddEnemiedTownEvent(Town town, Town newAlly) {
		this.town = town;
		this.newEnemy = newAlly;
		setCancelMessage("A town enemying another town was cancelled by another plugin.");
	}

	/**
	 * @return town Town which is receiving a new enemy.
	 */
	public Town getTown() {
		return town;
	}

	/**
	 * @return newEnemy Town which is being added as a new enemy.
	 */
	public Town getNewEnemy() {
		return newEnemy;
	}
}
