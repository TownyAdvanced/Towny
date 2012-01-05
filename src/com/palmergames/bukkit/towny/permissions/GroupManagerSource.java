package com.palmergames.bukkit.towny.permissions;

import org.anjocaido.groupmanager.GroupManager;
import org.anjocaido.groupmanager.data.Group;
import org.anjocaido.groupmanager.events.GMGroupEvent;
import org.anjocaido.groupmanager.events.GMSystemEvent;
import org.anjocaido.groupmanager.events.GMUserEvent;
import org.anjocaido.groupmanager.permissions.AnjoPermissionsHandler;
import org.bukkit.entity.Player;
import org.bukkit.event.CustomEventListener;
import org.bukkit.event.Event;
import org.bukkit.event.Event.Priority;
import org.bukkit.plugin.Plugin;

import com.palmergames.bukkit.towny.NotRegisteredException;
import com.palmergames.bukkit.towny.Towny;
import com.palmergames.bukkit.towny.TownySettings;
import com.palmergames.bukkit.towny.object.Resident;

/**
 * @author ElgarL
 * 
 */
public class GroupManagerSource extends TownyPermissionSource {

	public GroupManagerSource(Towny towny, Plugin test) {
		this.groupManager = (GroupManager) test;
		this.plugin = towny;

		plugin.getServer().getPluginManager().registerEvent(Event.Type.CUSTOM_EVENT, new GMCustomEventListener(), Priority.High, plugin);
	}

	/**
	 * getPermissionNode
	 * 
	 * returns the specified prefix/suffix nodes from GroupManager
	 * 
	 * @param resident
	 * @param node
	 * @return
	 */
	@Override
	public String getPrefixSuffix(Resident resident, String node) {

		String group = "", user = "";
		Player player = this.plugin.getServer().getPlayer(resident.getName());

		//sendDebugMsg("    GroupManager installed.");
		AnjoPermissionsHandler handler = groupManager.getWorldsHolder().getWorldData(player).getPermissionsHandler();

		if (node == "prefix") {
			group = handler.getGroupPrefix(handler.getPrimaryGroup(player.getName()));
			user = handler.getUserPrefix(player.getName());
		} else if (node == "suffix") {
			group = handler.getGroupSuffix(handler.getPrimaryGroup(player.getName()));
			user = handler.getUserSuffix(player.getName());
		}

		if (!group.equals(user))
			user = group + user;
		user = TownySettings.parseSingleLineString(user);

		return user;

	}

	/**
	 * 
	 * @param playerName
	 * @param node
	 * @return -1 = can't find
	 */
	@Override
	public int getGroupPermissionIntNode(String playerName, String node) {
		Player player = plugin.getServer().getPlayer(playerName);

		AnjoPermissionsHandler handler = groupManager.getWorldsHolder().getWorldData(player).getPermissionsHandler();
		return handler.getPermissionInteger(playerName, node);

	}

	/**
	 * 
	 * @param playerName
	 * @param node
	 * @return empty = can't find
	 */
	@Override
	public String getPlayerPermissionStringNode(String playerName, String node) {
		Player player = plugin.getServer().getPlayer(playerName);

		AnjoPermissionsHandler handler = groupManager.getWorldsHolder().getWorldData(player).getPermissionsHandler();

		return handler.getPermissionString(playerName, node);

	}

	/**
	 * hasPermission
	 * 
	 * returns if a player has a certain permission node.
	 * 
	 * @param player
	 * @param node
	 * @return
	 */
	@Override
	public boolean hasPermission(Player player, String node) {
		
		if (player.isOp())
    		return true;
		
		AnjoPermissionsHandler handler = groupManager.getWorldsHolder().getWorldData(player).getPermissionsHandler();
		return handler.has(player, node);
	}

	/**
	 * Returns the players Group name.
	 * 
	 * @param player
	 * @return
	 */
	@Override
	public String getPlayerGroup(Player player) {
		AnjoPermissionsHandler handler = groupManager.getWorldsHolder().getWorldData(player).getPermissionsHandler();
		return handler.getGroup(player.getName());
	}

	protected class GMCustomEventListener extends CustomEventListener {

		public GMCustomEventListener() {
		}

		@Override
		public void onCustomEvent(Event event) {

			Resident resident = null;
			Player player = null;

			try {
				if (event instanceof GMUserEvent) {
					
					if (PermissionEventEnums.GMUser_Action.valueOf(event.getEventName()) != null) {
						GMUserEvent UserEvent = (GMUserEvent) event;
						try {
							resident = plugin.getTownyUniverse().getResident(UserEvent.getUserName());
							player = plugin.getServer().getPlayerExact(resident.getName());
							if (player != null) {
								//setup default modes for this player.
								String[] modes = getPlayerPermissionStringNode(player.getName(), PermissionNodes.TOWNY_DEFAULT_MODES.getNode()).split(",");
								plugin.setPlayerMode(player, modes, false);
							}
						} catch (NotRegisteredException x) {
						}
	
					}
				} else if (event instanceof GMGroupEvent) {
					if (PermissionEventEnums.GMGroup_Action.valueOf(event.getEventName()) != null) {
						GMGroupEvent GroupEvent = (GMGroupEvent) event;
						Group group = GroupEvent.getGroup();
						// Update all players who are in this group.
						for (Player toUpdate : plugin.getTownyUniverse().getOnlinePlayers()) {
							if (group.equals(getPlayerGroup(toUpdate))) {
								//setup default modes
								String[] modes = getPlayerPermissionStringNode(toUpdate.getName(), PermissionNodes.TOWNY_DEFAULT_MODES.getNode()).split(",");
								plugin.setPlayerMode(player, modes, false);
							}
						}
	
					}
	
				} else if (event instanceof GMSystemEvent) {
					if (PermissionEventEnums.GMGroup_Action.valueOf(event.getEventName()) != null) {
						// Update all players.
						for (Player toUpdate : plugin.getTownyUniverse().getOnlinePlayers()) {
							//setup default modes
							String[] modes = getPlayerPermissionStringNode(toUpdate.getName(), PermissionNodes.TOWNY_DEFAULT_MODES.getNode()).split(",");
							plugin.setPlayerMode(player, modes, false);
						}
	
					}
	
				}
			} catch (IllegalArgumentException ex) {
				// We are not looking for this event type.
			}

		}
	}
}