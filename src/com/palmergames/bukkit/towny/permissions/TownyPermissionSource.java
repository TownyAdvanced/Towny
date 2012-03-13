package com.palmergames.bukkit.towny.permissions;

import org.anjocaido.groupmanager.GroupManager;
import org.bukkit.entity.Player;
import org.bukkit.permissions.Permission;
import org.bukkit.permissions.PermissionDefault;

import ru.tehkode.permissions.bukkit.PermissionsEx;

import com.palmergames.bukkit.towny.Towny;
import com.palmergames.bukkit.towny.TownySettings;
import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.towny.object.TownyPermission;
import com.palmergames.bukkit.towny.object.TownyWorld;


/**
 * @author ElgarL
 * 
 * Manager for Permission provider plugins
 *
 */
public abstract class TownyPermissionSource {
	protected TownySettings settings;
	protected Towny plugin;
	
	protected GroupManager groupManager = null;
	protected com.nijikokun.bukkit.Permissions.Permissions permissions = null;
	protected PermissionsEx pex = null;


	abstract public String getPrefixSuffix(Resident resident, String node);
	abstract public int getGroupPermissionIntNode(String playerName, String node);
	abstract public boolean hasPermission(Player player, String node);
	abstract public String getPlayerGroup(Player player);
	abstract public String getPlayerPermissionStringNode(String playerName, String node);
	
	public boolean hasWildOverride(TownyWorld world, Player player, int blockId, TownyPermission.ActionType action) {

		boolean bpermissions;
		
		//check for permissions
		if (bpermissions = plugin.isPermissions())
			if ((hasPermission(player, PermissionNodes.TOWNY_WILD_BLOCK_ALL.getNode(blockId + "." + action.toString().toLowerCase()))))
				return true;
		
		// Allow ops all access when no permissions
		if (!bpermissions) {
			if (isTownyAdmin(player))
				return true;
			
			// No perms so check world settings.
			switch (action) {

			case BUILD:
				return world.getUnclaimedZoneBuild() || world.isUnclaimedZoneIgnoreId(blockId);
			case DESTROY:
				return world.getUnclaimedZoneDestroy() || world.isUnclaimedZoneIgnoreId(blockId);
			case SWITCH:
				return world.getUnclaimedZoneSwitch() || world.isUnclaimedZoneIgnoreId(blockId);
			case ITEM_USE:
				return world.getUnclaimedZoneItemUse() || world.isUnclaimedZoneIgnoreId(blockId);
			}
		}
		
		return false;
	}
	
	public boolean hasOwnTownOverride(Player player, int blockId, TownyPermission.ActionType action) {

		boolean bpermissions;
		
		//check for permissions
		if (bpermissions = plugin.isPermissions())
			if ((hasPermission(player, PermissionNodes.TOWNY_CLAIMED_ALL_BLOCK.getNode(blockId + "." + action.toString().toLowerCase())))
				|| (hasAllTownOverride(player, blockId, action)))
				return true;

		// Allow ops all access when no permissions
		if ((!bpermissions) && (isTownyAdmin(player)))
			return true;
		
		return false;
	}
	
	public boolean hasAllTownOverride(Player player, int blockId, TownyPermission.ActionType action) {

		boolean bpermissions;
		
		//check for permissions
		if (bpermissions = plugin.isPermissions())
			if ((hasPermission(player, PermissionNodes.TOWNY_CLAIMED_ALL_BLOCK.getNode(blockId + "." + action.toString().toLowerCase()))))
				return true;

		// Allow ops all access when no permissions
		if ((!bpermissions) && (isTownyAdmin(player)))
			return true;
		
		return false;
	}
	
	public boolean isTownyAdmin(Player player) {
		if (player.isOp())
			return true;
		return hasPermission(player, PermissionNodes.TOWNY_ADMIN.getNode());
	}
	
