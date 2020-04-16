package com.palmergames.bukkit.towny.tasks;

import com.earth2me.essentials.Essentials;
import com.palmergames.bukkit.towny.Towny;
import com.palmergames.bukkit.towny.TownyAPI;
import com.palmergames.bukkit.towny.TownyEconomyHandler;
import com.palmergames.bukkit.towny.TownyMessaging;
import com.palmergames.bukkit.towny.TownySettings;
import com.palmergames.bukkit.towny.TownyUniverse;
import com.palmergames.bukkit.towny.exceptions.AlreadyRegisteredException;
import com.palmergames.bukkit.towny.exceptions.EconomyException;
import com.palmergames.bukkit.towny.exceptions.NotRegisteredException;
import com.palmergames.bukkit.towny.object.Nation;
import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.towny.object.Town;
import com.palmergames.bukkit.towny.permissions.TownyPerms;
import com.palmergames.bukkit.util.BukkitTools;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;


/**
 * @author ElgarL
 *
 */
public class OnPlayerLogin implements Runnable {
	
	Towny plugin;
	com.palmergames.bukkit.towny.TownyUniverse universe;
	volatile Player player;
	
	/**
	 * Constructor
	 * 
	 * @param plugin - Towny plugin,
	 * @param player - Player to run login code on.
	 */
	public OnPlayerLogin(Towny plugin, Player player) {
		
		this.plugin = plugin;
		this.universe = com.palmergames.bukkit.towny.TownyUniverse.getInstance();
		this.player = player;
	}

	@Override
	public void run() {
		
		Resident resident = null;

		/*
		 * No record of this resident exists
		 * So create a fresh set of data.
		 */
		try {
			universe.getDatabaseHandler().newResident(player.getUniqueId(), player.getName());
			resident = universe.getDatabaseHandler().getResident(player.getUniqueId());

			if (TownySettings.isShowingRegistrationMessage())
				TownyMessaging.sendMessage(player, TownySettings.getRegistrationMsg(player.getName()));
			resident.setRegistered(System.currentTimeMillis());
			if (!TownySettings.getDefaultTownName().equals("")) {
				try {
					Town town = TownyUniverse.getInstance().getDataSource().getTown(TownySettings.getDefaultTownName());
					town.addResident(resident);
					universe.getDatabaseHandler().save(town);
				} catch (NotRegisteredException | AlreadyRegisteredException ignored) {
				}
			}

			// Update resident data
			if (TownySettings.isUsingEssentials()) {
				Essentials ess = (Essentials) Bukkit.getPluginManager().getPlugin("Essentials");
				/*
				 * Don't update last online for a player who is vanished.
				 */
				if (!ess.getUser(player).isVanished())
					resident.setLastOnline(System.currentTimeMillis());
			} else
				resident.setLastOnline(System.currentTimeMillis());
			
			universe.getDatabaseHandler().save(resident);
			
		} catch (NotRegisteredException ex) {
			// Should never happen
		}

		if (resident != null)
			TownyPerms.assignPermissions(resident, player);

        if (TownySettings.getShowTownBoardOnLogin()) {
            TownyMessaging.sendTownBoard(player, resident.getTown());
        }
        if (TownySettings.getShowNationBoardOnLogin()) {
            if (resident.getTown().hasNation()) {
                TownyMessaging.sendNationBoard(player, resident.getTown().getNation());
            }
        }
        resident.getTown(); // Exception check, this does not do anything at all!

        if (TownyAPI.getInstance().isWarTime()) {
			universe.getWarEvent().sendScores(player, 3);
		}

		//Schedule to setup default modes when the player has finished loading
		if (BukkitTools.scheduleSyncDelayedTask(new SetDefaultModes(player.getName(), false), 1) == -1) {
			TownyMessaging.sendErrorMsg("Could not set default modes for " + player.getName() + ".");
		}
		
		// Send any warning messages at login.
		warningMessage(resident);
	}
	
	/**
	 * Send a warning message if the town or nation is due to be deleted.
	 * 
	 * @param resident - Resident to send the warning to.
	 */
	private void warningMessage(Resident resident) {

		if (TownyEconomyHandler.isActive() && TownySettings.isTaxingDaily()) {
			if (resident.hasTown()) {
                Town town = resident.getTown();
                if (town.hasUpkeep()) {
                    double upkeep = TownySettings.getTownUpkeepCost(town);
                    try {
                        if ((upkeep > 0) && (!town.getAccount().canPayFromHoldings(upkeep))) {
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
                        if ((upkeep > 0) && (!nation.getAccount().canPayFromHoldings(upkeep))) {
                            /*
                             *  Warn that the nation is due to be deleted.
                             */
                            TownyMessaging.sendMessage(resident, String.format(TownySettings.getLangString("msg_warning_delete"), nation.getName()));
                        }
                    } catch (EconomyException ex) {
                        // Economy error, so ignore it and try to continue.
                    }
                }

            }
		}
		
	}
}
