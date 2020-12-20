package com.palmergames.bukkit.towny.war.siegewar.utils;

import com.palmergames.bukkit.towny.Towny;
import com.palmergames.bukkit.towny.TownyAPI;
import com.palmergames.bukkit.towny.TownyMessaging;
import com.palmergames.bukkit.towny.TownySettings;
import com.palmergames.bukkit.towny.TownyUniverse;
import com.palmergames.bukkit.towny.exceptions.TownyException;
import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.towny.object.Town;
import com.palmergames.bukkit.towny.object.Translation;
import com.palmergames.bukkit.towny.war.siegewar.SiegeWarSettings;
import com.palmergames.bukkit.towny.war.siegewar.enums.SiegeSide;
import com.palmergames.bukkit.towny.war.siegewar.enums.SiegeStatus;
import com.palmergames.bukkit.towny.war.siegewar.enums.SiegeWarPermissionNodes;
import com.palmergames.bukkit.towny.war.siegewar.objects.BannerControlSession;
import com.palmergames.bukkit.towny.war.siegewar.objects.Siege;
import com.palmergames.bukkit.towny.war.siegewar.siege.SiegeController;
import com.palmergames.util.TimeMgmt;
import com.palmergames.util.TimeTools;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.ArrayList;
import java.util.List;

/**
 * This class contains utility functions related to banner control
 *
 * @author Goosius
 */
public class SiegeWarBannerControlUtil {

	public static void evaluateBannerControl(Siege siege) {
		try {
			if(siege.getStatus() == SiegeStatus.IN_PROGRESS) {
				evaluateBannerControlEffects(siege);
				evaluateExistingBannerControlSessions(siege);
				evaluateNewBannerControlSessions(siege);
			}
		} catch (Exception e) {
			try {
				System.out.println("Problem evaluating banner control for siege: " + siege.getName());
			} catch (Exception e2) {
				System.out.println("Problem evaluating banner control for siege: (could not read siege name)");
			}
			e.printStackTrace();
		}
	}

	private static void evaluateNewBannerControlSessions(Siege siege) {
		try {
			TownyUniverse universe = TownyUniverse.getInstance();
			Town defendingTown = siege.getDefendingTown();
			Resident resident;
			Town residentTown;

			for(Player player: Bukkit.getOnlinePlayers()) {

				resident = universe.getResident(player.getUniqueId());
	            if (resident == null)
	            	throw new TownyException(Translation.of("msg_err_not_registered_1", player.getName()));
	            
				if(!doesPlayerMeetBasicSessionRequirements(siege, player, resident))
					continue;

				if(siege.getBannerControlSessions().containsKey(player))
					continue; // Player already has a control session

				residentTown = resident.getTown();
				if(residentTown == siege.getDefendingTown()
					&& universe.getPermissionSource().testPermission(resident.getPlayer(), SiegeWarPermissionNodes.TOWNY_TOWN_SIEGE_POINTS.getNode())) {
					//Player is defending their own town

					if(siege.getBannerControllingSide() == SiegeSide.DEFENDERS && siege.getBannerControllingResidents().contains(resident))
						continue; //Player already defending

					addNewBannerControlSession(siege, player, resident, SiegeSide.DEFENDERS);
					continue;

				} else if (residentTown.hasNation()
					&& universe.getPermissionSource().testPermission(resident.getPlayer(), SiegeWarPermissionNodes.TOWNY_NATION_SIEGE_POINTS.getNode())) {

					if (defendingTown.hasNation()
						&& (defendingTown.getNation() == residentTown.getNation()
							|| defendingTown.getNation().hasMutualAlly(residentTown.getNation()))) {
						//Player is defending another town in the nation

						if(siege.getBannerControllingSide() == SiegeSide.DEFENDERS && siege.getBannerControllingResidents().contains(resident))
							continue; //Player already defending

						addNewBannerControlSession(siege, player, resident, SiegeSide.DEFENDERS);
						continue;
					}

					if (siege.getAttackingNation() == residentTown.getNation()
							|| siege.getAttackingNation().hasMutualAlly(residentTown.getNation())) {
						//Player is attacking

						if(siege.getBannerControllingSide() == SiegeSide.ATTACKERS && siege.getBannerControllingResidents().contains(resident))
							continue; //Player already attacking

						addNewBannerControlSession(siege, player, resident, SiegeSide.ATTACKERS);
						continue;
					}
				}
			}
		} catch (Exception e) {
			System.out.println("Problem evaluating new banner control sessions");
			e.printStackTrace();
		}
	}

