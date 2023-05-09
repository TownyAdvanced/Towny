package com.palmergames.bukkit.towny.event;

import com.palmergames.bukkit.towny.object.Nation;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

public class NationPreRemoveEnemyEvent extends CancellableTownyEvent {
	private static final HandlerList HANDLER_LIST = new HandlerList();

	private final String enemyName;
	private final Nation enemy;
	private final String nationName;
	private final Nation nation;

	public NationPreRemoveEnemyEvent(Nation nation, Nation enemy) {
		this.enemyName = enemy.getName();
		this.enemy = enemy;
		this.nation = nation;
		this.nationName = nation.getName();
	}

	public String getEnemyName() {
		return enemyName;
	}

	public String getNationName() {
		return nationName;
	}

	public Nation getEnemy() {
		return enemy;
	}

	public Nation getNation() {
		return nation;
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
