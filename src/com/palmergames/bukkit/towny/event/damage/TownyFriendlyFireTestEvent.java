package com.palmergames.bukkit.towny.event.damage;

import com.palmergames.bukkit.towny.object.TownyWorld;
import com.palmergames.bukkit.towny.object.Translatable;

import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class TownyFriendlyFireTestEvent extends Event {

	private static final HandlerList handlers = new HandlerList();
	
	private final Player attacker;
	private final Player defender;
	private final TownyWorld world;
	private String cancelledMessage;
	private boolean pvp;

	public TownyFriendlyFireTestEvent(Player attacker, Player defender, TownyWorld world) { 
		this.attacker = attacker;
		this.defender = defender;
		this.world = world;
		
		this.pvp = false;
		this.setCancelledMessage(Translatable.of("msg_err_friendly_fire_disable").forLocale(attacker));
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

	public boolean isPVP() {
		return pvp;
	}

	public void setPVP(boolean pvp) {
		this.pvp = pvp;
	}

	public String getCancelledMessage() {
		return cancelledMessage;
	}

	public void setCancelledMessage(String cancelledMessage) {
		this.cancelledMessage = cancelledMessage;
	}
}
