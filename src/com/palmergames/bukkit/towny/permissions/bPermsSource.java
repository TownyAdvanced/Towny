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
		//this.bPermissions = (Permissions)test;
		this.plugin = towny;
	}
	
	/** getPermissionNode
     * 
     * returns the specified prefix/suffix nodes from permissionsEX
     * 
     * @param resident
     * @param node
     * @return String of the prefix/suffix
     */
    @Override
	public String getPrefixSuffix(Resident resident, String node) {
    	
    	String group = "", user = ""; 
        Player player = plugin.getServer().getPlayer(resident.getName());
        
        //PermissionSet bPermPM = Permissions.getWorldPermissionsManager().getPermissionSet(player.getWorld());
        InfoReader bPermIR = Permissions.getInfoReader();
        
        if (node == "prefix") {
        	group = bPermIR.getGroupPrefix(getPlayerGroup(player), player.getWorld().getName());
        	user = bPermIR.getPrefix(player);
        } else if (node == "suffix") {
        	group = bPermIR.getGroupSuffix(getPlayerGroup(player), player.getWorld().getName());
        	user = bPermIR.getSuffix(player);
        }
        if (group == null) group = "";
        if (user == null) user = "";
    	
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
    
    /**
     * 
     * @param playerName
     * @param node
     * @return empty = can't find
     */
    @Override
    public String getPlayerPermissionStringNode(String playerName, String node) {
    	Player player = plugin.getServer().getPlayer(playerName);
		
		InfoReader bPermIR = Permissions.getInfoReader();
		
		String result =  bPermIR.getValue(player, node);
		   	
		if (result == null)
			return "";
		
		return result;
    	
    }
	
    /** hasPermission
     * 
     * returns if a player has a certain permission node.
     * 
     * @param player
     * @param node
     * @return true is Op or has the permission node
     */
    @Override
	public boolean hasPermission(Player player, String node) {
    	//PermissionSet bPermPM = Permissions.getWorldPermissionsManager().getPermissionSet(player.getWorld());
    	
    	if (player.isOp())
    		return true;
    	
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
		return player.hasPermission(node);
        //return player.hasPermission(node);
    }
	
    /**
     * Returns the players Group name.
     * 
     * @param player
     * @return String name of this players group.
     */
    @Override
	public String getPlayerGroup(Player player) {

    	PermissionSet bPermPM = Permissions.getWorldPermissionsManager().getPermissionSet(player.getWorld());
    	
    	return bPermPM.getGroups(player).get(0);
		
    }
	
	
}