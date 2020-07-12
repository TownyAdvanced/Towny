package com.palmergames.bukkit.towny.utils;

import com.palmergames.bukkit.towny.*;
import com.palmergames.bukkit.towny.TownyUniverse;
import com.palmergames.bukkit.towny.db.TownyDataSource;
import com.palmergames.bukkit.towny.exceptions.NotRegisteredException;
import com.palmergames.bukkit.towny.object.*;
import com.palmergames.bukkit.towny.war.siegewar.utils.SiegeWarDistanceUtil;
import com.palmergames.bukkit.util.BukkitTools;
import com.palmergames.util.TimeMgmt;
import com.palmergames.util.TimeTools;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;

public class TownPeacefulnessUtil {

	/**
	 * This method adjust the peacefulness counters of all towns, where required
	 */
	public static void updateTownPeacefulnessCounters() {
		TownyUniverse townyUniverse = TownyUniverse.getInstance();

		List<Town> towns = new ArrayList<>(townyUniverse.getDataSource().getTowns());
		ListIterator<Town> townItr = towns.listIterator();
		Town town;

		while (townItr.hasNext()) {
			town = townItr.next();
			/*
			 * Only adjust counter for this town if it really still exists.
			 * We are running in an Async thread so MUST verify all objects.
			 */
			if (townyUniverse.getDataSource().hasTown(town.getName()) && !town.isRuined())
				updateTownPeacefulnessCounters(town);
		}
	}

	public static void updateTownPeacefulnessCounters(Town town) {
		String message;

		if(town.getPeacefulnessChangeConfirmationCounterDays() != 0) {
			town.decrementPeacefulnessChangeConfirmationCounterDays();

			if(town.getPeacefulnessChangeConfirmationCounterDays() < 1) {
				town.flipPeaceful();

				if(TownySettings.getWarSiegeEnabled()) {
					if (town.isPeaceful()) {
						message = String.format(TownySettings.getLangString("msg_war_siege_town_became_peaceful"), town.getFormattedName(), TownySettings.getWarSiegePeacefulTownsGuardianTownPopulationRequirement(), TownySettings.getWarSiegePeacefulTownsGuardianTownPlotsRequirement());
					} else {
						message = String.format(TownySettings.getLangString("msg_war_siege_town_became_non_peaceful"), town.getFormattedName());
					}
				} else {
					if (town.isPeaceful()) {
						message = String.format(TownySettings.getLangString("msg_war_common_town_became_peaceful"), town.getFormattedName());
					} else {
						message = String.format(TownySettings.getLangString("msg_war_common_town_became_non_peaceful"), town.getFormattedName());
					}
				}
				TownyMessaging.sendGlobalMessage(message);
			}

			TownyUniverse.getInstance().getDataSource().saveTown(town);
		}
	}

	public static boolean doesPlayerHaveTownRelatedPeacefulness(Player player) {
		try {
			Resident resident = TownyUniverse.getInstance().getDataSource().getResident(player.getName());
			if(resident.hasTown()) {
				/* 
				 * True if:
				 * Current town is peaceful,
				 * current town is declared peaceful,
				 * or player just left a peaceful town
				*/
				return resident.getTown().isPeaceful() 
					|| resident.getTown().getDesiredPeacefulnessValue()
					|| resident.isPostTownLeavePeacefulEnabled();
			} else {
				//True if if player just left a peaceful town
				return resident.isPostTownLeavePeacefulEnabled();
			}

		} catch (NotRegisteredException e) { return false; }
}

	public static void grantPostTownLeavePeacefulnessToResident(Resident resident) {
		int hours = TownySettings.getWarCommonPeacefulTownsResidentPostLeavePeacefulnessDurationHours();
		String timeString = TimeMgmt.getFormattedTimeValue(hours * TimeMgmt.ONE_HOUR_IN_MILLIS);
		resident.setPostTownLeavePeacefulEnabled(true);
		resident.setPostTownLeavePeacefulHoursRemaining(hours);
		String message;
		if(TownySettings.getWarSiegeEnabled()) {
			message  = String.format(TownySettings.getLangString("msg_war_siege_resident_left_peaceful_town"), timeString);
			TownyMessaging.sendMsg(resident, message);
		}
	}

