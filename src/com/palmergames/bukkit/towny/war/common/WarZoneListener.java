package com.palmergames.bukkit.towny.war.common;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

import com.palmergames.bukkit.towny.Towny;
import com.palmergames.bukkit.towny.TownyAPI;
import com.palmergames.bukkit.towny.TownyMessaging;
import com.palmergames.bukkit.towny.event.TownyBuildEvent;
import com.palmergames.bukkit.towny.event.TownyDestroyEvent;
import com.palmergames.bukkit.towny.event.TownyItemuseEvent;
import com.palmergames.bukkit.towny.event.TownySwitchEvent;
import com.palmergames.bukkit.towny.exceptions.TownyException;
import com.palmergames.bukkit.towny.object.Coord;
import com.palmergames.bukkit.towny.object.Translation;
import com.palmergames.bukkit.towny.object.WorldCoord;
import com.palmergames.bukkit.towny.object.PlayerCache.TownBlockStatus;
import com.palmergames.bukkit.towny.war.eventwar.WarUtil;
import com.palmergames.bukkit.towny.war.flagwar.FlagWar;
import com.palmergames.bukkit.towny.war.flagwar.FlagWarConfig;

public class WarZoneListener implements Listener {
	
	private final Towny plugin;
	
	public WarZoneListener(Towny instance) {

		plugin = instance;
	}
	
	@EventHandler
	public void onDestroy(TownyDestroyEvent event) {
		
		Player player = event.getPlayer();
		Material mat = event.getMaterial();
		TownBlockStatus status = plugin.getCache(player).getStatus();

		if ((status == TownBlockStatus.WARZONE && FlagWarConfig.isAllowingAttacks()) // Flag War
				|| (TownyAPI.getInstance().isWarTime() && status == TownBlockStatus.WARZONE && !WarUtil.isPlayerNeutral(player))) { // Event War
			if (!WarZoneConfig.isEditableMaterialInWarZone(mat)) {
				event.setCancelled(true);
				TownyMessaging.sendErrorMsg(player, Translation.of("msg_err_warzone_cannot_edit_material", "destroy", mat.toString().toLowerCase()));
				return;
			}
			event.setCancelled(false);
		}
	}
	
	@EventHandler
	public void onBuild(TownyBuildEvent event) {
		
		Player player = event.getPlayer();
		Material mat = event.getMaterial();
		TownBlockStatus status = plugin.getCache(player).getStatus();
		
		if ((status == TownBlockStatus.WARZONE && FlagWarConfig.isAllowingAttacks()) // Flag War 
				|| (TownyAPI.getInstance().isWarTime() && status == TownBlockStatus.WARZONE && !WarUtil.isPlayerNeutral(player))) { // Event War
			if (!WarZoneConfig.isEditableMaterialInWarZone(mat)) {
				event.setCancelled(true);
				TownyMessaging.sendErrorMsg(player, Translation.of("msg_err_warzone_cannot_edit_material", "build", mat.toString().toLowerCase()));
				return;
			}
			event.setCancelled(false);
		}
	}
	
	@EventHandler
	public void onItemUse(TownyItemuseEvent event) {
		Player player = event.getPlayer();
		TownBlockStatus status = plugin.getCache(event.getPlayer()).getStatus();
		
		// Allow item_use for Event War if isAllowingItemUseInWarZone is true, FlagWar also handled here
		if ((status == TownBlockStatus.WARZONE && FlagWarConfig.isAllowingAttacks()) // Flag War
				|| (TownyAPI.getInstance().isWarTime() && status == TownBlockStatus.WARZONE && !WarUtil.isPlayerNeutral(player))) { // Event War
			if (!WarZoneConfig.isAllowingItemUseInWarZone()) {				
				event.setCancelled(true);
				TownyMessaging.sendErrorMsg(player, Translation.of("msg_err_warzone_cannot_use_item"));
				return;
			}
			event.setCancelled(false);
		}
	}
	
	@EventHandler (ignoreCancelled= true)
	public void onSwitchUse(TownySwitchEvent event) {
		Player player = event.getPlayer();
		TownBlockStatus status = plugin.getCache(player).getStatus();

		if ((status == TownBlockStatus.WARZONE && FlagWarConfig.isAllowingAttacks()) // Flag War
				|| (TownyAPI.getInstance().isWarTime() && status == TownBlockStatus.WARZONE && !WarUtil.isPlayerNeutral(player))) { // Event War
			if (!WarZoneConfig.isAllowingSwitchesInWarZone()) {
				event.setCancelled(true);
				TownyMessaging.sendErrorMsg(player, Translation.of("msg_err_warzone_cannot_use_switches"));
				return;
			}
			event.setCancelled(false);
		}
	}

	@EventHandler (priority=EventPriority.LOWEST, ignoreCancelled = true)
	public void onFlagWarFlagPlace(TownyBuildEvent event) {
		Player player = event.getPlayer();
		Material mat = event.getMaterial();
		Block block = player.getWorld().getBlockAt(event.getLocation());
		WorldCoord worldCoord = new WorldCoord(block.getWorld().getName(), Coord.parseCoord(block));
		
		if (((plugin.getCache(player).getStatus() == TownBlockStatus.ENEMY) && FlagWarConfig.isAllowingAttacks()) && (mat == FlagWarConfig.getFlagBaseMaterial())) 
			try {
				if (FlagWar.callAttackCellEvent(plugin, player, block, worldCoord))
					event.setCancelled(false);
			} catch (TownyException e) {
				TownyMessaging.sendErrorMsg(player, e.getMessage());
			}
	}
}
