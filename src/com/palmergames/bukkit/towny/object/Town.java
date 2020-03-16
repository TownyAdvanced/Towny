package com.palmergames.bukkit.towny.object;

import com.palmergames.bukkit.config.ConfigNodes;
import com.palmergames.bukkit.towny.TownyMessaging;
import com.palmergames.bukkit.towny.TownySettings;
import com.palmergames.bukkit.towny.TownyUniverse;
import com.palmergames.bukkit.towny.event.TownAddResidentEvent;
import com.palmergames.bukkit.towny.event.TownRemoveResidentEvent;
import com.palmergames.bukkit.towny.event.TownTagChangeEvent;
import com.palmergames.bukkit.towny.exceptions.AlreadyRegisteredException;
import com.palmergames.bukkit.towny.exceptions.EconomyException;
import com.palmergames.bukkit.towny.exceptions.EmptyNationException;
import com.palmergames.bukkit.towny.exceptions.EmptyTownException;
import com.palmergames.bukkit.towny.exceptions.NotRegisteredException;
import com.palmergames.bukkit.towny.exceptions.TownyException;
import com.palmergames.bukkit.towny.invites.Invite;
import com.palmergames.bukkit.towny.invites.InviteHandler;
import com.palmergames.bukkit.towny.invites.exceptions.TooManyInvitesException;
import com.palmergames.bukkit.towny.object.metadata.CustomDataField;
import com.palmergames.bukkit.towny.permissions.TownyPerms;
import com.palmergames.bukkit.towny.war.siegewar.SiegeWarMembershipController;
import com.palmergames.bukkit.towny.war.siegewar.enums.SiegeStatus;
import com.palmergames.bukkit.towny.war.siegewar.locations.Siege;
import com.palmergames.bukkit.util.BukkitTools;
import com.palmergames.util.StringMgmt;
import com.palmergames.util.TimeMgmt;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

import static com.palmergames.util.TimeMgmt.ONE_HOUR_IN_MILLIS;
import static com.palmergames.bukkit.towny.object.EconomyAccount.SERVER_ACCOUNT;

public class Town extends TownyObject implements ResidentList, TownyInviter, ObjectGroupManageable<PlotGroup>, Bank, TownBlockOwner {

	private static final String ECONOMY_ACCOUNT_PREFIX = TownySettings.getTownAccountPrefix();

	private List<Resident> residents = new ArrayList<>();
	private List<Resident> outlaws = new ArrayList<>();
	private List<Location> outpostSpawns = new ArrayList<>();
	private List<Location> jailSpawns = new ArrayList<>();
	private HashMap<String, PlotGroup> plotGroups = null;
	
	private Resident mayor;
	private int bonusBlocks = 0;
	private int purchasedBlocks = 0;
	private double taxes = TownySettings.getTownDefaultTax();
	private double plotTax= TownySettings.getTownDefaultPlotTax();
	private double commercialPlotTax = TownySettings.getTownDefaultShopTax();
	private double plotPrice = 0.0;
	private double embassyPlotTax = TownySettings.getTownDefaultEmbassyTax();
	private double commercialPlotPrice, embassyPlotPrice, spawnCost;
	private Nation nation;
	private boolean hasUpkeep = true;
	private boolean isPublic = TownySettings.getTownDefaultPublic();
	private boolean isTaxPercentage = TownySettings.getTownDefaultTaxPercentage();
	private boolean isOpen = TownySettings.getTownDefaultOpen();
	private String townBoard = "/town set board [msg]";
	private String tag = "";
	private TownBlock homeBlock;
	private TownyWorld world;
	private Location spawn;
	private boolean adminDisabledPVP = false; // This is a special setting to make a town ignore All PVP settings and keep PVP disabled.
	private boolean adminEnabledPVP = false; // This is a special setting to make a town ignore All PVP settings and keep PVP enabled. Overrides the admin disabled too.
	private UUID uuid;
	private long registered;
	private transient List<Invite> receivedinvites = new ArrayList<>();
	private transient List<Invite> sentinvites = new ArrayList<>();
	private boolean isConquered = false;
	private int conqueredDays;
	private EconomyAccount account;
	private List<TownBlock> townBlocks = new ArrayList<>();
	private TownyPermission permissions = new TownyPermission();
	private long recentlyRuinedEndTime;
	private long revoltImmunityEndTime;
	private long siegeImmunityEndTime;
	private Siege siege;
	private boolean occupied;
	private boolean neutral;
	private boolean desiredNeutralityValue;
	private int neutralityChangeConfirmationCounterDays;
	
	public Town(String name) {
		super(name);
		permissions.loadDefault(this);
		recentlyRuinedEndTime = 0;
		revoltImmunityEndTime = 0;
		siegeImmunityEndTime = System.currentTimeMillis()
			+ (long)(TownySettings.getWarSiegeSiegeImmunityTimeNewTownsHours() * ONE_HOUR_IN_MILLIS);
		siege = null;
		occupied = false;
		neutral = false;
		desiredNeutralityValue = false;
		neutralityChangeConfirmationCounterDays = 0;
	}

	@Override
	public void setTownblocks(List<TownBlock> townblocks) {
		this.townBlocks = townblocks;
	}

	@Override
	public List<TownBlock> getTownBlocks() {
		return townBlocks;
	}

	@Override
	public boolean hasTownBlock(TownBlock townBlock) {
		return townBlocks.contains(townBlock);
	}

	@Override
	public void addTownBlock(TownBlock townBlock) throws AlreadyRegisteredException {

		if (hasTownBlock(townBlock))
			throw new AlreadyRegisteredException();
		else {
			townBlocks.add(townBlock);
			if (townBlocks.size() == 1 && !hasHomeBlock())
				try {
					setHomeBlock(townBlock);
				} catch (TownyException ignored) {}
		}
	}

