package com.palmergames.bukkit.towny.utils;

import com.palmergames.bukkit.towny.Towny;
import com.palmergames.bukkit.towny.TownyAPI;
import com.palmergames.bukkit.towny.TownyMessaging;
import com.palmergames.bukkit.towny.TownySettings;
import com.palmergames.bukkit.towny.TownyUniverse;
import com.palmergames.bukkit.towny.event.player.PlayerCacheGetTownBlockStatusEvent;
import com.palmergames.bukkit.towny.hooks.PluginIntegrations;
import com.palmergames.bukkit.towny.object.Coord;
import com.palmergames.bukkit.towny.object.Nation;
import com.palmergames.bukkit.towny.object.PlayerCache;
import com.palmergames.bukkit.towny.object.PlayerCache.TownBlockStatus;
import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.towny.object.Town;
import com.palmergames.bukkit.towny.object.TownBlock;
import com.palmergames.bukkit.towny.object.TownyPermission;
import com.palmergames.bukkit.towny.object.TownyPermission.ActionType;
import com.palmergames.bukkit.towny.object.Translatable;
import com.palmergames.bukkit.towny.object.WorldCoord;
import com.palmergames.bukkit.towny.permissions.PermissionNodes;
import com.palmergames.bukkit.towny.regen.TownyRegenAPI;
import com.palmergames.bukkit.towny.utils.PermissionGUIUtil.SetPermissionType;
import com.palmergames.bukkit.util.BukkitTools;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;

/**
 * Groups all the cache status and permissions in one place.
 * 
 * @author ElgarL/Shade
 * 
 */
public class PlayerCacheUtil {
	
	static Towny plugin = null;
	
	public static void initialize(Towny plugin) {
		PlayerCacheUtil.plugin = plugin;
	}

	/**
	 * Returns the PlayerCache of a Player.
	 * @param player The {@link Player} for which to fetch the {@link PlayerCache}.
	 * @return PlayerCache
	 */
	public static PlayerCache getCache(Player player) {
		
		return plugin.getCache(player);
	}

	/**
	 * Returns player cached permission for BUILD, DESTROY, SWITCH or ITEM_USE
	 * at this location for the specified item id.
	 * 
	 * Generates the cache if it doesn't exist.
	 * 
	 * @param player - Player to check
	 * @param location - Location 
	 * @param material - Material
	 * @param action - ActionType
	 * @return true if the player has permission.
	 */
	public static boolean getCachePermission(Player player, Location location, Material material, ActionType action) {

		WorldCoord worldCoord;

		try {
			// Test required for portalCreateEvent in WorldListener, player hasn't changed worlds yet.
			if (location.getWorld().equals(player.getWorld())) 
				worldCoord = new WorldCoord(player.getWorld().getName(), Coord.parseCoord(location));
			else 
				worldCoord = new WorldCoord(location.getWorld().getName(), Coord.parseCoord(location));
			PlayerCache cache = plugin.getCache(player);
			cache.updateCoord(worldCoord);

			TownyMessaging.sendDebugMsg("Cache permissions for " + action.toString() + " : " + cache.getCachePermission(material, action));
			return cache.getCachePermission(material, action); // Throws NullPointerException if the cache is empty

		} catch (NullPointerException e) {
			// New or old cache permission was null, update it

			// Test required for portalCreateEvent in WorldListener, player hasn't changed worlds yet.
			if (location.getWorld().equals(player.getWorld())) 
				worldCoord = new WorldCoord(player.getWorld().getName(), Coord.parseCoord(location));
			else 
				worldCoord = new WorldCoord(location.getWorld().getName(), Coord.parseCoord(location));

			TownBlockStatus status = cacheStatus(player, worldCoord, fetchTownBlockStatus(player, worldCoord));
			triggerCacheCreate(player, location, worldCoord, status, material, action);

			PlayerCache cache = plugin.getCache(player);
			cache.updateCoord(worldCoord);
			
			TownyMessaging.sendDebugMsg("New Cache Created and updated!");

			TownyMessaging.sendDebugMsg("New Cache permissions for " + material + ":" + action.toString() + ":" + status.name() + " = " + cache.getCachePermission(material, action));
			return cache.getCachePermission(material, action);
		}
	}

