package com.palmergames.bukkit.towny.event.alliance;

import com.palmergames.bukkit.towny.object.Alliance;
import org.bukkit.Bukkit;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public abstract class AllianceListDisplayedValueCalculationEvent extends Event {
	private static final HandlerList handlers = new HandlerList();
	private int displayedValue;
	private final Alliance alliance;

	public AllianceListDisplayedValueCalculationEvent(Alliance alliance, int displayedValue) {
		super(!Bukkit.getServer().isPrimaryThread());
		this.alliance = alliance;
		this.displayedValue = displayedValue;
	}

	public Alliance getAlliance() {
		return alliance;
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
