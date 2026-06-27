package com.palmergames.bukkit.towny.event.town;

import com.palmergames.bukkit.towny.object.Town;
import com.palmergames.bukkit.towny.object.comparators.ComparatorType;
import org.bukkit.Bukkit;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class DisplayedTownsListSortEvent extends Event {
	private static final HandlerList handlers = new HandlerList();
	private List<Town> towns;
	private final ComparatorType comparatorType;

	public DisplayedTownsListSortEvent(List<Town> towns, ComparatorType comparatorType) {
		super(!Bukkit.getServer().isPrimaryThread());
		this.towns = towns;
		this.comparatorType = comparatorType;
	}

	@NotNull
	@Override
	public HandlerList getHandlers() {
		return handlers;
	}

	public static HandlerList getHandlerList() {
		return handlers;
	}

	public List<Town> getTowns() {
		return towns;
	}

	public void setTowns(List<Town> towns) {
		this.towns = towns;
	}

	public ComparatorType getComparatorType() {
		return comparatorType;
	}
}
