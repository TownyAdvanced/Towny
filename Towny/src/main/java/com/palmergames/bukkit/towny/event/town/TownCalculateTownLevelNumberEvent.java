package com.palmergames.bukkit.towny.event.town;

import com.palmergames.bukkit.towny.TownySettings;
import com.palmergames.bukkit.towny.object.Town;

import org.bukkit.Bukkit;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.ApiStatus;

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
	private static final HandlerList HANDLER_LIST = new HandlerList();

	private final Town town;
	private int townLevelNumber;
	private final int modifier;

	@ApiStatus.Internal
	public TownCalculateTownLevelNumberEvent(Town town, int predeterminedLevelNumber, int modifier) {
		super(!Bukkit.getServer().isPrimaryThread());
		this.town = town;
		this.townLevelNumber = predeterminedLevelNumber;
		this.modifier = modifier;
	}

	/**
	 * {@return the town which is having their TownLevel number calculated}
	 */
	public Town getTown() {
		return town;
	}

	public void setTownLevelNumber(int value) {
		this.townLevelNumber = value;
	}

	public int getTownLevelNumber() {
		return townLevelNumber;
	}

	/**
	 * {@return the modifier which is being used to calculate the town level, typically the resident count}
	 */
	public int getModifier() {
		return modifier;
	}

	@Override
	public HandlerList getHandlers() {
		return HANDLER_LIST;
	}

	public static HandlerList getHandlerList() {
		return HANDLER_LIST;
	}
}
