package com.palmergames.bukkit.towny.permissions;

import org.bukkit.entity.Player;

import com.palmergames.bukkit.towny.Towny;
import com.palmergames.bukkit.towny.object.Resident;



public class NullPermSource extends TownyPermissionSource {
	
	public NullPermSource(Towny towny) {
		this.plugin = towny;
	}
	
	@Override
	public String getPrefixSuffix(Resident resident, String node) {
		// using no permissions provider
		return "";
	}
	
	/**
     * 
     * @param playerName
     * @param node
     * @return -1 = can't find
     */
    @Override
	public int getGroupPermissionIntNode(String playerName, String node) {
    	// // using no permissions provider
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
		// using no permissions provider
    	return false;
    }
    
    /**
     * Returns the players Group name.
     * 
     * @param player
     * @return
     */
    @Override
	public String getPlayerGroup(Player player) {
    	// using no permissions provider
    	return "";	
    }
	
}