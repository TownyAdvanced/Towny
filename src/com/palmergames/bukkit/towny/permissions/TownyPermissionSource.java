package com.palmergames.bukkit.towny.permissions;

import org.anjocaido.groupmanager.GroupManager;
import org.bukkit.entity.Player;
//import org.bukkit.permissions.Permission;
//import org.bukkit.permissions.PermissionDefault;

import ru.tehkode.permissions.bukkit.PermissionsEx;

import com.palmergames.bukkit.towny.Towny;
import com.palmergames.bukkit.towny.TownyMessaging;
import com.palmergames.bukkit.towny.TownySettings;
import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.towny.object.TownyPermission;
import com.palmergames.bukkit.towny.object.TownyWorld;

/**
 * @author ElgarL
 * 
 *         Manager for Permission provider plugins
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

	//abstract public boolean hasPermission(Player player, String node);

	abstract public String getPlayerGroup(Player player);

	abstract public String getPlayerPermissionStringNode(String playerName, String node);

	
	/*
	 * Wrappers for backwards compatibility
	 */
	@Deprecated
	public boolean hasWildOverride(TownyWorld world, Player player, int blockId, TownyPermission.ActionType action) {
		return hasWildOverride(world, player, blockId, (byte)0, action);
	}
	@Deprecated
	public boolean hasOwnTownOverride(Player player, int blockId, TownyPermission.ActionType action) {
		return hasOwnTownOverride(player, blockId, (byte)0, action);
	}
	@Deprecated
	public boolean hasAllTownOverride(Player player, int blockId, TownyPermission.ActionType action) {
		return hasAllTownOverride(player, blockId, (byte)0, action);
	}

	/*
	 * 
	 */

	/**
	 * Test if the player has a wild override to permit this action.
	 * 
	 * @param world
	 * @param player
	 * @param blockId
	 * @param data
	 * @param action
	 * @return true if the action is permitted.
	 */
	public boolean hasWildOverride(TownyWorld world, Player player, int blockId, byte data, TownyPermission.ActionType action) {

		// check for permissions
		if (plugin.isPermissions()) {

			String blockPerm = PermissionNodes.TOWNY_WILD_ALL.getNode(action.toString().toLowerCase() + "." + blockId);
			String dataPerm = PermissionNodes.TOWNY_WILD_ALL.getNode(action.toString().toLowerCase() + "." + blockId + ":" + data);

			//boolean blockRegistered = player.isPermissionSet(blockPerm);
			boolean dataRegistered = player.isPermissionSet(dataPerm);

			boolean hasBlock = has(player, blockPerm);
			boolean hasData = has(player, dataPerm);

			/*
			 * If the player has the data node permission registered directly
			 *  or
			 * the player has the block permission and the data node isn't registered
			 */
			if ((hasData && dataRegistered) || (hasBlock && !dataRegistered))
				return true;

			// No node set but we are using permissions so check world settings
			// (without UnclaimedIgnoreId's).
			switch (action) {

			case BUILD:
				return world.getUnclaimedZoneBuild();
			case DESTROY:
				return world.getUnclaimedZoneDestroy();
			case SWITCH:
				return world.getUnclaimedZoneSwitch();
			case ITEM_USE:
				return world.getUnclaimedZoneItemUse();
			}

		} else {
			/*
			 * Not using a permissions plugin
			 */

			// Allow ops all access when no permissions
			if (isTownyAdmin(player))
				return true;

			// Check world settings as we are not using permissions.
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
	
	/**
	 * Test if the player has an own town (or all town) override to permit this action.
	 * 
	 * @param player
	 * @param blockId
	 * @param data
	 * @param action
	 * @return true if the action is permitted.
	 */
	public boolean hasOwnTownOverride(Player player, int blockId, byte data, TownyPermission.ActionType action) {

		//check for permissions
		if (plugin.isPermissions()) {
			
			String blockPerm = PermissionNodes.TOWNY_CLAIMED_ALL.getNode("owntown." + action.toString().toLowerCase() + "." + blockId);
			String dataPerm = PermissionNodes.TOWNY_CLAIMED_ALL.getNode("owntown." + action.toString().toLowerCase() + "." + blockId +  ":" + data);

			//boolean blockRegistered = player.isPermissionSet(blockPerm);
			boolean dataRegistered = player.isPermissionSet(dataPerm);

			boolean hasBlock = has(player, blockPerm);
			boolean hasData = has(player, dataPerm);
			
			TownyMessaging.sendDebugMsg(player.getName() + " - owntown (Block: " + hasBlock + " - Data: " + hasData + ":" + (dataRegistered? "Registered":"None"));

			/*
			 * If the player has the data node permission registered directly
			 *  or
			 * the player has the block permission and the data node isn't registered
			 *  or
			 * the player has an All town Override
			 */
			if ((hasData && dataRegistered) || (hasBlock && !dataRegistered)|| hasAllTownOverride(player, blockId, data, action))
				return true;
			
		} else {

			// Allow ops all access when no permissions
			if (isTownyAdmin(player))
				return true;
		}

		return false;
	}

	/**
	 * Test if the player has an all town override to permit this action.
	 * 
	 * @param player
	 * @param blockId
	 * @param data
	 * @param action
	 * @return true if the action is permitted.
	 */
	public boolean hasAllTownOverride(Player player, int blockId, byte data, TownyPermission.ActionType action) {

		//check for permissions
		if (plugin.isPermissions()) {
			
			String blockPerm = PermissionNodes.TOWNY_CLAIMED_ALL.getNode("alltown." + action.toString().toLowerCase() + "." + blockId);
			String dataPerm = PermissionNodes.TOWNY_CLAIMED_ALL.getNode("alltown." + action.toString().toLowerCase() + "." + blockId + ":" + data);

			//boolean blockRegistered = player.isPermissionSet(blockPerm);
			boolean dataRegistered = player.isPermissionSet(dataPerm);

			boolean hasBlock = has(player, blockPerm);
			boolean hasData = has(player, dataPerm);
			
			TownyMessaging.sendDebugMsg(player.getName() + " - alltown (Block: " + hasBlock + " - Data: " + hasData + ":" + (dataRegistered? "Registered":"None"));


			/*
			 * If the player has the data node permission registered directly
			 *  or
			 * the player has the block permission and the data node isn't registered
			 */
			if ((hasData && dataRegistered) || (hasBlock && !dataRegistered))
				return true;
			
			
		} else {

			// Allow ops all access when no permissions
			if (isTownyAdmin(player))
				return true;
		}

		return false;
	}

	public boolean isTownyAdmin(Player player) {

		return (player.isOp()) || (plugin.isPermissions() && has(player, PermissionNodes.TOWNY_ADMIN.getNode()));

	}

	/**
	 * All permission checks should go through here.
	 * 
	 * Returns true if a player has a certain permission node.
	 * 
	 * @param player
	 * @param node
	 * @return true if the player has this permission node.
	 */
	public boolean has(Player player, String node) {

		if (player.isOp())
			return true;

		//return (plugin.isPermissions() && hasPermission(player, node));
		
		/*
		 * Node has been set or negated so return the actual value
		 */
		if (player.isPermissionSet(node))
			return player.hasPermission(node);
		
		/*
		 * Check for a parent with a wildcard
		 */
		final String[] parts = node.split("\\.");
		final StringBuilder builder = new StringBuilder(node.length());
		for (String part : parts) {
			builder.append('*');
			if (player.hasPermission("-" + builder.toString())) {
				return false;
			}
			if (player.hasPermission(builder.toString())) {
				return true;
			}
			builder.deleteCharAt(builder.length() - 1);
			builder.append(part).append('.');
		}
		
		/*
		 * No parent found so we don't have this node.
		 */
		return false;
		
	}

	/*
	 * public void registerPermissionNodes() {
	 * 
	 * plugin.getServer().getScheduler().scheduleSyncDelayedTask(plugin, new
	 * Runnable(){
	 * 
	 * @Override
	 * public void run() {
	 * Permission perm = null;
	 * 
	 * for (int blockId = 0; blockId < 512; blockId++) {
	 * /**
	 * Register all towny.wild.block.[id].* nodes
	 *//*
		 * perm = new
		 * Permission(PermissionNodes.TOWNY_WILD_BLOCK_BUILD.getNode(blockId),
		 * "User can build a specific block in the wild.",
		 * PermissionDefault.FALSE, null);
		 * perm.addParent(PermissionNodes.TOWNY_WILD_BUILD.getNode(), true);
		 * 
		 * perm = new
		 * Permission(PermissionNodes.TOWNY_WILD_BLOCK_DESTROY.getNode(blockId +
		 * ""), "User can destroy a specific block in the wild.",
		 * PermissionDefault.FALSE, null);
		 * perm.addParent(PermissionNodes.TOWNY_WILD_DESTROY.getNode(), true);
		 * 
		 * perm = new
		 * Permission(PermissionNodes.TOWNY_WILD_BLOCK_SWITCH.getNode(blockId +
		 * ""), "User can switch a specific block in the wild.",
		 * PermissionDefault.FALSE, null);
		 * perm.addParent(PermissionNodes.TOWNY_WILD_SWITCH.getNode(), true);
		 * 
		 * perm = new
		 * Permission(PermissionNodes.TOWNY_WILD_BLOCK_ITEM_USE.getNode(blockId
		 * + ""), "User can item_use a specific block in the wild.",
		 * PermissionDefault.FALSE, null);
		 * perm.addParent(PermissionNodes.TOWNY_WILD_ITEM_USE.getNode(), true);
		 * 
		 * /**
		 * Register all towny.claimed.alltown.block.[id].* nodes
		 *//*
			 * perm = new
			 * Permission(PermissionNodes.TOWNY_CLAIMED_ALL_BLOCK_BUILD
			 * .getNode(blockId + ""), "User can build in all town zones.",
			 * PermissionDefault.FALSE, null);
			 * perm.addParent(PermissionNodes.TOWNY_CLAIMED_BUILD.getNode(),
			 * true);
			 * 
			 * perm = new
			 * Permission(PermissionNodes.TOWNY_CLAIMED_ALL_BLOCK_DESTROY
			 * .getNode(blockId + ""), "User can destroy in all town zones.",
			 * PermissionDefault.FALSE, null);
			 * perm.addParent(PermissionNodes.TOWNY_CLAIMED_DESTROY.getNode(),
			 * true);
			 * 
			 * perm = new
			 * Permission(PermissionNodes.TOWNY_CLAIMED_ALL_BLOCK_SWITCH
			 * .getNode(blockId + ""), "User can switch in all town zones.",
			 * PermissionDefault.FALSE, null);
			 * perm.addParent(PermissionNodes.TOWNY_CLAIMED_SWITCH.getNode(),
			 * true);
			 * 
			 * perm = new
			 * Permission(PermissionNodes.TOWNY_CLAIMED_ALL_BLOCK_ITEM_USE
			 * .getNode(blockId + ""), "User can item_use in all town zones.",
			 * PermissionDefault.FALSE, null);
			 * perm.addParent(PermissionNodes.TOWNY_CLAIMED_ITEM_USE.getNode(),
			 * true);
			 * 
			 * /**
			 * Register all towny.claimed.owntown.block.[id].* nodes
			 *//*
				 * perm = new
				 * Permission(PermissionNodes.TOWNY_CLAIMED_OWNTOWN_BLOCK_BUILD
				 * .getNode(blockId + ""), "User can build in own town zones.",
				 * PermissionDefault.FALSE, null);
				 * perm.addParent(PermissionNodes.TOWNY_CLAIMED_BUILD.getNode(),
				 * true);
				 * 
				 * perm = new
				 * Permission(PermissionNodes.TOWNY_CLAIMED_OWNTOWN_BLOCK_DESTROY
				 * .getNode(blockId + ""),
				 * "User can destroy in own town zones.",
				 * PermissionDefault.FALSE, null);
				 * perm.addParent(PermissionNodes.TOWNY_CLAIMED_DESTROY.getNode()
				 * , true);
				 * 
				 * perm = new
				 * Permission(PermissionNodes.TOWNY_CLAIMED_OWNTOWN_BLOCK_SWITCH
				 * .getNode(blockId + ""), "User can switch in own town zones.",
				 * PermissionDefault.FALSE, null);
				 * perm.addParent(PermissionNodes.TOWNY_CLAIMED_SWITCH.getNode(),
				 * true);
				 * 
				 * perm = new
				 * Permission(PermissionNodes.TOWNY_CLAIMED_OWNTOWN_BLOCK_ITEM_USE
				 * .getNode(blockId + ""),
				 * "User can item_use in own town zones.",
				 * PermissionDefault.FALSE, null);
				 * perm.addParent(PermissionNodes.TOWNY_CLAIMED_ITEM_USE.getNode(
				 * ), true);
				 * }
				 * 
				 * }
				 * },1);
				 * }
				 */
}