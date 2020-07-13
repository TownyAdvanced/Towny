package com.palmergames.bukkit.towny.war.siegewar.utils;

import com.palmergames.bukkit.towny.*;
import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.towny.object.TownBlock;
import com.palmergames.bukkit.towny.war.siegewar.objects.BattleSession;
import com.palmergames.bukkit.util.BukkitTools;
import com.palmergames.util.TimeMgmt;
import com.palmergames.util.TimeTools;
import org.bukkit.entity.Player;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.ArrayList;
import java.util.List;

public class SiegeWarBattleSessionUtil {

	public static final String METADATA_TAG_NAME= "towny.siegewar.battle.session";
	
	public static void evaluateBattleSessions() {
		for(Player player: BukkitTools.getOnlinePlayers()) {
			//Don't apply to towny admins
			if(TownyUniverse.getInstance().getPermissionSource().isTownyAdmin(player))
				continue;

			try {
				if (player.hasMetadata(METADATA_TAG_NAME)) {
					//PLAYER HAS SESSION
					BattleSession battleSession = (BattleSession) player.getMetadata(METADATA_TAG_NAME).get(0).value();

					//Delete session is deletion time has been reached
					if (battleSession.isExpired() && System.currentTimeMillis() >= battleSession.getDeletionTime()) {
						player.removeMetadata(METADATA_TAG_NAME, Towny.getPlugin());
						TownyMessaging.sendMsg(player, TownySettings.getLangString("msg_war_siege_battle_session_ended"));
						continue;
					}

					//No punish if player in in a peaceful town
					TownBlock townBlockAtPlayerLocation = TownyAPI.getInstance().getTownBlock(player.getLocation());
					if (townBlockAtPlayerLocation != null && townBlockAtPlayerLocation.getTown().isPeaceful()) {
						continue;
					}

					//No punish if player is in their own town
					Resident resident = TownyUniverse.getInstance().getDataSource().getResident(player.getName());
					if (resident.hasTown()
						&& townBlockAtPlayerLocation != null
						&& resident.getTown() == townBlockAtPlayerLocation.getTown()) {
						continue;
					}

					//No punish if player is far from all siege zones
					if (!SiegeWarDistanceUtil.isLocationInActiveSiegeZone(player.getLocation())) {
						continue;
					}

					//Warn if first warning time has been reached
					if (!battleSession.isFirstWarningGiven()) {
						if (System.currentTimeMillis() >= battleSession.getFirstWarningTime()) {
							String timeUntilExpiry = TimeMgmt.getFormattedTimeValue(TownySettings.getWarSiegeBattleSessionsFirstWarningMinutesToExpiry() * TimeMgmt.ONE_MINUTE_IN_MILLIS);
							TownyMessaging.sendMsg(player, String.format(TownySettings.getLangString("msg_war_siege_battle_session_warning"), timeUntilExpiry));
							battleSession.setFirstWarningGiven(true);
						} else {
							continue;
						}
					}

					//Warn if second warning time has been reached
					if (!battleSession.isSecondWarningGiven()) {
						//phase 2
						if (System.currentTimeMillis() >= battleSession.getSecondWarningTime()) {
							String timeUntilExpiry = TimeMgmt.getFormattedTimeValue(TownySettings.getWarSiegeBattleSessionsSecondWarningMinutesToExpiry() * TimeMgmt.ONE_MINUTE_IN_MILLIS);
							TownyMessaging.sendMsg(player, String.format(TownySettings.getLangString("msg_war_siege_battle_session_warning"), timeUntilExpiry));
							battleSession.setSecondWarningGiven(true);
						} else {
							continue;
						}
					}

					//Expire session if expiry time has been reached
					if (!battleSession.isExpired()) {
						//phase 3
						if (System.currentTimeMillis() >= battleSession.getExpiryTime()) {
							battleSession.setExpired(true);
						} else {
							continue;
						}
					}

					//Punish player
					int effectDurationTicks = (int) (TimeTools.convertToTicks(TownySettings.getShortInterval() + 5));
					Towny.getPlugin().getServer().getScheduler().scheduleSyncDelayedTask(Towny.getPlugin(), new Runnable() {
						public void run() {
							List<PotionEffect> potionEffects = new ArrayList<>();
							potionEffects.add(new PotionEffect(PotionEffectType.BLINDNESS, effectDurationTicks, 4));
							potionEffects.add(new PotionEffect(PotionEffectType.POISON, effectDurationTicks, 4));
							potionEffects.add(new PotionEffect(PotionEffectType.WEAKNESS, effectDurationTicks, 4));
							potionEffects.add(new PotionEffect(PotionEffectType.SLOW, effectDurationTicks, 2));
							player.addPotionEffects(potionEffects);
							player.setHealth(1);
						}
					});
					String timeRemainingString = TimeMgmt.getFormattedTimeValue(battleSession.getDeletionTime() - System.currentTimeMillis());
					TownyMessaging.sendMsg(player, String.format(TownySettings.getLangString("msg_war_siege_battle_session_expired"), timeRemainingString));

				} else {
					//PLAYER DOES NOT HAVE SESSION

					//No session if player in in a peaceful town
					TownBlock townBlockAtPlayerLocation = TownyAPI.getInstance().getTownBlock(player.getLocation());
					if (townBlockAtPlayerLocation != null && townBlockAtPlayerLocation.getTown().isPeaceful()) {
						continue;
					}

					//No session if player is in their own town
					Resident resident = TownyUniverse.getInstance().getDataSource().getResident(player.getName());
					if (resident.hasTown()
						&& townBlockAtPlayerLocation != null
						&& resident.getTown() == townBlockAtPlayerLocation.getTown()) {
						continue;
					}

					//No punish if player is far from all siege zones
					if (!SiegeWarDistanceUtil.isLocationInActiveSiegeZone(player.getLocation())) {
						continue;
					}

					//Create new battle session
					BattleSession battleSession = new BattleSession();
					int activePhaseDurationMillis = (int) (TownySettings.getWarSiegeBattleSessionsActivePhaseDurationMinutes() * TimeMgmt.ONE_MINUTE_IN_MILLIS);
					int expiredPhaseDurationMillis = (int) (TownySettings.getWarSiegeBattleSessionsExpiredPhaseDurationMinutes() * TimeMgmt.ONE_MINUTE_IN_MILLIS);
					long expiryTime = System.currentTimeMillis() + activePhaseDurationMillis;
					long deleteTime = expiryTime + expiredPhaseDurationMillis;
					long firstWarningTime = expiryTime - (int) (TownySettings.getWarSiegeBattleSessionsFirstWarningMinutesToExpiry() * TimeMgmt.ONE_MINUTE_IN_MILLIS);
					long secondWarningTime = expiryTime - (int) (TownySettings.getWarSiegeBattleSessionsSecondWarningMinutesToExpiry() * TimeMgmt.ONE_MINUTE_IN_MILLIS);

					battleSession.setExpiryTime(expiryTime);
					battleSession.setDeletionTime(deleteTime);
					battleSession.setFirstWarningTime(firstWarningTime);
					battleSession.setSecondWarningTime(secondWarningTime);

					player.setMetadata(METADATA_TAG_NAME, new FixedMetadataValue(Towny.getPlugin(), battleSession));

					String totalActiveTimeString = TimeMgmt.getFormattedTimeValue(TownySettings.getWarSiegeBattleSessionsActivePhaseDurationMinutes() * TimeMgmt.ONE_MINUTE_IN_MILLIS);
					String restTimeString = TimeMgmt.getFormattedTimeValue(TownySettings.getWarSiegeBattleSessionsExpiredPhaseDurationMinutes() * TimeMgmt.ONE_MINUTE_IN_MILLIS);
					TownyMessaging.sendMsg(player, String.format(TownySettings.getLangString("msg_war_siege_battle_session_started"), totalActiveTimeString, restTimeString));
				}
			} catch (Exception e) {
				try {
					System.out.println("Problem evaluating battle session player " + player.getName());
				} catch (Exception e2) {
					System.out.println("Problem evaluating battle session for a player (could not read player name)");
				}
				e.printStackTrace();
			}
		}
	}
}
