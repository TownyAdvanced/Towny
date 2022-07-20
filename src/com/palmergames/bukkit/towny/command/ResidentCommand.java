package com.palmergames.bukkit.towny.command;

import com.palmergames.bukkit.towny.Towny;
import com.palmergames.bukkit.towny.TownyAPI;
import com.palmergames.bukkit.towny.TownyCommandAddonAPI;
import com.palmergames.bukkit.towny.TownyEconomyHandler;
import com.palmergames.bukkit.towny.TownyFormatter;
import com.palmergames.bukkit.towny.TownyMessaging;
import com.palmergames.bukkit.towny.TownySettings;
import com.palmergames.bukkit.towny.TownyUniverse;
import com.palmergames.bukkit.towny.TownyCommandAddonAPI.CommandType;
import com.palmergames.bukkit.towny.confirmations.Confirmation;
import com.palmergames.bukkit.towny.exceptions.AlreadyRegisteredException;
import com.palmergames.bukkit.towny.exceptions.NoPermissionException;
import com.palmergames.bukkit.towny.exceptions.TownyException;
import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.towny.object.SpawnType;
import com.palmergames.bukkit.towny.object.Town;
import com.palmergames.bukkit.towny.object.TownyPermission;
import com.palmergames.bukkit.towny.object.Translatable;
import com.palmergames.bukkit.towny.object.Translator;
import com.palmergames.bukkit.towny.object.jail.UnJailReason;
import com.palmergames.bukkit.towny.permissions.PermissionNodes;
import com.palmergames.bukkit.towny.tasks.CooldownTimerTask;
import com.palmergames.bukkit.towny.tasks.CooldownTimerTask.CooldownType;
import com.palmergames.bukkit.towny.utils.JailUtil;
import com.palmergames.bukkit.towny.utils.NameUtil;
import com.palmergames.bukkit.towny.utils.ResidentUtil;
import com.palmergames.bukkit.towny.utils.SpawnUtil;
import com.palmergames.bukkit.util.BukkitTools;
import com.palmergames.bukkit.util.ChatTools;
import com.palmergames.bukkit.util.Colors;
import com.palmergames.util.StringMgmt;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Send a list of all towny resident help commands to player Command: /resident
 */

public class ResidentCommand extends BaseCommand implements CommandExecutor {

	private static Towny plugin;
	private static final List<String> residentTabCompletes = Arrays.asList(
		"friend",
		"list",
		"jail",
		"spawn",
		"toggle",
		"set",
		"tax"
	);
	
	private static final List<String> residentFriendTabCompletes = Arrays.asList(
		"add",
		"remove",
		"clear",
		"list"
	);

	private static final List<String> residentToggleTabCompletes = Arrays.asList(
		"pvp",
		"fire",
		"mobs",
		"explosion",
		"plotborder",
		"constantplotborder",
		"ignoreplots",
		"bordertitles",
		"townclaim",
		"map",
		"spy",
		"reset",
		"clear"
	);
	
	private static final List<String> residentModeTabCompletes = Arrays.asList(
		"map",
		"townclaim",
		"townunclaim",
		"plotborder",
		"constantplotborder",
		"ignoreplots",
		"reset",
		"clear"
	);
	
	private static final List<String> residentConsoleTabCompletes = Arrays.asList(
		"?",
		"help",
		"list"
	);
	
	private static final List<String> residentSetTabCompletes = Arrays.asList(
		"perm",
		"mode"
	);
	
	private static final List<String> residentToggleChoices = Arrays.asList(
		"pvp",
		"fire",
		"mobs",
		"explosion"
	);
	
	private static final List<String> residentToggleModes = new ArrayList<>(residentToggleTabCompletes).stream()
		.filter(str -> !residentToggleChoices.contains(str))
		.collect(Collectors.toList());

	private static final List<String> residentToggleModesUnionToggles = Stream.concat(
		new ArrayList<>(residentToggleModes).stream(),
		BaseCommand.setOnOffCompletes.stream()
	).collect(Collectors.toList());

	public ResidentCommand(Towny instance) {

		plugin = instance;
	}

