package com.palmergames.bukkit.towny.war.siegewar.listeners;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerItemConsumeEvent;

import com.palmergames.bukkit.towny.Towny;
import com.palmergames.bukkit.towny.TownyMessaging;
import com.palmergames.bukkit.towny.TownySettings;
import com.palmergames.bukkit.towny.TownyUniverse;
import com.palmergames.bukkit.towny.event.NationPreAddTownEvent;
import com.palmergames.bukkit.towny.event.actions.TownyBuildEvent;
import com.palmergames.bukkit.towny.event.actions.TownyBurnEvent;
import com.palmergames.bukkit.towny.event.actions.TownyDestroyEvent;
import com.palmergames.bukkit.towny.event.actions.TownyExplodingBlocksEvent;
import com.palmergames.bukkit.towny.event.player.PlayerKilledPlayerEvent;
import com.palmergames.bukkit.towny.object.Nation;
import com.palmergames.bukkit.towny.object.Translation;
import com.palmergames.bukkit.towny.utils.TownPeacefulnessUtil;
import com.palmergames.bukkit.towny.war.siegewar.SiegeWarDeathController;
import com.palmergames.bukkit.towny.war.siegewar.objects.Siege;
import com.palmergames.bukkit.towny.war.siegewar.utils.SiegeWarBlockUtil;
import com.palmergames.bukkit.towny.war.siegewar.utils.SiegeWarDistanceUtil;

public class SiegeWarActionListener implements Listener {

	private final Towny plugin;
	
	public SiegeWarActionListener(Towny instance) {

		plugin = instance;
	}
	
	@EventHandler
	public void onBlockBreak(TownyDestroyEvent event) {
		if (TownySettings.getWarSiegeEnabled() && SiegeWarBlockUtil.isBlockNearAnActiveSiegeBanner(event.getBlock())) {
			event.setMessage(Translation.of("msg_err_siege_war_cannot_destroy_siege_banner"));
			event.setCancelled(true);
		}
	}
	
	@EventHandler
	public void onBurn(TownyBurnEvent event) {
		if (TownySettings.getWarSiegeEnabled() && SiegeWarBlockUtil.isBlockNearAnActiveSiegeBanner(event.getBlock())) {	
			event.setCancelled(true);	
		}
	}
	
	@EventHandler
	public void onBlockExplode(TownyExplodingBlocksEvent event) {
		if (TownySettings.getWarSiegeEnabled()) {
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
	
	@EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
	public void onPlayerConsume(PlayerItemConsumeEvent event) {
		if (plugin.isError()) {
			event.setCancelled(true);
			return;
		}

		if(TownySettings.getWarSiegeEnabled()) {
			try {
				//Prevent milk bucket usage while attempting to gain banner control
				if(event.getItem().getType() == Material.MILK_BUCKET) {
					for(Siege siege: TownyUniverse.getInstance().getDataSource().getSieges()) {
						if(siege.getBannerControlSessions().containsKey(event.getPlayer())) {
							event.setCancelled(true);
							TownyMessaging.sendErrorMsg(event.getPlayer(), Translation.of("msg_war_siege_zone_milk_bucket_forbidden_while_attempting_banner_control"));
						}
					}
				}
		
			} catch (Exception e) {
				System.out.println("Problem evaluating siege player consume event");
				e.printStackTrace();
			}
		}
	}
	
	@EventHandler
	public void onBucketUse(TownyBuildEvent event) {
		if(TownySettings.getWarSiegeEnabled() && event.isInWilderness() && TownySettings.isWarSiegeZoneBucketEmptyingRestrictionsEnabled() && TownySettings.getWarSiegeZoneBucketEmptyingRestrictionsMaterials().contains(event.getMaterial())) {
			if (SiegeWarDistanceUtil.isLocationInActiveSiegeZone(event.getLocation())) {
				event.setMessage(Translation.of("msg_war_siege_zone_bucket_emptying_forbidden"));
				event.setCancelled(true);
			}
		}
	}
	
	@EventHandler
	public void onPlayerKillsPlayer(PlayerKilledPlayerEvent event) {
		//Check for siege-war related death effects
		if(TownySettings.getWarSiegeEnabled()) {
			/*
			 * TODO: Evaluate if we're doing something bad in the SiegeWarDeathController
			 * by moving to an PlayerKilledPlayerEvent which is fired from a MONITOR 
			 * priority PlayerDeathEvent.
			 */
			SiegeWarDeathController.evaluateSiegePlayerDeath(event.getVictim(), event.getPlayerDeathEvent());
		}
	}
	
	@EventHandler
	public void onNationAddTownEvent(NationPreAddTownEvent event) {
		if(TownySettings.getWarSiegeEnabled() && TownySettings.getWarCommonPeacefulTownsEnabled() && event.getTown().isPeaceful()) {
			Set<Nation> validGuardianNations = TownPeacefulnessUtil.getValidGuardianNations(event.getTown());
			if(!validGuardianNations.contains(event.getNation())) {
				event.setCancelMessage(String.format(Translation.of("msg_war_siege_peaceful_town_cannot_join_nation"), 
						event.getTown().getName(),
						event.getNation().getName(),
						TownySettings.getWarSiegePeacefulTownsGuardianTownMinDistanceRequirement(),
						TownySettings.getWarSiegePeacefulTownsGuardianTownPlotsRequirement()));
				event.setCancelled(true);
			}
		}
	}
}
