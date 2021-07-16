package com.palmergames.bukkit.towny.object;

import com.palmergames.bukkit.towny.TownyAPI;
import com.palmergames.bukkit.towny.TownyEconomyHandler;
import com.palmergames.bukkit.towny.TownyMessaging;
import com.palmergames.bukkit.towny.TownySettings;
import com.palmergames.bukkit.towny.TownyUniverse;
import com.palmergames.bukkit.towny.event.NationAddTownEvent;
import com.palmergames.bukkit.towny.event.NationRemoveTownEvent;
import com.palmergames.bukkit.towny.event.town.TownMapColourCalculationEvent;
import com.palmergames.bukkit.towny.exceptions.AlreadyRegisteredException;
import com.palmergames.bukkit.towny.exceptions.EmptyNationException;
import com.palmergames.bukkit.towny.exceptions.EmptyTownException;
import com.palmergames.bukkit.towny.exceptions.NotRegisteredException;
import com.palmergames.bukkit.towny.exceptions.TownyException;
import com.palmergames.bukkit.towny.object.SpawnPoint.SpawnPointType;
import com.palmergames.bukkit.towny.object.jail.Jail;
import com.palmergames.bukkit.towny.object.metadata.CustomDataField;
import com.palmergames.bukkit.towny.permissions.TownyPerms;
import com.palmergames.bukkit.util.BukkitTools;
import com.palmergames.util.MathUtil;
import com.palmergames.util.StringMgmt;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class Town extends Government implements TownBlockOwner {

	private static final String ECONOMY_ACCOUNT_PREFIX = TownySettings.getTownAccountPrefix();

	private final List<Resident> residents = new ArrayList<>();
	private final List<Resident> outlaws = new ArrayList<>();
	private final Set<Resident> trustedResidents = new HashSet<>();
	private List<Location> outpostSpawns = new ArrayList<>();
	private List<Jail> jails = null;
	private HashMap<String, PlotGroup> plotGroups = null;
	
	private Resident mayor;
	private int bonusBlocks = 0;
	private int purchasedBlocks = 0;
	private double plotTax= TownySettings.getTownDefaultPlotTax();
	private double commercialPlotTax = TownySettings.getTownDefaultShopTax();
	private double plotPrice = 0.0;
	private double embassyPlotTax = TownySettings.getTownDefaultEmbassyTax();
	private double maxPercentTaxAmount = TownySettings.getMaxTownTaxPercentAmount();
	private double commercialPlotPrice, embassyPlotPrice;
	private double debtBalance = 0.0;
	private Nation nation;
	private boolean hasUpkeep = true;
	private boolean isTaxPercentage = TownySettings.getTownDefaultTaxPercentage();
	private TownBlock homeBlock;
	private TownyWorld world;
	private boolean adminDisabledPVP = false; // This is a special setting to make a town ignore All PVP settings and keep PVP disabled.
	private boolean adminEnabledPVP = false; // This is a special setting to make a town ignore All PVP settings and keep PVP enabled. Overrides the admin disabled too.
	private boolean isConquered = false;
	private int conqueredDays;
	private final ConcurrentHashMap<WorldCoord, TownBlock> townBlocks = new ConcurrentHashMap<>();
	private final TownyPermission permissions = new TownyPermission();
	private boolean ruined = false;
	private long ruinedTime;
	private long joinedNationAt;
	private long movedHomeBlockAt;
	private Jail primaryJail;

	public Town(String name) {
		super(name);
		permissions.loadDefault(this);
		
		// Set defaults.
		setTaxes(TownySettings.getTownDefaultTax());
		setOpen(TownySettings.getTownDefaultOpen());
		setBoard(TownySettings.getTownDefaultBoard());
		setNeutral(TownySettings.getTownDefaultNeutral());
	}
	
	public Town(String name, UUID uuid) {
		this(name);
		setUUID(uuid);
	}

	@Override
	public Collection<TownBlock> getTownBlocks() {
		return Collections.unmodifiableCollection(townBlocks.values());
	}

	@Override
	public boolean hasTownBlock(TownBlock townBlock) {
		return hasTownBlock(townBlock.getWorldCoord());
	}

	public boolean hasTownBlock(WorldCoord worldCoord) {
		return townBlocks.containsKey(worldCoord);
	}
	
	@Override
	public void addTownBlock(TownBlock townBlock) throws AlreadyRegisteredException {

		if (hasTownBlock(townBlock))
			throw new AlreadyRegisteredException();
		else {
			townBlocks.put(townBlock.getWorldCoord(), townBlock);
			if (townBlocks.size() < 2 && !hasHomeBlock())
				setHomeBlock(townBlock);
		}
	}
	
	public TownBlock getTownBlock(WorldCoord worldCoord) {
		if (hasTownBlock(worldCoord))
			return townBlocks.get(worldCoord);
		return null;
	}
	
	public ConcurrentHashMap<WorldCoord, TownBlock> getTownBlockMap() {
		return townBlocks;
	}

	public Resident getMayor() {

		return mayor;
	}

	public void setTaxes(double taxes) {
		this.taxes = Math.min(taxes, isTaxPercentage ? TownySettings.getMaxTownTaxPercent() : TownySettings.getMaxTownTax());
		
		// Fix invalid taxes
		if (this.taxes < 0)
			this.taxes = TownySettings.getTownDefaultTax();
	}

	/**
	 * Sets a resident to become mayor. Used only in database loading.
	 * 
	 * @param mayor - Town Resident to make into mayor.
	 * @throws TownyException - When given mayor is not a resident.
	 */
	public void forceSetMayor(Resident mayor) throws TownyException {

		if (!hasResident(mayor))
			throw new TownyException(Translation.of("msg_err_mayor_doesnt_belong_to_town"));
		
		setMayor(mayor);
	}
	
	/**
	 * Sets a town resident to become mayor.
	 * 
	 * @param mayor - Resident to become mayor.
	 */
	public void setMayor(Resident mayor) {
		if (!hasResident(mayor))
			return;
				
		this.mayor = mayor;
		
		TownyPerms.assignPermissions(mayor, null);	
	}

	public Nation getNation() throws NotRegisteredException {

		if (hasNation())
			return nation;
		else
			throw new NotRegisteredException(Translation.of("msg_err_town_doesnt_belong_to_any_nation"));
	}
	
	/**
	 * Safe to use as long as {@link #hasNation()} has returned true.
	 * 
	 * @return Nation of the Town or null if no Nation.
	 */
	@Nullable
	public Nation getNationOrNull() {
		return nation;
	}

	public void removeNation() {

		if (!hasNation())
			return;
		
		Nation nation = this.nation;
				
		for (Resident res : getResidents()) {
			if (res.hasTitle() || res.hasSurname()) {
				res.setTitle("");
				res.setSurname("");
			}
			res.updatePermsForNationRemoval();
			res.save();
		}

		try {
			nation.removeTown(this);
		} catch (EmptyNationException e) {
			TownyUniverse.getInstance().getDataSource().removeNation(nation);
			TownyMessaging.sendGlobalMessage(Translation.of("msg_del_nation", e.getNation().getName()));
		}
		
		try {
			setNation(null);
		} catch (AlreadyRegisteredException ignored) {
			// Cannot occur when setting null;
		}
		
		//The town is no longer conquered/occupied because it has left the nation
		this.isConquered = false;
		this.conqueredDays = 0;

		setJoinedNationAt(0);
		
		this.save();
		BukkitTools.getPluginManager().callEvent(new NationRemoveTownEvent(this, nation));
	}
	
	public void setNation(Nation nation) throws AlreadyRegisteredException {
		setNation(nation, true);
	}
	
	public void setNation(Nation nation, boolean updateJoinedAt) throws AlreadyRegisteredException {

		if (this.nation == nation)
			return;

		if (nation == null) {
			this.nation = null;
			return;
		}

		if (hasNation())
			throw new AlreadyRegisteredException();

		this.nation = nation;
		nation.addTown(this);

		if (updateJoinedAt)
			setJoinedNationAt(System.currentTimeMillis());

		TownyPerms.updateTownPerms(this);
		BukkitTools.getPluginManager().callEvent(new NationAddTownEvent(this, nation));
	}

	private boolean residentsSorted = false;

	@Override
	public List<Resident> getResidents() {
		if (!residentsSorted)
			sortResidents();
		
		return Collections.unmodifiableList(residents);
	}

	/**
	 * @deprecated as of 0.96.3.0, use {@link #getRank(String)} instead.
	 * @return Returns a list of residents with the assistant rank.
	 */
	@Deprecated
	public List<Resident> getAssistants() {

	    return getRank("assistant");
	}

	public List<Resident> getRank(String rank) {

		List<Resident> residentsWithRank = new ArrayList<>();
		
		for (Resident resident : getResidents()) {
			if (resident.hasTownRank(rank))
				residentsWithRank.add(resident);
		}
		return Collections.unmodifiableList(residentsWithRank);
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

	public boolean hasResident(Player player) {
		
		return hasResident(player.getUniqueId());
	}
	
	public boolean hasResident(UUID uuid) {
		
		Resident resident = TownyAPI.getInstance().getResident(uuid);
		return resident != null && hasResident(resident);
	}
	
	/**
	 * @deprecated Since 0.96.3.0, use {@link Resident#hasTownRank(String)} (using "assistant" as argument) instead.
	 * Whether a resident has an assistant role or not.
	 * 
	 * @param resident - Resident to check for a rank.
	 * @return A true if the resident is an assistant, false otherwise.
	 */
	@Deprecated
	public boolean hasAssistant(Resident resident) {

		return resident.hasTownRank("assistant");
	}
	
	public boolean hasResidentWithRank(Resident resident, String rank) {
		return hasResident(resident) && resident.hasTownRank(rank);
	}

	void addResident(Resident resident) {
		residents.add(resident);
	}

	public void addResidentCheck(Resident resident) throws AlreadyRegisteredException {

		if (hasResident(resident))
			throw new AlreadyRegisteredException(Translation.of("msg_err_already_in_town", resident.getName(), getFormattedName()));
		else if (resident.hasTown())
			try {
				if (!resident.getTown().equals(this))
					throw new AlreadyRegisteredException(Translation.of("msg_err_already_in_town", resident.getName(), resident.getTown().getFormattedName()));
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
		double price = (Math.pow(TownySettings.getPurchasedBonusBlocksIncreaseValue() , getPurchasedBlocks()) * TownySettings.getPurchasedBonusBlocksCost());
		double maxprice = TownySettings.getPurchasedBonusBlocksMaxPrice();
		return (maxprice == -1 ? price : Math.min(price, maxprice));
	}
	
	public double getTownBlockCost() {
		double price = Math.round(Math.pow(TownySettings.getClaimPriceIncreaseValue(), getTownBlocks().size()) * TownySettings.getClaimPrice());
		double maxprice = TownySettings.getMaxClaimPrice();
		return (maxprice == -1 ? price : Math.min(price, maxprice));
	}

	public double getTownBlockCostN(int inputN) throws TownyException {
		
		if (inputN < 0)
			throw new TownyException(Translation.of("msg_err_negative"));

		if (inputN == 0)
			return inputN;
		
		double nextprice = getTownBlockCost();
		int i = 1;
		double cost = nextprice;
		boolean hasmaxprice = TownySettings.getMaxClaimPrice() != -1;
		double maxprice = TownySettings.getMaxClaimPrice();
		while (i < inputN){
			nextprice = Math.round(Math.pow(TownySettings.getClaimPriceIncreaseValue() , getTownBlocks().size() + (double)i) * TownySettings.getClaimPrice());
			
			if(hasmaxprice && nextprice > maxprice) {
				cost += maxprice * ( inputN - i);
				break;
			}
			
			cost += nextprice;
			i++;
		}
		cost = Math.round(cost);
		return cost;
	}
	
	public double getBonusBlockCostN(int inputN) throws TownyException {
		
		if (inputN < 0)
			throw new TownyException(Translation.of("msg_err_negative"));

		int current = getPurchasedBlocks();
		int n;
		if (current + inputN > TownySettings.getMaxPurchasedBlocks(this)) {
			n = TownySettings.getMaxPurchasedBlocks(this) - current;
		} else {
			n = inputN;
		}

		if (n == 0)
			return n;
		
		double nextprice = getBonusBlockCost();
		int i = 1;
		double cost = nextprice;
		boolean hasmaxprice = TownySettings.getPurchasedBonusBlocksMaxPrice() != -1;
		double maxprice = TownySettings.getPurchasedBonusBlocksMaxPrice();
		while (i < n){
			nextprice = Math.round(Math.pow(TownySettings.getPurchasedBonusBlocksIncreaseValue() , getPurchasedBlocks()+(double)i) * TownySettings.getPurchasedBonusBlocksCost());			
			
			if (hasmaxprice && nextprice > maxprice) {
				cost += maxprice * (inputN - i);
				break;
			}

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
	 */
	public void setHomeBlock(@Nullable TownBlock homeBlock) {

		this.homeBlock = homeBlock;
		
		if (homeBlock == null)
			return;

		// Set the world if it has changed.
		if (!getHomeblockWorld().getName().equals(homeBlock.getWorld().getName()))
			setWorld(homeBlock.getWorld());

		// Unset the spawn if it is not inside of the new homeblock.
		if (spawn != null && !homeBlock.getWorldCoord().equals(WorldCoord.parseCoord(spawn))) {
			TownyUniverse.getInstance().removeSpawnPoint(spawn);
			spawn = null;
		}

		if (this.hasNation() && TownySettings.getNationRequiresProximity() > 0) {
			Nation nation = TownyAPI.getInstance().getTownNationOrNull(this);
			if (!nation.getCapital().equals(this) && nation.getCapital().hasHomeBlock() && hasHomeBlock()) {
				WorldCoord capitalCoord = null;
				WorldCoord townCoord = null;
				try {
					capitalCoord = nation.getCapital().getHomeBlock().getWorldCoord();
					townCoord = this.getHomeBlock().getWorldCoord();
				} catch (TownyException ignored) {}
				
				if (!nation.getCapital().getHomeblockWorld().equals(getHomeblockWorld())) {
					TownyMessaging.sendNationMessagePrefixed(nation, Translation.of("msg_nation_town_moved_their_homeblock_too_far", getName()));
					removeNation();
				}

				double distance = MathUtil.distance(capitalCoord.getX(), townCoord.getX(), capitalCoord.getZ(), townCoord.getZ());
				if (distance > TownySettings.getNationRequiresProximity()) {
					TownyMessaging.sendNationMessagePrefixed(nation, Translation.of("msg_nation_town_moved_their_homeblock_too_far", getName()));
					removeNation();
				}	
			}
		}
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
			TownyMessaging.sendErrorMsg("town.forceSetHomeblock() is returning null.");
			return;
		}

		this.homeBlock = homeBlock;

		// Set the world as it may have changed
		if (this.world != homeBlock.getWorld())
			setWorld(homeBlock.getWorld());

	}

	public TownBlock getHomeBlock() throws TownyException {

		if (hasHomeBlock())
			return homeBlock;
		else
			throw new TownyException(this.getName() + " has not set a home block.");
	}

	/**
	 * Sets the world this town homeblock belongs to. If it's a world change it will
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

		this.world.addTown(this);
	}

	/**
	 * Fetch the World this town homeblock is registered too.
	 * If the world is null it will poll the TownyWorlds for a townblock owned by the Town.
	 * If it fails to find any claimed blocks it will return the first TownyWorld as a placeholder.
	 * 
	 * @return world
	 */
	public TownyWorld getHomeblockWorld() {

		if (world != null)
			return world;
		if (homeBlock != null)
			return homeBlock.getWorld();
		
		return TownyUniverse.getInstance().getDataSource().getWorlds().get(0);
	}

	public boolean hasMayor() {

		return mayor != null;
	}

	public void removeResident(Resident resident) throws EmptyTownException, NotRegisteredException {

		if (!hasResident(resident)) {
			throw new NotRegisteredException();
		} else {

			remove(resident);
			resident.setJoinedTownAt(0);

			if (getNumResidents() == 0) {
				throw new EmptyTownException(this);
			}
		}
	}

	private void remove(Resident resident) {
		// Mayoral succession.
		if (isMayor(resident)) {
			if (residents.size() > 1) {
				findNewMayor();

				// Town is not removing its last resident so be sure to save it.
				this.save();
			}
		}
		// Remove resident.
		residents.remove(resident);
	}
	
	/** 
	 * Begins search for new mayor.
	 * 
	 */
	public void findNewMayor() {
		for (String rank : TownySettings.getOrderOfMayoralSuccession()) {
			if (findNewMayor(rank)) {
				return;
			}
		}
		// No one has the rank to suceed the mayor, choose a resident.
		findNewMayorCatchAll();
	}

	/**
	 * Tries to find a new mayor from among the town's residents with the rank specified.
	 * 
	 * @param rank - the rank being checked for potential mayors
	 * @return found - whether or not a new mayor was found
	 */
	private boolean findNewMayor(String rank) {
		boolean found = false;
		for (Resident newMayor : getRank(rank)) {
			if ((newMayor != mayor) && (newMayor.hasTownRank(rank))) {  // The latter portion seems redundant.
				setMayor(newMayor);
				found = true;
				break;
			}
		}
		return found;
	}
	
	/**
	 * Tries to find a new mayor from among the town's residents.
	 * 
	 * @return found - whether or not a new mayor was found
	 */
	private boolean findNewMayorCatchAll() {
		boolean found = false;
		for (Resident newMayor : getResidents()) {
			if (newMayor != mayor) {
				setMayor(newMayor);
				found = true;
				break;
			}
		}
		return found;
	}

	@Override
	public void setSpawn(Location spawn) {
		this.spawn = spawn;
		if (spawn != null)
			TownyUniverse.getInstance().addSpawnPoint(new SpawnPoint(spawn, SpawnPointType.TOWN_SPAWN));
	}

	@Override
	public Location getSpawn() throws TownyException {
		if (hasHomeBlock() && spawn != null) {
			return spawn;
		} else {
			this.spawn = null;
			throw new TownyException(Translation.of("msg_err_town_has_not_set_a_spawn_location"));
		}
	}

	public boolean hasHomeBlock() {
		return homeBlock != null;
	}

	public boolean hasWorld() {
		return world != null;
	}

	@Override
	public void removeTownBlock(TownBlock townBlock) {

		if (hasTownBlock(townBlock)) {
			// Remove the spawn point for this outpost.
			if (townBlock.isOutpost()) {
				removeOutpostSpawn(townBlock.getCoord());
			}
			if (townBlock.isJail()) {
				removeJail(townBlock.getJail());
			}
			
			// Clear the towns homeblock if this is it.
			try {
				if (getHomeBlock() == townBlock) {
					setHomeBlock(null);
				}
			} catch (TownyException ignored) {}
			
			
			try {
				if (hasNation() 
					&& getNationOrNull().hasSpawn()
					&& townBlock.getWorldCoord().equals(WorldCoord.parseWorldCoord(getNationOrNull().getSpawn())))
						getNationOrNull().setSpawn(null);
			} catch (TownyException ignored) {}
				
			townBlocks.remove(townBlock.getWorldCoord());
			this.save();
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
	 * Add or update an outpost spawn for a town.
	 * Saves the TownBlock if it is not already an Outpost.
	 * Saves the Town when finished.
	 * 
	 * @param spawn - Location to set an outpost's spawn point
	 */
	public void addOutpostSpawn(Location spawn) {

		// Remove any potential previous outpost spawn at this location (when run via /t set outpost.)
		removeOutpostSpawn(Coord.parseCoord(spawn));

		// Set the TownBlock to be an outpost.
		TownBlock outpost = TownyAPI.getInstance().getTownBlock(spawn);
		if (!outpost.isOutpost()) {
			outpost.setOutpost(true);
			outpost.save();
		}

		// Add to the towns' outpost list.
		outpostSpawns.add(spawn);
		
		// Add a SpawnPoint so a particle effect is displayed.
		TownyUniverse.getInstance().addSpawnPoint(new SpawnPoint(spawn, SpawnPointType.OUTPOST_SPAWN));
		
		// Save the town.
		this.save();
	}
	
	/**
	 * Only to be called from the Loading methods.
	 * 
	 * @param spawn - Location to set Outpost's spawn point
	 */
	public void forceAddOutpostSpawn(Location spawn) {
		outpostSpawns.add(spawn);
		TownyUniverse.getInstance().addSpawnPoint(new SpawnPoint(spawn, SpawnPointType.OUTPOST_SPAWN));
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
			throw new TownyException(Translation.of("msg_err_town_has_no_outpost_spawns_set"));

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
		new ArrayList<>(outpostSpawns).stream()
			.filter(spawn -> Coord.parseCoord(spawn).equals(coord))
			.forEach(spawn -> {
				outpostSpawns.remove(spawn);
				TownyUniverse.getInstance().removeSpawnPoint(spawn);
			});
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

	public boolean isHomeBlock(TownBlock townBlock) {
		return hasHomeBlock() && townBlock == homeBlock;
	}

	public void setPlotTax(double plotTax) {
		this.plotTax = Math.min(plotTax, TownySettings.getMaxPlotTax());
	}

	public double getPlotTax() {
		return plotTax;
	}

	public void setCommercialPlotTax(double commercialTax) {
		this.commercialPlotTax = Math.min(commercialTax, TownySettings.getMaxPlotTax());
	}

	public double getCommercialPlotTax() {
		return commercialPlotTax;
	}

	public void setEmbassyPlotTax(double embassyPlotTax) {
		this.embassyPlotTax = Math.min(embassyPlotTax, TownySettings.getMaxPlotTax());
	}

	public double getEmbassyPlotTax() {
		return embassyPlotTax;
	}

	public void collect(double amount) {
		
		if (TownyEconomyHandler.isActive()) {
			double bankcap = TownySettings.getTownBankCap();
			if (bankcap > 0) {
				if (amount + getAccount().getHoldingBalance() > bankcap) {
					TownyMessaging.sendPrefixedTownMessage(this, Translation.of("msg_err_deposit_capped", bankcap));
					return;
				}
			}
			
			getAccount().deposit(amount, null);
		}

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

	@Override
	public Collection<Resident> getOutlaws() {
		return Collections.unmodifiableList(outlaws);
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
			throw new AlreadyRegisteredException(Translation.of("msg_err_resident_already_an_outlaw"));
		else if (resident.hasTown())
			try {
				if (resident.getTown().equals(this))
					throw new AlreadyRegisteredException(Translation.of("msg_err_not_outlaw_in_your_town"));
			} catch (NotRegisteredException e) {
				e.printStackTrace();
			}
	}
	
	public void removeOutlaw(Resident resident) {

		if (hasOutlaw(resident))
			outlaws.remove(resident);			
	}

	public boolean hasValidUUID() {
		return uuid != null;
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
	public void addMetaData(@NotNull CustomDataField<?> md) {
		this.addMetaData(md, true);
	}

	@Override
	public void removeMetaData(@NotNull CustomDataField<?> md) {
		this.removeMetaData(md, true);
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
	
	public void addJail(Jail jail) {
		if (!hasJails())
			jails = new ArrayList<>(1);
		
		jails.add(jail);
	}
	
	public void removeJail(Jail jail) {
		if (hasJails() && hasJail(jail))
			jails.remove(jail);
		
		if (getPrimaryJail() != null && getPrimaryJail().getUUID().equals(jail.getUUID()))
			setPrimaryJail(null);
	}
	
	public boolean hasJails() {
		return jails != null;
	}
	
	public boolean hasJail(Jail jail) {
		return jails.contains(jail);
	}

	@Nullable
	public Collection<Jail> getJails() {
		if (!hasJails())
			return null;
		return Collections.unmodifiableCollection(jails);
	}
	
	@Nullable
	public Jail getJail(int i) {
		if (!hasJails() || jails.size() < i)
			return null;
		
		return jails.get(--i);
	}
	
	public void setPrimaryJail(Jail jail) {
		primaryJail = jail;
	}
	
	@Nullable
	public Jail getPrimaryJail() {
		if (primaryJail == null && hasJails())
			return getJail(1);
		return primaryJail;
	}
	
	public void renamePlotGroup(String oldName, PlotGroup group) {
		plotGroups.remove(oldName);
		plotGroups.put(group.getName(), group);
	}
	
	public void addPlotGroup(PlotGroup group) {
		if (!hasPlotGroups()) 
			plotGroups = new HashMap<>();
		
		plotGroups.put(group.getName(), group);
		
	}
	
	public void removePlotGroup(PlotGroup plotGroup) {
		if (hasPlotGroups() && plotGroups.remove(plotGroup.getName()) != null) {
			for (TownBlock tb : new ArrayList<>(plotGroup.getTownBlocks())) {
				if (tb.hasPlotObjectGroup() && tb.getPlotObjectGroup().getID().equals(plotGroup.getID())) {
					plotGroup.removeTownBlock(tb);
					tb.removePlotObjectGroup();
					tb.save();
				}
			}
		}
	}

	// Abstract to collection in case we want to change structure in the future
	public Collection<PlotGroup> getPlotGroups() {
		if (plotGroups == null)
			return null;
		
		return Collections.unmodifiableCollection(plotGroups.values());
	}

	// Method is inefficient compared to getting the group from name.
	/**
	 * @deprecated since 0.97.0.11 for being unused.
	 */
	@Deprecated
	public PlotGroup getObjectGroupFromID(UUID ID) {
		if (hasPlotGroups()) {
			for (PlotGroup pg : getPlotGroups()) {
				if (pg.getID().equals(ID)) 
					return pg;
			}
		}
		
		return null;
	}
	
	public boolean hasPlotGroups() {
		return plotGroups != null;
	}

	public boolean hasPlotGroupName(String name) {
		return hasPlotGroups() && plotGroups.containsKey(name);
	}

	@Nullable
	public PlotGroup getPlotObjectGroupFromName(String name) {
		if (hasPlotGroups() && hasPlotGroupName(name))
			return plotGroups.get(name);
		return null;
	}

	@Override
	public double getBankCap() {
		return TownySettings.getTownBankCap();
	}
	
	public World getWorld() {
		return hasWorld() ? BukkitTools.getWorld(getHomeblockWorld().getName()) :
			BukkitTools.getWorlds().get(0);
	}

	@Override
	public String getBankAccountPrefix() {
		return ECONOMY_ACCOUNT_PREFIX;
	}

	@Override
	public String getFormattedName() {
		if (this.isCapital()) {
			return TownySettings.getCapitalPrefix(this) + this.getName().replaceAll("_", " ") + TownySettings.getCapitalPostfix(this);
		}
		
		return TownySettings.getTownPrefix(this) + this.getName().replaceAll("_", " ") + TownySettings.getTownPostfix(this);
	}
	
	public String getPrefix() {
		return TownySettings.getTownPrefix(this);
	}
	
	public String getPostfix() {
		return TownySettings.getTownPostfix(this);
	}

	public double getMaxPercentTaxAmount() {
		return maxPercentTaxAmount;
	}

	public void setMaxPercentTaxAmount(double maxPercentTaxAmount) {
		// Max tax amount cannot go over amount defined in config.
		this.maxPercentTaxAmount = Math.min(maxPercentTaxAmount, TownySettings.getMaxTownTaxPercentAmount());
	}
	
	/**
	 * Whether a town is bankrupted.
	 * 
	 * @return true if bankrupt.
	 */
	public boolean isBankrupt() { 
		return TownyEconomyHandler.isActive() && debtBalance > 0;
	}

	/**
	 * @return the amount of debt held by the Town.
	 */
	public double getDebtBalance() {
		return debtBalance;
	}

	/**
	 * @param balance the amount to set the debtBalance of the town to.
	 */
	public void setDebtBalance(double balance) {
		this.debtBalance = balance;
	}

	public boolean isRuined() {
		if(!ruined && residents.size() == 0) {
			ruined = true;  //If all residents have been deleted, flag town as ruined.
		}
		return ruined;
	}
	
	public void setRuined(boolean b) {
		ruined = b;
	}
	
	public void setRuinedTime(long time) {
		this.ruinedTime = time;
	}
	
	public long getRuinedTime() {
		return ruinedTime;
	}

	@Nullable 
	public String getMapColorHexCode() {
		String rawMapColorHexCode = hasNation() ? nation.getMapColorHexCode() : null;
		TownMapColourCalculationEvent event = new TownMapColourCalculationEvent(this, rawMapColorHexCode);
		Bukkit.getPluginManager().callEvent(event);
		return event.getMapColorHexCode();
	}

	@Override
	public void save() {
		TownyUniverse.getInstance().getDataSource().saveTown(this);
	}
	
	/**
	 * @deprecated As of 0.96.0.0+ please use {@link EconomyAccount#getWorld()} instead.
	 * 
	 * @return The world this resides in.
	 */
	@Deprecated
	public World getBukkitWorld() {
		if (hasWorld()) {
			return BukkitTools.getWorld(getHomeblockWorld().getName());
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
		return StringMgmt.trimMaxLength(Town.ECONOMY_ACCOUNT_PREFIX + getName(), 32);
	}
	
	/**
	 * @deprecated As of 0.96.2.0, please use {@link #getBoard()} instead.
	 * 
	 * @return getBoard()
	 */
	@Deprecated
	public String getTownBoard() {
		return getBoard();
	}

	/**
	 * @return The UUID for this town.
	 * @deprecated as of 0.96.4.0, use {@link #getUUID()} instead.
	 */
	@Deprecated
	public UUID getUuid() {
		return getUUID();
	}

	public long getJoinedNationAt() {
		return joinedNationAt;
	}

	public void setJoinedNationAt(long joinedNationAt) {
		this.joinedNationAt = joinedNationAt;
	}
	
	public long getMovedHomeBlockAt() {
		return movedHomeBlockAt;
	}

	public void setMovedHomeBlockAt(long movedHomeBlockAt) {
		this.movedHomeBlockAt = movedHomeBlockAt;
	}

	private void sortResidents() {
		List<Resident> sortedResidents = residents.stream().sorted(Comparator.comparingLong(Resident::getJoinedTownAt)).collect(Collectors.toList());
		residents.clear();
		residents.addAll(sortedResidents);
		residentsSorted = true;
	}

	public Set<Resident> getTrustedResidents() {
		return trustedResidents;
	}
	
	public boolean hasTrustedResident(Resident resident) {
		return trustedResidents.contains(resident);
	}
	
	public void addTrustedResident(Resident resident) {
		trustedResidents.add(resident);
	}
	
	public void removeTrustedResident(Resident resident) {
		trustedResidents.remove(resident);
	}
}
