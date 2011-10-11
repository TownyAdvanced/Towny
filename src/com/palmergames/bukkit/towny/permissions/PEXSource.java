package com.palmergames.bukkit.towny.permissions;


import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import ru.tehkode.permissions.PermissionManager;
import ru.tehkode.permissions.bukkit.PermissionsEx;

import com.palmergames.bukkit.towny.Towny;
import com.palmergames.bukkit.towny.TownySettings;
import com.palmergames.bukkit.towny.object.Resident;


public class PEXSource extends TownyPermissionSource {
	
	public PEXSource(Towny towny, Plugin test) {
		this.pex = (PermissionsEx)test;
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
        
        PermissionManager pexPM = PermissionsEx.getPermissionManager();
        
        if (node == "prefix") {
        	group = pexPM.getUser(player).getPrefix();
        	user = pexPM.getUser(player).getOwnPrefix();
        } else if (node == "suffix") {
        	group = pexPM.getUser(player).getSuffix();
        	user = pexPM.getUser(player).getOwnSuffix();
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
		String worldName = player.getWorld().getName();
		
		PermissionManager pexPM = PermissionsEx.getPermissionManager();
		
		//return pexPM.getUser(player).getOptionInteger(node, worldName, -1);
		
		String result = pexPM.getUser(player).getOption(node, worldName);
		
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
        
        return pex.has(player, node);
    }
	
	
	
	
}