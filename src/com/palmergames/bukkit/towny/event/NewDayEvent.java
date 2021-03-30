package com.palmergames.bukkit.towny.event;

import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class NewDayEvent extends Event{
	private static final HandlerList handlers = new HandlerList();
	private final List<String> bankruptTowns;
	private final List<String> fallenTowns;
	private final List<String> fallenNations;
	private final double townUpkeepCollected;
	private final double nationUpkeepCollected;
	private final long time;

	/**
	 * Event thrown after the upkeep and taxes are collected on each Towny New Day.
	 * 
	 * @param bankruptTowns - Towns that went bankrupt.
	 * @param removedTowns - Towns that were deleted.
	 * @param fallenNations - Nations that were deleted.
	 * @param townUpkeepCollected - Total amount of upkeep paid by towns.
	 * @param nationUpkeepCollected - Total amount of upkeep paid by nations.
	 * @param time - The system time in milliseconds when the new day code started.
	 */
	public NewDayEvent(final List<String> bankruptTowns, final List<String> removedTowns, final List<String> fallenNations, final double townUpkeepCollected, final double nationUpkeepCollected, final long time) {
		super(!Bukkit.getServer().isPrimaryThread());
		this.bankruptTowns = bankruptTowns;
		this.fallenTowns = removedTowns;		
		this.fallenNations = fallenNations;
		this.townUpkeepCollected = townUpkeepCollected;
		this.nationUpkeepCollected = nationUpkeepCollected;
		this.time = time;
	}
	
	@Override
	public HandlerList getHandlers() {
		return handlers;
	}

	public static HandlerList getHandlerList() {
		return handlers;
	}

	public List<String> getBankruptedTowns() {
		return bankruptTowns;
	}
	
	public List<String> getFallenTowns() {
		return fallenTowns;
	}

	public List<String> getFallenNations() {
		return fallenNations;
	}

	public double getTownUpkeepCollected() {
		return townUpkeepCollected;
	}

	public double getNationUpkeepCollected() {
		return nationUpkeepCollected;
	}
	
	/**
	 * To turn this into something you could display use the following:
	 * 
	 * timeFormat = new SimpleDateFormat("MMMMM dd '@' HH:mm")
	 * timeFormat.format(event.getTime())
	 * 
	 * @return time 
	 */
	public long getTime() {
		return time;
	}

}
