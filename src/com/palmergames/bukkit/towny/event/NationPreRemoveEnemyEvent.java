package com.palmergames.bukkit.towny.event;

import com.palmergames.bukkit.towny.object.Nation;
import org.bukkit.Bukkit;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class NationPreRemoveEnemyEvent extends Event implements Cancellable {

	private static final HandlerList handlers = new HandlerList();
	private boolean cancelled = false;
	private final String enemyName;
	private final Nation enemy;
	private final String nationName;
	private final Nation nation;
	private String cancelMessage = "Sorry this event was cancelled";

	@Override
	public HandlerList getHandlers() {
		return handlers;
	}

	public static HandlerList getHandlerList() {
		return handlers;
	}

	public NationPreRemoveEnemyEvent(Nation nation, Nation enemy) {
		super(!Bukkit.getServer().isPrimaryThread());
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

	@Override
	public boolean isCancelled() {
		return cancelled;
	}

	@Override
	public void setCancelled(boolean cancelled) {
		this.cancelled = cancelled;
	}

	public String getCancelMessage() {
		return cancelMessage;
	}

	public void setCancelMessage(String cancelMessage) {
		this.cancelMessage = cancelMessage;
	}
}
