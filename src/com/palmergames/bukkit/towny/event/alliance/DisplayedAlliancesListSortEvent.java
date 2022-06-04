package com.palmergames.bukkit.towny.event.alliance;

import com.palmergames.bukkit.towny.object.Alliance;
import com.palmergames.bukkit.towny.object.comparators.ComparatorType;
import org.bukkit.Bukkit;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import java.util.List;

public class DisplayedAlliancesListSortEvent extends Event {
	private static final HandlerList handlers = new HandlerList();
	private List<Alliance> alliances;
	private final ComparatorType comparatorType;

	public DisplayedAlliancesListSortEvent(List<Alliance> alliances, ComparatorType comparatorType) {
		super(!Bukkit.getServer().isPrimaryThread());
		this.alliances = alliances;
		this.comparatorType = comparatorType;
	}

	@Override
	public HandlerList getHandlers() {
		return handlers;
	}

	public static HandlerList getHandlerList() {
		return handlers;
	}

	public List<Alliance> getAlliances() {
		return alliances;
	}

	public void setAlliances(List<Alliance> alliances) {
		this.alliances = alliances;
	}

	public ComparatorType getComparatorType() {
		return comparatorType;
	}
}
