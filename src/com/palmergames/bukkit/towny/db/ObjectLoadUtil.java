package com.palmergames.bukkit.towny.db;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.jetbrains.annotations.Nullable;

import com.palmergames.bukkit.towny.Towny;
import com.palmergames.bukkit.towny.TownyAPI;
import com.palmergames.bukkit.towny.TownyMessaging;
import com.palmergames.bukkit.towny.TownySettings;
import com.palmergames.bukkit.towny.TownyUniverse;
import com.palmergames.bukkit.towny.exceptions.EmptyNationException;
import com.palmergames.bukkit.towny.exceptions.NotRegisteredException;
import com.palmergames.bukkit.towny.exceptions.TownyException;
import com.palmergames.bukkit.towny.object.Nation;
import com.palmergames.bukkit.towny.object.PlotGroup;
import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.towny.object.Town;
import com.palmergames.bukkit.towny.object.TownBlock;
import com.palmergames.bukkit.towny.object.TownyWorld;
import com.palmergames.bukkit.towny.object.Translation;
import com.palmergames.bukkit.towny.object.WorldCoord;
import com.palmergames.bukkit.towny.object.jail.Jail;
import com.palmergames.bukkit.towny.object.metadata.MetadataLoader;
import com.palmergames.bukkit.towny.utils.MapUtil;

public class ObjectLoadUtil {
	
	private final Towny plugin;
	private final TownyDataSource source;
	private final TownyUniverse universe;
	
	public ObjectLoadUtil(Towny plugin, TownyDataSource source, TownyUniverse universe) {
		this.plugin = plugin;
		this.source = source;
		this.universe = universe;
	}
	/*
	 * New Load Object Methods
	 * 
	 * These are called from the FlatFileSource and SQLSource which present Towny
	 * with an object, UUID and the keys which are used to load an object.
	 */
	
	public boolean loadJail(Jail jail, HashMap<String, String> keys) {
		String line = "";
		line = keys.get("townblock");
		if (line != null) {
			try {
				TownBlock tb = parseTownBlockFromDB(line);
				jail.setTownBlock(tb);
				jail.setTown(tb.getTownOrNull());
				tb.setJail(jail);
				tb.getTown().addJail(jail);
			} catch (NumberFormatException | NotRegisteredException e) {
				TownyMessaging.sendErrorMsg("Jail " + jail.getUUID() + " tried to load invalid townblock " + line + " deleting jail.");
				source.removeJail(jail);
				source.deleteJail(jail);
				return true;
			}
		}
		line = keys.get("spawns");
		if (line != null) {
			String[] jails = line.split(";");
			for (String spawn : jails) {
				Location loc = parseSpawnLocationFromDB(spawn);
				if (loc != null)
					jail.addJailCell(loc);
			}
			if (jail.getJailCellLocations().isEmpty()) {
				TownyMessaging.sendErrorMsg("Jail " + jail.getUUID() + " loaded with zero spawns " + line + " deleting jail.");
				source.removeJail(jail);
				source.deleteJail(jail);
				return true;
			}
		}
		return true;
	}
	
	public boolean loadPlotGroup(PlotGroup group, HashMap<String, String> keys) {
		String line = "";
		try {
			line = keys.get("town");
			if (line != null && !line.isEmpty()) {
				Town town = getTownFromDB(line);
				if (town != null) {
					group.setTown(town);
					group.setName(keys.getOrDefault("groupName", ""));
					group.setPrice(getOrDefault(keys, "groupPrice", -1.0));
				} else {
					TownyMessaging.sendDebugMsg(Translation.of("flatfile_dbg_group_file_missing_town_delete", group.getUUID()));
					source.deletePlotGroup(group); 
					TownyMessaging.sendDebugMsg(Translation.of("flatfile_dbg_missing_file_delete_group_entry", group.getUUID()));
					return true;
				}
			} else {
				TownyMessaging.sendErrorMsg(Translation.of("flatfile_err_could_not_add_to_town"));
				source.deletePlotGroup(group);
			}

			return true;
		} catch (Exception e) {
			TownyMessaging.sendErrorMsg(Translation.of("flatfile_err_exception_reading_group_file_at_line", group.getUUID(), line));
			return false;
		}
	}

