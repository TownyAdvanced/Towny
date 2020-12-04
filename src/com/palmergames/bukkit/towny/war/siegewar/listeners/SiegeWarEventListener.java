package com.palmergames.bukkit.towny.war.siegewar.listeners;

import java.util.List;
import java.util.Set;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPistonRetractEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;

import com.palmergames.bukkit.towny.Towny;
import com.palmergames.bukkit.towny.TownyEconomyHandler;
import com.palmergames.bukkit.towny.TownyMessaging;
import com.palmergames.bukkit.towny.TownySettings;
import com.palmergames.bukkit.towny.TownyUniverse;
import com.palmergames.bukkit.towny.event.NationPreAddTownEvent;
import com.palmergames.bukkit.towny.event.PreDeleteNationEvent;
import com.palmergames.bukkit.towny.event.TownPreAddResidentEvent;
import com.palmergames.bukkit.towny.event.nation.NationPreTownLeaveEvent;
import com.palmergames.bukkit.towny.event.nation.NationRankAddEvent;
import com.palmergames.bukkit.towny.event.nation.PreNewNationEvent;
import com.palmergames.bukkit.towny.event.town.TownRuinedEvent;
import com.palmergames.bukkit.towny.exceptions.NotRegisteredException;
import com.palmergames.bukkit.towny.object.Nation;
import com.palmergames.bukkit.towny.object.Town;
import com.palmergames.bukkit.towny.object.Translation;
import com.palmergames.bukkit.towny.utils.TownPeacefulnessUtil;
import com.palmergames.bukkit.towny.war.siegewar.SiegeWarSettings;
import com.palmergames.bukkit.towny.war.siegewar.enums.SiegeSide;
import com.palmergames.bukkit.towny.war.siegewar.enums.SiegeWarPermissionNodes;
import com.palmergames.bukkit.towny.war.siegewar.objects.Siege;
import com.palmergames.bukkit.towny.war.siegewar.utils.SiegeWarBlockUtil;
import com.palmergames.bukkit.towny.war.siegewar.utils.SiegeWarPermissionUtil;
import com.palmergames.bukkit.towny.war.siegewar.utils.SiegeWarTimeUtil;

public class SiegeWarEventListener implements Listener {

	private final Towny plugin;
	
	public SiegeWarEventListener(Towny instance) {

		plugin = instance;
	}

