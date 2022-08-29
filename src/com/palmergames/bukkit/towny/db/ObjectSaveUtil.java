package com.palmergames.bukkit.towny.db;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.bukkit.Location;

import com.google.gson.Gson;
import com.palmergames.bukkit.towny.TownyMessaging;
import com.palmergames.bukkit.towny.object.Nation;
import com.palmergames.bukkit.towny.object.PermissionData;
import com.palmergames.bukkit.towny.object.PlotGroup;
import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.towny.object.Town;
import com.palmergames.bukkit.towny.object.TownBlock;
import com.palmergames.bukkit.towny.object.TownyObject;
import com.palmergames.bukkit.towny.object.TownyWorld;
import com.palmergames.bukkit.towny.object.jail.Jail;
import com.palmergames.bukkit.towny.object.metadata.DataFieldIO;
import com.palmergames.util.StringMgmt;

public class ObjectSaveUtil {

	/*
	 * HashMap methods used to save objects in the TownyFlatFileSource and TownySQLSource
	 */

	public static HashMap<String, Object> getJailMap(Jail jail) throws Exception {
		try {
			HashMap<String, Object> jail_hm = new HashMap<>();
			jail_hm.put("uuid", jail.getUUID());
			jail_hm.put("townBlock", getTownBlockForSaving(jail.getTownBlock()));
			
			StringBuilder jailCellArray = new StringBuilder();
			if (jail.hasCells())
				for (Location cell : new ArrayList<>(jail.getJailCellLocations()))
					jailCellArray.append(parseLocationForSaving(cell)).append(";");

			jail_hm.put("spawns", jailCellArray);
			
			return jail_hm;
		} catch (Exception e) {
			TownyMessaging.sendErrorMsg("Saving: Jail HashMap could not be made.");
			e.printStackTrace();
			throw new Exception(e);
		}
	}

	public static HashMap<String, Object> getPlotGroupMap(PlotGroup group) throws Exception {
		try {
			HashMap<String, Object> pltgrp_hm = new HashMap<>();
			pltgrp_hm.put("groupID", group.getUUID());
			pltgrp_hm.put("groupName", group.getName());
			pltgrp_hm.put("groupPrice", group.getPrice());
			pltgrp_hm.put("town", group.getTown().toString());

			return pltgrp_hm;

		} catch (Exception e) {
			TownyMessaging.sendErrorMsg("Saving: PlotGroup HashMap could not be made.");
			e.printStackTrace();
			throw new Exception(e);
		}
	}

	public static HashMap<String, Object> getResidentMap(Resident resident) throws Exception {
		try {
			HashMap<String, Object> res_hm = new HashMap<>();
			res_hm.put("name", resident.getName());
			res_hm.put("uuid", resident.hasUUID() ? resident.getUUID().toString() : "");
			res_hm.put("lastOnline", resident.getLastOnline());
			res_hm.put("registered", resident.getRegistered());
			res_hm.put("joinedTownAt", resident.getJoinedTownAt());
			res_hm.put("isNPC", resident.isNPC());
			res_hm.put("jailUUID", resident.isJailed() ? resident.getJail().getUUID() : "");
			res_hm.put("jailCell", resident.getJailCell());
			res_hm.put("jailHours", resident.getJailHours());
			res_hm.put("title", resident.getTitle());
			res_hm.put("surname", resident.getSurname());
			res_hm.put("town", resident.hasTown() ? resident.getTown().getUUID() : "");
			res_hm.put("town-ranks", resident.hasTown() ? StringMgmt.join(resident.getTownRanks(), "#") : "");
			res_hm.put("nation-ranks", resident.hasTown() ? StringMgmt.join(resident.getNationRanks(), "#") : "");
			res_hm.put("friends", StringMgmt.join(resident.getFriendsUUIDs(), "#"));
			res_hm.put("protectionStatus", resident.getPermissions().toString().replaceAll(",", "#"));
			res_hm.put("metadata", resident.hasMeta() ? ObjectSaveUtil.serializeMetadata(resident) : "");
			return res_hm;
		} catch (Exception e) {
			TownyMessaging.sendErrorMsg("Saving: Town HashMap could not be made.");
			e.printStackTrace();
			throw new Exception(e);
		}
	}

	public static HashMap<String, Object> getHibernatedResidentMap(UUID uuid, long registered) throws Exception {
		try {
			HashMap<String, Object> res_hm = new HashMap<>();
			res_hm.put("uuid", uuid);
			res_hm.put("registered", registered);
			return res_hm;
		} catch (Exception e) {
			TownyMessaging.sendErrorMsg("Saving: HibernatedResident HashMap could not be made.");
			e.printStackTrace();
			throw new Exception(e);
		}
	}