	public void setTag(String text) throws TownyException {

		if (text.length() > 4)
			throw new TownyException(TownySettings.getLangString("msg_err_tag_too_long"));
		this.tag = text.toUpperCase();
		if (this.tag.matches(" "))
			this.tag = "";
		Bukkit.getPluginManager().callEvent(new TownTagChangeEvent(this.tag, this));
	}

	public String getTag() {

		return tag;
	}

	public boolean hasTag() {

		return !tag.isEmpty();
	}

	public Resident getMayor() {

		return mayor;
	}

	public void setTaxes(double taxes) {

		if (isTaxPercentage) {
			this.taxes = Math.min(taxes, TownySettings.getMaxTaxPercent());
		} else {
			this.taxes = Math.min(taxes, TownySettings.getMaxTax());
		}
	}

	public double getTaxes() {

		setTaxes(taxes); //make sure the tax level is right.
		return taxes;
	}

	public void setMayor(Resident mayor) throws TownyException {

		if (!hasResident(mayor))
			throw new TownyException(TownySettings.getLangString("msg_err_mayor_doesnt_belong_to_town"));
		this.mayor = mayor;
		
		TownyPerms.assignPermissions(mayor, null);
	}

	public Nation getNation() throws NotRegisteredException {

		if (hasNation())
			return nation;
		else
			throw new NotRegisteredException(TownySettings.getLangString("msg_err_town_doesnt_belong_to_any_nation"));
	}

	public void setNation(Nation nation) throws AlreadyRegisteredException {

		if (nation == null) {
			this.nation = null;
			TownyPerms.updateTownPerms(this);
			return;
		}
		if (this.nation == nation)
			return;
		if (hasNation())
			throw new AlreadyRegisteredException();
		this.nation = nation;
		TownyPerms.updateTownPerms(this);
	}

	@Override
	public List<Resident> getResidents() {

		return residents;
	}

	public List<Resident> getAssistants() {

		List<Resident> assistants = new ArrayList<>();
		
		for (Resident assistant: residents) {
			if (assistant.hasTownRank("assistant"))
				assistants.add(assistant);
		}
		return assistants;
	}

	@Override
	public boolean hasResident(String name) {

		for (Resident resident : residents)
			if (resident.getName().equalsIgnoreCase(name))
				return true;
		return false;
	}

	public boolean hasResident(Resident resident) {

		return residents.contains(resident);
	}

	public boolean hasAssistant(Resident resident) {

		return getAssistants().contains(resident);
	}

	public void addResident(Resident resident) throws AlreadyRegisteredException {

		addResidentCheck(resident);
		residents.add(resident);
		resident.setTown(this);
		
		BukkitTools.getPluginManager().callEvent(new TownAddResidentEvent(resident, this));
	}

	public void addResidentCheck(Resident resident) throws AlreadyRegisteredException {

		if (hasResident(resident))
			throw new AlreadyRegisteredException(String.format(TownySettings.getLangString("msg_err_already_in_town"), resident.getName(), getFormattedName()));
		else if (resident.hasTown())
			try {
				if (!resident.getTown().equals(this))
					throw new AlreadyRegisteredException(String.format(TownySettings.getLangString("msg_err_already_in_town"), resident.getName(), resident.getTown().getFormattedName()));
			} catch (NotRegisteredException e) {
				e.printStackTrace();
			}
	}

	public boolean isMayor(Resident resident) {

		return resident == mayor;
	}

	public boolean hasNation() {

		return nation != null;
	}

	public int getNumResidents() {

		return residents.size();
	}

	public boolean isCapital() {

		return hasNation() && nation.isCapital(this);
	}

	public void setHasUpkeep(boolean hasUpkeep) {

		this.hasUpkeep = hasUpkeep;
	}

	public boolean hasUpkeep() {

		return hasUpkeep;
	}

	public void setHasMobs(boolean hasMobs) {

		this.permissions.mobs = hasMobs;
	}

	public boolean hasMobs() {

		return this.permissions.mobs;
	}

	public void setPVP(boolean isPVP) {

		this.permissions.pvp = isPVP;
	}
	
	public void setAdminDisabledPVP(boolean isPVPDisabled) {

		this.adminDisabledPVP = isPVPDisabled;
	}
	
	public void setAdminEnabledPVP(boolean isPVPEnabled) {

		this.adminEnabledPVP = isPVPEnabled;
	}

	public boolean isPVP() {

		// Admin has enabled PvP for this town.
		if (isAdminEnabledPVP()) 
			return true;
				
		// Admin has disabled PvP for this town.
		if (isAdminDisabledPVP()) 
			return false;

		if(TownySettings.getWarSiegeEnabled()
			&& TownySettings.getWarSiegePvpAlwaysOnInBesiegedTowns()
			&& !(TownySettings.getWarSiegeTownNeutralityEnabled() && isNeutral())
			&& siege != null
			&& siege.getStatus() == SiegeStatus.IN_PROGRESS) {
			return true;
		}

		return this.permissions.pvp;
	}
	
	public boolean isAdminDisabledPVP() {

		// Admin has disabled PvP for this town.
		return this.adminDisabledPVP;
	}
	
	public boolean isAdminEnabledPVP() {

		// Admin has enabled PvP for this town.
		return this.adminEnabledPVP;
	}

	public void setBANG(boolean isBANG) {

		this.permissions.explosion = isBANG;
	}

	public boolean isBANG() {

		return this.permissions.explosion;
	}

	public void setTaxPercentage(boolean isPercentage) {

		this.isTaxPercentage = isPercentage;
		if (this.getTaxes() > 100) {
			this.setTaxes(0);
		}
	}

	public boolean isTaxPercentage() {

		return isTaxPercentage;
	}

	public void setFire(boolean isFire) {

		this.permissions.fire = isFire;
	}

	public boolean isFire() {

		return this.permissions.fire;
	}

	public void setTownBoard(String townBoard) {

		this.townBoard = townBoard;
	}

