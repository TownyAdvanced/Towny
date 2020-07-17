package com.palmergames.bukkit.towny.object;

import com.palmergames.bukkit.config.ConfigNodes;
import com.palmergames.bukkit.towny.Towny;
import com.palmergames.bukkit.towny.TownyAPI;
import com.palmergames.bukkit.towny.TownyMessaging;
import com.palmergames.bukkit.towny.TownySettings;
import com.palmergames.bukkit.towny.TownyUniverse;
import com.palmergames.bukkit.towny.database.handler.annotations.SavedEntity;
import com.palmergames.bukkit.towny.database.handler.annotations.LoadSetter;
import com.palmergames.bukkit.towny.database.handler.annotations.OneToMany;
import com.palmergames.bukkit.towny.event.NationAddTownEvent;
import com.palmergames.bukkit.towny.event.NationRemoveTownEvent;
import com.palmergames.bukkit.towny.exceptions.AlreadyRegisteredException;
import com.palmergames.bukkit.towny.exceptions.EconomyException;
import com.palmergames.bukkit.towny.exceptions.EmptyNationException;
import com.palmergames.bukkit.towny.exceptions.NotRegisteredException;
import com.palmergames.bukkit.towny.exceptions.TownyException;
import com.palmergames.bukkit.towny.invites.Invite;
import com.palmergames.bukkit.towny.invites.InviteHandler;
import com.palmergames.bukkit.towny.invites.exceptions.TooManyInvitesException;
import com.palmergames.bukkit.towny.object.economy.AccountAuditor;
import com.palmergames.bukkit.towny.object.economy.GovernmentAccountAuditor;
import com.palmergames.bukkit.towny.object.metadata.CustomDataField;
import com.palmergames.bukkit.towny.permissions.TownyPerms;
import com.palmergames.bukkit.towny.war.flagwar.FlagWar;
import com.palmergames.bukkit.util.BukkitTools;
import com.palmergames.util.StringMgmt;
import org.bukkit.Location;
import org.bukkit.World;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

@SavedEntity(
	tableName = "NATIONS",
	directory = "nations"
)
public class Nation extends Government {

	private transient static final String ECONOMY_ACCOUNT_PREFIX = TownySettings.getNationAccountPrefix();

	//private List<Resident> assistants = new ArrayList<Resident>();
	private final List<Town> towns = new ArrayList<>();
	
	@OneToMany(tableName = "allies")
	private List<Nation> allies = new ArrayList<>();
	@OneToMany(tableName = "enemies")
	private List<Nation> enemies = new ArrayList<>();
	private UUID capital;
	private boolean neutral = false;
	private String mapColorHexCode = "";
	private String tag = "";
	private long registered;
	
	@LoadSetter(setterName = "setNationSpawn")
	private Location nationSpawn;
	private final transient List<Invite> sentAllyInvites = new ArrayList<>();
	@SuppressWarnings("unused")
	private final AccountAuditor accountAuditor = new GovernmentAccountAuditor();

	public Nation(UUID uniqueIdentifier) {
		super(uniqueIdentifier);

		// Set defaults
		setBoard(TownySettings.getNationDefaultBoard());
		setOpen(TownySettings.getNationDefaultOpen());
	}
	
	public Nation(UUID uniqueIdentifier, String name) {
		this(uniqueIdentifier);
		super(name);
	}

	/**
	 * Renames the nation to the specified new name.
	 * NOTE: This method <b>does not</b> perform name validation checks!
	 *
	 * @param newName New filtered name of the nation.
	 */
	@Override
	public void rename(String newName) {
		String oldName = getName();
		
		setName(newName);
		TownyUniverse.getInstance().updateNationName(oldName, newName);
		
		// Migrate economy
		if (TownySettings.isUsingEconomy()) {
			try {
				double nationBalance = getAccount().getHoldingBalance();
				if (TownySettings.isEcoClosedEconomyEnabled()) {
					getAccount().pay(nationBalance, "Nation Rename");
				}
				getAccount().removeAccount();
				// Rename account
				getAccount().setName(TownySettings.getNationAccountPrefix() + newName);
				getAccount().setBalance(nationBalance, "Rename Nation - Transfer to new account");
			} catch (EconomyException ignored) {
			}
		}
		
		// Save nation
		save();

		BukkitTools.getPluginManager().callEvent(new RenameNationEvent(oldName, this));
		
		// Set defaults
		setBoard(TownySettings.getNationDefaultBoard());
		setOpen(TownySettings.getNationDefaultOpen());
	}

