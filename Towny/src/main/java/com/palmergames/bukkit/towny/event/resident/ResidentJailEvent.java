package com.palmergames.bukkit.towny.event.resident;

import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.towny.object.Town;
import com.palmergames.bukkit.towny.object.jail.Jail;
import com.palmergames.bukkit.towny.object.jail.JailReason;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ResidentJailEvent extends Event {

	private static final HandlerList handlers = new HandlerList();
	private final Resident resident;
	private final JailReason reason;
	private final Player sender;
	
	public ResidentJailEvent(Resident resident, JailReason reason, Player sender){

		this.resident = resident;
		this.reason = reason;
		this.sender = sender;
	}
	
	@NotNull
	@Override
	public HandlerList getHandlers() {
		return handlers;
	}

	public static HandlerList getHandlerList() {
		return handlers;
	}

	public Resident getResident() {
		return resident;
	}
	
	public Jail getJail() {
		return resident.getJail();
	}
	
	public int getJailCell() {
		return resident.getJailCell();
	}
	
	public int getJailHours() {
		return resident.getJailHours();
	}
	
	public Town getJailTown() {
		return getJail().getTown();
	}

	public String getJailTownName() {
		return getJailTown().getName();
	}

	public Location getJailSpawnLocation() {
		return resident.getJailSpawn();
	}

	public double getBailAmount() {
		return resident.getJailBailCost();
	}

	public JailReason getJailReason() {
		return reason;
	}

	/**
	 * Returns the Player that jailed the resident via jail command, or via killing them as an outlaw or prisoner of war.
	 * @return player who jailed the resident, or null if they were jailed by an admin.
	 */
	@Nullable
	public Player getPlayerThatSentResidentToJail() {
		return sender;
	}
}
