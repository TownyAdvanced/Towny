package com.palmergames.bukkit.towny.command;

import com.palmergames.bukkit.towny.TownyUniverse;
import com.palmergames.bukkit.towny.exceptions.NoPermissionException;
import com.palmergames.bukkit.towny.exceptions.NotRegisteredException;
import com.palmergames.bukkit.towny.exceptions.TownyException;
import com.palmergames.bukkit.towny.object.Nation;
import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.towny.object.Town;
import com.palmergames.bukkit.towny.object.Translatable;
import com.palmergames.bukkit.towny.object.Translation;
import com.palmergames.bukkit.towny.utils.NameUtil;
import com.palmergames.bukkit.util.BukkitTools;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.permissions.Permissible;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

public class BaseCommand implements TabCompleter{
	
	private static final List<String> setPermTabCompletes = Arrays.asList(
		"on",
		"off",
		"resident",
		"friend",
		"town",
		"nation",
		"ally",
		"outsider",
		"build",
		"destroy",
		"switch",
		"itemuse",
		"reset"
	);

	private static final List<String> setLevelCompletes = Arrays.asList(
		"resident",
		"ally",
		"outsider",
		"nation",
		"friend",
		"town"		
	);

	private static final List<String> setTypeCompletes = Arrays.asList(
		"build",
		"destroy",
		"switch",
		"itemuse"
	);

	public static final List<String> setOnOffCompletes = Collections.unmodifiableList(Arrays.asList(
		"on",
		"off"
	));

	private static final List<String> toggleTypeOnOffCompletes = Arrays.asList(
		"build",
		"destroy",
		"switch",
		"itemuse",
		"on",
		"off"
	);
	
	public static final List<String> numbers = Arrays.asList(
		"1",
		"2",
		"3",
		"4",
		"5",
		"6",
		"7",
		"8",
		"9"
	);
	