	public void addAlly(Nation nation) throws AlreadyRegisteredException {

		if (hasAlly(nation))
			throw new AlreadyRegisteredException();
		else {
			try {
				removeEnemy(nation);
			} catch (NotRegisteredException ignored) {}
			getAllies().add(nation);
		}
	}

	public boolean removeAlly(Nation nation) throws NotRegisteredException {

		if (!hasAlly(nation))
			throw new NotRegisteredException();
		else
			return getAllies().remove(nation);
	}

	public boolean removeAllAllies() {

		for (Nation ally : new ArrayList<>(getAllies()))
			try {
				removeAlly(ally);
				ally.removeAlly(this);
			} catch (NotRegisteredException ignored) {}
		return getAllies().size() == 0;
	}

	public boolean hasAlly(Nation nation) {

		return getAllies().contains(nation);
	}

	public boolean IsAlliedWith(Nation nation) {

		return getAllies().contains(nation);
	}

	public void addEnemy(Nation nation) throws AlreadyRegisteredException {

		if (hasEnemy(nation))
			throw new AlreadyRegisteredException();
		else {
			try {
				removeAlly(nation);
			} catch (NotRegisteredException ignored) {}
			getEnemies().add(nation);
		}

	}

	public boolean removeEnemy(Nation nation) throws NotRegisteredException {

		if (!hasEnemy(nation))
			throw new NotRegisteredException();
		else
			return getEnemies().remove(nation);
	}

	public boolean removeAllEnemies() {

		for (Nation enemy : new ArrayList<>(getEnemies()))
			try {
				removeEnemy(enemy);
				enemy.removeEnemy(this);
			} catch (NotRegisteredException ignored) {}
		return getAllies().size() == 0;
	}

	public boolean hasEnemy(Nation nation) {

		return getEnemies().contains(nation);
	}

	public List<Town> getTowns() {
		return Collections.unmodifiableList(towns);
	}

	public boolean isKing(Resident resident) {

		return hasCapital() && getCapital().isMayor(resident);
	}

	public boolean hasCapital() {

		return getCapital() != null;
	}

	public boolean hasAssistant(Resident resident) {

		return getAssistants().contains(resident);
	}

	public boolean isCapital(Town town) {
		return town.getUniqueIdentifier().equals(getCapital().getUniqueIdentifier());
	}

	public boolean hasTown(String name) {

		for (Town town : towns)
			if (town.getName().equalsIgnoreCase(name))
				return true;
		return false;
	}

	public boolean hasTown(Town town) {

		return towns.contains(town);
	}

	public void addTown(Town town) throws AlreadyRegisteredException {

		if (hasTown(town))
			throw new AlreadyRegisteredException();
		
		if (town.hasNation()) {
			// Allow code execution if there is a desync (which there is on load).
			try {
				if (!town.getNation().equals(this))
					throw new AlreadyRegisteredException();
			} catch (NotRegisteredException ignore) {}
		}

		towns.add(town);
		town.setNation(this);

		BukkitTools.getPluginManager().callEvent(new NationAddTownEvent(town, this));
	}

	public void setCapital(Town capital) {
		this.capital = capital.getUniqueIdentifier();
		try {
			TownyPerms.assignPermissions(capital.getMayor(), null);
		} catch (Exception e) {
			// Dummy catch to prevent errors on startup when setting nation.
		}
		//save();
	}

	public Town getCapital() {
		try {
			return TownyUniverse.getInstance().getTown(capital);
		} catch (NotRegisteredException e) {
			TownyMessaging.sendErrorMsg("The capital for nation " + getName() + " does not exist!" +
				" Please fix this in the database!");
			capital = null;
		}

		return null;
	}

	@Override
	public Location getSpawn() throws TownyException {
		if(nationSpawn == null){
			throw new TownyException(TownySettings.getLangString("msg_err_nation_has_not_set_a_spawn_location"));
		}

		return nationSpawn;
	}