	/**
	 * Generate a new cache for this player/action.
	 * 
	 * @param player - Player
	 * @param location - Location
	 * @param worldCoord - WorldCoord
	 * @param status - TownBlockStatus
	 * @param material - Material
	 * @param action - ActionType
	 */
	private static void triggerCacheCreate(Player player, Location location, WorldCoord worldCoord, TownBlockStatus status, Material material, ActionType action) {

		switch (action) {

		case BUILD: // BUILD
			cacheBuild(player, worldCoord, material, getPermission(player, status, worldCoord, material, action));
			return;
		case DESTROY: // DESTROY
			cacheDestroy(player, worldCoord, material, getPermission(player, status, worldCoord, material, action));
			return;
		case SWITCH: // SWITCH
			cacheSwitch(player, worldCoord, material, getPermission(player, status, worldCoord, material, action));
			return;
		case ITEM_USE: // ITEM_USE
			cacheItemUse(player, worldCoord, material, getPermission(player, status, worldCoord, material, action));
			return;
		default:
			//for future expansion of permissions
		}
	}
	
	/**
	 * Update and return back the townBlockStatus for the player at this
	 * worldCoord.
	 * 
	 * @param player - Player
	 * @param worldCoord - WorldCoord
	 * @param townBlockStatus - TownBlockStatus
	 * @return TownBlockStatus type.
	 */
	public static TownBlockStatus cacheStatus(Player player, WorldCoord worldCoord, TownBlockStatus townBlockStatus) {

		PlayerCache cache = plugin.getCache(player);
		cache.updateCoord(worldCoord);
		cache.setStatus(townBlockStatus);

		TownyMessaging.sendDebugMsg(player.getName() + " (" + worldCoord.toString() + ") Cached Status: " + townBlockStatus);
		return townBlockStatus;
	}

	/**
	 * Update the player cache for Build rights at this WorldCoord.
	 * 
	 * @param player - Player
	 * @param worldCoord - WorldCoord
	 * @param material - Material
	 * @param buildRight - Boolean
	 */
	private static void cacheBuild(Player player, WorldCoord worldCoord, Material material, Boolean buildRight) {

		PlayerCache cache = plugin.getCache(player);
		cache.updateCoord(worldCoord);
		cache.setBuildPermission(material, buildRight);

		TownyMessaging.sendDebugMsg(player.getName() + " (" + worldCoord.toString() + ") Cached Build: " + buildRight);
	}

	/**
	 * Update the player cache for Destroy rights at this WorldCoord.
	 * 
	 * @param player - Player
	 * @param worldCoord - WorldCoord
	 * @param material - Material
	 * @param destroyRight - Boolean
	 */
	private static void cacheDestroy(Player player, WorldCoord worldCoord, Material material, Boolean destroyRight) {

		PlayerCache cache = plugin.getCache(player);
		cache.updateCoord(worldCoord);
		cache.setDestroyPermission(material, destroyRight);

		TownyMessaging.sendDebugMsg(player.getName() + " (" + worldCoord.toString() + ") Cached Destroy: " + destroyRight);
	}

	/**
	 * Update the player cache for Switch rights at this WorldCoord.
	 * 
	 * @param player - Player
	 * @param worldCoord - WorldCoord
	 * @param material - Material
	 * @param switchRight - Boolean
	 */
	private static void cacheSwitch(Player player, WorldCoord worldCoord, Material material, Boolean switchRight) {

		PlayerCache cache = plugin.getCache(player);
		cache.updateCoord(worldCoord);
		cache.setSwitchPermission(material, switchRight);

		TownyMessaging.sendDebugMsg(player.getName() + " (" + worldCoord.toString() + ") Cached Switch: " + switchRight);
	}

