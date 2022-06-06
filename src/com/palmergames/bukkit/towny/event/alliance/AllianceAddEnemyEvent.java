package com.palmergames.bukkit.towny.event.alliance;

import com.palmergames.bukkit.towny.object.Alliance;
import com.palmergames.bukkit.towny.object.Nation;

import org.bukkit.Bukkit;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class AllianceAddEnemyEvent extends Event {
	
	private static final HandlerList handlers = new HandlerList();

	private final Nation enemy;
	private final Alliance alliance;
	
	@Override
	public HandlerList getHandlers() {
		return handlers;
	}
	
	public static HandlerList getHandlerList() {
		return handlers;
	}
	
	public AllianceAddEnemyEvent(Alliance alliance, Nation enemy) {
		super(!Bukkit.getServer().isPrimaryThread());
		this.enemy = enemy;
		this.alliance = alliance;
	}

	/**
	 *
	 * @return the alliance that added the enemy.
	 */
	public Alliance getAlliance() {
		return alliance;
	}

	/**
	 *
	 * @return the nation that is now an enemy.
	 */
	public Nation getEnemy() {
		return enemy;
	}
}
