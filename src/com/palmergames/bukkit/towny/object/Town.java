package com.palmergames.bukkit.towny.object;

import com.palmergames.bukkit.towny.Towny;
import com.palmergames.bukkit.towny.TownyAPI;
import com.palmergames.bukkit.towny.TownyEconomyHandler;
import com.palmergames.bukkit.towny.TownyMessaging;
import com.palmergames.bukkit.towny.TownySettings;
import com.palmergames.bukkit.towny.TownyUniverse;
import com.palmergames.bukkit.towny.event.NationAddTownEvent;
import com.palmergames.bukkit.towny.event.NationRemoveTownEvent;
import com.palmergames.bukkit.towny.event.BonusBlockPurchaseCostCalculationEvent;
import com.palmergames.bukkit.towny.event.TownBlockClaimCostCalculationEvent;
import com.palmergames.bukkit.towny.event.town.TownAddAlliedTownEvent;
import com.palmergames.bukkit.towny.event.town.TownAddEnemiedTownEvent;
import com.palmergames.bukkit.towny.event.town.TownMapColourLocalCalculationEvent;
import com.palmergames.bukkit.towny.event.town.TownMapColourNationalCalculationEvent;
import com.palmergames.bukkit.towny.event.town.TownRemoveAlliedTownEvent;
import com.palmergames.bukkit.towny.event.town.TownRemoveEnemiedTownEvent;
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
import net.kyori.adventure.audience.Audience;
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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;

public class Town extends Government implements TownBlockOwner {

	private static final String ECONOMY_ACCOUNT_PREFIX = TownySettings.getTownAccountPrefix();

	private final List<Resident> residents = new ArrayList<>();
	private final List<Resident> outlaws = new ArrayList<>();
	private Map<UUID, Town> allies = new LinkedHashMap<>();
	private Map<UUID, Town> enemies = new LinkedHashMap<>();
	private final Set<Resident> trustedResidents = new HashSet<>();
	private List<Location> outpostSpawns = new ArrayList<>();
	private List<Jail> jails = null;
	private HashMap<String, PlotGroup> plotGroups = null;
	private TownBlockTypeCache plotTypeCache = new TownBlockTypeCache();
	
	private Resident mayor;
	private int bonusBlocks = 0;
	private int purchasedBlocks = 0;
	private double plotTax= TownySettings.getTownDefaultPlotTax();
	private double commercialPlotTax = TownySettings.getTownDefaultShopTax();
	private double plotPrice = 0.0;
	private double embassyPlotTax = TownySettings.getTownDefaultEmbassyTax();
	private double maxPercentTaxAmount = TownySettings.getMaxTownTaxPercentAmount();
	private double commercialPlotPrice;
	private double embassyPlotPrice;
	private double debtBalance = 0.0;
	private Nation nation;
	private boolean hasUpkeep = true;
	private boolean hasUnlimitedClaims = false;
	private boolean isTaxPercentage = TownySettings.getTownDefaultTaxPercentage();
	private TownBlock homeBlock;
	private TownyWorld world;
	private boolean adminDisabledPVP = false; // This is a special setting to make a town ignore All PVP settings and keep PVP disabled.
	private boolean adminEnabledPVP = false; // This is a special setting to make a town ignore All PVP settings and keep PVP enabled. Overrides the admin disabled too.
	private boolean isConquered = false;
	private int conqueredDays;
	private int nationZoneOverride = 0;
	private boolean nationZoneEnabled = true;
	private final ConcurrentHashMap<WorldCoord, TownBlock> townBlocks = new ConcurrentHashMap<>();
	private final TownyPermission permissions = new TownyPermission();
	private boolean ruined = false;
	private long ruinedTime;
	private long joinedNationAt;
	private long movedHomeBlockAt;
	private Jail primaryJail;
	private int manualTownLevel = -1;

	public Town(String name) {
		super(name);
		permissions.loadDefault(this);
		
		// Set defaults.
		setTaxes(TownySettings.getTownDefaultTax());
		setOpen(TownySettings.getTownDefaultOpen());
		setBoard(TownySettings.getTownDefaultBoard());
		setNeutral(TownySettings.getTownDefaultNeutral());
		setPublic(TownySettings.getTownDefaultPublic());
	}
	
	public Town(String name, UUID uuid) {
		this(name);
		setUUID(uuid);
	}

