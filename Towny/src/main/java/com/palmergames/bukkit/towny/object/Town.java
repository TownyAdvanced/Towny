package com.palmergames.bukkit.towny.object;

import com.google.common.collect.Lists;
import com.palmergames.bukkit.towny.Towny;
import com.palmergames.bukkit.towny.TownyAPI;
import com.palmergames.bukkit.towny.TownyEconomyHandler;
import com.palmergames.bukkit.towny.TownyMessaging;
import com.palmergames.bukkit.towny.TownySettings;
import com.palmergames.bukkit.towny.TownySettings.TownLevel;
import com.palmergames.bukkit.towny.TownyUniverse;
import com.palmergames.bukkit.towny.event.DeleteNationEvent;
import com.palmergames.bukkit.towny.event.NationAddTownEvent;
import com.palmergames.bukkit.towny.event.NationRemoveTownEvent;
import com.palmergames.bukkit.towny.event.BonusBlockPurchaseCostCalculationEvent;
import com.palmergames.bukkit.towny.event.TownBlockClaimCostCalculationEvent;
import com.palmergames.bukkit.towny.event.TownyObjectFormattedNameEvent;
import com.palmergames.bukkit.towny.event.plot.group.PlotGroupDeletedEvent;
import com.palmergames.bukkit.towny.event.town.TownAddAlliedTownEvent;
import com.palmergames.bukkit.towny.event.town.TownAddEnemiedTownEvent;
import com.palmergames.bukkit.towny.event.town.TownCalculateTownLevelNumberEvent;
import com.palmergames.bukkit.towny.event.town.TownConqueredEvent;
import com.palmergames.bukkit.towny.event.town.TownIsTownOverClaimedEvent;
import com.palmergames.bukkit.towny.event.town.TownMapColourLocalCalculationEvent;
import com.palmergames.bukkit.towny.event.town.TownMapColourNationalCalculationEvent;
import com.palmergames.bukkit.towny.event.town.TownMayorChangedEvent;
import com.palmergames.bukkit.towny.event.town.TownMayorChosenBySuccessionEvent;
import com.palmergames.bukkit.towny.event.town.TownRemoveAlliedTownEvent;
import com.palmergames.bukkit.towny.event.town.TownRemoveEnemiedTownEvent;
import com.palmergames.bukkit.towny.event.town.TownUnconquerEvent;
import com.palmergames.bukkit.towny.exceptions.AlreadyRegisteredException;
import com.palmergames.bukkit.towny.exceptions.EmptyNationException;
import com.palmergames.bukkit.towny.exceptions.EmptyTownException;
import com.palmergames.bukkit.towny.exceptions.NotRegisteredException;
import com.palmergames.bukkit.towny.exceptions.ObjectSaveException;
import com.palmergames.bukkit.towny.exceptions.TownyException;
import com.palmergames.bukkit.towny.object.SpawnPoint.SpawnPointType;
import com.palmergames.bukkit.towny.object.jail.Jail;
import com.palmergames.bukkit.towny.object.metadata.CustomDataField;
import com.palmergames.bukkit.towny.object.metadata.MetadataLoader;
import com.palmergames.bukkit.towny.permissions.TownyPerms;
import com.palmergames.bukkit.towny.utils.CombatUtil;
import com.palmergames.bukkit.towny.utils.MapUtil;
import com.palmergames.bukkit.towny.utils.MoneyUtil;
import com.palmergames.bukkit.towny.utils.ProximityUtil;
import com.palmergames.bukkit.towny.utils.TownUtil;
import com.palmergames.bukkit.towny.utils.TownyComponents;
import com.palmergames.bukkit.util.BukkitTools;
import com.palmergames.util.StringMgmt;

import net.kyori.adventure.audience.Audience;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.logging.Level;
import java.util.stream.Collectors;

public class Town extends Government implements TownBlockOwner {

	private static final String ECONOMY_ACCOUNT_PREFIX = TownySettings.getTownAccountPrefix();

	private final List<Resident> residents = new ArrayList<>();
	private final List<Resident> residentsView = Collections.unmodifiableList(residents);
	private final List<Resident> outlaws = new ArrayList<>();
	private Map<UUID, Town> allies = new LinkedHashMap<>();
	private Map<UUID, Town> enemies = new LinkedHashMap<>();
	private final Set<Resident> trustedResidents = new HashSet<>();
	private final Map<UUID, Town> trustedTowns = new LinkedHashMap<>();
	private final List<Position> outpostSpawns = new ArrayList<>();
	private List<Jail> jails = null;
	private HashMap<String, PlotGroup> plotGroups = null;
	private TownBlockTypeCache plotTypeCache = new TownBlockTypeCache();
	private HashMap<String, District> districts = null;
	
	private Resident mayor;
	private String founderName;
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
	private boolean isForSale = false;
	private double forSalePrice = 0;
	private boolean isTaxPercentage = TownySettings.getTownDefaultTaxPercentage();
	private TownBlock homeBlock;
	private TownyWorld world;
	private boolean adminEnabledMobs = false;
	private boolean adminDisabledPVP = false; // This is a special setting to make a town ignore All PVP settings and keep PVP disabled.
	private boolean adminEnabledPVP = false; // This is a special setting to make a town ignore All PVP settings and keep PVP enabled. Overrides the admin disabled too.
	private boolean allowedToWar = TownySettings.getTownDefaultAllowedToWar();
	private boolean isConquered = false;
	private int conqueredDays;
	private int nationZoneOverride = 0;
	private boolean nationZoneEnabled = true;
	private final ConcurrentHashMap<WorldCoord, TownBlock> townBlocks = new ConcurrentHashMap<>();
	private final TownyPermission permissions = new TownyPermission();
	private boolean ruined = false;
	private long ruinedTime;
	private long forSaleTime;
	private long joinedNationAt;
	private long movedHomeBlockAt;
	private Jail primaryJail;
	private int manualTownLevel = -1;
	private boolean visibleOnTopLists = true;

