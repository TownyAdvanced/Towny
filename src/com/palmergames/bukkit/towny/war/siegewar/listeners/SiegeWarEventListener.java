package com.palmergames.bukkit.towny.war.siegewar.listeners;

import java.util.ArrayList;
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
import com.palmergames.bukkit.towny.event.NewDayEvent;
import com.palmergames.bukkit.towny.event.NewTownEvent;
import com.palmergames.bukkit.towny.event.PreDeleteNationEvent;
import com.palmergames.bukkit.towny.event.TownPreAddResidentEvent;
import com.palmergames.bukkit.towny.event.TownPreClaimEvent;
import com.palmergames.bukkit.towny.event.nation.NationPreTownLeaveEvent;
import com.palmergames.bukkit.towny.event.nation.NationRankAddEvent;
import com.palmergames.bukkit.towny.event.nation.PreNewNationEvent;
import com.palmergames.bukkit.towny.event.time.NewHourEvent;
import com.palmergames.bukkit.towny.event.town.TownPreUnclaimCmdEvent;
import com.palmergames.bukkit.towny.event.town.TownRuinedEvent;
import com.palmergames.bukkit.towny.event.town.toggle.TownToggleExplosionEvent;
import com.palmergames.bukkit.towny.event.town.toggle.TownToggleNeutralEvent;
import com.palmergames.bukkit.towny.event.town.toggle.TownToggleOpenEvent;
import com.palmergames.bukkit.towny.event.town.toggle.TownTogglePVPEvent;
import com.palmergames.bukkit.towny.exceptions.NotRegisteredException;
import com.palmergames.bukkit.towny.object.Nation;
import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.towny.object.Town;
import com.palmergames.bukkit.towny.object.Translation;
import com.palmergames.bukkit.towny.utils.TownPeacefulnessUtil;
import com.palmergames.bukkit.towny.war.siegewar.SiegeWarSettings;
import com.palmergames.bukkit.towny.war.siegewar.SiegeWarTimerTaskController;
import com.palmergames.bukkit.towny.war.siegewar.enums.SiegeSide;
import com.palmergames.bukkit.towny.war.siegewar.enums.SiegeStatus;
import com.palmergames.bukkit.towny.war.siegewar.enums.SiegeWarPermissionNodes;
import com.palmergames.bukkit.towny.war.siegewar.objects.Siege;
import com.palmergames.bukkit.towny.war.siegewar.utils.SiegeWarBlockUtil;
import com.palmergames.bukkit.towny.war.siegewar.utils.SiegeWarDistanceUtil;
import com.palmergames.bukkit.towny.war.siegewar.utils.SiegeWarPermissionUtil;
import com.palmergames.bukkit.towny.war.siegewar.utils.SiegeWarTimeUtil;
import com.palmergames.util.TimeMgmt;

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
		if(SiegeWarSettings.getWarSiegeEnabled() && SiegeWarSettings.getWarCommonPeacefulTownsEnabled() && event.getTown().isNeutral()) {
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
				&& event.getTown().isNeutral()) {
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
			if(SiegeWarSettings.getWarCommonPeacefulTownsEnabled() && town.isNeutral()) {
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
			&& event.getResident().getTown().isNeutral()) { // We know that the resident's town will not be null based on the tests already done.
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

	/*
	 * Upon creation of a town, towns can be set to neutral.
	 */
	@EventHandler
	public void onCreateNewTown(NewTownEvent event) {
		if (SiegeWarSettings.getWarSiegeEnabled()) {
			Town town = event.getTown();
			town.setNeutral(SiegeWarSettings.getWarCommonNewTownPeacefulnessEnabled());
			town.setSiegeImmunityEndTime(System.currentTimeMillis() + (long)(SiegeWarSettings.getWarSiegeSiegeImmunityTimeNewTownsHours() * TimeMgmt.ONE_HOUR_IN_MILLIS));
			town.setDesiredPeacefulnessValue(SiegeWarSettings.getWarCommonNewTownPeacefulnessEnabled());

			TownyUniverse.getInstance().getDataSource().saveTown(town);
		}
	}
	
	/*
	 * On toggle explosions, SW will stop a town toggling explosions.
	 */
	@EventHandler
	public void onTownToggleExplosion(TownToggleExplosionEvent event) {
		if(SiegeWarSettings.getWarSiegeEnabled()
				&& SiegeWarSettings.getWarSiegeExplosionsAlwaysOnInBesiegedTowns()
				&& event.getTown().hasSiege()
				&& event.getTown().getSiege().getStatus().isActive())  {
			event.setCancellationMsg(Translation.of("msg_err_siege_besieged_town_cannot_toggle_explosions"));
			event.setCancelled(true);
		}
	}
	
	/*
	 * On toggle pvp, SW will stop a town toggling pvp.
	 */
	@EventHandler
	public void onTownTogglePVP(TownTogglePVPEvent event) {
		if(SiegeWarSettings.getWarSiegeEnabled()
				&& SiegeWarSettings.getWarSiegePvpAlwaysOnInBesiegedTowns()
				&& event.getTown().hasSiege()
				&& event.getTown().getSiege().getStatus().isActive())  {
			event.setCancellationMsg(Translation.of("msg_err_siege_besieged_town_cannot_toggle_pvp"));
			event.setCancelled(true);
		}
	}
	
	/*
	 * On toggle open, SW will stop a town toggling open.
	 */
	@EventHandler
	public void onTownToggleOpen(TownToggleOpenEvent event) {
		if(SiegeWarSettings.getWarSiegeEnabled()
				&& SiegeWarSettings.getWarSiegeBesiegedTownRecruitmentDisabled()
				&& event.getTown().hasSiege()
				&& event.getTown().getSiege().getStatus().isActive()) {
			event.setCancellationMsg(Translation.of("msg_err_siege_besieged_town_cannot_toggle_open_off"));
			event.setCancelled(true);
		}
	}
	
	/*
	 * On toggle neutral, SW will evaluate a number of things.
	 */
	@EventHandler
	public void onTownToggleNeutral(TownToggleNeutralEvent event) {
		if (!SiegeWarSettings.getWarSiegeEnabled())
			return;
		
		if(!SiegeWarSettings.getWarCommonPeacefulTownsEnabled()) {
			event.setCancellationMsg(Translation.of("msg_err_command_disable"));
			event.setCancelled(true);
			return;
		}
		
		Town town = event.getTown();
		
		if (event.isAdminAction()) {
			town.setNeutral(!town.isNeutral());
		} else {
			if (town.getPeacefulnessChangeConfirmationCounterDays() == 0) {
				
				//Here, no countdown is in progress, and the town wishes to change peacefulness status
				town.setDesiredPeacefulnessValue(!town.isNeutral());
				
				int counterValue;
				if(System.currentTimeMillis() < (town.getRegistered() + (TimeMgmt.ONE_DAY_IN_MILLIS * 7))) {
					counterValue = SiegeWarSettings.getWarCommonPeacefulTownsNewTownConfirmationRequirementDays();
				} else {
					counterValue = SiegeWarSettings.getWarCommonPeacefulTownsConfirmationRequirementDays();
				}
				town.setPeacefulnessChangeConfirmationCounterDays(counterValue);
				
				//Send message to town
				if (town.getDesiredPeacefulnessValue())
					TownyMessaging.sendPrefixedTownMessage(town, String.format(Translation.of("msg_war_common_town_declared_peaceful"), counterValue));
				else
					TownyMessaging.sendPrefixedTownMessage(town, String.format(Translation.of("msg_war_common_town_declared_non_peaceful"), counterValue));
				
				//Remove any military nation ranks of residents
				for(Resident peacefulTownResident: town.getResidents()) {
					for (String nationRank : new ArrayList<>(peacefulTownResident.getNationRanks())) {
						if (SiegeWarPermissionUtil.doesNationRankAllowPermissionNode(nationRank, SiegeWarPermissionNodes.TOWNY_NATION_SIEGE_POINTS)) {
							try {
								peacefulTownResident.removeNationRank(nationRank);
							} catch (NotRegisteredException ignored) {}
						}
					}
				}
				
			} else {
				//Here, a countdown is in progress, and the town wishes to cancel the countdown,
				town.setDesiredPeacefulnessValue(town.isNeutral());
				town.setPeacefulnessChangeConfirmationCounterDays(0);
				//Send message to town
				TownyMessaging.sendPrefixedTownMessage(town, String.format(Translation.of("msg_war_common_town_peacefulness_countdown_cancelled")));
			}
		}
	}
	
	/*
	 * Update town peacefulness counters.
	 */
	@EventHandler
	public void onNewDay(NewDayEvent event) {
		if (SiegeWarSettings.getWarCommonPeacefulTownsEnabled()) {
			TownPeacefulnessUtil.updateTownPeacefulnessCounters();
			if(SiegeWarSettings.getWarSiegeEnabled())
				TownPeacefulnessUtil.evaluatePeacefulTownNationAssignments();
		}
	}
	
	/*
	 * On NewHours SW makes some calculations.
	 */
	@EventHandler
	public void onNewHour(NewHourEvent event) {
		if(SiegeWarSettings.getWarSiegeEnabled()) {
			SiegeWarTimerTaskController.updatePopulationBasedSiegePointModifiers();
		}
	}
	
	/*
	 * Upon attempting to claim land, SW will stop it under some conditions.
	 */
	@EventHandler
	public void onTownClaim(TownPreClaimEvent event) {
		if (SiegeWarSettings.getWarSiegeEnabled()) {
			//If the claimer's town is under siege, they cannot claim any land
			if (SiegeWarSettings.getWarSiegeBesiegedTownClaimingDisabled()
				&& event.getTown().hasSiege()
				&& event.getTown().getSiege().getStatus().isActive()) {
				event.setCancelled(true);
				event.setCancelMessage(Translation.of("msg_err_siege_besieged_town_cannot_claim"));
				return;
			}

			//If the land is too near any active siege zone, it cannot be claimed.
			if(SiegeWarSettings.getWarSiegeClaimingDisabledNearSiegeZones()) {
				for(Siege siege: TownyUniverse.getInstance().getDataSource().getSieges()) {
					try {
						if (siege.getStatus().isActive()
							&& SiegeWarDistanceUtil.isInSiegeZone(event.getPlayer(), siege)) {
							event.setCancelled(true);
							event.setCancelMessage(Translation.of("msg_err_siege_claim_too_near_siege_zone"));
							break;
						}
					} catch (Exception e) {
						//Problem with this particular siegezone. Ignore siegezone
						try {
							System.out.println("Problem with verifying claim against the following siege zone" + siege.getName() + ". Claim allowed.");
						} catch (Exception e2) {
							System.out.println("Problem with verifying claim against a siege zone (name could not be read). Claim allowed");
						}
						e.printStackTrace();
					}
				}
			}
		}
	}
	
	/*
	 * Siege War will prevent unclaiming land in some situations.
	 */
	@EventHandler
	public void onTownUnclaim(TownPreUnclaimCmdEvent event) {
		if (SiegeWarSettings.getWarCommonOccupiedTownUnClaimingDisabled() && event.getTown().isOccupied()) {
			event.setCancelled(true);
			event.setCancelMessage(Translation.of("msg_err_war_common_occupied_town_cannot_unclaim"));
			return;
		}
			
		if(SiegeWarSettings.getWarSiegeEnabled()
			&& SiegeWarSettings.getWarSiegeBesiegedTownUnClaimingDisabled()
			&& event.getTown().hasSiege()
			&& (
				event.getTown().getSiege().getStatus().isActive()
				|| event.getTown().getSiege().getStatus() == SiegeStatus.ATTACKER_WIN
				|| event.getTown().getSiege().getStatus() == SiegeStatus.DEFENDER_SURRENDER
				)
			)
		{
			event.setCancelled(true);
			event.setCancelMessage(Translation.of("msg_err_siege_besieged_town_cannot_unclaim"));
		}
	}
}
