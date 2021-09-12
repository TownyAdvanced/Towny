package com.palmergames.bukkit.towny.war.eventwar.listeners;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import com.palmergames.bukkit.towny.TownyAPI;
import com.palmergames.bukkit.towny.TownyEconomyHandler;
import com.palmergames.bukkit.towny.TownyMessaging;
import com.palmergames.bukkit.towny.TownySettings;
import com.palmergames.bukkit.towny.TownyUniverse;
import com.palmergames.bukkit.towny.event.PreNewTownEvent;
import com.palmergames.bukkit.towny.event.TownPreClaimEvent;
import com.palmergames.bukkit.towny.event.nation.NationPreTownLeaveEvent;
import com.palmergames.bukkit.towny.event.player.PlayerKilledPlayerEvent;
import com.palmergames.bukkit.towny.event.town.TownLeaveEvent;
import com.palmergames.bukkit.towny.event.town.TownPreSetHomeBlockEvent;
import com.palmergames.bukkit.towny.event.town.TownPreUnclaimCmdEvent;
import com.palmergames.bukkit.towny.exceptions.NotRegisteredException;
import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.towny.object.Town;
import com.palmergames.bukkit.towny.object.Translatable;
import com.palmergames.bukkit.towny.object.Translation;
import com.palmergames.bukkit.towny.utils.CombatUtil;
import com.palmergames.bukkit.towny.war.eventwar.WarMetaDataController;
import com.palmergames.bukkit.towny.war.eventwar.WarType;
import com.palmergames.bukkit.towny.war.eventwar.instance.War;

public class EventWarListener implements Listener {

	public EventWarListener() {
		
	}

	@EventHandler(ignoreCancelled = true)
	public void onPlayerLogin(PlayerLoginEvent event) {
		War war = TownyUniverse.getInstance().getWarEvent(event.getPlayer());
		if (war == null)
			return;
		war.getWarParticipants().addOnlineWarrior(event.getPlayer());
		war.getScoreManager().sendScores(event.getPlayer(), 3);
		
	}
	
	@EventHandler(ignoreCancelled = true)
	public void onPlayerLogout(PlayerQuitEvent event) {
		War war = TownyUniverse.getInstance().getWarEvent(event.getPlayer());
		if (war == null)
			return;
		war.getWarParticipants().removeOnlineWarrior(event.getPlayer());		
	}
	
	@EventHandler
	private void onPlayerKillsPlayer(PlayerKilledPlayerEvent event) {
		Resident killerRes = event.getKillerRes();
		Resident victimRes = event.getVictimRes();
		War war = TownyUniverse.getInstance().getWarEvent(event.getKiller());
		War victimWar = TownyUniverse.getInstance().getWarEvent(event.getVictim());

		if (war == null || victimWar == null)
			return; // One of the players is not in a war.
		if (!war.getWarUUID().equals(victimWar.getWarUUID()))
			return; // The wars are not the same war.
		if (CombatUtil.isAlly(killerRes.getName(), victimRes.getName()) && war.getWarType() != WarType.RIOT)
			return; // They are allies and this was a friendly fire kill.
		
		/*
		 * Handle lives being lost, for wars without unlimited lives.
		 */
		if (war.getWarType().residentLives != -1){
			residentLosesALife(victimRes, killerRes, war, event);
		}
		
		/*
		 * Handle death payments. 
		 */
		double price = TownySettings.getWartimeDeathPrice();
		double townPrice = 0;
		if (!victimRes.getAccount().canPayFromHoldings(price)) {
			townPrice = price - victimRes.getAccount().getHoldingBalance();
			price = victimRes.getAccount().getHoldingBalance();
		}

		if (price > 0) {
			if (!TownySettings.isEcoClosedEconomyEnabled()){
				victimRes.getAccount().payTo(price, killerRes, "Death Payment (War)");
				TownyMessaging.sendMsg(killerRes, Translatable.of("msg_you_robbed_player", victimRes.getName(), TownyEconomyHandler.getFormattedBalance(price)));
				TownyMessaging.sendMsg(victimRes, Translatable.of("msg_player_robbed_you", killerRes.getName(), TownyEconomyHandler.getFormattedBalance(price)));
			} else {
				victimRes.getAccount().withdraw(price, "Death Payment (War)");
				TownyMessaging.sendMsg(victimRes, Translatable.of("msg_you_lost_money", TownyEconomyHandler.getFormattedBalance(price)));
			}
		}

		// Resident doesn't have enough funds.
		if (townPrice > 0 && victimRes.hasTown()) {
			Town town = TownyAPI.getInstance().getResidentTownOrNull(victimRes);
			if (!town.getAccount().canPayFromHoldings(townPrice)) {
				// Town doesn't have enough funds.
				townPrice = town.getAccount().getHoldingBalance();
				try {
					war.getWarZoneManager().remove(town, killerRes.getTown());
				} catch (NotRegisteredException ignored) {}
			} else if (!TownySettings.isEcoClosedEconomyEnabled()){
				TownyMessaging.sendPrefixedTownMessage(town, Translatable.of("msg_player_couldnt_pay_player_town_bank_paying_instead", victimRes.getName(), killerRes.getName(), townPrice));
				town.getAccount().payTo(townPrice, killerRes, String.format("Death Payment (War) (%s couldn't pay)", victimRes.getName()));
			} else {
				TownyMessaging.sendPrefixedTownMessage(town, Translatable.of("msg_player_couldnt_pay_player_town_bank_paying_instead", victimRes.getName(), killerRes.getName(), townPrice));
				town.getAccount().withdraw(townPrice, String.format("Death Payment (War) (%s couldn't pay)", victimRes.getName()));
			}
		}
	}
	
