package com.palmergames.bukkit.townywar.listener;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.entity.Player;
import org.bukkit.event.CustomEventListener;
import org.bukkit.event.Event;

import com.palmergames.bukkit.towny.NotRegisteredException;
import com.palmergames.bukkit.towny.Towny;
import com.palmergames.bukkit.towny.TownyException;
import com.palmergames.bukkit.towny.TownyMessaging;
import com.palmergames.bukkit.towny.TownySettings;
import com.palmergames.bukkit.towny.command.TownCommand;
import com.palmergames.bukkit.towny.object.Nation;
import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.towny.object.Town;
import com.palmergames.bukkit.towny.object.TownBlock;
import com.palmergames.bukkit.towny.object.TownyUniverse;
import com.palmergames.bukkit.towny.object.TownyWorld;
import com.palmergames.bukkit.towny.object.WorldCoord;
import com.palmergames.bukkit.towny.tasks.TownClaim;
import com.palmergames.bukkit.townywar.CellUnderAttack;
import com.palmergames.bukkit.townywar.TownyWar;
import com.palmergames.bukkit.townywar.event.CellAttackCanceledEvent;
import com.palmergames.bukkit.townywar.event.CellAttackEvent;
import com.palmergames.bukkit.townywar.event.CellDefendedEvent;
import com.palmergames.bukkit.townywar.event.CellWonEvent;

public class TownyWarCustomListener extends CustomEventListener {
	private final Towny plugin;

	public TownyWarCustomListener(Towny instance) {
		plugin = instance;
	}
	
	@Override
	public void onCustomEvent(Event event) {
		if (event.getEventName().equals("CellAttack")) {
			CellAttackEvent cellAttackEvent = (CellAttackEvent)event;
			try {
				CellUnderAttack cell = cellAttackEvent.getData();
				TownyWar.registerAttack(cell);
			} catch (Exception e) {
				cellAttackEvent.setCancelled(true);
				cellAttackEvent.setReason(e.getMessage());
			}
		} else if (event.getEventName().equals("CellDefended")) {
			CellDefendedEvent cellDefendedEvent = (CellDefendedEvent)event;
			Player player = cellDefendedEvent.getPlayer();
			CellUnderAttack cell = cellDefendedEvent.getCell().getAttackData();
			
			TownyUniverse universe = plugin.getTownyUniverse();
			try {
				TownyWorld world = TownyUniverse.getWorld(cell.getWorldName());
				WorldCoord worldCoord = new WorldCoord(world, cell.getX(), cell.getZ());
				universe.removeWarZone(worldCoord);
				
				plugin.updateCache(worldCoord);
			} catch (NotRegisteredException e) {
				e.printStackTrace();
			}
			
			String playerName;
			if (player == null) {
				playerName = "Greater Forces";
			} else {
				playerName = player.getName();
				try {
					playerName = plugin.getTownyUniverse().getResident(player.getName()).getFormattedName();
				} catch (TownyException e) {
				}
			}
			
			plugin.getServer().broadcastMessage(String.format(TownySettings.getLangString("msg_enemy_war_area_defended"),
					playerName,
					cell.getCellString()));
		} else if (event.getEventName().equals("CellWon")) {
			CellWonEvent cellWonEvent = (CellWonEvent)event;
			CellUnderAttack cell = cellWonEvent.getCellAttackData();
			
			TownyUniverse universe = plugin.getTownyUniverse();
			try {
				Resident resident = universe.getResident(cell.getNameOfFlagOwner());
				Town town = resident.getTown();
				Nation nation = town.getNation();
				
				TownyWorld world = TownyUniverse.getWorld(cell.getWorldName());
				WorldCoord worldCoord = new WorldCoord(world, cell.getX(), cell.getZ());
				universe.removeWarZone(worldCoord);
				
				TownBlock townBlock = worldCoord.getTownBlock();
				universe.removeTownBlock(townBlock);
				
				try {
					List<WorldCoord> selection = new ArrayList<WorldCoord>();
					selection.add(worldCoord);
					TownCommand.checkIfSelectionIsValid(town, selection, false, 0, false);
					new TownClaim(plugin, null, town, selection, true, false).start();
					
					//TownCommand.townClaim(town, worldCoord);
					//TownyUniverse.getDataSource().saveTown(town);
					//TownyUniverse.getDataSource().saveWorld(world);
					
					//TODO
					//PlotCommand.plotClaim(resident, worldCoord);
					//TownyUniverse.getDataSource().saveResident(resident);
					//TownyUniverse.getDataSource().saveWorld(world);
				} catch (TownyException te) {
					// Couldn't claim it.
				}
				
				plugin.updateCache(worldCoord);
				
				TownyMessaging.sendGlobalMessage(String.format(TownySettings.getLangString("msg_enemy_war_area_won"),
						resident.getFormattedName(),
						(nation.hasTag() ? nation.getTag() : nation.getFormattedName()),
						cell.getCellString()));
			} catch (NotRegisteredException e) {
				e.printStackTrace();
			}
		} else if (event.getEventName().equals("CellAttackCanceled")) {
			System.out.println("CellAttackCanceled");
			CellAttackCanceledEvent cancelCellAttackerEvent = (CellAttackCanceledEvent)event;
			CellUnderAttack cell = cancelCellAttackerEvent.getCell();
			
			TownyUniverse universe = plugin.getTownyUniverse();
			try {
				TownyWorld world = TownyUniverse.getWorld(cell.getWorldName());
				WorldCoord worldCoord = new WorldCoord(world, cell.getX(), cell.getZ());
				universe.removeWarZone(worldCoord);
				plugin.updateCache(worldCoord);
			} catch (NotRegisteredException e) {
				e.printStackTrace();
			}
			System.out.println(cell.getCellString());
		}
	}
}
