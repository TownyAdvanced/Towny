package com.palmergames.bukkit.towny.event.town;

import com.palmergames.bukkit.towny.TownySettings;
import com.palmergames.bukkit.towny.object.Town;

import org.bukkit.Bukkit;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

/**
 * An event that is thrown which allows other plugins to alter a Town's
 * TownLevel Number. ie: 1 to {@link TownySettings#getTownLevelMax()}. This is
 * used as a key to determine which TownLevel a Town receives, and ultimately
 * which attributes that Town will receive.
 * 
 * @author LlmDl
 * @since 0.99.6.3
 */
public class TownCalculateTownLevelNumberEvent extends Event {
	private static final HandlerList handlers = new HandlerList();
	private int townLevelNumber;
	private final Town town;

	/**
	 * An event that is thrown which allows other plugins to alter a Town's
	 * TownLevel Number. ie: 1 to {@link TownySettings#getTownLevelMax()}. This is
	 * used as a key to determine which TownLevel a Town receives, and ultimately
	 * which attributes that Town will receive.
	 * 
	 * @param town                     Town which is having their TownLevel number
	 *                                 calculated.
	 * @param predeterminedLevelNumber The number which Towny has already assigned
	 *                                 to the town.
	 */
	public TownCalculateTownLevelNumberEvent(Town town, int predeterminedLevelNumber) {
		super(!Bukkit.getServer().isPrimaryThread());
		this.town = town;
		this.townLevelNumber = predeterminedLevelNumber;
	}

	public Town getTown() {
		return town;
	}

	public void setTownLevelNumber(int value) {
		this.townLevelNumber = value;
	}

	public int getTownLevelNumber() {
		return townLevelNumber;
	}

	@Override
	public HandlerList getHandlers() {
		return handlers;
	}

	public static HandlerList getHandlerList() {
		return handlers;
	}
}
