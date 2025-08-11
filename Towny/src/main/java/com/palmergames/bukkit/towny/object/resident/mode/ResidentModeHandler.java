package com.palmergames.bukkit.towny.object.resident.mode;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.palmergames.bukkit.towny.Towny;
import com.palmergames.bukkit.towny.TownyMessaging;
import com.palmergames.bukkit.towny.TownyUniverse;
import com.palmergames.bukkit.towny.event.ResidentModesInitializeEvent;
import com.palmergames.bukkit.towny.event.resident.ResidentToggleModeEvent;
import com.palmergames.bukkit.towny.exceptions.NoPermissionException;
import com.palmergames.bukkit.towny.exceptions.TownyException;
import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.towny.object.Translatable;
import com.palmergames.bukkit.towny.permissions.PermissionNodes;
import com.palmergames.bukkit.util.BukkitTools;
import com.palmergames.util.StringMgmt;

public class ResidentModeHandler {

	private static Map<String, AbstractResidentMode> modes = new ConcurrentHashMap<>();
	private static Map<Resident, Set<AbstractResidentMode>> residentModesMap = new ConcurrentHashMap<>();

	public static void initialize() {
		modes.clear();
		residentModesMap.clear();

		addMode(new GenericResidentMode("adminbypass", "")); // No permission so that admins can toggle it when they don't have admin powers.
		addMode(new GenericResidentMode("bedspawn", PermissionNodes.TOWNY_COMMAND_RESIDENT_TOGGLE_BEDSPAWN.getNode()));
		addMode(new GenericResidentMode("bordertitles", PermissionNodes.TOWNY_COMMAND_RESIDENT_TOGGLE_BORDERTITLES.getNode()));
		addMode(new GenericResidentMode("ignoreinvites", PermissionNodes.TOWNY_COMMAND_RESIDENT_TOGGLE_IGNOREPLOTS.getNode()));
		addMode(new GenericResidentMode("ignoreotherchannels", PermissionNodes.TOWNY_COMMAND_RESIDENT_TOGGLE_IGNOREOTHERCHANNELS.getNode()));
		addMode(new GenericResidentMode("ignoreplots", PermissionNodes.TOWNY_COMMAND_RESIDENT_TOGGLE_IGNOREPLOTS.getNode()));
		addMode(new GenericResidentMode("infotool", PermissionNodes.TOWNY_COMMAND_RESIDENT_TOGGLE_INFOTOOL.getNode()));
		addMode(new GenericResidentMode("map", PermissionNodes.TOWNY_COMMAND_RESIDENT_TOGGLE_MAP.getNode()));
		addMode(new GenericResidentMode("plotgroup", PermissionNodes.TOWNY_COMMAND_RESIDENT_TOGGLE_PLOTGROUP.getNode()));
		addMode(new GenericResidentMode("district", PermissionNodes.TOWNY_COMMAND_RESIDENT_TOGGLE_DISTRICT.getNode()));
		addMode(new GenericResidentMode("townborder", PermissionNodes.TOWNY_COMMAND_RESIDENT_TOGGLE_TOWNBORDER.getNode()));
		addMode(new GenericResidentMode("spy", PermissionNodes.TOWNY_CHAT_SPY.getNode()));

		addMode(new BorderResidentMode("constantplotborder", PermissionNodes.TOWNY_COMMAND_RESIDENT_TOGGLE_CONSTANTPLOTBORDER.getNode()));
		addMode(new BorderResidentMode("plotborder", PermissionNodes.TOWNY_COMMAND_RESIDENT_TOGGLE_PLOTBORDER.getNode()));

		addMode(new ClaimingResidentMode("townclaim", PermissionNodes.TOWNY_COMMAND_RESIDENT_TOGGLE_TOWNCLAIM.getNode()));
		addMode(new ClaimingResidentMode("townunclaim", PermissionNodes.TOWNY_COMMAND_RESIDENT_TOGGLE_TOWNUNCLAIM.getNode()));

		BukkitTools.fireEvent(new ResidentModesInitializeEvent());
	}

	/**
	 * Registers a new ResidentMode. Should not be used at all outside of the ResidentModesInitializeEvent.
	 * @param mode The ResidentMode to add.
	 * @throws TownyException if a mode with this name is already registered.
	 */
	public static void registerMode(@NotNull AbstractResidentMode mode) throws TownyException {
		if (modes.containsKey(mode.name))
			throw new TownyException();
		addMode(mode);
	}