	public static HashMap<String, Object> getTownBlockMap(TownBlock townBlock) throws Exception {
		try {
			HashMap<String, Object> tb_hm = new HashMap<>();
			tb_hm.put("world", townBlock.getWorld().getUUID());
			tb_hm.put("x", townBlock.getX());
			tb_hm.put("z", townBlock.getZ());
			tb_hm.put("name", townBlock.getName());
			tb_hm.put("price", townBlock.getPlotPrice());
			tb_hm.put("town", townBlock.getTown().getUUID());
			tb_hm.put("resident", (townBlock.hasResident()) ? townBlock.getResidentOrNull().getUUID() : "");
			tb_hm.put("typeName", townBlock.getTypeName());
			tb_hm.put("outpost", townBlock.isOutpost());
			tb_hm.put("permissions",
					townBlock.isChanged() ? townBlock.getPermissions().toString().replaceAll(",", "#") : "");
			tb_hm.put("locked", townBlock.isLocked());
			tb_hm.put("changed", townBlock.isChanged());
			tb_hm.put("claimedAt", townBlock.getClaimedAt());
			tb_hm.put("groupID", townBlock.hasPlotObjectGroup() ? townBlock.getPlotObjectGroup().getUUID().toString() : "");
			tb_hm.put("metadata", townBlock.hasMeta() ? serializeMetadata(townBlock) : "");
			tb_hm.put("trustedResidents", StringMgmt.join(toUUIDList(townBlock.getTrustedResidents()), "#"));

			Map<String, String> stringMap = new HashMap<>();
			for (Map.Entry<Resident, PermissionData> entry : townBlock.getPermissionOverrides().entrySet())
				stringMap.put(entry.getKey().getUUID().toString(), entry.getValue().toString());
			tb_hm.put("customPermissionData", new Gson().toJson(stringMap));

			return tb_hm;
		} catch (Exception e) {
			TownyMessaging.sendErrorMsg("Saving: TownBlock HashMap could not be made.");
			e.printStackTrace();
			throw new Exception(e);
		}
	}

	public static HashMap<String, Object> getTownMap(Town town) throws Exception {
		try {
			HashMap<String, Object> twn_hm = new HashMap<>();
			twn_hm.put("name", town.getName());
			twn_hm.put("uuid", town.hasValidUUID() ? town.getUUID() : UUID.randomUUID()); //TODO: Do we really want this?
			twn_hm.put("outlaws", StringMgmt.join(town.getOutlaws(), "#"));
			twn_hm.put("mayor", town.hasMayor() ? town.getMayor().getUUID() : "");
			twn_hm.put("nation", town.hasNation() ? town.getNation().getUUID() : "");
			twn_hm.put("assistants", StringMgmt.join(town.getRank("assistant"), "#"));
			twn_hm.put("townBoard", town.getBoard());
			twn_hm.put("tag", town.getTag());
			twn_hm.put("protectionStatus", town.getPermissions().toString().replaceAll(",", "#"));
			twn_hm.put("bonus", town.getBonusBlocks());
			twn_hm.put("manualTownLevel", town.getManualTownLevel());
			twn_hm.put("purchased", town.getPurchasedBlocks());
			twn_hm.put("nationZoneOverride", town.getNationZoneOverride());
			twn_hm.put("nationZoneEnabled", town.isNationZoneEnabled());
			twn_hm.put("commercialPlotPrice", town.getCommercialPlotPrice());
			twn_hm.put("commercialPlotTax", town.getCommercialPlotTax());
			twn_hm.put("embassyPlotPrice", town.getEmbassyPlotPrice());
			twn_hm.put("embassyPlotTax", town.getEmbassyPlotTax());
			twn_hm.put("spawnCost", town.getSpawnCost());
			twn_hm.put("plotPrice", town.getPlotPrice());
			twn_hm.put("plotTax", town.getPlotTax());
			twn_hm.put("taxes", town.getTaxes());
			twn_hm.put("hasUpkeep", town.hasUpkeep());
			twn_hm.put("hasUnlimitedClaims", town.hasUnlimitedClaims());
			twn_hm.put("taxpercent", town.isTaxPercentage());
			twn_hm.put("maxPercentTaxAmount", town.getMaxPercentTaxAmount());
			twn_hm.put("open", town.isOpen());
			twn_hm.put("public", town.isPublic());
			twn_hm.put("conquered", town.isConquered());
			twn_hm.put("conqueredDays", town.getConqueredDays());
			twn_hm.put("admindisabledpvp", town.isAdminDisabledPVP());
			twn_hm.put("adminenabledpvp", town.isAdminEnabledPVP());
			twn_hm.put("joinedNationAt", town.getJoinedNationAt());
			twn_hm.put("mapColorHexCode", town.getMapColorHexCode());
			twn_hm.put("movedHomeBlockAt", town.getMovedHomeBlockAt());
			twn_hm.put("metadata", town.hasMeta() ? serializeMetadata(town) : "");
			twn_hm.put("homeblock", town.hasHomeBlock() ? getTownBlockForSaving(town.getHomeBlock()) : "");
			twn_hm.put("spawn", town.hasSpawn() ? parseLocationForSaving(town.getSpawn()) : "");
			StringBuilder outpostArray = new StringBuilder();
			if (town.hasOutpostSpawn())
				for (Location spawn : new ArrayList<>(town.getAllOutpostSpawns()))
					outpostArray.append(parseLocationForSaving(spawn)).append(";");
			twn_hm.put("outpostSpawns", outpostArray.toString());
			twn_hm.put("registered", town.getRegistered());
			twn_hm.put("ruined", town.isRuined());
			twn_hm.put("ruinedTime", town.getRuinedTime());
			twn_hm.put("neutral", town.isNeutral());
			twn_hm.put("debtBalance", town.getDebtBalance());
			if (town.getPrimaryJail() != null)
				twn_hm.put("primaryJail", town.getPrimaryJail().getUUID());
			twn_hm.put("trustedResidents", StringMgmt.join(toUUIDList(town.getTrustedResidents()), "#"));
			twn_hm.put("allies", StringMgmt.join(town.getAlliesUUIDs(), "#"));
			twn_hm.put("enemies", StringMgmt.join(town.getEnemiesUUIDs(), "#"));
			return twn_hm;
		} catch (Exception e) {
			TownyMessaging.sendErrorMsg("Saving: Town HashMap could not be made.");
			e.printStackTrace();
			throw new Exception(e);
		}
	}

