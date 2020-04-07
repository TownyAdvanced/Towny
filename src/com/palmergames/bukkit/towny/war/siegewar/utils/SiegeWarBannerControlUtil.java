package com.palmergames.bukkit.towny.war.siegewar.utils;

import com.palmergames.bukkit.towny.TownyAPI;
import com.palmergames.bukkit.towny.TownyMessaging;
import com.palmergames.bukkit.towny.TownySettings;
import com.palmergames.bukkit.towny.TownyUniverse;
import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.towny.object.Town;
import com.palmergames.bukkit.towny.permissions.PermissionNodes;
import com.palmergames.bukkit.towny.war.siegewar.enums.SiegeSide;
import com.palmergames.bukkit.towny.war.siegewar.enums.SiegeStatus;
import com.palmergames.bukkit.towny.war.siegewar.locations.BannerControlSession;
import com.palmergames.bukkit.towny.war.siegewar.locations.SiegeZone;
import com.palmergames.util.TimeMgmt;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffectType;

/**
 * This class contains utility functions related to banner control
 *
 * @author Goosius
 */
public class SiegeWarBannerControlUtil {

	public static void evaluateBannerControl(SiegeZone siegeZone) {
		try {
			if(siegeZone.getSiege().getStatus() == SiegeStatus.IN_PROGRESS) {
				evaluateBannerControlEffects(siegeZone);
				evaluateCurrentBannerControlSessions(siegeZone);
				evaluateNewBannerControlSessions(siegeZone);
			}
		} catch (Exception e) {
			try {
				System.out.println("Problem evaluating banner control for siege zone: " + siegeZone.getName());
			} catch (Exception e2) {
				System.out.println("Problem evaluating banner control for siege zone: (could not read siegezone name)");
			}
			e.printStackTrace();
		}
	}

	private static void evaluateNewBannerControlSessions(SiegeZone siegeZone) {
		try {
			TownyUniverse universe = TownyUniverse.getInstance();
			Town defendingTown = siegeZone.getDefendingTown();
			Resident resident;
			Town residentTown;

			for(Player player: Bukkit.getOnlinePlayers()) {

				resident = universe.getDataSource().getResident(player.getName());
				if(!doesPlayerMeetBasicSessionRequirements(siegeZone, player, resident))
					continue;

				if(siegeZone.getBannerControlSessions().containsKey(player))
					continue; // Player already has a control session

				residentTown = resident.getTown();
				if(residentTown == siegeZone.getDefendingTown()
					&& universe.getPermissionSource().has(resident, PermissionNodes.TOWNY_TOWN_SIEGE_POINTS)) {
					//Player is defending their own town

					if(siegeZone.getBannerControllingSide() == SiegeSide.DEFENDERS && siegeZone.getBannerControllingResidents().contains(resident))
						continue; //Player already defending

					addNewBannerControlSession(siegeZone, player, resident, SiegeSide.DEFENDERS);
					continue;

				} else if (residentTown.hasNation()
					&& universe.getPermissionSource().has(resident, PermissionNodes.TOWNY_NATION_SIEGE_POINTS)) {

					if (defendingTown.hasNation()
						&& (defendingTown.getNation() == residentTown.getNation()
							|| defendingTown.getNation().hasMutualAlly(residentTown.getNation()))) {
						//Player is defending another town in the nation

						if(siegeZone.getBannerControllingSide() == SiegeSide.DEFENDERS && siegeZone.getBannerControllingResidents().contains(resident))
							continue; //Player already defending

						addNewBannerControlSession(siegeZone, player, resident, SiegeSide.DEFENDERS);
						continue;
					}

					if (siegeZone.getAttackingNation() == residentTown.getNation()
							|| siegeZone.getAttackingNation().hasMutualAlly(residentTown.getNation())) {
						//Player is attacking

						if(siegeZone.getBannerControllingSide() == SiegeSide.ATTACKERS && siegeZone.getBannerControllingResidents().contains(resident))
							continue; //Player already attacking

						addNewBannerControlSession(siegeZone, player, resident, SiegeSide.ATTACKERS);
						continue;
					}
				}
			}
		} catch (Exception e) {
			System.out.println("Problem evaluating new banner control sessions");
			e.printStackTrace();
		}
	}

