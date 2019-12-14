package com.palmergames.bukkit.towny.war.siegewar;

import com.palmergames.bukkit.towny.Towny;
import com.palmergames.bukkit.towny.TownyMessaging;
import com.palmergames.bukkit.towny.TownySettings;
import com.palmergames.bukkit.towny.TownyUniverse;
import com.palmergames.bukkit.towny.exceptions.NotRegisteredException;
import com.palmergames.bukkit.towny.object.*;
import com.palmergames.bukkit.towny.war.siegewar.enums.SiegeStatus;
import com.palmergames.bukkit.towny.war.siegewar.locations.Siege;
import com.palmergames.bukkit.towny.war.siegewar.locations.SiegeZone;
import com.palmergames.bukkit.towny.war.siegewar.playeractions.*;
import com.palmergames.bukkit.towny.war.siegewar.utils.SiegeWarBlockUtil;
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
 * 1. An attack request  (place coloured banner outside town)
 * 2. A siege abandon request  (place white banner near attack banner)
 * 3. A town surrender request  (place white banner in town)
 * 4. A town invasion request (place chest in town)
 * 5. A town plunder request (place coloured banner in town)
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
					return evaluateSiegeWarPlaceBannerRequest(player, block, event, plugin);
				case CHEST:
				case TRAPPED_CHEST:
					return evaluateSiegeWarPlaceChestRequest(player, block, event);
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
	private static boolean evaluateSiegeWarPlaceBannerRequest(Player player,
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
				return evaluatePlaceWhiteBannerOutsideTown(block, player, event);
			} else {
				return evaluatePlaceColouredBannerOutsideTown(block, player, event);
			}
			
		} else {
			//Town block found
			
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

			if (block.getType() == Material.WHITE_BANNER  && ((Banner) block.getState()).getPatterns().size() == 0) {
				return evaluatePlaceWhiteBannerInTown(player, town, event);
			} else {
				return evaluatePlaceColouredBannerInTown(plugin, player, town, event);
			}
		}
	}
	
	/**
	 * Evaluates placing a white banner outside a town.
	 * Determines if the event will be considered as an abandon request.
	 */
	private static boolean evaluatePlaceWhiteBannerOutsideTown(Block block, Player player, BlockPlaceEvent event) {
		if (!TownySettings.getWarSiegeAbandonEnabled())
			return false;

		//Find the nearest siege zone to the player,from IN_PROGRESS sieges
		SiegeZone nearestSiegeZone = null;
		double distanceToNearestSiegeZone = -1;
		for(SiegeZone siegeZone: com.palmergames.bukkit.towny.TownyUniverse.getInstance().getDataSource().getSiegeZones()) {

			if(siegeZone.getSiege().getStatus() != SiegeStatus.IN_PROGRESS)
				continue;

			if (nearestSiegeZone == null) {
				nearestSiegeZone = siegeZone;
				distanceToNearestSiegeZone = block.getLocation().distance(nearestSiegeZone.getFlagLocation());
			} else {
				double distanceToNewTarget = block.getLocation().distance(siegeZone.getFlagLocation());
				if(distanceToNewTarget < distanceToNearestSiegeZone) {
					nearestSiegeZone = siegeZone;
					distanceToNearestSiegeZone = distanceToNewTarget;
				}
			}
		}

		//If there are no in-progress sieges at all,then regular block request
		if(nearestSiegeZone == null)
			return false;

		//If the player is too far from the nearest zone, then regular block request
		if(distanceToNearestSiegeZone > TownySettings.getTownBlockSize())
			return false;

		AbandonAttack.processAbandonSiegeRequest(player,
			nearestSiegeZone,
			event);

		return true;
	}

	/**
	 * Evaluates placing a coloured banner outside a town.
	 * Determines if the event will be considered as an attack request.
	 */
	private static boolean evaluatePlaceColouredBannerOutsideTown(Block block, Player player, BlockPlaceEvent event) {
		if (!TownySettings.getWarSiegeAttackEnabled())
			return false;

		List<TownBlock> nearbyTownBlocks = SiegeWarBlockUtil.getAdjacentTownBlocks(player, block);
		if (nearbyTownBlocks.size() == 0)
			return false;   //No town blocks are nearby. Normal block placement

		AttackTown.processAttackTownRequest(
			player,
			block,
			nearbyTownBlocks,
			event);

		return true;
	}

	/**
	 * Evaluates placing a white banner inside a town.
	 * Determines if the event will be considered as a surrender request.
	 */
    private static boolean evaluatePlaceWhiteBannerInTown(Player player, Town town, BlockPlaceEvent event) {
		if (!TownySettings.getWarSiegeSurrenderEnabled())
			return false;

		SurrenderTown.processTownSurrenderRequest(
			player,
			town,
			event);
		return true;
	}

	/**
	 * Evaluates placing a coloured banner inside a town.
	 * Determines if the event will be considered as an invade request.
	 * 
	 * The main verifications here are that a siege exists in the town (already checked) and
	 * the placer is a member of an attacking nation
	 */
	private static boolean evaluatePlaceColouredBannerInTown(Towny plugin, Player player, Town town, BlockPlaceEvent event) throws NotRegisteredException {
		if (!TownySettings.getWarSiegeInvadeEnabled())
			return false;

		TownyUniverse townyUniverse = TownyUniverse.getInstance();
		Resident resident = townyUniverse.getDataSource().getResident(player.getName());
		Siege siege = town.getSiege();

		if(resident.hasTown()
			&& resident.hasNation()
			&& siege.getSiegeZones().containsKey(resident.getTown().getNation())) {

			InvadeTown.processInvadeTownRequest(plugin, player, resident, town, siege, event);
			return true;
		} else {
			return false;
		}
	}
	
	/**
	 * Evaluates placing a chest inside a town.
	 * Determines if the event will be considered as a plunder request.
	 * 
	 * The main verifications here are that a siege exists in the town and
	 * the placer is a member of an attacking nation
	 */
	private static boolean evaluateSiegeWarPlaceChestRequest(Player player,
													  Block block,
													  BlockPlaceEvent event) throws NotRegisteredException {
		if (!TownySettings.getWarSiegePlunderEnabled())
			return false;

		Town town;
		TownyUniverse townyUniverse = TownyUniverse.getInstance();
		TownyWorld world = townyUniverse.getDataSource().getWorld(player.getWorld().getName());
		Coord coord = Coord.parseCoord(block);
		
		if (!world.hasTownBlock(coord)) 
			return false;
		
		TownBlock townBlock = world.getTownBlock(coord);
		
		if(townBlock.hasTown()) {
			town = townBlock.getTown();
		} else {
			return false;
		}
		
		if (town.hasSiege()) {
			Resident resident = townyUniverse.getDataSource().getResident(player.getName());
			Siege siege = town.getSiege();
	
			if(resident.hasTown()
				&& resident.hasNation()
				&& siege.getSiegeZones().containsKey(resident.getTown().getNation())) {

				PlunderTown.processPlunderTownRequest(player, resident, town, siege, event);
				return true;
			} else {
				return false;
			}
		} else {
			return false;
		}
	}
}
