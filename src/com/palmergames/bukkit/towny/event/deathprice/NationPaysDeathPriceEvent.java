package com.palmergames.bukkit.towny.event.deathprice;

import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;

import com.palmergames.bukkit.towny.object.Nation;
import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.towny.object.economy.Account;

public class NationPaysDeathPriceEvent extends DeathPriceEvent {

	protected final Nation nation;
	private static final HandlerList handlers = new HandlerList();

	public NationPaysDeathPriceEvent(Account payer, double amount, Resident deadResident, Player killer, Nation nation) {
		super(payer, amount, deadResident, killer);
		this.nation = nation;
	}

	/**
	 * @return the nation
	 */
	public Nation getNation() {
		return nation;
	}

	public static HandlerList getHandlerList() {
		return handlers;
	}

	public HandlerList getHandlers() {
		return handlers;
	}
}
