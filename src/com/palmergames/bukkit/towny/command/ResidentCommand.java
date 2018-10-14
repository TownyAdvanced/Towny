package com.palmergames.bukkit.towny.command;

import com.earth2me.essentials.Teleport;
import com.earth2me.essentials.User;
import com.palmergames.bukkit.towny.Towny;
import com.palmergames.bukkit.towny.TownyEconomyHandler;
import com.palmergames.bukkit.towny.TownyFormatter;
import com.palmergames.bukkit.towny.TownyMessaging;
import com.palmergames.bukkit.towny.TownySettings;
import com.palmergames.bukkit.towny.TownyTimerHandler;
import com.palmergames.bukkit.towny.exceptions.AlreadyRegisteredException;
import com.palmergames.bukkit.towny.exceptions.EconomyException;
import com.palmergames.bukkit.towny.exceptions.NotRegisteredException;
import com.palmergames.bukkit.towny.exceptions.TownyException;
import com.palmergames.bukkit.towny.object.Nation;
import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.towny.object.Town;
import com.palmergames.bukkit.towny.object.TownSpawnLevel;
import com.palmergames.bukkit.towny.object.TownyPermission;
import com.palmergames.bukkit.towny.object.TownyUniverse;
import com.palmergames.bukkit.towny.permissions.PermissionNodes;
import com.palmergames.bukkit.util.BukkitTools;
import com.palmergames.bukkit.util.ChatTools;
import com.palmergames.bukkit.util.Colors;
import com.palmergames.util.StringMgmt;

import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerTeleportEvent.TeleportCause;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

/**
 * Send a list of all towny resident help commands to player Command: /resident
 */

public class ResidentCommand extends BaseCommand implements CommandExecutor {

	private static Towny plugin;
	private static final List<String> output = new ArrayList<String>();

	static {
		output.add(ChatTools.formatTitle("/resident"));
		output.add(ChatTools.formatCommand("", "/resident", "", TownySettings.getLangString("res_1")));
		output.add(ChatTools.formatCommand("", "/resident", TownySettings.getLangString("res_2"), TownySettings.getLangString("res_3")));
		output.add(ChatTools.formatCommand("", "/resident", "list", TownySettings.getLangString("res_4")));
		output.add(ChatTools.formatCommand("", "/resident", "tax", ""));
		output.add(ChatTools.formatCommand("", "/resident", "jail", ""));
		output.add(ChatTools.formatCommand("", "/resident", "toggle", "[mode]...[mode]"));
		output.add(ChatTools.formatCommand("", "/resident", "set [] .. []", "'/resident set' " + TownySettings.getLangString("res_5")));
		output.add(ChatTools.formatCommand("", "/resident", "friend [add/remove] " + TownySettings.getLangString("res_2"), TownySettings.getLangString("res_6")));
		output.add(ChatTools.formatCommand("", "/resident", "friend [add+/remove+] " + TownySettings.getLangString("res_2") + " ", TownySettings.getLangString("res_7")));
		output.add(ChatTools.formatCommand("", "/resident", "spawn", ""));
		// output.add(ChatTools.formatCommand(TownySettings.getLangString("admin_sing"),
		// "/resident", "delete " + TownySettings.getLangString("res_2"), ""));
	}

	public ResidentCommand(Towny instance) {

		plugin = instance;
	}

	@Override
	public boolean onCommand(CommandSender sender, Command cmd, String commandLabel, String[] args) {

		if (sender instanceof Player) {
			Player player = (Player) sender;
			if (args == null) {
				for (String line : output)
					player.sendMessage(line);
				parseResidentCommand(player, args);
			} else {
				parseResidentCommand(player, args);
			}

		} else
			try {
				parseResidentCommandForConsole(sender, args);
			} catch (TownyException e) {
			}

		return true;
	}