	public static HashMap<String, Object> getNationMap(Nation nation) throws Exception {
		try {
			HashMap<String, Object> nat_hm = new HashMap<>();
			nat_hm.put("name", nation.getName());
			nat_hm.put("uuid", nation.hasValidUUID() ? nation.getUUID() : UUID.randomUUID()); //TODO: Do we really want this?
			nat_hm.put("capital", nation.hasCapital() ? nation.getCapital().getUUID() : "");
			nat_hm.put("nationBoard", nation.getBoard());
			nat_hm.put("mapColorHexCode", nation.getMapColorHexCode());
			nat_hm.put("tag", nation.hasTag() ? nation.getTag() : "");
			nat_hm.put("allies", StringMgmt.join(nation.getAlliesUUIDs(), "#"));
			nat_hm.put("enemies", StringMgmt.join(nation.getEnemiesUUIDs(), "#"));
			nat_hm.put("taxes", nation.getTaxes());
			nat_hm.put("spawnCost", nation.getSpawnCost());
			nat_hm.put("neutral", nation.isNeutral());
			nat_hm.put("nationSpawn", nation.hasSpawn() ? parseLocationForSaving(nation.getSpawn()) : "");
			nat_hm.put("registered", nation.getRegistered());
			nat_hm.put("isPublic", nation.isPublic());
			nat_hm.put("isOpen", nation.isOpen());
			nat_hm.put("metadata", nation.hasMeta() ? serializeMetadata(nation) : "");
			return nat_hm;
		} catch (Exception e) {
			TownyMessaging.sendErrorMsg("Saving: Nation HashMap could not be made.");
			e.printStackTrace();
			throw new Exception(e);
		}
	}

