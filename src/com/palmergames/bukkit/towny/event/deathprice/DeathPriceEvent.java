package com.palmergames.bukkit.towny.event.deathprice;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.Nullable;

import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.towny.object.economy.Account;

public abstract class DeathPriceEvent extends Event implements Cancellable {

	private static final HandlerList handlers = new HandlerList();
	protected boolean cancelled;
	protected final Account payer;
	protected double amount;
	protected final Resident deadResident;
	protected final Player killer;

	public DeathPriceEvent(Account payer, double amount, Resident deadResident, Player killer) {
		super(!Bukkit.getServer().isPrimaryThread());
		this.payer = payer;
		this.amount = amount;
		this.deadResident = deadResident;
		this.killer = killer;
	}

	public static HandlerList getHandlerList() {
		return handlers;
	}

	public HandlerList getHandlers() {
		return handlers;
	}

	/**
	 * @return the payer
	 */
	public Account getPayer() {
		return payer;
	}

	/**
	 * @return the amount
	 */
	public double getAmount() {
		return amount;
	}

	/**
	 * @param amount the amount to set
	 */
	public void setAmount(double amount) {
		this.amount = amount;
	}

	/**
	 * @return the deadResident
	 */
	public Resident getDeadResident() {
		return deadResident;
	}

	/**
	 * @return killer, the Player who is getting paid, or null if this was not a PVP death.
	 */
	@Nullable
	public Player getKiller() {
		return killer;
	}
	
	/**
	 * @return true when this is a PVP related death.
	 */
	public boolean isPVPDeath() {
		return killer != null;
	}

	/**
	 * Whether the event has been cancelled or the amount has been made 0 or less.
	 */
	@Override
	public boolean isCancelled() {
		return cancelled || getAmount() <= 0;
	}

	/**
	 * Set the event to cancelled. False meaning money will be lost, True meaning no
	 * money will be lost.
	 */
	@Override
	public void setCancelled(boolean cancel) {
		cancelled = cancel;
	}
}