	public boolean loadResident(Resident resident, HashMap<String, String> keys) {
		try {
			String line = "";
			// Name
			resident.setName(keys.getOrDefault("name", generateMissingName()));
			// Registered Date
			resident.setRegistered(getOrDefault(keys, "registered", 0l));
			// Last Online Date
			resident.setLastOnline(getOrDefault(keys, "lastOnline", 0l));
			// isNPC
			resident.setNPC(getOrDefault(keys, "isNPC", false));
			// jail
			line = keys.get("jail");
			if (line != null && universe.hasJail(UUID.fromString(line)))
				resident.setJail(universe.getJail(UUID.fromString(line)));
			if (resident.isJailed()) {
				line = keys.get("jailCell");
				if (line != null)
					resident.setJailCell(Integer.parseInt(line));

				line = keys.get("jailHours");
				if (line != null)
					resident.setJailHours(Integer.parseInt(line));
			}
			line = keys.get("friends");
			if (line != null)
				resident.loadFriends(getResidentsFromDB(line));

			resident.setPermissions(keys.getOrDefault("protectionStatus", ""));

			line = keys.get("metadata");
			if (line != null && !line.isEmpty())
				MetadataLoader.getInstance().deserializeMetadata(resident, line.trim());

			line = keys.get("town");
			if (line != null) {
				Town town = getTownFromDB(line);
				if (town == null)
					TownyMessaging.sendErrorMsg(Translation.of("flatfile_err_resident_tried_load_invalid_town", resident.getName(), line));

				if (town != null) {
					resident.setTown(town, false);

					line = keys.get("title");
					if (line != null)
						resident.setTitle(line);

					line = keys.get("surname");
					if (line != null)
						resident.setSurname(line);

					try {
						line = keys.get("town-ranks");
						if (line != null)
							resident.setTownRanks(Arrays.asList(line.split(getSplitter(line))));
					} catch (Exception e) {}
	
					try {
						line = keys.get("nation-ranks");
						if (line != null)
							resident.setNationRanks(Arrays.asList(line.split(getSplitter(line))));
					} catch (Exception e) {}
	
					line = keys.get("joinedTownAt");
					if (line != null) {
						resident.setJoinedTownAt(Long.valueOf(line));
					}
				}
			}
			
			return true;
		} catch (Exception e) {
			TownyMessaging.sendErrorMsg(e.getMessage());
			return false;
		} finally {
			source.saveResident(resident);
		}
	}
	