	private void residentLosesALife(Resident victimRes, Resident killerRes, War war, PlayerKilledPlayerEvent event) {
	
		int victimLives = war.getWarParticipants().getLives(victimRes); // Use a variable for this because it will be lost once takeLife(victimRes) is called.

		/*
		 * Take a life off of the victim no matter what type of war it is.
		 */
		war.getWarParticipants().takeLife(victimRes);
		
		/*
		 * Someone is being removed from the war.
		 */
		if (victimLives == 0)
			residentLostLastLife(victimRes, killerRes, war);
	
		/*
		 * Give the killer some points. 
		 */
		if (war.getWarType().pointsPerKill > 0)
			war.getScoreManager().residentScoredKillPoints(victimRes, killerRes, event.getLocation());
	}
	
	private void residentLostLastLife(Resident victimRes, Resident killerRes, War war) {

		/*
		 * Remove the resident from the war, handling kings and mayors if monarchdeath is enabled.
		 */
		switch (war.getWarType()) {
		
			case RIOT:
				try {
					TownyMessaging.sendPrefixedTownMessage(killerRes.getTown(), victimRes.getName() + " has run out of lives and is eliminated from the " + war.getWarName());
				} catch (NotRegisteredException ignored) {}
				war.getWarParticipants().remove(victimRes);
				break;
			case NATIONWAR:
			case WORLDWAR:
				/*
				 * Look to see if the king's death would remove a nation from the war.
				 */
				if (war.getWarType().hasMayorDeath && victimRes.hasNation() && victimRes.isKing() && killerRes.hasTown()) {
					TownyMessaging.sendGlobalMessage(Translatable.of("MSG_WAR_KING_KILLED", victimRes.getNationOrNull().getName()));
					/*
					 * Remove the king's nation from the war. Where-in the king will be removed with the rest of the residents.
					 */
					war.getWarZoneManager().remove(victimRes.getNationOrNull(), killerRes.getTownOrNull());
	
				/*
				 * Look to see if the mayor's death would remove a town from the war.
				 */
				} else if (war.getWarType().hasMayorDeath && victimRes.hasTown() && victimRes.isMayor() && killerRes.hasTown()) {
					TownyMessaging.sendGlobalMessage(Translatable.of("MSG_WAR_MAYOR_KILLED", victimRes.getTownOrNull().getName()));
					/*
					 * Remove the mayor's town from the war. Where-in the mayor will be removed with the rest of the residents.
					 */
					war.getWarZoneManager().remove(victimRes.getTownOrNull(), killerRes.getTownOrNull());
					
				/*
				 * Handle regular resident removal when they've run out of lives.	
				 */
				} else {
					TownyMessaging.sendPrefixedTownMessage(victimRes.getTownOrNull(), victimRes.getName() + " has run out of lives and is eliminated from the " + war.getWarName());
					TownyMessaging.sendPrefixedTownMessage(killerRes.getTownOrNull(), victimRes.getName() + " has run out of lives and is eliminated from the " + war.getWarName());
					war.getWarParticipants().remove(victimRes);
					
					// Test if this was the last resident of the town to have any lives left.
					int residentsWithLives = 0;
					for (Resident res : victimRes.getTownOrNull().getResidents()) {
						if (WarMetaDataController.getResidentLives(res) > 0)
							residentsWithLives++;
					}
					if (residentsWithLives == 0)
						war.getWarZoneManager().remove(victimRes.getTownOrNull(), killerRes.getTownOrNull());
					
				}
				break;
			case CIVILWAR:
			case TOWNWAR:
				try {
					/*
					 * Look to see if the mayor's death would remove a town from the war.
					 */
					if (war.getWarType().hasMayorDeath && victimRes.hasTown() && victimRes.isMayor()) {
						TownyMessaging.sendGlobalMessage(Translation.of("MSG_WAR_MAYOR_KILLED", victimRes.getTown().getName()));
						/*
						 * Remove the mayor's town from the war. Where-in the mayor will be removed with the rest of the residents.
						 */
						war.getWarZoneManager().remove(victimRes.getTown(), killerRes.getTown());

					/*
					 * Handle regular resident removal when they've run out of lives.	
					 */
					} else {
						TownyMessaging.sendPrefixedTownMessage(victimRes.getTown(), victimRes.getName() + " has run out of lives and is eliminated from the " + war.getWarName());
						TownyMessaging.sendPrefixedTownMessage(killerRes.getTown(), victimRes.getName() + " has run out of lives and is eliminated from the " + war.getWarName());
						war.getWarParticipants().remove(victimRes);
						
						// Test if this was the last resident of the town to have any lives left.
						int residentsWithLives = 0;
						for (Resident res : victimRes.getTownOrNull().getResidents()) {
							if (WarMetaDataController.getResidentLives(res) > 0)
								residentsWithLives++;
						}
						if (residentsWithLives == 0)
							war.getWarZoneManager().remove(victimRes.getTownOrNull(), killerRes.getTownOrNull());
					}
				} catch (NotRegisteredException ignored) {}
				break;
		}
	}
	