	/**
	 * Update the player cache for Item_use rights at this WorldCoord.
	 * 
	 * @param player - Player
	 * @param worldCoord - WorldCoord
	 * @param material - Material
	 * @param itemUseRight - Boolean
	 */
	private static void cacheItemUse(Player player, WorldCoord worldCoord, Material material, Boolean itemUseRight) {

		PlayerCache cache = plugin.getCache(player);
		cache.updateCoord(worldCoord);
		cache.setItemUsePermission(material, itemUseRight);

		TownyMessaging.sendDebugMsg(player.getName() + " (" + worldCoord.toString() + ") Cached Item Use: " + itemUseRight);
	}

	/**
	 * Update the cached BlockErrMsg for this player.
	 * 
	 * @param player - Player
	 * @param msg - String
	 */
	public static void cacheBlockErrMsg(Player player, String msg) {

		PlayerCache cache = plugin.getCache(player);
		cache.setBlockErrMsg(msg);
	}

	/**
	 * Wrapper for {@link #getTownBlockStatus(Player, WorldCoord)} which allows another plugin
	 * to alter the pre-determined TownBlockStatus.
	 * 
	 * @param player {@link Player} who is getting a new TownBlockStatus for their PlayerCache.
	 * @param worldCoord {@link WorldCoord} where the TownBlockStatus is being fetched for.
	 * @return {@link TownBlockStatus} which will be used by Towny.
	 */
	public static TownBlockStatus fetchTownBlockStatus(Player player, WorldCoord worldCoord) {
		TownBlockStatus status = getTownBlockStatus(player, worldCoord);
		PlayerCacheGetTownBlockStatusEvent event = new PlayerCacheGetTownBlockStatusEvent(player, worldCoord, status);
		BukkitTools.fireEvent(event);
		return event.getTownBlockStatus();
	}
	
	/**
	 * Fetch the TownBlockStatus type for this player at this WorldCoord.
	 * 
	 * @param player - Player
	 * @param worldCoord - WorldCoord
	 * @return TownBlockStatus type.
	 */
	public static TownBlockStatus getTownBlockStatus(Player player, WorldCoord worldCoord) {

		if (!TownyAPI.getInstance().isTownyWorld(worldCoord.getBukkitWorld()))
			return TownBlockStatus.OFF_WORLD;

		// Has to be wilderness.
		if (!worldCoord.hasTownBlock())
			// When nation zones are enabled we do extra tests to determine if this is near to a nation.
			// If NationZones are not enabled we return normal wilderness.
			return TownySettings.getNationZonesEnabled() ? TownyAPI.getInstance().hasNationZone(worldCoord) : TownBlockStatus.UNCLAIMED_ZONE;  

		// Has to be in a town.
		TownBlock townBlock = worldCoord.getTownBlockOrNull();
		Town town = worldCoord.getTownOrNull();
		if (townBlock.isLocked()) {
			// Push the TownBlock location to the queue for a snapshot (if it's not already in the queue).
			if (townBlock.getWorld().isUsingPlotManagementRevert() && (TownySettings.getPlotManagementSpeed() > 0)) {
				TownyRegenAPI.addWorldCoord(townBlock.getWorldCoord());
				return TownBlockStatus.LOCKED;
			}
			townBlock.setLocked(false);
		}

		/*
		 * Find the resident data for this player.
		 */
		Resident resident = TownyUniverse.getInstance().getResident(player.getUniqueId());
		
		if (resident == null) {
			// Check if entity is a Citizens NPC
			if (PluginIntegrations.getInstance().checkCitizens(player))
				return TownBlockStatus.NOT_REGISTERED;
 
			// Retry getting a resident with the ability to get a fake player resident.
			resident = TownyUniverse.getInstance().getResident(player.getName());
			
			if (resident == null) {
				// If not an NPC then there is likely some sort of problem that should be logged.
				plugin.getLogger().warning("Failed to fetch resident: " + player.getName());
				return TownBlockStatus.NOT_REGISTERED;
			}
		}

		if (town.isMayor(resident))
			return TownBlockStatus.TOWN_OWNER;
		
		if (town.hasTrustedResident(resident))
			return TownBlockStatus.TOWN_TRUSTED;
		
		if (townBlock.hasTrustedResident(resident) && !townBlock.hasResident(resident))
			return TownBlockStatus.PLOT_TRUSTED;
		
		// Resident Plot rights
		if (townBlock.hasResident()) {
			Resident owner = townBlock.getResidentOrNull();
			if (resident == owner)
				return TownBlockStatus.PLOT_OWNER;
			else if (owner.hasFriend(resident))
				return TownBlockStatus.PLOT_FRIEND;
			else if (resident.hasTown() && CombatUtil.isSameTown(owner, resident))
				return TownBlockStatus.PLOT_TOWN;
			else if (resident.hasTown() && CombatUtil.isAlly(owner, resident))
				return TownBlockStatus.PLOT_ALLY;
			else
				return TownBlockStatus.OUTSIDER;
		}

		// Resident with no town.
		if (!resident.hasTown())
			return TownBlockStatus.OUTSIDER;
		
		// Town has this resident, who isn't the mayor.
		if (town.hasResident(resident))
			return TownBlockStatus.TOWN_RESIDENT;
		
		// Nation group.
		if (CombatUtil.isSameNation(town, resident.getTownOrNull()))
			return TownBlockStatus.TOWN_NATION;
		
		// Ally group.
		if (CombatUtil.isAlly(town, resident.getTownOrNull()))
			return TownBlockStatus.TOWN_ALLY;
		
		// Enemy.
		if (CombatUtil.isEnemy(resident.getTownOrNull(), town))
			return TownBlockStatus.ENEMY;

		// Nothing left but Outsider.
		return TownBlockStatus.OUTSIDER;
	}

