package com.palmergames.bukkit.towny.event;

import com.palmergames.bukkit.towny.TownySettings;
import com.palmergames.bukkit.towny.exceptions.NotRegisteredException;
import com.palmergames.bukkit.towny.exceptions.TownyException;
import com.palmergames.bukkit.towny.object.Nation;
import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.towny.object.Town;
import org.bukkit.Bukkit;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class TownMayorChangeEvent extends Event implements Cancellable {

	private static final HandlerList handlers = new HandlerList();
	private Town town;
	private Resident mayor;
	private boolean isCancelled = false;
	private String cancelMessage = "Sorry this event was cancelled";

	public TownMayorChangeEvent(Town town, Resident newMayor) {
		super(!Bukkit.getServer().isPrimaryThread());
		this.town = town;
		this.mayor = newMayor;
	}


	public static HandlerList getHandlerList() {
		return handlers;
	}

	@Override
	public HandlerList getHandlers() {
		return handlers;
	}

	public Town getTown() {
		return town;
	}

	public Resident getNewMayor() {
		return mayor;
	}

	@Override
	public boolean isCancelled() {
		return isCancelled;
	}

	@Override
	public void setCancelled(boolean cancelled) {
		isCancelled = cancelled;
	}

	public String getCancelMessage() {
		return cancelMessage;
	}

	public void setCancelMessage(String cancelMessage) {
		this.cancelMessage = cancelMessage;
	}
}
