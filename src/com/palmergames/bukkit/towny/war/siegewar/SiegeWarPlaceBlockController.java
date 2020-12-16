package com.palmergames.bukkit.towny.war.siegewar;

import com.palmergames.bukkit.towny.Towny;
import com.palmergames.bukkit.towny.TownyAPI;
import com.palmergames.bukkit.towny.TownyMessaging;
import com.palmergames.bukkit.towny.TownyUniverse;
import com.palmergames.bukkit.towny.exceptions.NotRegisteredException;
import com.palmergames.bukkit.towny.exceptions.TownyException;
import com.palmergames.bukkit.towny.object.TownBlock;
import com.palmergames.bukkit.towny.object.Translation;
import com.palmergames.bukkit.towny.object.Town;
import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.towny.object.Nation;
import com.palmergames.bukkit.towny.war.siegewar.enums.SiegeStatus;
import com.palmergames.bukkit.towny.war.siegewar.enums.SiegeWarPermissionNodes;
import com.palmergames.bukkit.towny.war.siegewar.objects.Siege;
import com.palmergames.bukkit.towny.war.siegewar.playeractions.*;
import com.palmergames.bukkit.towny.war.siegewar.utils.SiegeWarBlockUtil;
import com.palmergames.bukkit.towny.war.siegewar.utils.SiegeWarDistanceUtil;
import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.block.Banner;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockPlaceEvent;

import java.util.ArrayList;
import java.util.List;

/**
 * This class intercepts 'place block' events coming from the towny block listener class
 *
 * The class evaluates the event, and determines if it is siege related e.g.:
 * 1. A siege attack request  (place coloured banner outside town)
 * 2. A siege abandon request  (place white banner near attack banner)
 * 3. A town surrender request  (place white banner in town)
 * 4. A town invasion request (place chest near attack banner)
 * 5. A town plunder request (place coloured banner near attack banner)
 * 6. A siege-forbidden block
 * 7. None of the above
 * 
 * If the place block event is determined to be a siege action,
 * this class then calls an appropriate class/method in the 'playeractions' package
 *
 * @author Goosius
 */
public class SiegeWarPlaceBlockController {
	
	/**
	 * Evaluates a block placement request.
	 * If the block is a standing banner or chest, this method calls an appropriate private method.
	 *
	 * @param player The player placing the block
	 * @param block The block about to be placed
	 * @param event The event object related to the block placement    	
	 * @param plugin The Towny object
	 * @return true if subsequent perm checks for the event should be skipped
	 */
	public static boolean evaluateSiegeWarPlaceBlockRequest(Player player, Block block, BlockPlaceEvent event, Towny plugin) {
		
		try {
			Material mat = block.getType();
			//Banner placement
			if (Tag.BANNERS.isTagged(mat))
				return evaluatePlaceBanner(player, block, event, plugin);
	
			//Chest placement
			if (mat == Material.CHEST || mat == Material.TRAPPED_CHEST)
				return evaluatePlaceChest(player, block, event);
	
			//Check for forbidden block placement
			if(SiegeWarSettings.isWarSiegeZoneBlockPlacementRestrictionsEnabled() && TownyAPI.getInstance().isWilderness(block) && SiegeWarDistanceUtil.isLocationInActiveSiegeZone(block.getLocation())) {
				if(SiegeWarSettings.getWarSiegeZoneBlockPlacementRestrictionsMaterials().contains(mat)) {
					event.setCancelled(true);
					event.setBuild(false);
					TownyMessaging.sendErrorMsg(player, Translation.of("msg_war_siege_zone_block_placement_forbidden"));
					return true;
				}
			}
		} catch (TownyException e) {
			event.setCancelled(true);
//			event.setCancelMessage(e.getMessage()); TODO: replace with TownyBuildEvent
		}
		return false;
		
	}