	/**
	 * Test if the player has permission to perform a certain action at this
	 * WorldCoord.
	 * 
	 * @param player - {@link Player}
	 * @param status - {@link TownBlockStatus}
	 * @param pos - {@link WorldCoord}
	 * @param material - {@link Material}
	 * @param action {@link ActionType}
	 * @return true if allowed.
	 */
	private static boolean getPermission(Player player, TownBlockStatus status, WorldCoord pos, Material material, TownyPermission.ActionType action) {
		// Allow admins to have ALL permissions
		TownyUniverse townyUniverse = TownyUniverse.getInstance();
		if (townyUniverse.getPermissionSource().isTownyAdmin(player))
			return true;
		
		Town targetTown = pos.getTownOrNull();

		//If town is bankrupt (but not ruined), nobody can build
		if(targetTown != null 
			&& TownySettings.isTownBankruptcyEnabled() 
			&& action == ActionType.BUILD
			&& targetTown.isBankrupt() 
			&& !targetTown.isRuined()) {
				cacheBlockErrMsg(player, Translatable.of("msg_err_bankrupt_town_cannot_build").forLocale(player));
				return false;
		}

		if (status == TownBlockStatus.OFF_WORLD || status == TownBlockStatus.PLOT_OWNER || status == TownBlockStatus.TOWN_OWNER)
			return true;
		
		Resident res = townyUniverse.getResident(player.getUniqueId());
		if (res == null) {
			cacheBlockErrMsg(player, Translatable.of("msg_err_not_registered").forLocale(player));
			return false;
		}

		if (status == TownBlockStatus.NOT_REGISTERED) {
			cacheBlockErrMsg(player, Translatable.of("msg_cache_block_error").forLocale(player));
			return false;
		}

		if (status == TownBlockStatus.LOCKED) {
			cacheBlockErrMsg(player, Translatable.of("msg_cache_block_error_locked").forLocale(player));
			return false;
		}

		/*
		 * Handle the wilderness. 
		 */
		if (TownyAPI.getInstance().isWilderness(pos)) {
			
			/*
			 * Handle the Wilderness.
			 */
			boolean hasWildOverride = townyUniverse.getPermissionSource().hasWildOverride(pos.getTownyWorld(), player, material, action);

			if (status == TownBlockStatus.UNCLAIMED_ZONE) {
				if (hasWildOverride)
					return true;

				// Don't have permission to build/destroy/switch/item_use here
				cacheBlockErrMsg(player, Translatable.of("msg_cache_block_error_wild", Translatable.of(action.toString())).forLocale(player));
				return false;
			}
			
			/*
			 * Handle the possiblity that NationZones are enabled the 
			 * TownBlockStatus is NATION_ZONE instead of UNCLAIMED_ZONE.
			 * In all situations the player still has to hasWildOverride.
			 */
			if (TownySettings.getNationZonesEnabled() && status == TownBlockStatus.NATION_ZONE) {
				// Admins that also have wilderness permission can bypass the nation zone.
				if (res.hasPermissionNode(PermissionNodes.TOWNY_ADMIN_NATION_ZONE.getNode()))
					return true;

				// Wasn't able to build in the wilderness, regardless.
				if (!hasWildOverride) {
					cacheBlockErrMsg(player, Translatable.of("msg_cache_block_error_wild", Translatable.of(action.toString())).forLocale(player));
					return false;
				}

				// We know that the nearest Town will have a nation because the TownBlockStatus.
				Nation nearestNation = TownyAPI.getInstance().getTownNationOrNull(pos.getTownyWorld().getClosestTownWithNationFromCoord(pos.getCoord(), null));

				// If the player has a Nation and is a member of this NationZone's nation.
				if (res.hasNation() && res.getNationOrNull().getUUID().equals(nearestNation.getUUID()))
					return true;

				// The player is not a nation member of this NationZone.
				cacheBlockErrMsg(player, Translatable.of("nation_zone_this_area_under_protection_of", pos.getTownyWorld().getFormattedUnclaimedZoneName(), nearestNation.getName()).forLocale(player));
				return false;
			}
		}
		
		/*
		 * Not going to be in the wilderness at this point.
		 */
		TownBlock townBlock = pos.getTownBlockOrNull();
		
		/*
		 * Check all-towns overrides before testing any plot permissions.
		 */
		if (townyUniverse.getPermissionSource().hasAllTownOverride(player, material, action))
			return true;
		
		/*
		 * Check own-town & town-owned overrides before testing plot permissions, if the resident is in their own town.
		 */
		if (targetTown.equals(TownyAPI.getInstance().getResidentTownOrNull(res))) {
			
			if (townyUniverse.getPermissionSource().hasOwnTownOverride(player, material, action))
				return true;
		
			if (!townBlock.hasResident() && townyUniverse.getPermissionSource().hasTownOwnedOverride(player, material, action))
				return true;
		}
		
		/*
		 * Player has a permission override set.
		 */
		if (townBlock.getPermissionOverrides().containsKey(res) && townBlock.getPermissionOverrides().get(res).getPermissionTypes()[action.getIndex()] != SetPermissionType.UNSET) {
			SetPermissionType type = townBlock.getPermissionOverrides().get(res).getPermissionTypes()[action.getIndex()];
			if (type == SetPermissionType.NEGATED)
				cacheBlockErrMsg(player, Translatable.of("msg_cache_block_err", Translatable.of(action.toString())).forLocale(player));
			
			return type.equals(SetPermissionType.SET);				
		}
		
		/*
		 * Player has Trusted status here.
		 */
		if (status == TownBlockStatus.PLOT_TRUSTED || status == TownBlockStatus.TOWN_TRUSTED)
			return true;
		
		/*
		 * Handle personally-owned plots' friend and town permissions.
		 */
		if (status == TownBlockStatus.PLOT_FRIEND) {
			
			// Plot allows Friends perms and we aren't stopped by a TownBlockType overriding the allowed material and action.
			if (townBlock.getPermissions().getResidentPerm(action) && isAllowedMaterial(townBlock, material, action))
				return true;

			cacheBlockErrMsg(player, Translatable.of("msg_cache_block_error_plot", Translatable.of("msg_cache_block_error_plot_friends"), Translatable.of(action.toString())).forLocale(player));
			return false;
		} 
		
		if (status == TownBlockStatus.PLOT_TOWN) {

			// Plot allows Town perms and we aren't stopped by a TownBlockType overriding the allowed material and action.
			if (townBlock.getPermissions().getNationPerm(action) && isAllowedMaterial(townBlock, material, action))
				return true;
			
			cacheBlockErrMsg(player, Translatable.of("msg_cache_block_error_plot", Translatable.of("msg_cache_block_error_plot_town_members"), Translatable.of(action.toString())).forLocale(player));
			return false;
		}

		/*
		 * Handle town-owned plots' resident and nation permissions.
		 */
		if (status == TownBlockStatus.TOWN_RESIDENT) {
			
			// Plot allows Resident perms and we aren't stopped by a TownBlockType overriding the allowed material and action.
			if (townBlock.getPermissions().getResidentPerm(action) && isAllowedMaterial(townBlock, material, action))
				return true;

			cacheBlockErrMsg(player, Translatable.of("msg_cache_block_error_town_resident", Translatable.of(action.toString())).forLocale(player));
			return false;
		} 
		
		if (status == TownBlockStatus.TOWN_NATION) {

			// Plot allows Nation perms and we aren't stopped by a TownBlockType overriding the allowed material and action.
			if (townBlock.getPermissions().getNationPerm(action) && isAllowedMaterial(townBlock, material, action))
				return true;

			cacheBlockErrMsg(player, Translatable.of("msg_cache_block_error_town_nation", Translatable.of(action.toString())).forLocale(player));
			return false;
		}
		
		/*
		 * Handle both personally-owned and town-owned Ally permissions.
		 */
		if (status == TownBlockStatus.PLOT_ALLY || status == TownBlockStatus.TOWN_ALLY) {

			// Plot allows Ally perms and we aren't stopped by a TownBlockType overriding things.
			if (townBlock.getPermissions().getAllyPerm(action) && isAllowedMaterial(townBlock, material, action))
				return true;

			// Choose which error message will be shown.
			if (status == TownBlockStatus.PLOT_ALLY) 
				cacheBlockErrMsg(player, Translatable.of("msg_cache_block_error_plot", Translatable.of("msg_cache_block_error_plot_allies"), Translatable.of(action.toString())).forLocale(player));
			else 
				cacheBlockErrMsg(player, Translatable.of("msg_cache_block_error_town_allies", Translatable.of(action.toString())).forLocale(player));
			return false;
		}
		
		/*
		 * Handle both personally-owned and town-owned Outsider and Enemy statuses.
		 */
		if (status == TownBlockStatus.OUTSIDER || status == TownBlockStatus.ENEMY) {
			
			// Plot allows Outsider perms and we aren't stopped by a TownBlockType overriding the allowed material and action.
			if (townBlock.getPermissions().getOutsiderPerm(action) && isAllowedMaterial(townBlock, material, action))
				return true;

			// Choose which error message will be shown.
			if (townBlock.hasResident())
				cacheBlockErrMsg(player, Translatable.of("msg_cache_block_error_plot", Translatable.of("msg_cache_block_error_plot_outsiders"), Translatable.of(action.toString())).forLocale(player));
			else 
				cacheBlockErrMsg(player, Translatable.of("msg_cache_block_error_town_outsider", Translatable.of(action.toString())).forLocale(player));
			return false;
		}
		
		/*
		 * Towny doesn't set a WARZONE status itself, some other plugin has used the API. 
		 */
		if (status == TownBlockStatus.WARZONE)
			return true;

		TownyMessaging.sendErrorMsg(player, "Error updating " + action.toString() + " permission.");
		return false;
	}

	/**  
	 * @param townBlock The townblock.
	 * @param material Material being actioned upon.
	 * @param action ActionType being done on the material.
	 * @return True if this material is allowed in this townblock.
	 */
	private static boolean isAllowedMaterial(TownBlock townBlock, Material material, ActionType action) {
		if ((action == ActionType.BUILD || action == ActionType.DESTROY) && !townBlock.getData().getAllowedBlocks().isEmpty())
			return townBlock.getData().getAllowedBlocks().contains(material);

		return true;
	}
}