	private static void addNewBannerControlSession(SiegeZone siegeZone, Player player, Resident resident, SiegeSide siegeSide) {
		//Add session
		int sessionDurationMillis = (int)(TownySettings.getWarSiegeBannerControlSessionDurationMinutes() * TimeMgmt.ONE_MINUTE_IN_MILLIS);
		long sessionEndTime = System.currentTimeMillis() + sessionDurationMillis;
		BannerControlSession bannerControlSession =
			new BannerControlSession(resident, player, siegeSide, sessionEndTime);
		siegeZone.addBannerControlSession(player, bannerControlSession);

		//Notify Player
		TownyMessaging.sendMsg(player, String.format(TownySettings.getLangString("msg_siege_war_banner_control_session_started"), TownySettings.getTownBlockSize(), TimeMgmt.getFormattedTimeValue(sessionDurationMillis)));

		//If this is a switching session, notify participating nations/towns
		if(siegeSide != siegeZone.getBannerControllingSide()) {

			boolean firstControlSwitchingSession = true;
			for (BannerControlSession otherSession : siegeZone.getBannerControlSessions().values()) {
				if (otherSession != bannerControlSession
					&& otherSession.getSiegeSide() != siegeZone.getBannerControllingSide()) {
					firstControlSwitchingSession = false;
					break;
				}
			}

			if(firstControlSwitchingSession) {
				String message;
				if (siegeSide == SiegeSide.ATTACKERS) {
					message = String.format(TownySettings.getLangString("msg_siege_war_attacking_troops_at_siege_banner"), siegeZone.getDefendingTown().getFormattedName());
				} else {
					message = String.format(TownySettings.getLangString("msg_siege_war_defending_troops_at_siege_banner"), siegeZone.getDefendingTown().getFormattedName());
				}

				SiegeWarNotificationUtil.informSiegeParticipants(siegeZone, message);
			}
		}
	}

	private static boolean doesPlayerMeetBasicSessionRequirements(SiegeZone siegeZone, Player player, Resident resident) throws Exception {
		if (!resident.hasTown())
			return false; //Player is a nomad

		if(resident.getTown().isOccupied() || resident.getTown().isNeutral())
			return false; // Player is from occupied or neutral town

		if(player.isDead())
			return false; // Player is dead

		if(!player.isOnline())
			return false; // Player offline

		if(player.isFlying() || player.getPotionEffect(PotionEffectType.INVISIBILITY) != null)
			return false;   // Player is flying or invisible

		if(!SiegeWarPointsUtil.isPlayerInTimedPointZone(player, siegeZone))
			return false; //player is not in the timed point zone

		if(SiegeWarBlockUtil.doesPlayerHaveANonAirBlockAboveThem(player))
			return false; //Player is under a block

		return true;
	}