	public String getTownBoard() {
		return townBoard;
	}

	public void setBonusBlocks(int bonusBlocks) {

		this.bonusBlocks = bonusBlocks;
	}

	public int getTotalBlocks() {

		return TownySettings.getMaxTownBlocks(this);
	}

	public int getBonusBlocks() {

		return bonusBlocks;
	}
	
	public double getBonusBlockCost() {
		return (Math.pow(TownySettings.getPurchasedBonusBlocksIncreaseValue() , getPurchasedBlocks()) * TownySettings.getPurchasedBonusBlocksCost());
	}
	
	public double getTownBlockCost() {
		return (Math.pow(TownySettings.getClaimPriceIncreaseValue(), getTownBlocks().size()) * TownySettings.getClaimPrice());
	}

	public double getTownBlockCostN(int inputN) throws TownyException {
		
		if (inputN < 0)
			throw new TownyException(TownySettings.getLangString("msg_err_negative"));

		if (inputN == 0)
			return inputN;
		
		double nextprice = getTownBlockCost();
		int i = 1;
		double cost = nextprice;
		while (i < inputN){
			nextprice = Math.round(Math.pow(TownySettings.getClaimPriceIncreaseValue() , getTownBlocks().size()+i) * TownySettings.getClaimPrice());			
			cost += nextprice;
			i++;
		}
		cost = Math.round(cost);
		return cost;
	}
	
	public double getBonusBlockCostN(int inputN) throws TownyException {
		
		if (inputN < 0)
			throw new TownyException(TownySettings.getLangString("msg_err_negative"));

		int current = getPurchasedBlocks();
		int n;
		if (current + inputN > TownySettings.getMaxPurchedBlocks(this)) {
			n = TownySettings.getMaxPurchedBlocks(this) - current;
		} else {
			n = inputN;
		}

		if (n == 0)
			return n;
		
		double nextprice = getBonusBlockCost();
		int i = 1;
		double cost = nextprice;
		while (i < n){
			nextprice = Math.round(Math.pow(TownySettings.getPurchasedBonusBlocksIncreaseValue() , getPurchasedBlocks()+i) * TownySettings.getPurchasedBonusBlocksCost());			
			cost += nextprice;
			i++;
		}
		cost = Math.round(cost);
		return cost;
	}

	public void addBonusBlocks(int bonusBlocks) {

		this.bonusBlocks += bonusBlocks;
	}

	public void setPurchasedBlocks(int purchasedBlocks) {

		this.purchasedBlocks = purchasedBlocks;
	}

	public int getPurchasedBlocks() {

		return purchasedBlocks;
	}

	public void addPurchasedBlocks(int purchasedBlocks) {

		this.purchasedBlocks += purchasedBlocks;
	}

	/**
	 * Sets the HomeBlock of a town
	 * 
	 * @param homeBlock - The TownBlock to set as the HomeBlock
	 * @return true if the HomeBlock was successfully set
	 * @throws TownyException if the TownBlock is not owned by the town
	 */
	public boolean setHomeBlock(TownBlock homeBlock) throws TownyException {

		if (homeBlock == null) {
			this.homeBlock = null;
			return false;
		}
		if (!hasTownBlock(homeBlock))
			throw new TownyException(TownySettings.getLangString("msg_err_town_has_no_claim_over_this_town_block"));
		this.homeBlock = homeBlock;

		// Set the world as it may have changed
		if (this.world != homeBlock.getWorld()) {
			if ((world != null) && (world.hasTown(this)))
				world.removeTown(this);

			setWorld(homeBlock.getWorld());
		}

		// Attempt to reset the spawn to make sure it's in the homeblock
		try {
			setSpawn(spawn);
		} catch (TownyException e) {
			// Spawn is not in the homeblock so null.
			spawn = null;
		} catch (NullPointerException e) {
			// In the event that spawn is already null
		}
		if (this.hasNation() && TownySettings.getNationRequiresProximity() > 0)
			if (!this.getNation().getCapital().equals(this)) {
				Nation nation = this.getNation();
				Coord capitalCoord = nation.getCapital().getHomeBlock().getCoord();
				Coord townCoord = this.getHomeBlock().getCoord();
				if (!nation.getCapital().getHomeBlock().getWorld().getName().equals(this.getHomeBlock().getWorld().getName())) {
					TownyMessaging.sendNationMessagePrefixed(nation, String.format(TownySettings.getLangString("msg_nation_town_moved_their_homeblock_too_far"), this.getName()));
					try {
						nation.removeTown(this);
					} catch (EmptyNationException e) {
						e.printStackTrace();
					}
				}
				double distance;
				distance = Math.sqrt(Math.pow(capitalCoord.getX() - townCoord.getX(), 2) + Math.pow(capitalCoord.getZ() - townCoord.getZ(), 2));			
				if (distance > TownySettings.getNationRequiresProximity()) {
					TownyMessaging.sendNationMessagePrefixed(nation, String.format(TownySettings.getLangString("msg_nation_town_moved_their_homeblock_too_far"), this.getName()));
					try {
						nation.removeTown(this);
					} catch (EmptyNationException e) {
						e.printStackTrace();
					}
				}	
			}
			
		return true;
	}
	
	/**
	 * Only to be called from the Loading methods.
	 * 
	 * @param homeBlock - TownBlock to forcefully set as HomeBlock
	 * @throws TownyException - General TownyException
	 */
	public void forceSetHomeBlock(TownBlock homeBlock) throws TownyException {

		if (homeBlock == null) {
			this.homeBlock = null;
			return;
		}

		this.homeBlock = homeBlock;

		// Set the world as it may have changed
		if (this.world != homeBlock.getWorld()) {
			if ((world != null) && (world.hasTown(this)))
				world.removeTown(this);

			setWorld(homeBlock.getWorld());
		}

	}

	public TownBlock getHomeBlock() throws TownyException {

		if (hasHomeBlock())
			return homeBlock;
		else
			throw new TownyException("Town has not set a home block.");
	}

