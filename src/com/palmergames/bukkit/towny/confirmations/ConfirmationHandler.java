package com.palmergames.bukkit.towny.confirmations;

import com.palmergames.bukkit.towny.Towny;
import com.palmergames.bukkit.towny.exceptions.TownyException;
import com.palmergames.bukkit.towny.object.Nation;
import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.towny.object.Town;
import com.palmergames.bukkit.towny.tasks.TownClaim;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class ConfirmationHandler {
	private static Towny plugin;

	public static void initialize(Towny plugin) {
		ConfirmationHandler.plugin = plugin;
	}

	private static HashMap<Resident, Town> towndeleteconfirmations = new HashMap<Resident, Town>();
	private static HashMap<Resident, Town> townunclaimallconfirmations = new HashMap<Resident, Town>();
	private static HashMap<Resident, Nation> nationdeleteconfirmations = new HashMap<Resident, Nation>();
	private static List<Resident> townypurgeconfirmations = new ArrayList<Resident>();

	public static void addConfirmation(final Resident r, final ConfirmationType type) throws TownyException {
		if (type == ConfirmationType.TOWNDELETE) {
			r.setConfirmationType(type);
			towndeleteconfirmations.put(r, r.getTown()); // The good thing is, using the "put" option we override the past one!

			new BukkitRunnable() {
				@Override
				public void run() {
					try {
						removeConfirmation(r, type);
					} catch (TownyException e) {
						// Shouldn't be possible since we added it in the first place!
					}
				}
			}.runTaskLater(plugin, 400);
		}
		if (type == ConfirmationType.PURGE) {
			if (!townypurgeconfirmations.contains(r)) {
				r.setConfirmationType(type);
				townypurgeconfirmations.add(r); // However an add option doesn't overridee so, we need to check if it exists first.
				new BukkitRunnable() {
					@Override
					public void run() {
						try {
							removeConfirmation(r, type);
						} catch (TownyException e) {
							// Shouldn't be possible since we added it in the first place!
						}
					}
				}.runTaskLater(plugin, 400);
			} else {
				townypurgeconfirmations.remove(r); // Remove the old one,
				townypurgeconfirmations.add(r); // Add the new one.
				r.setConfirmationType(type);

				new BukkitRunnable() {
					@Override
					public void run() {
						try {
							removeConfirmation(r, type);
						} catch (TownyException e) {
							// Shouldn't be possible since we added it in the first place!
						}
					}
				}.runTaskLater(plugin, 400);
			}
		}
		if (type == ConfirmationType.UNCLAIMALL) {

			r.setConfirmationType(type);
			townunclaimallconfirmations.put(r, r.getTown());

			new BukkitRunnable() {
				@Override
				public void run() {
					try {
						removeConfirmation(r, type);
					} catch (TownyException e) {
						// Shouldn't be possible since we added it in the first place!
					}
				}
			}.runTaskLater(plugin, 400);
		}
		if (type == ConfirmationType.NATIONDELETE) {

			r.setConfirmationType(type);
			nationdeleteconfirmations.put(r, r.getTown().getNation());

			new BukkitRunnable() {
				@Override
				public void run() {
					try {
						removeConfirmation(r, type);
					} catch (TownyException e) {
						// Shouldn't be possible since we added it in the first place!
					}
				}
			}.runTaskLater(plugin, 400);
		}
	}

	public static void removeConfirmation(Resident r, ConfirmationType type) throws TownyException {
		if (type == ConfirmationType.TOWNDELETE) {
			towndeleteconfirmations.remove(r);
			r.setConfirmationType(null);
		}
		if (type == ConfirmationType.PURGE) {
			townypurgeconfirmations.remove(r);
			r.setConfirmationType(null);
		}
		if (type == ConfirmationType.UNCLAIMALL) {
			townunclaimallconfirmations.remove(r);
			r.setConfirmationType(null);
		}
		if (type == ConfirmationType.NATIONDELETE) {
			nationdeleteconfirmations.remove(r);
			r.setConfirmationType(null);
		}
	}

	public static void handleConfirmation(Resident r, ConfirmationType type) throws TownyException {
		if (type == ConfirmationType.TOWNDELETE) {

		}
		if (type == ConfirmationType.PURGE) {
		}
		if (type == ConfirmationType.UNCLAIMALL) {
			if (townunclaimallconfirmations.containsKey(r)) {
				if (townunclaimallconfirmations.get(r).equals(r.getTown())) {
					TownClaim.townUnclaimAll(plugin, r.getTown());
					removeConfirmation(r, type);
				}
			}
		}
		if (type == ConfirmationType.NATIONDELETE) {
		}
	}

}
