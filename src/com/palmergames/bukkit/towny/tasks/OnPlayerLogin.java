package com.palmergames.bukkit.towny.tasks;

import com.palmergames.bukkit.towny.Towny;
import com.palmergames.bukkit.towny.TownyAPI;
import com.palmergames.bukkit.towny.TownyEconomyHandler;
import com.palmergames.bukkit.towny.TownyMessaging;
import com.palmergames.bukkit.towny.TownySettings;
import com.palmergames.bukkit.towny.TownyUniverse;
import com.palmergames.bukkit.towny.TownyUpdateChecker;
import com.palmergames.bukkit.towny.event.resident.NewResidentEvent;
import com.palmergames.bukkit.towny.exceptions.AlreadyRegisteredException;
import com.palmergames.bukkit.towny.exceptions.NotRegisteredException;
import com.palmergames.bukkit.towny.object.Nation;
import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.towny.object.Town;
import com.palmergames.bukkit.towny.object.Translatable;
import com.palmergames.bukkit.towny.permissions.PermissionNodes;
import com.palmergames.bukkit.towny.permissions.TownyPerms;
import com.palmergames.bukkit.towny.utils.ResidentUtil;
import com.palmergames.bukkit.towny.utils.TownRuinUtil;
import com.palmergames.bukkit.util.BukkitTools;
import com.palmergames.bukkit.util.Colors;

import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.event.ClickEvent;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.metadata.MetadataValue;

