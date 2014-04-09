package com.palmergames.bukkit.towny.listeners;

import org.bukkit.Material;
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

				if (event.getVehicle() instanceof org.bukkit.entity.minecart.StorageMinecart) {

					blockID = 342;

				} else if (event.getVehicle() instanceof org.bukkit.entity.minecart.RideableMinecart) {

					blockID = 328;

				} else if (event.getVehicle() instanceof org.bukkit.entity.minecart.PoweredMinecart) {

					blockID = 343;

				} else if (event.getVehicle() instanceof org.bukkit.entity.minecart.HopperMinecart) {

					blockID = 408;

				} else {

					blockID = 321;
				}

				if ((blockID != 0) && (!TownySettings.isItemUseMaterial(Material.getMaterial(blockID).name())))
					return;

				// Get permissions (updates if none exist)
				bBreak = PlayerCacheUtil.getCachePermission(player, event.getVehicle().getLocation(), blockID, (byte) 0, TownyPermission.ActionType.ITEM_USE);
				break;

			}

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