	private static void addNewBannerControlSession(Siege siege, Player player, Resident resident, SiegeSide siegeSide) {
		//Add session
		int sessionDurationMillis = (int)(SiegeWarSettings.getWarSiegeBannerControlSessionDurationMinutes() * TimeMgmt.ONE_MINUTE_IN_MILLIS);
		long sessionEndTime = System.currentTimeMillis() + sessionDurationMillis;
		BannerControlSession bannerControlSession =
			new BannerControlSession(resident, player, siegeSide, sessionEndTime);
		siege.addBannerControlSession(player, bannerControlSession);

		//Notify Player
		TownyMessaging.sendMsg(player, String.format(
			Translation.of("msg_siege_war_banner_control_session_started"), 
			SiegeWarSettings.getBannerControlHorizontalDistanceBlocks(),
			SiegeWarSettings.getBannerControlVerticalDistanceBlocks(),
			TimeMgmt.getFormattedTimeValue(sessionDurationMillis)));

		//Make player glow (which also shows them a timer)
		int effectDurationSeconds = (SiegeWarSettings.getWarSiegeBannerControlSessionDurationMinutes() * 60) + (int)TownySettings.getShortInterval(); 
		int effectDurationTicks = (int)(TimeTools.convertToTicks(effectDurationSeconds));
		Towny.getPlugin().getServer().getScheduler().scheduleSyncDelayedTask(Towny.getPlugin(), new Runnable() {
			public void run() {
				List<PotionEffect> potionEffects = new ArrayList<>();
				potionEffects.add(new PotionEffect(PotionEffectType.GLOWING, effectDurationTicks, 0));
				player.addPotionEffects(potionEffects);
			}
		});

		//If this is a switching session, notify participating nations/towns
		if(siegeSide != siege.getBannerControllingSide()) {

			boolean firstControlSwitchingSession = true;
			for (BannerControlSession otherSession : siege.getBannerControlSessions().values()) {
				if (otherSession != bannerControlSession
					&& otherSession.getSiegeSide() != siege.getBannerControllingSide()) {
					firstControlSwitchingSession = false;
					break;
				}
			}

			if(firstControlSwitchingSession) {
				String message;
				if (siegeSide == SiegeSide.ATTACKERS) {
					message = Translation.of("msg_siege_war_attacking_troops_at_siege_banner", siege.getDefendingTown().getFormattedName());
				} else {
					message = Translation.of("msg_siege_war_defending_troops_at_siege_banner", siege.getDefendingTown().getFormattedName());
				}

				SiegeWarNotificationUtil.informSiegeParticipants(siege, message);
			}
		}
	}

	private static boolean doesPlayerMeetBasicSessionRequirements(Siege siege, Player player, Resident resident) throws Exception {
		if(player.getWorld() != siege.getFlagLocation().getWorld())
			return false; //Player not in same world as siege

		if (!resident.hasTown())
			return false; //Player is a nomad

		if(resident.getTown().isConquered())
			return false; // Player is from occupied town

		if(player.isDead())
			return false; // Player is dead

		if(!player.isOnline())
			return false; // Player offline

		if(player.isFlying())
			return false;   // Player is flying

		if(!SiegeWarPointsUtil.isPlayerInTimedPointZone(player, siege))
			return false; //player is not in the timed point zone

		if(!SiegeWarDistanceUtil.isUndergroundBannerControlEnabledInWorld(player.getLocation().getWorld())
			&& SiegeWarDistanceUtil.doesLocationHaveANonAirBlockAboveIt(player.getLocation()))
			return false; //player has a block above them

		return true;
	}