	@Override
	public Collection<TownBlock> getTownBlocks() {
		return Collections.unmodifiableCollection(townBlocks.values());
	}

	public int getNumTownBlocks() {
		return getTownBlocks().size();
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
			getTownBlockTypeCache().addTownBlockOfType(townBlock.getType());
		}
	}
	
	public TownBlock getTownBlock(WorldCoord worldCoord) {
		if (hasTownBlock(worldCoord))
			return townBlocks.get(worldCoord);
		return null;
	}
	
	public ConcurrentMap<WorldCoord, TownBlock> getTownBlockMap() {
		return townBlocks;
	}

	/**
	 * @return the plotTypeCache
	 */
	public TownBlockTypeCache getTownBlockTypeCache() {
		return plotTypeCache;
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
		
		Nation oldNation = this.nation;
				
		for (Resident res : getResidents()) {
			if (res.hasTitle() || res.hasSurname()) {
				res.setTitle("");
				res.setSurname("");
			}
			res.updatePermsForNationRemoval();
			res.save();
		}

		try {
			oldNation.removeTown(this);
		} catch (EmptyNationException e) {
			TownyUniverse.getInstance().getDataSource().removeNation(oldNation);
			TownyMessaging.sendGlobalMessage(Translatable.of("msg_del_nation", e.getNation().getName()));
		}
		
		try {
			setNation(null);
		} catch (AlreadyRegisteredException ignored) {
			// Cannot occur when setting null
		}
		
		//The town is no longer conquered/occupied because it has left the nation
		this.isConquered = false;
		this.conqueredDays = 0;

		setJoinedNationAt(0);
		
		this.save();
		BukkitTools.getPluginManager().callEvent(new NationRemoveTownEvent(this, oldNation));
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

	/**
	 * @return whether the town hasUnlimitedClaims
	 */
	public boolean hasUnlimitedClaims() {
		return TownySettings.areTownBlocksUnlimited() || hasUnlimitedClaims;
	}

	/**
	 * @param hasUnlimitedClaims set whether the town has unlimited claims or not.
	 */
	public void setHasUnlimitedClaims(boolean hasUnlimitedClaims) {
		this.hasUnlimitedClaims = hasUnlimitedClaims;
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

	public String getMaxTownBlocksAsAString() {
		if (hasUnlimitedClaims())
			return "∞";
		else
			return String.valueOf(getMaxTownBlocks());
	}
	
	public int getMaxTownBlocks() {

		return TownySettings.getMaxTownBlocks(this);
	}

	public int getBonusBlocks() {

		return bonusBlocks;
	}
	
	public double getBonusBlockCost() {
		double price = (Math.pow(TownySettings.getPurchasedBonusBlocksIncreaseValue() , getPurchasedBlocks()) * TownySettings.getPurchasedBonusBlocksCost());
		double maxprice = TownySettings.getPurchasedBonusBlocksMaxPrice();
		BonusBlockPurchaseCostCalculationEvent event = new BonusBlockPurchaseCostCalculationEvent(this, (maxprice == -1 ? price : Math.min(price, maxprice)), 1);
		Bukkit.getPluginManager().callEvent(event);
		return event.getPrice();
	}
	
	public double getTownBlockCost() {
		double price = Math.round(Math.pow(TownySettings.getClaimPriceIncreaseValue(), getTownBlocks().size()) * TownySettings.getClaimPrice());
		double maxprice = TownySettings.getMaxClaimPrice();
		TownBlockClaimCostCalculationEvent event = new TownBlockClaimCostCalculationEvent(this, (maxprice == -1 ? price : Math.min(price, maxprice)), 1);
		Bukkit.getPluginManager().callEvent(event);
		return event.getPrice();
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
		TownBlockClaimCostCalculationEvent event = new TownBlockClaimCostCalculationEvent(this, Math.round(cost), inputN);
		Bukkit.getPluginManager().callEvent(event);
		return event.getPrice();
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
		BonusBlockPurchaseCostCalculationEvent event = new BonusBlockPurchaseCostCalculationEvent(this, Math.round(cost), inputN);
		Bukkit.getPluginManager().callEvent(event);
		return event.getPrice();
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

		// Set the world if it has not been set yet, or if if has changed. 
		if (world == null || !getHomeblockWorld().getName().equals(homeBlock.getWorld().getName()))
			setWorld(homeBlock.getWorld());

		// Unset the spawn if it is not inside of the new homeblock.
		if (spawn != null && !homeBlock.getWorldCoord().equals(Coord.parseCoord(spawn))) {
			TownyUniverse.getInstance().removeSpawnPoint(spawn);
			spawn = null;
		}

		Nation townNation = TownyAPI.getInstance().getTownNationOrNull(this);
		if (this.hasNation() && townNation != null && !townNation.getCapital().equals(this) 
			&& TownySettings.getNationRequiresProximity() > 0
			&& townNation.getCapital().hasHomeBlock() && hasHomeBlock()) {
			
			WorldCoord capitalCoord = townNation.getCapital().getHomeBlockOrNull().getWorldCoord();
			WorldCoord townCoord = this.getHomeBlockOrNull().getWorldCoord();
			
			if (!townNation.getCapital().getHomeblockWorld().equals(getHomeblockWorld())) {
				TownyMessaging.sendNationMessagePrefixed(townNation, Translatable.of("msg_nation_town_moved_their_homeblock_too_far", getName()));
				removeNation();
			}
			
			int x1 = capitalCoord.getX();
			int x2 = townCoord.getX();
			int y1 = capitalCoord.getZ();
			int y2 = townCoord.getZ();
			double  distance = MathUtil.distance(x1, x2, y1, y2);
			
			if (distance > TownySettings.getNationRequiresProximity()) {
				TownyMessaging.sendNationMessagePrefixed(townNation, Translatable.of("msg_nation_town_moved_their_homeblock_too_far", getName()));
				removeNation();
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
	
	@Nullable
	public TownBlock getHomeBlockOrNull() {
		return homeBlock;
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
		
		return TownyUniverse.getInstance().getTownyWorlds().get(0);
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
		if (isMayor(resident) && residents.size() > 1) {
			findNewMayor();

			// Town is not removing its last resident so be sure to save it.
			this.save();
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
		// No one has the rank to succeed the mayor, choose a resident.
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

	@Nullable
	@Override
	public Location getSpawnOrNull() {
		return spawn;
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
			if (townBlock.isOutpost() || isAnOutpost(townBlock.getCoord())) {
				removeOutpostSpawn(townBlock.getCoord());
			}
			if (townBlock.isJail()) {
				removeJail(townBlock.getJail());
			}
			
			// Clear the towns home-block if this is it.
			try {
				if (getHomeBlock() == townBlock) {
					setHomeBlock(null);
				}
			} catch (TownyException ignored) {}
			
			
			
			Nation testNation = getNationOrNull();
			try {
				if (hasNation() && testNation != null && testNation.hasSpawn()
					&& townBlock.getWorldCoord().equals(WorldCoord.parseWorldCoord(testNation.getSpawn())))
					testNation.setSpawn(null);
			} catch (TownyException ignored) {
				// Cannot getSpawn, but that's alright!
			}

			townBlocks.remove(townBlock.getWorldCoord());
			getTownBlockTypeCache().removeTownBlockOfType(townBlock.getType());
			if (townBlock.isForSale())
				getTownBlockTypeCache().removeTownBlockOfTypeForSale(townBlock.getType());
			if (townBlock.hasResident())
				getTownBlockTypeCache().removeTownBlockOfTypeResidentOwned(townBlock.getType());
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
		TownBlock townBlock = TownyAPI.getInstance().getTownBlock(spawn);
		if (townBlock == null || !townBlock.hasTown() || !townBlock.getTownOrNull().equals(this))
			return;
		
		// Remove any potential previous outpost spawn at this location (when run via /t set outpost.)
		removeOutpostSpawn(Coord.parseCoord(spawn));

		// Set the TownBlock to be an outpost.
		if (!townBlock.isOutpost()) {
			townBlock.setOutpost(true);
			townBlock.save();
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
		return (!outpostSpawns.isEmpty());
	}

	// Used because (perhaps) some mysql databases do not properly save a townblock's outpost flag.
	private boolean isAnOutpost(Coord coord) {
		return new ArrayList<>(outpostSpawns).stream().anyMatch(spawn -> Coord.parseCoord(spawn).equals(coord));
	}

	/**
	 * Get an unmodifiable List of all outpost spawns.
	 * 
	 * @return List of outpostSpawns
	 */
	public List<Location> getAllOutpostSpawns() {
		return new ArrayList<>(outpostSpawns);
	}

	public void removeOutpostSpawn(Coord coord) {
		getAllOutpostSpawns().stream()
			.filter(spawn -> Coord.parseCoord(spawn).equals(coord))
			.forEach(spawn -> {
				removeOutpostSpawn(spawn);
				TownyUniverse.getInstance().removeSpawnPoint(spawn);
			});
	}

	public void removeOutpostSpawn(Location loc) {
		outpostSpawns.remove(loc);
	}

	public void setPlotPrice(double plotPrice) {
		this.plotPrice = Math.min(plotPrice, TownySettings.getMaxPlotPrice());
	}

	public double getPlotPrice() {
		return plotPrice;
	}

	public double getPlotTypePrice(TownBlockType type) {
		double plotPrice = switch (type.getName().toLowerCase()) {
			case "shop" -> getCommercialPlotPrice();
			case "embassy" -> getEmbassyPlotPrice();
			default -> getPlotPrice();
		};
		
		return Math.max(plotPrice, 0);
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
			if (bankcap > 0 && amount + getAccount().getHoldingBalance() > bankcap) {
				TownyMessaging.sendPrefixedTownMessage(this, Translatable.of("msg_err_deposit_capped", bankcap));
				return;
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
		List<Resident> assistants = getRank("assistant");
		
		if (!assistants.isEmpty())
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
		return outlaws.stream().anyMatch(outlaw -> outlaw.getName().equalsIgnoreCase(name));
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
		return TownySettings.isOutpostsLimitedByLevels() && (getMaxOutpostSpawn() > getOutpostLimit());

	}
	
	public boolean isOverClaimed() {
		return !hasUnlimitedClaims() && getTownBlocks().size() > getMaxTownBlocks();
	}
	
	/**
	 * Only use this if TownySettings.getTownBlockRatio() is greater than -1.
	 * 
	 * @return the number of TownBlocks a town is able to claim.
	 */
	public int availableTownBlocks() {
		return getMaxTownBlocks() - getTownBlocks().size();
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
		if (plotGroups == null || plotGroups.isEmpty())
			return Collections.emptyList();
		
		return Collections.unmodifiableCollection(plotGroups.values());
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
		String prefix = (this.isCapital() && !TownySettings.getCapitalPrefix(this).isEmpty()) ? TownySettings.getCapitalPrefix(this) : TownySettings.getTownPrefix(this);
		String postfix = (this.isCapital() && !TownySettings.getCapitalPostfix(this).isEmpty()) ? TownySettings.getCapitalPostfix(this) : TownySettings.getTownPostfix(this);
		return prefix + this.getName().replace("_", " ") + postfix;
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

	/**
	 * Used by Dynmap-Towny to get the town's *local* map-colour.
	 * 
	 * @return String value of hex code or null.
	 */
	@Override
	@Nullable
	public String getMapColorHexCode() {
		String rawMapColorHexCode = super.getMapColorHexCode();
		TownMapColourLocalCalculationEvent event = new TownMapColourLocalCalculationEvent(this, rawMapColorHexCode);
		Bukkit.getPluginManager().callEvent(event);
		return event.getMapColorHexCode();
	}

	/**
	 * Used by Dynmap-Towny to get the town's *national* map colour.
	 *
	 * @return String value of hex code or null.
	 */
	@Nullable
	public String getNationMapColorHexCode() {
		String rawMapColorHexCode = hasNation() ? nation.getMapColorHexCode() : null;
		TownMapColourNationalCalculationEvent event = new TownMapColourNationalCalculationEvent(this, rawMapColorHexCode);
		Bukkit.getPluginManager().callEvent(event);
		return event.getMapColorHexCode();
	}

	@Override
	public void save() {
		TownyUniverse.getInstance().getDataSource().saveTown(this);
	}

	public int getNationZoneOverride() {
		return nationZoneOverride;
	}
	
	public void setNationZoneOverride(int size) {
		this.nationZoneOverride = size;
	}
	
	public boolean hasNationZoneOverride() {
		return nationZoneOverride > 0;
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

	@Override
	public int getNationZoneSize() {
		if (!TownySettings.getNationZonesEnabled() || !hasNation())
			return 0;
		
		if (!isCapital() && TownySettings.getNationZonesCapitalsOnly())
			return 0;
		
		if (hasNationZoneOverride())
			return getNationZoneOverride();
		
		return nation.getNationZoneSize() + (isCapital() ? TownySettings.getNationZonesCapitalBonusSize() : 0);
	}

	/**
	 * Only to be used when loading the database.
	 * @param towns List&lt;Town&gt; which will be loaded in as allies.
	 */
	public void loadAllies(List<Town> towns) {
		for (Town town : towns)
			allies.put(town.getUUID(), town);
	}
	
	public void addAlly(Town town) {
		TownAddAlliedTownEvent taate = new TownAddAlliedTownEvent(this, town);
		Bukkit.getPluginManager().callEvent(taate);
		if (taate.isCancelled()) {
			TownyMessaging.sendMsg(taate.getCancelMessage());
			return;
		}
		enemies.remove(town.getUUID());
		allies.put(town.getUUID(), town);
	}

	public void removeAlly(Town town) {
		TownRemoveAlliedTownEvent trate = new TownRemoveAlliedTownEvent(this, town);
		Bukkit.getPluginManager().callEvent(trate);
		if (trate.isCancelled()) {
			TownyMessaging.sendMsg(trate.getCancelMessage());
			return;
		}
		allies.remove(town.getUUID());
	}

	public boolean removeAllAllies() {
		for (Town ally : new ArrayList<>(getAllies())) {
			removeAlly(ally);
			ally.removeAlly(this);
		}
		return getAllies().isEmpty();
	}

	public boolean hasAlly(Town town) {
		return allies.containsKey(town.getUUID());
	}

	public boolean hasMutualAlly(Town town) {
		return hasAlly(town) && town.hasAlly(this);
	}

	/**
	 * Only to be used when loading the database.
	 * @param towns List&lt;Town&gt; which will be loaded in as enemies.
	 */
	public void loadEnemies(List<Town> towns) {
		for (Town town : towns)
			enemies.put(town.getUUID(), town);
	}

	
	public void addEnemy(Town town) {
		TownAddEnemiedTownEvent taete = new TownAddEnemiedTownEvent(this, town);
		Bukkit.getPluginManager().callEvent(taete);
		if (taete.isCancelled()) {
			TownyMessaging.sendMsg(taete.getCancelMessage());
			return;
		}
		allies.remove(town.getUUID());
		enemies.put(town.getUUID(), town);
	}

	public void removeEnemy(Town town) {
		TownRemoveEnemiedTownEvent trete = new TownRemoveEnemiedTownEvent(this, town);
		Bukkit.getPluginManager().callEvent(trete);
		if (trete.isCancelled()) {
			TownyMessaging.sendMsg(trete.getCancelMessage());
			return;
		}
		enemies.remove(town.getUUID());
	}

	public boolean removeAllEnemies() {
		for (Town enemy : new ArrayList<>(getEnemies())) {
			removeEnemy(enemy);
			enemy.removeEnemy(this);
		}
		return getEnemies().isEmpty();
	}

	public boolean hasEnemy(Town town) {
		return enemies.containsKey(town.getUUID());
	}

	public List<Town> getEnemies() {
		return Collections.unmodifiableList(enemies.values().stream().collect(Collectors.toList()));
	}

	public List<Town> getAllies() {
		return Collections.unmodifiableList(allies.values().stream().collect(Collectors.toList()));
	}
	
	public List<Town> getMutualAllies() {
		List<Town> result = new ArrayList<>();
		for(Town ally: getAllies()) {
			if(ally.hasAlly(this))
				result.add(ally);
		}
		return result;
	}

	public List<UUID> getAlliesUUIDs() {
		return Collections.unmodifiableList(allies.keySet().stream().collect(Collectors.toList()));
	}

	public List<UUID> getEnemiesUUIDs() {
		return Collections.unmodifiableList(enemies.keySet().stream().collect(Collectors.toList()));
	}
	
	public boolean isNationZoneEnabled() {
		return nationZoneEnabled;
	}
	
	public void setNationZoneEnabled(boolean nationZoneEnabled) {
		this.nationZoneEnabled = nationZoneEnabled;
	}

	/**
	 * Tests whether a location is inside this town's boundaries
	 * @param location The location
	 * @return Whether the location is inside this town.
	 */
	public boolean isInsideTown(@NotNull Location location) {
		return this.equals(WorldCoord.parseWorldCoord(location).getTownOrNull());
	}
	
	/**
	 * Is the Town Neutral or Peaceful?
	 * 
	 * Tests against a config option that prevents a capital city from being neutral.
	 * 
	 * @since 0.96.5.4
	 * @return true if the object is Neutral or Peaceful.
	 */
	@Override
	public boolean isNeutral() {
		return (!TownySettings.nationCapitalsCantBeNeutral() || !isCapital()) && isNeutral;
	}

	/**
	 * Get the Town's current level, based on its population.
	 * <p>
	 *     Note that Town Levels are not hard-coded. They can be defined by the server administrator,
	 *     and may be different from the default configuration.
	 * </p>
	 * @return Current Town Level.
	 */
	public int getLevel() {
		return getLevel(this.getNumResidents());
	}

	/**
	 * Get the town level for a given population size.
	 * <p>
	 *     Great for debugging, or just to see what the town level is for a given amount of residents. 
	 *     But for most cases you'll want to use {@link Town#getLevel()}, which uses the town's current population.
	 *     <br />
	 *     Note that Town Levels are not hard-coded. They can be defined by the server administrator,
	 *     and may be different from the default configuration.
	 * </p>
	 * @param populationSize Number of residents used to calculate the level.
	 * @return The calculated Town Level. 0, if the town is ruined, or the method otherwise fails through.
	 */
	public int getLevel(int populationSize) {
		if (this.isRuined())
			return 0;

		int key = 0;
		for (int level : TownySettings.getConfigTownLevel().keySet()) {
			key++;
			// Some towns might have their townlevel overridden.
			if (getManualTownLevel() > -1 && key == getMaxLevel() - getManualTownLevel())
				return level;
			// No overridden townlevel, use population instead.
			if (getManualTownLevel() == -1 && populationSize >= level)
				return level;
		}
		return 0;
	}

	/**
	 * Get the maximum level a Town may achieve.
	 * @return Size of TownySettings' configTownLevel SortedMap.
	 */
	public int getMaxLevel() {
		return TownySettings.getConfigTownLevel().size();
	}

	/**
	 * Returns the Town Level ID.
	 * <p>
	 *     Note, this is not the Town Level, but an associated classifier.
	 *     If you need a Town's level, use {@link Town#getLevel()} or {@link Town#getLevel(int)}.
	 *     Due to Town Levels being configurable by administrators, caution is advised when relying on this method.
	 *     See <a href="https://github.com/TownyAdvanced/TownyResources">TownyResources</a>
	 *     or <a href="https://github.com/TownyAdvanced/SiegeWar">SiegeWar</a> for example usages.
	 *     <br />
	 *     e.g.
	 *     ruins = 0
	 * 	   hamlet = 1
	 * 	   village = 2
	 * </p> 
	 *
	 * @return id
	 */
	public int getLevelID() {
		if(this.isRuined())
			return 0;

		int townLevelId = -1;
		for (Integer level : TownySettings.getConfigTownLevel().keySet()) {
			if (level <= this.getNumResidents())
				townLevelId ++;
		}
		return townLevelId;
	}

	/**
	 * @return the manualTownLevel
	 */
	public int getManualTownLevel() {
		return manualTownLevel;
	}

	/**
	 * @param manualTownLevel the manualTownLevel to set
	 */
	public void setManualTownLevel(int manualTownLevel) {
		this.manualTownLevel = manualTownLevel;
	}

	/**
	 * @param type The townblock type to get the limit for.
	 * @return The townblock type limit, or -1 if no limit is configured.
	 */
	public int getTownBlockTypeLimit(TownBlockType type) {
		if (!TownySettings.areLevelTypeLimitsConfigured())
			return -1;
		
		return TownySettings.getTownLevel(this).townBlockTypeLimits().getOrDefault(type.getName().toLowerCase(Locale.ROOT), -1);
	}
	
	@Override
	public @NotNull Iterable<? extends Audience> audiences() {
		return TownyAPI.getInstance().getOnlinePlayers(this).stream().map(player -> Towny.getAdventure().player(player)).collect(Collectors.toSet());
	}
}
