package com.palmergames.bukkit.towny.war.siegewar.listeners;

import java.util.ArrayList;
import java.util.List;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import com.palmergames.bukkit.towny.Towny;
import com.palmergames.bukkit.towny.event.actions.TownyBuildEvent;
import com.palmergames.bukkit.towny.event.actions.TownyBurnEvent;
import com.palmergames.bukkit.towny.event.actions.TownyDestroyEvent;
import com.palmergames.bukkit.towny.event.actions.TownyExplodingBlocksEvent;
import com.palmergames.bukkit.towny.event.player.PlayerKilledPlayerEvent;
import com.palmergames.bukkit.towny.object.Translation;
import com.palmergames.bukkit.towny.war.siegewar.SiegeWarDeathController;
import com.palmergames.bukkit.towny.war.siegewar.SiegeWarSettings;
import com.palmergames.bukkit.towny.war.siegewar.utils.SiegeWarBlockUtil;
import com.palmergames.bukkit.towny.war.siegewar.utils.SiegeWarDistanceUtil;

public class SiegeWarActionListener implements Listener {

	@SuppressWarnings("unused")
	private final Towny plugin;
	
	public SiegeWarActionListener(Towny instance) {

		plugin = instance;
	}
	
	/*
	 * SW will prevent an block break from altering an area around a banner.
	 */
	@EventHandler
	public void onBlockBreak(TownyDestroyEvent event) {
		if (SiegeWarSettings.getWarSiegeEnabled() && SiegeWarBlockUtil.isBlockNearAnActiveSiegeBanner(event.getBlock())) {
			event.setMessage(Translation.of("msg_err_siege_war_cannot_destroy_siege_banner"));
			event.setCancelled(true);
		}
	}
	
	/*
	 * SW will prevent fire from altering an area around a banner.
	 */
	@EventHandler
	public void onBurn(TownyBurnEvent event) {
		if (SiegeWarSettings.getWarSiegeEnabled() && SiegeWarBlockUtil.isBlockNearAnActiveSiegeBanner(event.getBlock())) {	
			event.setCancelled(true);	
		}
	}
	
	/*
	 * SW will prevent an explosion from altering an area around a banner.
	 */
	@EventHandler
	public void onBlockExplode(TownyExplodingBlocksEvent event) {
		if (SiegeWarSettings.getWarSiegeEnabled()) {
			List<Block> blockList = event.getBlockList();
			List<Block> filteredList = new ArrayList<>();
			for (Block block : blockList) {
				if (!SiegeWarBlockUtil.isBlockNearAnActiveSiegeBanner(block)) {
					filteredList.add(block);
				}
			}
			event.setBlockList(filteredList);
		}
	}
	
	/*
	 * SW can affect the emptying of buckets, which could affect a banner.
	 */
	@EventHandler
	public void onBucketUse(TownyBuildEvent event) {
		if(SiegeWarSettings.getWarSiegeEnabled() && event.isInWilderness() && SiegeWarSettings.isWarSiegeZoneBucketEmptyingRestrictionsEnabled() && SiegeWarSettings.getWarSiegeZoneBucketEmptyingRestrictionsMaterials().contains(event.getMaterial())) {
			if (SiegeWarDistanceUtil.isLocationInActiveSiegeZone(event.getLocation())) {
				event.setMessage(Translation.of("msg_war_siege_zone_bucket_emptying_forbidden"));
				event.setCancelled(true);
			}
		}
	}
	
	/*
	 * SW can affect whether an inventory is dropped and also can degrade an inventory.
	 */
	@EventHandler
	public void onPlayerKillsPlayer(PlayerKilledPlayerEvent event) {
		//Check for siege-war related death effects
		if(SiegeWarSettings.getWarSiegeEnabled()) {
			/*
			 * TODO: Evaluate if we're doing something bad in the SiegeWarDeathController
			 * by moving to an PlayerKilledPlayerEvent which is fired from a MONITOR 
			 * priority PlayerDeathEvent.
			 */
			SiegeWarDeathController.evaluateSiegePlayerDeath(event.getVictim(), event.getPlayerDeathEvent());
		}
	}

}