	private static void addMode(AbstractResidentMode mode) {
		if (modes.containsKey(mode.name))
			return;
		modes.put(mode.name.toLowerCase(Locale.ROOT), mode);
	}

	public static boolean isValidMode(String name) {
		return modes.containsKey(name);
	}

	public static boolean isValidMode(AbstractResidentMode mode) {
		return modes.containsValue(mode);
	}

	public static List<String> getValidModeNames() {
		return new ArrayList<>(modes.keySet());
	}

	@Nullable
	public static AbstractResidentMode getMode(String name) {
		return modes.get(name);
	}

	public static Set<AbstractResidentMode> getModes(Player player) {
		try {
			return residentModesMap.getOrDefault(getResident(player), new HashSet<>());
		} catch (TownyException e) {}

		return new HashSet<>();
	}

	public static Set<AbstractResidentMode> getModes(Resident resident) {
		return residentModesMap.getOrDefault(resident, new HashSet<>());
	}

	public static boolean hasMode(Resident resident, String name) {
		return isValidMode(name) && hasMode(resident, getMode(name));
	}

	public static boolean hasMode(Resident resident, AbstractResidentMode mode) {
		return isValidMode(mode) && residentModesMap.containsKey(resident) && residentModesMap.get(resident).contains(mode);
	}

	/**
	 * Will clear a Player's modes and then apply all of the given named Modes.
	 * 
	 * @param player Player to toggle modes on.
	 * @param names  String[] of all modes to add to the Player.
	 * @param notify whether to notify the Player of their new modes.
	 */
	public static void toggleModes(Player player, String[] names, boolean notify) throws TownyException {
		toggleModes(getResident(player), names, notify, false);
	}

	/**
	 * Will clear a Resident's modes and then apply all of the given named Modes.
	 * 
	 * @param resident   Resident to toggle modes on.
	 * @param names      String[] of all modes to add to the Resident.
	 * @param notify     whether to notify the Resident of their new modes.
	 * @param clearModes whether a resident's nodes are cleared before toggling
	 *                   happens.
	 */
	public static void toggleModes(Resident resident, String[] names, boolean notify, boolean clearModes) {
		if (clearModes)
			clearModes(resident, false);
		for (String mode : names)
			try {
				toggleMode(resident, mode, false);
			} catch (TownyException e) {
				if (resident.isOnline())
					TownyMessaging.sendErrorMsg(resident.getPlayer(), e.getMessage(resident.getPlayer()));
			}
	
		if (notify && !getModes(resident).isEmpty())
			TownyMessaging.sendMsg(resident, Translatable.of("msg_modes_set").append(StringMgmt.join(getResidentModesNames(resident), ",")));
	}

	/**
	 * Will attempt to toggle on or off, a named mode on a Player.
	 * 
	 * @param player Player to toggle a mode on.
	 * @param name   the String name of the {@link AbstractResidentMode} to toggle.
	 * @param notify whether to notify the Player of their mode changing.
	 * @throws TownyException thrown when a mode doesn't exist, the Player does not
	 *                        have permisson, or when the ResidentToggleModeEvent is
	 *                        cancelled, or the Player isn't a registered Resident.
	 */
	public static void toggleMode(Player player, String name, boolean notify) throws TownyException {
		toggleMode(getResident(player), name, notify);
	}

	/**
	 * Will attempt to toggle on or off, a named mode on a Resident.
	 * 
	 * @param resident Resident to toggle a mode on.
	 * @param name     the String name of the {@link AbstractResidentMode} to
	 *                 toggle.
	 * @param notify   whether to notify the Resident of their mode changing.
	 * @throws TownyException thrown when a mode doesn't exist, the resident does
	 *                        not have permisson, or when the
	 *                        ResidentToggleModeEvent is cancelled.
	 */
	public static void toggleMode(Resident resident, String name, boolean notify) throws TownyException {
		if (!isValidMode(name))
			throw new TownyException(Translatable.of("msg_err_mode_does_not_exist", name));

		toggleMode(resident, getMode(name), notify);
	}