	@Override
	public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
		return getTownyStartingWith(args[args.length - 1], "rtn");
	}
	
	/**
	 * Returns a List&lt;String&gt; containing strings of resident, town, and/or nation names that match with arg.
	 * Can check for multiple types, for example "rt" would check for residents and towns but not nations or worlds.
	 *
	 * @param arg the string to match with the chosen type
	 * @param type the type of Towny object to check for, can be r(esident), t(own), n(ation), w(orld), or any combination of those to check
	 * @return Matches for the arg with the chosen type
	 */
	public static List<String> getTownyStartingWith(String arg, String type) {

		List<String> matches = new ArrayList<>();
		TownyUniverse townyUniverse = TownyUniverse.getInstance();

		if (type.contains("r")) {
			matches.addAll(townyUniverse.getResidentsTrie().getStringsFromKey(arg));
		}

		if (type.contains("t")) {
			matches.addAll(townyUniverse.getTownsTrie().getStringsFromKey(arg));
		}

		if (type.contains("n")) {
			matches.addAll(townyUniverse.getNationsTrie().getStringsFromKey(arg));
		}

		if (type.contains("w")) { // There aren't many worlds so check even if arg is empty
			matches.addAll(NameUtil.filterByStart(NameUtil.getNames(townyUniverse.getTownyWorlds()), arg));
		}

		return matches;
	}

	/**
	 * Checks if arg starts with filters, if not returns matches from {@link #getTownyStartingWith(String, String)}. 
	 * Add a "+" to the type to return both cases
	 *
	 * @param filters the strings to filter arg with
	 * @param arg the string to check with filters and possibly match with Towny objects if no filters are found
	 * @param type the type of check to use, see {@link #getTownyStartingWith(String, String)} for possible types. Add "+" to check for both filters and {@link #getTownyStartingWith(String, String)}
	 * @return Matches for the arg filtered by filters or checked with type
	 */
	public static List<String> filterByStartOrGetTownyStartingWith(List<String> filters, String arg, String type) {
		List<String> filtered = NameUtil.filterByStart(filters, arg);
		if (type.isEmpty())
			return filtered;
		else if (type.contains("+")) {
			filtered.addAll(getTownyStartingWith(arg, type));
			return filtered;
		} else {
			if (filtered.size() > 0) {
				return filtered;
			} else {
				return getTownyStartingWith(arg, type);
			}
		}
	}

	/**
	 * Used for set tab completes which are common across several commands
	 * 
	 * @param args args, make sure to remove the first few irrelevant args
	 * @return tab completes matching the proper arg
	 */
	static List<String> permTabComplete(String[] args) {
		switch (args.length) {
			case 1:
				return NameUtil.filterByStart(setPermTabCompletes, args[0]);
			case 2:
				if (setTypeCompletes.contains(args[0].toLowerCase()))
					return NameUtil.filterByStart(setOnOffCompletes, args[1]);
				if (setLevelCompletes.contains(args[0].toLowerCase()))
					return NameUtil.filterByStart(toggleTypeOnOffCompletes, args[1]);
				break;
			case 3:
				return NameUtil.filterByStart(setOnOffCompletes, args[2]);
			default:
				return Collections.emptyList();
		}
		
		return Collections.emptyList();
	}
	
	/**
	 * Returns the names a player's town's residents that start with a string
	 *
	 * @param player the player to get the town's residents of
	 * @param str the string to check if the town's residents start with
	 * @return the resident names that match str
	 */
	public static List<String> getTownResidentNamesOfPlayerStartingWith(Player player, String str){
		Resident res = TownyUniverse.getInstance().getResident(player.getUniqueId());
		
		if (res != null && res.hasTown()) {
			return NameUtil.filterByStart(NameUtil.getNames(res.getTownOrNull().getResidents()), str);
		}

		return Collections.emptyList();
	}

	/**
	 * Returns the names a town's residents that start with a string
	 *
	 * @param townName the town to get the residents of
	 * @param str the string to check if the town's residents start with
	 * @return the resident names that match str
	 */
	public static List<String> getResidentsOfTownStartingWith(String townName, String str) {
		Town town = TownyUniverse.getInstance().getTown(townName);
		
		if (town != null) {
			return NameUtil.filterByStart(NameUtil.getNames(town.getResidents()), str);
		}
		
		return Collections.emptyList();
	}
	
	/**
	 * Returns a list of residents which are online and have no town.
	 * 
	 * @param str the string to check if the resident's name starts with.
	 * @return the residents name or an empty list.
	 */
	public static List<String> getResidentsWithoutTownStartingWith(String str) {
		List<Resident> residents = getOnlinePlayersWithoutTown();
		if (!residents.isEmpty())
			return NameUtil.filterByStart(NameUtil.getNames(residents), str);
		else 
			return Collections.emptyList();
	}
	
	/**
	 * Returns a list of residents which are online and have no town, and can be seen by the sender.
	 * 
	 * @param arg the string to check if the resident's name starts with.
	 * @return the residents names or an empty list.
	 */
	public List<String> getVisibleResidentsForPlayerWithoutTownsStartingWith(String arg, CommandSender sender) {
		if (!(sender instanceof Player player))
			return getResidentsWithoutTownStartingWith(arg);
		List<String> residents = getOnlinePlayersWithoutTown().stream()
			.filter(res -> player.canSee(res.getPlayer()))
			.map(res -> res.getName())
			.collect(Collectors.toCollection(ArrayList::new));
		return !residents.isEmpty()
			? NameUtil.filterByStart(residents, arg)
			: Collections.emptyList();
	}

	/**
	 * Parses the given string into a boolean choice.
	 * @param str The string to parse
	 * @return true for "ON", false for "OFF", or null if no match.
	 */
	protected static Optional<Boolean> parseToggleChoice(String str) {
		if (str.equalsIgnoreCase("on")) return Optional.of(true);
		else if (str.equalsIgnoreCase("off")) return Optional.of(false);
		else return Optional.empty();
	}

	@NotNull
	protected static Town getTownFromPlayerOrThrow(Player player) throws TownyException {
		return getTownFromResidentOrThrow(getResidentOrThrow(player.getUniqueId()));
	}

	@NotNull
	protected static Town getTownFromResidentOrThrow(@NotNull Resident resident) throws TownyException {
		if (!resident.hasTown())
			throw new TownyException(Translatable.of("msg_err_dont_belong_town"));
		return resident.getTownOrNull();
	}

	@NotNull
	protected static Resident getResidentOrThrow(UUID playerUUID) throws NotRegisteredException {
		Resident res = TownyUniverse.getInstance().getResident(playerUUID);

		if (res == null) {
			throw new NotRegisteredException(Translation.of("msg_err_not_registered"));
		}

		return res;
	}

	@NotNull
	protected static Resident getResidentOrThrow(String residentName) throws NotRegisteredException {
		Resident res = TownyUniverse.getInstance().getResident(residentName);

		if (res == null) {
			throw new NotRegisteredException(Translation.of("msg_err_not_registered_1", residentName));
		}

		return res;
	}
	
	@NotNull
	protected static Town getTownOrThrow(String townName) throws NotRegisteredException {
		Town town = TownyUniverse.getInstance().getTown(townName);

		if (town == null) {
			throw new NotRegisteredException(Translation.of("msg_err_not_registered_1", townName));
		}

		return town;
	}
	
	@NotNull
	protected static Nation getNationOrThrow(String nationName) throws NotRegisteredException {
		Nation nation = TownyUniverse.getInstance().getNation(nationName);

		if (nation == null)
			throw new NotRegisteredException(Translation.of("msg_err_not_registered_1", nationName));

		return nation;
	}

	@NotNull
	protected static Nation getNationFromPlayerOrThrow(Player player) throws TownyException {
		return getNationFromResidentOrThrow(getResidentOrThrow(player.getUniqueId()));
	}

	@NotNull
	protected static Nation getNationFromResidentOrThrow(Resident resident) throws TownyException {
		if (!resident.hasNation())
			throw new TownyException(Translatable.of("msg_err_dont_belong_nation"));
		return resident.getNationOrNull();
	}

	private static List<Resident> getOnlinePlayersWithoutTown() {
		List<Resident> townlessResidents = new ArrayList<>();
		for (Player player : BukkitTools.getOnlinePlayers()) {
			Resident resident = TownyUniverse.getInstance().getResident(player.getUniqueId());
			if (resident == null || resident.hasTown())
				continue;
			townlessResidents.add(resident);
		}
		return townlessResidents;
	}
	
	public static void catchPlayer(CommandSender sender) throws TownyException {
		if (sender instanceof Player)
			throw new TownyException(Translatable.of("msg_err_console_only"));
	}
	
	public static void catchConsole(CommandSender sender) throws TownyException {
		if (sender instanceof ConsoleCommandSender)
			throw new TownyException(Translatable.of("msg_err_player_only"));
	}
	
	public static void checkPermOrThrow(Permissible permissible, String node) throws NoPermissionException {
		TownyUniverse.getInstance().getPermissionSource().testPermissionOrThrow(permissible, node);
	}
}
