package com.palmergames.bukkit.towny.war.eventwar.listeners;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import com.palmergames.bukkit.towny.Towny;
import com.palmergames.bukkit.towny.TownyAPI;
import com.palmergames.bukkit.towny.event.actions.TownyBuildEvent;
import com.palmergames.bukkit.towny.event.actions.TownyBurnEvent;
import com.palmergames.bukkit.towny.event.actions.TownyDestroyEvent;
import com.palmergames.bukkit.towny.event.actions.TownyExplodingBlocksEvent;
import com.palmergames.bukkit.towny.event.actions.TownyItemuseEvent;
import com.palmergames.bukkit.towny.event.actions.TownySwitchEvent;
import com.palmergames.bukkit.towny.event.damage.TownyExplosionDamagesEntityEvent;
import com.palmergames.bukkit.towny.object.Translatable;
import com.palmergames.bukkit.towny.object.PlayerCache.TownBlockStatus;
import com.palmergames.bukkit.towny.regen.TownyRegenAPI;
import com.palmergames.bukkit.towny.war.eventwar.WarUtil;
import com.palmergames.bukkit.towny.war.eventwar.settings.EventWarSettings;

public class EventWarTownyActionListener implements Listener {

	private final Towny plugin;
	public EventWarTownyActionListener(Towny plugin) {
		this.plugin = plugin;
	}
	
	@EventHandler
	public void onDestroy(TownyDestroyEvent event) {
		if (!TownyAPI.getInstance().isWarTime() || event.isInWilderness())
			return;
		Player player = event.getPlayer();
		if (!plugin.hasCache(player))
			plugin.newCache(player);
		TownBlockStatus status = plugin.getCache(player).getStatus();

		// Allow build for Event War if material is an EditableMaterial
		if (status == TownBlockStatus.WARZONE && !WarUtil.isPlayerNeutral(player)) {
			Material mat = event.getMaterial();
			if (!EventWarSettings.isEditableMaterialInWarZone(mat)) {
				event.setCancelled(true);
				event.setMessage(Translatable.of("msg_err_warzone_cannot_edit_material", "destroy", mat.toString().toLowerCase()).forLocale(player));
				return;
			}
			event.setCancelled(false);
		}
	}
	
	@EventHandler
	public void onBuild(TownyBuildEvent event) {
		if (!TownyAPI.getInstance().isWarTime() || event.isInWilderness())
			return;
		Player player = event.getPlayer();
		if (!plugin.hasCache(player))
			plugin.newCache(player);
		TownBlockStatus status = plugin.getCache(player).getStatus();

		// Allow destroy for Event War if material is an EditableMaterial
		if (status == TownBlockStatus.WARZONE && !WarUtil.isPlayerNeutral(player)) {
			Material mat = event.getMaterial();
			if (!EventWarSettings.isEditableMaterialInWarZone(mat)) {
				event.setCancelled(true);
				event.setMessage(Translatable.of("msg_err_warzone_cannot_edit_material", "build", mat.toString().toLowerCase()).forLocale(player));
				return;
			}
			event.setCancelled(false);
		}
	}
	
	@EventHandler
	public void onItemUse(TownyItemuseEvent event) {
		if (!TownyAPI.getInstance().isWarTime() || event.isInWilderness())
			return;
		Player player = event.getPlayer();
		if (!plugin.hasCache(player))
			plugin.newCache(player);
		TownBlockStatus status = plugin.getCache(player).getStatus();

		// Allow ItemUse for Event War if configured.
		if (status == TownBlockStatus.WARZONE && !WarUtil.isPlayerNeutral(player)) {
			if (!EventWarSettings.isAllowingItemUseInWarZone()) {				
				event.setCancelled(true);
				event.setMessage(Translatable.of("msg_err_warzone_cannot_use_item").forLocale(player));
				return;
			}
			event.setCancelled(false);
		}
	}
	