	@Override
	public boolean onCommand(CommandSender sender, Command cmd, String commandLabel, String[] args) {
		
		if (sender instanceof Player) {
			if (plugin.isError()) {
				TownyMessaging.sendErrorMsg(sender, "Locked in Safe mode!");
				return false;
			}
			Player player = (Player) sender;
			if (args == null) {
				HelpMenu.RESIDENT_HELP.send(player);
				parseResidentCommand(player, args);
			} else {
				parseResidentCommand(player, args);
			}

		} else
			try {
				parseResidentCommandForConsole(sender, args);
			} catch (TownyException e) {
				TownyMessaging.sendErrorMsg(sender, e.getMessage());
			}

		return true;
	}

	@Override
	public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {

		if (sender instanceof Player) {
			switch (args[0].toLowerCase()) {
				case "tax":
					if (args.length == 2)
						return getTownyStartingWith(args[1], "r");
					break;
				case "jail":
					if (args.length == 2)
						return Collections.singletonList("paybail");
					break;
				case "toggle":
					if (args.length == 2) {
						return NameUtil.filterByStart(TownyCommandAddonAPI.getTabCompletes(CommandType.RESIDENT_TOGGLE, residentToggleTabCompletes), args[1]);
					} else if (args.length == 3 && residentToggleChoices.contains(args[1].toLowerCase())) {
						return NameUtil.filterByStart(BaseCommand.setOnOffCompletes, args[2]);
					} else if (args.length >= 3) {
						String prevArg = args[args.length - 2].toLowerCase();
						if (residentToggleModes.contains(prevArg)) {
							return NameUtil.filterByStart(residentToggleModesUnionToggles, args[args.length - 1]);
						} else if (BaseCommand.setOnOffCompletes.contains(prevArg)) {
							return NameUtil.filterByStart(residentToggleModes, args[args.length - 1]);
						}
					}
					break;
				case "set":
					if (args.length == 2)
						return NameUtil.filterByStart(TownyCommandAddonAPI.getTabCompletes(CommandType.RESIDENT_SET, residentSetTabCompletes), args[1]);
					if (args.length > 2) {
						if (TownyCommandAddonAPI.hasCommand(CommandType.RESIDENT_SET, args[1]))
							return NameUtil.filterByStart(TownyCommandAddonAPI.getAddonCommand(CommandType.RESIDENT_SET, args[1]).getTabCompletion(sender, StringMgmt.remFirstArg(args)), args[args.length-1]);

						switch (args[1].toLowerCase()) {
							case "mode":
								return NameUtil.filterByStart(residentModeTabCompletes, args[args.length - 1]);
							case "perm":
								return permTabComplete(StringMgmt.remArgs(args, 2));
							default:
								return Collections.emptyList();
						}
					}
					break;
				case "friend":
					switch (args.length) {
						case 2:
							return NameUtil.filterByStart(residentFriendTabCompletes, args[1]);
						case 3:
							if (args[1].equalsIgnoreCase("remove")) {
								Resident res = TownyUniverse.getInstance().getResident(((Player) sender).getUniqueId());
								if (res != null)
									return NameUtil.filterByStart(NameUtil.getNames(res.getFriends()), args[2]);
							} else {
								return getTownyStartingWith(args[2], "r");
							}
						default:
							return Collections.emptyList();
					}
				default:
					if (args.length == 1)
						return filterByStartOrGetTownyStartingWith(TownyCommandAddonAPI.getTabCompletes(CommandType.RESIDENT, residentTabCompletes), args[0], "r");
					else if (args.length > 1 && TownyCommandAddonAPI.hasCommand(CommandType.RESIDENT, args[0]))
						return NameUtil.filterByStart(TownyCommandAddonAPI.getAddonCommand(CommandType.RESIDENT, args[0]).getTabCompletion(sender, args), args[args.length-1]);
			}
		} else if (args.length == 1){
				return filterByStartOrGetTownyStartingWith(residentConsoleTabCompletes, args[0], "r");
		}

		return Collections.emptyList();
	}

