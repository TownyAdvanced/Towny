package com.palmergames.bukkit.towny.object;

import com.palmergames.bukkit.towny.Towny;
import com.palmergames.bukkit.towny.TownyAPI;
import com.palmergames.bukkit.towny.TownyEconomyHandler;
import com.palmergames.bukkit.towny.TownyMessaging;
import com.palmergames.bukkit.towny.TownySettings;
import com.palmergames.bukkit.towny.TownySettings.NationLevel;
import com.palmergames.bukkit.towny.TownyUniverse;
import com.palmergames.bukkit.towny.event.TownyObjectFormattedNameEvent;
import com.palmergames.bukkit.towny.event.nation.NationCalculateNationLevelNumberEvent;
import com.palmergames.bukkit.towny.exceptions.EmptyNationException;
import com.palmergames.bukkit.towny.exceptions.TownyException;
import com.palmergames.bukkit.towny.invites.Invite;
import com.palmergames.bukkit.towny.invites.InviteHandler;
import com.palmergames.bukkit.towny.invites.exceptions.TooManyInvitesException;
import com.palmergames.bukkit.towny.object.SpawnPoint.SpawnPointType;
import com.palmergames.bukkit.towny.object.metadata.CustomDataField;
import com.palmergames.bukkit.towny.permissions.TownyPerms;
import com.palmergames.bukkit.towny.utils.TownyComponents;
import com.palmergames.bukkit.util.BukkitTools;
import com.palmergames.util.MathUtil;
import net.kyori.adventure.audience.Audience;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class Nation extends Government {

	private static final String ECONOMY_ACCOUNT_PREFIX = TownySettings.getNationAccountPrefix();

	
	private final List<Town> towns = new ArrayList<>();
	private List<Nation> allies = new ArrayList<>();
	private List<Nation> enemies = new ArrayList<>();
	private List<Town> taxExempt = new ArrayList<>();
	private Town capital;
	private final List<Invite> sentAllyInvites = new ArrayList<>();
	private boolean isTaxPercentage = TownySettings.getNationDefaultTaxPercentage();
	private double maxPercentTaxAmount = TownySettings.getMaxNationTaxPercentAmount();
	private double conqueredTax = TownySettings.getDefaultNationConqueredTaxAmount();

	public Nation(String name) {
		super(name);
		
		// Set defaults
		setTaxes(TownySettings.getNationDefaultTax());
		setBoard(TownySettings.getNationDefaultBoard());
		setOpen(TownySettings.getNationDefaultOpen());
	}

	public void addTaxExemptTown(Town town) {
		if(this.towns.contains(town)) {
			this.taxExempt.add(town);
		}
	}
	
	public void removeTaxExemptTown(Town town) {
		if(this.towns.contains(town)) {
			this.taxExempt.remove(town);
		}
	}
	
	public void addAlly(Nation nation) {

		if (!hasAlly(nation)) {
			removeEnemy(nation);
			getAllies().add(nation);
		}
	}

	public boolean removeAlly(Nation nation) {

		if (!hasAlly(nation))
			return false;
		else
			return getAllies().remove(nation);
	}

	public boolean removeAllAllies() {

		for (Nation ally : new ArrayList<>(getAllies())) {
			removeAlly(ally);
			ally.removeAlly(this);
		}
		return getAllies().isEmpty();
	}

	public boolean hasAlly(Nation nation) {

		return getAllies().contains(nation);
	}

	public boolean hasMutualAlly(Nation nation) {
		
		return getAllies().contains(nation) && nation.getAllies().contains(this);
	}

	public void addEnemy(Nation nation) {

		if (!hasEnemy(nation)) {
			removeAlly(nation);
			getEnemies().add(nation);
		}

	}

	public boolean removeEnemy(Nation nation) {

		if (!hasEnemy(nation))
			return false;
		else
			return getEnemies().remove(nation);
	}

	public boolean removeAllEnemies() {

		for (Nation enemy : new ArrayList<>(getEnemies())) {
			removeEnemy(enemy);
			enemy.removeEnemy(this);
		}
		return getEnemies().isEmpty();
	}

	public boolean hasEnemy(Nation nation) {

		return getEnemies().contains(nation);
	}

	public boolean hasTaxExempt(Town town) {
		
		return getTaxExempt().contains(town);
	}
	
	public boolean addTaxExempt(Town town) {
		if(hasTaxExempt(town)) 
			return false;
		else 
			return getTaxExempt().add(town);
	}
	
	public boolean removeTaxExempt(Town town) {
		
		if(!hasTaxExempt(town))
			return false;
		else 
			return getTaxExempt().remove(town);
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

		return town == getCapital();
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

	public void addTown(Town town) {
		towns.add(town);
	}

	/**
	 * Only to be called from the loading methods.
	 * 
	 * @param capital - Town to make capital.
	 * @throws EmptyNationException Thrown when no capital can be set.
	 */
	public void forceSetCapital(Town capital) throws EmptyNationException {

		if (towns.isEmpty())
			throw new EmptyNationException(this);

		if (hasTown(capital)) {
			setCapital(capital);
			return;
		}

		if (!findNewCapital())
			throw new EmptyNationException(this);
	}
	
	public void setCapital(Town capital) {

		TownyMessaging.sendDebugMsg("Nation " + this.getName() + " has set a capital city of " + capital.getName());
		this.capital = capital;
		
		if (this.spawn != null && TownySettings.isNationSpawnOnlyAllowedInCapital() && !capital.isInsideTown(this.spawn))
			this.spawn = capital.spawnPosition();

		try {
			TownyPerms.assignPermissions(capital.getMayor(), null);
		} catch (Exception e) {
			// Dummy catch to prevent errors on startup when setting nation.
		}

		// Save the capital city. A town that becomes a capital might have its
		// peaceful/neutral status overridden and require saving.
		this.capital.save();
	}

	public Town getCapital() {
		return capital;
	}
	
	/**
	 * Finds the town in the nation with the most residents and makes it the capital.
	 * 
	 * @return whether it successfully set a capital.
	 */
	public boolean findNewCapital() {
		
		int numResidents = 0;
		Town tempCapital = null;
		for (Town newCapital : getTowns()) {
			if (newCapital.getNumResidents() > numResidents) {
				tempCapital = newCapital;
				numResidents = newCapital.getNumResidents();
			}
		}

		if (tempCapital != null) {
			setCapital(tempCapital);
			return true;
		} else {
			return false;
		}
	}

	@Override
	@NotNull
	public Location getSpawn() throws TownyException {
		if (spawn == null)
			throw new TownyException(Translatable.of("msg_err_nation_has_not_set_a_spawn_location"));

		return this.spawn.asLocation();
	}
	
	@Override
	@Nullable
	public Position spawnPosition() {
		return spawn;
	}

	@Override
	public void setSpawn(@Nullable Location spawn) {
		spawnPosition(spawn == null ? null : Position.ofLocation(spawn));
	}

	@Override
	public void spawnPosition(@Nullable Position spawn) {
		if (this.spawn != null)
			TownyUniverse.getInstance().removeSpawnPoint(SpawnPointLocation.parsePos(this.spawn));

		this.spawn = spawn;

		if (spawn != null)
			TownyUniverse.getInstance().addSpawnPoint(new SpawnPoint(spawn, SpawnPointType.NATION_SPAWN));
	}

	public List<Resident> getAssistants() {

		return this.getResidents().stream().filter(assistant -> assistant.hasNationRank("assistant")).collect(Collectors.toList());
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
	
	public List<Town> getTaxExempt() {

		return taxExempt;
	}
	
	public void setTaxExempt(List<Town> taxExempt) {
		
		this.taxExempt = towns;
	}

	public List<Nation> getMutualAllies() {
		List<Nation> result = new ArrayList<>();
		for(Nation ally: getAllies()) {
			if(ally.hasAlly(this))
				result.add(ally);
		}
		return result;
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

	/**
	 * Should only be called by Town.removeNation();
	 * Removes town from {@link #towns} list and will choose a
	 * new Capital if necessary.
	 * 
	 * @param town - Town to remove from nation.
	 * @throws EmptyNationException - Thrown when last town is being removed.
	 */
	protected void removeTown(Town town) throws EmptyNationException {

		boolean isCapital = town.isCapital();
		remove(town);

		if (getNumTowns() == 0) {
			throw new EmptyNationException(this);
		} else if (isCapital) {
			findNewCapital();
		}
		this.save();
	}

	private void remove(Town town) {

		towns.remove(town);
	}

	private void removeAllTowns() {

		towns.clear();
	}

	public void setTaxes(double taxes) {
		this.taxes = Math.min(taxes, isTaxPercentage ? TownySettings.getMaxNationTaxPercent() : TownySettings.getMaxNationTax());
		
		// Fix invalid taxes
		if (this.taxes < 0 && !TownySettings.isNegativeNationTaxAllowed())
			this.taxes = TownySettings.getNationDefaultTax();
	}

	public double getMaxPercentTaxAmount() {
		return maxPercentTaxAmount;
	}

	public void setMaxPercentTaxAmount(double maxPercentTaxAmount) {
		// Max tax amount cannot go over amount defined in config.
		this.maxPercentTaxAmount = Math.min(maxPercentTaxAmount, TownySettings.getMaxNationTaxPercentAmount());
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

	public void clear() {

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
	 */
	public void removeOutOfRangeTowns() {
		if(capital != null && TownySettings.getNationRequiresProximity() > 0) {
			List<Town> toRemove = gatherOutOfRangeTowns(new ArrayList<>(getTowns()), capital);
			if (!toRemove.isEmpty())
				toRemove.stream().forEach(town -> {
					TownyMessaging.sendPrefixedTownMessage(town, Translatable.of("msg_town_left_nation", this.getName()));
					TownyMessaging.sendPrefixedNationMessage(this, Translatable.of("msg_nation_town_left", town.getName()));
					town.removeNation();
					town.save();
				});
		}
	}
	
	/**
	 * A method which returns a list of Towns too far from the given capital town.
	 * 
	 * @param towns - The list of towns to check.
	 * @param capital - The Town from which to check the distance.
	 * @return removedTowns - A list of Towns which would be removed by removeOutOfRangeTowns().
	 */
	public List<Town> gatherOutOfRangeTowns(List<Town> towns, Town capital) {
		List<Town> removedTowns = new ArrayList<>();
		final TownBlock capitalHomeBlock = capital.getHomeBlockOrNull();
		if (capital.hasHomeBlock() && TownySettings.getNationRequiresProximity() > 0 && capitalHomeBlock != null) {
			final Coord capitalCoord = capitalHomeBlock.getCoord();
			
			for (Town town : towns) {
				TownBlock townHomeBlock = town.getHomeBlockOrNull();
				if (town.hasHomeBlock() && capital.getHomeblockWorld().equals(town.getHomeblockWorld()) && townHomeBlock != null) {
					Coord townCoord = townHomeBlock.getCoord();
					final double distance = MathUtil.distance(capitalCoord.getX(), townCoord.getX(), capitalCoord.getZ(), townCoord.getZ());
					if (distance > TownySettings.getNationRequiresProximity())
						removedTowns.add(town);
				}
			}
		}
		return removedTowns;
	}	

	public void setKing(Resident king) throws TownyException {

		if (!hasResident(king))
			throw new TownyException(Translatable.of("msg_err_king_not_in_nation"));
		if (!king.isMayor())
			throw new TownyException(Translatable.of("msg_err_new_king_notmayor"));
		setCapital(king.getTown());
		this.save();
	}

	public boolean hasResident(Resident resident) {

		for (Town town : getTowns())
			if (town.hasResident(resident))
				return true;
		return false;
	}
	
	public void collect(double amount) {
		
		if (TownyEconomyHandler.isActive()) {
			double bankCap = getBankCap();
			if (bankCap > 0 && amount + this.getAccount().getHoldingBalance() > bankCap) {
				TownyMessaging.sendPrefixedNationMessage(this, Translatable.of("msg_err_deposit_capped", bankCap));
				return;
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
		out.add(String.format("%sNation (%s)", getTreeDepth(depth), getName()));
		out.add(String.format("%sCapital: %s", getTreeDepth(depth + 1), getCapital().getName()));
		
		List<Resident> assistants = getAssistants();
		
		if (!assistants.isEmpty())
			out.add(String.format("%sAssistants (%s): %s", 
				getTreeDepth(depth + 1), assistants.size(), Arrays.toString(assistants.toArray(new Resident[0]))));
		
		if (!getAllies().isEmpty())
			out.add(String.format("%sAllies (%s): %s", 
				getTreeDepth(depth + 1), getAllies().size(), Arrays.toString(getAllies().toArray(new Nation[0]))));
		
		if (!getEnemies().isEmpty())
			out.add(String.format("%sEnemies (%s): %s", 
				getTreeDepth(depth + 1), getEnemies().size(), Arrays.toString(getEnemies().toArray(new Nation[0]))));
		
		out.add(String.format("%sTowns (%s):", getTreeDepth(depth + 1), getTowns().size()));
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

	public boolean hasValidUUID() {
		return uuid != null;
	}
	
	public void newSentAllyInvite(Invite invite) throws TooManyInvitesException {
		if (sentAllyInvites.size() <= InviteHandler.getSentAllyRequestsMaxAmount(this) -1) {
			sentAllyInvites.add(invite);
		} else {
			throw new TooManyInvitesException(Translation.of("msg_err_nation_sent_too_many_requests"));
		}
	}
	
	public void deleteSentAllyInvite(Invite invite) {
		sentAllyInvites.remove(invite);
	}
	
	public List<Invite> getSentAllyInvites() {
		return Collections.unmodifiableList(sentAllyInvites);
	}
	
	public Collection<TownBlock> getTownBlocks() {
		List<TownBlock> townBlocks = new ArrayList<>();
		for (Town town : this.getTowns())
			townBlocks.addAll(town.getTownBlocks());
		
		return Collections.unmodifiableCollection(townBlocks);
	}
	
	public int getNumTownblocks() {
		return getTownBlocks().size();
	}
	
	public Resident getKing() {
		return capital.getMayor();
	}
	
	public boolean hasKing() {
		if (capital == null)
			return false;
		return capital.getMayor() != null;
	}

	@Override
	public String getFormattedName() {
		TownyObjectFormattedNameEvent event = new TownyObjectFormattedNameEvent(this, TownySettings.getNationPrefix(this), TownySettings.getNationPostfix(this));
		BukkitTools.fireEvent(event);
		return event.getPrefix() + getName().replace("_", " ") + event.getPostfix();
	}
	
	@Override
	public void addMetaData(@NotNull CustomDataField<?> md) {
		this.addMetaData(md, true);
	}

	@Override
	public void removeMetaData(@NotNull CustomDataField<?> md) {
		this.removeMetaData(md, true);
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
		return TownySettings.getNationBankCap(this);
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
	
	@Override
	public void save() {
		TownyUniverse.getInstance().getDataSource().saveNation(this);
	}
	
	@Override
	public int getNationZoneSize() {
		if (!TownySettings.getNationZonesEnabled())
			return 0;
		
		return getNationLevel().nationZonesSize();
	}

	/**
	 * Get the Nation's current NationLevel.
	 * <p>
	 *     Note that Nation Levels are not hard-coded. They can be defined by the server administrator,
	 *     and may be different from the default configuration.	 
	 * </p>
	 * @return NationLevel of the nation.
	 */
	public NationLevel getNationLevel() {
		return TownySettings.getNationLevel(this);
	}

	/**
	 * Get the Nation's current Nation Level number, ie: 1 to
	 * {@link TownySettings#getNationLevelMax()}. This is used as a key to determine
	 * which NationLevel a Nation receives, and ultimately which attributes that
	 * Nation will receive.
	 * <p>
	 * Note that Nation Levels are not hard-coded. They can be defined by the server
	 * administrator, and may be different from the default configuration.
	 * </p>
	 * 
	 * @return Nation Level (int) for current population or amount of towns.
	 */
	public int getLevelNumber() {
		int modifier = TownySettings.isNationLevelDeterminedByTownCount() ? getNumTowns() : getNumResidents();
		int nationLevelNumber = TownySettings.getNationLevelFromGivenInt(modifier);
		NationCalculateNationLevelNumberEvent ncnle = new NationCalculateNationLevelNumberEvent(this, nationLevelNumber);
		BukkitTools.fireEvent(ncnle);
		return ncnle.getNationLevelNumber();
	}

	@Override
	public @NotNull Iterable<? extends Audience> audiences() {
		return TownyAPI.getInstance().getOnlinePlayers(this).stream().map(player -> Towny.getAdventure().player(player)).collect(Collectors.toSet());
	}

	public double getConqueredTax() {
		return conqueredTax;
	}

	public void setConqueredTax(double conqueredTax) {
		this.conqueredTax = Math.min(conqueredTax, TownySettings.getMaxNationConqueredTaxAmount());
	}

	@ApiStatus.Internal
	@Override
	public boolean exists() {
		return TownyUniverse.getInstance().hasNation(getName());
	}

	/**
	 * @deprecated since 0.99.6.2, use {@link #getLevelNumber()} instead.
	 * Get the Nation's current Nation Level.
	 * <p>
	 *     Note that Nation Levels are not hard-coded. They can be defined by the server administrator,
	 *     and may be different from the default configuration.	 
	 * </p>
	 * @return Nation Level (int) for current population or amount of towns.
	 */
	@Deprecated
	public int getLevel() {
		return getLevelNumber();
	}

	/**
	 * @deprecated since 0.99.6.2 use {@link TownySettings#getNationLevelMax()} instead.
	 * Get the maximum level a Nation may achieve.
	 * @return Size of TownySettings' configNationLevel SortedMap.
	 */
	@Deprecated
	public int getMaxLevel() {
		return TownySettings.getConfigNationLevel().size();
	}

	/**
	 * @deprecated since 0.99.6.2 use {@link TownySettings#getNationLevelFromGivenInt(int)} instead.
	 * Get the Nation's Level for a supposed population size.
	 * <p>
	 *     Note that Nation Levels are not hard-coded. They can be defined by the server administrator,
	 *     and may be different from the default configuration.	 
	 * </p>
	 * @param populationSize Number of residents in the Nation, theoretical or real.
	 * @return Nation Level (int) for the supplied populationSize.
	 */
	@Deprecated
	public int getLevel(int populationSize) {
		return TownySettings.getNationLevelFromGivenInt(populationSize);
	}

	public void playerBroadCastMessageToNation(Player player, String message) {
		TownyMessaging.sendPrefixedNationMessage(this, Translatable.of("town_say_format", player.getName(), TownyComponents.stripClickTags(message)));
	}
}
