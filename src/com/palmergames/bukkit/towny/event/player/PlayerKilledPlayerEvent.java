package com.palmergames.bukkit.towny.event.player;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import com.palmergames.bukkit.towny.object.Resident;

public class PlayerKilledPlayerEvent extends Event {

	private static final HandlerList handlers = new HandlerList();
	private Player killer;
	private Player victim;
	private Resident killerRes;
	private Resident victimRes;
	private Location location;
	
	/**
	 * An event fired from a EventPriority.MONITOR listener. 
	 * Do not use to un-kill someone or you're likely to cause 
	 * issues with other plugins.
	 * 
	 * @param killer - Player that killed the victim.
	 * @param victim - Player that died.
	 * @param killerRes - Resident that killed the victim.
	 * @param victimRes - Resident that died.
	 * @param location - Location of the player that died.
	 */
	public PlayerKilledPlayerEvent(Player killer, Player victim, Resident killerRes, Resident victimRes, Location location) {
		super(!Bukkit.getServer().isPrimaryThread());
		this.killer = killer;
		this.victim = victim;
		this.killerRes = killerRes;
		this.victimRes = victimRes;
		this.location = location;
	}

	@Override
	public HandlerList getHandlers() {
		return handlers;
	}

	public Player getKiller() {
		return killer;
	}

	public Player getVictim() {
		return victim;
	}

	public Resident getKillerRes() {
		return killerRes;
	}

	public Resident getVictimRes() {
		return victimRes;
	}
	public Location getLocation() {
		return location;
	}

}