	private void parseResidentCommandForConsole(final CommandSender sender, String[] split) throws TownyException {

		if (split.length == 0 || split[0].equalsIgnoreCase("?") || split[0].equalsIgnoreCase("help")) {
			HelpMenu.RESIDENT_HELP_CONSOLE.send(sender);
		} else if (split[0].equalsIgnoreCase("list")) {
			listResidents(sender);

		} else {
			final Optional<Resident> resOpt = Optional.ofNullable(TownyUniverse.getInstance().getResident(split[0]));
			if (resOpt.isPresent())
				Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> TownyMessaging.sendStatusScreen(sender, TownyFormatter.getStatus(resOpt.get(), sender)));
			else
				throw new TownyException(Translatable.of("msg_err_not_registered_1", split[0]));
		}
		
	}

	public void parseResidentCommand(final Player player, String[] split) {

		try {

			if (split.length == 0) {
				Resident res = getResidentOrThrow(player.getUniqueId());

				Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> TownyMessaging.sendStatusScreen(player, TownyFormatter.getStatus(res, player)));
			} else if (split[0].equalsIgnoreCase("?") || split[0].equalsIgnoreCase("help")) {
				
				HelpMenu.RESIDENT_HELP.send(player);

			} else if (split[0].equalsIgnoreCase("list")) {

				checkPermOrThrow(player, PermissionNodes.TOWNY_COMMAND_RESIDENT_LIST.getNode());

				listResidents(player);

			} else if (split[0].equalsIgnoreCase("tax")) {

				checkPermOrThrow(player, PermissionNodes.TOWNY_COMMAND_RESIDENT_TAX.getNode());

				if (!TownyEconomyHandler.isActive())
					throw new TownyException(Translatable.of("msg_err_no_economy"));
				
				Resident res;
				if (split.length > 1)
					res = getResidentOrThrow(split[1]);
				else
					res = getResidentOrThrow(player.getUniqueId());
				
				TownyMessaging.sendMessage(player, TownyFormatter.getTaxStatus(res, Translator.locale(player)));

			} else if (split[0].equalsIgnoreCase("jail")) {

				checkPermOrThrow(player, PermissionNodes.TOWNY_COMMAND_RESIDENT_JAIL.getNode());

				if (!TownySettings.isAllowingBail()) {
					TownyMessaging.sendErrorMsg(player, Translatable.of("msg_err_bail_not_enabled"));
					return;
				}
				
				if (split.length == 1 ) {
					HelpMenu.RESIDENT_JAIL_HELP.send(player);
					return;
				}

				Resident resident = getResidentOrThrow(player.getUniqueId());
				
				if (!resident.isJailed()) {
					TownyMessaging.sendErrorMsg(player, Translatable.of("msg_err_you_aren't currently jailed"));
					return;
				}
				
				if (split[1].equalsIgnoreCase("paybail")) {
					
					// Fail if the economy isn't active.
					if (!TownyEconomyHandler.isActive())
						throw new TownyException(Translatable.of("msg_err_no_economy"));

					// Get Town the player is jailed in.
					final Town jailTown = resident.getJailTown();

					// Set cost of bail.
					double cost = TownySettings.getBailAmount();
					if (resident.isMayor())
						cost = TownySettings.getBailAmountMayor();
					if (resident.isKing())
						cost = TownySettings.getBailAmountKing();
					
					if (cost > 0) {
						if (resident.getAccount().canPayFromHoldings(cost)) {
							final double finalCost = cost;
							Confirmation.runOnAccept(() -> {
								if (resident.getAccount().canPayFromHoldings(finalCost)) {
									resident.getAccount().payTo(finalCost, jailTown, "Bail");
									JailUtil.unJailResident(resident, UnJailReason.BAIL);
								} else {
									TownyMessaging.sendErrorMsg(player, Translatable.of("msg_err_unable_to_pay_bail"));
								}
							})
							.setTitle(Translatable.of("msg_confirm_purchase", TownyEconomyHandler.getFormattedBalance(finalCost)))
							.sendTo(player);
						} else {
							throw new TownyException(Translatable.of("msg_err_unable_to_pay_bail"));
						}					
						
					} else {
						// No cost, so they can pay bail.
						JailUtil.unJailResident(resident, UnJailReason.BAIL);
					}

				} else {
					HelpMenu.RESIDENT_JAIL_HELP.send(player);
					return;
				}

			} else if (split[0].equalsIgnoreCase("set")) {

				/* perms checked in method.*/
				residentSet(player, StringMgmt.remFirstArg(split));

			} else if (split[0].equalsIgnoreCase("toggle")) {
				/* perms checked in method. */
				residentToggle(player, StringMgmt.remFirstArg(split));

			} else if (split[0].equalsIgnoreCase("friend")) {

				checkPermOrThrow(player, PermissionNodes.TOWNY_COMMAND_RESIDENT_FRIEND.getNode());
				residentFriend(player, StringMgmt.remFirstArg(split), false, null);

			} else if (split[0].equalsIgnoreCase("spawn")) {

				checkPermOrThrow(player, PermissionNodes.TOWNY_COMMAND_RESIDENT_SPAWN.getNode());
				SpawnUtil.sendToTownySpawn(player, split, getResidentOrThrow(player.getUniqueId()), Translatable.of("msg_err_cant_afford_tp").forLocale(player), false, false, SpawnType.RESIDENT);

			} else if (TownyCommandAddonAPI.hasCommand(CommandType.RESIDENT, split[0])) {
				TownyCommandAddonAPI.getAddonCommand(CommandType.RESIDENT, split[0]).execute(player, "resident", split);
			} else {
				final Resident resident = TownyUniverse.getInstance().getResidentOpt(split[0])
											.orElseThrow(() -> new TownyException(Translatable.of("msg_err_not_registered_1", split[0])));

				if (!resident.getName().equals(player.getName()))
					checkPermOrThrow(player, PermissionNodes.TOWNY_COMMAND_RESIDENT_OTHERRESIDENT.getNode());

				Bukkit.getScheduler().runTaskAsynchronously(Towny.getPlugin(),
					() -> TownyMessaging.sendStatusScreen(player, TownyFormatter.getStatus(resident, player)));
			}

		} catch (TownyException x) {
			TownyMessaging.sendErrorMsg(player, x.getMessage(player));
		} catch (Exception e) {
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
		Resident resident = TownyUniverse.getInstance().getResidentOpt(player.getUniqueId())
							.orElseThrow(() -> new TownyException(Translatable.of("msg_err_not_registered_1", player.getName())));;

		if (newSplit.length == 0) {
			HelpMenu.RESIDENT_TOGGLE.send(player);

			TownyMessaging.sendMsg(resident, Translatable.of("msg_modes_set").append(StringMgmt.join(resident.getModes(), ",")));
			return;

		}
		
		// Check if we're reseting before trying for nodes.
		if (newSplit[0].equalsIgnoreCase("reset") || newSplit[0].equalsIgnoreCase("clear")) {
			plugin.removePlayerMode(player);
			return;
		}
		TownyPermission perm = resident.getPermissions();
		
		Optional<Boolean> choice = Optional.empty();
		if (newSplit.length == 2 && residentToggleChoices.contains(newSplit[0].toLowerCase())) {
			choice = BaseCommand.parseToggleChoice(newSplit[1]);
		}

		// Special case chat spy
		if (StringMgmt.containsIgnoreCase(Arrays.asList(newSplit), "spy")) {
			checkPermOrThrow(player, PermissionNodes.TOWNY_CHAT_SPY.getNode());

			resident.toggleMode(newSplit, true);
			return;
			
		} else if (newSplit[0].equalsIgnoreCase("pvp")) {
			checkPermOrThrow(player, PermissionNodes.TOWNY_COMMAND_RESIDENT_TOGGLE_PVP.getNode());
			
			// Test to see if the pvp cooldown timer is active for the town this resident belongs to.
			if (TownySettings.getPVPCoolDownTime() > 0 && resident.hasTown() && !resident.isAdmin()) {
				if (CooldownTimerTask.hasCooldown(resident.getTownOrNull().getUUID().toString(), CooldownType.PVP))
					throw new TownyException(Translatable.of("msg_err_cannot_toggle_pvp_x_seconds_remaining", CooldownTimerTask.getCooldownRemaining(resident.getTownOrNull().getUUID().toString(), CooldownType.PVP))); 
				if (CooldownTimerTask.hasCooldown(resident.getName(), CooldownType.PVP))
					throw new TownyException(Translatable.of("msg_err_cannot_toggle_pvp_x_seconds_remaining", CooldownTimerTask.getCooldownRemaining(resident.getName(), CooldownType.PVP)));

			}
			perm.pvp = choice.orElse(!perm.pvp);
			// Add a task for the resident.
			if (TownySettings.getPVPCoolDownTime() > 0 && !resident.isAdmin())
				CooldownTimerTask.addCooldownTimer(resident.getName(), CooldownType.PVP);
		} else if (newSplit[0].equalsIgnoreCase("fire")) {
			checkPermOrThrow(player, PermissionNodes.TOWNY_COMMAND_RESIDENT_TOGGLE_FIRE.getNode());
			perm.fire = choice.orElse(!perm.fire);
		} else if (newSplit[0].equalsIgnoreCase("explosion")) {
			checkPermOrThrow(player, PermissionNodes.TOWNY_COMMAND_RESIDENT_TOGGLE_EXPLOSION.getNode());
			perm.explosion = choice.orElse(!perm.explosion);
		} else if (newSplit[0].equalsIgnoreCase("mobs")) {
			checkPermOrThrow(player, PermissionNodes.TOWNY_COMMAND_RESIDENT_TOGGLE_MOBS.getNode());
			perm.mobs = choice.orElse(!perm.mobs);
		} else if (newSplit[0].equalsIgnoreCase("bordertitles")) {
			checkPermOrThrow(player, PermissionNodes.TOWNY_COMMAND_RESIDENT_TOGGLE_BORDERTITLES.getNode());
			ResidentUtil.toggleResidentBorderTitles(resident, choice);
			return;
		} else if (TownyCommandAddonAPI.hasCommand(CommandType.RESIDENT_TOGGLE, newSplit[0])) {
			TownyCommandAddonAPI.getAddonCommand(CommandType.RESIDENT_TOGGLE, newSplit[0]).execute(player, "resident", newSplit);
		} else {

			resident.toggleMode(newSplit, true);
			return;

		}

		notifyPerms(player, perm);
		resident.save();

	}

	/**
	 * Show the player the new Permission settings after the toggle.
	 * 
	 * @param player
	 * @param perm
	 */
	private void notifyPerms(Player player, TownyPermission perm) {

		TownyMessaging.sendMsg(player, Translatable.of("msg_set_perms"));
		TownyMessaging.sendMessage(player, Colors.Green + "PvP: " + ((perm.pvp) ? Colors.Red + "ON" : Colors.LightGreen + "OFF") + Colors.Green + "  Explosions: " + ((perm.explosion) ? Colors.Red + "ON" : Colors.LightGreen + "OFF") + Colors.Green + "  Firespread: " + ((perm.fire) ? Colors.Red + "ON" : Colors.LightGreen + "OFF") + Colors.Green + "  Mob Spawns: " + ((perm.mobs) ? Colors.Red + "ON" : Colors.LightGreen + "OFF"));

	}
	
	public void listResidents(CommandSender sender) {

		TownyMessaging.sendMessage(sender, ChatTools.formatTitle(Translatable.of("res_list").forLocale(sender)));
		String colour;
		List<String> formattedList = new ArrayList<>();
		
		for (Player player : BukkitTools.getVisibleOnlinePlayers(sender)) {
			Resident resident = TownyAPI.getInstance().getResident(player);
			if (resident == null) {
				formattedList.add(Colors.White + player.getName() + Colors.White);
				continue;
			}
			
			if (resident.isKing())
				colour = Colors.Gold;
			else if (resident.isMayor())
				colour = Colors.LightBlue;
			else
				colour = Colors.White;

			formattedList.add(colour + resident.getName() + Colors.White);
		}
		
		TownyMessaging.sendMessage(sender, ChatTools.list(formattedList));
	}

	/**
	 * 
	 * Command: /resident set [] ... []
	 * 
	 * @param player - Player.
	 * @param split  - Current command arguments.
	 * @throws TownyException - Exception.
	 */

	/*
	 * perm [resident/outsider] [build/destroy] [on/off]
	 */

	public void residentSet(Player player, String[] split) throws TownyException {
		TownyUniverse townyUniverse = TownyUniverse.getInstance();

		if (split.length == 0) {
			TownyMessaging.sendMessage(player, ChatTools.formatCommand("", "/resident set", "perm ...", "'/resident set perm' " + Translatable.of("res_5").forLocale(player)));
			TownyMessaging.sendMessage(player, ChatTools.formatCommand("", "/resident set", "mode ...", "'/resident set mode' " + Translatable.of("res_5").forLocale(player)));
		} else {
			Optional<Resident> resOpt = townyUniverse.getResidentOpt(player.getUniqueId());
			
			if (!resOpt.isPresent()) {
				TownyMessaging.sendErrorMsg(player, Translatable.of("msg_err_not_registered_1", player.getName()));
				return;
			}

			Resident resident = resOpt.get();

			if (split[0].equalsIgnoreCase("perm")) {

				checkPermOrThrow(player, PermissionNodes.TOWNY_COMMAND_RESIDENT_SET_PERM.getNode());
				TownCommand.setTownBlockPermissions(player, resident, resident.getPermissions(), StringMgmt.remFirstArg(split), true);

			} else if (split[0].equalsIgnoreCase("mode")) {

				checkPermOrThrow(player, PermissionNodes.TOWNY_COMMAND_RESIDENT_SET_MODE.getNode());
				setMode(player, StringMgmt.remFirstArg(split));

			} else if (TownyCommandAddonAPI.hasCommand(CommandType.RESIDENT_SET, split[0])) {
				TownyCommandAddonAPI.getAddonCommand(CommandType.RESIDENT_SET, split[0]).execute(player, "resident", split);
			} else {

				TownyMessaging.sendErrorMsg(player, Translatable.of("msg_err_invalid_property", "resident"));
				return;

			}
			
			resident.save();
		}
	}

	private void setMode(Player player, String[] split) throws NoPermissionException {

		if (split.length == 0) {
			TownyMessaging.sendMessage(player, ChatTools.formatCommand("", "/resident set mode", "clear", ""));
			TownyMessaging.sendMessage(player, ChatTools.formatCommand("", "/resident set mode", "[mode] ...[mode]", ""));
			TownyMessaging.sendMessage(player, ChatTools.formatCommand("Mode", "map", "", Translatable.of("mode_1").forLocale(player)));
			TownyMessaging.sendMessage(player, ChatTools.formatCommand("Mode", "townclaim", "", Translatable.of("mode_2").forLocale(player)));
			TownyMessaging.sendMessage(player, ChatTools.formatCommand("Mode", "townunclaim", "", Translatable.of("mode_3").forLocale(player)));
			TownyMessaging.sendMessage(player, ChatTools.formatCommand("Mode", "tc", "", Translatable.of("mode_4").forLocale(player)));
			TownyMessaging.sendMessage(player, ChatTools.formatCommand("Mode", "nc", "", Translatable.of("mode_5").forLocale(player)));
			TownyMessaging.sendMessage(player, ChatTools.formatCommand("Mode", "ignoreplots", "", ""));
			TownyMessaging.sendMessage(player, ChatTools.formatCommand("Mode", "constantplotborder", "", ""));
			TownyMessaging.sendMessage(player, ChatTools.formatCommand("Mode", "plotborder", "", ""));
			TownyMessaging.sendMessage(player, ChatTools.formatCommand("Eg", "/resident set mode", "map townclaim town nation general", ""));

			return;
		}

		if (split[0].equalsIgnoreCase("reset") || split[0].equalsIgnoreCase("clear")) {
			plugin.removePlayerMode(player);
			return;
		}

		List<String> list = Arrays.asList(split);
		if (list.contains("spy"))
			checkPermOrThrow(player, PermissionNodes.TOWNY_CHAT_SPY.getNode());

		plugin.setPlayerMode(player, split, true);

	}

	public static void residentFriend(Player player, String[] split, boolean admin, Resident resident) {
		TownyUniverse townyUniverse = TownyUniverse.getInstance();

		if (split.length == 0) {
			TownyMessaging.sendMessage(player, ChatTools.formatCommand("", "/resident friend", "add " + Translatable.of("res_2").forLocale(player), ""));
			TownyMessaging.sendMessage(player, ChatTools.formatCommand("", "/resident friend", "remove " + Translatable.of("res_2").forLocale(player), ""));
			TownyMessaging.sendMessage(player, ChatTools.formatCommand("", "/resident friend", "list", ""));
			TownyMessaging.sendMessage(player, ChatTools.formatCommand("", "/resident friend", "clear", ""));
		} else {
			if (!admin) {
				Optional<Resident> resOpt = townyUniverse.getResidentOpt(player.getUniqueId());

				if (!resOpt.isPresent()) {
					TownyMessaging.sendErrorMsg(player, Translatable.of("msg_err_not_registered_1", player.getName()));
					return;
				}
				
				resident = resOpt.get();
			}

			if (split[0].equalsIgnoreCase("add")) {

				String[] names = StringMgmt.remFirstArg(split);
				List<Resident> invited = new ArrayList<>();
				for (String name : names) {
					Resident target = townyUniverse.getResident(name);
					if (target == null) {
						TownyMessaging.sendErrorMsg(player, Translatable.of("msg_err_not_registered_1", name));
					} else {
						invited.add(target);
					}
				}
				residentFriendAdd(player, resident, invited);

			} else if (split[0].equalsIgnoreCase("remove")) {

				String[] names = StringMgmt.remFirstArg(split);
				List<Resident> invited = new ArrayList<>();
				for (String name : names) {
					Resident target = townyUniverse.getResident(name);
					if (target == null) {
						TownyMessaging.sendErrorMsg(player, Translatable.of("msg_err_not_registered_1", name));
					} else {
						invited.add(target);
					}
				}
				residentFriendRemove(player, resident, invited);
			
			} else if (split[0].equalsIgnoreCase("list")) {

				residentFriendList(player, resident);

			} else if (split[0].equalsIgnoreCase("clearlist") || split[0].equalsIgnoreCase("clear")) {

				residentFriendRemove(player, resident, resident.getFriends());

			}

		}
	}

	private static void residentFriendList(Player player, Resident resident) {
		
		TownyMessaging.sendMessage(player, ChatTools.formatTitle(Translatable.of("friend_list").forLocale(player)));
		String colour;
		ArrayList<String> formatedList = new ArrayList<>();
		for (Resident friends : resident.getFriends()) {
			if (friends.isKing())
				colour = Colors.Gold;
			else if (friends.isMayor())
				colour = Colors.LightBlue;
			else
				colour = Colors.White;
			formatedList.add(colour + friends.getName() + Colors.White);
		}
		
		TownyMessaging.sendMessage(player, ChatTools.list(formatedList));
	}

	public static void residentFriendAdd(Player player, Resident resident, List<Resident> invited) {

		ArrayList<Resident> remove = new ArrayList<>();

		for (Resident newFriend : invited) {
			try {

				resident.addFriend(newFriend);
				plugin.deleteCache(newFriend);

			} catch (AlreadyRegisteredException e) {

				remove.add(newFriend);

			}
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
			for (Resident newFriend : invited)
				TownyMessaging.sendMsg(newFriend, Translatable.of("msg_friend_add", player.getName()));
			TownyMessaging.sendMsg(player, Translatable.of("msg_res_friend_added_to_list", StringMgmt.join(invited, ", ")));
			resident.save();
		} else
			TownyMessaging.sendErrorMsg(player, Translatable.of("msg_invalid_name"));
	}

	public static void residentFriendRemove(Player player, Resident resident, List<Resident> kicking) {

		List<Resident> remove = new ArrayList<>();
		List<Resident> toKick = new ArrayList<>(kicking);

		for (Resident friend : toKick) {
			if (!resident.hasFriend(friend))
				remove.add(friend);
			else {
				resident.removeFriend(friend);
				plugin.deleteCache(friend);
			}
		}
		// remove invalid names so we don't try to send them messages
		if (remove.size() > 0)
			for (Resident friend : remove)
				toKick.remove(friend);

		if (toKick.size() > 0) {
			for (Resident member : toKick)
				TownyMessaging.sendMsg(member, Translatable.of("msg_friend_remove", player.getName()));
			TownyMessaging.sendMsg(player, Translatable.of("msg_res_friend_removed_from_list", StringMgmt.join(toKick, ", ")));
			resident.save();
		} else
			TownyMessaging.sendErrorMsg(player, Translatable.of("msg_invalid_name"));
	}
}
