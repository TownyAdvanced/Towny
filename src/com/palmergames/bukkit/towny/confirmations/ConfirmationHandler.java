package com.palmergames.bukkit.towny.confirmations;

import com.palmergames.bukkit.towny.Towny;
import com.palmergames.bukkit.towny.TownyAPI;
import com.palmergames.bukkit.towny.TownyMessaging;
import com.palmergames.bukkit.towny.TownySettings;
import com.palmergames.bukkit.towny.TownyUniverse;
import com.palmergames.bukkit.towny.command.PlotCommand;
import com.palmergames.bukkit.towny.exceptions.EconomyException;
import com.palmergames.bukkit.towny.event.TownBlockSettingsChangedEvent;
import com.palmergames.bukkit.towny.exceptions.TownyException;
import com.palmergames.bukkit.towny.object.Nation;
import com.palmergames.bukkit.towny.object.PlotGroup;
import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.towny.object.Town;
import com.palmergames.bukkit.towny.object.TownBlock;
import com.palmergames.bukkit.towny.object.TownBlockOwner;
import com.palmergames.bukkit.towny.object.TownyPermission;
import com.palmergames.bukkit.towny.object.TownyPermissionChange;
import com.palmergames.bukkit.towny.object.WorldCoord;
import com.palmergames.bukkit.towny.permissions.PermissionNodes;
import com.palmergames.bukkit.towny.tasks.PlotClaim;
import com.palmergames.bukkit.towny.tasks.ResidentPurge;
import com.palmergames.bukkit.towny.tasks.TownClaim;
import com.palmergames.bukkit.util.Colors;
import com.palmergames.util.TimeTools;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.HashMap;

public class ConfirmationHandler {
	private static Towny plugin;

	public static void initialize(Towny plugin) {
		ConfirmationHandler.plugin = plugin;
	}

	private static HashMap<Resident, Town> towndeleteconfirmations = new HashMap<>();
	private static HashMap<Resident, Town> townunclaimallconfirmations = new HashMap<>();
	private static HashMap<Resident, Nation> nationdeleteconfirmations = new HashMap<>();
	private static HashMap<Resident, String> townypurgeconfirmations = new HashMap<>();
	private static HashMap<Resident, Nation> nationmergeconfirmations = new HashMap<>();
	private static HashMap<Resident, GroupConfirmation> groupclaimconfirmations = new HashMap<>();
	private static HashMap<Resident, GroupConfirmation> groupremoveconfirmations = new HashMap<>();
	private static HashMap<Resident, GroupConfirmation> groupsetpermconfirmations = new HashMap<>();
	private static HashMap<Resident, GroupConfirmation> grouptoggleconfirmations = new HashMap<>();
	public static ConfirmationType consoleConfirmationType = ConfirmationType.NULL;
	private static Object consoleExtra = null;

	public static void addConfirmation(final Resident r, final ConfirmationType type, Object extra) throws TownyException {
		// We use "extra" in certain instances like the number of days for something e.t.c
		switch (type) {
			case TOWN_DELETE:
				r.setConfirmationType(type);
				towndeleteconfirmations.put(r, r.getTown()); // The good thing is, using the "put" option we override the past one!
				
				new BukkitRunnable() {
					@Override
					public void run() {
						removeConfirmation(r, type, false);
					}
				}.runTaskLater(plugin, 400);
				break;
			case NATION_DELETE:
				r.setConfirmationType(type);
				nationdeleteconfirmations.put(r, r.getTown().getNation());

				new BukkitRunnable() {
					@Override
					public void run() {
						removeConfirmation(r, type, false);
					}
				}.runTaskLater(plugin, 400);
				break;
			case UNCLAIM_ALL:
				r.setConfirmationType(type);
				townunclaimallconfirmations.put(r, r.getTown());

				new BukkitRunnable() {
					@Override
					public void run() {
						removeConfirmation(r, type, false);
					}
				}.runTaskLater(plugin, 400);
				break;
			case PURGE:
				r.setConfirmationType(type);
				townypurgeconfirmations.put(r, (String) extra); // However an add option doesn't overridee so, we need to check if it exists first.
				new BukkitRunnable() {
					@Override
					public void run() {
						removeConfirmation(r, type, false);
					}
				}.runTaskLater(plugin, 400);
				break;
			case NATION_MERGE:
				r.setConfirmationType(type);
				nationmergeconfirmations.put(r, (Nation) extra);

				new BukkitRunnable() {
					@Override
					public void run() {
						removeConfirmation(r, type, false);
					}
				}.runTaskLater(plugin, 400);
				break;
			case NULL:
				break;
			case GROUP_CLAIM_ACTION:
				r.setConfirmationType(type);
				groupclaimconfirmations.put(r, (GroupConfirmation) extra);

				new BukkitRunnable() {
					@Override
					public void run() {
						removeConfirmation(r, type, false);
					}
				}.runTaskLater(plugin, 400);
				break;
			case GROUP_UNCLAIM_ACTION:
				r.setConfirmationType(type);
				groupremoveconfirmations.put(r, (GroupConfirmation) extra);

				new BukkitRunnable() {
					@Override
					public void run() {
						removeConfirmation(r, type, false);
					}
				}.runTaskLater(plugin, 400);
				break;
			case GROUP_SET_PERM_ACTION:
				r.setConfirmationType(type);
				groupsetpermconfirmations.put(r, (GroupConfirmation) extra);

				new BukkitRunnable() {
					@Override
					public void run() {
						removeConfirmation(r, type, false);
					}
				}.runTaskLater(plugin, 400);
				break;
			case GROUP_TOGGLE_ACTION:
				r.setConfirmationType(type);
				grouptoggleconfirmations.put(r, (GroupConfirmation) extra);

				new BukkitRunnable() {
					@Override
					public void run() {
						removeConfirmation(r, type, false);
					}
				}.runTaskLater(plugin, 400);
				break;
				
		}
	}

