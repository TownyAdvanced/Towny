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
import com.palmergames.bukkit.towny.object.resident.mode.ResidentModeHandler;
import com.palmergames.bukkit.towny.permissions.PermissionNodes;
import com.palmergames.bukkit.towny.permissions.TownyPerms;
import com.palmergames.bukkit.towny.utils.ResidentUtil;
import com.palmergames.bukkit.towny.utils.TownRuinUtil;
import com.palmergames.bukkit.util.BukkitTools;
import com.palmergames.bukkit.util.Colors;

import net.kyori.adventure.text.event.ClickEvent;
import org.bukkit.entity.Player;
import org.bukkit.metadata.MetadataValue;

import java.util.logging.Level;


/**
 * @author ElgarL
 *
 */
public class OnPlayerLogin implements Runnable {
	
	private final Towny plugin;
	private final TownyUniverse universe = TownyUniverse.getInstance();
	private final Player player;
	private final long inviteNotificationTicksDelay = 20L * 10;
	
	/**
	 * Constructor
	 * 
	 * @param plugin - Towny plugin,
	 * @param player - Player to run login code on.
	 */
	public OnPlayerLogin(Towny plugin, Player player) {
		
		this.plugin = plugin;
		this.player = player;
	}

	@Override
	public void run() {

		final Resident resident = getResidentReadyToLogIn();
		if (resident == null)
			return;

		loginExistingResident(resident);

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
				final Resident finalResident = resident;
				TownyEconomyHandler.economyExecutor().execute(() -> bankWarningMessage(finalResident, town, nation));
			}

			// Send a message warning of being overclaimed while the takeoverclaims feature is enabled.
			if (TownySettings.isOverClaimingAllowingStolenLand() && town.isOverClaimed())
				TownyMessaging.sendMsg(resident, Translatable.literal(Colors.Red).append(Translatable.of("msg_warning_your_town_is_overclaimed")));

			// Send a message warning of ruined status and time until deletion.
			if (town.isRuined())
				TownyMessaging.sendMsg(resident, Translatable.of("msg_warning_your_town_is_ruined_for_x_more_hours", TownySettings.getTownRuinsMaxDurationHours() - TownRuinUtil.getTimeSinceRuining(town)));