	public static void updatePostTownLeavePeacefulnessCounters() {
		TownyDataSource townyDataSource = TownyUniverse.getInstance().getDataSource();
		
		List<Resident> residents = new ArrayList<>(townyDataSource.getResidents());
		ListIterator<Resident> residentItr = residents.listIterator();
		Resident resident;

		while (residentItr.hasNext()) {
			resident = residentItr.next();
			/*
			 * We are running in an Async thread so MUST verify all objects.
			 */
			if (townyDataSource.hasResident(resident.getName())) {
				if(resident.isPostTownLeavePeacefulEnabled()) {
					resident.decrementPostTownLeavePeacefulHoursRemaining();

					if(resident.getPostTownLeavePeacefulHoursRemaining() < 1) {
						resident.setPostTownLeavePeacefulEnabled(false);
					}

					townyDataSource.saveResident(resident);
				}
			}
		}
	}

	/**
	 * This method punishes any peaceful players who are in siege-zones
	 * (except for their own town OR any peaceful town)
	 * 
	 * A player is peaceful if they
	 * 1. Are resident in a peaceful town
	 * 2. Are resident in a declared (but not confirmed) peaceful town
	 * 3. Were recently resident in a peaceful town
	 *
	 * The punishment is a status effect (e.g. poison, nausea)
	 * The punishment is refreshed every 20 seconds, until the player leaves the siege-zone
	 */
	public static void punishPeacefulPlayersInActiveSiegeZones() {
		for(Player player: BukkitTools.getOnlinePlayers()) {
			try {
				//Don't apply to towny admins
				if(TownyUniverse.getInstance().getPermissionSource().isTownyAdmin(player))
					continue;

				if(doesPlayerHaveTownRelatedPeacefulness(player)) {
					//Don't punish if the player is in a peaceful town
					TownBlock townBlockAtPlayerLocation = TownyAPI.getInstance().getTownBlock(player.getLocation());
					if(townBlockAtPlayerLocation != null
						&& townBlockAtPlayerLocation.getTown().isPeaceful())
					{
						continue;
					}

					//Don't punish if the player is in their own town
					Resident resident = TownyUniverse.getInstance().getDataSource().getResident(player.getName());
					if(resident.hasTown()
						&& townBlockAtPlayerLocation != null
						&& resident.getTown() == townBlockAtPlayerLocation.getTown())
					{
						continue;
					}

					//Punish if the player is in a siege zone
					if(SiegeWarDistanceUtil.isLocationInActiveSiegeZone(player.getLocation())) {
						TownyMessaging.sendMsg(player, TownySettings.getLangString("msg_war_siege_peaceful_player_punished_for_being_in_siegezone"));
						int effectDurationTicks = (int)(TimeTools.convertToTicks(TownySettings.getShortInterval() + 5));
						Towny.getPlugin().getServer().getScheduler().scheduleSyncDelayedTask(Towny.getPlugin(), new Runnable() {
							public void run() {
								List<PotionEffect> potionEffects = new ArrayList<>();
								potionEffects.add(new PotionEffect(PotionEffectType.POISON, effectDurationTicks, 1));
								potionEffects.add(new PotionEffect(PotionEffectType.SLOW, effectDurationTicks, 1));
								potionEffects.add(new PotionEffect(PotionEffectType.WEAKNESS, effectDurationTicks, 1));
								potionEffects.add(new PotionEffect(PotionEffectType.CONFUSION, effectDurationTicks, 1));
								player.addPotionEffects(potionEffects);
							}
						});
					}
				}
			} catch (Exception e) {
				try {
					System.out.println("Problem punishing peaceful player in siege zone - " + player.getName());
				} catch (Exception e2) {
					System.out.println("Problem punishing peaceful player in siege zone (could not read player name)");
				}
				e.printStackTrace();
			}
		}
	}
	