	@EventHandler
	public void onTownMoveHomeblock(TownPreSetHomeBlockEvent event) {
		if (event.getTown().hasActiveWar()) {
			event.setCancelled(true);
			event.setCancelMessage(Translation.of("msg_war_cannot_do"));
		}
	}
	
	@EventHandler
	public void onNewTown(PreNewTownEvent event) { // TODO: Make this configurable based on whether there is a world-war or not.
		if (TownyAPI.getInstance().isWarTime()) {
			event.setCancelled(true);
			event.setCancelMessage(Translation.of("msg_war_cannot_do"));
		}
	}
	
	@EventHandler
	public void onTownLeave(TownLeaveEvent event) {
		if (event.getTown().hasActiveWar()) {
			event.setCancelled(true);
			event.setCancelMessage(Translation.of("msg_war_cannot_do"));
		}
	}
	
	@EventHandler
	public void onTownClaim(TownPreClaimEvent event) {
		if (event.getTown().hasActiveWar()) {
			event.setCancelled(true);
			event.setCancelMessage(Translation.of("msg_war_cannot_do"));
		}
	}

	@EventHandler
	public void onTownUnclaim(TownPreUnclaimCmdEvent event) {
		if (event.getTown().hasActiveWar()) {
			event.setCancelled(true);
			event.setCancelMessage(Translation.of("msg_war_cannot_do"));
		}
	}

	@EventHandler
	public void onTownLeavesNation(NationPreTownLeaveEvent event) { // Also picks up towns being kicked using /n kick.
		if (event.getTown().hasActiveWar()) {
			event.setCancelled(true);
			event.setCancelMessage(Translation.of("msg_war_cannot_do"));
		}
	}
}