			if (townHasPendingNationInvites(town))
				plugin.getScheduler().runLater(player, ()-> TownyMessaging.sendMsg(player, Translatable.of("msg_your_town_has_pending_nation_invites")), inviteNotificationTicksDelay);
			else if (nationHasPendingAllyInvites(nation))
				plugin.getScheduler().runLater(player, ()-> TownyMessaging.sendMsg(player, Translatable.of("msg_your_nation_has_pending_ally_invites")), inviteNotificationTicksDelay);
		}

		if (residentHasPendingTownInvites(resident))
			plugin.getScheduler().runLater(player, ()-> TownyMessaging.sendMsg(player, Translatable.of("msg_you_have_pending_town_invites")), inviteNotificationTicksDelay);

		// Check if this is a player spawning into a Town in which they are outlawed.
		Town insideTown = TownyAPI.getInstance().getTown(player.getLocation());
		if (insideTown != null && insideTown.hasOutlaw(resident))
			ResidentUtil.outlawEnteredTown(resident, insideTown, player.getLocation());

		//Schedule to setup default modes when the player has finished loading
		plugin.getScheduler().runLater(player, () -> ResidentModeHandler.applyDefaultModes(resident, false), 1);

		if (TownyUpdateChecker.shouldShowNotification() && player.hasPermission(PermissionNodes.TOWNY_ADMIN_UPDATEALERTS.getNode())) {
			ClickEvent clickEvent = ClickEvent.openUrl(TownyUpdateChecker.getUpdateURL());

			player.sendMessage(Translatable.of("default_towny_prefix").append(Translatable.of("msg_new_update_available", TownyUpdateChecker.getNewVersion(), Towny.getPlugin().getVersion())).locale(player).component().clickEvent(clickEvent));
			player.sendMessage(Translatable.of("default_towny_prefix").append(Translatable.of("msg_click_to_download")).locale(player).component().clickEvent(clickEvent));
		}

		if (TownyEconomyHandler.isActive() && TownyEconomyHandler.getProvider().isLegacy() && player.hasPermission(PermissionNodes.TOWNY_ADMIN_UPDATEALERTS.getNode())) {
			ClickEvent clickEvent = ClickEvent.runCommand("/townyadmin eco convert modern");
			player.sendMessage(Translatable.of("default_towny_prefix").append(Translatable.of("msg_legacy_economy_detected")).locale(player).component().clickEvent(clickEvent));
			player.sendMessage(Translatable.of("default_towny_prefix").append(Translatable.of("msg_click_to_convert_to_modern_economy")).locale(player).component().clickEvent(clickEvent));
		}
	}

	private Resident getResidentReadyToLogIn() {

		Resident resident = universe.getResident(player.getUniqueId());

		/*
		 * We do have record of this UUID being used before, log in the resident after checking for a name change.
		 */
		if (resident != null) {
			checkForNameChangeSinceLastLogIn(resident);
			return universe.getResident(player.getUniqueId());
		}

		/*
		 * No record of this resident's UUID, begin with checking if there is a Resident with same name as the Player.
		 */
		resident = universe.getResident(player.getName());

		// If the universe has a resident and the resident has no UUID, log them in with their current name, UUID will be assigned later.
		if (resident != null && !resident.hasUUID()) {
			return resident;

		// We have a resident but the resident's UUID was not recorded properly (or the server has somehow altered the player's UUID since recording it.)
		} else if (resident != null && !resident.getUUID().equals(player.getUniqueId())) {
			try {
				universe.unregisterResident(resident);   // Unregister.
				resident.setUUID(player.getUniqueId());  // Set proper UUID.
				universe.registerResident(resident);     // Re-register.
			} catch (NotRegisteredException | AlreadyRegisteredException ignored) {}
			return resident;

		// Else we're dealing with a new resident, because there's no resident by that UUID or resident by that Name without a UUID.
		} else {
			return createNewResident(resident);
		}
	}

	private void checkForNameChangeSinceLastLogIn(Resident resident) {
		if (!resident.getName().equals(player.getName())) {
			try {
				universe.getDataSource().renamePlayer(resident, player.getName());
			} catch (AlreadyRegisteredException | NotRegisteredException e) {
				plugin.getLogger().log(Level.WARNING, "An exception occurred when trying to rename " + resident.getName() + " to " + player.getName(), e);
			}
		}
	}

	private Resident createNewResident(Resident resident) {
		try {
			resident = universe.getDataSource().newResident(player.getName(), player.getUniqueId());
			resident.setRegistered(System.currentTimeMillis());

			final Resident finalResident = resident;
			universe.getDataSource().getHibernatedResidentRegistered(player.getUniqueId()).thenAccept(registered -> {
				if (registered.isPresent()) {
					finalResident.setRegistered(registered.get());
					finalResident.save();
				}
			});

			resident.setLastOnline(System.currentTimeMillis());
			assignDefaultTownIfRequired(resident);

			resident.save();
			plugin.getScheduler().run(player, () -> BukkitTools.fireEvent(new NewResidentEvent(finalResident)));

		} catch (NotRegisteredException e) {
			plugin.getLogger().log(Level.WARNING, "Could not register resident '" + player.getName() + "' (" + player.getUniqueId() + ") due to an error, Towny features might be limited for this player until it is resolved", e);
		} catch (AlreadyRegisteredException ignored) {}

		return resident;
	}

	private void assignDefaultTownIfRequired(Resident resident) {
		Town town = TownyUniverse.getInstance().getTown(TownySettings.getDefaultTownName());
		if (town == null)
			return;
		try {
			resident.setTown(town);
		} catch (AlreadyRegisteredException ignore) {}
	}
	
	/**
	 * Update last online, add UUID if needed, then save the resident.
	 * 
	 * @param resident Resident logging in.
	 */
	private void loginExistingResident(Resident resident) {

		// Done in a task because some plugins won't assign the vanished meta to the
		// player until 1 tick after the player logs in.
		plugin.getScheduler().runLater(player, () -> {
			if (player.getMetadata("vanished").stream().noneMatch(MetadataValue::asBoolean))
				resident.setLastOnline(System.currentTimeMillis());

			if (!resident.hasUUID()) {
				resident.setUUID(player.getUniqueId());
				try {
					TownyUniverse.getInstance().registerResidentUUID(resident);
				} catch (AlreadyRegisteredException e) {
					plugin.getLogger().log(Level.WARNING, "uuid for resident " + resident.getName() + " was already registered! (" + player.getUniqueId() + ")", e);
				}
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
	private void bankWarningMessage(Resident resident, Town town, Nation nation) {
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
				TownyMessaging.sendMsg(resident, Translatable.of("msg_warning_town_deposit_hint"));
			}
		}
			
		if (nation != null) {
			double upkeep = TownySettings.getNationUpkeepCost(nation);
			if (upkeep > 0 && !nation.getAccount().canPayFromHoldings(upkeep)) {
				/*
				 *  Warn that the nation is due to be deleted.
				 */
				TownyMessaging.sendMsg(resident, Translatable.of("msg_warning_delete", nation.getName()));
				TownyMessaging.sendMsg(resident, Translatable.of("msg_warning_nation_deposit_hint"));
			}
		}
	}

	private boolean nationHasPendingAllyInvites(Nation nation) {
		return nation != null && !nation.getReceivedInvites().isEmpty()
				&& universe.getPermissionSource().testPermission(player, PermissionNodes.TOWNY_COMMAND_NATION_ALLY_ACCEPT.getNode());
	}

	private boolean townHasPendingNationInvites(Town town) {
		return !town.hasNation() && !town.getReceivedInvites().isEmpty()
				&& universe.getPermissionSource().testPermission(player, PermissionNodes.TOWNY_COMMAND_TOWN_INVITE_ACCEPT.getNode());
	}

	private boolean residentHasPendingTownInvites(Resident resident) {
		return !resident.hasTown() &&  !resident.getReceivedInvites().isEmpty()
				&& universe.getPermissionSource().testPermission(player, PermissionNodes.TOWNY_TOWN_RESIDENT.getNode());
	}
	
}
