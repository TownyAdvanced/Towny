package com.palmergames.bukkit.towny.event;

import com.palmergames.bukkit.towny.object.Nation;
import org.bukkit.Bukkit;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class NationRemoveEnemyEvent extends Event {

	private static final HandlerList handlers = new HandlerList();

	private final Nation enemy;
	private final Nation nation;

	@Override
	public HandlerList getHandlers() {
		return handlers;
	}

	public static HandlerList getHandlerList() {
		return handlers;
	}

	public NationRemoveEnemyEvent(Nation nation, Nation enemy) {
		super(!Bukkit.getServer().isPrimaryThread());
		this.enemy = enemy;
		this.nation = nation;
	}

	/**
	 *
	 * @return the nation that added the enemy.
	 */
	public Nation getNation() {
		return nation;
	}

	/**
	 *
	 * @return the nation that is now an enemy.
	 */
	public Nation getEnemy() {
		return enemy;
	}
}
