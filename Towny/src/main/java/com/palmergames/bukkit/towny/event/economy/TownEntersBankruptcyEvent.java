package com.palmergames.bukkit.towny.event.economy;

import org.bukkit.Bukkit;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import com.palmergames.bukkit.towny.object.Town;
import com.palmergames.bukkit.towny.object.economy.BankAccount;

/**
 * An event thrown when a {@link Town} becomes bankrupt.
 */
public class TownEntersBankruptcyEvent extends Event {
	private final Town town;
	private static final HandlerList handlers = new HandlerList();

	/**
	 * An event thrown when a {@link Town} becomes bankrupt.
	 * 
	 * @param town {@link Town} who went bankrupt.
	 */
	public TownEntersBankruptcyEvent(Town town) {
		super(!Bukkit.getServer().isPrimaryThread());
		this.town = town;
	}

	/**
	 * @return {@link town}
	 */
	public Town getTown() {
		return town;
	}

	/**
	 * @return {@link BankAccount} belonging to the town.
	 */
	public BankAccount getTownBankAccount() {
		return town.getAccount();
	}

	@Override
	public HandlerList getHandlers() {
		return handlers;
	}

	public static HandlerList getHandlerList() {
		return handlers;
	}
}