	private void parseResidentCommandForConsole(final CommandSender sender, String[] split) throws TownyException {

		if (split.length == 0 || split[0].equalsIgnoreCase("?") || split[0].equalsIgnoreCase("help")) {
			
			for (String line : output)
				sender.sendMessage(line);
			
		} else if (split[0].equalsIgnoreCase("list")) {

			listResidents(sender);

		} else {
			try {
				final Resident resident = TownyUniverse.getDataSource().getResident(split[0]);
				Bukkit.getScheduler().runTaskAsynchronously(this.plugin, new Runnable() {
					@Override
				    public void run() {
						Player player = null;
						TownyMessaging.sendMessage(sender, TownyFormatter.getStatus(resident, player));
					}
				});
			} catch (NotRegisteredException x) {
				throw new TownyException(String.format(TownySettings.getLangString("msg_err_not_registered_1"), split[0]));
			}
		}
		
	}

	public void parseResidentCommand(final Player player, String[] split) {

		try {

			if (split.length == 0) {

				try {
					Resident resident = TownyUniverse.getDataSource().getResident(player.getName());
					TownyMessaging.sendMessage(player, TownyFormatter.getStatus(resident, player));
				} catch (NotRegisteredException x) {
					throw new TownyException(TownySettings.getLangString("msg_err_not_registered"));
				}

			} else if (split[0].equalsIgnoreCase("?") || split[0].equalsIgnoreCase("help")) {

				for (String line : output)
					player.sendMessage(line);

			} else if (split[0].equalsIgnoreCase("list")) {

				if (!TownyUniverse.getPermissionSource().testPermission(player, PermissionNodes.TOWNY_COMMAND_RESIDENT_LIST.getNode()))
					throw new TownyException(TownySettings.getLangString("msg_err_command_disable"));

				listResidents(player);

			} else if (split[0].equalsIgnoreCase("tax")) {

				if (!TownyUniverse.getPermissionSource().testPermission(player, PermissionNodes.TOWNY_COMMAND_RESIDENT_TAX.getNode()))
					throw new TownyException(TownySettings.getLangString("msg_err_command_disable"));

				try {
					Resident resident = TownyUniverse.getDataSource().getResident(player.getName());
					TownyMessaging.sendMessage(player, TownyFormatter.getTaxStatus(resident));
				} catch (NotRegisteredException x) {
					throw new TownyException(TownySettings.getLangString("msg_err_not_registered"));
				}
			
			} else if (split[0].equalsIgnoreCase("jail")) {

				if (!TownyUniverse.getPermissionSource().testPermission(player, PermissionNodes.TOWNY_COMMAND_RESIDENT_JAIL.getNode()))
					throw new TownyException(TownySettings.getLangString("msg_err_command_disable"));

				if (!TownySettings.isAllowingBail()) {
					TownyMessaging.sendErrorMsg(player, Colors.Red + "Bail is not enabled");
					return;
				}
				
				if (split.length == 1 ) {
					player.sendMessage(ChatTools.formatTitle("/resident jail"));
					player.sendMessage(ChatTools.formatCommand("", "/resident", "jail paybail", ""));
					player.sendMessage(Colors.LightBlue + "Bail costs: " + Colors.Green + TownySettings.getBailAmount());
					return;
				}

				if (!TownyUniverse.getDataSource().getResident(player.getName()).isJailed())
					return;
				
				if (split[1].equalsIgnoreCase("paybail")) {
					Resident resident = TownyUniverse.getDataSource().getResident(player.getName());
					if (resident.canPayFromHoldings(TownySettings.getBailAmount())) {
						Town JailTown = TownyUniverse.getDataSource().getTown(resident.getJailTown());
						resident.payTo(TownySettings.getBailAmount(), JailTown, "Bail");
						resident.setJailed(false);
						resident.setJailSpawn(0);
						resident.setJailTown("");
						TownyMessaging.sendGlobalMessage(Colors.Red + player.getName() + "has paid bail and is free.");
						player.teleport(resident.getTown().getSpawn());
						TownyUniverse.getDataSource().saveResident(resident);
					} else {
						TownyMessaging.sendErrorMsg(player, Colors.Red + "Unable to afford bail.");
					}
				} else {
					player.sendMessage(ChatTools.formatTitle("/resident jail"));
					player.sendMessage(ChatTools.formatCommand("", "/resident", "jail paybail", ""));
					player.sendMessage(Colors.LightBlue + "Bail costs: " + Colors.Green + TownySettings.getBailAmount());					
				}

			} else if (split[0].equalsIgnoreCase("set")) {

				/*
				 * perms checked in method.
				 */
				String[] newSplit = StringMgmt.remFirstArg(split);
				residentSet(player, newSplit);

			} else if (split[0].equalsIgnoreCase("toggle")) {

				/*
				 * 
				 */
				String[] newSplit = StringMgmt.remFirstArg(split);
				residentToggle(player, newSplit);

			} else if (split[0].equalsIgnoreCase("friend")) {

				if (!TownyUniverse.getPermissionSource().testPermission(player, PermissionNodes.TOWNY_COMMAND_RESIDENT_FRIEND.getNode()))
					throw new TownyException(TownySettings.getLangString("msg_err_command_disable"));

				String[] newSplit = StringMgmt.remFirstArg(split);
				residentFriend(player, newSplit);

			} else if (split[0].equalsIgnoreCase("spawn")) {

				if (!TownyUniverse.getPermissionSource().testPermission(player, PermissionNodes.TOWNY_COMMAND_RESIDENT_SPAWN.getNode()))
					throw new TownyException(TownySettings.getLangString("msg_err_command_disable"));

				residentSpawn(player);

			} else {

				try {
					final Resident resident = TownyUniverse.getDataSource().getResident(split[0]);
					if (!TownyUniverse.getPermissionSource().testPermission(player, PermissionNodes.TOWNY_COMMAND_RESIDENT_OTHERRESIDENT.getNode()) && (!resident.getName().equals(player.getName()))) {
						throw new TownyException(TownySettings.getLangString("msg_err_command_disable"));
					}
					Bukkit.getScheduler().runTaskAsynchronously(this.plugin, new Runnable() {
						@Override
					    public void run() {
							TownyMessaging.sendMessage(player, TownyFormatter.getStatus(resident, player));
						}
					});
				} catch (NotRegisteredException x) {
					throw new TownyException(String.format(TownySettings.getLangString("msg_err_not_registered_1"), split[0]));
				}

			}

		} catch (Exception x) {
			TownyMessaging.sendErrorMsg(player, x.getMessage());
		}
	}

