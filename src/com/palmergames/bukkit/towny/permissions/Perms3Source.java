package com.palmergames.bukkit.towny.permissions;

import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import com.nijiko.permissions.PermissionHandler;
import com.nijikokun.bukkit.Permissions.Permissions;
import com.palmergames.bukkit.towny.Towny;
import com.palmergames.bukkit.towny.TownySettings;
import com.palmergames.bukkit.towny.object.Resident;


public class Perms3Source extends TownyPermissionSource {
	
	public Perms3Source(Towny towny, Plugin test) {
		this.permissions = (Permissions)test;
		this.plugin = towny;
	}
	
	/** getPermissionNode
     * 
     * returns the specified prefix/suffix nodes from permissions
     * 
     * @param resident
     * @param node
     * @return
     */
    @Override
    // Suppression is to clear warnings while retaining permissions 2.7 compatibility
	public String getPrefixSuffix(Resident resident, String node) {
    	
    	String group = "", user = ""; 
        Player player = plugin.getServer().getPlayer(resident.getName());
        
      //sendDebugMsg("    Permissions installed.");
        PermissionHandler handler = permissions.getHandler();
        
        if (node == "prefix") {
                group = handler.getGroupPrefix(player.getWorld().getName(), handler.getGroup(player.getWorld().getName(), player.getName()));
                //user =  handler.getUserPrefix(player.getWorld().getName(), player.getName());
        } else if (node == "suffix") {
                group = handler.getGroupSuffix(player.getWorld().getName(), handler.getGroup(player.getWorld().getName(), player.getName()));
                //user =  handler.getUserSuffix(player.getWorld().getName(), player.getName());
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
    // Suppression is to clear warnings while retaining permissions 2.7 compatibility
    public int getGroupPermissionIntNode(String playerName, String node) {
    	Player player = plugin.getServer().getPlayer(playerName);
		String worldName = player.getWorld().getName();
		String groupName;
		
		try {
			PermissionHandler handler = permissions.getHandler();
    		groupName = handler.getGroup(worldName, playerName);
    		
    		return handler.getGroupPermissionInteger(worldName, groupName, node);
		} catch (Exception e) {
			// Ignore UnsupportedOperationException on certain Permission APIs
		}
    	
    	return -1;
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
    	PermissionHandler handler = permissions.getHandler();
        return handler.permission(player, node);
    }
	
	
    /**
     * Returns the players Group name.
     * 
     * @param player
     * @return
     */
    @Override
	public String getPlayerGroup(Player player) {

    	PermissionHandler handler = permissions.getHandler();
    	return handler.getGroup(player.getWorld().getName(), player.getName());
		
    }
	
}