	@Override
	public void setSpawn(Location spawn) throws TownyException {
		if (TownyAPI.getInstance().isWilderness(spawn))
			throw new TownyException(String.format(TownySettings.getLangString("msg_cache_block_error_wild"), "set spawn"));

		TownBlock townBlock = TownyAPI.getInstance().getTownBlock(spawn);

		if(TownySettings.getBoolean(ConfigNodes.GNATION_SETTINGS_CAPITAL_SPAWN)){
			if(this.capital == null){
				throw new TownyException(TownySettings.getLangString("msg_err_spawn_not_within_capital"));
			}
			if(!townBlock.hasTown()){
				throw new TownyException(TownySettings.getLangString("msg_err_spawn_not_within_capital"));
			}
			if(townBlock.getTown() != this.getCapital()){
				throw new TownyException(TownySettings.getLangString("msg_err_spawn_not_within_capital"));
			}
		} else {
			if(!townBlock.hasTown()){
				throw new TownyException(TownySettings.getLangString("msg_err_spawn_not_within_nationtowns"));
			}

			if(!towns.contains(townBlock.getTown())){
				throw new TownyException(TownySettings.getLangString("msg_err_spawn_not_within_nationtowns"));
			}
		}

		this.nationSpawn = spawn;
	}

	/**
	 * Only to be called from the Loading methods.
	 *
	 * @param nationSpawn - Location to set as Nation Spawn
	 */
	public void forceSetNationSpawn(Location nationSpawn){
		this.nationSpawn = nationSpawn;
	}

	//TODO: Remove
	public boolean setAllegiance(String type, Nation nation) {

		try {
			if (type.equalsIgnoreCase("ally")) {
				removeEnemy(nation);
				addAlly(nation);
				if (!hasEnemy(nation) && hasAlly(nation))
					return true;
			} else if (type.equalsIgnoreCase("peaceful") || type.equalsIgnoreCase("neutral")) {
				removeEnemy(nation);
				removeAlly(nation);
				if (!hasEnemy(nation) && !hasAlly(nation))
					return true;
			} else if (type.equalsIgnoreCase("enemy")) {
				removeAlly(nation);
				addEnemy(nation);
				if (hasEnemy(nation) && !hasAlly(nation))
					return true;
			}
		} catch (AlreadyRegisteredException | NotRegisteredException x) {
			return false;
		}
		
		return false;
	}

	public List<Resident> getAssistants() {

		List<Resident> assistants = new ArrayList<>();
		
		for (Town town: towns)
		for (Resident assistant: town.getResidents()) {
			if (assistant.hasNationRank("assistant"))
				assistants.add(assistant);
		}
		return assistants;
	}

	public void setEnemies(List<Nation> enemies) {

		this.enemies = enemies;
	}

	public List<Nation> getEnemies() {

		return enemies;
	}

	public void setAllies(List<Nation> allies) {

		this.allies = allies;
	}

	public List<Nation> getAllies() {

		return allies;
	}

	public int getNumTowns() {

		return towns.size();
	}

	public int getNumResidents() {

		int numResidents = 0;
		for (Town town : getTowns())
			numResidents += town.getNumResidents();
		return numResidents;
	}

	public void removeTown(Town town) throws EmptyNationException, NotRegisteredException {

		if (!hasTown(town))
			throw new NotRegisteredException();
		else {

			boolean isCapital = town.isCapital();
			remove(town);

			if (getNumTowns() == 0) {
				throw new EmptyNationException(this);
			} else if (isCapital) {
				int numResidents = 0;
				Town tempCapital = null;
				for (Town newCapital : getTowns())
					if (newCapital.getNumResidents() > numResidents) {
						tempCapital = newCapital;
						numResidents = newCapital.getNumResidents();
					}

				if (tempCapital != null)
					setCapital(tempCapital);
			}
			save();
		}
	}

	private void remove(Town town) {

		//removeAssistantsIn(town);
		try {
			town.setNation(null);
		} catch (AlreadyRegisteredException ignored) {
		}
		
		/*
		 * Remove all resident titles/nationRanks before saving the town itself.
		 */
		List<Resident> titleRemove = new ArrayList<>(town.getResidents());

		for (Resident res : titleRemove) {
			if (res.hasTitle() || res.hasSurname()) {
				res.setTitle("");
				res.setSurname("");
			}
			res.updatePermsForNationRemoval(); // Clears the nationRanks.
			res.save();
		}
		
		towns.remove(town);
		
		BukkitTools.getPluginManager().callEvent(new NationRemoveTownEvent(town, this));
	}