	/**
	 * Attempt to send player to bed spawn.
	 * 
	 * @param player
	 */
	public void residentSpawn(Player player) {

		boolean isTownyAdmin = TownyUniverse.getPermissionSource().isTownyAdmin(player);
		Resident resident;

		try {

			resident = TownyUniverse.getDataSource().getResident(player.getName());
			Town town;
			Location spawnLoc;
			String notAffordMSG;
			TownSpawnLevel townSpawnPermission;

			// Set target town and affiliated messages.

			town = resident.getTown();
			notAffordMSG = TownySettings.getLangString("msg_err_cant_afford_tp");

			if (TownySettings.getBedUse() && player.getBedSpawnLocation() != null) {

				spawnLoc = player.getBedSpawnLocation();

			} else {
				spawnLoc = town.getSpawn();
			}

			if (isTownyAdmin) {
				townSpawnPermission = TownSpawnLevel.ADMIN;
			} else {
				townSpawnPermission = TownSpawnLevel.TOWN_RESIDENT;
			}

			if (!isTownyAdmin) {
				// Prevent spawn travel while in disallowed zones (if
				// configured)
				List<String> disallowedZones = TownySettings.getDisallowedTownSpawnZones();

				if (!disallowedZones.isEmpty()) {
					String inTown = null;
					try {
						Location loc = plugin.getCache(player).getLastLocation();
						inTown = TownyUniverse.getTownName(loc);
					} catch (NullPointerException e) {
						inTown = TownyUniverse.getTownName(player.getLocation());
					}

					if (inTown == null && disallowedZones.contains("unclaimed"))
						throw new TownyException(String.format(TownySettings.getLangString("msg_err_town_spawn_disallowed_from"), "the Wilderness"));
					if (inTown != null && resident.hasNation() && TownyUniverse.getDataSource().getTown(inTown).hasNation()) {
						Nation inNation = TownyUniverse.getDataSource().getTown(inTown).getNation();
						Nation playerNation = resident.getTown().getNation();
						if (inNation.hasEnemy(playerNation) && disallowedZones.contains("enemy"))
							throw new TownyException(String.format(TownySettings.getLangString("msg_err_town_spawn_disallowed_from"), "Enemy areas"));
						if (!inNation.hasAlly(playerNation) && !inNation.hasEnemy(playerNation) && disallowedZones.contains("neutral"))
							throw new TownyException(String.format(TownySettings.getLangString("msg_err_town_spawn_disallowed_from"), "Neutral towns"));
					}
				}
			}

			double travelCost = townSpawnPermission.getCost();

			// Check if need/can pay
			if (travelCost > 0 && TownySettings.isUsingEconomy() && (resident.getHoldingBalance() < travelCost))
				throw new TownyException(notAffordMSG);

			// Used later to make sure the chunk we teleport to is loaded.
			Chunk chunk = spawnLoc.getChunk();

			// Essentials tests
			boolean UsingESS = plugin.isEssentials();

			if (UsingESS && !isTownyAdmin) {
				try {
					User user = plugin.getEssentials().getUser(player);

					if (!user.isJailed()) {

						Teleport teleport = user.getTeleport();
						if (!chunk.isLoaded())
							chunk.load();
						// Cause an essentials exception if in cooldown.
						teleport.cooldown(true);
						teleport.teleport(spawnLoc, null);
					}
				} catch (Exception e) {
					TownyMessaging.sendErrorMsg(player, "Error: " + e.getMessage());
					// cooldown?
					return;
				}
			}

			// Show message if we are using iConomy and are charging for spawn
			// travel.
			if (travelCost > 0 && TownySettings.isUsingEconomy() && resident.payTo(travelCost, town, String.format("Resident Spawn (%s)", townSpawnPermission))) {
				TownyMessaging.sendMsg(player, String.format(TownySettings.getLangString("msg_cost_spawn"), TownyEconomyHandler.getFormattedBalance(travelCost))); // +
																																									// TownyEconomyObject.getEconomyCurrency()));
			}

			// If an Admin or Essentials teleport isn't being used, use our own.
			if (isTownyAdmin) {
				if (player.getVehicle() != null)
					player.getVehicle().eject();
				if (!chunk.isLoaded())
					chunk.load();
				player.teleport(spawnLoc, TeleportCause.COMMAND);
				return;
			}

			if (!UsingESS) {
				if (TownyTimerHandler.isTeleportWarmupRunning()) {
					// Use teleport warmup
					player.sendMessage(String.format(TownySettings.getLangString("msg_town_spawn_warmup"), TownySettings.getTeleportWarmupTime()));
					plugin.getTownyUniverse().requestTeleport(player, spawnLoc, travelCost);
				} else {
					// Don't use teleport warmup
					if (player.getVehicle() != null)
						player.getVehicle().eject();
					if (!chunk.isLoaded())
						chunk.load();
					player.teleport(spawnLoc, TeleportCause.COMMAND);
				}
			}
		} catch (TownyException e) {
			TownyMessaging.sendErrorMsg(player, e.getMessage());
		} catch (EconomyException e) {
			TownyMessaging.sendErrorMsg(player, e.getMessage());
		}

	}

