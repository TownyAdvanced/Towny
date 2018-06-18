package com.palmergames.bukkit.towny.permissions;

import org.anjocaido.groupmanager.GroupManager;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.permissions.PermissionAttachmentInfo;

import ru.tehkode.permissions.bukkit.PermissionsEx;

import com.palmergames.bukkit.towny.Towny;
import com.palmergames.bukkit.towny.TownyMessaging;
import com.palmergames.bukkit.towny.TownySettings;
import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.towny.object.TownyPermission;
import com.palmergames.bukkit.towny.object.TownyUniverse;
import com.palmergames.bukkit.towny.object.TownyWorld;
import com.palmergames.bukkit.util.BukkitTools;

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
	protected PermissionsEx pex = null;

	abstract public String getPrefixSuffix(Resident resident, String node);

	abstract public int getGroupPermissionIntNode(String playerName, String node);
	
	abstract public int getPlayerPermissionIntNode(String playerName, String node);

	//abstract public boolean hasPermission(Player player, String node);

	abstract public String getPlayerGroup(Player player);

	abstract public String getPlayerPermissionStringNode(String playerName, String node);

	protected int getEffectivePermIntNode(String playerName, String node) {

		/*
		 * Bukkit doesn't support non boolean nodes
		 * so treat the same as bPerms
		 */
		Player player = BukkitTools.getPlayer(playerName);

		for (PermissionAttachmentInfo test : player.getEffectivePermissions()) {
			if (test.getPermission().startsWith(node + ".")) {
				String[] split = test.getPermission().split("\\.");
				try {
					return Integer.parseInt(split[split.length - 1]);
				} catch (NumberFormatException e) {
				}
			}
		}

		return -1;

	}

	/**
	 * Test if the player has a wild override to permit this action.
	 * 
	 * @param world
	 * @param player
	 * @param material
	 * @param action
	 * @return true if the action is permitted.
	 */
	public boolean hasWildOverride(TownyWorld world, Player player, Material material, TownyPermission.ActionType action) {

		// check for permissions
		if (plugin.isPermissions()) {

			String blockPerm = PermissionNodes.TOWNY_WILD_ALL.getNode(action.toString().toLowerCase() + "." + material);
			//String dataPerm = PermissionNodes.TOWNY_WILD_ALL.getNode(action.toString().toLowerCase() + "." + blockId + ":" + data);

			//boolean dataRegistered = player.isPermissionSet(dataPerm);

			boolean hasBlock = has(player, blockPerm);
			//boolean hasData = has(player, dataPerm);

			/*
			 * If the player has the data node permission registered directly
			 *  or
			 * the player has the block permission and the data node isn't registered
			 */
			//if ((hasData))  && dataRegistered) || (hasBlock && !dataRegistered))
			if (hasBlock)
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
					return world.getUnclaimedZoneBuild() || world.isUnclaimedZoneIgnoreMaterial(material);
				case DESTROY:
					return world.getUnclaimedZoneDestroy() || world.isUnclaimedZoneIgnoreMaterial(material);
				case SWITCH:
					return world.getUnclaimedZoneSwitch() || world.isUnclaimedZoneIgnoreMaterial(material);
				case ITEM_USE:
					return world.getUnclaimedZoneItemUse() || world.isUnclaimedZoneIgnoreMaterial(material);
			}
		}

		return false;
	}

	public boolean unclaimedZoneAction(TownyWorld world, Material material, TownyPermission.ActionType action) {
		
		switch (action) {

			case BUILD:
				return world.getUnclaimedZoneBuild() || world.isUnclaimedZoneIgnoreMaterial(material);
			case DESTROY:
				return world.getUnclaimedZoneDestroy() || world.isUnclaimedZoneIgnoreMaterial(material);
			case SWITCH:
				return world.getUnclaimedZoneSwitch() || world.isUnclaimedZoneIgnoreMaterial(material);
			case ITEM_USE:
				return world.getUnclaimedZoneItemUse() || world.isUnclaimedZoneIgnoreMaterial(material);
		}

		return false;
	}

	/**
	 * Test if the player has an own town (or all town) override to permit this action.
	 * 
	 * @param player
	 * @param material
	 * @param action
	 * @return true if the action is permitted.
	 */
	public boolean hasOwnTownOverride(Player player, Material material, TownyPermission.ActionType action) {

		//check for permissions
		if (plugin.isPermissions()) {

			String blockPerm = PermissionNodes.TOWNY_CLAIMED_ALL.getNode("owntown." + action.toString().toLowerCase() + "." + material);
			//String dataPerm = PermissionNodes.TOWNY_CLAIMED_ALL.getNode("owntown." + action.toString().toLowerCase() + "." + blockId + ":" + data);

			//boolean dataRegistered = player.isPermissionSet(dataPerm);

			boolean hasBlock = has(player, blockPerm);
			//boolean hasData = has(player, dataPerm);

			TownyMessaging.sendDebugMsg(player.getName() + " - owntown (Block: " + material);

			/*
			 * If the player has the data node permission registered directly
			 *  or
			 * the player has the block permission and the data node isn't registered
			 *  or
			 * the player has an All town Override
			 */
			if (hasBlock || hasAllTownOverride(player, material, action))
				return true;

		} else {

			// Allow ops all access when no permissions
			if (isTownyAdmin(player))
				return true;
		}

		return false;
	}

	/**
	 * Test if the player has a 'town owned', 'Own town' or 'all town' override to permit this action.
	 * 
	 * @param player
	 * @param material
	 * @param action
	 * @return
	 */
	public boolean hasTownOwnedOverride(Player player, Material material, TownyPermission.ActionType action) {

		//check for permissions
		if (plugin.isPermissions()) {

			String blockPerm = PermissionNodes.TOWNY_CLAIMED_ALL.getNode("townowned." + action.toString().toLowerCase() + "." + material);
			//String dataPerm = PermissionNodes.TOWNY_CLAIMED_ALL.getNode("townowned." + action.toString().toLowerCase() + "." + blockId + ":" + data);

			//boolean dataRegistered = player.isPermissionSet(dataPerm);

			boolean hasBlock = has(player, blockPerm);
			//boolean hasData = has(player, dataPerm);

			TownyMessaging.sendDebugMsg(player.getName() + " - townowned (Block: " + hasBlock);

			/*
			 * If the player has the data node permission registered directly
			 *  or
			 * the player has the block permission and the data node isn't registered
			 *  or
			 * the player has an Own Town Override
			 *  or
			 * the player has an All town Override
			 */
			if (hasBlock || hasOwnTownOverride(player, material, action) || hasAllTownOverride(player, material, action))
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
	 * @param material
	 * @param action
	 * @return true if the action is permitted.
	 */
	public boolean hasAllTownOverride(Player player, Material material, TownyPermission.ActionType action) {

		//check for permissions
		if (plugin.isPermissions()) {

			String blockPerm = PermissionNodes.TOWNY_CLAIMED_ALL.getNode("alltown." + action.toString().toLowerCase() + "." + material);
			//String dataPerm = PermissionNodes.TOWNY_CLAIMED_ALL.getNode("alltown." + action.toString().toLowerCase() + "." + blockId + ":" + data);

			//boolean blockRegistered = player.isPermissionSet(blockPerm);
			//boolean dataRegistered = player.isPermissionSet(dataPerm);

			boolean hasBlock = has(player, blockPerm);
			//boolean hasData = has(player, dataPerm);

			TownyMessaging.sendDebugMsg(player.getName() + " - alltown (Block: " + hasBlock);

			/*
			 * If the player has the data node permission registered directly
			 *  or
			 * the player has the block permission and the data node isn't registered
			 */
			if (hasBlock)
				return true;

		} else {

			// Allow ops all access when no permissions
			if (isTownyAdmin(player))
				return true;
		}

		return false;
	}	

	public boolean isTownyAdmin(Player player) {

		return ((player == null) || player.isOp()) || (plugin.isPermissions() && has(player, PermissionNodes.TOWNY_ADMIN.getNode()));

	}

	public boolean testPermission(Player player, String perm) {

		if (!TownyUniverse.getPermissionSource().isTownyAdmin(player) && (!has(player, perm)))
			return false;

		return true;
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
	 * Wrappers for backwards compatibility
	 */
	@Deprecated
	public boolean hasWildOverride(TownyWorld world, Player player, int blockId, TownyPermission.ActionType action) {

		return hasWildOverride(world, player, blockId, (byte) 0, action);
	}

	@Deprecated
	public boolean hasOwnTownOverride(Player player, int blockId, TownyPermission.ActionType action) {

		return hasOwnTownOverride(player, blockId, (byte) 0, action);
	}

	@Deprecated
	public boolean hasAllTownOverride(Player player, int blockId, TownyPermission.ActionType action) {

		return hasAllTownOverride(player, blockId, (byte) 0, action);
	}
	
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
	@Deprecated
	public boolean hasWildOverride(TownyWorld world, Player player, int blockId, byte data, TownyPermission.ActionType action) {

		// check for permissions
		if (plugin.isPermissions()) {

			String blockPerm = PermissionNodes.TOWNY_WILD_ALL.getNode(action.toString().toLowerCase() + "." + blockId);
			String dataPerm = PermissionNodes.TOWNY_WILD_ALL.getNode(action.toString().toLowerCase() + "." + blockId + ":" + data);

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

		} 
		return false;
	}
	
	@Deprecated
	public boolean unclaimedZoneAction(TownyWorld world, int blockId, TownyPermission.ActionType action) {

		String mat = BukkitTools.getMaterial(blockId).name();

		switch (action) {

		case BUILD:
			return world.getUnclaimedZoneBuild() || world.isUnclaimedZoneIgnoreId(mat);
		case DESTROY:
			return world.getUnclaimedZoneDestroy() || world.isUnclaimedZoneIgnoreId(mat);
		case SWITCH:
			return world.getUnclaimedZoneSwitch() || world.isUnclaimedZoneIgnoreId(mat);
		case ITEM_USE:
			return world.getUnclaimedZoneItemUse() || world.isUnclaimedZoneIgnoreId(mat);
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
	@Deprecated
	public boolean hasOwnTownOverride(Player player, int blockId, byte data, TownyPermission.ActionType action) {

		//check for permissions
		if (plugin.isPermissions()) {

			String blockPerm = PermissionNodes.TOWNY_CLAIMED_ALL.getNode("owntown." + action.toString().toLowerCase() + "." + blockId);
			String dataPerm = PermissionNodes.TOWNY_CLAIMED_ALL.getNode("owntown." + action.toString().toLowerCase() + "." + blockId + ":" + data);

			boolean dataRegistered = player.isPermissionSet(dataPerm);

			boolean hasBlock = has(player, blockPerm);
			boolean hasData = has(player, dataPerm);

			TownyMessaging.sendDebugMsg(player.getName() + " - owntown (Block: " + hasBlock + " - Data: " + hasData + ":" + (dataRegistered ? "Registered" : "None"));

			/*
			 * If the player has the data node permission registered directly
			 *  or
			 * the player has the block permission and the data node isn't registered
			 *  or
			 * the player has an All town Override
			 */
			if ((hasData && dataRegistered) || (hasBlock && !dataRegistered) || hasAllTownOverride(player, blockId, data, action))
				return true;

		} else {

			// Allow ops all access when no permissions
			if (isTownyAdmin(player))
				return true;
		}

		return false;
	}
	
	/**
	 * Test if the player has a 'town owned', 'Own town' or 'all town' override to permit this action.
	 * 
	 * @param player
	 * @param blockId
	 * @param data
	 * @param action
	 * @return
	 */
	@Deprecated
	public boolean hasTownOwnedOverride(Player player, int blockId, byte data, TownyPermission.ActionType action) {

		//check for permissions
		if (plugin.isPermissions()) {

			String blockPerm = PermissionNodes.TOWNY_CLAIMED_ALL.getNode("townowned." + action.toString().toLowerCase() + "." + blockId);
			String dataPerm = PermissionNodes.TOWNY_CLAIMED_ALL.getNode("townowned." + action.toString().toLowerCase() + "." + blockId + ":" + data);

			boolean dataRegistered = player.isPermissionSet(dataPerm);

			boolean hasBlock = has(player, blockPerm);
			boolean hasData = has(player, dataPerm);

			TownyMessaging.sendDebugMsg(player.getName() + " - townowned (Block: " + hasBlock + " - Data: " + hasData + ":" + (dataRegistered ? "Registered" : "None"));

			/*
			 * If the player has the data node permission registered directly
			 *  or
			 * the player has the block permission and the data node isn't registered
			 *  or
			 * the player has an Own Town Override
			 *  or
			 * the player has an All town Override
			 */
			if ((hasData && dataRegistered) || (hasBlock && !dataRegistered) || hasOwnTownOverride(player, blockId, data, action) || hasAllTownOverride(player, blockId, data, action))
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
	@Deprecated
	public boolean hasAllTownOverride(Player player, int blockId, byte data, TownyPermission.ActionType action) {

		//check for permissions
		if (plugin.isPermissions()) {

			String blockPerm = PermissionNodes.TOWNY_CLAIMED_ALL.getNode("alltown." + action.toString().toLowerCase() + "." + blockId);
			String dataPerm = PermissionNodes.TOWNY_CLAIMED_ALL.getNode("alltown." + action.toString().toLowerCase() + "." + blockId + ":" + data);

			//boolean blockRegistered = player.isPermissionSet(blockPerm);
			boolean dataRegistered = player.isPermissionSet(dataPerm);

			boolean hasBlock = has(player, blockPerm);
			boolean hasData = has(player, dataPerm);

			TownyMessaging.sendDebugMsg(player.getName() + " - alltown (Block: " + hasBlock + " - Data: " + hasData + ":" + (dataRegistered ? "Registered" : "None"));

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

}