	/**
	 * Sets the world this town belongs to. If it's a world change it will
	 * remove the town from the old world and place in the new.
	 * 
	 * @param world - TownyWorld to attribute a town to
	 */
	public void setWorld(TownyWorld world) {

		if (world == null) {
			this.world = null;
			return;
		}
		if (this.world == world)
			return;

		if (hasWorld()) {
			try {
				world.removeTown(this);
			} catch (NotRegisteredException ignored) {
			}
		}

		this.world = world;

		try {
			this.world.addTown(this);
		} catch (AlreadyRegisteredException ignored) {
		}
	}

	/**
	 * Fetch the World this town is registered too. If (for any reason) it's
	 * null it will attempt to find the owning world from TownyUniverse.
	 * 
	 * @return world or null
	 */
	public TownyWorld getWorld() {

		if (world != null)
			return world;

		return TownyUniverse.getInstance().getDataSource().getTownWorld(this.getName());
	}

	public boolean hasMayor() {

		return mayor != null;
	}

	public void removeResident(Resident resident) throws EmptyTownException, NotRegisteredException {

		if (!hasResident(resident)) {
			throw new NotRegisteredException();
		} else {

			if(TownySettings.getWarSiegeEnabled())
				SiegeWarMembershipController.evaluateTownRemoveResident(this, resident);

			remove(resident);

			if (getNumResidents() == 0) {
				throw new EmptyTownException(this);
			}
		}
	}

	private void removeAllResidents() {

		for (Resident resident : new ArrayList<>(residents))
			remove(resident);
	}

	private void remove(Resident resident) {
		
		resident.setTitle("");
		resident.setSurname("");
		resident.updatePerms();

		for (TownBlock townBlock : new ArrayList<>(resident.getTownBlocks())) {
			// Do not remove Embassy plots
			if (townBlock.getType() != TownBlockType.EMBASSY) {
				townBlock.setResident(null);
				try {
					townBlock.setPlotPrice(townBlock.getTown().getPlotPrice());
				} catch (NotRegisteredException e) {
					e.printStackTrace();
				}
				TownyUniverse.getInstance().getDataSource().saveTownBlock(townBlock);
				
				// Set the plot permissions to mirror the towns.
				townBlock.setType(townBlock.getType());
			}
		}

		if (isMayor(resident)) {

			if (residents.size() > 1) {
				for (Resident assistant : new ArrayList<>(getAssistants()))
					if ((assistant != resident) && (resident.hasTownRank("assistant"))) {
						try {
							setMayor(assistant);
							break;
						} catch (TownyException e) {
							// Error setting mayor.
							e.printStackTrace();
						}
					}
				if (isMayor(resident)) {
					// Still mayor and no assistants so pick a resident to be mayor
					for (Resident newMayor : new ArrayList<>(getResidents()))
						if (newMayor != resident) {
							try {
								setMayor(newMayor);
								break;
							} catch (TownyException e) {
								// Error setting mayor.
								e.printStackTrace();
							}
						}
				}
			}

		}

		try {
			/* 
			 * Trigger a resident removal event if they are in a town.
			 */
			if (resident.hasTown()) {
				BukkitTools.getPluginManager().callEvent(new TownRemoveResidentEvent(resident, resident.getTown()));
			}
			resident.setTown(null);
		} catch (AlreadyRegisteredException ignored) {
		} catch (IllegalStateException | NotRegisteredException e) {
			e.printStackTrace();
		}
		residents.remove(resident);
	}

	public void setSpawn(Location spawn) throws TownyException {

		if (!hasHomeBlock())
			throw new TownyException(TownySettings.getLangString("msg_err_homeblock_has_not_been_set"));
		Coord spawnBlock = Coord.parseCoord(spawn);
		if (homeBlock.getX() == spawnBlock.getX() && homeBlock.getZ() == spawnBlock.getZ()) {
			this.spawn = spawn;
		} else
			throw new TownyException(TownySettings.getLangString("msg_err_spawn_not_within_homeblock"));
	}
	
	/**
	 * Only to be called from the Loading methods.
	 * 
	 * @param spawn - Location to forcefully set as town spawn
	 */
	public void forceSetSpawn(Location spawn) {

		this.spawn = spawn;

	}

	/**
	 * Gets the Town's spawn location
	 * 
	 * @return Location of the town spawn
	 * @throws TownyException if no town spawn has been set (null)
	 */
	public Location getSpawn() throws TownyException {

		if (hasHomeBlock() && spawn != null) {
			return spawn;
		}

		else {
			this.spawn = null;
			throw new TownyException(TownySettings.getLangString("msg_err_town_has_not_set_a_spawn_location"));
		}
	}

	public boolean hasSpawn() {

		return (hasHomeBlock() && spawn != null);
	}

	public boolean hasHomeBlock() {

		return homeBlock != null;
	}

	public void clear() throws EmptyNationException {

		//Cleanup
		removeAllResidents();
		mayor = null;
		residents.clear();
		outlaws.clear();
		homeBlock = null;
		outpostSpawns.clear();
		jailSpawns.clear();
		revoltImmunityEndTime = 0;
		siegeImmunityEndTime = 0;

//		try {                                               This section is being removed because the only method that calls town.clear() already does a check for the nation, 
//			if (hasWorld()) {                               and later on also saves the world. Still not understood, is whether world.removeTownblocks would even remove townblocks
//				world.removeTownBlocks(getTownBlocks());    which exist in other worlds beside the one in which the town spawn resides. Removed as of 0.94.0.5 by LlmDl.
//				world.removeTown(this);
//			}
//		} catch (NotRegisteredException e) {
//		}
//		if (hasNation())
//			try {
//				nation.removeTown(this);
//			} catch (NotRegisteredException e) {
//			}
	}

	public boolean hasWorld() {

		return world != null;
	}

