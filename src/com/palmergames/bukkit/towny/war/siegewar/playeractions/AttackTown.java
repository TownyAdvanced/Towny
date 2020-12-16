package com.palmergames.bukkit.towny.war.siegewar.playeractions;


import com.palmergames.bukkit.towny.TownyEconomyHandler;
import com.palmergames.bukkit.towny.TownyMessaging;
import com.palmergames.bukkit.towny.TownySettings;
import com.palmergames.bukkit.towny.TownyUniverse;
import com.palmergames.bukkit.towny.exceptions.EconomyException;
import com.palmergames.bukkit.towny.exceptions.TownyException;
import com.palmergames.bukkit.towny.object.Coord;
import com.palmergames.bukkit.towny.object.Nation;
import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.towny.object.Town;
import com.palmergames.bukkit.towny.object.TownBlock;
import com.palmergames.bukkit.towny.object.Translation;
import com.palmergames.bukkit.towny.war.siegewar.SiegeWarSettings;
import com.palmergames.bukkit.towny.war.siegewar.enums.SiegeStatus;
import com.palmergames.bukkit.towny.war.siegewar.objects.Siege;
import com.palmergames.bukkit.towny.war.siegewar.utils.SiegeWarDistanceUtil;
import com.palmergames.util.TimeMgmt;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockPlaceEvent;

/**
 * This class is responsible for processing requests to start siege attacks
 *
 * @author Goosius
 */
public class AttackTown {

	/**
	 * Process an attack town request
	 *
	 * This method does some final checks and if they pass, the attack is initiated.
	 *
	 * @param player the player who placed the attack banner
	 * @param block the attack banner 
	 * @param defendingTown the town about to be attacked
	 * @param event the place block event
	 */
    public static void processAttackTownRequest(Player player,
                                                Block block,
                                                TownBlock townBlock,
                                                Town defendingTown,
                                                BlockPlaceEvent event) {

        try {
			TownyUniverse universe = TownyUniverse.getInstance();
            Resident attackingResident = universe.getResident(player.getUniqueId());
            if (attackingResident == null)
            	throw new TownyException(Translation.of("msg_err_not_registered_1", player.getName()));
            
            Town townOfAttackingPlayer = attackingResident.getTown();




			if (defendingTown.hasSiege() && defendingTown.getSiege().getStatus().isActive())
				throw new TownyException(Translation.of("msg_err_siege_war_cannot_join_siege"));

			if (defendingTown.isNeutral())
				throw new TownyException(Translation.of("msg_war_siege_err_cannot_attack_peaceful_town"));

			if (defendingTown.isSiegeImmunityActive())
                throw new TownyException(Translation.of("msg_err_siege_war_cannot_attack_siege_immunity"));

			if (defendingTown.isRuined())
                throw new TownyException(Translation.of("msg_err_cannot_attack_ruined_town"));
			
            Nation nationOfAttackingPlayer= townOfAttackingPlayer.getNation();
            if (defendingTown.hasNation()) {
                Nation nationOfDefendingTown = defendingTown.getNation();

                if (nationOfAttackingPlayer == nationOfDefendingTown)
                    throw new TownyException(Translation.of("msg_err_siege_war_cannot_attack_town_in_own_nation"));

                if (!nationOfAttackingPlayer.hasEnemy(nationOfDefendingTown))
                    throw new TownyException(Translation.of("msg_err_siege_war_cannot_attack_non_enemy_nation"));
            }
            
            if (nationOfAttackingPlayer.isNationAttackingTown(defendingTown))
                throw new TownyException(Translation.of("msg_err_siege_war_nation_already_attacking_town"));

            if (TownySettings.isUsingEconomy() && !nationOfAttackingPlayer.getAccount().canPayFromHoldings(defendingTown.getSiegeCost()))
				throw new TownyException(Translation.of("msg_err_no_money"));

            if (!SiegeWarDistanceUtil.isUndergroundBannerControlEnabledInWorld(block.getWorld()) && SiegeWarDistanceUtil.doesLocationHaveANonAirBlockAboveIt(block.getLocation()))
                throw new TownyException(Translation.of("msg_err_siege_war_banner_must_be_placed_above_ground"));

            if(!SiegeWarDistanceUtil.isBannerToTownElevationDifferenceOk(block, townBlock)) {
				throw new TownyException(Translation.of("msg_err_siege_war_cannot_place_banner_far_above_town"));
			}

            if(nationOfAttackingPlayer.getNumActiveAttackSieges() >= SiegeWarSettings.getWarSiegeMaxActiveSiegeAttacksPerNation())
				throw new TownyException(Translation.of("msg_err_siege_war_nation_has_too_many_active_siege_attacks"));

			if (TownySettings.getNationRequiresProximity() > 0) {
				Coord capitalCoord = nationOfAttackingPlayer.getCapital().getHomeBlock().getCoord();
				Coord townCoord = defendingTown.getHomeBlock().getCoord();
				if (!nationOfAttackingPlayer.getCapital().getHomeBlock().getWorld().getName().equals(defendingTown.getHomeBlock().getWorld().getName())) {
					throw new TownyException(Translation.of("msg_err_nation_homeblock_in_another_world"));
				}
				double distance;
				distance = Math.sqrt(Math.pow(capitalCoord.getX() - townCoord.getX(), 2) + Math.pow(capitalCoord.getZ() - townCoord.getZ(), 2));
				if (distance > TownySettings.getNationRequiresProximity()) {
					throw new TownyException(Translation.of("msg_err_siege_war_town_not_close_enough_to_nation"));
				}
			}

            //Setup attack
            attackTown(block, nationOfAttackingPlayer, defendingTown);
        } catch (TownyException x) {
            TownyMessaging.sendErrorMsg(player, x.getMessage());
			event.setBuild(false);
			event.setCancelled(true);
        } catch (EconomyException x) {
			event.setBuild(false);
			event.setCancelled(true);
            TownyMessaging.sendErrorMsg(player, x.getMessage());
        }
    }


