package com.palmergames.bukkit.towny.event.internal;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import com.palmergames.bukkit.towny.TownyMessaging;
import com.palmergames.bukkit.towny.event.TownyDestroyEvent;
import com.palmergames.bukkit.towny.object.TownyPermission.ActionType;
import com.palmergames.bukkit.towny.utils.PlayerCacheUtil;
import com.palmergames.bukkit.util.BukkitTools;

public class TownyInternalSwitchPermissionEvent {
	
	private boolean cancelled;

	public TownyInternalSwitchPermissionEvent(Player player, Location loc, Material mat) {
		cancelled = !PlayerCacheUtil.getCachePermission(player, loc, mat, ActionType.SWITCH);
		TownyMessaging.sendDebugMsg("TownyInternalSwitchPermissionEvent - PRE - " + player.getName() +
				" - loc:" + loc + 
				" - mat:" + mat.name() + 
				" - cancelled:" + cancelled);

		TownyDestroyEvent event = new TownyDestroyEvent(player, loc, mat, isCancelled());
		BukkitTools.getPluginManager().callEvent(event);
		cancelled = event.isCancelled();

		TownyMessaging.sendDebugMsg("TownyInternalSwitchPermissionEvent - POST - " + player.getName() +
				" - loc:" + loc + 
				" - mat:" + mat.name() + 
				" - cancelled:" + cancelled);
	}
	
	public boolean isCancelled() {
		return cancelled;
	}
}
