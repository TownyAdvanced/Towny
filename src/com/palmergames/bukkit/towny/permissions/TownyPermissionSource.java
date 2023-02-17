package com.palmergames.bukkit.towny.permissions;

import org.anjocaido.groupmanager.GroupManager;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.permissions.Permissible;
import org.bukkit.permissions.PermissionAttachmentInfo;

import com.palmergames.bukkit.towny.Towny;
import com.palmergames.bukkit.towny.TownySettings;
import com.palmergames.bukkit.towny.exceptions.NoPermissionException;
import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.towny.object.TownyPermission;
import com.palmergames.bukkit.towny.object.TownyWorld;
import com.palmergames.bukkit.towny.object.Translatable;
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

	abstract public String getPrefixSuffix(Resident resident, String node);

	abstract public int getGroupPermissionIntNode(String playerName, String node);
	
	abstract public int getPlayerPermissionIntNode(String playerName, String node);

	abstract public String getPlayerGroup(Player player);

	abstract public String getPlayerPermissionStringNode(String playerName, String node);

	protected int getEffectivePermIntNode(String playerName, String node) {

		/*
		 * Bukkit doesn't support non boolean nodes
		 * so treat the same as bPerms
		 */
		Player player = BukkitTools.getPlayerExact(playerName);
		if (player == null)
			return -1;

		int biggest = -1;
		for (PermissionAttachmentInfo test : player.getEffectivePermissions()) {
			if (test.getPermission().startsWith(node + ".")) {
				String[] split = test.getPermission().split("\\.");
				try {
					int i = Integer.parseInt(split[split.length - 1]);
					biggest = Math.max(biggest, i);
				} catch (NumberFormatException e) {
				}
			}
		}
		return biggest;
	}

	/**
	 * Test if the player has permission to permit this action in the wilderness.
	 * 
	 * @param world    TownyWorld object.
	 * @param player   Player.
	 * @param material Material being tested.
	 * @param action   Action type.
	 * @return true if the action is permitted.
	 */
	public boolean hasWildOverride(TownyWorld world, Player player, Material material, TownyPermission.ActionType action) {

		// Figure out what permission node this would be.
		String blockPerm = PermissionNodes.TOWNY_WILD_ALL.getNode(action.toString().toLowerCase() + "." + material);

		/*
		 * Test if the player is an admin or actually has the specific permission node or,
		 * Test if the player the world explicitly allows this action or material type.
		 */
		return testPermission(player, blockPerm) || unclaimedZoneAction(world, material, action);

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
			default:
		}

		return false;
	}

	/**
	 * Test if the player has an own town (or all town) override to permit this
	 * action.
	 * 
	 * @param player   Player.
	 * @param material Material being tested.
	 * @param action   ActionType.
	 * @return true if the action is permitted.
	 */
	public boolean hasOwnTownOverride(Player player, Material material, TownyPermission.ActionType action) {

		// Figure out what permission node this would be.
		String blockPerm = PermissionNodes.TOWNY_CLAIMED_OWNTOWN_ALL.getNode(action.toString().toLowerCase() + "." + material);

		/*
		 * Test if the player is an admin or actually has the specific permission node or,
		 * Test if the player is allowed permissions in all towns for the material and action.
		 */
		return testPermission(player, blockPerm) || hasAllTownOverride(player, material, action);
	}

	/**
	 * Test if the player has a 'town owned', 'Own town' or 'all town' override to
	 * permit this action.
	 * 
	 * @param player   Player.
	 * @param material Material being tested.
	 * @param action   ActionType.
	 * @return true if action is permitted.
	 */
	public boolean hasTownOwnedOverride(Player player, Material material, TownyPermission.ActionType action) {

		// Figure out what permission node this would be.
		String blockPerm = PermissionNodes.TOWNY_CLAIMED_TOWNOWNED_ALL.getNode(action.toString().toLowerCase() + "." + material);

		/*
		 * Test if the player is an admin or actually has the specific permission node or,
		 * Test if the player is allowed permissions everywhere in their own town for the material and action or, 
		 * Test if the player is allowed permissions in all towns for the material and action.
		 */
		return testPermission(player, blockPerm) || hasOwnTownOverride(player, material, action) || hasAllTownOverride(player, material, action);
	}

	/**
	 * Test if the player has an all town override to permit this action.
	 * 
	 * @param player   Player.
	 * @param material Material being tested.
	 * @param action   ActionType.
	 * @return true if the action is permitted.
	 */
	public boolean hasAllTownOverride(Player player, Material material, TownyPermission.ActionType action) {

		// Figure out what permission node this would be.
		String blockPerm = PermissionNodes.TOWNY_CLAIMED_ALLTOWN_ALL.getNode(action.toString().toLowerCase() + "." + material);

		/*
		 * Test if the player is an admin or actually has the specific permission node or,
		 * Test if the player is allowed permissions in all towns for the material and action.
		 */
		return testPermission(player, blockPerm);
	}

	/**
	 * Tests if a Player is considered an admin and does not currenctly have the
	 * adminbypass mode enabled.
	 * 
	 * @param player Player to check.
	 * @return true if the player is not in adminbypass mode, and they are
	 *         considered a admin Permissible.
	 */
	public boolean isTownyAdmin(Player player) {
		return !Towny.getPlugin().hasPlayerMode(player, "adminbypass") && isTownyAdmin((Permissible) player);
	}

	/**
	 * The final destination for isTownyAdmin tests.
	 * 
	 * @param permissible Permissible object, a player or a console.
	 * @return true if the permissible is null or op or has the towny.admin node.
	 */
	public boolean isTownyAdmin(Permissible permissible) {
		return permissible == null || permissible.isOp() || strictHas(permissible, PermissionNodes.TOWNY_ADMIN.getNode());
	}

	/**
	 * A method to test if a Permissible has a specific permission node.
	 * 
	 * @param permissible Permissible object, a player or a console.
	 * @param perm        String representing the node to test for.
	 * @throws NoPermissionException thrown when the player does not have the
	 *                               required node.
	 */
	public void testPermissionOrThrow(Permissible permissible, String perm) throws NoPermissionException {
		if (!testPermission(permissible, perm))
			throw new NoPermissionException();
	}

	/**
	 * A method to test if a Permissible has a specific permission node, which will
	 * show a non-generic no permission message if the Permissible does not have the
	 * node.
	 * 
	 * @param permissible Permissible object, a player or a console.
	 * @param perm        String representing the node to test for.
	 * @param errormsg    Translatable used when the Permissible has no permission.
	 * @throws NoPermissionException thrown when the player does not have the
	 *                               required node, with custom Translatable
	 *                               message.
	 */
	public void testPermissionOrThrow(Permissible permissible, String perm, Translatable errormsg) throws NoPermissionException {
		if (!testPermission(permissible, perm))
			throw new NoPermissionException(errormsg);
	}

	/**
	 * A method that will check if a Player has a permission node or is otherwise
	 * allowed by Towny to do something, be it because they are OP, have the
	 * towny.admin permission node, or have the specified permission node (or a
	 * wildcard parent node.)
	 * 
	 * @param player Player to test.
	 * @param perm   String representing the node to test for.
	 * @return true if the player has the permission node or is otherwise allowed.
	 */
	public boolean testPermission(Player player, String perm) {
		return testPermission((Permissible) player, perm);
	}

	/**
	 * Primary test for a permission node, used throughout Towny.
	 * 
	 * @param permissible Permissible to check.
	 * @param perm Permission node to check for.
	 * @return true if the player has the permission node or is considered an admin.
	 */
	public boolean testPermission(Permissible permissible, String perm) {
		if (permissible instanceof Player player)
			// This shunts the test through #isTownyAdmin(Player) which will test if the
			// Player has the adminbypass mode enabled.
			return isTownyAdmin(player) || strictHas(permissible, perm);

		return isTownyAdmin(permissible) || strictHas(permissible, perm);
	}
	
	/**
	 * Unused by Towny, scheduled for removal.
	 * @deprecated since 0.98.4.6 use {@link #testPermission(Permissible, String)} instead.
	 * @param permissible Permissible to check.
	 * @param node PermissionNode to check for.
	 * @return true if the Permissble has the PermissionNodes.
	 */
	@Deprecated
	public boolean testPermission(Permissible permissible, PermissionNodes node) {
		return testPermission(permissible, node.getNode());
	}

	/**
	 * @param player Player to check
	 * @param node Permission node to check for
	 * @return true if the player has this permission node or is Op.
	 * @deprecated since 0.98.4.6 use {@link #testPermission(Player, String)} instead.
	 */
	@Deprecated
	public boolean has(Player player, String node) {
		return isTownyAdmin(player) || strictHas(player, node);
	}

	/**
	 * Return true if a player has a certain, specific permission node or a parent
	 * wildcard node.
	 *
	 * @param permissible Permissible to check
	 * @param node        Permission node to check for
	 * @return true if the player has this permission node or a parent wildcard
	 *         node.
	 */
	private boolean strictHas(Permissible permissible, String node) {

		/*
		 * Node has been set or negated so return the actual value
		 */
		if (permissible.isPermissionSet(node))
			return permissible.hasPermission(node);

		/*
		 * Check for a parent with a wildcard
		 */
		final String[] parts = node.split("\\.");
		final StringBuilder builder = new StringBuilder(node.length());
		for (String part : parts) {
			builder.append('*');
			if (permissible.hasPermission("-" + builder.toString())) {
				return false;
			}
			if (permissible.hasPermission(builder.toString())) {
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
}