	/**
	 * Toggle modes for this player.
	 * 
	 * @param player
	 * @param newSplit
	 * @throws TownyException
	 */
	private void residentToggle(Player player, String[] newSplit) throws TownyException {

		Resident resident;

		try {
			resident = TownyUniverse.getDataSource().getResident(player.getName());

		} catch (NotRegisteredException e) {
			// unknown resident
			throw new TownyException(String.format(TownySettings.getLangString("msg_err_not_registered"), player.getName()));
		}

		if (newSplit.length == 0) {
			player.sendMessage(ChatTools.formatTitle("/res toggle"));
			player.sendMessage(ChatTools.formatCommand("", "/res toggle", "pvp", ""));
			player.sendMessage(ChatTools.formatCommand("", "/res toggle", "fire", ""));
			player.sendMessage(ChatTools.formatCommand("", "/res toggle", "mobs", ""));
			player.sendMessage(ChatTools.formatCommand("", "/res toggle", "plotborder", ""));
			player.sendMessage(ChatTools.formatCommand("", "/res toggle", "spy", ""));

			TownyMessaging.sendMsg(resident, ("Modes set: " + StringMgmt.join(resident.getModes(), ",")));
			return;

		}

		if (!TownyUniverse.getPermissionSource().testPermission(player, PermissionNodes.TOWNY_COMMAND_RESIDENT_TOGGLE.getNode(newSplit[0].toLowerCase())))
			throw new TownyException(TownySettings.getLangString("msg_err_command_disable"));

		TownyPermission perm = resident.getPermissions();

		// Special case chat spy
		if (newSplit[0].equalsIgnoreCase("spy")) {
			
			if (!TownyUniverse.getPermissionSource().testPermission(player, PermissionNodes.TOWNY_CHAT_SPY.getNode(newSplit[0].toLowerCase())))
				throw new TownyException(TownySettings.getLangString("msg_err_command_disable"));
			
			resident.toggleMode(newSplit, true);
			return;
			
		} else if (newSplit[0].equalsIgnoreCase("pvp")) {
			perm.pvp = !perm.pvp;
		} else if (newSplit[0].equalsIgnoreCase("fire")) {
			perm.fire = !perm.fire;
		} else if (newSplit[0].equalsIgnoreCase("explosion")) {
			perm.explosion = !perm.explosion;
		} else if (newSplit[0].equalsIgnoreCase("mobs")) {
			perm.mobs = !perm.mobs;
		} else {

			resident.toggleMode(newSplit, true);
			return;

		}

		notifyPerms(player, perm);
		TownyUniverse.getDataSource().saveResident(resident);

	}

