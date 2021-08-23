package com.palmergames.bukkit.towny.event.damage;

import com.palmergames.bukkit.towny.object.TownyWorld;
import com.palmergames.bukkit.towny.utils.CombatUtil;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class TownyFriendlyFireTestEvent extends Event {

	private static final HandlerList handlers = new HandlerList();

	private final Player attacker;
	private final Player defender;
	private final TownyWorld world;
	private final Relationship relationship;

	private boolean pvp;

	public TownyFriendlyFireTestEvent(Player attacker, Player defender, TownyWorld world, Relationship relationship) {
		this.attacker = attacker;
		this.defender = defender;
		this.world = world;
		this.relationship = relationship;

		if (world.isFriendlyFireEnabled() || CombatUtil.isArenaPlot(attacker, defender)) {
			pvp = true;
		} else {
			switch (relationship) {
				case TOWN:
				case NATION:
				case ALLY:
					pvp = false;
					break;
				case NEUTRAL:
				case ENEMY:
					pvp = true;
					break;
			}
		}
	}

	public Player getAttacker() {
		return attacker;
	}

	public Player getDefender() {
		return defender;
	}

	public TownyWorld getWorld() {
		return world;
	}

	public Relationship getRelationship() {
		return relationship;
	}

	public boolean isPvp() {
		return pvp;
	}

	public void setPvp(boolean pvp) {
		this.pvp = pvp;
	}

	public static HandlerList getHandlerList() {
		return handlers;
	}

	public HandlerList getHandlers() {
		return handlers;
	}

	public enum Relationship {
		TOWN,
		NATION,
		ALLY,
		NEUTRAL,
		ENEMY
	}
}