	@Override
	public void removeTownBlock(TownBlock townBlock) throws NotRegisteredException {

		if (!hasTownBlock(townBlock))
			throw new NotRegisteredException();
		else {
			// Remove the spawn point for this outpost.
			if (townBlock.isOutpost())
				removeOutpostSpawn(townBlock.getCoord());
			if (townBlock.isJail())
				removeJailSpawn(townBlock.getCoord());
			
			// Clear the towns homeblock if this is it.
			try {
				if (getHomeBlock() == townBlock)
					setHomeBlock(null);
			} catch (TownyException ignored) {}
			townBlocks.remove(townBlock);
			TownyUniverse.getInstance().getDataSource().saveTown(this);
		}
	}

	@Override
	public void setPermissions(String line) {
		permissions.load(line);
	}

	@Override
	public TownyPermission getPermissions() {
		return permissions;
	}

	/**
	 * Add or update an outpost spawn
	 * 
	 * @param spawn - Location to set an outpost's spawn point
	 * @throws TownyException if the Location is not within an Outpost plot.
	 */
	public void addOutpostSpawn(Location spawn) throws TownyException {

		removeOutpostSpawn(Coord.parseCoord(spawn));

		Coord spawnBlock = Coord.parseCoord(spawn);

		try {
			TownBlock outpost = TownyUniverse.getInstance().getDataSource().getWorld(spawn.getWorld().getName()).getTownBlock(spawnBlock);
			if (outpost.getX() == spawnBlock.getX() && outpost.getZ() == spawnBlock.getZ()) {
				if (!outpost.isOutpost())
					throw new TownyException(TownySettings.getLangString("msg_err_location_is_not_within_an_outpost_plot"));

				outpostSpawns.add(spawn);
			}

		} catch (NotRegisteredException e) {
			throw new TownyException(TownySettings.getLangString("msg_err_location_is_not_within_a_town"));
		}

	}
	
	/**
	 * Only to be called from the Loading methods.
	 * 
	 * @param spawn - Location to set Outpost's spawn point
	 */
	public void forceAddOutpostSpawn(Location spawn) {

		outpostSpawns.add(spawn);


	}

	/**
	 * Return the Location for this Outpost index.
	 * 
	 * @param index - Numeric identifier of an Outpost
	 * @return Location of Outpost's spawn
	 * @throws TownyException if there are no Outpost spawns set
	 */
	public Location getOutpostSpawn(Integer index) throws TownyException {

		if (getMaxOutpostSpawn() == 0 && TownySettings.isOutpostsLimitedByLevels())
			throw new TownyException(TownySettings.getLangString("msg_err_town_has_no_outpost_spawns_set"));

		return outpostSpawns.get(Math.min(getMaxOutpostSpawn() - 1, Math.max(0, index - 1)));
	}

	public int getMaxOutpostSpawn() {

		return outpostSpawns.size();
	}

	public boolean hasOutpostSpawn() {

		return (outpostSpawns.size() > 0);
	}

	/**
	 * Get an unmodifiable List of all outpost spawns.
	 * 
	 * @return List of outpostSpawns
	 */
	public List<Location> getAllOutpostSpawns() {

		return outpostSpawns;
	}

	public void removeOutpostSpawn(Coord coord) {

		for (Location spawn : new ArrayList<>(outpostSpawns)) {
			Coord spawnBlock = Coord.parseCoord(spawn);
			if ((coord.getX() == spawnBlock.getX()) && (coord.getZ() == spawnBlock.getZ())) {
				outpostSpawns.remove(spawn);
			}
		}
	}

	public void setPlotPrice(double plotPrice) {
		this.plotPrice = Math.min(plotPrice, TownySettings.getMaxPlotPrice());
	}

	public double getPlotPrice() {

		return plotPrice;
	}

	public double getPlotTypePrice(TownBlockType type) {

		double plotPrice;
		switch (type.ordinal()) {
		case 1:
			plotPrice = getCommercialPlotPrice();
			break;
		case 3:
			plotPrice = getEmbassyPlotPrice();
			break;
		default:
			plotPrice = getPlotPrice();

		}
		// check price isn't negative
		if (plotPrice < 0)
			plotPrice = 0;

		return plotPrice;
	}

	public void setCommercialPlotPrice(double commercialPlotPrice) {
		this.commercialPlotPrice = Math.min(commercialPlotPrice, TownySettings.getMaxPlotPrice());
	}

	public double getCommercialPlotPrice() {

		return commercialPlotPrice;
	}

	public void setEmbassyPlotPrice(double embassyPlotPrice) {
		this.embassyPlotPrice = Math.min(embassyPlotPrice, TownySettings.getMaxPlotPrice());
	}

	public double getEmbassyPlotPrice() {

		return embassyPlotPrice;
	}
	
	public void setSpawnCost(double spawnCost) {

		this.spawnCost = spawnCost;
	}

	public double getSpawnCost() {

		return spawnCost;
	}

	public boolean isHomeBlock(TownBlock townBlock) {

		return hasHomeBlock() && townBlock == homeBlock;
	}

	public void setPlotTax(double plotTax) {
		this.plotTax = Math.min(plotTax, TownySettings.getMaxTax());
	}

	public double getPlotTax() {

		return plotTax;
	}

	public void setCommercialPlotTax(double commercialTax) {
		this.commercialPlotTax = Math.min(commercialTax, TownySettings.getMaxTax());
	}

	public double getCommercialPlotTax() {

		return commercialPlotTax;
	}

	public void setEmbassyPlotTax(double embassyPlotTax) {
		this.embassyPlotTax = Math.min(embassyPlotTax, TownySettings.getMaxTax());
	}

	public double getEmbassyPlotTax() {

		return embassyPlotTax;
	}

	public void setOpen(boolean isOpen) {

		this.isOpen = isOpen;
	}