	private void removeAllTowns() {

		for (Town town : new ArrayList<>(towns))
			remove(town);
	}

	public void setTaxes(double taxes) {
		this.taxes = Math.min(taxes, TownySettings.getMaxNationTax());
	}

	public void clear() {

		//TODO: Check cleanup
		removeAllAllies();
		removeAllEnemies();
		removeAllTowns();
		capital = null;
	}

	/**
	 * Method for rechecking town distances to a new nation capital/moved 
	 * nation capital homeblock. Results in towns whose homeblocks are no 
	 * longer close enough to the capital homeblock being removed from 
	 * the nation.
	 * 
	 * @throws TownyException - Generic TownyException
	 */
	public void recheckTownDistance() throws TownyException {
		Town capTown = getCapital();
		if(capTown != null) {
			if (TownySettings.getNationRequiresProximity() > 0) {
				final Coord capitalCoord = capTown.getHomeBlock().getCoord();
				Iterator<Town> it = towns.iterator();
				while(it.hasNext()) {
					Town town = it.next();
					Coord townCoord = town.getHomeBlock().getCoord();
					if (!capTown.getHomeBlock().getWorld().getName().equals(town.getHomeBlock().getWorld().getName())) {
						it.remove();
						continue;
					}

					final double distance = Math.sqrt(Math.pow(capitalCoord.getX() - townCoord.getX(), 2) + Math.pow(capitalCoord.getZ() - townCoord.getZ(), 2));
					if (distance > TownySettings.getNationRequiresProximity()) {
						TownyMessaging.sendPrefixedTownMessage(town, String.format(TownySettings.getLangString("msg_town_left_nation"), this.getName()));
						TownyMessaging.sendPrefixedNationMessage(this, String.format(TownySettings.getLangString("msg_nation_town_left"), town.getName()));
						this.remove(town);
						it.remove();
					}
				}
			}
		}
	}
	
	/**
	 * A dry-run method for rechecking town distances to a new nation capital/
	 * moved nation capital homeblock.
	 * 
	 * @param towns - The list of towns to check.
	 * @throws TownyException - Generic TownyException
	 * @return removedTowns - A list of Towns which would be removed under a real recheckTownDistance().
	 */
	public List<Town> recheckTownDistanceDryRun(List<Town> towns) throws TownyException {
		List<Town> removedTowns = new ArrayList<>();
		Town capTown = getCapital();
		if(capTown != null) {
			if (TownySettings.getNationRequiresProximity() > 0) {
				final Coord capitalCoord = capTown.getHomeBlock().getCoord();
				
				for (Town town : towns) {
					Coord townCoord = town.getHomeBlock().getCoord();
					if (!capTown.getHomeblockWorld().equals(town.getHomeblockWorld())) {
						continue;
					}
					final double distance = Math.sqrt(Math.pow(capitalCoord.getX() - townCoord.getX(), 2) + Math.pow(capitalCoord.getZ() - townCoord.getZ(), 2));
					if (distance > TownySettings.getNationRequiresProximity()) {
						removedTowns.add(town);
					}
				}
			}
		}
		return removedTowns;
	}	

	public void toggleNeutral(boolean neutral) throws TownyException {

		if (!TownySettings.isDeclaringNeutral() && neutral)
			throw new TownyException(TownySettings.getLangString("msg_err_fight_like_king"));
		else {
			if (neutral && !FlagWar.getCellsUnderAttack().isEmpty())
				for (Resident resident : getResidents())
					FlagWar.removeAttackerFlags(resident.getName());
			
			setNeutral(neutral);
		}
	}
	
	public void setNeutral(boolean neutral) {

		this.neutral = neutral;
	}

	public boolean isNeutral() {

		return neutral;
	}
	
	public void setKing(Resident king) throws TownyException {

		if (!hasResident(king))
			throw new TownyException(TownySettings.getLangString("msg_err_king_not_in_nation"));
		if (!king.isMayor())
			throw new TownyException(TownySettings.getLangString("msg_err_new_king_notmayor"));
		setCapital(king.getTown());
		TownyUniverse.getInstance().getDataSource().saveNation(this);
	}

	public boolean hasResident(Resident resident) {

		for (Town town : getTowns())
			if (town.hasResident(resident))
				return true;
		return false;
	}
	
