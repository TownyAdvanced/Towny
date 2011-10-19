package com.palmergames.bukkit.towny.permissions;


import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import de.bananaco.permissions.Permissions;
import de.bananaco.permissions.info.InfoReader;
import de.bananaco.permissions.interfaces.PermissionSet;

import com.palmergames.bukkit.towny.Towny;
import com.palmergames.bukkit.towny.TownySettings;
import com.palmergames.bukkit.towny.object.Resident;


public class bPermsSource extends TownyPermissionSource {
	
	public bPermsSource(Towny towny, Plugin test) {
		this.bPermissions = (Permissions)test;
		this.plugin = towny;
	}
	
	/** getPermissionNode
     * 
     * returns the specified prefix/suffix nodes from permissionsEX
     * 
     * @param resident
     * @param node
     * @return
     */
    @Override
	public String getPrefixSuffix(Resident resident, String node) {
    	
    	String group = "", user = ""; 
        Player player = plugin.getServer().getPlayer(resident.getName());
        
        //PermissionSet bPermPM = Permissions.getWorldPermissionsManager().getPermissionSet(player.getWorld());
        InfoReader bPermIR = Permissions.getInfoReader();
        
        if (node == "prefix") {
        	group = bPermIR.getPrefix(player);
        	//user = bPermIR.getPrefix(player);
        } else if (node == "suffix") {
        	group = bPermIR.getSuffix(player);
        	//user = bPermIR.getSuffix(player);
        }
        if (group == null) group = "";
        //if (user == null) user = "";
    	
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
		
		InfoReader bPermIR = Permissions.getInfoReader();
		
		String result = bPermIR.getValue(player, node);
		
		try {
			return Integer.parseInt(result);
		} catch (NumberFormatException e) {
			return -1;
		}    	
    	
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
    	PermissionSet bPermPM = Permissions.getWorldPermissionsManager().getPermissionSet(player.getWorld());
    	
        return bPermPM.has(player, node);
    }
	
    /**
     * Returns the players Group name.
     * 
     * @param player
     * @return
     */
    @Override
	public String getPlayerGroup(Player player) {

    	PermissionSet bPermPM = Permissions.getWorldPermissionsManager().getPermissionSet(player.getWorld());
    	
    	return bPermPM.getGroups(player).get(0);
		
    }
	
	
}