	public boolean isOpen() {
		//If town is under siege, public joining is disabled
		if(TownySettings.getWarSiegeEnabled()
			&& TownySettings.getWarSiegeBesiegedTownRecruitmentDisabled()
			&& hasSiege()
			&& getSiege().getStatus() == SiegeStatus.IN_PROGRESS)
		{
			return false;
		}
		else
		{
			return isOpen;
		}
	}

	public void collect(double amount) throws EconomyException {
		
		if (TownySettings.isUsingEconomy()) {
			double bankcap = TownySettings.getTownBankCap();
			if (bankcap > 0) {
				if (amount + getAccount().getHoldingBalance() > bankcap) {
					TownyMessaging.sendPrefixedTownMessage(this, String.format(TownySettings.getLangString("msg_err_deposit_capped"), bankcap));
					return;
				}
			}
			
			getAccount().collect(amount, null);
		}

	}

	@Override
	public void withdrawFromBank(Resident resident, int amount) throws EconomyException, TownyException {

		//if (!isMayor(resident))// && !hasAssistant(resident))
		//	throw new TownyException("You don't have access to the town's bank.");

		if (TownySettings.isUsingEconomy()) {
			if (!getAccount().payTo(amount, resident, "Town Withdraw"))
				throw new TownyException(TownySettings.getLangString("msg_err_no_money"));
		} else
			throw new TownyException(TownySettings.getLangString("msg_err_no_economy"));

	}

	@Override
	public List<String> getTreeString(int depth) {

		List<String> out = new ArrayList<>();
		out.add(getTreeDepth(depth) + "Town (" + getName() + ")");
		out.add(getTreeDepth(depth + 1) + "Mayor: " + (hasMayor() ? getMayor().getName() : "None"));
		out.add(getTreeDepth(depth + 1) + "Home: " + homeBlock);
		out.add(getTreeDepth(depth + 1) + "Bonus: " + bonusBlocks);
		out.add(getTreeDepth(depth + 1) + "TownBlocks (" + getTownBlocks().size() + "): " /*
																						 * +
																						 * getTownBlocks
																						 * (
																						 * )
																						 */);
		List<Resident> assistants = getAssistants();
		
		if (assistants.size() > 0)
			out.add(getTreeDepth(depth + 1) + "Assistants (" + assistants.size() + "): " + Arrays.toString(assistants.toArray(new Resident[0])));
		
		out.add(getTreeDepth(depth + 1) + "Residents (" + getResidents().size() + "):");
		for (Resident resident : getResidents())
			out.addAll(resident.getTreeString(depth + 2));
		return out;
	}

	public void setPublic(boolean isPublic) {

		this.isPublic = isPublic;
	}

	public boolean isPublic() {

		return isPublic;
	}
	
	public List<Location> getJailSpawns() {

		return jailSpawns;
	}

	public void addJailSpawn(Location spawn) throws TownyException {
		removeJailSpawn(Coord.parseCoord(spawn));
		
		Coord spawnBlock = Coord.parseCoord(spawn);
		TownyUniverse townyUniverse = TownyUniverse.getInstance();
		try {
			TownBlock jail = townyUniverse.getDataSource().getWorld(spawn.getWorld().getName()).getTownBlock(spawnBlock);
			if (jail.getX() == spawnBlock.getX() && jail.getZ() == spawnBlock.getZ()) {
				if (!jail.isJail())
					throw new TownyException(TownySettings.getLangString("msg_err_location_is_not_within_a_jail_plot"));
				
				jailSpawns.add(spawn);
				townyUniverse.getDataSource().saveTown(this);
			}

		} catch (NotRegisteredException e) {
			throw new TownyException(TownySettings.getLangString("msg_err_location_is_not_within_a_town"));
		}

	}
	
	public void removeJailSpawn(Coord coord) {

		for (Location spawn : new ArrayList<>(jailSpawns)) {
			Coord spawnBlock = Coord.parseCoord(spawn);
			if ((coord.getX() == spawnBlock.getX()) && (coord.getZ() == spawnBlock.getZ())) {
				jailSpawns.remove(spawn);
				TownyUniverse.getInstance().getDataSource().saveTown(this);
			}
		}
	}

	/**
	 * Only to be called from the Loading methods.
	 * 
	 * @param spawn - Location to set a Jail's spawn
	 */
	public void forceAddJailSpawn(Location spawn) {

		jailSpawns.add(spawn);
	}

	/**
	 * Return the Location for this Jail index.
	 * 
	 * @param index - Numerical identifier of a Town Jail
	 * @return Location of a jail spawn
	 * @throws TownyException if there are no jail spawns set
	 */
	public Location getJailSpawn(Integer index) throws TownyException {

		if (getMaxJailSpawn() == 0)
			throw new TownyException(TownySettings.getLangString("msg_err_town_has_no_jail_spawns_set"));

		return jailSpawns.get(Math.min(getMaxJailSpawn() - 1, Math.max(0, index - 1)));
	}

	public int getMaxJailSpawn() {

		return jailSpawns.size();
	}

	public boolean hasJailSpawn() {

		return (jailSpawns.size() > 0);
	}
	
	/**
	 * Get an unmodifiable List of all jail spawns.
	 * 
	 * @return List of jailSpawns
	 */
	public List<Location> getAllJailSpawns() {

		return Collections.unmodifiableList(jailSpawns);
	}

	@Override
	public List<Resident> getOutlaws() {

		return outlaws;
	}
	
	public boolean hasOutlaw (String name) {
		
		for (Resident outlaw : outlaws)
			if (outlaw.getName().equalsIgnoreCase(name))
				return true;
		return false;		
	}
	
	public boolean hasOutlaw(Resident outlaw) {

		return outlaws.contains(outlaw);
	}
	
	public void addOutlaw(Resident resident) throws AlreadyRegisteredException {

		addOutlawCheck(resident);
		outlaws.add(resident);
	}
	