	public void collect(double amount) throws EconomyException {
		
		if (TownySettings.isUsingEconomy()) {
			double bankcap = TownySettings.getNationBankCap();
			if (bankcap > 0) {
				if (amount + this.getAccount().getHoldingBalance() > bankcap) {
					TownyMessaging.sendPrefixedNationMessage(this, String.format(TownySettings.getLangString("msg_err_deposit_capped"), bankcap));
					return;
				}
			}
			
			this.getAccount().deposit(amount, null);
		}

	}

	@Override
	public List<Resident> getResidents() {

		List<Resident> out = new ArrayList<>();
		for (Town town : getTowns())
			out.addAll(town.getResidents());
		return out;
	}

	@Override
	public List<String> getTreeString(int depth) {

		List<String> out = new ArrayList<>();
		out.add(getTreeDepth(depth) + "Nation (" + getName() + ")");
		out.add(getTreeDepth(depth + 1) + "Capital: " + getCapital().getName());
		
		List<Resident> assistants = getAssistants();
		
		if (assistants.size() > 0)
			out.add(getTreeDepth(depth + 1) + "Assistants (" + assistants.size() + "): " + Arrays.toString(assistants.toArray(new Resident[0])));
		if (getAllies().size() > 0)
			out.add(getTreeDepth(depth + 1) + "Allies (" + getAllies().size() + "): " + Arrays.toString(getAllies().toArray(new Nation[0])));
		if (getEnemies().size() > 0)
			out.add(getTreeDepth(depth + 1) + "Enemies (" + getEnemies().size() + "): " + Arrays.toString(getEnemies().toArray(new Nation[0])));
		out.add(getTreeDepth(depth + 1) + "Towns (" + getTowns().size() + "):");
		for (Town town : getTowns())
			out.addAll(town.getTreeString(depth + 2));
		return out;
	}

	@Override
	public boolean hasResident(String name) {

		for (Town town : getTowns())
			if (town.hasResident(name))
				return true;
		return false;
	}

	@Override
	public Collection<Resident> getOutlaws() {
		List<Resident> out = new ArrayList<>();
		for (Town town : getTowns())
			out.addAll(town.getOutlaws());
		return Collections.unmodifiableList(out);
	}

	public long getRegistered() {
		return registered;
	}

	public void setRegistered(long registered) {
		this.registered = registered;
	}

	@Override
	public List<Invite> getReceivedInvites() {
		return receivedinvites;
	}

	@Override
	public void newReceivedInvite(Invite invite) throws TooManyInvitesException {
		if (receivedinvites.size() <= (InviteHandler.getReceivedInvitesMaxAmount(this) -1)) {
			receivedinvites.add(invite);
		} else {
			throw new TooManyInvitesException(String.format(TownySettings.getLangString("msg_err_nation_has_too_many_requests"),this.getName()));
		}
	}

	@Override
	public void deleteReceivedInvite(Invite invite) {
		receivedinvites.remove(invite);
	}

	@Override
	public List<Invite> getSentInvites() {
		return sentinvites;
	}

	@Override
	public void newSentInvite(Invite invite) throws TooManyInvitesException {
		if (sentinvites.size() <= (InviteHandler.getSentInvitesMaxAmount(this) -1)) {
			sentinvites.add(invite);
		} else {
			throw new TooManyInvitesException(TownySettings.getLangString("msg_err_nation_sent_too_many_invites"));
		}
	}

	@Override
	public void deleteSentInvite(Invite invite) {
		sentinvites.remove(invite);
	}
	
	public void newSentAllyInvite(Invite invite) throws TooManyInvitesException {
		if (sentAllyInvites.size() <= InviteHandler.getSentAllyRequestsMaxAmount(this) -1) {
			sentAllyInvites.add(invite);
		} else {
			throw new TooManyInvitesException(TownySettings.getLangString("msg_err_nation_sent_too_many_requests"));
		}
	}
	
	public void deleteSentAllyInvite(Invite invite) {
		sentAllyInvites.remove(invite);
	}
	
	public List<Invite> getSentAllyInvites() {
		return Collections.unmodifiableList(sentAllyInvites);
	}
	
	public int getNumTownblocks() {
		int townBlocksClaimed = 0;
		for (Town towns : this.getTowns()) {
			townBlocksClaimed = townBlocksClaimed + towns.getTownBlocks().size();
		}
		return townBlocksClaimed;
	}
	