	/**
	 * Evaluates a banner placement request. Determines which type of banner this
	 * is, and where it is being placed. Then calls an appropriate private method.
	 * 
	 * @throws TownyException thrown when the banner is not allowed to be placed.
	 */
	private static boolean evaluatePlaceBanner(Player player, Block block, BlockPlaceEvent event, Towny plugin)
			throws TownyException {

		// All outcomes require a town.
		Resident resident = TownyUniverse.getInstance().getResident(player.getUniqueId());
		if (resident == null || !resident.hasTown())
			throw new TownyException(Translation.of("msg_err_siege_war_action_not_a_town_member"));

		/*
		 * The banner is being placed in the wilderness as either a Nation abandoning a
		 * siege, or beginning a siege.
		 * 
		 * All wilderness outcomes require a nation.
		 */
		if (TownyAPI.getInstance().isWilderness(block)) {

			// Fail early if this Resident's Town has no Nation.
			if (!resident.hasNation())
				throw new TownyException(Translation.of("msg_err_siege_war_action_not_a_nation_member"));

			Town town = null;
			Nation nation = null;
			try {
				town = resident.getTown();
				nation = town.getNation();
			} catch (NotRegisteredException ignored) {
			}

			if (isSurrenderBanner(block)) {
				// Nation abandoning the siege.

				if (!SiegeWarSettings.getWarSiegeAbandonEnabled())
					return false;

				// Fail early if the nation has no sieges.
				if (nation.getSieges().isEmpty())
					throw new TownyException(Translation.of("msg_err_siege_war_cannot_abandon_nation_not_attacking_zone"));

				// If player has no permission to abandon,send error
				if (!TownyUniverse.getInstance().getPermissionSource().testPermission(player, SiegeWarPermissionNodes.TOWNY_NATION_SIEGE_ABANDON.getNode()))
					throw new TownyException(Translation.of("msg_err_command_disable"));

				//Find the nearest siege to the player, owned by the nation.
				Siege nearestSiege = SiegeWarDistanceUtil.findNearestSiegeForNation(block, nation);
				
				//If there are no nearby siege zones,then regular block request
				if(nearestSiege == null)
					return false;
				
		        //If the siege is not in progress, send error
				if (nearestSiege.getStatus() != SiegeStatus.IN_PROGRESS)
					throw new TownyException(Translation.of("msg_err_siege_war_cannot_abandon_siege_over"));
				
				// Start abandoning the siege.
				AbandonAttack.attackerAbandon(nearestSiege);

			} else {
				// Nation starting a siege.
				return evaluatePlaceColouredBannerInWilderness(block, player, resident, town, nation, event, plugin);
			}

			
		/*
		 * The banner is being placed in a town, which means it is a Town
		 * trying to surrender their siege. 
		 */
		} else {
			Town town = TownyAPI.getInstance().getTown(block.getLocation());
			// Town found
			if (town != null 
					&& SiegeWarSettings.getWarSiegeSurrenderEnabled() 
					&& town.hasSiege() 
					&& isSurrenderBanner(block)) {
				if (!town.hasResident(resident))
		            throw new TownyException(Translation.of("msg_err_siege_war_cannot_surrender_not_your_town"));

				if (!TownyUniverse.getInstance().getPermissionSource().testPermission(player, SiegeWarPermissionNodes.TOWNY_TOWN_SIEGE_SURRENDER.getNode()))
	                throw new TownyException(Translation.of("msg_err_command_disable"));

				if(town.getSiege().getStatus() != SiegeStatus.IN_PROGRESS)
					throw new TownyException(Translation.of("msg_err_siege_war_cannot_surrender_siege_finished"));
				
				SurrenderTown.defenderSurrender(town.getSiege());
			}
		}
		return false;
	}