	public boolean loadTown(Town town, HashMap<String, String> keys) {
		String line = "";
		try {
			line = keys.get("mayor");
			if (line != null)
				try {
					Resident res = getResidentFromDB(line);
					if (res == null)
						throw new TownyException();
					town.forceSetMayor(res);
				} catch (TownyException e1) {
					if (town.getResidents().isEmpty())
						source.deleteTown(town);
					else 
						town.findNewMayor();

					return true;
				}

			town.setName(keys.getOrDefault("name", generateMissingName()));
			town.setRegistered(getOrDefault(keys, "registered", 0l));
			town.setRuined(getOrDefault(keys, "ruined", false));
			town.setRuinedTime(getOrDefault(keys, "ruinedTime", 0l));
			town.setNeutral(getOrDefault(keys, "neutral", TownySettings.getTownDefaultNeutral()));
			town.setOpen(getOrDefault(keys, "open", TownySettings.getTownDefaultOpen()));
			town.setPublic(getOrDefault(keys, "public", TownySettings.getTownDefaultPublic()));
			town.setConquered(getOrDefault(keys, "conquered", false));
			town.setConqueredDays(getOrDefault(keys, "conqueredDays", 0));
			town.setDebtBalance(getOrDefault(keys, "debtBalance", 0.0));
			town.setNationZoneOverride(getOrDefault(keys, "nationZoneOverride", 0));
			town.setNationZoneEnabled(getOrDefault(keys, "nationZoneEnabled", false));
			town.setBoard(keys.getOrDefault("townBoard", TownySettings.getTownDefaultBoard()));
			town.setTag(keys.getOrDefault("tag", ""));
			town.setBonusBlocks(getOrDefault(keys, "bonusBlocks", 0));
			town.setPurchasedBlocks(getOrDefault(keys, "purchasedBlocks", 0));
			town.setHasUpkeep(getOrDefault(keys, "hasUpkeep", true));
			town.setHasUnlimitedClaims(getOrDefault(keys, "hasUnlimitedClaims", false));
			town.setTaxes(getOrDefault(keys, "taxes", TownySettings.getTownDefaultTax()));
			town.setTaxPercentage(getOrDefault(keys, "taxpercent", TownySettings.getTownDefaultTaxPercentage()));
			town.setPlotPrice(getOrDefault(keys, "plotPrice", 0.0));
			town.setPlotTax(getOrDefault(keys, "plotTax", TownySettings.getTownDefaultPlotTax()));
			town.setCommercialPlotTax(getOrDefault(keys, "commercialPlotTax", TownySettings.getTownDefaultShopTax()));
			town.setCommercialPlotPrice(getOrDefault(keys, "commercialPlotPrice", 0.0));
			town.setEmbassyPlotTax(getOrDefault(keys, "embassyPlotTax", TownySettings.getTownDefaultEmbassyTax()));
			town.setEmbassyPlotPrice(getOrDefault(keys, "embassyPlotPrice", 0.0));
			town.setMaxPercentTaxAmount(getOrDefault(keys, "maxPercentTaxAmount", TownySettings.getMaxTownTaxPercentAmount()));
			town.setSpawnCost(getOrDefault(keys, "spawnCost", TownySettings.getSpawnTravelCost()));
			town.setMapColorHexCode(keys.getOrDefault("mapColorHexCode", MapUtil.generateRandomTownColourAsHexCode()));
			town.setAdminDisabledPVP(getOrDefault(keys, "adminDisabledPvP", false));
			town.setAdminEnabledPVP(getOrDefault(keys, "adminEnabledPvP", false));
			town.setManualTownLevel(getOrDefault(keys, "manualTownLevel", -1));
			town.setPermissions(keys.getOrDefault("protectionStatus", ""));
			town.setJoinedNationAt(getOrDefault(keys, "joinedNationAt", 0l));
			town.setMovedHomeBlockAt(getOrDefault(keys, "movedHomeBlockAt", 0l));
			line = keys.get("homeBlock");
			if (line != null) {
				try {
					town.setHomeBlock(parseTownBlockFromDB(line));
				} catch (NumberFormatException e) {
					TownyMessaging.sendErrorMsg(Translation.of("flatfile_err_homeblock_load_invalid_location", town.getName()));
				} catch (NotRegisteredException e) {
					TownyMessaging.sendErrorMsg(Translation.of("flatfile_err_homeblock_load_invalid_townblock", town.getName()));
				}
			}

			line = keys.get("spawn");
			if (line != null) {
				Location loc = parseSpawnLocationFromDB(line);
				if (loc != null)
					town.setSpawn(loc);
			}

			// Load outpost spawns
			line = keys.get("outpostspawns");
			if (line != null) {
				String[] outposts = line.split(";");
				for (String spawn : outposts) {
					Location loc = parseSpawnLocationFromDB(spawn);
					if (loc != null)
						town.forceAddOutpostSpawn(loc);
				}
			}

			line = keys.get("metadata");
			if (line != null && !line.isEmpty())
				MetadataLoader.getInstance().deserializeMetadata(town, line.trim());
			
			line = keys.get("nation");
			if (line != null && !line.isEmpty()) {
				Nation nation = getNationFromDB(line);
				if (nation != null)
					town.setNation(nation, false);
			}

			line = keys.get("primaryJail");
			if (line != null) {
				UUID jailUUID = UUID.fromString(line);
				if (universe.hasJail(jailUUID))
					town.setPrimaryJail(universe.getJail(jailUUID));
			}

			line = keys.get("trustedResidents");
			if (line != null && !line.isEmpty())
				getResidentsFromDB(line).stream().forEach(res -> town.addTrustedResident(res));

			line = keys.get("allies");
			if (line != null && !line.isEmpty())
				town.loadAllies(TownyAPI.getInstance().getTowns(toUUIDArray(line.split(getSplitter(line)))));

			line = keys.get("enemies");
			if (line != null && !line.isEmpty())
				town.loadEnemies(TownyAPI.getInstance().getTowns(toUUIDArray(line.split(getSplitter(line)))));

			line = keys.get("outlaws");
			if (line != null && !line.isEmpty())
				town.loadOutlaws(getResidentsFromDB(line));

		} catch (Exception e) {
			TownyMessaging.sendErrorMsg(Translation.of("flatfile_err_reading_town_file_at_line", town.getName(), line, town.getUUID().toString()));
			e.printStackTrace();
			return false;
		} finally {
			source.saveTown(town);
		}
		return true;
	}

