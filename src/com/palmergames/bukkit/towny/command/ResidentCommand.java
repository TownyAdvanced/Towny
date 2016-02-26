package com.palmergames.bukkit.towny.command;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerTeleportEvent.TeleportCause;

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

/**
 * Send a list of all towny resident help commands to player Command: /resident
 */

public class ResidentCommand extends BaseCommand implements CommandExecutor {

	private static Towny plugin;
	private static final List<String> output = new ArrayList<String>();

	static {
		output.add(ChatTools.formatTitle("/주민"));
		output.add(ChatTools.formatCommand("", "/주민", "", TownySettings.getLangString("res_1")));
		output.add(ChatTools.formatCommand("", "/주민", TownySettings.getLangString("res_2"), TownySettings.getLangString("res_3")));
		output.add(ChatTools.formatCommand("", "/주민", "목록", TownySettings.getLangString("res_4")));
		output.add(ChatTools.formatCommand("", "/주민", "세금", ""));
		output.add(ChatTools.formatCommand("", "/주민", "감옥", ""));
		output.add(ChatTools.formatCommand("", "/주민", "토글", "[모드]...[모드]"));
		output.add(ChatTools.formatCommand("", "/주민", "설정 [] .. []", "'/주민 설정' " + TownySettings.getLangString("res_5")));
		output.add(ChatTools.formatCommand("", "/주민", "친구 [추가/제거] " + TownySettings.getLangString("res_2"), TownySettings.getLangString("res_6")));
		output.add(ChatTools.formatCommand("", "/주민", "친구 [추가+/제거+] " + TownySettings.getLangString("res_2") + " ", TownySettings.getLangString("res_7")));
		output.add(ChatTools.formatCommand("", "/주민", "스폰", ""));
		// output.add(ChatTools.formatCommand(TownySettings.getLangString("admin_sing"),
		// "/주민", "delete " + TownySettings.getLangString("res_2"), ""));
	}

	public ResidentCommand(Towny instance) {

		plugin = instance;
	}

	@Override
	public boolean onCommand(CommandSender sender, Command cmd, String commandLabel, String[] args) {

		if (sender instanceof Player) {
			Player player = (Player) sender;
			System.out.println("[PLAYER_COMMAND] " + player.getName() + ": /" + commandLabel + " " + StringMgmt.join(args));
			if (args == null) {
				for (String line : output)
					player.sendMessage(line);
				parseResidentCommand(player, args);
			} else {
				parseResidentCommand(player, args);
			}

		} else
			// Console
			for (String line : output)
				sender.sendMessage(Colors.strip(line));
		return true;
	}