	/**
	 * Show the player the new Permission settings after the toggle.
	 * 
	 * @param player
	 * @param perm
	 */
	private void notifyPerms(Player player, TownyPermission perm) {

		TownyMessaging.sendMsg(player, TownySettings.getLangString("msg_set_perms"));
		TownyMessaging.sendMessage(player, Colors.Green + "PvP: " + ((perm.pvp) ? Colors.Red + "ON" : Colors.LightGreen + "OFF") + Colors.Green + "  Explosions: " + ((perm.explosion) ? Colors.Red + "ON" : Colors.LightGreen + "OFF") + Colors.Green + "  Firespread: " + ((perm.fire) ? Colors.Red + "ON" : Colors.LightGreen + "OFF") + Colors.Green + "  Mob Spawns: " + ((perm.mobs) ? Colors.Red + "ON" : Colors.LightGreen + "OFF"));

	}

	public void listResidents(Player player) {

		player.sendMessage(ChatTools.formatTitle(TownySettings.getLangString("res_list")));
		String colour;
		ArrayList<String> formatedList = new ArrayList<String>();
		for (Resident resident : plugin.getTownyUniverse().getActiveResidents()) {
			if (player.canSee(BukkitTools.getPlayerExact(resident.getName()))) {
				if (resident.isKing())
					colour = Colors.Gold;
				else if (resident.isMayor())
					colour = Colors.LightBlue;
				else
					colour = Colors.White;
				formatedList.add(colour + resident.getName() + Colors.White);
			}
		}
		for (String line : ChatTools.list(formatedList))
			player.sendMessage(line);
	}
	
	public void listResidents(CommandSender sender) {

		sender.sendMessage(ChatTools.formatTitle(TownySettings.getLangString("res_list")));
		String colour;
		ArrayList<String> formatedList = new ArrayList<String>();
		for (Resident resident : plugin.getTownyUniverse().getActiveResidents()) {
			if (resident.isKing())
				colour = Colors.Gold;
			else if (resident.isMayor())
				colour = Colors.LightBlue;
			else
				colour = Colors.White;
			formatedList.add(colour + resident.getName() + Colors.White);
		}
		for (String line : ChatTools.list(formatedList))
			sender.sendMessage(line);
	}

	/**
	 * 
	 * Command: /resident set [] ... []
	 * 
	 * @param player
	 * @param split
	 * @throws TownyException
	 */