	/*
	 * SW will prevent someone in a banner area from curing their poisoning with milk.
	 */
	@EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
	public void onPlayerConsume(PlayerItemConsumeEvent event) {
		if (plugin.isError()) {
			event.setCancelled(true);
			return;
		}

		if(SiegeWarSettings.getWarSiegeEnabled()) {
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
	
	
	/*
	 * SW limits which Towns can join or be added to a nation.
	 */
	@EventHandler
	public void onNationAddTownEvent(NationPreAddTownEvent event) {
		if(SiegeWarSettings.getWarSiegeEnabled() && SiegeWarSettings.getWarCommonPeacefulTownsEnabled() && event.getTown().isPeaceful()) {
			Set<Nation> validGuardianNations = TownPeacefulnessUtil.getValidGuardianNations(event.getTown());
			if(!validGuardianNations.contains(event.getNation())) {
				event.setCancelMessage(Translation.of("msg_war_siege_peaceful_town_cannot_join_nation", 
						event.getTown().getName(),
						event.getNation().getName(),
						SiegeWarSettings.getWarSiegePeacefulTownsGuardianTownMinDistanceRequirement(),
						SiegeWarSettings.getWarSiegePeacefulTownsGuardianTownPlotsRequirement()));
				event.setCancelled(true);
			}
		}
	}
	
	/*
	 * SW warns peaceful towns who make nations their decision may be a poor one, but does not stop them.
	 */
	@EventHandler
	public void onNewNationEvent(PreNewNationEvent event) {
		if (SiegeWarSettings.getWarSiegeEnabled() && SiegeWarSettings.getWarCommonPeacefulTownsEnabled()
				&& event.getTown().isPeaceful()) {
			TownyMessaging.sendMsg(event.getTown().getMayor(),
					Translation.of("msg_war_siege_warning_peaceful_town_should_not_create_nation"));
		}
	}
	
	/*
	 * SW will warn a nation about to delete itself that it can claim a refund after the fact.
	 */
	@EventHandler
	public void onNationDeleteEvent(PreDeleteNationEvent event) {
		//If nation refund is enabled, warn the player that they will get a refund (and indicate how to claim it).
		if (SiegeWarSettings.getWarSiegeEnabled() && TownySettings.isUsingEconomy()
				&& SiegeWarSettings.getWarSiegeRefundInitialNationCostOnDelete()) {
			int amountToRefund = (int)(TownySettings.getNewNationPrice() * 0.01 * SiegeWarSettings.getWarSiegeNationCostRefundPercentageOnDelete());
			TownyMessaging.sendMsg(event.getNation().getKing(), Translation.of("msg_err_siege_war_delete_nation_warning", TownyEconomyHandler.getFormattedBalance(amountToRefund)));
		}

	}
	
	/*
	 * Duplicates what exists in the TownyBlockListener but on a higher priority.
	 */
	@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
	public void onBlockPistonRetract(BlockPistonRetractEvent event) {

		if (plugin.isError()) {
			event.setCancelled(true);
			return;
		}

		if (testBlockMove(event.getBlock(), event.isSticky() ? event.getDirection().getOppositeFace() : event.getDirection()))
			event.setCancelled(true);

		List<Block> blocks = event.getBlocks();
		
		if (!blocks.isEmpty()) {
			//check each block to see if it's going to pass a plot boundary
			for (Block block : blocks) {
				if (testBlockMove(block, event.getDirection()))
					event.setCancelled(true);
			}
		}
	}

	/*
	 * Duplicates what exists in the TownyBlockListener but on a higher priority.
	 */
	@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
	public void onBlockPistonExtend(BlockPistonExtendEvent event) {

		if (plugin.isError()) {
			event.setCancelled(true);
			return;
		}
		
		if (testBlockMove(event.getBlock(), event.getDirection()))
			event.setCancelled(true);
		
		List<Block> blocks = event.getBlocks();

		if (!blocks.isEmpty()) {
			//check each block to see if it's going to pass a plot boundary
			for (Block block : blocks) {
				if (testBlockMove(block, event.getDirection()))
					event.setCancelled(true);
			}
		}
	}

	/**
	 * Decides whether blocks moved by pistons follow the rules.
	 * 
	 * @param block - block that is being moved.
	 * @param direction - direction the piston is facing.
	 * 
	 * @return true if block is able to be moved according to siege war rules. 
	 */
	private boolean testBlockMove(Block block, BlockFace direction) {

		Block blockTo = block.getRelative(direction);

		if(SiegeWarSettings.getWarSiegeEnabled()) {
			if(SiegeWarBlockUtil.isBlockNearAnActiveSiegeBanner(block) || SiegeWarBlockUtil.isBlockNearAnActiveSiegeBanner(blockTo)) {
				return true;
			}
		}

		return false;
	}
	
	/*
	 * SW will prevent towns leaving their nations.
	 */
	@EventHandler
	public void onTownLeaveNation(NationPreTownLeaveEvent event) {
		if (SiegeWarSettings.getWarSiegeEnabled()) {

			Town town = event.getTown();
			//If a peaceful town has no options, we don't let it revolt
			if(SiegeWarSettings.getWarCommonPeacefulTownsEnabled() && town.isPeaceful()) {
				Set<Nation> validGuardianNations = TownPeacefulnessUtil.getValidGuardianNations(town);
				if(validGuardianNations.size() == 0) {
					event.setCancelMessage(Translation.of("msg_war_siege_peaceful_town_cannot_revolt_nearby_guardian_towns_zero", 
						SiegeWarSettings.getWarSiegePeacefulTownsGuardianTownMinDistanceRequirement(), 
						SiegeWarSettings.getWarSiegePeacefulTownsGuardianTownPlotsRequirement()));
					event.setCancelled(true);
				} else if(validGuardianNations.size() == 1) {
					event.setCancelMessage(Translation.of("msg_war_siege_peaceful_town_cannot_revolt_nearby_guardian_towns_one", 
						SiegeWarSettings.getWarSiegePeacefulTownsGuardianTownMinDistanceRequirement(), 
						SiegeWarSettings.getWarSiegePeacefulTownsGuardianTownPlotsRequirement()));
					event.setCancelled(true);
				}
			}

			if (SiegeWarSettings.getWarSiegeTownLeaveDisabled()) {

				if (!SiegeWarSettings.getWarSiegeRevoltEnabled()) {
					event.setCancelMessage(Translation.of("msg_err_siege_war_town_voluntary_leave_impossible"));
					event.setCancelled(true);
				}
				if (town.isRevoltImmunityActive()) {
					event.setCancelMessage(Translation.of("msg_err_siege_war_revolt_immunity_active"));
					event.setCancelled(true);
				}

				//Activate revolt immunity
				SiegeWarTimeUtil.activateRevoltImmunityTimer(town);

				TownyMessaging.sendGlobalMessage(
					String.format(Translation.of("msg_siege_war_revolt"),
						town.getFormattedName(),
						town.getMayor().getFormattedName(),
						event.getNation().getFormattedName()));
			}
		}
	}
	
	@EventHandler
	public void onTownGoesToRuin(TownRuinedEvent event) {
		if (event.getTown().hasSiege())
			TownyUniverse.getInstance().getDataSource().removeSiege(event.getTown().getSiege(), SiegeSide.ATTACKERS);
	}
	
	@EventHandler
	public void onNationRankGivenToPlayer(NationRankAddEvent event) throws NotRegisteredException {
		//In Siegewar, if target town is peaceful, can't add military rank
		if(SiegeWarSettings.getWarSiegeEnabled()
			&& SiegeWarSettings.getWarCommonPeacefulTownsEnabled()
			&& SiegeWarPermissionUtil.doesNationRankAllowPermissionNode(event.getRank(), SiegeWarPermissionNodes.TOWNY_NATION_SIEGE_POINTS)
			&& event.getResident().getTown().isPeaceful()) { // We know that the resident's town will not be null based on the tests already done.
			event.setCancelled(true);
			event.setCancelMessage(Translation.of("msg_war_siege_cannot_add_nation_military_rank_to_peaceful_resident"));
		}
		
	}

	/*
	 * If town is under siege, town cannot recruit new members
	 */
	@EventHandler
	public void onTownAddResident(TownPreAddResidentEvent event) {
		if (SiegeWarSettings.getWarSiegeEnabled()
				&& SiegeWarSettings.getWarSiegeBesiegedTownRecruitmentDisabled()
				&& event.getTown().hasSiege()
				&& event.getTown().getSiege().getStatus().isActive()) {
			event.setCancelled(true);
			event.setCancelMessage(Translation.of("msg_err_siege_besieged_town_cannot_recruit"));
		}
	}
		
}