import java.util.logging.Level;


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
		
		Resident resident = universe.getResident(player.getUniqueId());

		if (resident == null) {
			/*
			 * No record of this resident's UUID.
			 */
			resident = universe.getResident(player.getName());

			// If the universe has a resident and the resident has no UUID, log them in with their current name.
			if (resident != null && !resident.hasUUID()) {
				loginExistingResident(resident);

			// We have a resident but the resident's UUID was not recorded properly (or the server has somehow altered the player's UUID since recording it.)
			} else if (resident != null && !resident.getUUID().equals(player.getUniqueId())) {
				try {
					universe.unregisterResident(resident);   // Unregister.
					resident.setUUID(player.getUniqueId());  // Set proper UUID.
					universe.registerResident(resident);     // Re-register.
					
				} catch (NotRegisteredException | AlreadyRegisteredException ignored) {}
				loginExistingResident(resident);
				
			// Else we're dealing with a new resident, because there's no resident by that UUID or resident by that Name without a UUID.
			} else {

				/*
				 * Make a brand new Resident.
				 */
				try {
					universe.getDataSource().newResident(player.getName(), player.getUniqueId());
					TownySettings.incrementUUIDCount();
					
					resident = universe.getResident(player.getUniqueId());
					
					if (TownySettings.isShowingLocaleMessage())
					    TownyMessaging.sendMsg(resident, Translatable.of("msg_your_locale", player.getLocale()));

					resident.setRegistered(System.currentTimeMillis());

					final Resident finalResident = resident;
					universe.getDataSource().getHibernatedResidentRegistered(player.getUniqueId()).thenAccept(registered -> {
						if (registered.isPresent()) {
							finalResident.setRegistered(registered.get());
							finalResident.save();
						}
					});
						 
					resident.setLastOnline(System.currentTimeMillis());
					if (!TownySettings.getDefaultTownName().equals("")) {
						Town town = TownyUniverse.getInstance().getTown(TownySettings.getDefaultTownName());
						if (town != null) {
							try {
								resident.setTown(town);
								town.save();
							} catch (AlreadyRegisteredException ignore) {}
						}
					}
					
					resident.save();
					BukkitTools.fireEvent(new NewResidentEvent(resident));
					
				} catch (AlreadyRegisteredException | NotRegisteredException ignored) {}

			}

		} else {
			/*
			 * We do have record of this UUID being used before, log in the resident after checking for a name change.
			 */
			
			// Name change test.
			if (!resident.getName().equals(player.getName())) {
				try {
					universe.getDataSource().renamePlayer(resident, player.getName());
				} catch (AlreadyRegisteredException | NotRegisteredException e) {
					plugin.getLogger().log(Level.WARNING, "An exception occurred when trying to rename " + resident.getName() + " to " + player.getName(), e);
				}
			}
			/*
			 * This resident is known so fetch the data and update it.
			 */
			resident = universe.getResident(player.getUniqueId());
			loginExistingResident(resident);
		}

		if (resident != null) {
			TownyPerms.assignPermissions(resident, player);
			
			final Town town = resident.getTownOrNull();
			if (town != null) {
				Nation nation = resident.getNationOrNull();
				
				if (TownySettings.getShowTownBoardOnLogin() && !town.getBoard().isEmpty())
					TownyMessaging.sendTownBoard(player, town);

				if (TownySettings.getShowNationBoardOnLogin() && nation != null && !nation.getBoard().isEmpty())
					TownyMessaging.sendNationBoard(player, nation);
				
				// Send any warning messages at login.
				if (TownyEconomyHandler.isActive() && TownySettings.isTaxingDaily()) {
					if (TownySettings.isEconomyAsync()) {
						final Resident finalResident = resident;
						Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> warningMessage(finalResident, town, nation));
					} else
						warningMessage(resident, town, nation);
				}
				
				// Send a message warning of being overclaimed while the takeoverclaims feature is enabled.
				if (TownySettings.isOverClaimingAllowingStolenLand() && town.getTownBlocks().size() > town.getMaxTownBlocks())
					TownyMessaging.sendMsg(resident, Translatable.literal(Colors.Red).append(Translatable.of("msg_warning_your_town_is_overclaimed")));
				
				// Send a message warning of ruined status and time until deletion.
				if (town.isRuined())
					TownyMessaging.sendMsg(resident, Translatable.of("msg_warning_your_town_is_ruined_for_x_more_hours", TownySettings.getTownRuinsMaxDurationHours() - TownRuinUtil.getTimeSinceRuining(town)));
			}
			
			// Check if this is a player spawning into a Town in which they are outlawed.
			Town insideTown = TownyAPI.getInstance().getTown(player.getLocation());
			if (insideTown != null && insideTown.hasOutlaw(resident))
				ResidentUtil.outlawEnteredTown(resident, town, player.getLocation());

			//Schedule to setup default modes when the player has finished loading
			if (BukkitTools.scheduleSyncDelayedTask(new SetDefaultModes(player.getName(), false), 1) == -1)
				TownyMessaging.sendErrorMsg("Could not set default modes for " + player.getName() + ".");
			
			if (TownyUpdateChecker.shouldShowNotification() && player.hasPermission(PermissionNodes.TOWNY_ADMIN_UPDATEALERTS.getNode())) {
				Audience audience = Towny.getAdventure().player(player);
				ClickEvent clickEvent = ClickEvent.openUrl(TownyUpdateChecker.getUpdateURL());
				
				audience.sendMessage(Translatable.of("default_towny_prefix").append(Translatable.of("msg_new_update_available", TownyUpdateChecker.getNewVersion(), Towny.getPlugin().getVersion())).locale(player).component().clickEvent(clickEvent));
				audience.sendMessage(Translatable.of("default_towny_prefix").append(Translatable.of("msg_click_to_download")).locale(player).component().clickEvent(clickEvent));
			}
		}
	}
	
	/**
	 * Update last online, add UUID if needed, then save the resident.
	 * 
	 * @param resident Resident logging in.
	 */
	private void loginExistingResident(Resident resident) {

		// Done in a task because some plugins won't assign the vanished meta to the
		// player until 1 tick after the player logs in.
		Bukkit.getScheduler().runTaskLater(plugin, () -> {
			if (player.getMetadata("vanished").stream().noneMatch(MetadataValue::asBoolean))
				resident.setLastOnline(System.currentTimeMillis());

			if (!resident.hasUUID()) {
				resident.setUUID(player.getUniqueId());
				try {
					TownyUniverse.getInstance().registerResidentUUID(resident);
				} catch (AlreadyRegisteredException e) {
					e.printStackTrace();
				}
				TownySettings.incrementUUIDCount();
			}
			resident.save();
		}, 5);
	}
	
	/**
	 * Send a warning message if the town or nation is due to be deleted.
	 * 
	 * @param resident Resident to send the warning to.
	 * @param town Town which the resident is part of.
	 * @param nation Nation which the town is a part of or null.
	 */
	private void warningMessage(Resident resident, Town town, Nation nation) {
		if (town.hasUpkeep()) {
			double upkeep = TownySettings.getTownUpkeepCost(town);
			if (upkeep > 0 && !town.getAccount().canPayFromHoldings(upkeep)) {
				/*
				 *  Warn that the town is due to be deleted/bankrupted.
				 */
				if(TownySettings.isTownBankruptcyEnabled()) {
					Translatable msg = !town.isBankrupt()
						? Translatable.of("msg_warning_bankrupt", town.getName()) // Town will be bankrupt next time upkeep is taken.
						: Translatable.of("msg_your_town_is_bankrupt");           // Town is already bankrupt. 
					TownyMessaging.sendMsg(resident, msg);
				} else {
					TownyMessaging.sendMsg(resident, Translatable.of("msg_warning_delete", town.getName()));
				}
			}
		}
			
		if (nation != null) {
			double upkeep = TownySettings.getNationUpkeepCost(nation);
			if (upkeep > 0 && !nation.getAccount().canPayFromHoldings(upkeep)) {
				/*
				 *  Warn that the nation is due to be deleted.
				 */
				TownyMessaging.sendMsg(resident, Translatable.of("msg_warning_delete", nation.getName()));
			}
		}
	}
}
