package com.palmergames.bukkit.towny.event.deathprice;

import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;

import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.towny.object.Town;
import com.palmergames.bukkit.towny.object.economy.Account;

public class TownPaysDeathPriceEvent extends DeathPriceEvent {

	protected final Town town;
	private static final HandlerList handlers = new HandlerList();

	public TownPaysDeathPriceEvent(Account payer, double amount, Resident deadResident, Player killer, Town town) {
		super(payer, amount, deadResident, killer);
		this.town = town;
	}

	/**
	 * @return the town
	 */
	public Town getTown() {
		return town;
	}

	public static HandlerList getHandlerList() {
		return handlers;
	}

	public HandlerList getHandlers() {
		return handlers;
	}
}