	public static HashMap<String, Object> getWorldMap(TownyWorld world) throws Exception {
		try {
			HashMap<String, Object> world_hm = new HashMap<>();

			world_hm.put("name", world.getName());
			world_hm.put("usingTowny", world.isUsingTowny());
			world_hm.put("warAllowed", world.isWarAllowed());
			world_hm.put("pvp", world.isPVP());
			world_hm.put("forcepvp", world.isForcePVP());
			world_hm.put("friendlyFire", world.isFriendlyFireEnabled());
			world_hm.put("claimable", world.isClaimable());
			world_hm.put("worldmobs", world.hasWorldMobs());
			world_hm.put("wildernessmobs", world.hasWildernessMobs());
			world_hm.put("forcetownmobs", world.isForceTownMobs());
			world_hm.put("firespread", world.isFire());
			world_hm.put("forcefirespread", world.isForceFire());
			world_hm.put("explosions", world.isExpl());
			world_hm.put("forceexplosions", world.isForceExpl());
			world_hm.put("endermanprotect", world.isEndermanProtect());
			world_hm.put("disablecreaturetrample", world.isDisableCreatureTrample());

			world_hm.put("unclaimedZoneBuild", world.getUnclaimedZoneBuild());
			world_hm.put("unclaimedZoneDestroy", world.getUnclaimedZoneDestroy());
			world_hm.put("unclaimedZoneSwitch", world.getUnclaimedZoneSwitch());
			world_hm.put("unclaimedZoneItemUse", world.getUnclaimedZoneItemUse());
			if (world.getUnclaimedZoneName() != null)
				world_hm.put("unclaimedZoneName", world.getUnclaimedZoneName());

			// Unclaimed Zone Ignore Ids
			if (world.getUnclaimedZoneIgnoreMaterials() != null)
				world_hm.put("unclaimedZoneIgnoreIds", StringMgmt.join(world.getUnclaimedZoneIgnoreMaterials(), "#"));

			// Deleting EntityTypes from Townblocks on Unclaim.
			world_hm.put("isDeletingEntitiesOnUnclaim", world.isDeletingEntitiesOnUnclaim());
			if (world.getUnclaimDeleteEntityTypes() != null)
				world_hm.put("unclaimDeleteEntityTypes", StringMgmt.join(world.getUnclaimDeleteEntityTypes(), "#"));

			// Using PlotManagement Delete
			world_hm.put("usingPlotManagementDelete", world.isUsingPlotManagementDelete());
			// Plot Management Delete Ids
			if (world.getPlotManagementDeleteIds() != null)
				world_hm.put("plotManagementDeleteIds", StringMgmt.join(world.getPlotManagementDeleteIds(), "#"));

			// Using PlotManagement Mayor Delete
			world_hm.put("usingPlotManagementMayorDelete", world.isUsingPlotManagementMayorDelete());
			// Plot Management Mayor Delete
			if (world.getPlotManagementMayorDelete() != null)
				world_hm.put("plotManagementMayorDelete", StringMgmt.join(world.getPlotManagementMayorDelete(), "#"));

			// Using PlotManagement Revert
			world_hm.put("usingPlotManagementRevert", world.isUsingPlotManagementRevert());

			// Plot Management Ignore Ids
			if (world.getPlotManagementIgnoreIds() != null)
				world_hm.put("plotManagementIgnoreIds", StringMgmt.join(world.getPlotManagementIgnoreIds(), "#"));

			// Using PlotManagement Wild Regen
			world_hm.put("usingPlotManagementWildRegen", world.isUsingPlotManagementWildEntityRevert());

			// Wilderness Explosion Protection entities
			if (world.getPlotManagementWildRevertEntities() != null)
				world_hm.put("PlotManagementWildRegenEntities", StringMgmt.join(world.getPlotManagementWildRevertEntities(), "#"));

			// Wilderness Explosion Protection Block Whitelist
			if (world.getPlotManagementWildRevertBlockWhitelist() != null)
				world_hm.put("PlotManagementWildRegenBlockWhitelist", StringMgmt.join(world.getPlotManagementWildRevertBlockWhitelist(), "#"));

			// Using PlotManagement Wild Regen Delay
			world_hm.put("plotManagementWildRegenSpeed", world.getPlotManagementWildRevertDelay());
			
			// Using PlotManagement Wild Block Regen
			world_hm.put("usingPlotManagementWildRegenBlocks", world.isUsingPlotManagementWildBlockRevert());

			// Wilderness Explosion Protection blocks
			if (world.getPlotManagementWildRevertBlocks() != null)
				world_hm.put("PlotManagementWildRegenBlocks", StringMgmt.join(world.getPlotManagementWildRevertBlocks(), "#"));

			world_hm.put("metadata", world.hasMeta() ? serializeMetadata(world) : "");

			return world_hm;

		} catch (Exception e) {
			TownyMessaging.sendErrorMsg("Saving: World HashMap could not be made.");
			e.printStackTrace();
			throw new Exception(e);
		}
	}

	private final static String serializeMetadata(TownyObject obj) {
		return DataFieldIO.serializeCDFs(obj.getMetadata());
	}

	private static String getTownBlockForSaving(TownBlock tb) {
		return tb.getWorld().getUUID() + "#" + tb.getX() + "#" + tb.getZ();
	}

	private static String parseLocationForSaving(Location loc) {
		return loc.getWorld().getUID().toString() + "#" 
				+ loc.getX() + "#"
				+ loc.getY() + "#"
				+ loc.getZ() + "#"
				+ loc.getPitch() + "#"
				+ loc.getYaw();
	}

	private static List<UUID> toUUIDList(Collection<Resident> residents) {
		return residents.stream().filter(Resident::hasUUID).map(Resident::getUUID).collect(Collectors.toList());
	}

}
