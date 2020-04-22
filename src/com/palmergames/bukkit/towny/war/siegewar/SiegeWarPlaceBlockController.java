package com.palmergames.bukkit.towny.war.siegewar;

import com.palmergames.bukkit.towny.Towny;
import com.palmergames.bukkit.towny.TownyMessaging;
import com.palmergames.bukkit.towny.TownySettings;
import com.palmergames.bukkit.towny.TownyUniverse;
import com.palmergames.bukkit.towny.exceptions.NotRegisteredException;
import com.palmergames.bukkit.towny.exceptions.TownyException;
import com.palmergames.bukkit.towny.object.Coord;
import com.palmergames.bukkit.towny.object.TownBlock;
import com.palmergames.bukkit.towny.object.TownyWorld;
import com.palmergames.bukkit.towny.object.Town;
import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.towny.object.Nation;
import com.palmergames.bukkit.towny.war.siegewar.objects.SiegeDistance;
import com.palmergames.bukkit.towny.war.siegewar.playeractions.*;
import com.palmergames.bukkit.towny.war.siegewar.utils.SiegeWarBlockUtil;
import com.palmergames.bukkit.towny.war.siegewar.utils.SiegeWarDistanceUtil;
import org.bukkit.Material;
import org.bukkit.block.Banner;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockPlaceEvent;

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
 * 6. None of the above
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
	public static boolean evaluateSiegeWarPlaceBlockRequest(Player player,
													 Block block,
													 BlockPlaceEvent event,
													 Towny plugin)
	{
		try {
			switch(block.getType()) {
				case BLACK_BANNER:
				case BLUE_BANNER:
				case BROWN_BANNER:
				case CYAN_BANNER:
				case GRAY_BANNER:
				case GREEN_BANNER:
				case LIGHT_BLUE_BANNER:
				case LIGHT_GRAY_BANNER:
				case LIME_BANNER:
				case MAGENTA_BANNER:
				case ORANGE_BANNER:
				case PINK_BANNER:
				case PURPLE_BANNER:
				case RED_BANNER:
				case YELLOW_BANNER:
				case WHITE_BANNER:
					return evaluatePlaceBanner(player, block, event, plugin);
				case CHEST:
				case TRAPPED_CHEST:
					return evaluatePlaceChest(player, block, event);
				default:
					return false;
			}
		} catch (NotRegisteredException e) {
			TownyMessaging.sendErrorMsg(player, "Problem placing siege related block");
			e.printStackTrace();
			return false;
		}
	}

	/**
 	 * Evaluates a banner placement request.
     * Determines which type of banner this is, and where it is being placed.
	 * Then calls an appropriate private method.
 	*/
	private static boolean evaluatePlaceBanner(Player player,
											   Block block,
											   BlockPlaceEvent event,
											   Towny plugin) throws NotRegisteredException
	{
		TownyUniverse townyUniverse = TownyUniverse.getInstance();
		TownyWorld townyWorld = townyUniverse.getDataSource().getWorld(block.getWorld().getName());
		Coord blockCoord = Coord.parseCoord(block);

		if(!townyWorld.hasTownBlock(blockCoord)) {
			//Wilderness found
			if (block.getType() == Material.WHITE_BANNER  && ((Banner) block.getState()).getPatterns().size() == 0) {
				return evaluatePlaceWhiteBannerInWilderness(block, player, event);
			} else {
				return evaluatePlaceColouredBannerInWilderness(block, player, event, plugin);
			}
		} else {
			//Town block found 
			if (block.getType() == Material.WHITE_BANNER  && ((Banner) block.getState()).getPatterns().size() == 0) {
				return evaluatePlaceWhiteBannerInTown(player, blockCoord, event, townyWorld);
			} else {
				return false;
			}
		}
	}
	
	/**
	 * Evaluates placing a white banner in the wilderness.
	 * Determines if the event will be considered as an abandon request.
	 */
	private static boolean evaluatePlaceWhiteBannerInWilderness(Block block, Player player, BlockPlaceEvent event) {
		if (!TownySettings.getWarSiegeAbandonEnabled())
			return false;

		//Find the nearest siege zone to the player
		SiegeDistance nearestSiegeZoneDistance = SiegeWarDistanceUtil.findNearestSiegeDistance(block);
		
		//If there are no nearby siege zones,then regular block request
		if(nearestSiegeZoneDistance == null || nearestSiegeZoneDistance.getDistance() > TownySettings.getTownBlockSize())
			return false;
		
		AbandonAttack.processAbandonSiegeRequest(player,
			nearestSiegeZoneDistance.getSiege(),
			event);

		return true;
	}

	/**
	 * Evaluates placing a coloured banner in the wilderness.
	 * Determines if the event will be considered as an attack or invade request.
	 */
	private static boolean evaluatePlaceColouredBannerInWilderness(Block block, Player player, BlockPlaceEvent event, Towny plugin) {

		List<TownBlock> nearbyTownBlocks = SiegeWarBlockUtil.getAdjacentTownBlocks(player, block);
		if (nearbyTownBlocks.size() == 0)
			return false;   //No town blocks are nearby. Normal block placement

		if(nearbyTownBlocks.size() > 1) {
			//More than one town block nearby. Error
			TownyMessaging.sendErrorMsg(player, TownySettings.getLangString("msg_err_siege_war_too_many_town_blocks_nearby"));
			event.setBuild(false);
			event.setCancelled(true);
			return true;
		}
		
		//Get nearby town
		Town town = null;
		if(nearbyTownBlocks.get(0).hasTown()) {
			try {
				town = nearbyTownBlocks.get(0).getTown();
			} catch (NotRegisteredException e) {
				return false;
			}
		} else {
			return false;
		}

		//If the town has a siege where the player's nation is already attacking, 
		//attempt invasion, otherwise attempt attack
		TownyUniverse universe = TownyUniverse.getInstance();
		try {
			Resident resident = universe.getDataSource().getResident(player.getName());
			if(!resident.hasTown())
				throw new TownyException(TownySettings.getLangString("msg_err_siege_war_action_not_a_town_member"));

			Town townOfResident = resident.getTown();
			if(!townOfResident.hasNation())
				throw new TownyException(TownySettings.getLangString("msg_err_siege_war_action_not_a_nation_member"));

			Nation nationOfResident = townOfResident.getNation();
			if(town.hasSiege() && town.getSiege().getAttackingNation() == nationOfResident) {

				if (!TownySettings.getWarSiegeInvadeEnabled())
					return false;

				InvadeTown.processInvadeTownRequest(
					plugin,
					player,
					town,
					event);

			} else {

				if (!TownySettings.getWarSiegeAttackEnabled())
					return false;

				if(SiegeWarBlockUtil.isSupportBlockUnstable(block)) {
					TownyMessaging.sendErrorMsg(player, TownySettings.getLangString("msg_err_siege_war_banner_support_block_not_stable"));
					event.setBuild(false);
					event.setCancelled(true);
					return true;
				}

				AttackTown.processAttackTownRequest(
					player,
					block,
					nearbyTownBlocks.get(0),
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
	 * Evaluates placing a white banner inside a town.
	 * Determines if the event will be considered as a surrender request.
	 */
    private static boolean evaluatePlaceWhiteBannerInTown(Player player, Coord blockCoord, BlockPlaceEvent event, TownyWorld townyWorld) throws NotRegisteredException {
		if (!TownySettings.getWarSiegeSurrenderEnabled())
			return false;
		
		TownBlock townBlock = null;
		if(townyWorld.hasTownBlock(blockCoord)) {
			townBlock = townyWorld.getTownBlock(blockCoord);
		}

		if (townBlock == null) {
			return false;
		}

		Town town;
		if(townBlock.hasTown()) {
			town = townBlock.getTown();
		} else {
			return false;
		}

		//If there is no siege, do normal block placement
		if (!town.hasSiege())
			return false;
		
		SurrenderTown.processTownSurrenderRequest(
			player,
			town,
			event);
		return true;
	}
	
	/**
	 * Evaluates placing a chest.
	 * Determines if the event will be considered as a plunder request.
	 */
	private static boolean evaluatePlaceChest(Player player,
											  Block block,
											  BlockPlaceEvent event) throws NotRegisteredException {
		if (!TownySettings.getWarSiegePlunderEnabled())
			return false;

		TownyWorld townyWorld = TownyUniverse.getInstance().getDataSource().getWorld(block.getWorld().getName());
		Coord blockCoord = Coord.parseCoord(block);

		if(townyWorld.hasTownBlock(blockCoord))
			return false;   //The chest is being placed in a town. Normal block placement

		List<TownBlock> nearbyTownBlocks = SiegeWarBlockUtil.getAdjacentTownBlocks(player, block);
		if (nearbyTownBlocks.size() == 0)
			return false;   //No town blocks are nearby. Normal block placement

		if(nearbyTownBlocks.size() > 1) {
			//More than one town block nearby. Error
			TownyMessaging.sendErrorMsg(player, TownySettings.getLangString("msg_err_siege_war_too_many_town_blocks_nearby"));
			event.setBuild(false);
			event.setCancelled(true);
			return true;
		}

		//Get nearby town
		Town town = null;
		if(nearbyTownBlocks.get(0).hasTown()) {

			try {
				town = nearbyTownBlocks.get(0).getTown();
			} catch (NotRegisteredException e) {
				return false;
			}
		} else {
			return false;
		}

		//If the town has a siege, attempt plunder, otherwise return false
		if(town.hasSiege()) {
			PlunderTown.processPlunderTownRequest(
				player,
				town,
				event);
		} else {
			return false;
		}

		return true;

	}
}

