package com.palmergames.bukkit.towny.object;

import com.palmergames.bukkit.towny.Towny;
import com.palmergames.bukkit.towny.TownyAPI;
import com.palmergames.bukkit.towny.TownyEconomyHandler;
import com.palmergames.bukkit.towny.TownyMessaging;
import com.palmergames.bukkit.towny.TownySettings;
import com.palmergames.bukkit.towny.TownyUniverse;
import com.palmergames.bukkit.towny.exceptions.AlreadyRegisteredException;
import com.palmergames.bukkit.towny.exceptions.EmptyNationException;
import com.palmergames.bukkit.towny.exceptions.NotRegisteredException;
import com.palmergames.bukkit.towny.exceptions.TownyException;
import com.palmergames.bukkit.towny.invites.Invite;
import com.palmergames.bukkit.towny.invites.InviteHandler;
import com.palmergames.bukkit.towny.invites.exceptions.TooManyInvitesException;
import com.palmergames.bukkit.towny.object.SpawnPoint.SpawnPointType;
import com.palmergames.bukkit.towny.object.metadata.CustomDataField;
import com.palmergames.bukkit.towny.permissions.TownyPerms;
import com.palmergames.bukkit.util.BukkitTools;
import com.palmergames.util.MathUtil;
import net.kyori.adventure.audience.Audience;
import org.bukkit.Location;
import org.bukkit.World;
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
	private Town capital;
	private final List<Invite> sentAllyInvites = new ArrayList<>();

	public Nation(String name) {
		super(name);
		
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
		return getAllies().isEmpty();
	}

	public boolean hasAlly(Nation nation) {

		return getAllies().contains(nation);
	}

	public boolean hasMutualAlly(Nation nation) {
		
		return getAllies().contains(nation) && nation.getAllies().contains(this);
	}

	/**
	 * Check if the targetNation is an Ally.
	 * @param targetNation Any other nation than this one.
	 * @return Whether the targetNation is an ally.
	 * @deprecated Unused. Marked deprecated as of 0.97.4.1+. Use {@link #isAlliedWith(Nation)}.
	 */
	@Deprecated
	public boolean IsAlliedWith(Nation targetNation) {
		return isAlliedWith(targetNation);
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
		return getEnemies().isEmpty();
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
		
		try {
			if (capital.getNation().equals(this)) {
				setCapital(capital);
				return;
			}
		} catch (NotRegisteredException e) {
		}
		if (!findNewCapital())
			throw new EmptyNationException(this);
	}
	
	public void setCapital(Town capital) {

		TownyMessaging.sendDebugMsg("Nation " + this.getName() + " has set a capital city of " + capital.getName());
		this.capital = capital;
		
		if (this.spawn != null && TownySettings.isNationSpawnOnlyAllowedInCapital() && !capital.isInsideTown(this.spawn))
			this.spawn = capital.getSpawnOrNull();

		try {
			TownyPerms.assignPermissions(capital.getMayor(), null);
		} catch (Exception e) {
			// Dummy catch to prevent errors on startup when setting nation.
		}
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
	public Location getSpawn() throws TownyException {
		if (spawn == null)
			throw new TownyException(Translation.of("msg_err_nation_has_not_set_a_spawn_location"));

		return spawn;
	}
	
	@Override
	@Nullable
	public Location getSpawnOrNull() {
		return spawn;
	}
	

	@Override
	public void setSpawn(Location spawn) {
		if (spawn != null)
			TownyUniverse.getInstance().addSpawnPoint(new SpawnPoint(spawn, SpawnPointType.NATION_SPAWN));
		else if (this.spawn != null)
			TownyUniverse.getInstance().removeSpawnPoint(this.spawn);
		this.spawn = spawn;
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
		this.taxes = Math.min(taxes, TownySettings.getMaxNationTax());
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
			throw new TownyException(Translation.of("msg_err_king_not_in_nation"));
		if (!king.isMayor())
			throw new TownyException(Translation.of("msg_err_new_king_notmayor"));
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
			double bankCap = TownySettings.getNationBankCap();
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
		return TownySettings.getNationPrefix(this) + this.getName().replace("_", " ") + TownySettings.getNationPostfix(this);
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
	
	@Override
	public void save() {
		TownyUniverse.getInstance().getDataSource().saveNation(this);
	}
	
	@Override
	public int getNationZoneSize() {
		if (!TownySettings.getNationZonesEnabled())
			return 0;
		return Integer.parseInt(TownySettings.getNationLevel(this).get(TownySettings.NationLevel.NATIONZONES_SIZE).toString());
	}

	/**
	 * Get the Nation's current Nation Level.
	 * <p>
	 *     Note that Nation Levels are not hard-coded. They can be defined by the server administrator,
	 *     and may be different from the default configuration.	 
	 * </p>
	 * @return Nation Level (int) for current population.
	 */
	public int getLevel() {
		return getLevel(this.getNumResidents());
	}

	/**
	 * Get the maximum level a Nation may achieve.
	 * @return Size of TownySettings' configNationLevel SortedMap.
	 */
	public int getMaxLevel() {
		return TownySettings.getConfigNationLevel().size();
	}

	/**
	 * Get the Nation's Level for a supposed population size.
	 * <p>
	 *     Note that Nation Levels are not hard-coded. They can be defined by the server administrator,
	 *     and may be different from the default configuration.	 
	 * </p>
	 * @param populationSize Number of residents in the Nation, theoretical or real.
	 * @return Nation Level (int) for the supplied populationSize.
	 */
	public int getLevel(int populationSize) {
		for (Integer level : TownySettings.getConfigNationLevel().keySet())
			if (populationSize >= level)
				return level;
		return 0;
	}

	@Override
	public @NotNull Iterable<? extends Audience> audiences() {
		return TownyAPI.getInstance().getOnlinePlayers(this).stream().map(player -> Towny.getAdventure().player(player)).collect(Collectors.toSet());
	}
}
