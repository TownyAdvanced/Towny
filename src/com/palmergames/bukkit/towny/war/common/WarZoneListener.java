package com.palmergames.bukkit.towny.war.common;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

import com.palmergames.bukkit.towny.Towny;
import com.palmergames.bukkit.towny.TownyAPI;
import com.palmergames.bukkit.towny.event.actions.TownyBuildEvent;
import com.palmergames.bukkit.towny.event.actions.TownyDestroyEvent;
import com.palmergames.bukkit.towny.event.actions.TownyExplodeEvent;
import com.palmergames.bukkit.towny.event.actions.TownyItemuseEvent;
import com.palmergames.bukkit.towny.event.actions.TownySwitchEvent;
import com.palmergames.bukkit.towny.exceptions.TownyException;
import com.palmergames.bukkit.towny.object.Coord;
import com.palmergames.bukkit.towny.object.Translation;
import com.palmergames.bukkit.towny.object.WorldCoord;
import com.palmergames.bukkit.towny.object.PlayerCache.TownBlockStatus;
import com.palmergames.bukkit.towny.war.eventwar.War;
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

		// Allow destroy for Event War if material is an EditableMaterial, FlagWar also handled here
		if ((status == TownBlockStatus.WARZONE && FlagWarConfig.isAllowingAttacks()) // Flag War
				|| (TownyAPI.getInstance().isWarTime() && status == TownBlockStatus.WARZONE && !WarUtil.isPlayerNeutral(player))) { // Event War
			if (!WarZoneConfig.isEditableMaterialInWarZone(mat)) {
				event.setCancelled(true);
				event.setMessage(Translation.of("msg_err_warzone_cannot_edit_material", "destroy", mat.toString().toLowerCase()));
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
		
		// Allow build for Event War if material is an EditableMaterial, FlagWar also handled here
		if ((status == TownBlockStatus.WARZONE && FlagWarConfig.isAllowingAttacks()) // Flag War 
				|| (TownyAPI.getInstance().isWarTime() && status == TownBlockStatus.WARZONE && !WarUtil.isPlayerNeutral(player))) { // Event War
			if (!WarZoneConfig.isEditableMaterialInWarZone(mat)) {
				event.setCancelled(true);
				event.setMessage(Translation.of("msg_err_warzone_cannot_edit_material", "build", mat.toString().toLowerCase()));
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
				event.setMessage(Translation.of("msg_err_warzone_cannot_use_item"));
				return;
			}
			event.setCancelled(false);
		}
	}
	
	@EventHandler
	public void onSwitchUse(TownySwitchEvent event) {
		Player player = event.getPlayer();
		TownBlockStatus status = plugin.getCache(player).getStatus();

		// Allow switch for Event War if isAllowingSwitchesInWarZone is true, FlagWar also handled here
		if ((status == TownBlockStatus.WARZONE && FlagWarConfig.isAllowingAttacks()) // Flag War
				|| (TownyAPI.getInstance().isWarTime() && status == TownBlockStatus.WARZONE && !WarUtil.isPlayerNeutral(player))) { // Event War
			if (!WarZoneConfig.isAllowingSwitchesInWarZone()) {
				event.setCancelled(true);
				event.setMessage(Translation.of("msg_err_warzone_cannot_use_switches"));
				return;
			}
			event.setCancelled(false);
		}
	}
	
	@EventHandler
	public void onExplosion(TownyExplodeEvent event) {
		if (!TownyAPI.getInstance().isWarTime())
			return;
		
		/*
		 * Handle occasions in the wilderness first.
		 */
		if (TownyAPI.getInstance().isWilderness(event.getLocation()))
			return;

		/*
		 * Must be inside of a town.
		 */
		
		// Not in a war zone, do not modify the outcome of the event.
		if (!War.isWarZone(TownyAPI.getInstance().getTownBlock(event.getLocation()).getWorldCoord()))
			return;
			
		/*
		 * Stops any type of exploding damage and block damage if wars are not allowing explosions.
		 */
		if (!WarZoneConfig.isAllowingExplosionsInWarZone()) {
			event.setCancelled(true);
			return;
		}

		/*
		 * TODO: Separate event into EntityDamage and BlockDamage, so explosions can still harm entities but not cause block damage. 
		 */
		if (WarZoneConfig.explosionsBreakBlocksInWarZone())
			event.setCancelled(false);				
	}

	@EventHandler (priority=EventPriority.HIGH)
	public void onFlagWarFlagPlace(TownyBuildEvent event) {
		if (!(FlagWarConfig.isAllowingAttacks() && event.getMaterial() == FlagWarConfig.getFlagBaseMaterial()))
			return;
		Player player = event.getPlayer();
		Block block = player.getWorld().getBlockAt(event.getLocation());
		WorldCoord worldCoord = new WorldCoord(block.getWorld().getName(), Coord.parseCoord(block));
		
		if (plugin.getCache(player).getStatus() == TownBlockStatus.ENEMY) 
			try {
				if (FlagWar.callAttackCellEvent(plugin, player, block, worldCoord))
					event.setCancelled(false);
			} catch (TownyException e) {
				event.setMessage(e.getMessage());
			}
	}
	
	
}
