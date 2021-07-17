package com.palmergames.bukkit.towny.event.nation;

import com.palmergames.bukkit.towny.object.Nation;
import com.palmergames.bukkit.towny.object.comparators.ComparatorType;
import org.bukkit.Bukkit;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import java.util.List;

public class DisplayedNationsListSortEvent extends Event {
	private static final HandlerList handlers = new HandlerList();
	private List<Nation> nations;
	private final ComparatorType comparatorType;

	public DisplayedNationsListSortEvent(List<Nation> nations, ComparatorType comparatorType) {
		super(!Bukkit.getServer().isPrimaryThread());
		this.nations = nations;
		this.comparatorType = comparatorType;
	}

	@Override
	public HandlerList getHandlers() {
		return handlers;
	}

	public static HandlerList getHandlerList() {
		return handlers;
	}

	public List<Nation> getNations() {
		return nations;
	}

	public void setNations(List<Nation> nations) {
		this.nations = nations;
	}

	public ComparatorType getComparatorType() {
		return comparatorType;
	}
}