	public static void removeConfirmation(Resident r, ConfirmationType type, boolean successful) {
		boolean sendmessage = false;
		switch (type) {
			case TOWN_DELETE:
				if (towndeleteconfirmations.containsKey(r) && !successful) {
					sendmessage = true;
				}
				towndeleteconfirmations.remove(r);
				r.setConfirmationType(null);
				break;
			case NATION_DELETE:
				if (nationdeleteconfirmations.containsKey(r) && !successful) {
					sendmessage = true;
				}
				nationdeleteconfirmations.remove(r);
				r.setConfirmationType(null);
				break;
			case UNCLAIM_ALL:
				if (townunclaimallconfirmations.containsKey(r) && !successful) {
					sendmessage = true;
				}
				townunclaimallconfirmations.remove(r);
				r.setConfirmationType(null);
				break;
			case PURGE:
				if (townypurgeconfirmations.containsKey(r) && !successful) {
					sendmessage = true;
				}
				townypurgeconfirmations.remove(r);
				r.setConfirmationType(null);
				break;
			case NATION_MERGE:
				if (nationmergeconfirmations.containsKey(r) && !successful) {
					sendmessage = true;
				}
				nationmergeconfirmations.remove(r);
				r.setConfirmationType(null);
				break;
			case NULL:
				break;
			case GROUP_CLAIM_ACTION:
				if (groupclaimconfirmations.containsKey(r) && !successful) {
					sendmessage = true;
				}
				groupclaimconfirmations.remove(r);
				r.setConfirmationType(null);
				break;
			case GROUP_UNCLAIM_ACTION:
				if (groupremoveconfirmations.containsKey(r) && !successful) {
					sendmessage = true;
				}
				groupclaimconfirmations.remove(r);
				r.setConfirmationType(null);
				break;
			case GROUP_SET_PERM_ACTION:
				if (groupsetpermconfirmations.containsKey(r) && !successful) {
					sendmessage = true;
				}
				groupsetpermconfirmations.remove(r);
				r.setConfirmationType(null);
				break;
			case GROUP_TOGGLE_ACTION:
				if (grouptoggleconfirmations.containsKey(r) && !successful) {
					sendmessage = true;
				}
				grouptoggleconfirmations.remove(r);
				r.setConfirmationType(null);
				break;
		}
		
		if (sendmessage) {
			TownyMessaging.sendMsg(r, TownySettings.getLangString("successful_cancel"));
		}
	}

