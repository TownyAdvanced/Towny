package com.palmergames.bukkit.towny.object;

import com.palmergames.bukkit.towny.TownySettings;
import com.palmergames.bukkit.towny.event.TerritoryTagChangeEvent;
import com.palmergames.bukkit.towny.exceptions.EconomyException;
import com.palmergames.bukkit.towny.exceptions.TownyException;
import com.palmergames.bukkit.towny.invites.Invite;
import com.palmergames.bukkit.towny.invites.InviteHandler;
import com.palmergames.bukkit.towny.invites.exceptions.TooManyInvitesException;
import com.palmergames.bukkit.towny.object.economy.AccountAuditor;
import com.palmergames.bukkit.towny.object.economy.BankEconomyHandler;
import com.palmergames.bukkit.towny.object.economy.BankAccount;
import com.palmergames.bukkit.towny.object.economy.TerritoryAccountAuditor;
import com.palmergames.util.StringMgmt;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public abstract class Territory extends TownyObject implements BankEconomyHandler, ResidentList, TownyInviter, SpawnLocation {
	
	protected BankAccount account;
	protected Location spawn;
	protected String tag = "";
	protected String board = null;
	private final transient List<Invite> receivedInvites = new ArrayList<>();
	private final transient List<Invite> sentInvites = new ArrayList<>();
	private boolean isPublic = false;
	private boolean isOpen = false;
	private long registered;
	private double spawnCost;
	protected double taxes = -1;
	private final AccountAuditor accountAuditor = new TerritoryAccountAuditor();
	
	protected Territory(String name) {
		super(name);
	}

	@Override
	public final List<Invite> getReceivedInvites() {
		return Collections.unmodifiableList(receivedInvites);
	}

	@Override
	public final void newReceivedInvite(Invite invite) throws TooManyInvitesException {
		if (receivedInvites.size() <= (InviteHandler.getReceivedInvitesMaxAmount(this) -1)) { // We only want 10 Invites, for towns, later we can make this number configurable
			receivedInvites.add(invite);
		} else {
			throw new TooManyInvitesException(String.format(TownySettings.getLangString("msg_err_town_has_too_many_invites"),this.getName()));
		}
	}

	@Override
	public final void deleteReceivedInvite(Invite invite) {
		receivedInvites.remove(invite);
	}

	@Override
	public final List<Invite> getSentInvites() {
		return Collections.unmodifiableList(sentInvites);
	}

	@Override
	public final void newSentInvite(Invite invite)  throws TooManyInvitesException {
		if (sentInvites.size() <= (InviteHandler.getSentInvitesMaxAmount(this) -1)) { // We only want 35 Invites, for towns, later we can make this number configurable
			sentInvites.add(invite);
		} else {
			throw new TooManyInvitesException(TownySettings.getLangString("msg_err_town_sent_too_many_invites"));
		}
	}

	@Override
	public final void deleteSentInvite(Invite invite) {
		sentInvites.remove(invite);
	}

	public final void setBoard(String board) {
		this.board = board;
	}

	public String getBoard() {
		return board;
	}

	public final void setPublic(boolean isPublic) { 
		this.isPublic = isPublic; 
	}

	public final boolean isPublic() { 
		return isPublic; 
	}

	public final void setOpen(boolean isOpen) { 
		this.isOpen = isOpen; 
	}


	public final boolean isOpen() { 
		return isOpen; 
	}

	public final void setRegistered(long registered) {
		this.registered = registered;
	}

	public final long getRegistered() {
		return registered;
	}

	public final void setSpawnCost(double spawnCost) { 
		this.spawnCost = spawnCost; 
	}

	public final double getSpawnCost() {
		return spawnCost;
	}

	public final void setTag(String text) throws TownyException {
		this.tag = text.toUpperCase().substring(0,3);
		if (this.tag.matches(" "))
			this.tag = "";
		Bukkit.getPluginManager().callEvent(new TerritoryTagChangeEvent(this.tag, this));
	}

	public final String getTag() {
		return tag;
	}

	public final boolean hasTag() {
		return !tag.isEmpty();
	}
	
	public final void withdrawFromBank(Resident resident, int amount) throws EconomyException, TownyException {
		if (!TownySettings.isUsingEconomy()) {
			throw new TownyException(TownySettings.getLangString("msg_err_no_economy"));
		}
		if (!getAccount().payTo(amount, resident, getName() + " - " + " Withdraw")) {
			throw new TownyException(TownySettings.getLangString("msg_err_no_money"));
		}
	}
	
	public final void depositToBank(Resident resident, int amount) throws EconomyException, TownyException {
		if (!TownySettings.isUsingEconomy()) {
			throw new TownyException(TownySettings.getLangString("msg_err_no_economy"));
		}
		if (!resident.getAccount().payTo(amount, getAccount(), "Deposit from " + resident.getName())) {
			throw new TownyException(TownySettings.getLangString("msg_insuf_funds"));
		}
	}

	@Override
	public BankAccount getAccount() {
		if (account == null) {
			String accountName = StringMgmt.trimMaxLength(getEconomyPrefix() + getName(), 32);
			World world = getWorld();
			account = new BankAccount(accountName, world, getBankCap());
			account.setAuditor(accountAuditor);
		}

		return account;
	}
	
	public abstract void setTaxes(double taxes);
	public abstract World getWorld();
	
	public double getTaxes() {
		setTaxes(taxes); //make sure the tax level is right.
		return taxes;
	}
	
}
