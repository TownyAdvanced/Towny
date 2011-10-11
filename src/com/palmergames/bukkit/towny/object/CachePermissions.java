package com.palmergames.bukkit.towny.object;

import org.bukkit.Location;
import org.bukkit.entity.Player;

import com.palmergames.bukkit.towny.NotRegisteredException;
import com.palmergames.bukkit.towny.PlayerCache;
import com.palmergames.bukkit.towny.TownyException;
import com.palmergames.bukkit.towny.TownySettings;
import com.palmergames.bukkit.towny.PlayerCache.TownBlockStatus;
import com.palmergames.bukkit.towny.object.TownyPermission.ActionType;


/**
 * Groups all the cache status and permissions in one place.
 * 
 * @author ElgarL/Shade
 *
 */
public class CachePermissions extends TownyUniverse {
	
	/** getCachePermission
     * 
     * returns player cached permission for
     * BUILD, DESTROY, SWITCH or ITEM_USE
     * 
     * @param player
     * @param location
     * @param action
     * @return
     */
    public boolean getCachePermission(Player player, Location location, ActionType action ) {
    	
    	WorldCoord worldCoord;
    	
    	try {
			worldCoord = new WorldCoord(TownyUniverse.getWorld(player.getWorld().getName()), Coord.parseCoord(location));
			PlayerCache cache = plugin.getCache(player);
			cache.updateCoord(worldCoord);
			
			plugin.sendDebugMsg("Cache permissions for " + action.toString() + " : " + cache.getCachePermission(action));
			return cache.getCachePermission(action) || plugin.isTownyAdmin(player); // Throws NullPointerException if the cache is empty
			
		} catch (NotRegisteredException e) {
			// World not known
			e.printStackTrace();
		} catch (NullPointerException e) {
			// New or old cache permission was null, update it
			
			try {
				worldCoord = new WorldCoord(TownyUniverse.getWorld(player.getWorld().getName()), Coord.parseCoord(location));
				
				TownBlockStatus status = cacheStatus(player, worldCoord, getStatusCache(player, worldCoord));
				//plugin.cacheBuild(player, worldCoord, plugin.getPermission(player, status, worldCoord, action));
				triggerCacheCreate(player, location, worldCoord, status, action);
				
				PlayerCache cache = plugin.getCache(player);
				cache.updateCoord(worldCoord);
				
				plugin.sendDebugMsg("New Cache permissions for " + action.toString() + " : " + cache.getCachePermission(action));
				return cache.getCachePermission(action) || plugin.isTownyAdmin(player);
				
			} catch (NotRegisteredException e1) {
				// Will never get here.
			}
			
		}
		return false;
	}
    
    private void triggerCacheCreate(Player player, Location location, WorldCoord worldCoord, TownBlockStatus status, ActionType action) {
		
		switch(action){
		
		case BUILD: // BUILD
			cacheBuild(player, worldCoord, getPermission(player, status, worldCoord, action));
			return;
		case DESTROY: // DESTROY
			cacheDestroy(player, worldCoord, getPermission(player, status, worldCoord, action));		
			return;
		case SWITCH: // SWITCH
			cacheSwitch(player, worldCoord, getPermission(player, status, worldCoord, action));			
			return;
		case ITEM_USE: // ITEM_USE
			cacheItemUse(player, worldCoord, getPermission(player, status, worldCoord, action));
			return;
		default:
			//for future expansion of permissions
			
		}
		
	}
    