	public static void handleConfirmation(Resident r, ConfirmationType type) throws TownyException {
		TownyUniverse townyUniverse = TownyUniverse.getInstance();
		if (type == ConfirmationType.TOWN_DELETE) {
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
				int days = 1;
				boolean townless = false;
				if (townypurgeconfirmations.get(r).startsWith("townless")) {
					townless = true;
					days = Integer.parseInt(townypurgeconfirmations.get(r).substring(8));
				} else {
					days = Integer.parseInt(townypurgeconfirmations.get(r));
				}				

				new ResidentPurge(plugin, player, TimeTools.getMillis(days + "d"), townless).start();
				removeConfirmation(r,type, true);
			}
		}
		if (type == ConfirmationType.UNCLAIM_ALL) {
			if (townunclaimallconfirmations.containsKey(r)) {
				if (townunclaimallconfirmations.get(r).equals(r.getTown())) {
					TownClaim.townUnclaimAll(plugin, townunclaimallconfirmations.get(r));
					removeConfirmation(r, type, true);
					return;
				}
			}
		}
		if (type == ConfirmationType.NATION_DELETE) {
			if (nationdeleteconfirmations.containsKey(r)) {
				if (nationdeleteconfirmations.get(r).equals(r.getTown().getNation())) {
					townyUniverse.getDataSource().removeNation(nationdeleteconfirmations.get(r));
					TownyMessaging.sendGlobalMessage(TownySettings.getDelNationMsg(nationdeleteconfirmations.get(r)));
					removeConfirmation(r,type, true);
				}
			}
		}
		if (type == ConfirmationType.NATION_MERGE) {
			if (nationmergeconfirmations.containsKey(r)) {
				Nation succumbingNation = r.getTown().getNation();
				Nation prevailingNation = nationmergeconfirmations.get(r);
				townyUniverse.getDataSource().mergeNation(succumbingNation, prevailingNation);
				TownyMessaging.sendGlobalMessage(String.format(TownySettings.getLangString("nation1_has_merged_with_nation2"), succumbingNation, prevailingNation));
				removeConfirmation(r,type, true);
			}
		}
		
		if (type == ConfirmationType.GROUP_CLAIM_ACTION) {
			if (groupclaimconfirmations.containsKey(r)) {
				
				GroupConfirmation confirmation = groupclaimconfirmations.get(r);
				ArrayList<WorldCoord> coords = plotGroupBlocksToCoords(confirmation.getGroup());
				
				new PlotClaim(Towny.getPlugin(), confirmation.getPlayer(), r, coords, true, false, true).start();
				removeConfirmation(r, type, true);
			}
		}
		
		if (type == ConfirmationType.GROUP_UNCLAIM_ACTION) {
			if (groupremoveconfirmations.containsKey(r)) {
				
				GroupConfirmation confirmation = groupremoveconfirmations.get(r);
				ArrayList<WorldCoord> coords = plotGroupBlocksToCoords(confirmation.getGroup());
				
				new PlotClaim(Towny.getPlugin(), confirmation.getPlayer(), r, coords, false, false, false).start();
				removeConfirmation(r, type, true);
			}
		}
		
		if (type == ConfirmationType.GROUP_SET_PERM_ACTION) {
			if (groupsetpermconfirmations.containsKey(r)) {
				GroupConfirmation confirmation = groupsetpermconfirmations.get(r);
				
				// Test the waters
				TownBlock tb = confirmation.getGroup().getTownBlocks().get(0);
				TownBlockOwner townBlockOwner = confirmation.getTownBlockOwner();				
				
				// setTownBlockPermissions returns a towny permission change object
				TownyPermissionChange permChange = PlotCommand.setTownBlockPermissions(confirmation.getPlayer(), townBlockOwner, tb, confirmation.getArgs());
				
				// If the perm change object is not null
				if (permChange != null) {
					
					// A simple index loop starting from the second element
					for (int i = 1; i < confirmation.getGroup().getTownBlocks().size(); ++i) {
						tb = confirmation.getGroup().getTownBlocks().get(i);
						
						tb.getPermissions().change(permChange);

						tb.setChanged(true);
						townyUniverse.getDataSource().saveTownBlock(tb);

						// Change settings event
						TownBlockSettingsChangedEvent event = new TownBlockSettingsChangedEvent(tb);
						Bukkit.getServer().getPluginManager().callEvent(event);
					}

					plugin.resetCache();

					Player player = confirmation.getPlayer();
					
					TownyPermission perm = confirmation.getGroup().getTownBlocks().get(0).getPermissions();
					TownyMessaging.sendMsg(player, TownySettings.getLangString("msg_set_perms"));
					TownyMessaging.sendMessage(player, (Colors.Green + " Perm: " + ((townBlockOwner instanceof Resident) ? perm.getColourString().replace("n", "t") : perm.getColourString().replace("f", "r"))));
					TownyMessaging.sendMessage(player, (Colors.Green + " Perm: " + ((townBlockOwner instanceof Resident) ? perm.getColourString2().replace("n", "t") : perm.getColourString2().replace("f", "r"))));
					TownyMessaging.sendMessage(player, Colors.Green + "PvP: " + ((!perm.pvp) ? Colors.Red + "ON" : Colors.LightGreen + "OFF") + Colors.Green + "  Explosions: " + ((perm.explosion) ? Colors.Red + "ON" : Colors.LightGreen + "OFF") + Colors.Green + "  Firespread: " + ((perm.fire) ? Colors.Red + "ON" : Colors.LightGreen + "OFF") + Colors.Green + "  Mob Spawns: " + ((perm.mobs) ? Colors.Red + "ON" : Colors.LightGreen + "OFF"));
				}
				
				removeConfirmation(r, type, true);
			}
		}
		