	public void parseResidentCommand(Player player, String[] split) {

		try {

			if (split.length == 0) {

				try {
					Resident resident = TownyUniverse.getDataSource().getResident(player.getName());
					TownyMessaging.sendMessage(player, TownyFormatter.getStatus(resident, player));
				} catch (NotRegisteredException x) {
					throw new TownyException(TownySettings.getLangString("msg_err_not_registered"));
				}

			} else if (split[0].equalsIgnoreCase("?") || split[0].equalsIgnoreCase("help") || split[0].equalsIgnoreCase("도움말")) {

				for (String line : output)
					player.sendMessage(line);

			} else if (split[0].equalsIgnoreCase("list") || split[0].equalsIgnoreCase("목록")) {

				if (!TownyUniverse.getPermissionSource().testPermission(player, PermissionNodes.TOWNY_COMMAND_RESIDENT_LIST.getNode()))
					throw new TownyException(TownySettings.getLangString("msg_err_command_disable"));

				listResidents(player);

			} else if (split[0].equalsIgnoreCase("tax") || split[0].equalsIgnoreCase("세금")) {

				if (!TownyUniverse.getPermissionSource().testPermission(player, PermissionNodes.TOWNY_COMMAND_RESIDENT_TAX.getNode()))
					throw new TownyException(TownySettings.getLangString("msg_err_command_disable"));

				try {
					Resident resident = TownyUniverse.getDataSource().getResident(player.getName());
					TownyMessaging.sendMessage(player, TownyFormatter.getTaxStatus(resident));
				} catch (NotRegisteredException x) {
					throw new TownyException(TownySettings.getLangString("msg_err_not_registered"));
				}
			
			} else if (split[0].equalsIgnoreCase("jail") || split[0].equalsIgnoreCase("감옥")) {

				if (!TownyUniverse.getPermissionSource().testPermission(player, PermissionNodes.TOWNY_COMMAND_RESIDENT_TAX.getNode()))
					throw new TownyException(TownySettings.getLangString("msg_err_command_disable"));

				if (!TownySettings.isAllowingBail()) {
					TownyMessaging.sendErrorMsg(player, Colors.Red + "보석금 시스템이 비활성화 되어있습니다.");
					return;
				}
				
				if (split.length == 1 ) {
					player.sendMessage(ChatTools.formatTitle("/주민 감옥"));
					player.sendMessage(ChatTools.formatCommand("", "/주민", "감옥 보석금", ""));
					player.sendMessage(Colors.LightBlue + "보석금: " + Colors.Green + TownySettings.getBailAmount());
					return;
				}

				if (!TownyUniverse.getDataSource().getResident(player.getName()).isJailed())
					return;
				
				if (split[1].equalsIgnoreCase("paybail") || split[1].equalsIgnoreCase("보석금")) {
					Resident resident = TownyUniverse.getDataSource().getResident(player.getName());
					if (resident.canPayFromHoldings(TownySettings.getBailAmount())) {
						Town JailTown = TownyUniverse.getDataSource().getTown(resident.getJailTown());
						resident.payTo(TownySettings.getBailAmount(), JailTown, "보석금");
						resident.setJailed(false);
						resident.setJailSpawn(0);
						resident.setJailTown("");
						TownyMessaging.sendGlobalMessage(Colors.Red + player.getName() + "님이 보석금을 내고 풀려났습니다.");
						player.teleport(resident.getTown().getSpawn());
						TownyUniverse.getDataSource().saveResident(resident);
					} else {
						TownyMessaging.sendErrorMsg(player, Colors.Red + "소지금이 부족합니다.");
					}
				} else {
					player.sendMessage(ChatTools.formatTitle("/주민 감옥"));
					player.sendMessage(ChatTools.formatCommand("", "/주민", "감옥 보석금", ""));
					player.sendMessage(Colors.LightBlue + "보석금: " + Colors.Green + TownySettings.getBailAmount());					
				}

			} else if (split[0].equalsIgnoreCase("set") || split[0].equalsIgnoreCase("설정")) {

				/*
				 * perms checked in method.
				 */
				String[] newSplit = StringMgmt.remFirstArg(split);
				residentSet(player, newSplit);

			} else if (split[0].equalsIgnoreCase("toggle") || split[0].equalsIgnoreCase("토글")) {

				/*
				 * 
				 */
				String[] newSplit = StringMgmt.remFirstArg(split);
				residentToggle(player, newSplit);

			} else if (split[0].equalsIgnoreCase("friend") || split[0].equalsIgnoreCase("친구")) {

				if (!TownyUniverse.getPermissionSource().testPermission(player, PermissionNodes.TOWNY_COMMAND_RESIDENT_FRIEND.getNode()))
					throw new TownyException(TownySettings.getLangString("msg_err_command_disable"));

				String[] newSplit = StringMgmt.remFirstArg(split);
				residentFriend(player, newSplit);

			} else if (split[0].equalsIgnoreCase("spawn") || split[0].equalsIgnoreCase("스폰")) {

				if (!TownyUniverse.getPermissionSource().testPermission(player, PermissionNodes.TOWNY_COMMAND_RESIDENT_SPAWN.getNode()))
					throw new TownyException(TownySettings.getLangString("msg_err_command_disable"));

				residentSpawn(player);

			} else {

				try {
					Resident resident = TownyUniverse.getDataSource().getResident(split[0]);
					TownyMessaging.sendMessage(player, TownyFormatter.getStatus(resident, player));
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
						throw new TownyException(String.format(TownySettings.getLangString("msg_err_town_spawn_disallowed_from"), "야생"));
					if (inTown != null && resident.hasNation() && TownyUniverse.getDataSource().getTown(inTown).hasNation()) {
						Nation inNation = TownyUniverse.getDataSource().getTown(inTown).getNation();
						Nation playerNation = resident.getTown().getNation();
						if (inNation.hasEnemy(playerNation) && disallowedZones.contains("enemy"))
							throw new TownyException(String.format(TownySettings.getLangString("msg_err_town_spawn_disallowed_from"), "적의 영토"));
						if (!inNation.hasAlly(playerNation) && !inNation.hasEnemy(playerNation) && disallowedZones.contains("neutral"))
							throw new TownyException(String.format(TownySettings.getLangString("msg_err_town_spawn_disallowed_from"), "평화로운 마을"));
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
			if (travelCost > 0 && TownySettings.isUsingEconomy() && resident.payTo(travelCost, town, String.format("주민 스폰 (%s)", townSpawnPermission))) {
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
			player.sendMessage(ChatTools.formatTitle("/주민 토글"));
			player.sendMessage(ChatTools.formatCommand("", "/주민 토글", "pvp", ""));
			player.sendMessage(ChatTools.formatCommand("", "/주민 토글", "불", ""));
			player.sendMessage(ChatTools.formatCommand("", "/주민 토글", "몹", ""));
			player.sendMessage(ChatTools.formatCommand("", "/주민 토글", "토지경계", ""));
			player.sendMessage(ChatTools.formatCommand("", "/주민 토글", "스파이", ""));

			TownyMessaging.sendMsg(resident, ("모드 설정됨: " + StringMgmt.join(resident.getModes(), ",")));
			return;

		}

		if (!TownyUniverse.getPermissionSource().testPermission(player, PermissionNodes.TOWNY_COMMAND_RESIDENT_TOGGLE.getNode(newSplit[0].toLowerCase())))
			throw new TownyException(TownySettings.getLangString("msg_err_command_disable"));

		TownyPermission perm = resident.getPermissions();

		// Special case chat spy
		if (newSplit[0].equalsIgnoreCase("spy") || newSplit[0].equalsIgnoreCase("스파이")) {
			
			if (!TownyUniverse.getPermissionSource().testPermission(player, PermissionNodes.TOWNY_CHAT_SPY.getNode(newSplit[0].toLowerCase())))
				throw new TownyException(TownySettings.getLangString("msg_err_command_disable"));
			
			resident.toggleMode(newSplit, true);
			return;
			
		} else if (newSplit[0].equalsIgnoreCase("pvp")) {
			perm.pvp = !perm.pvp;
		} else if (newSplit[0].equalsIgnoreCase("fire") || newSplit[0].equalsIgnoreCase("불")) {
			perm.fire = !perm.fire;
		} else if (newSplit[0].equalsIgnoreCase("explosion") || newSplit[0].equalsIgnoreCase("폭발")) {
			perm.explosion = !perm.explosion;
		} else if (newSplit[0].equalsIgnoreCase("mobs") || newSplit[0].equalsIgnoreCase("몹")) {
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
		TownyMessaging.sendMessage(player, Colors.Green + "PvP: " + ((perm.pvp) ? Colors.Red + "켜짐" : Colors.LightGreen + "꺼짐") + Colors.Green + "  폭발: " + ((perm.explosion) ? Colors.Red + "켜짐" : Colors.LightGreen + "꺼짐") + Colors.Green + "  불번짐: " + ((perm.fire) ? Colors.Red + "켜짐" : Colors.LightGreen + "꺼짐") + Colors.Green + "  몹 스폰: " + ((perm.mobs) ? Colors.Red + "켜짐" : Colors.LightGreen + "꺼짐"));

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
			player.sendMessage(ChatTools.formatCommand("", "/주민 설정", "권한...", "'/주민 설정 권한' " + TownySettings.getLangString("res_5")));
			player.sendMessage(ChatTools.formatCommand("", "/주민 설정", "모드 ...", "'/주민 설정 모드' " + TownySettings.getLangString("res_5")));
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

			if (split[0].equalsIgnoreCase("perm") || split[0].equalsIgnoreCase("권한")) {

				String[] newSplit = StringMgmt.remFirstArg(split);
				TownCommand.setTownBlockPermissions(player, resident, resident.getPermissions(), newSplit, true);

			} else if (split[0].equalsIgnoreCase("mode") || split[0].equalsIgnoreCase("모드")) {

				String[] newSplit = StringMgmt.remFirstArg(split);
				setMode(player, newSplit);
			} else {

				TownyMessaging.sendErrorMsg(player, String.format(TownySettings.getLangString("msg_err_invalid_property"), "마을"));
				return;

			}

			TownyUniverse.getDataSource().saveResident(resident);
		}
	}

	private void setMode(Player player, String[] split) {

		if (split.length == 0) {
			player.sendMessage(ChatTools.formatCommand("", "/주민 설정 모드", "초기화", ""));
			player.sendMessage(ChatTools.formatCommand("", "/주민 설정 모드", "[모드] ...[모드]", ""));
			player.sendMessage(ChatTools.formatCommand("모드", "map", "", TownySettings.getLangString("mode_1")));
			player.sendMessage(ChatTools.formatCommand("모드", "townclaim", "", TownySettings.getLangString("mode_2")));
			player.sendMessage(ChatTools.formatCommand("모드", "townunclaim", "", TownySettings.getLangString("mode_3")));
			player.sendMessage(ChatTools.formatCommand("모드", "tc", "", TownySettings.getLangString("mode_4")));
			player.sendMessage(ChatTools.formatCommand("모드", "nc", "", TownySettings.getLangString("mode_5")));
			// String warFlagMaterial = (TownyWarConfig.getFlagBaseMaterial() ==
			// null ? "flag" :
			// TownyWarConfig.getFlagBaseMaterial().name().toLowerCase());
			// player.sendMessage(ChatTools.formatCommand("Mode", "warflag", "",
			// String.format(TownySettings.getLangString("mode_6"),
			// warFlagMaterial)));
			player.sendMessage(ChatTools.formatCommand("예시", "/주민 설정 모드", "map townclaim town nation general", ""));

			return;
		}

		if (split[0].equalsIgnoreCase("reset") || split[0].equalsIgnoreCase("clear") || split[0].equalsIgnoreCase("초기화")) {
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
			player.sendMessage(ChatTools.formatCommand("", "/주민 친구", "추가 " + TownySettings.getLangString("res_2"), ""));
			player.sendMessage(ChatTools.formatCommand("", "/주민 친구", "제거 " + TownySettings.getLangString("res_2"), ""));
			player.sendMessage(ChatTools.formatCommand("", "/주민 친구", "초기화", ""));
		} else {
			Resident resident;
			try {
				resident = TownyUniverse.getDataSource().getResident(player.getName());
			} catch (TownyException x) {
				TownyMessaging.sendErrorMsg(player, x.getMessage());
				return;
			}

			// TODO: Let admin's call a subfunction of this.
			if (split[0].equalsIgnoreCase("add") || split[0].equalsIgnoreCase("추가")) {

				String[] names = StringMgmt.remFirstArg(split);
				residentFriendAdd(player, resident, TownyUniverse.getDataSource().getResidents(player, names));

			} else if (split[0].equalsIgnoreCase("remove") || split[0].equalsIgnoreCase("제거")) {

				String[] names = StringMgmt.remFirstArg(split);
				residentFriendRemove(player, resident, TownyUniverse.getDataSource().getResidents(player, names));

			} else if (split[0].equalsIgnoreCase("clearlist") || split[0].equalsIgnoreCase("clear") || split[0].equalsIgnoreCase("초기화")) {

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
			;
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
