package com.palmergames.bukkit.towny.object;

import com.palmergames.bukkit.towny.TownySettings;
import com.palmergames.bukkit.towny.event.GovernmentTagChangeEvent;
import com.palmergames.bukkit.towny.exceptions.EconomyException;
import com.palmergames.bukkit.towny.exceptions.TownyException;
import com.palmergames.bukkit.towny.invites.Invite;
import com.palmergames.bukkit.towny.invites.InviteHandler;
import com.palmergames.bukkit.towny.invites.exceptions.TooManyInvitesException;
import com.palmergames.bukkit.towny.object.economy.AccountAuditor;
import com.palmergames.bukkit.towny.object.economy.BankEconomyHandler;
import com.palmergames.bukkit.towny.object.economy.BankAccount;
import com.palmergames.bukkit.towny.object.economy.GovernmentAccountAuditor;
import com.palmergames.util.StringMgmt;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * A class which represents the structure of objects that
 * occupy areas or interactive managed sub-objects.
 * 
 * @author Suneet Tipirneni (Siris)
 */
public abstract class Government extends TownyObject implements BankEconomyHandler, ResidentList, Inviteable, Identifiable, SpawnLocation {
	
	protected UUID uuid;
	protected BankAccount account;
	protected Location spawn;
	protected String tag = "";
	protected String board = null;
	private final transient List<Invite> receivedInvites = new ArrayList<>();
	private final transient List<Invite> sentInvites = new ArrayList<>();
	private boolean isPublic = false;
	private boolean isOpen = false;
	private boolean isNeutral = false;
	private long registered;
	private double spawnCost = TownySettings.getSpawnTravelCost();
	protected double taxes;
	private final AccountAuditor accountAuditor = new GovernmentAccountAuditor();
	
