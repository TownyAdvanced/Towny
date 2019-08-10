package com.palmergames.bukkit.towny.confirmations;

import com.palmergames.bukkit.towny.Towny;
import com.palmergames.bukkit.towny.TownyAPI;
import com.palmergames.bukkit.towny.TownyMessaging;
import com.palmergames.bukkit.towny.TownySettings;
import com.palmergames.bukkit.towny.TownyUniverse;
import com.palmergames.bukkit.towny.exceptions.TownyException;
import com.palmergames.bukkit.towny.object.Nation;
import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.towny.object.Town;
import com.palmergames.bukkit.towny.permissions.PermissionNodes;
import com.palmergames.bukkit.towny.tasks.ResidentPurge;
import com.palmergames.bukkit.towny.tasks.TownClaim;
import com.palmergames.util.TimeTools;

import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashMap;

public class ConfirmationHandler {
	private static Towny plugin;

	public static void initialize(Towny plugin) {
		ConfirmationHandler.plugin = plugin;
	}

	private static HashMap<Resident, Town> towndeleteconfirmations = new HashMap<>();
	private static HashMap<Resident, Town> townunclaimallconfirmations = new HashMap<>();
	private static HashMap<Resident, Nation> nationdeleteconfirmations = new HashMap<>();
	private static HashMap<Resident, Integer> townypurgeconfirmations = new HashMap<>();
	private static HashMap<Resident, Nation> nationmergeconfirmations = new HashMap<>();
	public static ConfirmationType consoleConfirmationType = ConfirmationType.NULL;
	private static Object consoleExtra = null;

	public static void addConfirmation(final Resident r, final ConfirmationType type, Object extra) throws TownyException {
		// We use "extra" in certain instances like the number of days for something e.t.c
		if (type == ConfirmationType.TOWNDELETE) {
			r.setConfirmationType(type);
			towndeleteconfirmations.put(r, r.getTown()); // The good thing is, using the "put" option we override the past one!

			new BukkitRunnable() {
				@Override
				public void run() {
					removeConfirmation(r, type, false);
				}
			}.runTaskLater(plugin, 400);
		}
		if (type == ConfirmationType.PURGE) {
			r.setConfirmationType(type);
			townypurgeconfirmations.put(r, (Integer) extra); // However an add option doesn't overridee so, we need to check if it exists first.
			new BukkitRunnable() {
				@Override
				public void run() {
					removeConfirmation(r, type, false);
				}
			}.runTaskLater(plugin, 400);
		}
		if (type == ConfirmationType.UNCLAIMALL) {

			r.setConfirmationType(type);
			townunclaimallconfirmations.put(r, r.getTown());

			new BukkitRunnable() {
				@Override
				public void run() {
					removeConfirmation(r, type, false);
				}
			}.runTaskLater(plugin, 400);
		}
		if (type == ConfirmationType.NATIONDELETE) {

			r.setConfirmationType(type);
			nationdeleteconfirmations.put(r, r.getTown().getNation());

			new BukkitRunnable() {
				@Override
				public void run() {
					removeConfirmation(r, type, false);
				}
			}.runTaskLater(plugin, 400);
		}
		if (type == ConfirmationType.NATIONMERGE) {
			r.setConfirmationType(type);
			nationmergeconfirmations.put(r, (Nation) extra);
			
			new BukkitRunnable() {
				@Override
				public void run() {
					removeConfirmation(r, type, false);
				}
			}.runTaskLater(plugin, 400);
		}
	}

	public static void removeConfirmation(Resident r, ConfirmationType type, boolean successful) {
		boolean sendmessage = false;
		if (type == ConfirmationType.TOWNDELETE) {
			if (towndeleteconfirmations.containsKey(r) && !successful) {
				sendmessage = true;
			}
			towndeleteconfirmations.remove(r);
			r.setConfirmationType(null);
		}
		if (type == ConfirmationType.PURGE) {
			if (townypurgeconfirmations.containsKey(r) && !successful) {
				sendmessage = true;
			}
			townypurgeconfirmations.remove(r);
			r.setConfirmationType(null);
		}
		if (type == ConfirmationType.UNCLAIMALL) {
			if (townunclaimallconfirmations.containsKey(r) && !successful) {
				sendmessage = true;
			}
			townunclaimallconfirmations.remove(r);
			r.setConfirmationType(null);
		}
		if (type == ConfirmationType.NATIONDELETE) {
			if (nationdeleteconfirmations.containsKey(r) && !successful) {
				sendmessage = true;
			}
			nationdeleteconfirmations.remove(r);
			r.setConfirmationType(null);
		}
		if (type == ConfirmationType.NATIONMERGE) {
			if (nationmergeconfirmations.containsKey(r) && !successful) {
				sendmessage = true;
			}
			nationmergeconfirmations.remove(r);
			r.setConfirmationType(null);
		}
		if (sendmessage) {
			TownyMessaging.sendMsg(r, TownySettings.getLangString("successful_cancel"));
		}
	}

