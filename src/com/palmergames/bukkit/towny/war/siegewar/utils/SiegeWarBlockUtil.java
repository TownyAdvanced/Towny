package com.palmergames.bukkit.towny.war.siegewar.utils;

import com.palmergames.bukkit.towny.Towny;
import com.palmergames.bukkit.towny.TownyMessaging;
import com.palmergames.bukkit.towny.TownyUniverse;
import com.palmergames.bukkit.towny.exceptions.NotRegisteredException;
import com.palmergames.bukkit.towny.object.*;
import com.palmergames.bukkit.towny.war.siegewar.enums.SiegeStatus;
import com.palmergames.bukkit.towny.war.siegewar.locations.Siege;
import com.palmergames.bukkit.towny.war.siegewar.locations.SiegeZone;
import com.palmergames.bukkit.towny.war.siegewar.playeractions.*;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Banner;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;

import java.util.ArrayList;
import java.util.List;

public class SiegeWarBlockUtil {
	
	/*
	 * coloured banner - could be attack or invade
	 * white banner - could be surrender
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
			if (blockTypeName.contains("banner")) {
				return evaluateSiegeWarPlaceBannerRequest(player, block, blockTypeName, event, plugin);
			} else if (block.getType().equals(Material.CHEST)) {
				return evaluateSiegeWarPlaceChestRequest(player, block, event);
			} else {
				return false;
			}
		} catch (NotRegisteredException e) {
			TownyMessaging.sendErrorMsg(player, "Problem placing banner");
			e.printStackTrace();
			event.setCancelled(true);
			return true;
		}
	}

	//While a siege exists, nobody can destroy the siege banner
	//Returns skipAdditionalPermChecks
	public static boolean evaluateSiegeWarBreakBlockRequest(Player player, Block block, BlockBreakEvent event)  {
		String blockTypeName = block.getType().getKey().getKey();

		if (blockTypeName.contains("banner")) {
			if (isBlockNearAnActiveSiegeBanner(block)) {
				//This is not a siege banner
				return false;
			} else {
				//This block is the banner of an active siege
				event.setCancelled(true);
				TownyMessaging.sendErrorMsg(player, "\"This is a siege banner. While the siege is in progress, you cannot destroy blocks near it.");
				return true;
			}
		} else {
			return false;
		}
	}


	private static boolean isBlockNearAnActiveSiegeBanner(Block block) {
		//Look through all siege zones
		//Note that we don't just look at the town at the given location
		//....because mayor may have unclaimed the plot after the siege started

		//Location must ne nearby
		//Siege must be in progress
		//Siege zone must be active
		Location blockLocation = block.getLocation();
		Location flagLocation;

		for (SiegeZone siegeZone : com.palmergames.bukkit.towny.object.TownyUniverse.getDataSource().getSiegeZones()) {
			flagLocation = siegeZone.getFlagLocation();
			if (
				blockLocation.getWorld() == flagLocation.getWorld()
					&& (Math.abs(blockLocation.getX() - flagLocation.getX()) < 3)
					&& (Math.abs(blockLocation.getY() - flagLocation.getY()) < 3)
					&& (Math.abs(blockLocation.getZ() - flagLocation.getZ()) < 3)
			) {
				if (siegeZone.getSiege().getStatus() == SiegeStatus.IN_PROGRESS && siegeZone.isActive()) {
					return true;
				} 
			}
		}

		//No active siege banner found near given block
		return false;
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
			List<TownBlock> nearbyTownBlocks = getAdjacentTownBlocks(player, block);
			if (nearbyTownBlocks.size() == 0) {
				//No town blocks are nearby. Normal block placement
				return false;
			} else {
				//One or more town are nearby.
				if (blockTypeName.contains("white")
					&& ((Banner) block.getState()).getPatterns().size() == 0) {
					//White banner
					AbandonAttack.processAbandonSiegeRequest(player,
						block,
						nearbyTownBlocks,
						event);
					return true;
				} else {
					//Coloured banner
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

			if (townBlock.hasTown()) {
				return false;
			}

			Town townWhereBlockWasPlaced = null;
			try {
				townWhereBlockWasPlaced = townBlock.getTown();
			} catch (NotRegisteredException e) {
			}

			//If there is no siege, do normal block placement
			if (!townWhereBlockWasPlaced.hasSiege())
				return false;

			//During a siege or aftermath, all in-town banner placement is restricted to siege actions only
			if (blockTypeName.contains("white")
				&& ((Banner) block.getState()).getPatterns().size() == 0) {
				//White banner
				SurrenderTown.processTownSurrenderRequest(
					player,
					townWhereBlockWasPlaced,
					event);
				return true;
			} else {
				//Coloured banner
				InvadeTown.processInvadeTownRequest(
					plugin,
					player,
					townWhereBlockWasPlaced.getName(),
					event);
				return true;
			}
		}
	}


	private static List<TownBlock> getAdjacentTownBlocks(Player player, Block block) {
		TownyUniverse townyUniverse = TownyUniverse.getInstance();
		TownyWorld townyWorld;
		List<TownBlock> nearbyTownBlocks = new ArrayList<>();

		try {
			townyWorld = townyUniverse.getDataSource().getWorld(player.getWorld().getName());
		} catch (NotRegisteredException e) {
			return nearbyTownBlocks;
		}

		List<Coord> nearbyCoOrdinates =new ArrayList<>();
		Coord blockCoordinate = Coord.parseCoord(block);
		nearbyCoOrdinates.add(blockCoordinate.add(0,-1));
		nearbyCoOrdinates.add(blockCoordinate.add(0,1));
		nearbyCoOrdinates.add(blockCoordinate.add(1,0));
		nearbyCoOrdinates.add(blockCoordinate.add(-1,0));

		TownBlock nearbyTownBlock = null;
		for(Coord nearbyCoord: nearbyCoOrdinates){
			if(townyWorld.hasTownBlock(nearbyCoord)) {

				try {nearbyTownBlock = townyWorld.getTownBlock(nearbyCoord);
				} catch (NotRegisteredException e) {}

				if (nearbyTownBlock.hasTown()) {
					nearbyTownBlocks.add(nearbyTownBlock);
				}
			}
		}

		return nearbyTownBlocks;
	}

	private static boolean evaluateSiegeWarPlaceChestRequest(Player player,
													  Block block,
													  BlockPlaceEvent event) throws NotRegisteredException {

		//Get Town Where block was placed
		Town townWhereBlockWasPlaced;
		TownyUniverse townyUniverse = TownyUniverse.getInstance();
		TownyWorld world;
		try {
			world = townyUniverse.getDataSource().getWorld(player.getWorld().getName());
		} catch (NotRegisteredException e) {
			return false;
		}
		Coord coord = Coord.parseCoord(block);
		TownBlock townBlock = world.getTownBlock(coord);

		if (townBlock != null && townBlock.hasTown()) {
			townWhereBlockWasPlaced = townBlock.getTown();
		} else {
			return false;
		}

		/*
		 * During a siege or aftermath
		 * If any resident member of the attacking nations attempts to place a chest,
		 * it is evaluated as a siege action
		 */
		if (townWhereBlockWasPlaced.hasSiege()) {
			Resident resident = townyUniverse.getDataSource().getResident(player.getName());
			Siege siege = townWhereBlockWasPlaced.getSiege();

			if(resident.hasTown()
				&& resident.hasNation()
				&& siege.getSiegeZones().keySet().contains(resident.getTown().getNation())) {

				PlunderTown.processPlunderTownRequest(player, townWhereBlockWasPlaced.getName(), event);
				return true;
			} else {
				return false;
			}
		} else {
			return false;
		}

	}


	public static boolean doesPlayerHaveANonAirBlockAboveThem(Player player) {
		return doesLocationHaveANonAirBlockAboveIt(player.getLocation());
	}

	public static boolean doesBlockHaveANonAirBlockAboveIt(Block block) {
		return doesLocationHaveANonAirBlockAboveIt(block.getLocation());
	}

	private static boolean doesLocationHaveANonAirBlockAboveIt(Location location) {
		location = location.add(0,1,0);

		while(location.getY() < 256)
		{
			if(location.getBlock().getType() != Material.AIR)
			{
				return true;   //There is a non-air block above them
			}
			location.add(0,1,0);
		}
		return false;  //There is nothing but air above them
	}

}