    public TownBlockStatus getStatusCache(Player player, WorldCoord worldCoord) {
        //if (isTownyAdmin(player))
        //        return TownBlockStatus.ADMIN;
        
        if (!worldCoord.getWorld().isUsingTowny())
                return TownBlockStatus.OFF_WORLD;
        
        //TownyUniverse universe = plugin.getTownyUniverse();
        TownBlock townBlock;
        Town town;
        try {
                townBlock = worldCoord.getTownBlock();
                town = townBlock.getTown();
        } catch (NotRegisteredException e) {
                // Unclaimed Zone switch rights
                return TownBlockStatus.UNCLAIMED_ZONE;
        }

        Resident resident;
        try {
                resident = plugin.getTownyUniverse().getResident(player.getName());
        } catch (TownyException e) {
        	System.out.print("Failed to fetch resident: " + player.getName());
                return TownBlockStatus.NOT_REGISTERED;
        }
        
        try {
                // War Time switch rights
                if (isWarTime())
                        try {
                                if (!resident.getTown().getNation().isNeutral() && !town.getNation().isNeutral())
                                        return TownBlockStatus.WARZONE; 
                        } catch (NotRegisteredException e) {
                        }
                
                // Town Owner Override
                try {
                        if (townBlock.getTown().isMayor(resident) || townBlock.getTown().hasAssistant(resident))
                                return TownBlockStatus.TOWN_OWNER;
                } catch (NotRegisteredException e) {
                }
                        
                // Resident Plot switch rights
                try {
                        Resident owner = townBlock.getResident();
                        if (resident == owner)
                                return TownBlockStatus.PLOT_OWNER;
                        else if (owner.hasFriend(resident))
                                return TownBlockStatus.PLOT_FRIEND;
                        else if (resident.hasTown() && isAlly(owner.getTown(), resident.getTown()))
                                return TownBlockStatus.PLOT_ALLY;
                        else
                                // Exit out and use town permissions
                                throw new TownyException();
                } catch (NotRegisteredException x) {
                } catch (TownyException x) {
                }

                // Town resident destroy rights
                if (!resident.hasTown())
                        throw new TownyException();

                if (resident.getTown() != town) {
                        // Allied destroy rights
                        if (isAlly(town, resident.getTown()))
                                return TownBlockStatus.TOWN_ALLY;
                        else if (isEnemy(resident.getTown(), town)) {
                        	if (townBlock.isWarZone())
                    			return TownBlockStatus.WARZONE;
                    		else
                    			return TownBlockStatus.ENEMY;
                		} else
                                return TownBlockStatus.OUTSIDER;
                } else if (resident.isMayor() || resident.getTown().hasAssistant(resident))
                        return TownBlockStatus.TOWN_OWNER;
                else
                        return TownBlockStatus.TOWN_RESIDENT;
        } catch (TownyException e) {
                // Outsider destroy rights
                return TownBlockStatus.OUTSIDER;
        }
    }

    public TownBlockStatus cacheStatus(Player player, WorldCoord worldCoord, TownBlockStatus townBlockStatus) {
        PlayerCache cache = plugin.getCache(player);
        cache.updateCoord(worldCoord);
        cache.setStatus(townBlockStatus);

        plugin.sendDebugMsg(player.getName() + " (" + worldCoord.toString() + ") Cached Status: " + townBlockStatus);
        return townBlockStatus;
	}


	public void cacheBuild(Player player, WorldCoord worldCoord, boolean buildRight) {
        PlayerCache cache = plugin.getCache(player);
        cache.updateCoord(worldCoord);
        cache.setBuildPermission(buildRight);

        plugin.sendDebugMsg(player.getName() + " (" + worldCoord.toString() + ") Cached Build: " + buildRight);
	}

	public void cacheDestroy(Player player, WorldCoord worldCoord, boolean destroyRight) {
        PlayerCache cache = plugin.getCache(player);
        cache.updateCoord(worldCoord);
        cache.setDestroyPermission(destroyRight);

        plugin.sendDebugMsg(player.getName() + " (" + worldCoord.toString() + ") Cached Destroy: " + destroyRight);
	}

	public void cacheSwitch(Player player, WorldCoord worldCoord, boolean switchRight) {
        PlayerCache cache = plugin.getCache(player);
        cache.updateCoord(worldCoord);
        cache.setSwitchPermission(switchRight);

        plugin.sendDebugMsg(player.getName() + " (" + worldCoord.toString() + ") Cached Switch: " + switchRight);
	}

	public void cacheItemUse(Player player, WorldCoord worldCoord, boolean itemUseRight) {
        PlayerCache cache = plugin.getCache(player);
        cache.updateCoord(worldCoord);
        cache.setItemUsePermission(itemUseRight);

        plugin.sendDebugMsg(player.getName() + " (" + worldCoord.toString() + ") Cached Item Use: " + itemUseRight);
	}

