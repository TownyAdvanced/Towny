package com.palmergames.bukkit.towny.permissions;

import com.palmergames.bukkit.towny.Towny;
import com.palmergames.bukkit.towny.TownyUniverse;
import com.palmergames.bukkit.towny.exceptions.NotRegisteredException;
import com.palmergames.bukkit.towny.object.Resident;
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
			System.out.print("Your Version of GroupManager is out of date. Please update.");
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

		String group = "", user = "";
		Player player = BukkitTools.getPlayer(resident.getName());

		//sendDebugMsg("    GroupManager installed.");
		AnjoPermissionsHandler handler = groupManager.getWorldsHolder().getWorldData(player).getPermissionsHandler();

		if (node == "prefix") {
			group = handler.getGroupPrefix(handler.getPrimaryGroup(player.getName()));
			user = handler.getUserPrefix(player.getName());
		} else if (node == "suffix") {
			group = handler.getGroupSuffix(handler.getPrimaryGroup(player.getName()));
			user = handler.getUserSuffix(player.getName());
		} else if (node == "userprefix") {
			group = "";
			user = handler.getUserPrefix(player.getName());					
		} else if (node == "usersuffix") {
			group = "";
			user = handler.getUserSuffix(player.getName());					
		} else if (node == "groupprefix") {
			group = handler.getGroupPrefix(handler.getPrimaryGroup(player.getName()));
			user = "";
		} else if (node == "groupsuffix") {
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
		
		Player player = BukkitTools.getPlayer(playerName);

		AnjoPermissionsHandler handler = groupManager.getWorldsHolder().getWorldData(player).getPermissionsHandler();
		iReturn  = handler.getPermissionInteger(playerName, node);
		
		if (iReturn == -1)
			iReturn = getEffectivePermIntNode(playerName, node);
		
		return iReturn;

	}
	
	@Override
	public int getPlayerPermissionIntNode(String playerName, String node) {
		
		int iReturn = -1;
		
		Player player = BukkitTools.getPlayer(playerName);

		AnjoPermissionsHandler handler = groupManager.getWorldsHolder().getWorldData(player).getPermissionsHandler();
		iReturn  = handler.getPermissionInteger(playerName, node);
		
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

		Player player = BukkitTools.getPlayer(playerName);

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

			Resident resident = null;
			Player player = null;

			try {
				if (PermissionEventEnums.GMUser_Action.valueOf(event.getAction().name()) != null) {

					try {
						resident = TownyUniverse.getInstance().getDataSource().getResident(event.getUserName());
						player = BukkitTools.getPlayerExact(resident.getName());
						if (player != null) {
							//setup default modes for this player.
							String[] modes = getPlayerPermissionStringNode(player.getName(), PermissionNodes.TOWNY_DEFAULT_MODES.getNode()).split(",");
							plugin.setPlayerMode(player, modes, false);
							plugin.resetCache(player);
						}
					} catch (NotRegisteredException ignored) {
					}

				}
			} catch (IllegalArgumentException e) {
				// Not tracking this event type
			}

		}

		@SuppressWarnings("unlikely-arg-type")
		@EventHandler(priority = EventPriority.HIGH)
		public void onGMGroupEvent(GMGroupEvent event) {

			try {
				if (PermissionEventEnums.GMGroup_Action.valueOf(event.getAction().name()) != null) {

					Group group = event.getGroup();
					// Update all players who are in this group.
					for (Player toUpdate : BukkitTools.getOnlinePlayers()) {
						if (toUpdate != null) {
							if (group.equals(getPlayerGroup(toUpdate))) {
								//setup default modes
								String[] modes = getPlayerPermissionStringNode(toUpdate.getName(), PermissionNodes.TOWNY_DEFAULT_MODES.getNode()).split(",");
								plugin.setPlayerMode(toUpdate, modes, false);
								plugin.resetCache(toUpdate);
							}
						}
					}

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
					for (Player toUpdate : BukkitTools.getOnlinePlayers()) {
						if (toUpdate != null) {
							//setup default modes
							String[] modes = getPlayerPermissionStringNode(toUpdate.getName(), PermissionNodes.TOWNY_DEFAULT_MODES.getNode()).split(",");
							plugin.setPlayerMode(toUpdate, modes, false);
							plugin.resetCache(toUpdate);
						}
					}

				}
			} catch (IllegalArgumentException e) {
				// Not tracking this event type
			}

		}

	}
}