	/**
	 * Evaluates placing a coloured banner in the wilderness.
	 * Determines if the event will be considered as an attack or invade request.
	 */
	private static boolean evaluatePlaceColouredBannerInWilderness(Block block, Player player, Resident resident, Town attackingTown, Nation nation, BlockPlaceEvent event, Towny plugin) {
		try {
			// Fail early if this is not a siege-enabled world.
			if(!SiegeWarDistanceUtil.isSiegeWarEnabledInWorld(block.getWorld()))
				throw new TownyException(Translation.of("msg_err_siege_war_not_enabled_in_world"));

			
			List<TownBlock> nearbyCardinalTownBlocks = SiegeWarBlockUtil.getCardinalAdjacentTownBlocks(player, block);
	
			//If no townblocks are nearby, do normal block placement
			if (nearbyCardinalTownBlocks.size() == 0)
				return false;
	
			//Ensure that only one of the cardinal points has a townblock
			if(nearbyCardinalTownBlocks.size() > 1) {
				TownyMessaging.sendErrorMsg(player, Translation.of("msg_err_siege_war_too_many_adjacent_cardinal_town_blocks"));
				event.setBuild(false);
				event.setCancelled(true);
				return true;
			}
	
			//Get nearby town
			Town town;
			try {
				town = nearbyCardinalTownBlocks.get(0).getTown();
			} catch (NotRegisteredException e) {
				return false;
			}
	
			//Ensure that there is only one town adjacent
			List<TownBlock> adjacentTownBlocks = new ArrayList<>();
			adjacentTownBlocks.addAll(nearbyCardinalTownBlocks);
			adjacentTownBlocks.addAll(SiegeWarBlockUtil.getNonCardinalAdjacentTownBlocks(player, block));
			for(TownBlock adjacentTownBlock: adjacentTownBlocks) {
				try {
					if (adjacentTownBlock.getTown() != town) {
						TownyMessaging.sendErrorMsg(player, Translation.of("msg_err_siege_war_too_many_adjacent_towns"));
						event.setBuild(false);
						event.setCancelled(true);
						return true;
					}
				} catch (NotRegisteredException nre) {}
			}

			//If the town has a siege where the player's nation is already attacking, 
			//attempt invasion, otherwise attempt attack
			if(town.hasSiege() && town.getSiege().getAttackingNation() == nation) {

				if (!SiegeWarSettings.getWarSiegeInvadeEnabled())
					return false;

				if (!TownyUniverse.getInstance().getPermissionSource().testPermission(player, SiegeWarPermissionNodes.TOWNY_NATION_SIEGE_INVADE.getNode()))
					throw new TownyException(Translation.of("msg_err_command_disable"));

				if(attackingTown == town)
					throw new TownyException(Translation.of("msg_err_siege_war_cannot_invade_own_town"));

				
				InvadeTown.processInvadeTownRequest(
					plugin,
					attackingTown,
					town,
					event);

			} else {

				if (!SiegeWarSettings.getWarSiegeAttackEnabled())
					return false;

				if (!TownyUniverse.getInstance().getPermissionSource().testPermission(player, SiegeWarPermissionNodes.TOWNY_NATION_SIEGE_ATTACK.getNode()))
					throw new TownyException(Translation.of("msg_err_command_disable"));
				
				if (attackingTown== town)
	                throw new TownyException(Translation.of("msg_err_siege_war_cannot_attack_own_town"));

				
				if(SiegeWarBlockUtil.isSupportBlockUnstable(block)) {
					TownyMessaging.sendErrorMsg(player, Translation.of("msg_err_siege_war_banner_support_block_not_stable"));
					event.setBuild(false);
					event.setCancelled(true);
					return true;
				}

				AttackTown.processAttackTownRequest(
					player,
					block,
					nearbyCardinalTownBlocks.get(0),
					town,
					event);
			}

		} catch (TownyException x) {
			event.setBuild(false);
			event.setCancelled(true);
			TownyMessaging.sendErrorMsg(player, x.getMessage());
			return true;
		}

		return true;
	}

	/**
	 * Evaluates placing a chest.
	 * Determines if the event will be considered as a plunder request.
	 */
	private static boolean evaluatePlaceChest(Player player, Block block, BlockPlaceEvent event) {
		if (!SiegeWarSettings.getWarSiegePlunderEnabled() || !TownyAPI.getInstance().isWilderness(block))
			return false;

		List<TownBlock> nearbyTownBlocks = SiegeWarBlockUtil.getCardinalAdjacentTownBlocks(player, block);
		if (nearbyTownBlocks.size() == 0)
			return false;   //No town blocks are nearby. Normal block placement

		if (nearbyTownBlocks.size() > 1) {
			//More than one town block nearby. Error
			TownyMessaging.sendErrorMsg(player, Translation.of("msg_err_siege_war_too_many_town_blocks_nearby"));
			event.setBuild(false);
			event.setCancelled(true);
			return true;
		}

		//Get nearby town
		Town town = null;
		try {
			town = nearbyTownBlocks.get(0).getTown();
		} catch (NotRegisteredException ignored) {}

		//If there is no siege, do normal block placement
		if(!town.hasSiege())
			return false;

		//Attempt plunder.
		PlunderTown.processPlunderTownRequest(player, town, event);
		return true;

	}
	
	private static boolean isSurrenderBanner(Block block) {
		return block.getType() == Material.WHITE_BANNER  && ((Banner) block.getState()).getPatterns().size() == 0;
	}
}