	public void registerPermissionNodes() {
		
		plugin.getServer().getScheduler().scheduleSyncDelayedTask(plugin, new Runnable(){

			@Override
			public void run() {
				Permission perm = null;
				
				for (int blockId = 0; blockId < 512; blockId++) {
					/**
					 * Register all towny.wild.block.[id].* nodes
					 */
					perm = new Permission(PermissionNodes.TOWNY_WILD_BLOCK_BUILD.getNode(blockId), "User can build a specific block in the wild.", PermissionDefault.FALSE, null);
					perm.addParent(PermissionNodes.TOWNY_WILD_BUILD.getNode(), true);
					perm.addParent(PermissionNodes.TOWNY_WILD_BLOCK_BUILD.getNode(), true);
					
					perm = new Permission(PermissionNodes.TOWNY_WILD_BLOCK_DESTROY.getNode(blockId + ""), "User can destroy a specific block in the wild.", PermissionDefault.FALSE, null);
					perm.addParent(PermissionNodes.TOWNY_WILD_DESTROY.getNode(), true);
					perm.addParent(PermissionNodes.TOWNY_WILD_BLOCK_DESTROY.getNode(), true);
					
					perm = new Permission(PermissionNodes.TOWNY_WILD_BLOCK_SWITCH.getNode(blockId + ""), "User can switch a specific block in the wild.", PermissionDefault.FALSE, null);
					perm.addParent(PermissionNodes.TOWNY_WILD_SWITCH.getNode(), true);
					perm.addParent(PermissionNodes.TOWNY_WILD_BLOCK_SWITCH.getNode(), true);
					
					perm = new Permission(PermissionNodes.TOWNY_WILD_BLOCK_ITEM_USE.getNode(blockId + ""), "User can item_use a specific block in the wild.", PermissionDefault.FALSE, null);
					perm.addParent(PermissionNodes.TOWNY_WILD_ITEM_USE.getNode(), true);
					perm.addParent(PermissionNodes.TOWNY_WILD_BLOCK_ITEM_USE.getNode(), true);
					
					/**
					 * Register all towny.claimed.alltown.block.[id].* nodes
					 */
					perm = new Permission(PermissionNodes.TOWNY_CLAIMED_ALL_BLOCK_BUILD.getNode(blockId + ""), "User can build in all town zones.", PermissionDefault.FALSE, null);
					perm.addParent(PermissionNodes.TOWNY_CLAIMED_BUILD.getNode(), true);
					perm.addParent(PermissionNodes.TOWNY_CLAIMED_ALL_BLOCK_BUILD.getNode(), true);
					
					perm = new Permission(PermissionNodes.TOWNY_CLAIMED_ALL_BLOCK_DESTROY.getNode(blockId + ""), "User can destroy in all town zones.", PermissionDefault.FALSE, null);
					perm.addParent(PermissionNodes.TOWNY_CLAIMED_DESTROY.getNode(), true);
					perm.addParent(PermissionNodes.TOWNY_CLAIMED_ALL_BLOCK_DESTROY.getNode(), true);
					
					perm = new Permission(PermissionNodes.TOWNY_CLAIMED_ALL_BLOCK_SWITCH.getNode(blockId + ""), "User can switch in all town zones.", PermissionDefault.FALSE, null);
					perm.addParent(PermissionNodes.TOWNY_CLAIMED_SWITCH.getNode(), true);
					perm.addParent(PermissionNodes.TOWNY_CLAIMED_ALL_BLOCK_SWITCH.getNode(), true);
					
					perm = new Permission(PermissionNodes.TOWNY_CLAIMED_ALL_BLOCK_ITEM_USE.getNode(blockId + ""), "User can item_use in all town zones.", PermissionDefault.FALSE, null);
					perm.addParent(PermissionNodes.TOWNY_CLAIMED_ITEM_USE.getNode(), true);
					perm.addParent(PermissionNodes.TOWNY_CLAIMED_ALL_BLOCK_ITEM_USE.getNode(), true);
					
					/**
					 * Register all towny.claimed.owntown.block.[id].* nodes
					 */
					perm = new Permission(PermissionNodes.TOWNY_CLAIMED_OWNTOWN_BLOCK_BUILD.getNode(blockId + ""), "User can build in own town zones.", PermissionDefault.FALSE, null);
					perm.addParent(PermissionNodes.TOWNY_CLAIMED_BUILD.getNode(), true);
					perm.addParent(PermissionNodes.TOWNY_CLAIMED_OWNTOWN_BLOCK_BUILD.getNode(), true);
					
					perm = new Permission(PermissionNodes.TOWNY_CLAIMED_OWNTOWN_BLOCK_DESTROY.getNode(blockId + ""), "User can destroy in own town zones.", PermissionDefault.FALSE, null);
					perm.addParent(PermissionNodes.TOWNY_CLAIMED_DESTROY.getNode(), true);
					perm.addParent(PermissionNodes.TOWNY_CLAIMED_OWNTOWN_BLOCK_DESTROY.getNode(), true);
					
					perm = new Permission(PermissionNodes.TOWNY_CLAIMED_OWNTOWN_BLOCK_SWITCH.getNode(blockId + ""), "User can switch in own town zones.", PermissionDefault.FALSE, null);
					perm.addParent(PermissionNodes.TOWNY_CLAIMED_SWITCH.getNode(), true);
					perm.addParent(PermissionNodes.TOWNY_CLAIMED_OWNTOWN_BLOCK_SWITCH.getNode(), true);
					
					perm = new Permission(PermissionNodes.TOWNY_CLAIMED_OWNTOWN_BLOCK_ITEM_USE.getNode(blockId + ""), "User can item_use in own town zones.", PermissionDefault.FALSE, null);
					perm.addParent(PermissionNodes.TOWNY_CLAIMED_ITEM_USE.getNode(), true);
					perm.addParent(PermissionNodes.TOWNY_CLAIMED_OWNTOWN_BLOCK_ITEM_USE.getNode(), true);
				}
				
			}
		},1);
	}
	
}