	public boolean loadNation(Nation nation, HashMap<String, String> keys) {
		String line = "";
		try {
			line = keys.get("capital");
			String cantLoadCapital = Translation.of("flatfile_err_nation_could_not_load_capital_disband", nation.getName());
			if (line != null) {
				Town town = getTownFromDB(line);
				if (town != null) {
					try {
						nation.forceSetCapital(town);
					} catch (EmptyNationException e1) {
						plugin.getLogger().warning(cantLoadCapital);
						source.removeNation(nation);
						return true;
					}
				}
				else {
					TownyMessaging.sendDebugMsg(Translation.of("flatfile_dbg_cannot_set_capital_try_next", nation.getName(), line));
					if (!nation.findNewCapital()) {
						plugin.getLogger().warning(cantLoadCapital);
						source.removeNation(nation);
						return true;
					}
				}
			} else {
				TownyMessaging.sendDebugMsg(Translation.of("flatfile_dbg_undefined_capital_select_new", nation.getName()));
				if (!nation.findNewCapital()) {
					plugin.getLogger().warning(cantLoadCapital);
					source.removeNation(nation);
					return true;
				}
			}

			nation.setName(keys.getOrDefault("name", generateMissingName()));
			nation.setTaxes(getOrDefault(keys, "taxes", 0.0));
			nation.setSpawnCost(getOrDefault(keys, "spawnCost", TownySettings.getSpawnTravelCost()));
			nation.setNeutral(getOrDefault(keys, "neutral", false));
			nation.setRegistered(getOrDefault(keys, "registered", 0l));
			nation.setPublic(getOrDefault(keys, "isPublic", false));
			nation.setOpen(getOrDefault(keys, "isOpen", TownySettings.getNationDefaultOpen()));
			nation.setBoard(keys.getOrDefault("nationBoard", TownySettings.getNationDefaultBoard()));
			nation.setMapColorHexCode(keys.getOrDefault("mapColorHexCode", MapUtil.generateRandomNationColourAsHexCode()));
			nation.setTag(keys.getOrDefault("tag", ""));

			line = keys.get("allies");
			if (line != null && !line.isEmpty())
				nation.loadAllies(getNationsFromDB(line));

			line = keys.get("enemies");
			if (line != null && !line.isEmpty())
				nation.loadEnemies(getNationsFromDB(line));

			line = keys.get("nationSpawn");
			if (line != null) {
				Location loc = parseSpawnLocationFromDB(line);
				if (loc != null)
					nation.setSpawn(loc);
			}

			line = keys.get("metadata");
			if (line != null && !line.isEmpty())
				MetadataLoader.getInstance().deserializeMetadata(nation, line.trim());

		} catch (Exception e) {
			TownyMessaging.sendErrorMsg(Translation.of("flatfile_err_reading_nation_file_at_line", nation.getName(), line, nation.getUUID().toString()));
			e.printStackTrace();
			return false;
		} finally {
			source.saveNation(nation);
		}
		return true;
	}
	
