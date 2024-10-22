package com.palmergames.bukkit.towny.permissions;

import com.palmergames.bukkit.towny.Towny;
import com.palmergames.bukkit.towny.TownyUniverse;
import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.towny.object.resident.mode.ResidentModeHandler;
import com.palmergames.bukkit.util.BukkitTools;
import com.palmergames.bukkit.util.Colors;

import org.anjocaido.groupmanager.GroupManager;
import org.anjocaido.groupmanager.data.Group;
import org.anjocaido.groupmanager.events.GMGroupEvent;
import org.anjocaido.groupmanager.events.GMSystemEvent;
import org.anjocaido.groupmanager.events.GMUserEvent;
import org.anjocaido.groupmanager.permissions.AnjoPermissionsHandler;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.plugin.IllegalPluginAccessException;
import org.bukkit.plugin.Plugin;

/**
 * @author ElgarL
 * 
 */
public class GroupManagerSource extends TownyPermissionSource {

	public GroupManagerSource(Towny towny, Plugin test) {

		this.groupManager = (GroupManager) test;
		this.plugin = towny;
		try {
			plugin.getServer().getPluginManager().registerEvents(new GMCustomEventListener(), plugin);
		} catch (IllegalPluginAccessException e) {
			plugin.getLogger().warning("Your Version of GroupManager is out of date. Please update.");
		}

	}

	/**
	 * getPermissionNode
	 * 
	 * returns the specified prefix/suffix nodes from GroupManager
	 * 
	 * @param resident - Resident to check for
	 * @param node - Node to check
	 * @return String of the Prefix or Suffix.
	 */
	@Override
	public String getPrefixSuffix(Resident resident, String node) {
		Player player = resident.getPlayer();
		if (player == null)
			return "";

		String group = "", user = "";

		//sendDebugMsg("    GroupManager installed.");
		AnjoPermissionsHandler handler = groupManager.getWorldsHolder().getWorldData(player).getPermissionsHandler();

		if (node.equals("prefix")) {
			group = handler.getGroupPrefix(handler.getPrimaryGroup(player.getName()));
			user = handler.getUserPrefix(player.getName());
		} else if (node.equals("suffix")) {
			group = handler.getGroupSuffix(handler.getPrimaryGroup(player.getName()));
			user = handler.getUserSuffix(player.getName());
		} else if (node.equals("userprefix")) {
			group = "";
			user = handler.getUserPrefix(player.getName());					
		} else if (node.equals("usersuffix")) {
			group = "";
			user = handler.getUserSuffix(player.getName());					
		} else if (node.equals("groupprefix")) {
			group = handler.getGroupPrefix(handler.getPrimaryGroup(player.getName()));
			user = "";
		} else if (node.equals("groupsuffix")) {
			group = handler.getGroupSuffix(handler.getPrimaryGroup(player.getName()));
			user = "";
		}
		if (group == null) //Don't know why this null check wasn't being used, probably has a reason though
			group = "";
		if (user == null)
			user = "";

		if (!group.equals(user))
			user = group + user;
		user = Colors.translateColorCodes(user);

		return user;

	}

	/**
	 * Gets a Group Permission's Integer Node
	 * @param playerName - Player Name to check against
	 * @param node - Node to check against
	 * @return -1 = can't find
	 */
	@Override
	public int getGroupPermissionIntNode(String playerName, String node) {

		int iReturn = -1;
		
		Player player = BukkitTools.getPlayerExact(playerName);

		AnjoPermissionsHandler handler = groupManager.getWorldsHolder().getWorldData(player).getPermissionsHandler();
		iReturn  = handler.getPermissionInteger(playerName, node);
		
		if (iReturn == -1)
			iReturn = getEffectivePermIntNode(playerName, node);
		
		return iReturn;

	}
	
	@Override
	public int getPlayerPermissionIntNode(String playerName, String node) {
		Player player = BukkitTools.getPlayerExact(playerName);
		if (player == null)
			return -1;

		AnjoPermissionsHandler handler = groupManager.getWorldsHolder().getWorldData(player).getPermissionsHandler();
		int iReturn = handler.getPermissionInteger(playerName, node);
		
		if (iReturn == -1)
			iReturn = getEffectivePermIntNode(playerName, node);
		
		return iReturn;
	}

	/**
	 * 
	 * @param playerName - Player's Name to check against
	 * @param node - Node to check
	 * @return empty = can't find
	 */
	@Override
	public String getPlayerPermissionStringNode(String playerName, String node) {

		Player player = BukkitTools.getPlayerExact(playerName);
		if (player == null)
			return "";

		AnjoPermissionsHandler handler = groupManager.getWorldsHolder().getWorldData(player).getPermissionsHandler();

		return handler.getPermissionString(playerName, node);

	}

	/**
	 * Returns the players Group name.
	 * 
	 * @param player - Player
	 * @return name of players group
	 */
	@Override
	public String getPlayerGroup(Player player) {

		AnjoPermissionsHandler handler = groupManager.getWorldsHolder().getWorldData(player).getPermissionsHandler();
		return handler.getGroup(player.getName());
	}

	protected class GMCustomEventListener implements Listener {

		public GMCustomEventListener() {

		}

		@EventHandler(priority = EventPriority.HIGH)
		public void onGMUserEvent(GMUserEvent event) {

			try {
				PermissionEventEnums.GMUser_Action.valueOf(event.getAction().name());
			} catch (IllegalArgumentException e) {
				// Not tracking this event type
				return;
			}

			updateDefaultResidentModes(event.getUserName());
		}

		@EventHandler(priority = EventPriority.HIGH)
		public void onGMGroupEvent(GMGroupEvent event) {

			try {
				if (PermissionEventEnums.GMGroup_Action.valueOf(event.getAction().name()) != null) {

					Group group = event.getGroup();
					// Update all players who are in this group.
					for (Player player : BukkitTools.getOnlinePlayers())
						if (player != null && group.toString().equals(getPlayerGroup(player)))
							updateDefaultResidentModes(player.getName());
				}
			} catch (IllegalArgumentException e) {
				// Not tracking this event type
			}
		}

		@EventHandler(priority = EventPriority.HIGH)
		public void onGMSystemEvent(GMSystemEvent event) {

			try {
				if (PermissionEventEnums.GMSystem_Action.valueOf(event.getAction().name()) != null) {
					// Update all players.
					for (Player player : BukkitTools.getOnlinePlayers())
						if (player != null)
							updateDefaultResidentModes(player.getName());
				}
			} catch (IllegalArgumentException e) {
				// Not tracking this event type
			}

		}

		private void updateDefaultResidentModes(String name) {
			Resident resident = TownyUniverse.getInstance().getResident(name);
			if (resident == null || !resident.isOnline())
				return;
			ResidentModeHandler.applyDefaultModes(resident, false);
			plugin.resetCache(resident.getPlayer());
		}
	}
	
	protected class PermissionEventEnums {

		// GroupManager Event Enums
		public enum GMUser_Action {
			USER_PERMISSIONS_CHANGED,
			USER_INHERITANCE_CHANGED,
			USER_INFO_CHANGED,
			USER_GROUP_CHANGED,
			USER_SUBGROUP_CHANGED,
			USER_ADDED,
			USER_REMOVED,
		}

		public enum GMGroup_Action {
			GROUP_PERMISSIONS_CHANGED,
			GROUP_INHERITANCE_CHANGED,
			GROUP_INFO_CHANGED,
			GROUP_REMOVED,
		}

		public enum GMSystem_Action {
			RELOADED,
			DEFAULT_GROUP_CHANGED,
		}
	}
}