	@ApiStatus.Internal
	public Town(String name, UUID uuid) {
		super(name, uuid);
		permissions.loadDefault(this);
		
		// Set defaults.
		setTaxes(TownySettings.getTownDefaultTax());
		setOpen(TownySettings.getTownDefaultOpen());
		setBoard(TownySettings.getTownDefaultBoard());
		setNeutral(TownySettings.getTownDefaultNeutral());
		setPublic(TownySettings.getTownDefaultPublic());
	}
	
	@Deprecated(since = "0.102.0.4")
	public Town(String name) {
		this(name, UUID.randomUUID());
	}

	@Override
	public boolean equals(Object other) {
		if (other == this)
			return true;
		if (!(other instanceof Town otherTown))
			return false;
		return this.getUUID().equals(otherTown.getUUID());
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
		if (this.taxes < 0 && !TownySettings.isNegativeTownTaxAllowed())
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
		
		setMayor(mayor, false);
	}
	
	/**
	 * Sets a town resident to become mayor.
	 * 
	 * @param mayor - Resident to become mayor.
	 */
	public void setMayor(Resident mayor) {
		setMayor(mayor, true);
	}
	
	public void setMayor(Resident mayor, boolean callEvent) {
		if (!hasResident(mayor))
			return;
		
		final Resident oldMayor = this.mayor;

		if (callEvent)
			BukkitTools.fireEvent(new TownMayorChangedEvent(this.mayor, mayor));

		this.mayor = mayor;

		if (oldMayor != null)
			TownyPerms.assignPermissions(oldMayor, null);
		
		TownyPerms.assignPermissions(mayor, null);
	}

	public String getFounder() {
		return founderName != null ? founderName : getMayor() != null ? getMayor().getName() : "None";
	}

	public void setFounder(String founderName) {
		this.founderName = founderName;
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
			if (TownyUniverse.getInstance().getDataSource().removeNation(oldNation, DeleteNationEvent.Cause.NO_TOWNS))
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
		BukkitTools.fireEvent(new NationRemoveTownEvent(this, oldNation));

		ProximityUtil.removeOutOfRangeTowns(oldNation);
	}
	
	public void setNation(Nation nation) throws AlreadyRegisteredException {
		setNation(nation, true);
	}
	
	public void setNation(Nation nation, boolean updateJoinedAt) throws AlreadyRegisteredException {

		if (this.nation == nation)
			return;

		if (nation == null) {
			this.nation = null;
			if (isConquered()) {
				setConquered(false);
				setConqueredDays(0);
			}
			return;
		}

		if (hasNation())
			throw new AlreadyRegisteredException();

		this.nation = nation;
		nation.addTown(this);

		if (updateJoinedAt)
			setJoinedNationAt(System.currentTimeMillis());

		TownyPerms.updateTownPerms(this);
		BukkitTools.fireEvent(new NationAddTownEvent(this, nation));
	}

	private boolean residentsSorted = false;