		if (type == ConfirmationType.GROUP_TOGGLE_ACTION) {
			GroupConfirmation confirmation = grouptoggleconfirmations.get(r);
			
			// Perform the toggle.
			new PlotCommand(Towny.getPlugin()).plotGroupToggle(confirmation.getPlayer(),
				confirmation.getGroup(), confirmation.getArgs());
			
			removeConfirmation(r, type, true);
		}
	}

	/**
	 * A simple method to get coordinates from plot group plots.
	 * @param group The {@link PlotGroup} to get the coords from.
	 * @return An {@link ArrayList} of {@link WorldCoord}'s.
	 * @author Suneet Tipirneni (Siris)
	 */
	private static ArrayList<WorldCoord> plotGroupBlocksToCoords(PlotGroup group) {
		ArrayList<WorldCoord> coords = new ArrayList<>();
		
		for (TownBlock tb : group.getTownBlocks()) {
			coords.add(tb.getWorldCoord());
		}
		
		return coords;
	}

	/**
	 * Adds a confirmation for the console.
	 * 
	 * @param type - Type of ConfirmationType.
	 * @param extra - Extra object, used for the number of days to purge for example.
	 * @author LlmDl
	 */
	public static void addConfirmation(final ConfirmationType type, Object extra) {
		if (consoleConfirmationType.equals(ConfirmationType.NULL)) {
			consoleExtra = extra;
			consoleConfirmationType = type;
			new BukkitRunnable() {
				@Override
				public void run() {
					removeConfirmation(type, false);
				}
			}.runTaskLater(plugin, 400);
		} else {
			TownyMessaging.sendMsg("Unable to start a new confirmation, one already exists of type: " + consoleConfirmationType.toString());
		}
	}

	/** 
	 * Removes confirmations for the console.
	 * 
	 * @param type of ConfirmationType
	 * @param successful if the calling operation was successful or not.
	 * @author LlmDl
	 */
	public static void removeConfirmation(final ConfirmationType type, boolean successful) {
		boolean sendmessage = false;
		if (!consoleConfirmationType.equals(ConfirmationType.NULL) && !successful) {
			sendmessage = true;
		}
		consoleConfirmationType = ConfirmationType.NULL;
		if (sendmessage) {
			TownyMessaging.sendMsg(TownySettings.getLangString("successful_cancel"));
		}
		
	}
	
	/**
	 * Handles confirmations sent via the console.
	 * 
	 * @param type of ConfirmationType.
	 * @throws TownyException - Generic TownyException
	 * @author LlmDl
	 */
	public static void handleConfirmation(ConfirmationType type) throws TownyException {
		TownyUniverse townyUniverse = TownyUniverse.getInstance();
		if (type == ConfirmationType.TOWN_DELETE) {
			Town town = (Town) consoleExtra;
			TownyMessaging.sendGlobalMessage(TownySettings.getDelTownMsg(town));
			townyUniverse.getDataSource().removeTown(town, false);
			removeConfirmation(type, true);
			consoleExtra = null;
			return;
		}
		if (type == ConfirmationType.PURGE) {
			int days = 1;
			boolean townless = false;
			if (((String) consoleExtra).startsWith("townless")) {
				townless = true;
				days = Integer.parseInt(((String) consoleExtra).substring(8));
			} else {
				days = Integer.parseInt(((String) consoleExtra));
			}			
			
			new ResidentPurge(plugin, null, TimeTools.getMillis(days + "d"), townless).start();
			removeConfirmation(type, true);
			consoleExtra = null;
		}
		if (type == ConfirmationType.NATION_DELETE) {
			Nation nation = (Nation) consoleExtra;
			TownyMessaging.sendGlobalMessage(TownySettings.getDelNationMsg(nation));
			townyUniverse.getDataSource().removeNation(nation);
			removeConfirmation(type, true);
			consoleExtra = null;
			return;
		}
	}
}