	public void addOutlawCheck(Resident resident) throws AlreadyRegisteredException {

		if (hasOutlaw(resident))
			throw new AlreadyRegisteredException(TownySettings.getLangString("msg_err_resident_already_an_outlaw"));
		else if (resident.hasTown())
			try {
				if (resident.getTown().equals(this))
					throw new AlreadyRegisteredException(TownySettings.getLangString("msg_err_not_outlaw_in_your_town"));
			} catch (NotRegisteredException e) {
				e.printStackTrace();
			}
	}
	
	public void removeOutlaw(Resident resident) throws NotRegisteredException {

		if (!hasOutlaw(resident))
			throw new NotRegisteredException();
		else 
			outlaws.remove(resident);			
	}

	public UUID getUuid() {
		return uuid;
	}

	public void setUuid(UUID uuid) {
		this.uuid = uuid;
	}

	public boolean hasValidUUID() {
		return uuid != null;
	}

	public void setRegistered(long registered) {
		this.registered = registered;
	}

	public long getRegistered() {
		return registered;
	}

	public void setOutpostSpawns(List<Location> outpostSpawns) {
		this.outpostSpawns = outpostSpawns;
	}

	public boolean isAlliedWith(Town othertown) {
		if (this.hasNation() && othertown.hasNation()) {
			try {
				if (this.getNation().hasAlly(othertown.getNation())) {
					return true;
				} else {
					return this.getNation().equals(othertown.getNation());
				}
			} catch (NotRegisteredException e) {
				return false;
			}
		} else {
			return false;
		}
	}

	@Override
	public List<Invite> getReceivedInvites() {
		return receivedinvites;
	}