	/*
	 * perm [resident/outsider] [build/destroy] [on/off]
	 */

	public void residentSet(Player player, String[] split) throws TownyException {

		if (split.length == 0) {
			player.sendMessage(ChatTools.formatCommand("", "/resident set", "perm ...", "'/resident set perm' " + TownySettings.getLangString("res_5")));
			player.sendMessage(ChatTools.formatCommand("", "/resident set", "mode ...", "'/resident set mode' " + TownySettings.getLangString("res_5")));
		} else {
			Resident resident;
			try {
				resident = TownyUniverse.getDataSource().getResident(player.getName());
			} catch (TownyException x) {
				TownyMessaging.sendErrorMsg(player, x.getMessage());
				return;
			}

			if (!TownyUniverse.getPermissionSource().testPermission(player, PermissionNodes.TOWNY_COMMAND_RESIDENT_SET.getNode(split[0].toLowerCase())))
				throw new TownyException(TownySettings.getLangString("msg_err_command_disable"));

			if (split[0].equalsIgnoreCase("perm")) {

				String[] newSplit = StringMgmt.remFirstArg(split);
				TownCommand.setTownBlockPermissions(player, resident, resident.getPermissions(), newSplit, true);

			} else if (split[0].equalsIgnoreCase("mode")) {

				String[] newSplit = StringMgmt.remFirstArg(split);
				setMode(player, newSplit);
			} else {

				TownyMessaging.sendErrorMsg(player, String.format(TownySettings.getLangString("msg_err_invalid_property"), "town"));
				return;

			}

			TownyUniverse.getDataSource().saveResident(resident);
		}
	}

	private void setMode(Player player, String[] split) {

		if (split.length == 0) {
			player.sendMessage(ChatTools.formatCommand("", "/resident set mode", "clear", ""));
			player.sendMessage(ChatTools.formatCommand("", "/resident set mode", "[mode] ...[mode]", ""));
			player.sendMessage(ChatTools.formatCommand("Mode", "map", "", TownySettings.getLangString("mode_1")));
			player.sendMessage(ChatTools.formatCommand("Mode", "townclaim", "", TownySettings.getLangString("mode_2")));
			player.sendMessage(ChatTools.formatCommand("Mode", "townunclaim", "", TownySettings.getLangString("mode_3")));
			player.sendMessage(ChatTools.formatCommand("Mode", "tc", "", TownySettings.getLangString("mode_4")));
			player.sendMessage(ChatTools.formatCommand("Mode", "nc", "", TownySettings.getLangString("mode_5")));
			// String warFlagMaterial = (TownyWarConfig.getFlagBaseMaterial() ==
			// null ? "flag" :
			// TownyWarConfig.getFlagBaseMaterial().name().toLowerCase());
			// player.sendMessage(ChatTools.formatCommand("Mode", "warflag", "",
			// String.format(TownySettings.getLangString("mode_6"),
			// warFlagMaterial)));
			player.sendMessage(ChatTools.formatCommand("Eg", "/resident set mode", "map townclaim town nation general", ""));

			return;
		}

		if (split[0].equalsIgnoreCase("reset") || split[0].equalsIgnoreCase("clear")) {
			plugin.removePlayerMode(player);
			return;
		}

		List<String> list = Arrays.asList(split);
		if ((list.contains("spy")) && (plugin.isPermissions() && !TownyUniverse.getPermissionSource().has(player, PermissionNodes.TOWNY_CHAT_SPY.getNode()))) {
			TownyMessaging.sendErrorMsg(player, TownySettings.getLangString("msg_err_command_disable"));
			return;
		}

		plugin.setPlayerMode(player, split, true);

	}