	public boolean loadWorld(TownyWorld world, HashMap<String, String> keys) {
		String line = "";
		try {
			world.setName(keys.getOrDefault("name", generateMissingName()));
			world.setClaimable(getOrDefault(keys,"claimable", true));
			world.setUsingTowny(getOrDefault(keys, "usingTowny", TownySettings.isUsingTowny()));
			world.setWarAllowed(getOrDefault(keys, "warAllowed", TownySettings.isWarAllowed()));
			world.setPVP(getOrDefault(keys, "pvp", TownySettings.isPvP()));
			world.setForcePVP(getOrDefault(keys, "forcepvp", TownySettings.isForcingPvP()));
			world.setFriendlyFire(getOrDefault(keys, "friendlyFire", TownySettings.isFriendlyFireEnabled()));
			world.setForceTownMobs(getOrDefault(keys, "forcetownmobs", TownySettings.isForcingMonsters()));
			world.setWildernessMobs(getOrDefault(keys, "wildernessmobs", TownySettings.isWildernessMonstersOn()));
			world.setWorldMobs(getOrDefault(keys, "worldmobs", TownySettings.isWorldMonstersOn()));
			world.setFire(getOrDefault(keys, "firespread", TownySettings.isFire()));
			world.setForceFire(getOrDefault(keys, "forcefirespread", TownySettings.isForcingFire()));
			world.setExpl(getOrDefault(keys, "explosions", TownySettings.isExplosions()));
			world.setForceExpl(getOrDefault(keys, "forceexplosions", TownySettings.isForcingExplosions()));
			world.setEndermanProtect(getOrDefault(keys, "endermanprotect", TownySettings.getEndermanProtect()));
			world.setDisableCreatureTrample(getOrDefault(keys, "disablecreaturetrample", TownySettings.isCreatureTramplingCropsDisabled()));
			world.setUnclaimedZoneBuild(getOrDefault(keys, "unclaimedZoneBuild", TownySettings.getUnclaimedZoneBuildRights()));
			world.setUnclaimedZoneDestroy(getOrDefault(keys, "unclaimedZoneDestroy", TownySettings.getUnclaimedZoneDestroyRights()));
			world.setUnclaimedZoneSwitch(getOrDefault(keys, "unclaimedZoneSwitch", TownySettings.getUnclaimedZoneSwitchRights()));
			world.setUnclaimedZoneItemUse(getOrDefault(keys, "unclaimedZoneItemUse", TownySettings.getUnclaimedZoneItemUseRights()));
			world.setUnclaimedZoneName(keys.getOrDefault("unclaimedZoneName", TownySettings.getUnclaimedZoneName()));
			world.setUnclaimedZoneIgnore(toList(keys.get("unclaimedZoneIgnoreIds")));
			world.setPlotManagementDeleteIds(toList(keys.get("plotManagementDeleteIds")));
			world.setUsingPlotManagementDelete(getOrDefault(keys, "usingPlotManagementDelete", TownySettings.isUsingPlotManagementDelete()));
			world.setDeletingEntitiesOnUnclaim(getOrDefault(keys, "isDeletingEntitiesOnUnclaim", TownySettings.isDeletingEntitiesOnUnclaim()));
			world.setUnclaimDeleteEntityTypes(toList(keys.get("unclaimDeleteEntityTypes")));
			world.setPlotManagementMayorDelete(toList(keys.get("plotManagementMayorDelete")));
			world.setUsingPlotManagementMayorDelete(getOrDefault(keys, "usingPlotManagementMayorDelete", TownySettings.isUsingPlotManagementMayorDelete()));
			world.setPlotManagementIgnoreIds(toList(keys.get("plotManagementIgnoreIds")));
			world.setUsingPlotManagementRevert(getOrDefault(keys, "usingPlotManagementRevert", TownySettings.isUsingPlotManagementRevert()));
			world.setPlotManagementWildRevertEntities(toList(keys.get("PlotManagementWildRegenEntities")));
			world.setUsingPlotManagementWildEntityRevert(getOrDefault(keys, "usingPlotManagementWildRegen", TownySettings.isUsingPlotManagementWildEntityRegen()));
			world.setPlotManagementWildRevertBlockWhitelist(toList(keys.get("PlotManagementWildRegenBlockWhitelist")));
			world.setPlotManagementWildRevertMaterials(toList(keys.get("PlotManagementWildRegenBlocks")));
			world.setUsingPlotManagementWildBlockRevert(getOrDefault(keys, "usingPlotManagementWildRegenBlocks", TownySettings.isUsingPlotManagementWildBlockRegen()));
			world.setPlotManagementWildRevertDelay(getOrDefault(keys, "usingPlotManagementWildRegenDelay", TownySettings.getPlotManagementWildRegenDelay()));
			line = keys.get("metadata");
			if (line != null && !line.isEmpty())
				MetadataLoader.getInstance().deserializeMetadata(world, line.trim());

		} catch (Exception e) {
			TownyMessaging.sendErrorMsg(Translation.of("flatfile_err_exception_reading_world_file_at_line", world.getName(), line, world.getUUID().toString()));
			return false;
		} finally {
			source.saveWorld(world);
		}
		return true;
	}
	
	/*
	 * Private methods used to read a key and set a default value from the config if it isn't present.
	 */

	private boolean getOrDefault(HashMap<String, String> keys, String key, boolean bool) {
		return Boolean.parseBoolean(keys.getOrDefault(key, String.valueOf(bool)));
	}
	
	private long getOrDefault(HashMap<String, String> keys, String key, long num) {
		return Long.parseLong(keys.getOrDefault(key, String.valueOf(num)));
	}

	private double getOrDefault(HashMap<String, String> keys, String key, double num) {
		return Double.parseDouble(keys.getOrDefault(key, String.valueOf(num)));
	}

	private int getOrDefault(HashMap<String, String> keys, String key, int num) {
		return Integer.parseInt(keys.getOrDefault(key, String.valueOf(num)));
	}

	private List<String> toList(String string) {
		List<String> mats = new ArrayList<>();
		if (string != null)
			try {
				for (String s : string.split(getSplitter(string)))
					if (!s.isEmpty())
						mats.add(s);
			} catch (Exception ignored) {
			}
		return mats;
	}

