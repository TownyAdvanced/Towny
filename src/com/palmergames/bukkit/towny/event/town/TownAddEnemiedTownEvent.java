package com.palmergames.bukkit.towny.event.town;

import com.palmergames.bukkit.towny.event.CancellableTownyEvent;
import com.palmergames.bukkit.towny.object.Town;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

public class TownAddEnemiedTownEvent extends CancellableTownyEvent {
	private static final HandlerList HANDLER_LIST = new HandlerList();
	
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

	public static HandlerList getHandlerList() {
		return HANDLER_LIST;
	}

	@NotNull
	@Override
	public HandlerList getHandlers() {
		return HANDLER_LIST;
	}
}
