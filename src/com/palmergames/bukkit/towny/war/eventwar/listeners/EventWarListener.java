package com.palmergames.bukkit.towny.war.eventwar.listeners;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;

import com.palmergames.bukkit.towny.TownyAPI;
import com.palmergames.bukkit.towny.TownyEconomyHandler;
import com.palmergames.bukkit.towny.TownyMessaging;
import com.palmergames.bukkit.towny.TownySettings;
import com.palmergames.bukkit.towny.TownyUniverse;
import com.palmergames.bukkit.towny.event.PreNewTownEvent;
import com.palmergames.bukkit.towny.event.TownPreClaimEvent;
import com.palmergames.bukkit.towny.event.nation.NationPreTownLeaveEvent;
import com.palmergames.bukkit.towny.event.player.PlayerKilledPlayerEvent;
import com.palmergames.bukkit.towny.event.time.NewHourEvent;
import com.palmergames.bukkit.towny.event.town.TownLeaveEvent;
import com.palmergames.bukkit.towny.event.town.TownPreSetHomeBlockEvent;
import com.palmergames.bukkit.towny.event.town.TownPreUnclaimCmdEvent;
import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.towny.object.Town;
import com.palmergames.bukkit.towny.object.Translatable;
import com.palmergames.bukkit.towny.object.Translation;
import com.palmergames.bukkit.towny.utils.CombatUtil;
import com.palmergames.bukkit.towny.war.eventwar.WarBooks;
import com.palmergames.bukkit.towny.war.eventwar.WarType;
import com.palmergames.bukkit.towny.war.eventwar.WarUtil;
import com.palmergames.bukkit.towny.war.eventwar.instance.War;
import com.palmergames.bukkit.util.BookFactory;

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
	public void onNewHourEvent(NewHourEvent event) {
		for (War war : TownyUniverse.getInstance().getWars()) {
			ItemStack book = BookFactory.makeBook(war.getWarName(), "War Continues", WarBooks.warUpdateBook(war));
			war.getWarParticipants().getOnlineWarriors().stream()
				.forEach(res -> res.getPlayer().getInventory().addItem(book));
		}
	}
	
	@EventHandler
	private void onPlayerKillsPlayer(PlayerKilledPlayerEvent event) {
		Resident killerRes = event.getKillerRes();
		Resident victimRes = event.getVictimRes();
		War war = TownyUniverse.getInstance().getWarEvent(killerRes);
		if (!WarUtil.hasSameWar(killerRes, victimRes))
			return;
		if (CombatUtil.isAlly(killerRes.getName(), victimRes.getName()) && (war.getWarType() != WarType.RIOT || war.getWarType() != WarType.CIVILWAR))
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

		Town victimTown = victimRes.getTownOrNull();
		Town killerTown = killerRes.getTownOrNull();
		// Resident doesn't have enough funds.
		if (townPrice > 0) {
			if (!victimTown.getAccount().canPayFromHoldings(townPrice)) {
				// Town doesn't have enough funds.
				townPrice = victimTown.getAccount().getHoldingBalance();
				TownyMessaging.sendPrefixedTownMessage(victimTown, Translatable.of("msg_player_couldnt_pay_player_town_bank_paying_instead", victimRes.getName(), killerRes.getName(), townPrice));
				TownyMessaging.sendPrefixedTownMessage(killerTown, String.format("Town could not pay death costs, removing %s from the war.", victimTown.getName()));
				TownyMessaging.sendPrefixedTownMessage(victimTown, String.format("Town could not pay death costs, removing %s from the war.", victimTown.getName()));
				victimTown.getAccount().payTo(townPrice, killerRes, String.format("Death Payment (War) (%s couldn't pay)", victimRes.getName()));
				war.getWarZoneManager().remove(victimTown, killerTown);
			} else if (!TownySettings.isEcoClosedEconomyEnabled()){
				TownyMessaging.sendPrefixedTownMessage(victimTown, Translatable.of("msg_player_couldnt_pay_player_town_bank_paying_instead", victimRes.getName(), killerRes.getName(), townPrice));
				victimTown.getAccount().payTo(townPrice, killerRes, String.format("Death Payment (War) (%s couldn't pay)", victimRes.getName()));
			} else {
				TownyMessaging.sendPrefixedTownMessage(victimTown, Translatable.of("msg_player_couldnt_pay_player_town_bank_paying_instead", victimRes.getName(), killerRes.getName(), townPrice));
				victimTown.getAccount().withdraw(townPrice, String.format("Death Payment (War) (%s couldn't pay)", victimRes.getName()));
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
			residentLostLastLife(victimRes, killerRes.getTownOrNull(), war);
	
		/*
		 * Give the killer some points. 
		 */
		if (war.getWarType().pointsPerKill > 0)
			war.getScoreManager().residentScoredKillPoints(victimRes, killerRes, event.getLocation());
	}
	
	private void residentLostLastLife(Resident victimRes, Town killerTown, War war) {
		Town victimTown = victimRes.getTownOrNull();
		/*
		 * Remove the resident from the war, handling kings and mayors if monarchdeath is enabled.
		 */
		switch (war.getWarType()) {
		
			case RIOT:
			TownyMessaging.sendPrefixedTownMessage(killerTown, victimRes.getName() + " has run out of lives and is eliminated from the " + war.getWarName());
				war.getWarParticipants().remove(victimRes);
				war.checkEnd();
				break;
			case NATIONWAR:
			case WORLDWAR:
				/*
				 * Look to see if the king's death would remove a nation from the war.
				 */
				if (war.getWarType().hasMayorDeath && victimRes.isKing()) {
					TownyMessaging.sendGlobalMessage(Translatable.of("MSG_WAR_KING_KILLED", victimRes.getNationOrNull().getName()));
					/*
					 * Remove the king's nation from the war. Where-in the king will be removed with the rest of the residents.
					 */
					war.getWarZoneManager().remove(victimRes.getNationOrNull(), killerTown);
	
				/*
				 * Look to see if the mayor's death would remove a town from the war.
				 */
				} else if (war.getWarType().hasMayorDeath && victimRes.isMayor()) {
					TownyMessaging.sendGlobalMessage(Translatable.of("MSG_WAR_MAYOR_KILLED", victimTown.getName()));
					/*
					 * Remove the mayor's town from the war. Where-in the mayor will be removed with the rest of the residents.
					 */
					war.getWarZoneManager().remove(victimTown, killerTown);
					
				/*
				 * Handle regular resident removal when they've run out of lives.	
				 */
				} else {
					TownyMessaging.sendPrefixedTownMessage(victimTown, victimRes.getName() + " has run out of lives and is eliminated from the " + war.getWarName());
					TownyMessaging.sendPrefixedTownMessage(killerTown, victimRes.getName() + " has run out of lives and is eliminated from the " + war.getWarName());
					war.getWarParticipants().remove(victimRes);
					
					// Test if this was the last resident of the town to have any lives left.
					int residentsWithLives = 0;
					for (Resident res : victimTown.getResidents()) {
						if (war.getWarParticipants().getResidents().contains(res))
							residentsWithLives++;
					}
					if (residentsWithLives == 0)
						war.getWarZoneManager().remove(victimTown, killerTown);
					
				}
				break;
			case CIVILWAR:
			case TOWNWAR:
				/*
				 * Look to see if the mayor's death would remove a town from the war.
				 */
				if (war.getWarType().hasMayorDeath && victimRes.isMayor()) {
					TownyMessaging.sendGlobalMessage(Translation.of("MSG_WAR_MAYOR_KILLED", victimTown.getName()));
					/*
					 * Remove the mayor's town from the war. Where-in the mayor will be removed with the rest of the residents.
					 */
					war.getWarZoneManager().remove(victimTown, killerTown);
	
				/*
				 * Handle regular resident removal when they've run out of lives.	
				 */
				} else {
					TownyMessaging.sendPrefixedTownMessage(victimTown, victimRes.getName() + " has run out of lives and is eliminated from the " + war.getWarName());
					TownyMessaging.sendPrefixedTownMessage(killerTown, victimRes.getName() + " has run out of lives and is eliminated from the " + war.getWarName());
					war.getWarParticipants().remove(victimRes);
					
					// Test if this was the last resident of the town to have any lives left.
					int residentsWithLives = 0;
					for (Resident res : victimTown.getResidents()) {
						if (war.getWarParticipants().getResidents().contains(res))
							residentsWithLives++;
					}
					if (residentsWithLives == 0)
						war.getWarZoneManager().remove(victimTown, killerTown);
				}
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
	public void onNewTown(PreNewTownEvent event) {
		if (WarUtil.hasWorldWar(TownyAPI.getInstance().getTownyWorld(event.getPlayer().getWorld().getName()))) {
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