	public void residentFriend(Player player, String[] split) {

		if (split.length == 0) {
			player.sendMessage(ChatTools.formatCommand("", "/resident friend", "add " + TownySettings.getLangString("res_2"), ""));
			player.sendMessage(ChatTools.formatCommand("", "/resident friend", "remove " + TownySettings.getLangString("res_2"), ""));
			player.sendMessage(ChatTools.formatCommand("", "/resident friend", "clear", ""));
		} else {
			Resident resident;
			try {
				resident = TownyUniverse.getDataSource().getResident(player.getName());
			} catch (TownyException x) {
				TownyMessaging.sendErrorMsg(player, x.getMessage());
				return;
			}

			// TODO: Let admin's call a subfunction of this.
			if (split[0].equalsIgnoreCase("add")) {

				String[] names = StringMgmt.remFirstArg(split);
				residentFriendAdd(player, resident, TownyUniverse.getDataSource().getResidents(player, names));

			} else if (split[0].equalsIgnoreCase("remove")) {

				String[] names = StringMgmt.remFirstArg(split);
				residentFriendRemove(player, resident, TownyUniverse.getDataSource().getResidents(player, names));

			} else if (split[0].equalsIgnoreCase("clearlist") || split[0].equalsIgnoreCase("clear")) {

				residentFriendRemove(player, resident, resident.getFriends());

			}

		}
	}

	public void residentFriendAdd(Player player, Resident resident, List<Resident> invited) {

		ArrayList<Resident> remove = new ArrayList<Resident>();

		for (Resident newFriend : invited)

			try {

				resident.addFriend(newFriend);
				plugin.deleteCache(newFriend.getName());

			} catch (AlreadyRegisteredException e) {

				remove.add(newFriend);

			}

		/*
		 *  Remove any names from the list who were already listed as friends
		 */
		for (Resident newFriend : remove) {

			invited.remove(newFriend);

		}

		/*
		 * If we added any friends format the confirmation message.
		 */
		if (invited.size() > 0) {

			String msg = "Added ";

			for (Resident newFriend : invited) {

				msg += newFriend.getName() + ", ";
				Player p = BukkitTools.getPlayer(newFriend.getName());

				if (p != null) {

					TownyMessaging.sendMsg(p, String.format(TownySettings.getLangString("msg_friend_add"), player.getName()));

				}

			}

			msg = msg.substring(0, msg.length() - 2);
			msg += TownySettings.getLangString("msg_to_list");
			TownyMessaging.sendMsg(player, msg);
			TownyUniverse.getDataSource().saveResident(resident);

		} else {

			TownyMessaging.sendErrorMsg(player, TownySettings.getLangString("msg_invalid_name"));

		}
	}

	public void residentFriendRemove(Player player, Resident resident, List<Resident> kicking) {

		List<Resident> remove = new ArrayList<Resident>();
		List<Resident> toKick = new ArrayList<Resident>(kicking);

		for (Resident friend : toKick) {
			try {
				resident.removeFriend(friend);
				plugin.deleteCache(friend.getName());
			} catch (NotRegisteredException e) {
				remove.add(friend);
			}
		}
		// remove invalid names so we don't try to send them messages
		if (remove.size() > 0)
			for (Resident friend : remove)
				toKick.remove(friend);

		if (toKick.size() > 0) {
			String msg = TownySettings.getLangString("msg_removed");
			Player p;
			for (Resident member : toKick) {
				msg += member.getName() + ", ";
				p = BukkitTools.getPlayer(member.getName());
				if (p != null)
					TownyMessaging.sendMsg(p, String.format(TownySettings.getLangString("msg_friend_remove"), player.getName()));
			}
			msg = msg.substring(0, msg.length() - 2);
			msg += TownySettings.getLangString("msg_from_list");
			TownyMessaging.sendMsg(player, msg);
			TownyUniverse.getDataSource().saveResident(resident);
		} else
			TownyMessaging.sendErrorMsg(player, TownySettings.getLangString("msg_invalid_name"));

	}

	/**
	 * Overridden method custom for this command set.
	 * 
	 */
	@Override
	public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {

		LinkedList<String> output = new LinkedList<String>();
		String lastArg = "";

		// Get the last argument
		if (args.length > 0) {
			lastArg = args[args.length - 1].toLowerCase();
		}

		if (!lastArg.equalsIgnoreCase("")) {

			// Match residents
			for (Resident resident : TownyUniverse.getDataSource().getResidents()) {
				if (resident.getName().toLowerCase().startsWith(lastArg)) {
					output.add(resident.getName());
				}

			}

		}

		return output;
	}

}
