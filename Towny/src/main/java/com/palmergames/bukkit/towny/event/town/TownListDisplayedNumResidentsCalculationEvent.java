package com.palmergames.bukkit.towny.event.town;

import org.bukkit.Bukkit;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import com.palmergames.bukkit.towny.object.Town;

/**
 * Used to alter how many residents a town appears to have in the /town list pages.
 * 
 * @author LlmDl
 * @since 0.100.1.8.
 */
public class TownListDisplayedNumResidentsCalculationEvent extends Event {
	private static final HandlerList handlers = new HandlerList();
	private int displayedValue;
	private final Town town;
	
	public TownListDisplayedNumResidentsCalculationEvent(int displayedValue, Town town) {
		super(!Bukkit.getServer().isPrimaryThread());
		this.displayedValue = displayedValue;
		this.town = town;
	}

	public Town getTown() {
		return town;
	}

	public void setDisplayedValue(int value) {
		this.displayedValue = value;
	}

	public int getDisplayedValue() {
		return displayedValue;
	}

	@Override
	public HandlerList getHandlers() {
		return handlers;
	}

	public static HandlerList getHandlerList() {
		return handlers;
	}
}
