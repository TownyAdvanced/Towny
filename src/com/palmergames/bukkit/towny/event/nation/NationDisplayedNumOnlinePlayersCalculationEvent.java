package com.palmergames.bukkit.towny.event.nation;

import com.palmergames.bukkit.towny.object.Nation;
import org.bukkit.Bukkit;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class NationDisplayedNumOnlinePlayersCalculationEvent extends Event {
	private static final HandlerList handlers = new HandlerList();
	private int displayedNumOnlinePlayers;
	private final Nation nation;

	public NationDisplayedNumOnlinePlayersCalculationEvent(Nation nation, int displayedNumOnlinePlayers) {
		super(!Bukkit.getServer().isPrimaryThread());
		this.nation = nation;
		this.displayedNumOnlinePlayers = displayedNumOnlinePlayers;
	}

	public Nation getNation() {
		return nation;
	}

	public void setDisplayedNumOnlinePlayers(int value) {
		this.displayedNumOnlinePlayers = value;
	}

	public int getDisplayedNumOnlinePlayers() {
		return displayedNumOnlinePlayers;
	}

	@Override
	public HandlerList getHandlers() {
		return handlers;
	}

	public static HandlerList getHandlerList() {
		return handlers;
	}
}