	@Nullable
	private Resident getResidentFromDB(String line) {
		Resident resident = null;
		try {
			resident = universe.getResident(UUID.fromString(line.trim()));
		} catch (IllegalArgumentException e) { // Legacy DB used Names instead of UUIDs.
			resident = universe.getResident(line.trim());
		}
		return resident;
	}

	@Nullable
	private List<Resident> getResidentsFromDB(String line) {
		List<Resident> residents = new ArrayList<>();
		try {
			residents = TownyAPI.getInstance().getResidents(toUUIDArray(line.split("#")));
		} catch (IllegalArgumentException e) { // Legacy DB used Names instead of UUIDs.
			residents = TownyAPI.getInstance().getResidents(line.split(","));
		}
		return residents;
	}

	@Nullable
	private Town getTownFromDB(String line) {
		Town town = null;
		try {
			town = universe.getTown(UUID.fromString(line.trim()));
		} catch (IllegalArgumentException e) { // Legacy DB used Names instead of UUIDs.
			town = universe.getTown(line.trim());
		}
		return town;
	}

	@Nullable
	private Nation getNationFromDB(String line) {
		Nation nation = null;
		try {
			nation = universe.getNation(UUID.fromString(line.trim()));
		} catch (IllegalArgumentException e) { // Legacy DB used Names instead of UUIDs.
			nation = universe.getNation(line.trim());
		}
		return nation;
	}

	@Nullable
	private List<Nation> getNationsFromDB(String line) {
		List<Nation> nations = new ArrayList<>();
		try {
			nations = TownyAPI.getInstance().getNations(toUUIDArray(line.split("#"))); 
		} catch (IllegalArgumentException e) { // Legacy DB used Names instead of UUIDs.
			nations = TownyAPI.getInstance().getNations(line.split(","));
		}
		return nations;
	}

	private TownBlock parseTownBlockFromDB(String input) throws NumberFormatException, NotRegisteredException {
		String[] tokens = input.split(getSplitter(input));
		try {
			UUID uuid = UUID.fromString(tokens[0]);
			if (universe.getWorld(uuid) == null)
				throw new NotRegisteredException("TownBlock tried to load an invalid world!");
			return universe.getTownBlock(new WorldCoord(uuid, Integer.parseInt(tokens[1].trim()), Integer.parseInt(tokens[2].trim())));
		} catch (IllegalArgumentException e) { // Legacy DB used Names instead of UUIDs.
			if (universe.getWorld(tokens[0]) == null)
				throw new NotRegisteredException("TownBlock tried to load an invalid world!");
			return universe.getTownBlock(new WorldCoord(tokens[0], Integer.parseInt(tokens[1].trim()), Integer.parseInt(tokens[2].trim())));
		}
	}

	@Nullable
	private Location parseSpawnLocationFromDB(String raw) {
		String[] tokens = raw.split(getSplitter(raw));
		if (tokens.length >= 4)
			try {
				World world = null;
				try {
					world = Bukkit.getWorld(UUID.fromString(tokens[0]));
				} catch (IllegalArgumentException e) { // Legacy DB used Names instead of UUIDs.
					world = Bukkit.getWorld(tokens[0]);
				}
				if (world == null)
					return null;
				double x = Double.parseDouble(tokens[1]);
				double y = Double.parseDouble(tokens[2]);
				double z = Double.parseDouble(tokens[3]);

				Location loc = new Location(world, x, y, z);
				if (tokens.length == 6) {
					loc.setPitch(Float.parseFloat(tokens[4]));
					loc.setYaw(Float.parseFloat(tokens[5]));
				}
				return loc;
			} catch (NumberFormatException | NullPointerException ignored) {
			}
		return null;
	}

	// TODO: return this to private once TownBlock loading is made new. 
	static UUID[] toUUIDArray(String[] uuidArray) throws IllegalArgumentException {
		UUID[] uuids = new UUID[uuidArray.length];

		for (int i = 0; i < uuidArray.length; i++)
			uuids[i] = UUID.fromString(uuidArray[i]);

		return uuids;
	}

	private String generateMissingName() {
		// TODO: Make this a thing.
		return "bob";
	}

	/**
	 * Legacy DB used , instead of #.
	 * @param raw Text from DB
	 * @return splitter character.
	 */
	private String getSplitter(String raw) {
		return raw.contains("#") ? "#" : ",";
	}
}