    private static void attackTown(Block block, Nation attackingNation, Town defendingTown) throws TownyException {
		//Create Siege
		String siegeName = attackingNation.getName() + "#vs#" + defendingTown.getName();
		TownyUniverse universe = TownyUniverse.getInstance();
		universe.getDataSource().newSiege(siegeName);
		Siege siege = universe.getDataSource().getSiege(siegeName);
		
		//Set values in siege object
		siege.setAttackingNation(attackingNation);
		siege.setDefendingTown(defendingTown);
		siege.setStatus(SiegeStatus.IN_PROGRESS);
		siege.setTownPlundered(false);
		siege.setTownInvaded(false);
		siege.setStartTime(System.currentTimeMillis());
		siege.setScheduledEndTime(
			(System.currentTimeMillis() +
				((long) (SiegeWarSettings.getWarSiegeMaxHoldoutTimeHours() * TimeMgmt.ONE_HOUR_IN_MILLIS))));
		siege.setActualEndTime(0);
		siege.setFlagLocation(block.getLocation());
		siege.setWarChestAmount(defendingTown.getSiegeCost());
		
		//Set values in town and nation objects
		defendingTown.setSiege(siege);
		attackingNation.addSiege(siege);
		
		defendingTown.getPermissions().explosion = true;
		defendingTown.getPermissions().pvp = true;
		
		//Pay into warchest
		if (TownySettings.isUsingEconomy()) {
			try {
				//Pay upfront cost into warchest now
				attackingNation.getAccount().withdraw(siege.getWarChestAmount(), "Cost of starting a siege.");
				String moneyMessage =
					String.format(
						Translation.of("msg_siege_war_attack_pay_war_chest"),
						attackingNation.getFormattedName(),
						TownyEconomyHandler.getFormattedBalance(siege.getWarChestAmount()));

				TownyMessaging.sendPrefixedNationMessage(attackingNation, moneyMessage);
				TownyMessaging.sendPrefixedTownMessage(defendingTown, moneyMessage);
			} catch (EconomyException e) {
				System.out.println("Problem paying into war chest");
				e.printStackTrace();
			}
		}

		//Save to DB
		universe.getDataSource().saveSiege(siege);
		universe.getDataSource().saveNation(attackingNation);
		universe.getDataSource().saveTown(defendingTown);
		universe.getDataSource().saveSiegeList();

		//Send global message;
		if (siege.getDefendingTown().hasNation()) {
			TownyMessaging.sendGlobalMessage(String.format(
				Translation.of("msg_siege_war_siege_started_nation_town"),
				attackingNation.getFormattedName(),
				defendingTown.getNation().getFormattedName(),
				defendingTown.getFormattedName()
			));
		} else {
			TownyMessaging.sendGlobalMessage(String.format(
				Translation.of("msg_siege_war_siege_started_neutral_town"),
				attackingNation.getFormattedName(),
				defendingTown.getFormattedName()
			));
		}
    }
}
