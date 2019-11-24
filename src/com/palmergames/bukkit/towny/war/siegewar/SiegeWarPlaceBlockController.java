package com.palmergames.bukkit.towny.war.siegewar;

import com.palmergames.bukkit.towny.Towny;
import com.palmergames.bukkit.towny.TownyMessaging;
import com.palmergames.bukkit.towny.TownySettings;
import com.palmergames.bukkit.towny.TownyUniverse;
import com.palmergames.bukkit.towny.exceptions.NotRegisteredException;
import com.palmergames.bukkit.towny.object.*;
import com.palmergames.bukkit.towny.war.siegewar.locations.Siege;
import com.palmergames.bukkit.towny.war.siegewar.playeractions.*;
import com.palmergames.bukkit.towny.war.siegewar.utils.SiegeWarBlockUtil;
import org.bukkit.Material;
import org.bukkit.block.Banner;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockPlaceEvent;

import java.util.List;

/**
 * @author Goosius
 */
public class SiegeWarPlaceBlockController {
	
	/*
	 * coloured standing banner - could be attack or invade
	 * white standing banner - could be surrender
	 * chest - could be plunder
	 *
	 * Return - skipOtherPerChecks
	 */
	public static boolean evaluateSiegeWarPlaceBlockRequest(Player player,
													 Block block,
													 BlockPlaceEvent event,
													 Towny plugin)
	{
		try {
			String blockTypeName = block.getType().getKey().getKey();
			if (blockTypeName.endsWith("banner") && !blockTypeName.contains("wall")) {
				return evaluateSiegeWarPlaceBannerRequest(player, block, blockTypeName, event, plugin);
			} else if (block.getType().equals(Material.CHEST)) {
				return evaluateSiegeWarPlaceChestRequest(player, block, event);
			} else {
				return false;
			}
		} catch (NotRegisteredException e) {
			TownyMessaging.sendErrorMsg(player, "Problem placing siege related block");
			e.printStackTrace();
			event.setCancelled(true);
			return true;
		}
	}


	private static boolean evaluateSiegeWarPlaceBannerRequest(Player player,
													   Block block,
													   String blockTypeName,
													   BlockPlaceEvent event,
													   Towny plugin)
	{
		TownyUniverse townyUniverse = TownyUniverse.getInstance();
		TownyWorld townyWorld;
		try {
			townyWorld = townyUniverse.getDataSource().getWorld(block.getWorld().getName());
		} catch (NotRegisteredException e) {
			event.setCancelled(true);
			return false;
		}

		Coord blockCoord = Coord.parseCoord(block);

		if(!townyWorld.hasTownBlock(blockCoord)) {
			//Wilderness found
			//Possible abandon or attack request
			List<TownBlock> nearbyTownBlocks = SiegeWarBlockUtil.getAdjacentTownBlocks(player, block);
			if (nearbyTownBlocks.size() == 0) {
				//No town blocks are nearby. Normal block placement
				return false;
			} else {
				//One or more town are nearby.
				if (blockTypeName.contains("white")
					&& ((Banner) block.getState()).getPatterns().size() == 0) {
					//White banner

					if (!TownySettings.getWarSiegeAbandonEnabled())
						return false;
						
					AbandonAttack.processAbandonSiegeRequest(player,
						block,
						nearbyTownBlocks,
						event);
					return true;
				} else {
					//Coloured banner

					if (!TownySettings.getWarSiegeAttackEnabled())
						return false;

					AttackTown.processAttackTownRequest(
						player,
						block,
						nearbyTownBlocks,
						event);
					return true;
				}
			}

		} else {
			TownBlock townBlock = null;
			try {
				townBlock = townyWorld.getTownBlock(blockCoord);
			} catch (NotRegisteredException e) {
			}

			if (!townBlock.hasTown()) {
				return false;
			}

			Town town = null;
			try {
				town = townBlock.getTown();
			} catch (NotRegisteredException e) {
			}

			//If there is no siege, do normal block placement
			if (!town.hasSiege())
				return false;

			//During a siege or aftermath, all in-town banner placement is restricted to siege actions only
			if (blockTypeName.contains("white")
				&& ((Banner) block.getState()).getPatterns().size() == 0) {
				//White banner
				if (!TownySettings.getWarSiegeSurrenderEnabled())
					return false;

				SurrenderTown.processTownSurrenderRequest(
					player,
					town,
					event);
				return true;
			} else {
				//Coloured banner
				if (!TownySettings.getWarSiegeInvadeEnabled())
					return false;
				
				InvadeTown.processInvadeTownRequest(
					plugin,
					player,
					town.getName(),
					event);
				return true;
			}
		}
	}


	private static boolean evaluateSiegeWarPlaceChestRequest(Player player,
													  Block block,
													  BlockPlaceEvent event) throws NotRegisteredException {
		if (!TownySettings.getWarSiegePlunderEnabled())
			return false;

		Town townWhereBlockWasPlaced;
		TownyUniverse townyUniverse = TownyUniverse.getInstance();
		TownyWorld world;
		try {
			world = townyUniverse.getDataSource().getWorld(player.getWorld().getName());
		} catch (NotRegisteredException e) {
			return false;
		}
		Coord coord = Coord.parseCoord(block);
		
		if (!world.hasTownBlock(coord)) 
			return false;
		
		TownBlock townBlock = world.getTownBlock(coord);
		
		if(townBlock.hasTown()) {
			townWhereBlockWasPlaced = townBlock.getTown();
		} else {
			return false;
		}

		/*
		 * During a siege or aftermath
		 * If any player from one of the attacking nations attempts to place a chest,
		 * it is evaluated as an attempted siege action
		 */
		if (townWhereBlockWasPlaced.hasSiege()) {
			Resident resident = townyUniverse.getDataSource().getResident(player.getName());
			Siege siege = townWhereBlockWasPlaced.getSiege();

			if(resident.hasTown()
				&& resident.hasNation()
				&& siege.getSiegeZones().containsKey(resident.getTown().getNation())) {

				PlunderTown.processPlunderTownRequest(player, townWhereBlockWasPlaced.getName(), event);
				return true;
			} else {
				return false;
			}
		} else {
			return false;
		}
	}
	
}