	private static void evaluateCurrentBannerControlSessions(SiegeZone siegeZone) {
		for(BannerControlSession bannerControlSession: siegeZone.getBannerControlSessions().values()) {
			try {
				//Check if session failed
				if (!doesPlayerMeetBasicSessionRequirements(siegeZone, bannerControlSession.getPlayer(), bannerControlSession.getResident())) {
					siegeZone.removeBannerControlSession(bannerControlSession);
					TownyMessaging.sendMsg(bannerControlSession.getPlayer(), TownySettings.getLangString("msg_siege_war_banner_control_session_failure"));
					continue;
				}

				//Check if session succeeded
				if(System.currentTimeMillis() > bannerControlSession.getSessionEndTime()) {
					siegeZone.removeBannerControlSession(bannerControlSession);

					if(bannerControlSession.getSiegeSide() == siegeZone.getBannerControllingSide()) {
						//The player contributes to ongoing banner control
						siegeZone.addBannerControllingResident(bannerControlSession.getResident());
						TownyMessaging.sendMsg(bannerControlSession.getPlayer(), TownySettings.getLangString("msg_siege_war_banner_control_session_success"));
					} else {
						//The player gains banner control for their side
						siegeZone.clearBannerControllingResidents();
						siegeZone.setBannerControllingSide(bannerControlSession.getSiegeSide());
						siegeZone.addBannerControllingResident(bannerControlSession.getResident());
						//Inform player
						TownyMessaging.sendMsg(bannerControlSession.getPlayer(), TownySettings.getLangString("msg_siege_war_banner_control_session_success"));
						//Inform town/nation participants
						String message;
						if (bannerControlSession.getSiegeSide() == SiegeSide.ATTACKERS) {
							message = String.format(TownySettings.getLangString("msg_siege_war_banner_control_gained_by_attacker"), siegeZone.getDefendingTown().getFormattedName());
						} else {
							message = String.format(TownySettings.getLangString("msg_siege_war_banner_control_gained_by_defender"), siegeZone.getDefendingTown().getFormattedName());
						}
						SiegeWarNotificationUtil.informSiegeParticipants(siegeZone, message);
					}
				}
			} catch (Exception e) {
				System.out.println("Problem evaluating banner control session for player " + bannerControlSession.getPlayer().getName());
			}
		}
	}


	private static void evaluateBannerControlEffects(SiegeZone siegeZone) {
		//Evaluate the siege zone only if the siege is 'in progress'.
		if(siegeZone.getSiege().getStatus() != SiegeStatus.IN_PROGRESS)
			return;

		//Award siege points and pillage
		int siegePointsAdjustment;
		switch(siegeZone.getBannerControllingSide()) {
			case ATTACKERS:
				//Adjust siege points
				siegePointsAdjustment = siegeZone.getBannerControllingResidents().size() * TownySettings.getWarSiegePointsForAttackerOccupation();
				siegePointsAdjustment = SiegeWarPointsUtil.adjustSiegePointGainForCurrentSiegePointBalance(siegePointsAdjustment, siegeZone);
				siegeZone.adjustSiegePoints(siegePointsAdjustment);
				//Pillage
				double maximumPillageAmount = TownySettings.getWarSiegeMaximumPillageAmountPerPlot() * siegeZone.getDefendingTown().getTownBlocks().size();
				if(TownySettings.getWarSiegePillagingEnabled()
					&& TownySettings.isUsingEconomy()
					&& !siegeZone.getDefendingTown().isNeutral()
					&& siegeZone.getDefendingTown().getSiege().getTotalPillageAmount() < maximumPillageAmount)
				{
					SiegeWarMoneyUtil.pillageTown(siegeZone.getBannerControllingResidents(), siegeZone.getAttackingNation(), siegeZone.getDefendingTown());
				}
				//Save siege zone
				TownyUniverse.getInstance().getDataSource().saveSiegeZone(siegeZone);
			break;
			case DEFENDERS:
				//Adjust siege points
				siegePointsAdjustment = -(siegeZone.getBannerControllingResidents().size() * TownySettings.getWarSiegePointsForDefenderOccupation());
				siegePointsAdjustment = SiegeWarPointsUtil.adjustSiegePointGainForCurrentSiegePointBalance(siegePointsAdjustment, siegeZone);
				siegeZone.adjustSiegePoints(siegePointsAdjustment);
				//Save siege zone
				TownyUniverse.getInstance().getDataSource().saveSiegeZone(siegeZone);
			break;
			default:
			break;
		}

		//Remove banner control if all players on the list are logged out
		boolean allBannerControllingPlayersOffline = true;
		for(Resident resident: siegeZone.getBannerControllingResidents()) {
			Player player = TownyAPI.getInstance().getPlayer(resident);
			if(player != null) {
				allBannerControllingPlayersOffline = false;
				break;
			}
		}
		if(allBannerControllingPlayersOffline) {
			siegeZone.setBannerControllingSide(SiegeSide.NOBODY);
		}
	}
}