	@Override
	public List<Resident> getResidents() {
		if (!residentsSorted)
			sortResidents();
		
		return residentsView;
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

	/**
	 * DO NOT USE THIS. This is visiable for testing only!
	 * Use {@link Resident#setTown(Town)} instead.
	 *
	 * @param resident Resident that gets added to the town.
	 */
	void addResident(Resident resident) {
		residents.add(resident);
	}

	public void addResidentCheck(Resident resident) throws AlreadyRegisteredException {

		if (hasResident(resident))
			throw new AlreadyRegisteredException(Translation.of("msg_err_already_in_town", resident.getName(), getFormattedName()));
		
		final Town residentTown = resident.getTownOrNull();
		if (residentTown != null && !this.equals(residentTown))
			throw new AlreadyRegisteredException(Translation.of("msg_err_already_in_town", resident.getName(), residentTown.getFormattedName()));
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

	public void setAdminEnabledMobs(boolean isMobsForced) {
		this.adminEnabledMobs = isMobsForced;
	}

	public boolean isAdminEnabledMobs() {
		return this.adminEnabledMobs;
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

	public boolean isAllowedToWar() {
		return allowedToWar;
	}

	public void setAllowedToWar(boolean allowedToWar) {
		this.allowedToWar = allowedToWar;
	}

	public void setExplosion(boolean isExplosion) {
		this.permissions.explosion = isExplosion;
	}
	
	public boolean isExplosion() {
		return this.permissions.explosion;
	}

	public void setTaxPercentage(boolean isPercentage) {

		this.isTaxPercentage = isPercentage;
		if (isPercentage && this.getTaxes() > 100) {
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
		BukkitTools.fireEvent(event);
		return event.getPrice();
	}
	
	public double getTownBlockCost() {
		double price = Math.round(Math.pow(TownySettings.getClaimPriceIncreaseValue(), getTownBlocks().size()) * TownySettings.getClaimPrice());
		double maxprice = TownySettings.getMaxClaimPrice();
		TownBlockClaimCostCalculationEvent event = new TownBlockClaimCostCalculationEvent(this, (maxprice == -1 ? price : Math.min(price, maxprice)), 1);
		BukkitTools.fireEvent(event);
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
		BukkitTools.fireEvent(event);
		return event.getPrice();
	}
	
	public double getBonusBlockCostN(int n) throws TownyException {

		if (n < 0)
			throw new TownyException(Translation.of("msg_err_negative"));

		double cost = MoneyUtil.returnPurchasedBlocksCost(getPurchasedBlocks(), n, this);

		BonusBlockPurchaseCostCalculationEvent event = new BonusBlockPurchaseCostCalculationEvent(this, cost, n);
		BukkitTools.fireEvent(event);
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

	public void playerSetsHomeBlock(TownBlock townBlock, Location location, Player player) {
		setHomeBlock(townBlock);
		setSpawn(location);
		setMovedHomeBlockAt(System.currentTimeMillis());
		TownyMessaging.sendMsg(player, Translatable.of("msg_set_town_home", townBlock.getCoord().toString()));
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
		if (spawn != null && !homeBlock.getWorldCoord().equals(spawn.worldCoord())) {
			TownyUniverse.getInstance().removeSpawnPoint(SpawnPointLocation.parsePos(spawn));
			spawn = null;
		}

		if (!hasNation() || TownySettings.getNationProximityToCapital() <= 0 || isCapital())
			return;

		Nation townNation = getNationOrNull();
		if (townNation == null || !townNation.getCapital().hasHomeBlock())
			return;

		List<Town> outOfRangeTowns = ProximityUtil.gatherOutOfRangeTowns(townNation);
		if (outOfRangeTowns.size() > 0) {
			if (outOfRangeTowns.contains(this))
				TownyMessaging.sendNationMessagePrefixed(townNation, Translatable.of("msg_nation_town_moved_their_homeblock_too_far", getName()));

			ProximityUtil.removeOutOfRangeTowns(townNation);
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

	public void removeResident(Resident resident) throws EmptyTownException {

		if (!hasResident(resident))
			return;

		remove(resident);
		resident.setJoinedTownAt(0);

		if (getNumResidents() == 0) {
			throw new EmptyTownException(this);
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
			if (findNewMayor(getRank(rank))) {
				return;
			}
		}
		// No one has the rank to succeed the mayor, choose a resident.
		findNewMayor(getResidents());
	}

	/**
	 * Tries to find a new mayor from ths list of potential residenst.
	 * 
	 * @param potentialResidents the List of Residents that could be mayor.
	 * @return true if a new mayor is selected.
	 */
	private boolean findNewMayor(List<Resident> potentialResidents) {
		for (Resident newMayor : potentialResidents) {
			if (newMayor.equals(mayor))
				continue;

			TownMayorChosenBySuccessionEvent tmcbse = new TownMayorChosenBySuccessionEvent(mayor, newMayor, potentialResidents);
			setMayor(tmcbse.getNewMayor());
			return true;
		}
		return false;
	}

	@Override
	public void setSpawn(@Nullable Location spawn) {
		spawnPosition(spawn == null ? null : Position.ofLocation(spawn));
	}

	@Override
	@NotNull
	public Location getSpawn() throws TownyException {
		if (hasHomeBlock() && spawn != null) {
			return spawn.asLocation();
		} else {
			this.spawn = null;
			throw new TownyException(Translation.of("msg_err_town_has_not_set_a_spawn_location"));
		}
	}

	@Nullable
	@Override
	public Location getSpawnOrNull() {
		if (this.spawn != null)
			return this.spawn.asLocation();
		
		return null;
	}
	
	@Nullable
	@Override
	public Position spawnPosition() {
		return this.spawn;
	}
	
	public void spawnPosition(@Nullable Position spawn) {
		if (this.spawn != null)
			TownyUniverse.getInstance().removeSpawnPoint(SpawnPointLocation.parsePos(this.spawn));
		
		this.spawn = spawn;
		
		if (spawn != null)
			TownyUniverse.getInstance().addSpawnPoint(new SpawnPoint(spawn, SpawnPointType.TOWN_SPAWN));
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
			// Remove the plot group for this town block.
			final PlotGroup plotGroup = townBlock.getPlotObjectGroup();
			if (plotGroup != null) {
				plotGroup.removeTownBlock(townBlock);

				if (!plotGroup.hasTownBlocks()) {
					new PlotGroupDeletedEvent(plotGroup, null, PlotGroupDeletedEvent.Cause.NO_TOWNBLOCKS).callEvent();
					removePlotGroup(plotGroup);

					TownyUniverse.getInstance().getDataSource().removePlotGroup(plotGroup);
				}

				townBlock.removePlotObjectGroup();
			}

			// Remove the spawn point for this outpost.
			if (townBlock.isOutpost() || isAnOutpost(townBlock.getCoord())) {
				removeOutpostSpawn(townBlock.getCoord());
				townBlock.setOutpost(false);
				townBlock.save();
			}
			if (townBlock.isJail() && townBlock.getJail() != null) {
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
	
	public void addOutpostSpawn(Location location) {
		addOutpostSpawn(Position.ofLocation(location));
	}

	/**
	 * Add or update an outpost spawn for a town.
	 * Saves the TownBlock if it is not already an Outpost.
	 * Saves the Town when finished.
	 * 
	 * @param position Position to set an outpost's spawn point
	 */
	public void addOutpostSpawn(Position position) {
		TownBlock townBlock = position.worldCoord().getTownBlockOrNull();
		if (townBlock == null || !this.equals(townBlock.getTownOrNull()))
			return;
		
		// Remove any potential previous outpost spawn at this location (when run via /t set outpost.)
		removeOutpostSpawn(position.worldCoord());

		// Set the TownBlock to be an outpost.
		if (!townBlock.isOutpost()) {
			townBlock.setOutpost(true);
			townBlock.save();
		}

		// Add to the towns' outpost list.
		outpostSpawns.add(position);
		
		// Add a SpawnPoint so a particle effect is displayed.
		TownyUniverse.getInstance().addSpawnPoint(new SpawnPoint(spawn, SpawnPointType.OUTPOST_SPAWN));
		
		// Save the town.
		this.save();
	}
	
	/**
	 * Only to be called from the Loading methods.
	 * 
	 * @param position Location to set Outpost's spawn point
	 */
	@ApiStatus.Internal
	public void forceAddOutpostSpawn(Position position) {
		outpostSpawns.add(position);
		TownyUniverse.getInstance().addSpawnPoint(new SpawnPoint(position, SpawnPointType.OUTPOST_SPAWN));
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

		return outpostSpawns.get(Math.min(getMaxOutpostSpawn() - 1, Math.max(0, index - 1))).asLocation();
	}

	public int getMaxOutpostSpawn() {
		return outpostSpawns.size();
	}

	public boolean hasOutpostSpawn() {
		return !outpostSpawns.isEmpty();
	}

	// Used because (perhaps) some mysql databases do not properly save a townblock's outpost flag.
	private boolean isAnOutpost(Coord coord) {
		return new ArrayList<>(outpostSpawns).stream().anyMatch(spawn -> spawn.worldCoord().equals(coord));
	}

	/**
	 * Get an unmodifiable List of all outpost spawns.
	 * 
	 * @return List of outpostSpawns
	 */
	public List<Location> getAllOutpostSpawns() {
		return Collections.unmodifiableList(Lists.transform(this.outpostSpawns, Position::asLocation));
	}

	/**
	 * @return Similar to {@link #getAllOutpostSpawns()}, but with positions.
	 */
	public Collection<Position> getOutpostSpawns() {
		return Collections.unmodifiableList(this.outpostSpawns);
	}

	public void removeOutpostSpawn(Coord coord) {
		new ArrayList<>(getAllOutpostSpawns()).stream()
			.filter(spawn -> Coord.parseCoord(spawn).equals(coord))
			.forEach(spawn -> {
				removeOutpostSpawn(spawn);
				TownyUniverse.getInstance().removeSpawnPoint(spawn);
			});
	}

	public void removeOutpostSpawn(Location loc) {
		outpostSpawns.remove(Position.ofLocation(loc));
	}

	public List<String> getOutpostNames() {
		List<String> outpostNames = new ArrayList<>();
		int i = 0;
		
		final Iterator<Position> outpostSpawnIterator = this.outpostSpawns.iterator();
		while (outpostSpawnIterator.hasNext()) {
			i++;
			final Position pos = outpostSpawnIterator.next();
			TownBlock tboutpost = TownyAPI.getInstance().getTownBlock(pos.worldCoord());

			if (tboutpost == null) {
				outpostSpawnIterator.remove();
				save();
				continue;
			}

			String name = !tboutpost.hasPlotObjectGroup() ? tboutpost.getName() : tboutpost.getPlotObjectGroup().getName();
			if (!name.isEmpty())
				outpostNames.add(name);
			else
				outpostNames.add(String.valueOf(i));
		}
		return outpostNames;
	}

	/**
	 * Sets the town for sale.
	 *
	 * @param isForSale whether the town is for sale.
	 */
	public final void setForSale(boolean isForSale) {
		this.isForSale = isForSale;
		
		if (!isForSale)
			this.forSalePrice = 0;
	}

	/**
	 * Whether the town is for sale.
	 *
	 * @return true for on sale, false otherwise.
	 */
	public final boolean isForSale() {
		return isForSale;
	}

	/**
	 * Sets town sale price.
	 *
	 * @param forSalePrice double representing sale price.
	 */
	public final void setForSalePrice(double forSalePrice) {
		this.forSalePrice = Math.min(forSalePrice, TownySettings.maxBuyTownPrice());
	}

	/**
	 * Get town sale price.
	 *
	 * @return double representing sale price.
	 */
	public final double getForSalePrice() {
		return forSalePrice;
	}

	public void setForSaleTime(long time) {
		this.forSaleTime = time;
	}
	
	public long getForSaleTime() {
		return forSaleTime;
	}

	public void setPlotPrice(double plotPrice) {
		if (plotPrice < 0)
			plotPrice = -1;
		
		this.plotPrice = Math.min(plotPrice, TownySettings.getMaxPlotPrice());
	}

	public double getPlotPrice() {
		return plotPrice;
	}

	public double getPlotTypePrice(TownBlockType type) {
		double plotPrice = switch (type.getName().toLowerCase(Locale.ROOT)) {
			case "shop" -> getCommercialPlotPrice();
			case "embassy" -> getEmbassyPlotPrice();
			default -> getPlotPrice();
		};
		
		return Math.max(plotPrice, 0);
	}

	public void setCommercialPlotPrice(double commercialPlotPrice) {
		if (commercialPlotPrice < 0)
			commercialPlotPrice = -1;
		
		this.commercialPlotPrice = Math.min(commercialPlotPrice, TownySettings.getMaxPlotPrice());
	}

	public double getCommercialPlotPrice() {

		return commercialPlotPrice;
	}

	public void setEmbassyPlotPrice(double embassyPlotPrice) {
		if (embassyPlotPrice < 0)
			embassyPlotPrice = -1;
		
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
			double bankcap = getBankCap();
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
		
		final Town residentTown = resident.getTownOrNull();
		if (this.equals(residentTown))
			throw new AlreadyRegisteredException(Translation.of("msg_err_not_outlaw_in_your_town"));
	}
	
	public void removeOutlaw(Resident resident) {

		if (hasOutlaw(resident))
			outlaws.remove(resident);
	}
	
	public void loadOutlaws(List<Resident> outlaws) {
		outlaws.stream().forEach(o -> {
			try {
				addOutlaw(o);
			} catch (AlreadyRegisteredException ignored) {}
		});
	}

	public boolean hasValidUUID() {
		return uuid != null;
	}

	public void setOutpostSpawns(List<Location> outpostSpawns) {
		this.outpostSpawns.clear();
		
		for (Location location : outpostSpawns)
			addOutpostSpawn(location);
	}

	public boolean isAlliedWith(Town othertown) {
		return CombatUtil.isAlly(this, othertown);
	}

	public int getOutpostLimit() {
		return TownySettings.getMaxOutposts(this);
	}

	public boolean isOverOutpostLimit() {
		return TownySettings.isOutpostsLimitedByLevels() && (getMaxOutpostSpawn() > getOutpostLimit());

	}
	
	public boolean isOverClaimed() {
		if (hasUnlimitedClaims() || getTownBlocks().size() <= getMaxTownBlocks())
			return false;

		TownIsTownOverClaimedEvent event = new TownIsTownOverClaimedEvent(this);
		return !BukkitTools.isEventCancelled(event);
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
		setConquered(conquered, true);
	}
	
	public void setConquered(boolean conquered, boolean callEvent) {
		if (conquered == this.isConquered)
			return;

		this.isConquered = conquered;

		if (!callEvent)
			return;

		if (TownyPerms.hasConqueredNodes())
			TownyPerms.updateTownPerms(this);

		if (this.isConquered)
			BukkitTools.fireEvent(new TownConqueredEvent(this));
		else
			BukkitTools.fireEvent(new TownUnconquerEvent(this));
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
	
	/**
	 * @return the number of jailed residents in the town.
	 */
	public int getJailedPlayerCount() {
		return getJailedResidents().size();
	}

	/**
	 * @return an unmodifiable List of Residents which are jailed in the town.
	 */
	public List<Resident> getJailedResidents() {
		return Collections.unmodifiableList(new ArrayList<>(TownyUniverse.getInstance().getJailedResidentMap()).stream()
				.filter(res -> res.hasJailTown(getName()))
				.collect(Collectors.toList()));
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
				if (tb.hasPlotObjectGroup() && tb.getPlotObjectGroup().getUUID().equals(plotGroup.getUUID())) {
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

	public void renameDistrict(String oldName, District district) {
		districts.remove(oldName);
		districts.put(district.getName(), district);
	}

	public void addDistrict(District district) {
		if (!hasDistricts())
			districts = new HashMap<>();
		
		districts.put(district.getName(), district);
	}

	public void removeDistrict(District district) {
		if (hasDistricts() && districts.remove(district.getName()) != null) {
			for (TownBlock tb : new ArrayList<>(district.getTownBlocks())) {
				if (tb.hasDistrict() && tb.getDistrict().getUUID().equals(district.getUUID())) {
					district.removeTownBlock(tb);
					tb.removeDistrict();
					tb.save();
				}
			}
		}
	}

	// Abstract to collection in case we want to change structure in the future
	public Collection<District> getDistricts() {
		if (districts == null || districts.isEmpty())
			return Collections.emptyList();
		
		return Collections.unmodifiableCollection(districts.values());
	}

	public boolean hasDistricts() {
		return districts != null;
	}

	public boolean hasDistrictName(String name) {
		return hasDistricts() && districts.containsKey(name);
	}

	@Nullable
	public District getDistrictFromName(String name) {
		if (hasDistricts() && hasDistrictName(name))
			return districts.get(name);
		return null;
	}
	
	@Override
	public double getBankCap() {
		return TownySettings.getTownBankCap(this);
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

		TownyObjectFormattedNameEvent event = new TownyObjectFormattedNameEvent(this, prefix, postfix);
		BukkitTools.fireEvent(event);

		return event.getPrefix() + getName().replace("_", " ") + event.getPostfix();
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
		BukkitTools.fireEvent(event);
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
		BukkitTools.fireEvent(event);
		return event.getMapColorHexCode();
	}

	@Override
	public void save() {
		TownyUniverse.getInstance().getDataSource().saveTown(this);
	}

	@Override
	public Map<String, Object> getObjectDataMap() throws ObjectSaveException {
		try {
			Map<String, Object> twn_hm = new HashMap<>();
			twn_hm.put("name", getName());
			twn_hm.put("outlaws", StringMgmt.join(getOutlaws(), "#"));
			twn_hm.put("mayor", hasMayor() ? getMayor().getUUID() : "");
			twn_hm.put("nation", hasNation() ? getNation().getUUID() : "");
			twn_hm.put("assistants", StringMgmt.join(getRank("assistant"), "#"));
			twn_hm.put("townBoard", getBoard());
			twn_hm.put("founder", getFounder());
			twn_hm.put("tag", getTag());
			twn_hm.put("protectionStatus", getPermissions().toString().replaceAll(",", "#"));
			twn_hm.put("bonus", getBonusBlocks());
			twn_hm.put("manualTownLevel", getManualTownLevel());
			twn_hm.put("purchased", getPurchasedBlocks());
			twn_hm.put("nationZoneOverride", getNationZoneOverride());
			twn_hm.put("nationZoneEnabled", isNationZoneEnabled());
			twn_hm.put("commercialPlotPrice", getCommercialPlotPrice());
			twn_hm.put("commercialPlotTax", getCommercialPlotTax());
			twn_hm.put("embassyPlotPrice", getEmbassyPlotPrice());
			twn_hm.put("embassyPlotTax", getEmbassyPlotTax());
			twn_hm.put("spawnCost", getSpawnCost());
			twn_hm.put("plotPrice", getPlotPrice());
			twn_hm.put("plotTax", getPlotTax());
			twn_hm.put("taxes", getTaxes());
			twn_hm.put("hasUpkeep", hasUpkeep());
			twn_hm.put("hasUnlimitedClaims", hasUnlimitedClaims());
			twn_hm.put("visibleOnTopLists", isVisibleOnTopLists());
			twn_hm.put("taxpercent", isTaxPercentage());
			twn_hm.put("maxPercentTaxAmount", getMaxPercentTaxAmount());
			twn_hm.put("forSale", isForSale());
			twn_hm.put("forSalePrice", getForSalePrice());
			twn_hm.put("forSaleTime", getForSaleTime());
			twn_hm.put("open", isOpen());
			twn_hm.put("public", isPublic());
			twn_hm.put("conquered", isConquered());
			twn_hm.put("conqueredDays", getConqueredDays());
			twn_hm.put("allowedToWar", isAllowedToWar());
			twn_hm.put("hasActiveWar", hasActiveWar());
			twn_hm.put("adminDisabledPvP", isAdminDisabledPVP());
			twn_hm.put("adminEnabledPvP", isAdminEnabledPVP());
			twn_hm.put("adminEnabledMobs", isAdminEnabledMobs());
			twn_hm.put("joinedNationAt", getJoinedNationAt());
			twn_hm.put("mapColorHexCode", getMapColorHexCode());
			twn_hm.put("movedHomeBlockAt", getMovedHomeBlockAt());
			twn_hm.put("metadata", hasMeta() ? serializeMetadata(this) : "");
			twn_hm.put("homeblock", hasHomeBlock() ? getTownBlockForSaving(getHomeBlock()) : "");
			twn_hm.put("spawn", hasSpawn() ? parseLocationForSaving(getSpawn()) : "");
			StringBuilder outpostArray = new StringBuilder();
			if (hasOutpostSpawn())
				for (Location spawn : new ArrayList<>(getAllOutpostSpawns()))
					outpostArray.append(parseLocationForSaving(spawn)).append(";");
			twn_hm.put("outpostSpawns", outpostArray.toString());
			twn_hm.put("registered", getRegistered());
			twn_hm.put("ruined", isRuined());
			twn_hm.put("ruinedTime", getRuinedTime());
			twn_hm.put("neutral", isNeutral());
			twn_hm.put("debtBalance", getDebtBalance());
			if (getPrimaryJail() != null)
				twn_hm.put("primaryJail", getPrimaryJail().getUUID());
			twn_hm.put("trustedResidents", StringMgmt.join(toUUIDList(getTrustedResidents()), "#"));
			twn_hm.put("trustedTowns", StringMgmt.join(getTrustedTownsUUIDS(), "#"));
			twn_hm.put("allies", StringMgmt.join(getAlliesUUIDs(), "#"));
			twn_hm.put("enemies", StringMgmt.join(getEnemiesUUIDs(), "#"));
			return twn_hm;
		} catch (Exception e) {
			throw new ObjectSaveException("An exception occurred when constructing data for town " + getName() + " (" + getUUID() + ").");
		}
	}

	public boolean load(Map<String, String> dataAsMap) {
		String line = "";
		TownyUniverse universe = TownyUniverse.getInstance();
		try {
			line = dataAsMap.get("mayor");
			if (line != null) {
				try {
					Resident res = universe.getResident(UUID.fromString(line));
					if (res == null)
						throw new TownyException();
					forceSetMayor(res);
				} catch (TownyException e1) {
					if (getResidents().isEmpty())
						universe.getDataSource().deleteTown(this);
					else 
						findNewMayor();

					return true;
				}
			}

			setName(dataAsMap.getOrDefault("name", ""));
			setRegistered(getOrDefault(dataAsMap, "registered", 0l));
			setRuined(getOrDefault(dataAsMap, "ruined", false));
			setRuinedTime(getOrDefault(dataAsMap, "ruinedTime", 0l));
			setNeutral(getOrDefault(dataAsMap, "neutral", TownySettings.getTownDefaultNeutral()));
			setOpen(getOrDefault(dataAsMap, "open", TownySettings.getTownDefaultOpen()));
			setPublic(getOrDefault(dataAsMap, "public", TownySettings.getTownDefaultPublic()));
			setConquered(getOrDefault(dataAsMap, "conquered", false));
			setConqueredDays(getOrDefault(dataAsMap, "conqueredDays", 0));
			setAllowedToWar(getOrDefault(dataAsMap, "allowedToWar", TownySettings.getTownDefaultAllowedToWar()));
			setDebtBalance(getOrDefault(dataAsMap, "debtBalance", 0.0));
			setNationZoneOverride(getOrDefault(dataAsMap, "nationZoneOverride", 0));
			setNationZoneEnabled(getOrDefault(dataAsMap, "nationZoneEnabled", false));
			setBoard(dataAsMap.getOrDefault("townBoard", TownySettings.getTownDefaultBoard()));
			setTag(dataAsMap.getOrDefault("tag", ""));
			setBonusBlocks(getOrDefault(dataAsMap, "bonusBlocks", 0));
			setPurchasedBlocks(getOrDefault(dataAsMap, "purchasedBlocks", 0));
			setHasUpkeep(getOrDefault(dataAsMap, "hasUpkeep", true));
			setHasUnlimitedClaims(getOrDefault(dataAsMap, "hasUnlimitedClaims", false));
			setVisibleOnTopLists(getOrDefault(dataAsMap, "visibleOnTopLists", true));
			setTaxes(getOrDefault(dataAsMap, "taxes", TownySettings.getTownDefaultTax()));
			setTaxPercentage(getOrDefault(dataAsMap, "taxpercent", TownySettings.getTownDefaultTaxPercentage()));
			setPlotPrice(getOrDefault(dataAsMap, "plotPrice", 0.0));
			setPlotTax(getOrDefault(dataAsMap, "plotTax", TownySettings.getTownDefaultPlotTax()));
			setCommercialPlotTax(getOrDefault(dataAsMap, "commercialPlotTax", TownySettings.getTownDefaultShopTax()));
			setCommercialPlotPrice(getOrDefault(dataAsMap, "commercialPlotPrice", 0.0));
			setEmbassyPlotTax(getOrDefault(dataAsMap, "embassyPlotTax", TownySettings.getTownDefaultEmbassyTax()));
			setEmbassyPlotPrice(getOrDefault(dataAsMap, "embassyPlotPrice", 0.0));
			setMaxPercentTaxAmount(getOrDefault(dataAsMap, "maxPercentTaxAmount", TownySettings.getMaxTownTaxPercentAmount()));
			setSpawnCost(getOrDefault(dataAsMap, "spawnCost", TownySettings.getSpawnTravelCost()));
			setMapColorHexCode(dataAsMap.getOrDefault("mapColorHexCode", MapUtil.generateRandomTownColourAsHexCode()));
			setAdminDisabledPVP(getOrDefault(dataAsMap, "adminDisabledPvP", false));
			setAdminEnabledPVP(getOrDefault(dataAsMap, "adminEnabledPvP", false));
			setAdminEnabledMobs(getOrDefault(dataAsMap, "adminEnabledMobs", false));
			setAllowedToWar(getOrDefault(dataAsMap, "allowedToWar", TownySettings.getTownDefaultAllowedToWar()));
			setAllowedToWar(getOrDefault(dataAsMap, "hasActiveWar", false));
			setManualTownLevel(getOrDefault(dataAsMap, "manualTownLevel", -1));
			setPermissions(dataAsMap.getOrDefault("protectionStatus", ""));
			setJoinedNationAt(getOrDefault(dataAsMap, "joinedNationAt", 0l));
			setMovedHomeBlockAt(getOrDefault(dataAsMap, "movedHomeBlockAt", 0l));
			setForSale(getOrDefault(dataAsMap, "forSale", false));
			setForSalePrice(getOrDefault(dataAsMap, "forSalePrice", 0.0));
			setForSaleTime(getOrDefault(dataAsMap, "forSaleTime", 0l));
			line = dataAsMap.get("founder");
			if (hasData(line))
				setFounder(line);

			line = dataAsMap.get("homeBlock");
			if (line != null) {
				try {
					setHomeBlock(parseTownBlockFromDB(line));
				} catch (NumberFormatException e) {
					TownyMessaging.sendErrorMsg(Translation.of("flatfile_err_homeblock_load_invalid_location", getName()));
				} catch (NotRegisteredException e) {
					TownyMessaging.sendErrorMsg(Translation.of("flatfile_err_homeblock_load_invalid_townblock", getName()));
				}
			}

			line = dataAsMap.get("spawn");
			if (hasData(line)) {
				Location loc = parseSpawnLocationFromDB(line);
				if (loc != null)
					setSpawn(loc);
			}

			// Load outpost spawns
			line = dataAsMap.get("outpostspawns");
			if (hasData(line)) {
				String[] outposts = line.split(";");
				for (String spawn : outposts) {
					Location loc = parseSpawnLocationFromDB(spawn);
					if (loc != null)
						forceAddOutpostSpawn(Position.ofLocation(loc));
				}
			}

			line = dataAsMap.get("metadata");
			if (hasData(line))
				MetadataLoader.getInstance().deserializeMetadata(this, line.trim());
			
			line = dataAsMap.get("nation");
			if (hasData(line)) {
				Nation nation = universe.getNation(UUID.fromString(line));
				if (nation != null)
					setNation(nation, false);
			}

			line = dataAsMap.get("primaryJail");
			if (hasData(line)) {
				UUID jailUUID = UUID.fromString(line);
				if (universe.hasJail(jailUUID))
					setPrimaryJail(universe.getJail(jailUUID));
			}

			line = dataAsMap.get("trustedResidents");
			if (hasData(line))
				getResidentsFromDB(line).stream().forEach(res -> addTrustedResident(res));
			line = dataAsMap.get("trustedTowns");
			if (hasData(line))
				getTownsFromDB(line).stream().forEach(this::addTrustedTown);

			line = dataAsMap.get("allies");
			if (hasData(line))
				loadAllies(TownyAPI.getInstance().getTowns(toUUIDArray(line.split(getSplitter(line)))));

			line = dataAsMap.get("enemies");
			if (hasData(line))
				loadEnemies(TownyAPI.getInstance().getTowns(toUUIDArray(line.split(getSplitter(line)))));

			line = dataAsMap.get("outlaws");
			if (hasData(line))
				loadOutlaws(getResidentsFromDB(line));

			try {
				universe.registerTown(this);
			} catch (AlreadyRegisteredException ignored) {}
			if (exists())
				save();

		} catch (Exception e) {
			Towny.getPlugin().getLogger().log(Level.WARNING, Translation.of("flatfile_err_reading_town_file_at_line", getName(), line, getUUID().toString()), e);
			return false;
		}
		return true;
	}

	public void saveTownBlocks() {
		townBlocks.values().stream().forEach(tb -> tb.save());
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
		Town residentsTown = resident.getTownOrNull();
		return trustedResidents.contains(resident) || (residentsTown != null && this.hasTrustedTown(residentsTown));
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
		if (BukkitTools.isEventCancelled(taate)) {
			TownyMessaging.sendMsg(taate.getCancelMessage());
			return;
		}
		enemies.remove(town.getUUID());
		allies.put(town.getUUID(), town);
	}

	public void removeAlly(Town town) {
		TownRemoveAlliedTownEvent trate = new TownRemoveAlliedTownEvent(this, town);
		if (BukkitTools.isEventCancelled(trate)) {
			TownyMessaging.sendMsg(trate.getCancelMessage());
			return;
		}
		allies.remove(town.getUUID());
	}

	public boolean removeAllAllies() {
		for (Town ally : getAllies()) {
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
	 * @param towns List&lt;Town&gt; which will be loaded in as trusted towns.
	 */
	public void loadTrustedTowns(List<Town> towns) {
		for (Town trustTown : towns) {
			trustedTowns.put(trustTown.getUUID(), trustTown);
		}
	}

	public void addTrustedTown(Town town) {
		trustedTowns.put(town.getUUID(), town);
	}

	public void removeTrustedTown(Town town) {
		trustedTowns.remove(town.getUUID());
	}

	public boolean removeAllTrustedTowns() {
		for (Town trusted : getTrustedTowns()) {
			removeTrustedTown(trusted);
		}
		return getTrustedTowns().isEmpty();
	}

	public boolean hasTrustedTown(Town town) {
		return trustedTowns.containsKey(town.getUUID());
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
		if (BukkitTools.isEventCancelled(taete)) {
			TownyMessaging.sendMsg(taete.getCancelMessage());
			return;
		}
		allies.remove(town.getUUID());
		enemies.put(town.getUUID(), town);
	}

	public void removeEnemy(Town town) {
		TownRemoveEnemiedTownEvent trete = new TownRemoveEnemiedTownEvent(this, town);
		if (BukkitTools.isEventCancelled(trete)) {
			TownyMessaging.sendMsg(trete.getCancelMessage());
			return;
		}
		enemies.remove(town.getUUID());
	}

	public boolean removeAllEnemies() {
		for (Town enemy : getEnemies()) {
			removeEnemy(enemy);
			enemy.removeEnemy(this);
		}
		return getEnemies().isEmpty();
	}

	public boolean hasEnemy(Town town) {
		return enemies.containsKey(town.getUUID());
	}

	public @Unmodifiable List<Town> getEnemies() {
		return List.copyOf(enemies.values());
	}

	public @Unmodifiable List<Town> getAllies() {
		return List.copyOf(allies.values());
	}

	public @Unmodifiable List<Town> getTrustedTowns() {
		return List.copyOf(trustedTowns.values());
	}
	
	public List<Town> getMutualAllies() {
		List<Town> result = new ArrayList<>();
		for(Town ally: getAllies()) {
			if(ally.hasAlly(this))
				result.add(ally);
		}
		return result;
	}

	public @Unmodifiable List<UUID> getAlliesUUIDs() {
		return List.copyOf(allies.keySet());
	}

	public @Unmodifiable List<UUID> getEnemiesUUIDs() {
		return List.copyOf(enemies.keySet());
	}
	
	public @Unmodifiable List<UUID> getTrustedTownsUUIDS() {
		return List.copyOf(trustedTowns.keySet());
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
	 * Tests whether a position is inside this town's boundaries
	 * @param position The position
	 * @return Whether the position is inside this town.
	 */
	public boolean isInsideTown(@NotNull Position position) {
		return this.equals(position.worldCoord().getTownOrNull());
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

	public TownLevel getTownLevel() {
		return TownySettings.getTownLevel(this);
	}

	/**
	 * Get the Town's current TownLevel number, based on its population.
	 * <p>
	 *     Note that Town Levels are not hard-coded. They can be defined by the server administrator,
	 *     and may be different from the default configuration.
	 *     If you need a Town's level, use {@link Town#getTownLevel()}.
	 *     Due to Town Levels being configurable by administrators, caution is advised when relying on this method.
	 *     See <a href="https://github.com/TownyAdvanced/TownyResources">TownyResources</a>
	 *     or <a href="https://github.com/TownyAdvanced/SiegeWar">SiegeWar</a> for example usages.
	 *     <br />
	 *     e.g.
	 *     ruins = 0
	 * 	   hamlet = 1
	 * 	   village = 2
	 * </p>
	 * @return Current TownLevel number.
	 */
	public int getLevelNumber() {
		int modifier = TownySettings.isTownLevelDeterminedByTownBlockCount() ? getNumTownBlocks() : getNumResidents();
		int townLevelNumber = getManualTownLevel() > -1
				? Math.min(getManualTownLevel(), TownySettings.getTownLevelMax())
				: TownySettings.getTownLevelWhichIsNotManuallySet(modifier, this);

		TownCalculateTownLevelNumberEvent tctle = new TownCalculateTownLevelNumberEvent(this, townLevelNumber);
		BukkitTools.fireEvent(tctle);
		return tctle.getTownLevelNumber();
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
		
		return getTownLevel().townBlockTypeLimits().getOrDefault(type.getName().toLowerCase(Locale.ROOT), -1);
	}
	
	@Override
	public @NotNull Iterable<? extends Audience> audiences() {
		return TownyAPI.getInstance().getOnlinePlayers(this);
	}

	@ApiStatus.Internal
	@Override
	public boolean exists() {
		return TownyUniverse.getInstance().hasTown(getName());
	}

	public boolean isVisibleOnTopLists() {
		return visibleOnTopLists;
	}

	public void setVisibleOnTopLists(boolean visibleOnTopLists) {
		this.visibleOnTopLists = visibleOnTopLists;
	}

	public void playerBroadCastMessageToTown(Player player, String message) {
		TownyMessaging.sendPrefixedTownMessage(this, Translatable.of("town_say_format", player.getName(), TownyComponents.stripClickTags(message)));
	}

	public void checkTownHasEnoughResidentsForNationRequirements() {
		TownUtil.checkNationResidentsRequirementsOfTown(this);
	}

	public boolean hasEnoughResidentsToJoinANation() {
		return TownUtil.townHasEnoughResidentsToJoinANation(this);
	}

	public boolean hasEnoughResidentsToBeANationCapital() {
		return TownUtil.townHasEnoughResidentsToBeANationCapital(this);
	}

	/**
	 * Is this town allowed to have the given number of residents?
	 * 
	 * @param residentCount Number of residents to test with.
	 * @param isCapital     When false, a capital city will be tested as though it
	 *                      were not a non-Capital city.
	 * @return true if the town can support the number of residents based on the
	 *         rules configured on the server.
	 */
	public boolean isAllowedThisAmountOfResidents(int residentCount, boolean isCapital) {
		return TownUtil.townCanHaveThisAmountOfResidents(this, residentCount, isCapital);
	}

	public int getMaxAllowedNumberOfResidentsWithoutNation() {
		return TownUtil.getMaxAllowedNumberOfResidentsWithoutNation(this);
	}
}
