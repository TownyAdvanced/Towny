package com.palmergames.bukkit.towny.permissions;

import org.anjocaido.groupmanager.GroupManager;
import org.bukkit.entity.Player;

import ru.tehkode.permissions.bukkit.PermissionsEx;

import com.palmergames.bukkit.towny.Towny;
import com.palmergames.bukkit.towny.TownySettings;
import com.palmergames.bukkit.towny.object.Resident;


/**
 * @author ElgarL
 * 
 * Manager for Permission provider plugins
 *
 */
public abstract class TownyPermissionSource {
	protected TownySettings settings;
	protected Towny plugin;
	
	protected GroupManager groupManager = null;
	protected de.bananaco.permissions.Permissions bPermissions = null;
	protected com.nijikokun.bukkit.Permissions.Permissions permissions = null;
	protected PermissionsEx pex = null;


	abstract public String getPrefixSuffix(Resident resident, String node);
	abstract public int getGroupPermissionIntNode(String playerName, String node);
	abstract public boolean hasPermission(Player player, String node);
	
	
}