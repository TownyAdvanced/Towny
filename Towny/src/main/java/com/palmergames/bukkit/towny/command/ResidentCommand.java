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
import com.palmergames.bukkit.towny.exceptions.TownyException;
import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.towny.object.SpawnType;
import com.palmergames.bukkit.towny.object.Town;
import com.palmergames.bukkit.towny.object.TownyPermission;
import com.palmergames.bukkit.towny.object.Translatable;
import com.palmergames.bukkit.towny.object.Translator;
import com.palmergames.bukkit.towny.object.jail.UnJailReason;
import com.palmergames.bukkit.towny.object.resident.mode.ResidentModeHandler;
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
import com.palmergames.bukkit.util.NameValidation;
import com.palmergames.util.MathUtil;
import com.palmergames.util.StringMgmt;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;
public class ResidentCommand extends BaseCommand implements CommandExecutor {

	private static Towny plugin;
	private static final List<String> residentTabCompletes = Arrays.asList(
		"friend",
		"list",
		"jail",
		"plotlist",
		"outlawlist",
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

	private static final List<String> residentConsoleTabCompletes = Arrays.asList(
		"?",
		"help",
		"list"
	);
	
	private static final List<String> residentSetTabCompletes = Arrays.asList(
		"about",
		"perm",
		"mode"
	);
	
	private static final List<String> residentAboutTabCompletes = Arrays.asList(
		"reset",
		"none",
		"clear"
	);
	
	private static final List<String> residentToggleChoices = Arrays.asList(
		"pvp",
		"fire",
		"mobs",
		"explosion"
	);

	private static final List<String> residentToggleModeTabCompletes = ResidentModeHandler.getValidModeNames();

	private static final List<String> residentSetModeTabCompletesWithClearAndReset = Stream.concat(
		Arrays.asList("reset", "clear").stream(),
		new ArrayList<>(residentToggleModeTabCompletes).stream()
	).collect(Collectors.toList());

	private static final List<String> residentCompleteToggleChoices = Stream.concat(
		new ArrayList<>(residentToggleChoices).stream(),
		new ArrayList<>(residentToggleModeTabCompletes).stream()
	).collect(Collectors.toList());

	public ResidentCommand(Towny instance) {

		plugin = instance;
	}

	@Override
	public boolean onCommand(@NotNull CommandSender sender, @NotNull Command cmd, @NotNull String label, String[] args) {
		
		if (sender instanceof Player player) {
			if (plugin.isError()) {
				TownyMessaging.sendErrorMsg(sender, "Locked in Safe mode!");
				return false;
			}

			try {
				parseResidentCommand(player, args);
			} catch (TownyException e) {
				TownyMessaging.sendErrorMsg(sender, e.getMessage());
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

		if (sender instanceof Player player) {
			switch (args[0].toLowerCase(Locale.ROOT)) {
				case "plotlist":
					if (args.length == 2)
						return getTownyStartingWith(args[1], "r");
					if (args.length == 3)
						return Collections.singletonList("[page #]");
					break;
				case "tax":
				case "outlawlist":
					if (args.length == 2)
						return getTownyStartingWith(args[1], "r");
					break;
				case "jail":
					if (args.length == 2)
						return Collections.singletonList("paybail");
					break;
				case "toggle":
					if (args.length == 2) {
						return NameUtil.filterByStart(TownyCommandAddonAPI.getTabCompletes(CommandType.RESIDENT_TOGGLE, residentCompleteToggleChoices), args[1]);
					} else if (args.length == 3 && residentToggleChoices.contains(args[1].toLowerCase(Locale.ROOT))) {
						return NameUtil.filterByStart(BaseCommand.setOnOffCompletes, args[2]);
					}
					break;
				case "set":
					if (args.length == 2)
						return NameUtil.filterByStart(TownyCommandAddonAPI.getTabCompletes(CommandType.RESIDENT_SET, residentSetTabCompletes), args[1]);
					if (args.length > 2) {
						if (TownyCommandAddonAPI.hasCommand(CommandType.RESIDENT_SET, args[1]))
							return NameUtil.filterByStart(TownyCommandAddonAPI.getAddonCommand(CommandType.RESIDENT_SET, args[1]).getTabCompletion(sender, StringMgmt.remFirstArg(args)), args[args.length-1]);

						switch (args[1].toLowerCase(Locale.ROOT)) {
							case "mode":
								if (args.length == 3)
									return NameUtil.filterByStart(residentSetModeTabCompletesWithClearAndReset, args[2]);
								else
									return NameUtil.filterByStart(residentToggleModeTabCompletes, args[args.length - 1]);
							case "perm":
								return permTabComplete(StringMgmt.remArgs(args, 2));
							case "about":
								return NameUtil.filterByStart(residentAboutTabCompletes, args[args.length - 1]);
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
							return switch (args[1].toLowerCase(Locale.ROOT)) {
								case "remove" -> {
									Resident res = TownyUniverse.getInstance().getResident(player.getUniqueId());
									if (res != null) {
										yield NameUtil.filterByStart(NameUtil.getNames(res.getFriends()), args[2]);
									}

									yield Collections.emptyList();
								}
								case "add" -> getTownyStartingWith(args[2], "r");
								case "list" -> NameUtil.filterByStart(List.of("online"), args[2]);
								default -> Collections.emptyList();
							};
						default:
							return Collections.emptyList();
					}
				default:
					if (args.length == 1)
						return filterByStartOrGetTownyStartingWith(TownyCommandAddonAPI.getTabCompletes(CommandType.RESIDENT, residentTabCompletes), args[0], "r");
					else if (TownyCommandAddonAPI.hasCommand(CommandType.RESIDENT, args[0]))
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
		} else if (TownyCommandAddonAPI.hasCommand(CommandType.RESIDENT, split[0])) {
			TownyCommandAddonAPI.getAddonCommand(CommandType.RESIDENT, split[0]).execute(sender, "resident", split);
		} else {
			final Optional<Resident> resOpt = Optional.ofNullable(TownyUniverse.getInstance().getResident(split[0]));
			if (resOpt.isPresent())
				TownyEconomyHandler.economyExecutor().execute(() -> TownyMessaging.sendStatusScreen(sender, TownyFormatter.getStatus(resOpt.get(), sender)));
			else
				throw new TownyException(Translatable.of("msg_err_not_registered_1", split[0]));
		}
		
	}

	public void parseResidentCommand(final Player player, String[] split) throws TownyException {

		if (split.length == 0) {
			Resident res = getResidentOrThrow(player);
			plugin.getScheduler().runAsync(() -> TownyMessaging.sendStatusScreen(player, TownyFormatter.getStatus(res, player)));
			return;
		}

		switch(split[0].toLowerCase(Locale.ROOT)) {
		case "?", "help" -> HelpMenu.RESIDENT_HELP.send(player); 
		case "list" -> listResidents(player); 
		case "tax" -> parseResidentTax(player, StringMgmt.remFirstArg(split));
		case "plotlist" -> parseResidentPlotlist(player, StringMgmt.remFirstArg(split));
		case "outlawlist" -> parseResidentOutlawlist(player, StringMgmt.remFirstArg(split));
		case "jail" -> parseResidentJail(player, StringMgmt.remFirstArg(split));
		case "set" -> residentSet(player, StringMgmt.remFirstArg(split));
		case "toggle" -> residentToggle(player, StringMgmt.remFirstArg(split));
		case "friend" -> residentFriend(player, StringMgmt.remFirstArg(split), false, null);
		case "spawn" -> {
			checkPermOrThrow(player, PermissionNodes.TOWNY_COMMAND_RESIDENT_SPAWN.getNode());
			SpawnUtil.sendToTownySpawn(player, split, getResidentOrThrow(player), Translatable.of("msg_err_cant_afford_tp").forLocale(player), false, false, SpawnType.RESIDENT);
		}
		default -> {
			if (tryResidentAddonCommand(player, split))
				return;
			final Resident resident = TownyUniverse.getInstance().getResidentOpt(split[0])
					.orElseThrow(() -> new TownyException(Translatable.of("msg_err_not_registered_1", split[0])));

			if (!resident.getName().equals(player.getName()))
				checkPermOrThrow(player, PermissionNodes.TOWNY_COMMAND_RESIDENT_OTHERRESIDENT.getNode());

			plugin.getScheduler().runAsync(() -> TownyMessaging.sendStatusScreen(player, TownyFormatter.getStatus(resident, player)));
		}
		}
	}

	private boolean tryResidentAddonCommand(CommandSender sender, String[] split) {
		if (TownyCommandAddonAPI.hasCommand(CommandType.RESIDENT, split[0])) {
			TownyCommandAddonAPI.getAddonCommand(CommandType.RESIDENT, split[0]).execute(sender, "resident", split);
			return true;
		}
		return false;
	}

	private void parseResidentTax(Player player, String[] split) throws TownyException {

		checkPermOrThrow(player, PermissionNodes.TOWNY_COMMAND_RESIDENT_TAX.getNode());

		if (!TownyEconomyHandler.isActive())
			throw new TownyException(Translatable.of("msg_err_no_economy"));

		Resident res = split.length > 0 ? getResidentOrThrow(split[0]) : getResidentOrThrow(player);
		TownyMessaging.sendMessage(player, TownyFormatter.getTaxStatus(res, Translator.locale(player)));
	}

	private void parseResidentPlotlist(Player player, String[] split) throws TownyException {

		checkPermOrThrow(player, PermissionNodes.TOWNY_COMMAND_RESIDENT_PLOTLIST.getNode());

		int pageLoc = 0;
		Resident res = getResidentOrThrow(player);
		if (split.length > 0) {
			if (TownyUniverse.getInstance().hasResident(split[0])) {
				res = getResidentOrThrow(split[0]);
				pageLoc = 1;
			}
		}

		if (res.getTownBlocks().isEmpty())
			throw new TownyException(Translatable.of("msg_err_resident_doesnt_own_any_land"));

		int page = 1;
		int total = (int) Math.ceil(((double) res.getTownBlocks().size()) / ((double) 10));
		if (split.length > pageLoc) {
			page = MathUtil.getPositiveIntOrThrow(split[pageLoc]);
			if (page == 0)
				throw new TownyException(Translatable.of("msg_error_must_be_int"));
			// Page will continue to be one.
		}
		if (page > total)
			throw new TownyException(Translatable.of("LIST_ERR_NOT_ENOUGH_PAGES", total));

		TownyMessaging.sendPlotList(player, res, page, total);
	}

	private void parseResidentOutlawlist(Player player, String[] split) throws TownyException {
		checkPermOrThrow(player, PermissionNodes.TOWNY_COMMAND_RESIDENT_OUTLAWLIST.getNode());

		Resident resident;
		if (split.length == 0) {
			resident = getResidentOrThrow(player);
		} else {
			resident = getResidentOrThrow(split[0]);
		}

		TownyMessaging.sendMessage(player, TownyFormatter.getFormattedTownyObjects(Translatable.of("outlawed_in").forLocale(player), new ArrayList<>(resident.getTownsOutlawedIn())));
	}

	private void parseResidentJail(Player player, String[] split) throws TownyException {
		checkPermOrThrow(player, PermissionNodes.TOWNY_COMMAND_RESIDENT_JAIL.getNode());

		if (!TownySettings.isAllowingBail())
			throw new TownyException(Translatable.of("msg_err_bail_not_enabled"));
		
		if (split.length == 0 || !split[0].equalsIgnoreCase("paybail")) {
			HelpMenu.RESIDENT_JAIL_HELP.send(player);
			return;
		}

		Resident resident = getResidentOrThrow(player);

		if (!resident.isJailed())
			throw new TownyException(Translatable.of("msg_err_you_aren't currently jailed"));

		// Fail if the economy isn't active.
		if (!TownyEconomyHandler.isActive())
			throw new TownyException(Translatable.of("msg_err_no_economy"));

		// Get Town the player is jailed in.
		final Town jailTown = resident.getJailTown();
		// Set cost of bail.
		final double cost = resident.getJailBailCost();

		if (cost <= 0) {
			// No cost, so they can pay bail.
			JailUtil.unJailResident(resident, UnJailReason.BAIL);
			return;
		}

		if (!resident.getAccount().canPayFromHoldings(cost))
			throw new TownyException(Translatable.of("msg_err_unable_to_pay_bail"));

		Confirmation.runOnAccept(() -> {
			if (resident.getAccount().canPayFromHoldings(cost)) {
				resident.getAccount().payTo(cost, jailTown, "Bail paid to " + jailTown.getName());
				JailUtil.unJailResident(resident, UnJailReason.BAIL);
			} else {
				TownyMessaging.sendErrorMsg(player, Translatable.of("msg_err_unable_to_pay_bail"));
			}
		})
		.setTitle(Translatable.of("msg_confirm_purchase", prettyMoney(cost)))
		.sendTo(player);
	}

	/**
	 * Toggle modes for this player.
	 * 
	 * @param player The player
	 * @param newSplit The array of arguments
	 * @throws TownyException when the player does not have permission or some other blocking factor.
	 */
	private void residentToggle(Player player, String[] newSplit) throws TownyException {
		Resident resident = getResidentOrThrow(player);

		if (newSplit.length == 0) {
			HelpMenu.RESIDENT_TOGGLE.send(player);

			TownyMessaging.sendMsg(resident, Translatable.of("msg_modes_set").append(StringMgmt.join(resident.getModes(), ",")));
			return;
		}

		// Check if we're reseting before trying for nodes.
		if (newSplit[0].equalsIgnoreCase("clear")) {
			checkPermOrThrow(resident.getPlayer(), PermissionNodes.TOWNY_COMMAND_RESIDENT_SET_MODE_CLEAR.getNode());
			ResidentModeHandler.clearModes(resident, false);
			return;
		}

		if (newSplit[0].equalsIgnoreCase("reset")) {
			ResidentModeHandler.resetModes(resident, false);
			return;
		}
		TownyPermission perm = resident.getPermissions();
		
		Optional<Boolean> choice = Optional.empty();
		if (newSplit.length == 2 && residentToggleChoices.contains(newSplit[0].toLowerCase(Locale.ROOT))) {
			choice = BaseCommand.parseToggleChoice(newSplit[1]);
		}

		if (newSplit[0].equalsIgnoreCase("pvp")) {
			checkPermOrThrow(player, PermissionNodes.TOWNY_COMMAND_RESIDENT_TOGGLE_PVP.getNode());
			
			Town town = resident.getTownOrNull();
			// Test to see if the pvp cooldown timer is active for the town this resident belongs to.
			if (TownySettings.getPVPCoolDownTime() > 0 && town != null && !resident.isAdmin()) {
				if (CooldownTimerTask.hasCooldown(town.getUUID().toString(), CooldownType.PVP))
					throw new TownyException(Translatable.of("msg_err_cannot_toggle_pvp_x_seconds_remaining", CooldownTimerTask.getCooldownRemaining(town.getUUID().toString(), CooldownType.PVP))); 
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
			return;
		} else {
			ResidentModeHandler.toggleMode(resident, newSplit[0].toLowerCase(Locale.ROOT), true);
			return;

		}

		notifyPerms(player, perm);
		resident.save();

	}

	/**
	 * Show the player the new Permission settings after the toggle.
	 * 
	 * @param player The player to show the permissions to
	 * @param perm The perms to show
	 */
	private void notifyPerms(Player player, TownyPermission perm) {

		TownyMessaging.sendMsg(player, Translatable.of("msg_set_perms"));
		TownyMessaging.sendMessage(player, Colors.Green + "PvP: " + ((perm.pvp) ? Colors.Red + "ON" : Colors.LightGreen + "OFF") + Colors.Green + "  Explosions: " + ((perm.explosion) ? Colors.Red + "ON" : Colors.LightGreen + "OFF") + Colors.Green + "  Firespread: " + ((perm.fire) ? Colors.Red + "ON" : Colors.LightGreen + "OFF") + Colors.Green + "  Mob Spawns: " + ((perm.mobs) ? Colors.Red + "ON" : Colors.LightGreen + "OFF"));

	}
	
	public void listResidents(CommandSender sender) throws TownyException {
		checkPermOrThrow(sender, PermissionNodes.TOWNY_COMMAND_RESIDENT_LIST.getNode());

		TownyMessaging.sendMessage(sender, ChatTools.formatTitle(Translatable.of("res_list").forLocale(sender)));
		List<String> formattedList = new ArrayList<>();
		
		for (Player player : BukkitTools.getVisibleOnlinePlayers(sender)) {
			Resident resident = TownyAPI.getInstance().getResident(player);
			if (resident == null) {
				formattedList.add(Colors.White + player.getName() + Colors.White);
				continue;
			}
			formattedList.add(getColour(resident) + resident.getName() + Colors.White);
		}
		
		TownyMessaging.sendMessage(sender, ChatTools.list(formattedList));
	}

	/**
	 * 
	 * Command: /resident set [] ... []
	 * 
	 * @param player Player.
	 * @param split Current command arguments.
	 * @throws TownyException Exception.
	 */
	public void residentSet(Player player, String[] split) throws TownyException {

		if (split.length == 0) {
			HelpMenu.RESIDENT_SET.send(player);
			return;
		}

		Resident resident = getResidentOrThrow(player);

		switch (split[0].toLowerCase(Locale.ROOT)) {
		case "perm" -> {
			checkPermOrThrow(player, PermissionNodes.TOWNY_COMMAND_RESIDENT_SET_PERM.getNode());
			TownCommand.setTownBlockPermissions(player, resident, resident.getPermissions(), StringMgmt.remFirstArg(split), true);
		}
		case "mode" -> setMode(resident, StringMgmt.remFirstArg(split));
		case "about" -> setAbout(player, String.join(" ", StringMgmt.remFirstArg(split)), resident);
		default -> {
			if (TownyCommandAddonAPI.hasCommand(CommandType.RESIDENT_SET, split[0])) {
				TownyCommandAddonAPI.getAddonCommand(CommandType.RESIDENT_SET, split[0]).execute(player, "resident", split);
				return;
			}
			throw new TownyException(Translatable.of("msg_err_invalid_property", "resident"));
		}
		}

		resident.save();
	}

	private void setMode(Resident resident, String[] split) throws TownyException {
		if (split.length == 0) {
			HelpMenu.RESIDENT_SET_MODE.send(resident.getPlayer());
			return;
		}

		if (split[0].equalsIgnoreCase("clear")) {
			checkPermOrThrow(resident.getPlayer(), PermissionNodes.TOWNY_COMMAND_RESIDENT_SET_MODE_CLEAR.getNode());
			ResidentModeHandler.clearModes(resident, true);
			return;
		}

		if (split[0].equalsIgnoreCase("reset")) {
			ResidentModeHandler.resetModes(resident, true);
			return;
		}

		ResidentModeHandler.toggleModes(resident, split, true, false);
	}

	private void setAbout(Player player, String about, Resident resident) throws TownyException {
		checkPermOrThrow(player, PermissionNodes.TOWNY_COMMAND_RESIDENT_SET_ABOUT.getNode());

		if (about.isEmpty())
			throw new TownyException("Eg: /res set about " + Translatable.of("res_8").forLocale(player));

		if ("reset".equalsIgnoreCase(about)) {
			about = TownySettings.getDefaultResidentAbout();

			TownyMessaging.sendMsg(player, Translatable.of("msg_resident_about_reset"));
		} else if ("none".equalsIgnoreCase(about) || "clear".equalsIgnoreCase(about)) {
			about = "";
		} else {
			if (!NameValidation.isValidBoardString(about)) {
				TownyMessaging.sendErrorMsg(player, Translatable.of("msg_err_invalid_string_about_not_set"));
				return;
			}
			
			if (about.length() > 159)
				about = about.substring(0, 159);
		}
		
		resident.setAbout(about);
		resident.save();

		if (about.isEmpty())
			TownyMessaging.sendMsg(player, Translatable.of("msg_clear_about", resident.getName()));
		else
			TownyMessaging.sendMsg(player, Translatable.of("msg_set_about", resident.getName(), about));
	}

	public static void residentFriend(Player player, String[] split, boolean admin, Resident resident) throws TownyException {
		checkPermOrThrow(player, PermissionNodes.TOWNY_COMMAND_RESIDENT_FRIEND.getNode());

		if (split.length == 0) {
			HelpMenu.RESIDENT_FRIEND.send(player);
			return;
		}

		if (!admin)
			resident = getResidentOrThrow(player);

		String[] args = StringMgmt.remFirstArg(split);
		switch(split[0].toLowerCase(Locale.ROOT)) {
			case "add" -> residentFriendAdd(player, resident, filterResidentList(player, args));
			case "remove" -> residentFriendRemove(player, resident, filterResidentList(player, args));
			case "list" -> residentFriendList(player, resident, args.length > 0 && args[0].equalsIgnoreCase("online"));
			case "clearlist", "clear" -> residentFriendRemove(player, resident, resident.getFriends());
		}
	}

	private static List<Resident> filterResidentList(Player player, String[] names) {
		List<Resident> residents = new ArrayList<>();
		for (String name : names) {
			Resident target = TownyUniverse.getInstance().getResident(name);
			if (target == null)
				TownyMessaging.sendErrorMsg(player, Translatable.of("msg_err_not_registered_1", name));
			else
				residents.add(target);
		}
		return residents;
	}

	private static void residentFriendList(Player player, Resident resident, boolean requireOnline) {
		
		TownyMessaging.sendMessage(player, ChatTools.formatTitle(Translatable.of("friend_list").forLocale(player)));
		List<String> formatedList = resident.getFriends().stream()
			.filter(friend -> !requireOnline || (friend.getPlayer() != null && player.canSee(friend.getPlayer())))
			.map(friend -> getColour(friend) + friend.getName() + Colors.White)
			.collect(Collectors.toList());
		TownyMessaging.sendMessage(player, ChatTools.list(formatedList));
	}

	private static String getColour(Resident resident) {
		return resident.isMayor() ? resident.isKing() ? Colors.Gold : Colors.LightBlue : Colors.White;
	}

	public static void residentFriendAdd(Player player, Resident resident, List<Resident> friending) {
		List<Resident> toFriend = friending.stream()
				.filter(friend -> !resident.hasFriend(friend) && !friend.isNPC() && !friend.getName().equalsIgnoreCase(resident.getName()))
				.collect(Collectors.toList());

		if (toFriend.isEmpty()) {
			TownyMessaging.sendErrorMsg(player, Translatable.of("msg_invalid_name"));
			return;
		}

		toFriend.forEach(friend -> {
			resident.addFriend(friend);
			plugin.deleteCache(friend);
			TownyMessaging.sendMsg(friend, Translatable.of("msg_friend_add", player.getName()));
		});

		TownyMessaging.sendMsg(player, Translatable.of("msg_res_friend_added_to_list", StringMgmt.join(toFriend, ", ")));
		resident.save();
	}

	public static void residentFriendRemove(Player player, Resident resident, List<Resident> unFriending) {
		List<Resident> toUnfriend = unFriending.stream().filter(resident::hasFriend).collect(Collectors.toList());

		if (toUnfriend.isEmpty()) {
			TownyMessaging.sendErrorMsg(player, Translatable.of("msg_invalid_name"));
			return;
		}

		toUnfriend.forEach(exFriend -> {
			resident.removeFriend(exFriend);
			plugin.deleteCache(exFriend);
			TownyMessaging.sendMsg(exFriend, Translatable.of("msg_friend_remove", player.getName()));
		});

		TownyMessaging.sendMsg(player, Translatable.of("msg_res_friend_removed_from_list", StringMgmt.join(toUnfriend, ", ")));
		resident.save();
	}
}
