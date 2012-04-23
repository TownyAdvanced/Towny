/**
 * 
 */
package com.palmergames.bukkit.towny.tasks;

import static com.palmergames.bukkit.towny.object.TownyObservableType.PLAYER_LOGIN;

import org.bukkit.entity.Player;

import com.palmergames.bukkit.towny.Towny;
import com.palmergames.bukkit.towny.TownyEconomyHandler;
import com.palmergames.bukkit.towny.TownyMessaging;
import com.palmergames.bukkit.towny.TownySettings;
import com.palmergames.bukkit.towny.exceptions.AlreadyRegisteredException;
import com.palmergames.bukkit.towny.exceptions.EconomyException;
import com.palmergames.bukkit.towny.exceptions.NotRegisteredException;
import com.palmergames.bukkit.towny.object.Nation;
import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.towny.object.Town;
import com.palmergames.bukkit.towny.object.TownyUniverse;
import com.palmergames.bukkit.util.BukkitTools;


/**
 * @author ElgarL
 *
 */
public class OnPlayerLogin extends Thread{
	
	Towny plugin;
	TownyUniverse universe;
	volatile Player player;
	
	/**
	 * Constructor
	 * 
	 * @param plugin
	 * @param player
	 */
	public OnPlayerLogin(Towny plugin, Player player) {
		
		this.plugin = plugin;
		this.universe = plugin.getTownyUniverse();
		this.player = player;
	}

	@Override
	public void run() {
		
		Resident resident = null;

		if (!TownyUniverse.getDataSource().hasResident(player.getName())) {
			/*
			 * No record of this resident exists
			 * So create a fresh set of data.
			 */
			try {
				TownyUniverse.getDataSource().newResident(player.getName());
				resident = TownyUniverse.getDataSource().getResident(player.getName());
				
				TownyMessaging.sendMessage(player, TownySettings.getRegistrationMsg(player.getName()));
				resident.setRegistered(System.currentTimeMillis());
				if (!TownySettings.getDefaultTownName().equals(""))
					try {
						Town town = TownyUniverse.getDataSource().getTown(TownySettings.getDefaultTownName());
						town.addResident(resident);
						TownyUniverse.getDataSource().saveTown(town);
					} catch (NotRegisteredException ex) {
					} catch (AlreadyRegisteredException ex) {
					}

				TownyUniverse.getDataSource().saveResident(resident);
				TownyUniverse.getDataSource().saveResidentList();
				
			} catch (AlreadyRegisteredException ex) {
				// Should never happen
			} catch (NotRegisteredException ex) {
				// Should never happen
			}

		} else {
			/*
			 * This resident is known so fetch the data and update it.
			 */
			try {
				resident = TownyUniverse.getDataSource().getResident(player.getName());
				resident.setLastOnline(System.currentTimeMillis());

				TownyUniverse.getDataSource().saveResident(resident);
				
			} catch (NotRegisteredException ex) {
				// Should never happen
			}
		}

		if (resident != null)
			try {
				TownyMessaging.sendTownBoard(player, resident.getTown());
			} catch (NotRegisteredException ex) {
			}

		if (TownyUniverse.isWarTime())
			universe.getWarEvent().sendScores(player, 3);

		//Schedule to setup default modes when the player has finished loading
		if (BukkitTools.scheduleSyncDelayedTask(new SetDefaultModes(player.getName(), false), 1) == -1)
			TownyMessaging.sendErrorMsg("Could not set default modes for " + player.getName() + ".");
		
		// Send any warning messages at login.
		warningMessage(resident);
		
		
		universe.setChangedNotify(PLAYER_LOGIN);
	}
	
	/**
	 * Send a warning message if the town or nation is due to be deleted.
	 * 
	 * @param resident
	 */
	private void warningMessage(Resident resident) {

		if (TownyEconomyHandler.isActive() && TownySettings.isTaxingDaily()) {
			if (resident.hasTown()) {
				try {
					Town town = resident.getTown();
					if (town.hasUpkeep()) {
						double upkeep = TownySettings.getTownUpkeepCost(town);
						try {
							if ((upkeep > 0) && (!town.canPayFromHoldings(upkeep))) {
								/*
								 *  Warn that the town is due to be deleted.
								 */
								TownyMessaging.sendMessage(resident, String.format(TownySettings.getLangString("msg_warning_delete"), town.getName()));
							}
						} catch (EconomyException ex) {
							// Economy error, so ignore it and try to continue.
						}
					}
						
					if (town.hasNation()) {
						Nation nation = town.getNation();
						
						double upkeep = TownySettings.getNationUpkeepCost(nation);
						try {
							if ((upkeep > 0) && (!nation.canPayFromHoldings(upkeep))) {
								/*
								 *  Warn that the nation is due to be deleted.
								 */
								TownyMessaging.sendMessage(resident, String.format(TownySettings.getLangString("msg_warning_delete"), nation.getName()));
							}
						} catch (EconomyException ex) {
							// Economy error, so ignore it and try to continue.
						}
					}
					
				} catch (NotRegisteredException ex) {
					// Should never reach here as we tested it beforehand.
				}
			}
		}
		
	}
}