	@Override
	public void newReceivedInvite(Invite invite) throws TooManyInvitesException {
		if (receivedinvites.size() <= (InviteHandler.getReceivedInvitesMaxAmount(this) -1)) { // We only want 10 Invites, for towns, later we can make this number configurable
			receivedinvites.add(invite);

		} else {
			throw new TooManyInvitesException(String.format(TownySettings.getLangString("msg_err_town_has_too_many_invites"),this.getName()));
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
	public void newSentInvite(Invite invite)  throws TooManyInvitesException {
		if (sentinvites.size() <= (InviteHandler.getSentInvitesMaxAmount(this) -1)) { // We only want 35 Invites, for towns, later we can make this number configurable
			sentinvites.add(invite);
		} else {
			throw new TooManyInvitesException(TownySettings.getLangString("msg_err_town_sent_too_many_invites"));
		}
	}

	@Override
	public void deleteSentInvite(Invite invite) {
		sentinvites.remove(invite);
	}

	public int getOutpostLimit() {
		return TownySettings.getMaxOutposts(this);
	}

	public boolean isOverOutpostLimit() {
		
		return (getMaxOutpostSpawn() > getOutpostLimit());

	}
	
	public boolean isOverClaimed() {
		
		return (getTownBlocks().size() > TownySettings.getMaxTownBlocks(this));
	}
	
    @Override
	public void addMetaData(CustomDataField<?> md) {
		super.addMetaData(md);

		TownyUniverse.getInstance().getDataSource().saveTown(this);
	}

	@Override
	public void removeMetaData(CustomDataField<?> md) {
		super.removeMetaData(md);

		TownyUniverse.getInstance().getDataSource().saveTown(this);
	}
	
	public void setConquered(boolean conquered) {
		this.isConquered = conquered;
	}
	
	public boolean isConquered() {
		return this.isConquered;
	}
	
	public void setConqueredDays(int conqueredDays) {
		this.conqueredDays = conqueredDays;
	}
	
	public int getConqueredDays() {
		return this.conqueredDays;
	}

	public Siege getSiege() {
		return siege;
	}

	public void setSiege(Siege siege) {
		this.siege = siege;
	}

	public boolean isSiegeImmunityActive() {
		if(hasSiege() && siege.getStatus() == SiegeStatus.IN_PROGRESS)
			return false; //Cooldown always off until the siege has finished

		if(System.currentTimeMillis() < siegeImmunityEndTime) {
			return true;
		} else {
			return false;
		}
	}

	public boolean isRevoltImmunityActive() {
		return System.currentTimeMillis() < revoltImmunityEndTime;
	}

	public void setSiegeImmunityEndTime(long endTimeMillis) {
		this.siegeImmunityEndTime = endTimeMillis;
	}

	public void setRevoltImmunityEndTime(long timeMillis) {
		this.revoltImmunityEndTime = timeMillis;
	}

	public boolean hasSiege() {
		return siege != null;
	}

	public long getSiegeImmunityEndTime() {
		return siegeImmunityEndTime;
	}

	public long getRevoltImmunityEndTime() {
		return revoltImmunityEndTime;
	}

	public String getFormattedHoursUntilRevoltCooldownEnds() {
		double hoursRemainingMillis = revoltImmunityEndTime - System.currentTimeMillis();
		return TimeMgmt.getFormattedTimeValue(hoursRemainingMillis);
	}

	public double getPlunderValue() {
		return TownySettings.getWarSiegeAttackerPlunderAmountPerPlot() * townBlocks.size();
	}

	public double getSiegeCost() {
		return TownySettings.getWarSiegeAttackerCostUpFrontPerPlot() * townBlocks.size();
	}

	public String getFormattedHoursUntilSiegeImmunityEnds() {
		double hoursUntilSiegeCooldownEnds = siegeImmunityEndTime - System.currentTimeMillis();
		return TimeMgmt.getFormattedTimeValue(hoursUntilSiegeCooldownEnds);
	}

	public boolean isRuined() {
		return (residents.size() == 0 || recentlyRuinedEndTime > 0);
	}

	/*
	888 = just ruined.
	999 = 1 upkeep cycle completed since being ruined.
	 */
	public void setRecentlyRuinedEndTime(long recentlyRuinedEndTime) {
		this.recentlyRuinedEndTime = recentlyRuinedEndTime;
	}

	public long getRecentlyRuinedEndTime() {
		return recentlyRuinedEndTime;
	}

	public void setOccupied(boolean occupied) {
		this.occupied = occupied;
	}

	public boolean isOccupied() {
		return this.occupied;
	}
	
	public List<TownBlock> getTownBlocksForPlotGroup(PlotGroup group) {
		
		ArrayList<TownBlock> retVal = new ArrayList<>();
		
		TownyMessaging.sendErrorMsg(group.toString());
		
		for (TownBlock townBlock : getTownBlocks()) {
			if (townBlock.hasPlotObjectGroup() && townBlock.getPlotObjectGroup().equals(group))
				retVal.add(townBlock);
		}
		
		return retVal;
	}
	
	public void renamePlotGroup(String oldName, PlotGroup group) {
		plotGroups.remove(oldName);
		plotGroups.put(group.getName(), group);
	}
	
	public void addPlotGroup(PlotGroup group) {
		if (!hasObjectGroups()) 
			plotGroups = new HashMap<>();
		
		plotGroups.put(group.getName(), group);
		
	}
	
	public void removePlotGroup(PlotGroup plotGroup) {
		if (hasObjectGroups() && plotGroups.remove(plotGroup.getName()) != null) {
			for (TownBlock tb : getTownBlocks()) {
				if (tb.hasPlotObjectGroup() && tb.getPlotObjectGroup().equals(plotGroup)) {
					tb.getPlotObjectGroup().setID(null);
					TownyUniverse.getInstance().getDataSource().saveTownBlock(tb);
				}
			}
		}
	}

	// Abstract to collection in case we want to change structure in the future
	@Override
	public Collection<PlotGroup> getObjectGroups() {
		
		if (plotGroups == null)
			return null;
		
		return plotGroups.values();
	}

	// Method is inefficient compared to getting the group from name.
	@Override
	public PlotGroup getObjectGroupFromID(UUID ID) {
		if (hasObjectGroups()) {
			for (PlotGroup pg : getObjectGroups()) {
				if (pg.getID().equals(ID)) 
					return pg;
			}
		}
		
		return null;
	}

	@Override
	public boolean hasObjectGroups() {
		return plotGroups != null;
	}

	// Override default method for efficient access
	@Override
	public boolean hasObjectGroupName(String name) {
		return hasObjectGroups() && plotGroups.containsKey(name);
	}

	public PlotGroup getPlotObjectGroupFromName(String name) {
		if (hasObjectGroups()) {
			return plotGroups.get(name);
		}
		
		return null;
	}
	
	// Wraps other functions to provide a better naming scheme for the end developer.
	public PlotGroup getPlotObjectGroupFromID(UUID ID) {
		return getObjectGroupFromID(ID);
	}
	
	public Collection<PlotGroup> getPlotObjectGroups() {
		return getObjectGroups();
	}

	@Override
	public EconomyAccount getAccount() {
		if (account == null) {
			
			String accountName = StringMgmt.trimMaxLength(Town.ECONOMY_ACCOUNT_PREFIX + getName(), 32);
			World world;

			if (hasWorld()) {
				world = BukkitTools.getWorld(getWorld().getName());
			} else {
				world = BukkitTools.getWorlds().get(0);
			}
			
			account = new EconomyAccount(accountName, world);
		}
		
		return account;
	}

	@Override
	public String getFormattedName() {
		if (this.isCapital()) {
			return TownySettings.getCapitalPrefix(this) + this.getName().replaceAll("_", " ") + TownySettings.getCapitalPostfix(this);
		}
		
		return TownySettings.getTownPrefix(this) + this.getName().replaceAll("_", " ") + TownySettings.getTownPostfix(this);
	}

	/**
	 * @deprecated As of 0.97.0.0+ please use {@link EconomyAccount#getWorld()} instead.
	 * 
	 * @return The world this resides in.
	 */
	@Deprecated
	public World getBukkitWorld() {
		if (hasWorld()) {
			return BukkitTools.getWorld(getWorld().getName());
		} else {
			return BukkitTools.getWorlds().get(0);
		}
	}

	/**
	 * @deprecated As of 0.97.0.0+ please use {@link EconomyAccount#getName()} instead.
	 * 
	 * @return The name of the economy account.
	 */
	@Deprecated
	public String getEconomyName() {
		return StringMgmt.trimMaxLength(Town.ECONOMY_ACCOUNT_PREFIX + getName(), 32);
	}

	public boolean isNeutral() {
		return neutral;
	}

	public int getNeutralityChangeConfirmationCounterDays() {
		return neutralityChangeConfirmationCounterDays;
	}

	public void decrementNeutralityChangeConfirmationCounterDays() {
		neutralityChangeConfirmationCounterDays--;
	}

	public void flipNeutral() {
		neutral = !neutral;
	}

	public void flipDesiredNeutralityValue() {
		desiredNeutralityValue = !desiredNeutralityValue;
	}

	public void setNeutralityChangeConfirmationCounterDays(int counterValueDays) {
		neutralityChangeConfirmationCounterDays = counterValueDays;
	}

	public void setDesiredNeutralityValue(boolean value) {
		desiredNeutralityValue = value;
	}

	public boolean getDesiredNeutralityValue() {
		return desiredNeutralityValue;
	}

	public void setNeutral(boolean value) {
		neutral = value;
	}

	/**
	 * @deprecated as of 0.95.2.15, please use {@link EconomyAccount#getHoldingBalance()} instead.
	 * 
	 * @return the holding balance of the economy account.
	 * @throws EconomyException
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
		if (TownySettings.getBoolean(ConfigNodes.ECO_CLOSED_ECONOMY_ENABLED)) {
			return getAccount().payTo(amount, SERVER_ACCOUNT, reason);
		} else {
			return getAccount()._pay(amount);
		}
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
		return getAccount().collect(amount, reason);
	}
}