	public static void handleConfirmation(Resident r, ConfirmationType type) throws TownyException {
		TownyUniverse townyUniverse = TownyUniverse.getInstance();
		if (type == ConfirmationType.TOWNDELETE) {
			if (towndeleteconfirmations.containsKey(r)) {
				if (towndeleteconfirmations.get(r).equals(r.getTown())) {
					TownyMessaging.sendGlobalMessage(TownySettings.getDelTownMsg(towndeleteconfirmations.get(r)));
					townyUniverse.getDataSource().removeTown(towndeleteconfirmations.get(r));
					removeConfirmation(r,type, true);
					return;
				}
			}
		}
		if (type == ConfirmationType.PURGE) {
			if (townypurgeconfirmations.containsKey(r)) {
				Player player = TownyAPI.getInstance().getPlayer(r);
				if (player == null) {
					throw new TownyException("Player could not be found!");
				}
				if (!townyUniverse.getPermissionSource().testPermission(player, PermissionNodes.TOWNY_COMMAND_TOWNYADMIN_PURGE.getNode())) {
					throw new TownyException(TownySettings.getLangString("msg_err_admin_only"));
				}
				int days = townypurgeconfirmations.get(r);
				new ResidentPurge(plugin, player, TimeTools.getMillis(days + "d")).start();
				removeConfirmation(r,type, true);
			}
		}
		if (type == ConfirmationType.UNCLAIMALL) {
			if (townunclaimallconfirmations.containsKey(r)) {
				if (townunclaimallconfirmations.get(r).equals(r.getTown())) {
					TownClaim.townUnclaimAll(plugin, townunclaimallconfirmations.get(r));
					removeConfirmation(r, type, true);
					return;
				}
			}
		}
		if (type == ConfirmationType.NATIONDELETE) {
			if (nationdeleteconfirmations.containsKey(r)) {
				if (nationdeleteconfirmations.get(r).equals(r.getTown().getNation())) {
					townyUniverse.getDataSource().removeNation(nationdeleteconfirmations.get(r));
					TownyMessaging.sendGlobalMessage(TownySettings.getDelNationMsg(nationdeleteconfirmations.get(r)));
					removeConfirmation(r,type, true);
				}
			}
		}
		if (type == ConfirmationType.NATIONMERGE) {
			if (nationmergeconfirmations.containsKey(r)) {
				Nation succumbingNation = r.getTown().getNation();
				Nation prevailingNation = nationmergeconfirmations.get(r);
				townyUniverse.getDataSource().mergeNation(succumbingNation, prevailingNation);
				TownyMessaging.sendGlobalMessage(String.format(TownySettings.getLangString("nation1_has_merged_with_nation2"), succumbingNation, prevailingNation));
				removeConfirmation(r,type, true);
			}
		}
	}

	/**
	 * Adds a confirmation for the console.
	 * 
	 * @param type - Type of ConfirmationType.
	 * @param extra - Extra object, used for the number of days to purge for example.
	 * @author LlmDl
	 */
	public static void addConfirmation(final ConfirmationType type, Object extra) {
		if (type == ConfirmationType.PURGE) {
			if (consoleConfirmationType.equals(ConfirmationType.NULL)) {
				consoleExtra = extra;
				consoleConfirmationType = type;
				new BukkitRunnable() {
					@Override
					public void run() {
						removeConfirmation(type, false);
					}
				}.runTaskLater(plugin, 400);
			}
		}
		
	}

	/** 
	 * Removes confirmations for the console.
	 * 
	 * @param type of ConfirmationType
	 * @param successful
	 * @author LlmDl
	 */
	public static void removeConfirmation(final ConfirmationType type, boolean successful) {
		boolean sendmessage = false;
		if (type == ConfirmationType.PURGE) {
			if (consoleConfirmationType != null && !successful) {
				sendmessage = true;
			}
			consoleConfirmationType = ConfirmationType.NULL;			
		}
		if (sendmessage) {
			TownyMessaging.sendMsg(TownySettings.getLangString("successful_cancel"));
		}
		
	}
	
	/**
	 * Handles confirmations sent via the console.
	 * 
	 * @param type of ConfirmationType.
	 * @throws TownyException
	 * @author LlmDl
	 */
	public static void handleConfirmation(ConfirmationType type) throws TownyException {
		if (type == ConfirmationType.PURGE) {
			int days = (Integer) consoleExtra;
			new ResidentPurge(plugin, null, TimeTools.getMillis(days + "d")).start();
			removeConfirmation(type, true);
			consoleExtra = null;
		}
	}
}