	@EventHandler
	public void onSwitchUse(TownySwitchEvent event) {
		if (!TownyAPI.getInstance().isWarTime() || event.isInWilderness())
			return;
		Player player = event.getPlayer();
		if (!plugin.hasCache(player))
			plugin.newCache(player);
		TownBlockStatus status = plugin.getCache(player).getStatus();

		// Allow Switch for Event War if configured.
		if (status == TownBlockStatus.WARZONE && !WarUtil.isPlayerNeutral(player)) {
			if (!EventWarSettings.isAllowingSwitchesInWarZone()) {
				event.setCancelled(true);
				event.setMessage(Translatable.of("msg_err_warzone_cannot_use_switches").forLocale(player));
				return;
			}
			event.setCancelled(false);
		}
	}
	
	@EventHandler
	public void onExplosionDamagingBlocks(TownyExplodingBlocksEvent event) {
		if (!TownyAPI.getInstance().isWarTime())
			return;

		List<Block> alreadyAllowed = event.getTownyFilteredBlockList();
		List<Block> toAllow = new ArrayList<Block>();
		
		int count = 0;
		for (Block block : event.getVanillaBlockList()) {
			// Wilderness, skip it.
			if (TownyAPI.getInstance().isWilderness(block))
				continue;
			
			// Non-warzone, skip it.
			if (!TownyAPI.getInstance().getTownBlock(block.getLocation()).isWarZone())
				continue;
			
			// A war that doesn't allow any kind of explosions.
			if (!EventWarSettings.isAllowingExplosionsInWarZone()) {
				// Remove from the alreadyAllowed list if it exists there.
				if (alreadyAllowed.contains(block))
					alreadyAllowed.remove(block);
				continue;
			}

			// A war that does allow explosions and explosions regenerate.
			if (EventWarSettings.regenBlocksAfterExplosionInWarZone()) {
				// Skip this block if it is in the ignore list. TODO: with the blockdata nowadays this might not even be necessary.
				if (EventWarSettings.getExplosionsIgnoreList().contains(block.getType().name()) || EventWarSettings.getExplosionsIgnoreList().contains(block.getRelative(BlockFace.UP).getType().name())) {
					// Remove from the alreadyAllowed list if it exists there.
					if (alreadyAllowed.contains(block))
						alreadyAllowed.remove(block);
					continue;
				}
				count++;
				TownyRegenAPI.beginProtectionRegenTask(block, count, TownyAPI.getInstance().getTownyWorld(block.getLocation().getWorld().getName()), event);
			}
			// This is an allowed explosion, so add it to our War-allowed list.
			toAllow.add(block);
		}
		// Add all TownyFilteredBlocks to our list, since our list will be used.
		toAllow.addAll(alreadyAllowed);
		
		// Return the list of allowed blocks for this block explosion event.
		event.setBlockList(toAllow);
	}

	@EventHandler
	public void onExplosionDamagingEntity(TownyExplosionDamagesEntityEvent event) {
		if (!TownyAPI.getInstance().isWarTime())
			return;
		
		/*
		 * Handle occasions in the wilderness first.
		 */
		if (event.isInWilderness())
			return;

		/*
		 * Must be inside of a town.
		 */
		
		// Not in a war zone, do not modify the outcome of the event.
		if (!event.getTownBlock().isWarZone())
			return;
			
		/*
		 * Stops any type of exploding damage if wars are not allowing explosions.
		 */
		if (!EventWarSettings.isAllowingExplosionsInWarZone()) {
			event.setCancelled(true);
			return;
		}
		
		/*
		 * Stops explosions from damaging entities protected from explosions during war.
		 */
		if (EventWarSettings.getExplosionsIgnoreList().contains(event.getEntity().getType().name())) {
			event.setCancelled(true);
			return;
		}

		/*
		 * Explosions must be allowed, so un-cancel the event.
		 */
		event.setCancelled(false);
	}
	
	@EventHandler
	public void onBurn(TownyBurnEvent event) {
		/*
		 * Return early if this is in the wild.
		 */
		if (event.isInWilderness())
			return;

		/*
		 * Event War fire control settings.
		 */
		if (TownyAPI.getInstance().isWarTime() && event.getTownBlock().isWarZone()) {
			if (EventWarSettings.isAllowingFireInWarZone() || EventWarSettings.isAllowWarBlockGriefing()) {
				event.setCancelled(false);
			} else {
				event.setCancelled(true);
			}
		}
	}
	
}
