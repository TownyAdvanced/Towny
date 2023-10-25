package com.palmergames.bukkit.towny.event.time.dailytaxes;

import com.palmergames.bukkit.towny.event.CancellableTownyEvent;
import com.palmergames.bukkit.towny.object.Nation;
import com.palmergames.bukkit.towny.object.Town;
import com.palmergames.bukkit.towny.object.Translation;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

public class PreTownPaysNationTaxEvent extends CancellableTownyEvent {
	private static final HandlerList HANDLER_LIST = new HandlerList();

	private final Town town;
	private final Nation nation;
	private double tax;

	/**
	 * Cancellable event that precedes a town paying the nation tax.
	 * 
	 * @param town Town about to pay tax.
	 * @param nation Nation about to have tax paid to it.
	 * @param tax the amount the town will pay.
	 */
	public PreTownPaysNationTaxEvent(Town town, Nation nation, double tax) {
		this.town = town;
		this.nation = nation;
		this.tax = tax;
		setCancelMessage(Translation.of("msg_your_town_was_exempt_from_the_nation_tax"));
	}
	
	public Town getTown() {
		return town;
	}

	public Nation getNation() {
		return nation;
	}

	public double getTax() {
		return tax;
	}

	public void setTax(double tax) {
		this.tax = tax;
	}

	public static HandlerList getHandlerList() {
		return HANDLER_LIST;
	}

	@NotNull
	@Override
	public HandlerList getHandlers() {
		return HANDLER_LIST;
	}
}
