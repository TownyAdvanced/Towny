package com.palmergames.bukkit.towny.event.time.dailytaxes;

import org.bukkit.Bukkit;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import com.palmergames.bukkit.towny.object.Nation;
import com.palmergames.bukkit.towny.object.Town;
import com.palmergames.bukkit.towny.object.Translation;

public class PreTownPaysNationTaxEvent extends Event implements Cancellable {

	private static final HandlerList handlers = new HandlerList();
	private boolean isCancelled;
	private Town town;
	private Nation nation;
	private double tax;
	private String cancellationMessage = Translation.of("msg_your_town_was_exempt_from_the_nation_tax");
	
	/**
	 * Cancellable event that precedes a town paying the nation tax.
	 * 
	 * @param town Town about to pay tax.
	 * @param nation Nation about to have tax paid to it.
	 * @param tax the amount the town will pay.
	 */
	public PreTownPaysNationTaxEvent(Town town, Nation nation, double tax) {
		super(!Bukkit.getServer().isPrimaryThread());
		this.town = town;
		this.nation = nation;
		this.tax = tax;
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

	@Override
	public boolean isCancelled() {
		return isCancelled;
	}

	@Override
	public void setCancelled(boolean cancel) {
		this.isCancelled = cancel;
	}

	public String getCancellationMessage() {
		return cancellationMessage;
	}

	public void setCancellationMessage(String cancellationMessage) {
		this.cancellationMessage = cancellationMessage;
	}
	
	public static HandlerList getHandlerList() {
		return handlers;
	}
	
	@Override
	public HandlerList getHandlers() {
		return handlers;
	}

}
