package com.palmergames.bukkit.towny.event.deathprice;

import org.bukkit.entity.Player;
import com.palmergames.bukkit.towny.object.Nation;
import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.towny.object.economy.Account;

public class NationPaysDeathPriceEvent extends DeathPriceEvent {

	protected final Nation nation;

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
}