	/**
	 * Will attempt to toggle on or off, a AbstractResidentMode on a Resident.
	 * 
	 * @param resident Resident to toggle a mode on.
	 * @param mode     {@link AbstractResidentMode} to toggle on or off.
	 * @param notify   whether to notify the Resident of their mode changing.
	 * @throws TownyException thrown when a mode doesn't exist, the resident does
	 *                        not have permisson, or when the
	 *                        ResidentToggleModeEvent is cancelled.
	 */
	public static void toggleMode(Resident resident, AbstractResidentMode mode, boolean notify) throws TownyException {
		if (!isValidMode(mode))
			throw new TownyException(Translatable.of("msg_err_mode_does_not_exist", mode.name));

		if (!mode.permissionNode.isEmpty() && !resident.hasPermissionNode(mode.permissionNode) && isNotBecauseOfDefaultModes(resident, mode.name))
			throw new NoPermissionException();

		BukkitTools.ifCancelledThenThrow(new ResidentToggleModeEvent(resident, mode.name));

		mode.toggle(resident);

		if (notify)
			TownyMessaging.sendMsg(resident, Translatable.of("msg_modes_set").append(StringMgmt.join(getResidentModesNames(resident), ",")));
	}

	/**
	 * Default modes bypass the permission requirement, so that Admins can force
	 * players to have modes they cannot remove.
	 * 
	 * @param resident Resident to test.
	 * @param name     Mode name to test for.
	 * @return true if the player does not have the mode in their default modes.
	 */
	private static boolean isNotBecauseOfDefaultModes(Resident resident, String name) {
		List<String> defaultModes = Stream.of(getDefaultModes(resident).split(",")).collect(Collectors.toList());
		return !StringMgmt.containsIgnoreCase(defaultModes, name);
	}

	/**
	 * Removes all modes from a Player.
	 * 
	 * @param player Player to remove the modes from.
	 */
	public static void clearModes(Player player) {
		try {
			clearModes(getResident(player), false);
		} catch (TownyException ignored) {}
	}

	/**
	 * Removes all modes from a Resident.
	 * 
	 * @param resident Resident to remove the modes from.
	 * @param notify whether to notify the Resident of their modes being cleared.
	 */
	public static void clearModes(Resident resident, boolean notify) {
		if (!residentModesMap.containsKey(resident))
			return;

		residentModesMap.get(resident).clear();
		if (notify)
			TownyMessaging.sendMsg(resident, (Translatable.of("msg_modes_set")));
	}

	/**
	 * Removes all modes from a Resident and then resets their modes to the Default Modes according to their permissions.
	 * 
	 * @param resident Resident to reset.
	 * @param notify whether to notify the Resident of their modes after.
	 */
	public static void resetModes(Resident resident, boolean notify) {
		if (residentModesMap.containsKey(resident))
			residentModesMap.get(resident).clear();

		Towny.getPlugin().getScheduler().runAsyncLater(() -> applyDefaultModes(resident, notify), 1);
	}

	public static void applyDefaultModes(Resident resident, boolean notify) {
		// Is the player still available
		if (resident == null || !resident.isOnline())
			return;

		try {
			String modeString = getDefaultModes(resident);
			if (modeString.isEmpty())
				return;
			toggleModes(resident, modeString.split(","), notify, true);
		} catch (NullPointerException ignored) {}
	}
	
	protected static void addMode(Resident resident, AbstractResidentMode mode) {
		if (!residentModesMap.containsKey(resident)) {
			residentModesMap.put(resident, new HashSet<>(Arrays.asList(mode)));
			return;
		}

		Set<AbstractResidentMode> modes = residentModesMap.get(resident);
		modes.add(mode);
		residentModesMap.put(resident, modes);
	}

	protected static void removeMode(Resident resident, AbstractResidentMode mode) {
		if (!residentModesMap.containsKey(resident) || !residentModesMap.get(resident).contains(mode))
			return;

		Set<AbstractResidentMode> modes = residentModesMap.get(resident);
		modes.remove(mode);
		residentModesMap.put(resident, modes);
	}

	public static List<String> getResidentModesNames(Resident resident) {
		if (!residentModesMap.containsKey(resident))
			return new ArrayList<>();
		
		return residentModesMap.get(resident).stream().map(AbstractResidentMode::name).collect(Collectors.toUnmodifiableList());
	}

	protected static Set<AbstractResidentMode> getResidentModes(Resident resident) {
		return residentModesMap.getOrDefault(resident, new HashSet<>());
	}

	private static Resident getResident(Player player) throws TownyException {
		Resident resident = TownyUniverse.getInstance().getResident(player.getUniqueId());
		if (resident == null)
			throw new TownyException(String.format("The player with the name '%s' is not registered!", player.getName()));
		return resident;
	}

	private static String getDefaultModes(Resident resident) {
		return TownyUniverse.getInstance().getPermissionSource().getPlayerPermissionStringNode(resident.getName(), PermissionNodes.TOWNY_DEFAULT_MODES.getNode());
	}
}