	public static void evaluatePeacefulTownNationAssignments() {
		TownyUniverse townyUniverse = TownyUniverse.getInstance();

		List<Town> towns = new ArrayList<>(townyUniverse.getDataSource().getTowns());
		ListIterator<Town> townItr = towns.listIterator();
		Town peacefulTown = null;
		Coord peacefulTownCoord;
		Town guardianTown = null;
		double guardianTownSignificance = 0;
		double candidateTownSignificance;
		double candidateTownDistance;
		Coord candidateTownCoord;
		int guardianTownPopulationRequirement = TownySettings.getWarSiegePeacefulTownsGuardianTownPopulationRequirement();
		int guardianTownPlotsRequirement = TownySettings.getWarSiegePeacefulTownsGuardianTownPlotsRequirement();
		int guardianTownMaxDistanceRequirementTownblocks = TownySettings.getWarSiegePeacefulTownsGuardianTownMinDistanceRequirement();

		while (townItr.hasNext()) {
			try {
				peacefulTown = townItr.next();
				if (!townyUniverse.getDataSource().hasTown(peacefulTown.getName())) 
					continue;
				if(!peacefulTown.isPeaceful())
					continue;
				if(!peacefulTown.hasHomeBlock())
					continue;
				peacefulTownCoord = peacefulTown.getHomeBlock().getCoord();

				//Find guardian town
				List<Town> candidateTowns = new ArrayList<>(townyUniverse.getDataSource().getTowns());
				for(Town candidateTown: candidateTowns) {

					if(!candidateTown.isPeaceful()
						&& !candidateTown.hasSiege()
						&& candidateTown.hasHomeBlock()
						&& peacefulTown.getHomeBlock().getWorld().getName().equalsIgnoreCase(candidateTown.getHomeBlock().getWorld().getName())
						&& candidateTown.getNumResidents() >= guardianTownPopulationRequirement
						&& candidateTown.getTownBlocks().size() >= guardianTownPlotsRequirement)
					{
						//Check distance
						candidateTownCoord = candidateTown.getHomeBlock().getCoord();
						candidateTownDistance = Math.sqrt(Math.pow(candidateTownCoord.getX() - peacefulTownCoord.getX(), 2) + Math.pow(candidateTownCoord.getZ() - peacefulTownCoord.getZ(), 2));
						if(candidateTownDistance > guardianTownMaxDistanceRequirementTownblocks)
							continue;

						if(guardianTown == null) {
							//set as best candidate
							guardianTown = candidateTown;
							guardianTownSignificance = guardianTown.getNumResidents() * guardianTown.getTownBlocks().size();
						} else {
							//compare with previous best candidate
							candidateTownSignificance = candidateTown.getNumResidents() * candidateTown.getTownBlocks().size();
							if(candidateTownSignificance > guardianTownSignificance){
								guardianTown = candidateTown;
								guardianTownSignificance = candidateTownSignificance;
							}
						}
					}
				}

				//Did we find a guardian town which also has a nation ?
				if(guardianTown != null && guardianTown.hasNation()) {
					if (peacefulTown.hasNation()) {
						if (guardianTown.getNation() != peacefulTown.getNation()) {
							//Transfer peaceful town from one nation to another
							Nation previousNation = peacefulTown.getNation();
							townyUniverse.getDataSource().removeTownFromNation(Towny.getPlugin(), peacefulTown, peacefulTown.getNation());
							townyUniverse.getDataSource().addTownToNation(Towny.getPlugin(), peacefulTown, guardianTown.getNation());
							TownyMessaging.sendGlobalMessage(String.format(TownySettings.getLangString("msg_war_siege_peaceful_town_changed_nation"), peacefulTown.getFormattedName(), previousNation.getFormattedName(), guardianTown.getNation().getFormattedName()));
						}
					} else {
						//Peaceful town joins nation
						townyUniverse.getDataSource().addTownToNation(Towny.getPlugin(), peacefulTown, guardianTown.getNation());
						TownyMessaging.sendGlobalMessage(String.format(TownySettings.getLangString("msg_war_siege_peaceful_town_joined_nation"), peacefulTown.getFormattedName(), guardianTown.getNation().getFormattedName()));
					}
				} else {
					if(peacefulTown.hasNation()) {
						//Peaceful town leaves nation
						Nation previousNation = peacefulTown.getNation();
						townyUniverse.getDataSource().removeTownFromNation(Towny.getPlugin(), peacefulTown, peacefulTown.getNation());
						TownyMessaging.sendGlobalMessage(String.format(TownySettings.getLangString("msg_war_siege_peaceful_town_left_nation"), peacefulTown.getFormattedName(), previousNation.getFormattedName()));
					}
				}
			} catch (Exception e) {
				try {
					System.out.println("Problem evaluating peaceful town nation assignment for - " + peacefulTown.getName());
				} catch (Exception e2) {
					System.out.println("Problem evaluating peaceful town nation assignment (could not read town name)");
				}
				e.printStackTrace();
			}
		}
	}
}
