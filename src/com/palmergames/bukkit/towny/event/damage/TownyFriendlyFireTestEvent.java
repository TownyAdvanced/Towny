package com.palmergames.bukkit.towny.event.damage;

import com.palmergames.bukkit.towny.object.TownyWorld;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class TownyFriendlyFireTestEvent extends Event {

	private static final HandlerList handlers = new HandlerList();
	
	private final Player attacker;
	private final Player defender;
	private final TownyWorld world;
	
	private boolean pvp;

	public TownyFriendlyFireTestEvent(Player attacker, Player defender, TownyWorld world, boolean pvp) {
		this.attacker = attacker;
		this.defender = defender;
		this.world = world;
		
		this.pvp = pvp;
	}

	public static HandlerList getHandlerList() {
		return handlers;
	}

	public HandlerList getHandlers() {
		return handlers;
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

	public boolean isPvp() {
		return pvp;
	}

	public void setPvp(boolean pvp) {
		this.pvp = pvp;
	}
}
