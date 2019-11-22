package com.palmergames.bukkit.towny.event;

import com.palmergames.bukkit.towny.object.Nation;
import com.palmergames.bukkit.towny.object.Town;
import org.bukkit.Bukkit;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.List;

public class PreNationAddTownEvent extends Event implements Cancellable {

	private static final HandlerList handlers = new HandlerList();
	private String townName;
	private Town town;
	private String nationName;
	private Nation nation;
	private boolean isCancelled = false;
	private String cancelMessage = "Sorry this event was cancelled";
	private List<Town> towns;

	@Override
	public HandlerList getHandlers() {
		return handlers;
	}

	public static HandlerList getHandlerList() {
		return handlers;
	}

	public PreNationAddTownEvent(Nation nation, Town town) {
		super(!Bukkit.getServer().isPrimaryThread());
		this.townName = town.getName();
		this.town = town;
		this.nation = nation;
		this.nationName = nation.getName();
	}
	
	public PreNationAddTownEvent(Nation nation, List<Town> towns) {
		this.towns = towns;
		this.nation = nation;
		this.nationName = nation.getName();
	}

	public String getTownName() {
		return townName;
	}

	public String getNationName() {
		return nationName;
	}

	public Town getTown() {
		return town;
	}

	public Nation getNation() {
		return nation;
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
