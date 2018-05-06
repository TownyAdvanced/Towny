package com.palmergames.bukkit.towny.listeners;

import org.bukkit.entity.Minecart;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.vehicle.VehicleDestroyEvent;

import com.palmergames.bukkit.towny.Towny;
import com.palmergames.bukkit.towny.TownyMessaging;
import com.palmergames.bukkit.towny.TownySettings;
import com.palmergames.bukkit.towny.object.PlayerCache;
import com.palmergames.bukkit.towny.object.TownyPermission;
import com.palmergames.bukkit.towny.utils.PlayerCacheUtil;
import com.palmergames.bukkit.util.BukkitTools;

/**
 * Handle events for all Vehicle related events
 * 
 * @author ElgarL
 * 
 */
public class TownyVehicleListener implements Listener {
	
	private final Towny plugin;

	public TownyVehicleListener(Towny instance) {

		plugin = instance;
	}

	
	
	@EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
	public void onVehicleDestroy(VehicleDestroyEvent event) {

		if (plugin.isError()) {
			event.setCancelled(true);
			return;
		}

		if (event.getAttacker() instanceof Player) {
			
			Player player = (Player) event.getAttacker();
			boolean bBreak = true;
			int blockID = 0;

			switch (event.getVehicle().getType()) {

			case MINECART:
				blockID = 328;
				break;
			
			case MINECART_FURNACE:
				blockID = 343;
				break;
			
			case MINECART_HOPPER:
				blockID = 408;
				break;
				
			case MINECART_CHEST:
				blockID = 342;
				break;
			
			case MINECART_COMMAND:
				blockID = 422;
				break;
			
			case MINECART_TNT:
				blockID = 407;
				break;
				
			default:
				break;

			}
			
			if ((blockID != 0) && (!TownySettings.isItemUseMaterial(BukkitTools.getMaterial(blockID).name())))
				return;

			// Get permissions (updates if none exist)
			bBreak = PlayerCacheUtil.getCachePermission(player, event.getVehicle().getLocation(), blockID, (byte) 0, TownyPermission.ActionType.ITEM_USE);

			if (blockID != 0) {

				// Allow the removal if we are permitted
				if (bBreak)
					return;

				event.setCancelled(true);

				/*
				 * Fetch the players cache
				 */
				PlayerCache cache = plugin.getCache(player);

				if (cache.hasBlockErrMsg())
					TownyMessaging.sendErrorMsg(player, cache.getBlockErrMsg());

				return;
			}
		}

	}

}