	public void cacheBlockErrMsg(Player player, String msg) {
        PlayerCache cache = plugin.getCache(player);
        cache.setBlockErrMsg(msg);
	}
	
	 public boolean getPermission(Player player, TownBlockStatus status, WorldCoord pos, TownyPermission.ActionType actionType) {
         if (status == TownBlockStatus.OFF_WORLD ||
                 status == TownBlockStatus.WARZONE ||
                 status == TownBlockStatus.PLOT_OWNER ||
                 status == TownBlockStatus.TOWN_OWNER ||
                 plugin.isTownyAdmin(player)) // status == TownBlockStatus.ADMIN ||
                         return true;
         
         if (status == TownBlockStatus.NOT_REGISTERED) {
                 cacheBlockErrMsg(player, TownySettings.getLangString("msg_cache_block_error"));
                 return false;
         }
         
         TownBlock townBlock;
         Town town;
         try {
                 townBlock = pos.getTownBlock();
                 town = townBlock.getTown();
         } catch (NotRegisteredException e) {
                 
                 // Wilderness Permissions
                 if (status == TownBlockStatus.UNCLAIMED_ZONE)
                         if (TownyUniverse.getPermissionSource().hasPermission(player, "towny.wild." + actionType.toString()))
                                 return true;
                 
                         else if (!TownyPermission.getUnclaimedZonePerm(actionType, pos.getWorld())) {
                                 // TODO: Have permission to destroy here
                                 cacheBlockErrMsg(player, String.format(TownySettings.getLangString("msg_cache_block_error_wild"), actionType.toString()));
                                 return false;
                         } else
                                 return true;
                 else {
                         plugin.sendErrorMsg(player, "Error updating destroy permission.");
                         return false;
                 }
         }
         
         // Plot Permissions
         try {
                 Resident owner = townBlock.getResident();
                 
                 if (status == TownBlockStatus.PLOT_FRIEND) {
                         if (owner.getPermissions().getResidentPerm(actionType))
                                 return true;
                         else {
                                 cacheBlockErrMsg(player, String.format(TownySettings.getLangString("msg_cache_block_error_plot"), "friends", actionType.toString()));
                                 return false;
                         }
                 } else if (status == TownBlockStatus.PLOT_ALLY)
                         if (owner.getPermissions().getAllyPerm(actionType))
                                 return true;
                         else {
                                 cacheBlockErrMsg(player, String.format(TownySettings.getLangString("msg_cache_block_error_plot"), "allies", actionType.toString()));
                                 return false;
                         }
                 else //TODO: (Remove) if (status == TownBlockStatus.OUTSIDER)
                         if (owner.getPermissions().getOutsiderPerm(actionType))
                                 return true;
                         else {
                                 cacheBlockErrMsg(player, String.format(TownySettings.getLangString("msg_cache_block_error_plot"), "outsiders", actionType.toString()));
                                 return false;
                         }
         } catch (NotRegisteredException x) {
         }
 
         // Town Permissions
         if (status == TownBlockStatus.TOWN_RESIDENT) {
                 if (town.getPermissions().getResidentPerm(actionType))
                         return true;
                 else {
                         cacheBlockErrMsg(player, String.format(TownySettings.getLangString("msg_cache_block_error_town_resident"), actionType.toString()));
                         return false;
                 }
         } else if (status == TownBlockStatus.TOWN_ALLY)
                 if (town.getPermissions().getAllyPerm(actionType))
                         return true;
                 else {
                         cacheBlockErrMsg(player, String.format(TownySettings.getLangString("msg_cache_block_error_town_allies"), actionType.toString()));
                         return false;
                 }
         else if (status == TownBlockStatus.OUTSIDER || status == TownBlockStatus.ENEMY)
                 if (town.getPermissions().getOutsiderPerm(actionType))
                         return true;
                 else {
                         cacheBlockErrMsg(player, String.format(TownySettings.getLangString("msg_cache_block_error_town_outsider"), actionType.toString()));
                         return false;
                 }
         
         plugin.sendErrorMsg(player, "Error updating " + actionType.toString() + " permission.");
         return false;
 }       

	
	
}