	public Resident getKing() {
		return capital != null ? getCapital().getMayor() : null; 
	}

	@Override
	public String getFormattedName() {
		return TownySettings.getNationPrefix(this) + this.getName().replaceAll("_", " ")
			+ TownySettings.getNationPostfix(this);
	}
	
	@Override
	public void addMetaData(CustomDataField<?> md) {
		super.addMetaData(md);
		save();
	}

	public void removeMetaData(CustomDataField md) {
		super.removeMetaData(md);
		save();
	}

	@Override
	public World getWorld() {
		if (hasCapital() && getCapital().hasWorld()) {
			return BukkitTools.getWorld(getCapital().getHomeblockWorld().getName());
		} else {
			return BukkitTools.getWorlds().get(0);
		}
	}

	@Override
	public String getBankAccountPrefix() {
		return ECONOMY_ACCOUNT_PREFIX;
	}

	@Override
	public double getBankCap() {
		return TownySettings.getNationBankCap();
	}

	/**
	 * Shows if the nation is allied with the specified nation.
	 * 
	 * @param nation The nation that is allied.
	 * @return true if it is allied, false otherwise.
	 */
	public boolean isAlliedWith(Nation nation) {
		return allies.contains(nation);
	}

	/**
	 * @deprecated As of 0.96.0.0+ please use {@link EconomyAccount#getWorld()} instead.
	 *
	 * @return The world this resides in.
	 */
	@Deprecated
	public World getBukkitWorld() {
		if (hasCapital() && getCapital().hasWorld()) {
			return BukkitTools.getWorld(getCapital().getHomeblockWorld().getName());
		} else {
			return BukkitTools.getWorlds().get(0);
		}
	}

	/**
	 * @deprecated As of 0.96.0.0+ please use {@link EconomyAccount#getName()} instead.
	 *
	 * @return The name of the economy account.
	 */
	@Deprecated
	public String getEconomyName() {
		return StringMgmt.trimMaxLength(Nation.ECONOMY_ACCOUNT_PREFIX + getName(), 32);
	}
	
	
	/**
	 * @deprecated as of 0.95.2.15, please use {@link EconomyAccount#getHoldingBalance()} instead.
	 * 
	 * @return the holding balance of the economy account.
	 * @throws EconomyException When an economy error occurs
	 */
	@Deprecated
	public double getHoldingBalance() throws EconomyException {
		try {
			return getAccount().getHoldingBalance();
		} catch (NoClassDefFoundError e) {
			e.printStackTrace();
			throw new EconomyException("Economy error getting holdings for " + getEconomyName());
		}
	}

	/**
	 * @deprecated As of 0.95.1.15, please use {@link EconomyAccount#pay(double, String)} instead.
	 *
	 * @param amount value to deduct from the player's account
	 * @param reason leger memo stating why amount is deducted
	 * @return true if successful
	 * @throws EconomyException if the transaction fails
	 */
	@Deprecated
	public boolean pay(double amount, String reason) throws EconomyException {
		return getAccount().withdraw(amount, reason);
	}

	/**
	 * @deprecated As of 0.95.1.15, please use {@link EconomyAccount#collect(double, String)} instead.
	 *
	 * @param amount currency to collect
	 * @param reason memo regarding transaction
	 * @return collected or pay to server account   
	 * @throws EconomyException if transaction fails
	 */
	@Deprecated
	public boolean collect(double amount, String reason) throws EconomyException {
		return getAccount().deposit(amount, reason);
	}

	public String getMapColorHexCode() {
		return mapColorHexCode;
	}

	public void setMapColorHexCode(String mapColorHexCode) {
		this.mapColorHexCode = mapColorHexCode;
	}
	
	/**
	 * @deprecated As of 0.96.2.0, please use {@link #getSpawn()} instead.
	 * 
	 * @return getSpawn()
	 * @throws TownyException When a nation spawn isn't available
	 */
	@Deprecated
	public Location getNationSpawn() throws TownyException {
		return getSpawn();
	}
	
	/**
	 * @deprecated As of 0.96.2.0, please use {@link #getBoard()} instead.
	 *  
	 * @return getBoard()
	 */
	@Deprecated
	public String getNationBoard() {
		return getBoard();
	}
}
