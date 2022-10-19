package com.palmergames.bukkit.towny.event.town;

import com.palmergames.bukkit.towny.event.CancellableTownyEvent;
import com.palmergames.bukkit.towny.object.Town;

public class TownRemoveEnemiedTownEvent extends CancellableTownyEvent {
	
	private final Town town;
	private final Town removedEnemy;
	
	public TownRemoveEnemiedTownEvent(Town town, Town newAlly) {
		this.town = town;
		this.removedEnemy = newAlly;
		setCancelMessage("A town removing an another town as an enemy was cancelled by another plugin.");
	}

	/**
	 * @return town Town which is removing an enemy.
	 */
	public Town getTown() {
		return town;
	}

	/**
	 * @return removedEnemy Town which is being removed as an enemy.
	 */
	public Town getRemovedEnemy() {
		return removedEnemy;
	}
}
