package com.palmergames.bukkit.towny.event;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

/**
 * Deprecated as of 0.96.4.2. Use {@link com.palmergames.bukkit.towny.event.damage.TownyPlayerDamagePlayerEvent} instead.
 */
@Deprecated
public class DisallowedPVPEvent extends Event implements Cancellable {
	private static final HandlerList handlers = new HandlerList();

	private boolean cancelled = false;
	private final Player attacker;
	private final Player defender;

	public DisallowedPVPEvent(final Player attacker, final Player defender) {
		super(!Bukkit.getServer().isPrimaryThread());
		this.attacker = attacker;
		this.defender = defender;
	}

	public boolean isCancelled() {
		return cancelled;
	}

	public void setCancelled(boolean cancelled) {
		this.cancelled = cancelled;
	}

	public Player getAttacker() {
		return attacker;
	}

	public Player getDefender() {
		return defender;
	}

	@Override
	public HandlerList getHandlers() {
		return handlers;
	}

	public static HandlerList getHandlerList() {
		return handlers;
	}
}
