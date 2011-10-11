package com.palmergames.bukkit.towny.permissions;

import org.anjocaido.groupmanager.GroupManager;
import org.anjocaido.groupmanager.permissions.AnjoPermissionsHandler;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import com.palmergames.bukkit.towny.Towny;
import com.palmergames.bukkit.towny.TownySettings;
import com.palmergames.bukkit.towny.object.Resident;



public class GroupManagerSource extends TownyPermissionSource {
	
	public GroupManagerSource(Towny towny, Plugin test) {
		this.groupManager = (GroupManager)test;
		this.plugin = towny;
	}
	
	/** getPermissionNode
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
	
    /** hasPermission
     * 
     * returns if a player has a certain permission node.
     * 
     * @param player
     * @param node
     * @return
     */
    @Override
	public boolean hasPermission(Player player, String node) {
    	AnjoPermissionsHandler handler = groupManager.getWorldsHolder().getWorldData(player).getPermissionsHandler();
		return handler.has(player, node);    		
    }
	
	
}