	protected Government(String name) {
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
			throw new TooManyInvitesException(String.format(Translation.of("msg_err_town_has_too_many_invites", this.getName())));
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
			throw new TooManyInvitesException(Translation.of("msg_err_town_sent_too_many_invites"));
		}
	}

	@Override
	public final void deleteSentInvite(Invite invite) {
		sentInvites.remove(invite);
	}

	/**
	 * Sets the board of the government.
	 * 
	 * @param board A string for the new board.
	 */
	public final void setBoard(String board) {
		this.board = board;
	}

	/**
	 * Gets the board of the government.
	 *
	 * @return A string representing the board.
	 */
	public String getBoard() {
		return board;
	}

	/**
	 * Sets the spawn to be visitable by non-member players,
	 * also sets whether the homeblock coordinates are visible.
	 *
	 * @param isPublic false for not hidden, true otherwise.
	 */
	public final void setPublic(boolean isPublic) { 
		this.isPublic = isPublic; 
	}

	/**
	 * Whether the spawn is visitable by non-members,
	 * and also whether the homeblock coordinates are visible.
	 *
	 * @return false for not hidden, true otherwise.
	 */
	public final boolean isPublic() { 
		return isPublic; 
	}

	/**
	 * Sets this to be open to any resident joining without 
	 * an invitation or not.
	 *
	 * @param isOpen true for invitation-less joining, false otherwise.
	 */
	public final void setOpen(boolean isOpen) { 
		this.isOpen = isOpen; 
	}

	/**
	 * Indicates if any resident can join without 
	 * an invitation or not.
	 *
	 * @return true for invitation-less joining, false otherwise.
	 */
	public final boolean isOpen() { 
		return isOpen; 
	}
	
	/**
	 * Sets the neutrality/peacefulness of the object. 
	 * 
	 * @param neutral whether the object will be neutral or peaceful.
	 */
	public final void setNeutral(boolean neutral) {
		this.isNeutral = neutral;
	}

	/**
	 * Is the object Neutral or Peaceful?
	 * 
	 * @return true if the object is Neutral or Peaceful.
	 */
	public final boolean isNeutral() {
		return isNeutral;
	}
	
	/**
	 * Sets the date when this was registered.
	 * 
	 * @param registered A long of unix time when this was registered.
	 */
	public final void setRegistered(long registered) {
		this.registered = registered;
	}

	/**
	 * Gets the date when this was registered.
	 *
	 * @return A long of unix time when this was registered.
	 */
	public final long getRegistered() {
		return registered;
	}

	/**
	 * Sets the cost of spawning to this location.
	 *
	 * @param spawnCost The cost to spawn.
	 */
	public final void setSpawnCost(double spawnCost) { 
		this.spawnCost = spawnCost; 
	}

	/**
	 * Gets the cost of spawning to this location.
	 *
	 * @return The cost to spawn.
	 */
	public final double getSpawnCost() {
		return spawnCost;
	}

	/**
	 * Sets the concise string representation of this object.
	 * 
	 * @param text An upper-cased four or less letter string.
	 * @throws TownyException Thrown on an error setting.
	 */
	public final void setTag(String text) throws TownyException {

		if (text.length() > 4)
			throw new TownyException(Translation.of("msg_err_tag_too_long"));

		this.tag = text.toUpperCase();
		if (this.tag.matches(" "))
			this.tag = "";
		Bukkit.getPluginManager().callEvent(new GovernmentTagChangeEvent(this.tag, this));
	}

	/**
	 * Gets the concise string representation of this object.
	 * 
	 * @return An upper-cased four or less letter string, representing the tag.
	 */
	public final String getTag() {
		return tag;
	}

	/**
	 * Whether or not this object has a tag.
	 * 
	 * @return true if present, false otherwise.
	 */
	public final boolean hasTag() {
		return !tag.isEmpty();
	}

	/**
	 * Takes money from the bank.
	 * 
	 * @param resident The resident to give the money to.
	 * @param amount The amount to give.
	 * @throws EconomyException On economy error.
	 * @throws TownyException On general error.
	 */
	public final void withdrawFromBank(Resident resident, int amount) throws EconomyException, TownyException {
		if (!TownySettings.isUsingEconomy()) {
			throw new TownyException(Translation.of("msg_err_no_economy"));
		}
		if (!getAccount().payTo(amount, resident, getName() + " - " + " Withdraw")) {
			throw new TownyException(Translation.of("msg_err_no_money"));
		}
	}

	/**
	 * Puts money into the bank.
	 *
	 * @param resident The resident to take money from.
	 * @param amount The amount to take.
	 * @throws EconomyException On economy error.
	 * @throws TownyException On general error.
	 */
	public final void depositToBank(Resident resident, int amount) throws EconomyException, TownyException {
		if (!TownySettings.isUsingEconomy()) {
			throw new TownyException(Translation.of("msg_err_no_economy"));
		}
		if (!resident.getAccount().payTo(amount, getAccount(), "Deposit from " + resident.getName())) {
			throw new TownyException(Translation.of("msg_insuf_funds"));
		}
	}

	@Override
	public BankAccount getAccount() {
		if (account == null) {
			String accountName = StringMgmt.trimMaxLength(getBankAccountPrefix() + getName(), 32);
			World world = getWorld();
			account = new BankAccount(accountName, world, getBankCap());
			account.setAuditor(accountAuditor);
		}

		return account;
	}

	/**
	 * Sets the tax amount of this object.
	 * 
	 * @param taxes The taxes as a percentage or flat number.
	 */
	public abstract void setTaxes(double taxes);

	/**
	 * Gets the taxes as a percentage or as a flat number.
	 * 
	 * @return The tax number.
	 */
	public double getTaxes() {
		setTaxes(taxes); //make sure the tax level is right.
		return taxes;
	}

	/**
	 * Gets the world in which this object resides.
	 * 
	 * @return The {@link World} this object is in.
	 */
	public abstract World getWorld();

	@Override
	public UUID getUUID() {
		return uuid;
	}

	@Override
	public void setUUID(UUID uuid) {
		this.uuid = uuid;
	}

}