	private static void evaluateExistingBannerControlSessions(Siege siege) {
		for(BannerControlSession bannerControlSession: siege.getBannerControlSessions().values()) {
			try {
				//Check if session failed
				if (!doesPlayerMeetBasicSessionRequirements(siege, bannerControlSession.getPlayer(), bannerControlSession.getResident())) {
					siege.removeBannerControlSession(bannerControlSession);
					TownyMessaging.sendMsg(bannerControlSession.getPlayer(), Translation.of("msg_siege_war_banner_control_session_failure"));
					continue;
				}

				//Check if session succeeded
				if(System.currentTimeMillis() > bannerControlSession.getSessionEndTime()) {
					siege.removeBannerControlSession(bannerControlSession);

					if(bannerControlSession.getSiegeSide() == siege.getBannerControllingSide()) {
						//The player contributes to ongoing banner control
						siege.addBannerControllingResident(bannerControlSession.getResident());
						TownyMessaging.sendMsg(bannerControlSession.getPlayer(), Translation.of("msg_siege_war_banner_control_session_success"));
					} else {
						//The player gains banner control for their side
						siege.clearBannerControllingResidents();
						siege.setBannerControllingSide(bannerControlSession.getSiegeSide());
						siege.addBannerControllingResident(bannerControlSession.getResident());
						//Inform player
						TownyMessaging.sendMsg(bannerControlSession.getPlayer(), Translation.of("msg_siege_war_banner_control_session_success"));
						//Inform town/nation participants
						String message;
						if (bannerControlSession.getSiegeSide() == SiegeSide.ATTACKERS) {
							message = Translation.of("msg_siege_war_banner_control_gained_by_attacker", siege.getDefendingTown().getFormattedName());
						} else {
							message = Translation.of("msg_siege_war_banner_control_gained_by_defender", siege.getDefendingTown().getFormattedName());
						}
						SiegeWarNotificationUtil.informSiegeParticipants(siege, message);
					}
				}
			} catch (Exception e) {
				System.out.println("Problem evaluating banner control session for player " + bannerControlSession.getPlayer().getName());
			}
		}
	}


	private static void evaluateBannerControlEffects(Siege siege) {
		//Evaluate the siege zone only if the siege is 'in progress'.
		if(siege.getStatus() != SiegeStatus.IN_PROGRESS)
			return;

		//Award siege points and pillage
		int siegePoints;
		switch(siege.getBannerControllingSide()) {
			case ATTACKERS:
				//Adjust siege points
				siegePoints = siege.getBannerControllingResidents().size() * SiegeWarSettings.getWarSiegePointsForAttackerOccupation();
				siegePoints = SiegeWarPointsUtil.adjustSiegePointsForPopulationQuotient(true, siegePoints, siege);
				siege.adjustSiegePoints(siegePoints);
				//Save siege zone
				SiegeController.saveSiege(siege);
			break;
			case DEFENDERS:
				//Adjust siege points
				siegePoints = -(siege.getBannerControllingResidents().size() * SiegeWarSettings.getWarSiegePointsForDefenderOccupation());
				siegePoints = SiegeWarPointsUtil.adjustSiegePointsForPopulationQuotient(false, siegePoints, siege);
				siege.adjustSiegePoints(siegePoints);
				//Save siege zone
				SiegeController.saveSiege(siege);
			break;
			default:
			return;
		}

		//Remove banner control if 
		//1. All players on the list are logged out, or
		//2. The list is now empty
		boolean bannerControlLost = true;
		for(Resident resident: siege.getBannerControllingResidents()) {
			Player player = TownyAPI.getInstance().getPlayer(resident);
			if(player != null) {
				bannerControlLost = false;
				break;
			}
		}
		if(bannerControlLost) {
			siege.setBannerControllingSide(SiegeSide.NOBODY);
			siege.clearBannerControllingResidents();
		}
	}
}
