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
import com.palmergames.bukkit.towny.confirmations.ConfirmationTransaction;
import com.palmergames.bukkit.towny.event.PlotClearEvent;
import com.palmergames.bukkit.towny.event.PlotPreChangeTypeEvent;
import com.palmergames.bukkit.towny.event.PlotPreClearEvent;
import com.palmergames.bukkit.towny.event.TownBlockSettingsChangedEvent;
import com.palmergames.bukkit.towny.event.TownPreAddResidentEvent;
import com.palmergames.bukkit.towny.event.TownBlockPermissionChangeEvent;
import com.palmergames.bukkit.towny.event.plot.PlayerChangePlotTypeEvent;
import com.palmergames.bukkit.towny.event.plot.PlotNotForSaleEvent;
import com.palmergames.bukkit.towny.event.plot.PlotSetForSaleEvent;
import com.palmergames.bukkit.towny.event.plot.PlotTrustAddEvent;
import com.palmergames.bukkit.towny.event.plot.PlotTrustRemoveEvent;
import com.palmergames.bukkit.towny.event.plot.district.DistrictAddEvent;
import com.palmergames.bukkit.towny.event.plot.district.DistrictCreatedEvent;
import com.palmergames.bukkit.towny.event.plot.district.DistrictDeletedEvent;
import com.palmergames.bukkit.towny.event.plot.group.PlotGroupAddEvent;
import com.palmergames.bukkit.towny.event.plot.group.PlotGroupCreatedEvent;
import com.palmergames.bukkit.towny.event.plot.group.PlotGroupDeletedEvent;
import com.palmergames.bukkit.towny.event.plot.toggle.PlotToggleExplosionEvent;
import com.palmergames.bukkit.towny.event.plot.toggle.PlotToggleFireEvent;
import com.palmergames.bukkit.towny.event.plot.toggle.PlotToggleMobsEvent;
import com.palmergames.bukkit.towny.event.plot.toggle.PlotTogglePvpEvent;
import com.palmergames.bukkit.towny.event.plot.toggle.PlotToggleTaxedEvent;
import com.palmergames.bukkit.towny.exceptions.AlreadyRegisteredException;
import com.palmergames.bukkit.towny.exceptions.CancelledEventException;
import com.palmergames.bukkit.towny.exceptions.NoPermissionException;
import com.palmergames.bukkit.towny.exceptions.TownyException;
import com.palmergames.bukkit.towny.huds.HUDManager;
import com.palmergames.bukkit.towny.object.Coord;
import com.palmergames.bukkit.towny.object.District;
import com.palmergames.bukkit.towny.object.PermissionData;
import com.palmergames.bukkit.towny.object.PlotGroup;
import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.towny.object.SpawnPointLocation;
import com.palmergames.bukkit.towny.object.Town;
import com.palmergames.bukkit.towny.object.TownBlock;
import com.palmergames.bukkit.towny.object.TownBlockOwner;
import com.palmergames.bukkit.towny.object.TownBlockType;
import com.palmergames.bukkit.towny.object.TownBlockTypeHandler;
import com.palmergames.bukkit.towny.object.TownyPermission;
import com.palmergames.bukkit.towny.object.TownyPermissionChange;
import com.palmergames.bukkit.towny.object.TownyWorld;
import com.palmergames.bukkit.towny.object.Translatable;
import com.palmergames.bukkit.towny.object.Translator;
import com.palmergames.bukkit.towny.object.WorldCoord;
import com.palmergames.bukkit.towny.object.jail.Jail;
import com.palmergames.bukkit.towny.permissions.PermissionNodes;
import com.palmergames.bukkit.towny.regen.WorldCoordMaterialRemover;
import com.palmergames.bukkit.towny.tasks.CooldownTimerTask;
import com.palmergames.bukkit.towny.tasks.PlotClaim;
import com.palmergames.bukkit.towny.tasks.CooldownTimerTask.CooldownType;
import com.palmergames.bukkit.towny.utils.AreaSelectionUtil;
import com.palmergames.bukkit.towny.utils.MoneyUtil;
import com.palmergames.bukkit.towny.utils.NameUtil;
import com.palmergames.bukkit.towny.utils.OutpostUtil;
import com.palmergames.bukkit.towny.utils.PermissionGUIUtil;
import com.palmergames.bukkit.towny.utils.ProximityUtil;
import com.palmergames.bukkit.util.BukkitTools;
import com.palmergames.bukkit.util.ChatTools;
import com.palmergames.bukkit.util.Colors;
import com.palmergames.bukkit.util.NameValidation;
import com.palmergames.util.MathUtil;
import com.palmergames.util.StringMgmt;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Send a list of all general towny plot help commands to player Command: /plot
 */

public class PlotCommand extends BaseCommand implements CommandExecutor {

	private static Towny plugin;
	
	private static final List<String> plotTabCompletes = Arrays.asList(
		"claim",
		"unclaim",
		"forsale",
		"fs",
		"notforsale",
		"nfs",
		"evict",
		"info",
		"perm",
		"set",
		"toggle",
		"clear",
		"group",
		"district",
		"jailcell",
		"trust"
	);
	
	private static final List<String> plotGroupTabCompletes = Arrays.asList(
		"add",
		"delete",
		"remove",
		"set",
		"toggle",
		"fs",
		"notforsale",
		"forsale",
		"perm",
		"rename",
		"trust"
	);
	
	private static final List<String> districtTabCompletes = Arrays.asList(
		"add",
		"new",
		"create",
		"delete",
		"remove",
		"rename"
	);
	
	private static final List<String> plotSetTabCompletes = Arrays.asList(
		"reset",
		"shop",
		"embassy",
		"arena",
		"wilds",
		"inn",
		"jail",
		"farm",
		"bank",
		"minjoindays",
		"maxjoindays",
		"outpost",
		"name",
		"perm"
	);
	
	private static final List<String> plotRectCircleCompletes = Arrays.asList(
		"rect",
		"circle"
	);
	
	private static final List<String> plotToggleTabCompletes = Arrays.asList(
		"taxed",
		"fire",
		"pvp",
		"explosion",
		"mobs"
	);
	
	private static final List<String> plotPermTabCompletes = Arrays.asList(
		"hud",
		"gui",
		"add",
		"remove"
	);

	public PlotCommand(Towny instance) {

		plugin = instance;
	}

	@Override
	public boolean onCommand(CommandSender sender, Command cmd, String commandLabel, String[] args) {
		
		if (sender instanceof Player player) {
			if (plugin.isError()) {
				TownyMessaging.sendErrorMsg(sender, "Locked in Safe mode!");
				return false;
			}

			if (!TownyAPI.getInstance().isTownyWorld(player.getWorld())) {
				TownyMessaging.sendErrorMsg(player, Translatable.of("msg_set_use_towny_off"));
				return false;
			}

			if (args == null) {
				HelpMenu.PLOT_HELP.send(player);
				return false;
			}

			try {
				parsePlotCommand(player, args);
			} catch (TownyException e) {
				TownyMessaging.sendErrorMsg(player, e.getMessage(player));
			}
		} else
			// Console
			HelpMenu.PLOT_HELP.send(sender);
		
		return true;
	}
	
	@Override
	public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
		if (sender instanceof Player player) {
			switch (args[0].toLowerCase(Locale.ROOT)) {
				case "set":
					if (args.length == 2) {
						return NameUtil.filterByStart(TownyCommandAddonAPI.getTabCompletes(CommandType.PLOT_SET, getPlotSetCompletions()), args[1]);
					}
					if (args.length == 3 && args[1].equalsIgnoreCase("outpost")) {
						return Arrays.asList("spawn");
					}
					if (args.length == 3 && (args[1].equalsIgnoreCase("minjoindays") || args[1].equalsIgnoreCase("maxjoindays"))) {
						List<String> numbersAndClear = new ArrayList<>(numbers);
						numbersAndClear.add("clear");
						return NameUtil.filterByStart(numbersAndClear, args[2]);
					}
					if (args.length > 2 && args[1].equalsIgnoreCase("perm")) {
						return permTabComplete(StringMgmt.remArgs(args, 2));
					} else if (args.length > 2 && TownyCommandAddonAPI.hasCommand(CommandType.PLOT_SET, args[1]))
						return NameUtil.filterByStart(TownyCommandAddonAPI.getAddonCommand(CommandType.PLOT_SET, args[1]).getTabCompletion(sender, StringMgmt.remFirstArg(args)), args[args.length-1]);
					break;
				case "toggle":
					if (args.length == 2)
						return NameUtil.filterByStart(TownyCommandAddonAPI.getTabCompletes(CommandType.PLOT_TOGGLE, plotToggleTabCompletes), args[1]);
					else if (args.length == 3)
						return NameUtil.filterByStart(BaseCommand.setOnOffCompletes, args[2]);
					break;
				case "claim":
				case "unclaim":
				case "notforsale":
				case "nfs":
					if (args.length == 2)
						return NameUtil.filterByStart(plotRectCircleCompletes, args[1]);
					break;
				case "evict":
					if (args.length == 2)
						return NameUtil.filterByStart(Collections.singletonList("forsale"), args[1]);
					break;
				case "forsale":
				case "fs":
					switch (args.length) {
						case 2:
							return NameUtil.filterByStart(Collections.singletonList("within"), args[1]);
						case 3:
							return NameUtil.filterByStart(plotRectCircleCompletes, args[2]);
						default:
							return Collections.emptyList();
					}
				case "district":
					if (args.length == 2)
						return NameUtil.filterByStart(districtTabCompletes, args[1]);
					if (args.length < 2)
						break;
				case "group":
					if (args.length == 2)
						return NameUtil.filterByStart(plotGroupTabCompletes, args[1]);
					
					if (args.length < 2)
						break;
					
					switch (args[1].toLowerCase(Locale.ROOT)) {
						case "set":
							if (args.length == 3)
								return NameUtil.filterByStart(Arrays.asList("perm", "maxjoindays", "minjoindays"), args[2]);
							if (args.length > 3 && args[2].equalsIgnoreCase("perm"))
								return permTabComplete(StringMgmt.remArgs(args, 3));
						case "trust":
							if (args.length == 3)
								return NameUtil.filterByStart(Arrays.asList("add", "remove"), args[2]);
							if (args.length == 4)
								return NameUtil.filterByStart(getTownyStartingWith(args[3], "r"), args[3]);
						case "perm":
							if (args.length == 3)
								return NameUtil.filterByStart(Arrays.asList("add", "remove", "gui"), args[2]);
							if (args.length == 4)
								return NameUtil.filterByStart(getTownyStartingWith(args[3], "r"), args[3]);
						case "toggle":
							if (args.length == 3)
								return NameUtil.filterByStart(plotToggleTabCompletes, args[2]);
						default:
							return Collections.emptyList();
					}

				case "jailcell":
					if (args.length == 2)
						return NameUtil.filterByStart(TownCommand.townAddRemoveTabCompletes, args [1]);
					break;
				case "perm":
					if (args.length == 2)
						return NameUtil.filterByStart(plotPermTabCompletes, args[1]);
					if (args.length == 3)
						if (args[1].equalsIgnoreCase("remove") || args[1].equalsIgnoreCase("add"))
							return NameUtil.filterByStart(getTownyStartingWith(args[2], "r"), args[2]);
					break;
				case "trust":
					if (args.length == 2)
						return NameUtil.filterByStart(Arrays.asList("add", "remove"), args[1]);
					if (args.length == 3) {
						if ("remove".equalsIgnoreCase(args[1])) {
							final TownBlock townBlock = WorldCoord.parseWorldCoord(player).getTownBlockOrNull();
							if (townBlock != null) {
								return townBlock.getTrustedResidents().stream().map(Resident::getName).toList();
							}
						} else {
							return getTownyStartingWith(args[2], "r");
						}
					}
				default:
					if (args.length == 1)
						return NameUtil.filterByStart(TownyCommandAddonAPI.getTabCompletes(CommandType.PLOT, plotTabCompletes), args[0]);
					else if (args.length > 1 && TownyCommandAddonAPI.hasCommand(CommandType.PLOT, args[0]))
						return NameUtil.filterByStart(TownyCommandAddonAPI.getAddonCommand(CommandType.PLOT, args[0]).getTabCompletion(sender, args), args[args.length-1]);
			}
		}

		return Collections.emptyList();
	}

	public void parsePlotCommand(Player player, String[] split) throws TownyException {

		if (split.length == 0 || split[0].equalsIgnoreCase("?")) {
			HelpMenu.PLOT_HELP.send(player);
			return;
		}

		Resident resident = getResidentOrThrow(player);
		TownBlock townBlock = TownyAPI.getInstance().getTownBlock(player);

		// Stops all commands except perm, claim and info being run in the wilderness.
		if (townBlock == null && !plotCommandAllowedInWilderness(split[0]))
			throw new TownyException(Translatable.of("msg_not_claimed_1"));

		if (townBlock != null && townBlock.getTownOrNull().isRuined())
			throw new TownyException(Translatable.of("msg_err_cannot_use_command_because_town_ruined"));

		switch(split[0].toLowerCase(Locale.ROOT)) {
		case "claim" -> parsePlotClaim(player, StringMgmt.remFirstArg(split), resident, townBlock);
		case "clear" -> parsePlotClear(resident, townBlock);
		case "evict" -> parsePlotEvict(resident, townBlock, StringMgmt.remFirstArg(split));
		case "fs", "forsale" -> parsePlotForSale(player, StringMgmt.remFirstArg(split), resident, townBlock);
		case "group" -> parsePlotGroup(StringMgmt.remFirstArg(split), resident, townBlock, player);
		case "district" -> parseDistrict(StringMgmt.remFirstArg(split), resident, townBlock, player);
		case "info" -> sendPlotInfo(player, StringMgmt.remFirstArg(split));
		case "jailcell" -> parsePlotJailCell(player, resident, townBlock, StringMgmt.remFirstArg(split));
		case "nfs", "notforsale" -> parsePlotNotForSale(player, StringMgmt.remFirstArg(split), resident, townBlock);
		case "perm" -> parsePlotPermCommand(player, StringMgmt.remFirstArg(split));
		case "set" -> parsePlotSet(player, StringMgmt.remFirstArg(split), resident, townBlock);
		case "toggle" -> plotToggle(player, resident, townBlock, StringMgmt.remFirstArg(split));
		case "trust" -> parsePlotTrustCommand(player, StringMgmt.remFirstArg(split));
		case "unclaim" -> parsePlotUnclaim(player, split, resident);
		default -> {
			if (TownyCommandAddonAPI.hasCommand(CommandType.PLOT, split[0]))
				TownyCommandAddonAPI.getAddonCommand(CommandType.PLOT, split[0]).execute(player, "plot", split);
			else
				throw new TownyException(Translatable.of("msg_err_invalid_property", split[0]));
			break;
		}
		}
	}

	public void parsePlotClaim(Player player, String[] args, Resident resident, TownBlock townBlock) throws TownyException {
		checkPermOrThrow(player, PermissionNodes.TOWNY_COMMAND_PLOT_CLAIM.getNode());
		List<WorldCoord> selection = AreaSelectionUtil.selectWorldCoordArea(resident, WorldCoord.parseWorldCoord(player), args, true);

		// Fast-fail if this is a single plot and it is already claimed, or the resident already owns this plot.
		if (selection.size() == 1 && selection.get(0).hasTownBlock() && selection.get(0).getTownBlock().hasResident()) { 
			if (!selection.get(0).getTownBlock().isForSale()) {
				final Translatable message = Translatable.of("msg_already_claimed", selection.get(0).getTownBlock().getResidentOrNull());
				// Add extra message if the player has permission to evict 
				if (player.hasPermission(PermissionNodes.TOWNY_COMMAND_PLOT_EVICT.getNode())) {
					try {
						TownyAPI.getInstance().testPlotOwnerOrThrow(resident, townBlock);
						message.append(Translatable.of("msg_plot_claim_consider_evict_instead"));
					} catch (TownyException ignored) {}
				}
				throw new TownyException(message);
			} else if (selection.get(0).getTownBlock().hasResident(resident)) {
				throw new TownyException(Translatable.of("msg_err_you_already_own_this_plot"));
			}
		}

		// Filter to just plots that are for sale, which can actually be bought by the
		// resident, (ie: outsiders can only buy Embassy plots, some residents cannot 
		// purchase a plot because they have been a resident of town for too little or 
		// too long.)
		selection = AreaSelectionUtil.filterPlotsForSale(resident, selection);

		// Filter out plots already owned by the player.
		selection = AreaSelectionUtil.filterUnownedBlocks(resident, selection);

		// Nothing was left to claim.
		if (selection.size() == 0)
			throw new TownyException(Translatable.of("msg_err_empty_area_selection"));

		if (selection.size() + resident.getTownBlocks().size()  > TownySettings.getMaxResidentPlots(resident))
			throw new TownyException(Translatable.of("msg_max_plot_own", TownySettings.getMaxResidentPlots(resident)));

		final Town town = townBlock != null ? townBlock.getTownOrNull() : selection.get(0).getTownOrNull();
		/*
		 * If a resident has no town, the Town is open, and the plot is not an Embassy,
		 * the Town is not going to exceed the maxResidentsWithoutANation value, and the
		 * Town will not exceed the maxResidentsPerTown value, and the resident is not
		 * an outlaw, and the resident has the permission node for /t join, THEN try to
		 * add the resident to the town if they confirm their join.
		 */
		if (playerIsAbleToJoinViaPlotClaim(resident, townBlock, town)) {
			final List<WorldCoord> coords = selection;
			Confirmation.runOnAccept(() -> {
				try {
					resident.setTown(town);
				} catch (AlreadyRegisteredException ignored) {}
				try {
					continuePlotClaimProcess(coords, resident, player);
				} catch (TownyException e) {
					TownyMessaging.sendErrorMsg(player, e.getMessage(player));
				}
			})
			.setTitle(Translatable.of("msg_you_must_join_this_town_to_claim_this_plot", town.getName()))
			.setCancellableEvent(new TownPreAddResidentEvent(town, resident))
			.sendTo(player);
			return;
		}
		continuePlotClaimProcess(selection, resident, player);
	}

	private boolean playerIsAbleToJoinViaPlotClaim(Resident resident, TownBlock townBlock, Town town) {
		return !resident.hasTown() &&
			town.isOpen() &&
			!townBlock.getType().equals(TownBlockType.EMBASSY) &&
			!town.isAllowedThisAmountOfResidents(town.getNumResidents() + 1, town.isCapital()) &&
			!town.hasOutlaw(resident) &&
			resident.hasPermissionNode(PermissionNodes.TOWNY_COMMAND_TOWN_JOIN.getNode());
	}

	public void parsePlotClear(Resident resident, TownBlock townBlock) throws TownyException {
		checkPermOrThrow(resident.getPlayer(), PermissionNodes.TOWNY_COMMAND_PLOT_CLEAR.getNode());

		if (townBlock == null) // Shouldn't ever happen but let's check anyways.
			throw new TownyException(Translatable.of("msg_err_empty_area_selection"));

		if (!townBlock.getWorld().isUsingPlotManagementMayorDelete())
			throw new TownyException(Translatable.of("msg_err_plot_clear_disabled_in_this_world"));

		/*
		 * Only allow mayors or plot owners to use this command.
		 * This will throw an exception if the player isn't a mayor or owner of the plot. 
		 */
		TownyAPI.getInstance().testPlotOwnerOrThrow(resident, townBlock);

		BukkitTools.ifCancelledThenThrow(new PlotPreClearEvent(townBlock));

		Collection<Material> materialsToDelete = townBlock.getWorld().getPlotManagementMayorDelete();
		if (materialsToDelete.isEmpty())
			return;

		WorldCoordMaterialRemover.queueDeleteWorldCoordMaterials(townBlock.getWorldCoord(), materialsToDelete);
		TownyMessaging.sendMsg(resident, Translatable.of("msg_clear_plot_material", StringMgmt.join(materialsToDelete, ", ")));

		// Raise an event.
		BukkitTools.fireEvent(new PlotClearEvent(townBlock));
	}

	public void parsePlotEvict(Resident resident, TownBlock townBlock, String[] split) throws TownyException {
		checkPermOrThrow(resident.getPlayer(), PermissionNodes.TOWNY_COMMAND_PLOT_EVICT.getNode());

		if (!townBlock.hasResident())
			throw new TownyException(Translatable.of("msg_no_one_to_evict"));
		// Test we are allowed to work on this plot
		// If this fails it will trigger a TownyException.
		TownyAPI.getInstance().testPlotOwnerOrThrow(resident, townBlock);

		boolean resell = split.length > 0 && split[0].equalsIgnoreCase("forsale");

		if (townBlock.hasPlotObjectGroup()) {
			townBlock.getPlotObjectGroup().getTownBlocks().stream().forEach(TownBlock::evictOwnerFromTownBlock);
			TownyMessaging.sendMsg(resident, Translatable.of("msg_plot_evict_group", townBlock.getPlotObjectGroup().getName()));
			return;
		}

		// Evict and save the townblock.
		townBlock.evictOwnerFromTownBlock(resell);
		TownyMessaging.sendMsg(resident, Translatable.of("msg_plot_evict"));
	}

	public void parsePlotForSale(Player player, String[] split, Resident resident, TownBlock townBlock) throws TownyException {
		checkPermOrThrow(player, PermissionNodes.TOWNY_COMMAND_PLOT_FORSALE.getNode());

		WorldCoord pos = WorldCoord.parseWorldCoord(player);
		Town town = townBlock.getTownOrNull();
		if (town == null) //Shouldn't be able to happen but check anyways.
			throw new TownyException(Translatable.of("msg_err_empty_area_selection"));

		double plotPrice = Math.max(town.getPlotTypePrice(townBlock.getType()), 0);

		if (split.length == 0) {
			/*
			 * basic 'plot fs' command
			 */

			// Skip over any plots that are part of a PlotGroup.
			if (pos.getTownBlock().hasPlotObjectGroup())
				throw new TownyException(Translatable.of("msg_err_plot_belongs_to_group_plot_fs2", pos));

			// Otherwise continue on normally.
			setPlotForSale(resident, pos, plotPrice);
			return;
		}

		/*
		 * This is not a case of '/plot fs' and has a cost and/or an area selection involved.
		 */

		// areaSelectPivot is how Towny handles the 'within' area selection when setting plots for sale.
		// Will return -1 if the word 'within' is not found.
		int areaSelectPivot = AreaSelectionUtil.getAreaSelectPivot(split);
		List<WorldCoord> selection;
		
		if (areaSelectPivot >= 0) { // 'within' has been used in the command, make a selection.
			selection = AreaSelectionUtil.selectWorldCoordArea(resident, pos, StringMgmt.subArray(split, areaSelectPivot + 1, split.length));

			// The follow test will clean up the initial selection fairly well, the plotTestOwner later on in the setPlotForSale will ultimately stop any funny business.
			if (
				(resident.hasPermissionNode(PermissionNodes.TOWNY_COMMAND_PLOT_ASMAYOR.getNode()) && town.hasResident(resident))
				|| (resident.hasPermissionNode(PermissionNodes.TOWNY_COMMAND_PLOT_ASMAYORINUNOWNED.getNode()) && town.hasResident(resident) && !townBlock.hasResident())
				) {
				selection = AreaSelectionUtil.filterOwnedBlocks(resident.getTown(), selection); // Treat it as a mayor able to put their town's plots for sale.
				selection = AreaSelectionUtil.filterOutResidentBlocks(resident, selection); // Filter out any resident-owned plots.
			} else {
				selection = AreaSelectionUtil.filterOwnedBlocks(resident, selection); // Treat it as a resident putting their own plots up for sale.
			}

			if (selection.isEmpty()) 
				throw new TownyException(Translatable.of("msg_err_empty_area_selection"));

		} else { // No 'within' found so this will be a case of /plot fs $, add a single coord to selection.
			selection = new ArrayList<>();
			selection.add(pos);
		}

		// Check that it's not: /plot forsale within rect 3
		if (areaSelectPivot != 0) {
			// command was 'plot fs $'
			plotPrice = MoneyUtil.getMoneyAboveZeroOrThrow(split[0]);
		}

		// Set each WorldCoord in selection for sale.
		for (WorldCoord worldCoord : selection) {
			TownBlock tb = worldCoord.getTownBlock();
			
			// Skip over any plots that are part of a PlotGroup.
			if (tb.hasPlotObjectGroup()) {
				TownyMessaging.sendErrorMsg(player, Translatable.of("msg_err_plot_belongs_to_group_plot_fs2", worldCoord));
				continue;
			}
			
			// Otherwise continue on normally.
			setPlotForSale(resident, worldCoord, plotPrice);
		}
	}

	public void parsePlotSet(Player player, String[] split, Resident resident, TownBlock townBlock) throws TownyException {

		if (split.length == 0 || split[0].equalsIgnoreCase("?")) {
			HelpMenu.PLOT_SET.send(player);
			return;
		}

		// Make sure that the player is only operating on a plot object group if one exists.
		// Commands such as /plot set name or commands added via the api are excluded from this.
		if (townBlock.hasPlotObjectGroup() && !"name".equalsIgnoreCase(split[0]) && !TownyCommandAddonAPI.hasCommand(CommandType.PLOT_SET, split[0]))
			throw new TownyException(Translatable.of("msg_err_plot_belongs_to_group_set"));

		switch(split[0].toLowerCase(Locale.ROOT)) {
		case "minjoindays":
			parsePlotSetMinJoinDays(player, resident, townBlock, StringMgmt.remFirstArg(split));
			break;
		case "maxjoindays":
			parsePlotSetMaxJoinDays(player, resident, townBlock, StringMgmt.remFirstArg(split));
			break;
		case "perm":
			checkPermOrThrow(player, PermissionNodes.TOWNY_COMMAND_PLOT_SET_PERM.getNode());
			TownyAPI.getInstance().testPlotOwnerOrThrow(resident, townBlock); // Test we are allowed to work on this plot
			setTownBlockPermissions(player, townBlock.getTownBlockOwner(), townBlock, StringMgmt.remFirstArg(split));
			break;
		case "name":
			checkPermOrThrow(player, PermissionNodes.TOWNY_COMMAND_PLOT_SET_NAME.getNode());
			TownyAPI.getInstance().testPlotOwnerOrThrow(resident, townBlock); // Test we are allowed to work on this plot
			parsePlotSetName(player, StringMgmt.remFirstArg(split), townBlock);
			break;
		case "outpost":
			checkPermOrThrow(player, PermissionNodes.TOWNY_COMMAND_TOWN_CLAIM_OUTPOST.getNode());
			TownyAPI.getInstance().testPlotOwnerOrThrow(resident, townBlock); // Test we are allowed to work on this plot
			boolean spawn = split.length == 2 && split[1].equalsIgnoreCase("spawn");
			parsePlotSetOutpost(player, resident, townBlock, spawn);
			break;
		default:
			if (tryPlotSetAddonCommand(player, split))
				return;

			/*
			 * After trying all of the other /plot set subcommands, attempt to set the townblock type.
			 */
			tryPlotSetType(player, resident, townBlock, split);
		}
	}

	private void parsePlotSetMinJoinDays(Player player, Resident resident, TownBlock townBlock, String[] args) throws TownyException {
		checkPermOrThrow(player, PermissionNodes.TOWNY_COMMAND_PLOT_ASMAYOR.getNode());
		TownyAPI.getInstance().testPlotOwnerOrThrow(resident, townBlock); // Test we are allowed to work on this plot

		if (args.length == 0 || args[0].equalsIgnoreCase("?")) {
			HelpMenu.PLOT_SET.send(player);
			return;
		}
		if (args[0].equalsIgnoreCase("clear")) {
			townBlock.setMinTownMembershipDays(-1);
			townBlock.save();
			TownyMessaging.sendMsg(player, Translatable.of("msg_townblock_min_join_days_removed"));
			return;
		}

		int days = MathUtil.getPositiveIntOrThrow(args[0]);
		if (days == 0)
			throw new TownyException(Translatable.of("msg_err_days_must_be_greater_than_0"));
		townBlock.setMinTownMembershipDays(days);
		townBlock.save();
		TownyMessaging.sendMsg(player, Translatable.of("msg_townblock_min_join_days_set_to", townBlock.getMinTownMembershipDays()));
	}

	private void parsePlotSetMaxJoinDays(Player player, Resident resident, TownBlock townBlock, String[] args) throws TownyException {
		checkPermOrThrow(player, PermissionNodes.TOWNY_COMMAND_PLOT_ASMAYOR.getNode());
		TownyAPI.getInstance().testPlotOwnerOrThrow(resident, townBlock); // Test we are allowed to work on this plot

		if (args.length == 0 || args[0].equalsIgnoreCase("?")) {
			HelpMenu.PLOT_SET.send(player);
			return;
		}
		if (args[0].equalsIgnoreCase("clear")) {
			townBlock.setMaxTownMembershipDays(-1);
			townBlock.save();
			TownyMessaging.sendMsg(player, Translatable.of("msg_townblock_max_join_days_removed"));
			return;
		}

		int days = MathUtil.getPositiveIntOrThrow(args[0]);
		if (days == 0)
			throw new TownyException(Translatable.of("msg_err_days_must_be_greater_than_0"));
		townBlock.setMaxTownMembershipDays(days);
		townBlock.save();
		TownyMessaging.sendMsg(player, Translatable.of("msg_townblock_max_join_days_set_to", townBlock.getMaxTownMembershipDays()));
	}

	private void tryPlotSetType(Player player, Resident resident, TownBlock townBlock, String[] split) throws TownyException {
		checkPermOrThrow(player, PermissionNodes.TOWNY_COMMAND_PLOT_SET.getNode(split[0].toLowerCase(Locale.ROOT)));
		String plotTypeName = split[0];

		// Handle type being reset
		if (plotTypeName.equalsIgnoreCase("reset"))
			plotTypeName = "default";

		TownBlockType townBlockType = TownBlockTypeHandler.getType(plotTypeName);
		TownBlockType oldType = townBlock.getType();

		if (townBlockType == null)
			throw new TownyException(Translatable.of("msg_err_not_block_type"));

		if (townBlockType == oldType)
			throw new TownyException(Translatable.of("msg_plot_already_of_type", townBlockType.getName()));

		// Test we are allowed to work on this plot
		TownyAPI.getInstance().testPlotOwnerOrThrow(resident, townBlock);

		if (TownBlockType.ARENA.equals(townBlockType) && TownySettings.getOutsidersPreventPVPToggle()) {
			for (Player target : Bukkit.getOnlinePlayers()) {
				if (!townBlock.getTownOrNull().hasResident(target) && !player.getName().equals(target.getName()) && townBlock.getWorldCoord().equals(WorldCoord.parseWorldCoord(target)))
					throw new TownyException(Translatable.of("msg_cant_toggle_pvp_outsider_in_plot"));
			}
		}

		BukkitTools.ifCancelledThenThrow(new PlotPreChangeTypeEvent(townBlockType, townBlock, resident));

		double cost = townBlockType.getData().getCost();

		// Test if we can pay first to throw an exception.
		if (cost > 0 && TownyEconomyHandler.isActive() && !resident.getAccount().canPayFromHoldings(cost))
			throw new TownyException(Translatable.of("msg_err_cannot_afford_plot_set_type_cost", townBlockType, prettyMoney(cost)));

		// Handle payment via a confirmation to avoid suprise costs.
		if (cost > 0 && TownyEconomyHandler.isActive()) {
			Confirmation.runOnAccept(() -> {
				TownyMessaging.sendMsg(resident, Translatable.of("msg_plot_set_cost", prettyMoney(cost), townBlockType));

				try {
					townBlock.setType(townBlockType, resident);
				} catch (TownyException e) {
					TownyMessaging.sendErrorMsg(resident, e.getMessage(player));
					return;
				}
				BukkitTools.fireEvent(new PlayerChangePlotTypeEvent(townBlockType, oldType, townBlock, player));
				TownyMessaging.sendMsg(player, Translatable.of("msg_plot_set_type", townBlockType));
			})
				.setCost(new ConfirmationTransaction(() -> cost, resident, String.format("Plot set to %s", townBlockType),
						Translatable.of("msg_err_cannot_afford_plot_set_type_cost", townBlockType, prettyMoney(cost))))
				.setTitle(Translatable.of("msg_confirm_purchase", prettyMoney(cost)))
				.sendTo(BukkitTools.getPlayerExact(resident.getName()));

		// No cost or economy so no confirmation.
		} else {
			townBlock.setType(townBlockType, resident);
			BukkitTools.fireEvent(new PlayerChangePlotTypeEvent(townBlockType, oldType, townBlock, player));
			TownyMessaging.sendMsg(player, Translatable.of("msg_plot_set_type", plotTypeName));
		}
	}

	private boolean tryPlotSetAddonCommand(Player player, String[] split) {
		if (TownyCommandAddonAPI.hasCommand(CommandType.PLOT_SET, split[0])) {
			TownyCommandAddonAPI.getAddonCommand(CommandType.PLOT_SET, split[0]).execute(player, "plot", split);
			return true;
		}
		return false;
	}

	public void parsePlotSetOutpost(Player player, Resident resident, TownBlock townBlock, boolean spawn) throws TownyException {
		if (!TownySettings.isAllowingOutposts()) 
			throw new TownyException(Translatable.of("msg_outpost_disable"));

		Town town = townBlock.getTownOrNull();

		if (spawn) {
			checkPermOrThrow(player, PermissionNodes.TOWNY_COMMAND_TOWN_SET_OUTPOST.getNode());

			if (!townBlock.isOutpost())
				throw new TownyException(Translatable.of("msg_err_location_is_not_within_an_outpost_plot"));

			town.addOutpostSpawn(player.getLocation());
			TownyMessaging.sendMsg(player, Translatable.of("msg_set_outpost_spawn"));
			return;
		}

		TownyWorld townyWorld = townBlock.getWorld();
		Coord key = Coord.parseCoord(player.getLocation());

		// Throws a TownyException with message if outpost should not be set.
		OutpostUtil.OutpostTests(town, resident, townyWorld, key, resident.isAdmin(), true);

		if (TownySettings.getOutpostCost() > 0) {
			// Create a confirmation for setting outpost.
			Confirmation.runOnAccept(() -> {
				// Set the outpost spawn and display feedback.
				town.addOutpostSpawn(player.getLocation());
				TownyMessaging.sendMsg(player, Translatable.of("msg_plot_set_cost", prettyMoney(TownySettings.getOutpostCost()), Translatable.of("outpost")));
			})
			.setCost(new ConfirmationTransaction(() -> TownySettings.getOutpostCost(), town, "PlotSetOutpost", Translatable.of("msg_err_cannot_afford_to_set_outpost")))
			.setTitle(Translatable.of("msg_confirm_purchase", prettyMoney(TownySettings.getOutpostCost())))
			.sendTo(player);
		} else {
			// Set the outpost spawn and display feedback with no cost confirmation.
			town.addOutpostSpawn(player.getLocation());
			TownyMessaging.sendMsg(player, Translatable.of("msg_plot_set_cost", prettyMoney(TownySettings.getOutpostCost()), Translatable.of("outpost")));
		}
	}

	public void parsePlotSetName(Player player, String[] name, TownBlock townBlock) throws TownyException {
		if (name.length == 0) {
			townBlock.setName("");
			TownyMessaging.sendMsg(player, Translatable.of("msg_plot_name_removed"));
			townBlock.save();
			return;
		}

		String newName = NameValidation.checkAndFilterPlotNameOrThrow(StringMgmt.join(name, "_"));
		townBlock.setName(newName);
		townBlock.save();
		TownyMessaging.sendMsg(player, Translatable.of("msg_plot_name_set_to", townBlock.getName()));
	}

	public void parsePlotNotForSale(Player player, String[] args, Resident resident, TownBlock townBlock) throws TownyException {
		checkPermOrThrow(player, PermissionNodes.TOWNY_COMMAND_PLOT_NOTFORSALE.getNode());

		List<WorldCoord> selection = AreaSelectionUtil.selectWorldCoordArea(resident, WorldCoord.parseWorldCoord(player), args);
		selection = AreaSelectionUtil.filterPlotsForSale(selection);
		
		if (resident.isAdmin()) {
			for (WorldCoord worldCoord : selection) {
				if (worldCoord.getTownBlock().hasPlotObjectGroup())
					throw new TownyException(Translatable.of("msg_err_plot_belongs_to_group_plot_nfs", worldCoord));
				setPlotForSale(resident, worldCoord, -1);
			}
			return;
		}
		Town town = townBlock.getTownOrNull();
		if (town == null) // Should not be possible but we check anyways.
			throw new TownyException(Translatable.of("msg_err_empty_area_selection"));

		// The follow test will clean up the initial selection fairly well, the plotTestOwner later on in the setPlotForSale will ultimately stop any funny business.
		if (
			(resident.hasPermissionNode(PermissionNodes.TOWNY_COMMAND_PLOT_ASMAYOR.getNode()) && town.hasResident(resident))
			|| (resident.hasPermissionNode(PermissionNodes.TOWNY_COMMAND_PLOT_ASMAYORINUNOWNED.getNode()) && town.hasResident(resident) && !townBlock.hasResident())
			) {
			selection = AreaSelectionUtil.filterOwnedBlocks(resident.getTown(), selection); // Treat it as a mayor able to set their town's plots not for sale.
		} else {
			selection = AreaSelectionUtil.filterOwnedBlocks(resident, selection); // Treat it as a resident making their own plots not for sale.
		}

		if (selection.isEmpty())
			throw new TownyException(Translatable.of("msg_err_empty_area_selection"));

		// Set each WorldCoord in selection not for sale.
		for (WorldCoord worldCoord : selection) {
			
			// Skip over any plots that are part of a PlotGroup.
			if (worldCoord.getTownBlock().hasPlotObjectGroup()) {
				TownyMessaging.sendErrorMsg(player, Translatable.of("msg_err_plot_belongs_to_group_plot_nfs", worldCoord));
				continue;
			}
			
			setPlotForSale(resident, worldCoord, -1);
		}
	}

	public void parsePlotUnclaim(Player player, String[] split, Resident resident) throws TownyException {
		checkPermOrThrow(player, PermissionNodes.TOWNY_COMMAND_PLOT_UNCLAIM.getNode());

		if (split.length == 2 && split[1].equalsIgnoreCase("all")) {
			// Start the unclaim task
			plugin.getScheduler().runAsync(new PlotClaim(plugin, player, resident, null, false, false, false));
			return;
		}

		List<WorldCoord> selection = AreaSelectionUtil.selectWorldCoordArea(resident, WorldCoord.parseWorldCoord(player), StringMgmt.remFirstArg(split));
		selection = AreaSelectionUtil.filterOwnedBlocks(resident, selection);

		if (selection.size() == 0)
			throw new TownyException(Translatable.of("msg_err_empty_area_selection"));


		for (WorldCoord coord : selection) {
			TownBlock townBlock = coord.getTownBlock();

			if (!townBlock.hasPlotObjectGroup()) {
				// Start the unclaim task
				plugin.getScheduler().runAsync(new PlotClaim(plugin, player, resident, selection, false, false, false));
				return;
			}

			// Get all the townblocks part of the group.
			final List<WorldCoord> groupSelection = townBlock.getPlotObjectGroup().getTownBlocks().stream().map(TownBlock::getWorldCoord).collect(Collectors.toList());

			// Create confirmation.
			Confirmation.runOnAcceptAsync(new PlotClaim(plugin, player, resident, groupSelection, false, false, false))
				.setTitle(Translatable.of("msg_plot_group_unclaim_confirmation", townBlock.getPlotObjectGroup().getTownBlocks().size()).append(" ").append(Translatable.of("are_you_sure_you_want_to_continue")))
				.sendTo(player);
			return;
		}
	}

	private boolean plotCommandAllowedInWilderness(String command) {
		return command.equalsIgnoreCase("perm") || command.equalsIgnoreCase("claim") || command.equalsIgnoreCase("info");
	}

	private void parsePlotJailCell(Player player, Resident resident, TownBlock townBlock, String[] args) throws TownyException {
		checkPermOrThrow(player, PermissionNodes.TOWNY_COMMAND_PLOT_JAILCELL.getNode());

		if (args.length == 0 || args[0].equalsIgnoreCase("?") || args[0].equalsIgnoreCase("help"))
			HelpMenu.PLOT_JAILCELL.send(player);

		// Fail if we're not in a jail plot.
		if (townBlock == null || !townBlock.isJail())
			throw new TownyException("msg_err_location_is_not_within_a_jail_plot");

		// Test we are allowed to work on this plot, and able to set a jail spawn.
		// If this fails it will trigger a TownyException.
		TownyAPI.getInstance().testPlotOwnerOrThrow(resident, townBlock);

		Jail jail = townBlock.getJail();
		
		if (args.length > 0) {
			if (args[0].equalsIgnoreCase("add")) {
				SpawnPointLocation cellLoc = SpawnPointLocation.parseSpawnPointLocation(player.getLocation());
				
				if (jail.hasJailCell(cellLoc))
					throw new TownyException(Translatable.of("msg_err_this_location_already_has_a_jailcell"));
				
				jail.addJailCell(player.getLocation());
				TownyMessaging.sendMsg(player, Translatable.of("msg_jail_cell_set"));
				jail.save();

			} else if (args[0].equalsIgnoreCase("remove")) {
				if (!jail.hasCells())
					throw new TownyException(Translatable.of("msg_err_this_jail_has_no_cells"));
				
				if (jail.getJailCellCount() == 1) 
					throw new TownyException(Translatable.of("msg_err_you_cannot_remove_the_last_cell"));
				
				SpawnPointLocation cellLoc = SpawnPointLocation.parseSpawnPointLocation(player.getLocation());
				
				if (!jail.hasJailCell(cellLoc))
					throw new TownyException(Translatable.of("msg_err_no_cell_found_at_this_location"));
				
				jail.removeJailCell(player.getLocation());
				TownyMessaging.sendMsg(player, Translatable.of("msg_jail_cell_removed"));
				jail.save();
			} else {
				HelpMenu.PLOT_JAILCELL.send(player);
			}
		}
	}

	/**
	 * Returns a TownyPermissionChange object representing the change action
	 *
	 * @param player Player initiator
	 * @param townBlockOwner Resident/Town with the targeted permissions change
	 * @param townBlock Targeted town block
	 * @param split Permission arguments
	 * @return a TownyPermissionChange object
	 * @throws CancelledEventException If the {@link TownBlockPermissionChangeEvent} is cancelled
	 */
	public static TownyPermissionChange setTownBlockPermissions(Player player, TownBlockOwner townBlockOwner, TownBlock townBlock, String[] split) throws CancelledEventException {
		TownyPermissionChange permChange;

		if (split.length == 0 || split[0].equalsIgnoreCase("?") || split.length > 3) {

			TownyMessaging.sendMessage(player, ChatTools.formatTitle("/... set perm"));
			if (townBlockOwner instanceof Town)
				TownyMessaging.sendMessage(player, ChatTools.formatCommand("Level", "[resident/nation/ally/outsider]", "", ""));
			if (townBlockOwner instanceof Resident)
				TownyMessaging.sendMessage(player, ChatTools.formatCommand("Level", "[friend/town/ally/outsider]", "", ""));

			TownyMessaging.sendMessage(player, ChatTools.formatCommand("Type", "[build/destroy/switch/itemuse]", "", ""));
			TownyMessaging.sendMessage(player, ChatTools.formatCommand("", "set perm", "[on/off]", "Toggle all permissions"));
			TownyMessaging.sendMessage(player, ChatTools.formatCommand("", "set perm", "[level/type] [on/off]", ""));
			TownyMessaging.sendMessage(player, ChatTools.formatCommand("", "set perm", "[level] [type] [on/off]", ""));
			TownyMessaging.sendMessage(player, ChatTools.formatCommand("", "set perm", "reset", ""));
			TownyMessaging.sendMessage(player, ChatTools.formatCommand("Eg", "/plot set perm", "friend build on", ""));
			return null;

		} else {

			TownyPermission perm = townBlock.getPermissions();

			if (split.length == 1) {

				if (split[0].equalsIgnoreCase("reset")) {

					// reset this townBlock permissions (by town/resident)
					permChange = new TownyPermissionChange(TownyPermissionChange.Action.RESET, false, townBlock);
					BukkitTools.ifCancelledThenThrow(new TownBlockPermissionChangeEvent(townBlock, permChange));
					perm.change(permChange);
					townBlock.save();

					TownyMessaging.sendMsg(player, Translatable.of("msg_set_perms_reset_single"));
					// Reset all caches as this can affect everyone.
					plugin.resetCache();

					return permChange;

				} else {

					// Set all perms to On or Off
					// '/plot set perm off'

					try {
						boolean b = StringMgmt.parseOnOff(split[0]);

						permChange = new TownyPermissionChange(TownyPermissionChange.Action.ALL_PERMS, b);
					} catch (Exception e) {
						TownyMessaging.sendErrorMsg(player, Translatable.of("msg_plot_set_perm_syntax_error"));
						return null;
					}

				}

			} else if (split.length == 2) {
				boolean b;

				try {
					b = StringMgmt.parseOnOff(split[1]);
				} catch (Exception e) {
					TownyMessaging.sendErrorMsg(player, Translatable.of("msg_plot_set_perm_syntax_error"));
					return null;
				}

				if (split[0].equalsIgnoreCase("friend"))
					split[0] = "resident";
				else if (split[0].equalsIgnoreCase("town"))
					split[0] = "nation";
				else if (split[0].equalsIgnoreCase("itemuse"))
					split[0] = "item_use";

				// Check if it is a perm level first
				try {
					TownyPermission.PermLevel permLevel = TownyPermission.PermLevel.valueOf(split[0].toUpperCase(Locale.ROOT));

					permChange = new TownyPermissionChange(TownyPermissionChange.Action.PERM_LEVEL, b, permLevel);
				}
				catch (IllegalArgumentException permLevelException) {
					// If it is not a perm level, then check if it is a action type
					try {
						TownyPermission.ActionType actionType = TownyPermission.ActionType.valueOf(split[0].toUpperCase(Locale.ROOT));

						permChange = new TownyPermissionChange(TownyPermissionChange.Action.ACTION_TYPE, b, actionType);
					} catch (IllegalArgumentException actionTypeException) {
						TownyMessaging.sendErrorMsg(player, Translatable.of("msg_plot_set_perm_syntax_error"));
						return null;
					}
				}
			} else {
				// Reset the friend to resident so the perm settings don't fail
				if (split[0].equalsIgnoreCase("friend"))
					split[0] = "resident";

					// reset the town to nation so the perm settings don't fail
				else if (split[0].equalsIgnoreCase("town"))
					split[0] = "nation";

				if (split[1].equalsIgnoreCase("itemuse"))
					split[1] = "item_use";

				TownyPermission.PermLevel permLevel;
				TownyPermission.ActionType actionType;

				try {
					permLevel = TownyPermission.PermLevel.valueOf(split[0].toUpperCase(Locale.ROOT));
					actionType = TownyPermission.ActionType.valueOf(split[1].toUpperCase(Locale.ROOT));
				} catch (IllegalArgumentException ignore) {
					TownyMessaging.sendErrorMsg(player, Translatable.of("msg_plot_set_perm_syntax_error"));
					return null;
				}

				try {
					boolean b = StringMgmt.parseOnOff(split[2]);

					permChange = new TownyPermissionChange(TownyPermissionChange.Action.SINGLE_PERM, b, permLevel, actionType);
				} catch (Exception e) {
					TownyMessaging.sendErrorMsg(player, Translatable.of("msg_plot_set_perm_syntax_error"));
					return null;
				}

			}

			BukkitTools.ifCancelledThenThrow(new TownBlockPermissionChangeEvent(townBlock, permChange));
			perm.change(permChange);

			townBlock.setChanged(true);
			townBlock.save();
			if (!townBlock.hasPlotObjectGroup()) {
				TownyMessaging.sendMsg(player, Translatable.of("msg_set_perms"));
				TownyMessaging.sendMessage(player, Colors.Green + Translatable.of("status_perm").forLocale(player) + " " + ((townBlockOwner instanceof Resident) ? perm.getColourString().replace("n", "t") : perm.getColourString().replace("f", "r")));
				TownyMessaging.sendMessage(player, Colors.Green + Translatable.of("status_pvp").forLocale(player) + " " + ((perm.pvp) ? Colors.Red + "ON" : Colors.LightGreen + "OFF") + 
												   Colors.Green + Translatable.of("explosions").forLocale(player) + " " + ((perm.explosion) ? Colors.Red + "ON" : Colors.LightGreen + "OFF") + 
												   Colors.Green + Translatable.of("firespread").forLocale(player) + " " + ((perm.fire) ? Colors.Red + "ON" : Colors.LightGreen + "OFF") + 
												   Colors.Green + Translatable.of("mobspawns").forLocale(player) + " " + ((perm.mobs) ? Colors.Red + "ON" : Colors.LightGreen + "OFF"));
			}


			//Change settings event
			BukkitTools.fireEvent(new TownBlockSettingsChangedEvent(townBlock));

			// Reset all caches as this can affect everyone.
			plugin.resetCache();
			return permChange;
		}
	}

	/**
	 * Set the plot for sale/not for sale if permitted
	 * 
	 * @param resident - Resident Object.
	 * @param worldCoord - WorldCoord.
	 * @param forSale - Price.
	 * @throws TownyException - Exception.
	 */
	public void setPlotForSale(Resident resident, WorldCoord worldCoord, double forSale) throws TownyException {
		
		TownBlock townBlock = worldCoord.getTownBlockOrNull();
		if (townBlock == null || !townBlock.hasTown())
			throw new TownyException(Translatable.of("msg_err_not_part_town"));
			
		// Test we are allowed to work on this plot
		// If this fails it will trigger a TownyException.
		TownyAPI.getInstance().testPlotOwnerOrThrow(resident, townBlock);
		townBlock.setPlotPrice(Math.min(TownySettings.getMaxPlotPrice(), forSale));

		if (forSale != -1) {
			Translatable message = TownyEconomyHandler.isActive()
				? Translatable.of("msg_plot_for_sale_amount", resident.getName(), worldCoord.toString(), prettyMoney(townBlock.getPlotPrice()))
				: Translatable.of("msg_plot_for_sale", resident.getName(), worldCoord.toString());
			
			TownyMessaging.sendPrefixedTownMessage(townBlock.getTownOrNull(), message);
			
			if (!resident.hasTown() || (resident.hasTown() && townBlock.getTownOrNull() != resident.getTownOrNull()))
				TownyMessaging.sendMsg(resident, message);
			
			BukkitTools.fireEvent(new PlotSetForSaleEvent(resident, townBlock.getPlotPrice(), townBlock));
		} else {
			TownyMessaging.sendMsg(resident, Translatable.of("msg_plot_set_to_nfs"));
			BukkitTools.fireEvent(new PlotNotForSaleEvent(resident, townBlock));
		}

		// Save this townblock so the for sale status is remembered.
		townBlock.save();
	}

	/**
	 * Toggle the plots flags for pvp/explosion/fire/mobs (if town/world
	 * permissions allow)
	 * 
	 * @param player - Player.
	 * @param resident Resident using the command.
	 * @param townBlock - TownBlock object.
	 * @param split  - Current command arguments.
	 * @throws TownyException 
	 */
	public void plotToggle(Player player, Resident resident, TownBlock townBlock, String[] split) throws TownyException {
		if (split.length == 0 || split[0].equalsIgnoreCase("?")) {
			HelpMenu.PLOT_TOGGLE.send(player);
			return;
		}

		TownyAPI.getInstance().testPlotOwnerOrThrow(resident, townBlock); // Test we are allowed to work on this plot

		catchPlotGroup(townBlock, "/plot group toggle ?");

		Town town = townBlock.getTownOrNull();
		if (town == null)
			throw new TownyException(Translatable.of("msg_not_claimed_1"));

		Optional<Boolean> choice = Optional.empty();
		if (split.length == 2)
			choice = BaseCommand.parseToggleChoice(split[1]);

		switch(split[0].toLowerCase(Locale.ROOT)) {
		case "pvp":
			checkPermOrThrow(player, PermissionNodes.TOWNY_COMMAND_PLOT_TOGGLE_PVP.getNode());
			tryToggleTownBlockPVP(player, resident, townBlock, split, town, choice);
			TownyMessaging.sendMsg(player, Translatable.of("msg_changed_pvp", "Plot", townBlock.getPermissions().pvp ? Translatable.of("enabled") : Translatable.of("disabled")));
			break;
		case "explosion":
			checkPermOrThrow(player, PermissionNodes.TOWNY_COMMAND_PLOT_TOGGLE_EXPLOSION.getNode());
			tryToggleTownBlockExplosion(player, townBlock, split, choice);
			TownyMessaging.sendMsg(player, Translatable.of("msg_changed_expl", "the Plot", townBlock.getPermissions().explosion ? Translatable.of("enabled") : Translatable.of("disabled")));
			break;
		case "fire":
			checkPermOrThrow(player, PermissionNodes.TOWNY_COMMAND_PLOT_TOGGLE_FIRE.getNode());
			tryToggleTownBlockFire(player, townBlock, split, choice);
			TownyMessaging.sendMsg(player, Translatable.of("msg_changed_fire", "the Plot", townBlock.getPermissions().fire ? Translatable.of("enabled") : Translatable.of("disabled")));
			break;
		case "mobs":
			checkPermOrThrow(player, PermissionNodes.TOWNY_COMMAND_PLOT_TOGGLE_MOBS.getNode());
			tryToggleTownBlockMobs(player, townBlock, split, choice);
			TownyMessaging.sendMsg(player, Translatable.of("msg_changed_mobs", "the Plot", townBlock.getPermissions().mobs ? Translatable.of("enabled") : Translatable.of("disabled")));
			break;
		case "taxed":
			checkPermOrThrow(player, PermissionNodes.TOWNY_COMMAND_PLOT_ASMAYOR.getNode());
			tryToggleTownBlockTaxed(player, townBlock, split, choice);
			TownyMessaging.sendMsg(player, Translatable.of("msg_changed_plot_taxed", townBlock.isTaxed() ? Translatable.of("enabled") : Translatable.of("disabled")));
			townBlock.save();
			return;
		default:
			if (TownyCommandAddonAPI.hasCommand(CommandType.PLOT_TOGGLE, split[0])) {
				TownyCommandAddonAPI.getAddonCommand(CommandType.PLOT_TOGGLE, split[0]).execute(player, "plot", split);
			} else {
				TownyMessaging.sendErrorMsg(player, Translatable.of("msg_err_invalid_property", "plot"));
				return;
			}	
		}

		townBlock.setChanged(true);
		//Change settings event
		BukkitTools.fireEvent(new TownBlockSettingsChangedEvent(townBlock));
		townBlock.save();
	}

	private void tryToggleTownBlockPVP(Player player, Resident resident, TownBlock townBlock, String[] split, Town town, Optional<Boolean> choice) throws TownyException {
		// Make sure we are allowed to set these permissions.
		toggleTest(player, townBlock, StringMgmt.join(split, " "));

		if (TownySettings.getPVPCoolDownTime() > 0 && !resident.isAdmin()) {
			// Test to see if the pvp cooldown timer is active for the town this plot belongs to.
			if (CooldownTimerTask.hasCooldown(town.getUUID().toString(), CooldownType.PVP))
				throw new TownyException(Translatable.of("msg_err_cannot_toggle_pvp_x_seconds_remaining", CooldownTimerTask.getCooldownRemaining(town.getUUID().toString(), CooldownType.PVP)));

			// Test to see if the pvp cooldown timer is active for this plot.
			if (CooldownTimerTask.hasCooldown(townBlock.getWorldCoord().toString(), CooldownType.PVP))
				throw new TownyException(Translatable.of("msg_err_cannot_toggle_pvp_x_seconds_remaining", CooldownTimerTask.getCooldownRemaining(townBlock.getWorldCoord().toString(), CooldownType.PVP)));
		}

		// Prevent plot pvp from being enabled if admin pvp is disabled
		if (town.isAdminDisabledPVP() && !townBlock.getPermissions().pvp)
			throw new TownyException(Translatable.of("msg_err_admin_controlled_pvp_prevents_you_from_changing_pvp", "adminDisabledPVP", "on"));
		
		// Prevent plot pvp from being disabled if admin pvp is enabled
		if (town.isAdminEnabledPVP() && townBlock.getPermissions().pvp)
			throw new TownyException(Translatable.of("msg_err_admin_controlled_pvp_prevents_you_from_changing_pvp", "adminEnabledPVP", "off"));

		if (TownySettings.getOutsidersPreventPVPToggle() && choice.orElse(!townBlock.getPermissions().pvp)) {
			for (Player target : Bukkit.getOnlinePlayers()) {
				if (!town.hasResident(target) && !player.getName().equals(target.getName()) && townBlock.getWorldCoord().equals(WorldCoord.parseWorldCoord(target)))
					throw new TownyException(Translatable.of("msg_cant_toggle_pvp_outsider_in_plot"));
			}
		}

		// Fire cancellable event directly before setting the toggle.
		BukkitTools.ifCancelledThenThrow(new PlotTogglePvpEvent(townBlock, player, choice.orElse(!townBlock.getPermissions().pvp)));

		townBlock.getPermissions().pvp = choice.orElse(!townBlock.getPermissions().pvp);
		// Add a cooldown timer for this plot.
		if (TownySettings.getPVPCoolDownTime() > 0 && !resident.isAdmin())
			CooldownTimerTask.addCooldownTimer(townBlock.getWorldCoord().toString(), CooldownType.PVP);
	}

	private void tryToggleTownBlockExplosion(Player player, TownBlock townBlock, String[] split, Optional<Boolean> choice) throws TownyException {
		// Make sure we are allowed to set these permissions.
		toggleTest(player, townBlock, StringMgmt.join(split, " "));
		// Fire cancellable event directly before setting the toggle.
		BukkitTools.ifCancelledThenThrow(new PlotToggleExplosionEvent(townBlock, player, choice.orElse(!townBlock.getPermissions().explosion)));

		townBlock.getPermissions().explosion = choice.orElse(!townBlock.getPermissions().explosion);
	}

	private void tryToggleTownBlockFire(Player player, TownBlock townBlock, String[] split, Optional<Boolean> choice) throws TownyException {
		// Make sure we are allowed to set these permissions.
		toggleTest(player, townBlock, StringMgmt.join(split, " "));
		// Fire cancellable event directly before setting the toggle.
		BukkitTools.ifCancelledThenThrow(new PlotToggleFireEvent(townBlock, player, choice.orElse(!townBlock.getPermissions().fire)));

		townBlock.getPermissions().fire = choice.orElse(!townBlock.getPermissions().fire);
	}

	private void tryToggleTownBlockMobs(Player player, TownBlock townBlock, String[] split, Optional<Boolean> choice) throws TownyException {
		// Make sure we are allowed to set these permissions.
		toggleTest(player, townBlock, StringMgmt.join(split, " "));
		// Fire cancellable event directly before setting the toggle.
		BukkitTools.ifCancelledThenThrow(new PlotToggleMobsEvent(townBlock, player, choice.orElse(!townBlock.getPermissions().mobs)));

		townBlock.getPermissions().mobs = choice.orElse(!townBlock.getPermissions().mobs);
	}

	private void tryToggleTownBlockTaxed(Player player, TownBlock townBlock, String[] split, Optional<Boolean> choice) throws TownyException {
		// Fire cancellable event directly before setting the toggle.
		BukkitTools.ifCancelledThenThrow(new PlotToggleTaxedEvent(townBlock, player, choice.orElse(!townBlock.isTaxed())));

		townBlock.setTaxed(choice.orElse(!townBlock.isTaxed()));
	}
	
	/**
	 * Check the world and town settings to see if we are allowed to alter these
	 * settings
	 * 
	 * @param player
	 * @param townBlock
	 * @param split
	 * @throws TownyException if toggle is not permitted
	 */
	private void toggleTest(Player player, TownBlock townBlock, String split) throws TownyException {

		// Make sure we are allowed to set these permissions.

		split = split.toLowerCase(Locale.ROOT);

		if (split.contains("mobs")) {
			if (townBlock.getWorld().isForceTownMobs())
				throw new TownyException(Translatable.of("msg_world_mobs"));
			if (townBlock.getTownOrNull().isAdminEnabledMobs())
				throw new TownyException(Translatable.of("msg_town_mobs"));
		}

		if (split.contains("fire")) {
			if (townBlock.getWorld().isForceFire())
				throw new TownyException(Translatable.of("msg_world_fire"));
		}

		if (split.contains("explosion")) {
			if (townBlock.getWorld().isForceExpl())
				throw new TownyException(Translatable.of("msg_world_expl"));
		}

		if (split.contains("pvp")) {
			if (townBlock.getWorld().isForcePVP())
				throw new TownyException(Translatable.of("msg_world_pvp"));
		}
		if ((split.contains("pvp")) || (split.trim().equalsIgnoreCase("off"))) {
			if (townBlock.getType().equals(TownBlockType.ARENA))
				throw new TownyException(Translatable.of("msg_plot_pvp"));
		}
	}

	private void parseDistrict(String[] split, Resident resident, TownBlock townBlock, Player player) throws TownyException {

		Town town = townBlock.getTownOrNull();
		if (town == null)
			throw new TownyException(Translatable.of("msg_not_claimed_1"));
		
		if (!town.hasResident(player))
			throw new TownyException(Translatable.of("msg_err_not_part_town"));

		try {
			checkPermOrThrow(player, PermissionNodes.TOWNY_COMMAND_PLOT_ASMAYOR.getNode());
		} catch (NoPermissionException e) {
			throw new TownyException(Translatable.of("msg_not_mayor_ass"));
		}

		if (split.length <= 0 || split[0].equalsIgnoreCase("?")) {
			HelpMenu.PLOT_DISTRICT_HELP.send(player);
			if (townBlock.hasDistrict())
				TownyMessaging.sendMsg(player, Translatable.of("status_district_name_and_size", townBlock.getDistrict().getName(), townBlock.getDistrict().getTownBlocks().size()));
			return;
		}

		switch (split[0].toLowerCase(Locale.ROOT)) {
		case "add", "new", "create" -> parseDistrictAdd(split, townBlock, player, town);
		case "delete" -> parseDistrictDelete(townBlock, player, town);
		case "remove" -> parseDistrictRemove(townBlock, player, town);
		case "rename" ->  parseDistrictRename(split, townBlock, player);
		default -> {
			HelpMenu.PLOT_DISTRICT_HELP.send(player);
			if (townBlock.hasDistrict())
				TownyMessaging.sendMsg(player, Translatable.of("status_district_name_and_size", townBlock.getDistrict().getName(), townBlock.getDistrict().getTownBlocks().size()));
		}
		}
	}

	public void parseDistrictAdd(String[] split, TownBlock townBlock, Player player, Town town) throws TownyException {
		checkPermOrThrow(player, PermissionNodes.TOWNY_COMMAND_PLOT_DISTRICT_ADD.getNode());

		Resident resident = getResidentOrThrow(player);

		if (split.length != 2 && !resident.hasDistrictName())
			throw new TownyException(Translatable.of("msg_err_district_name_required"));

		String districtName = split.length == 2
				? NameValidation.checkAndFilterDistrictNameOrThrow(split[1])
				: resident.hasDistrictName()
					? resident.getDistrictName()
					: null;

		if (townBlock.hasDistrict()) {
			// Already has a District and it is the same name being used to re-add.
			if (townBlock.getDistrict().getName().equalsIgnoreCase(districtName))
				throw new TownyException(Translatable.of("msg_err_this_plot_is_already_part_of_the_district_x", districtName));

			District oldDistrict = townBlock.getDistrict();
			ProximityUtil.testAdjacentRemoveDistrictRulesOrThrow(townBlock.getWorldCoord(), town, oldDistrict, 1);

			final String name = districtName;
			// Already has a District, ask if they want to transfer from one district to another.
			Confirmation.runOnAccept( ()-> {

				oldDistrict.removeTownBlock(townBlock);
				if (oldDistrict.getTownBlocks().isEmpty() && !BukkitTools.isEventCancelled(new DistrictDeletedEvent(oldDistrict, player, DistrictDeletedEvent.Cause.NO_TOWNBLOCKS))) {
					String oldName = oldDistrict.getName();
					town.removeDistrict(oldDistrict);
					TownyUniverse.getInstance().getDataSource().removeDistrict(oldDistrict);
					TownyMessaging.sendMsg(player, Translatable.of("msg_district_deleted", oldName));
				} else 
					oldDistrict.save();
				
				try {
					createOrAddOnToDistrict(townBlock, town, player, name);
					resident.setDistrictName(name);
					TownyMessaging.sendMsg(player, Translatable.of("msg_townblock_transferred_from_x_to_x_district", oldDistrict.getName(), townBlock.getDistrict().getName()));
				} catch (TownyException e) {
					TownyMessaging.sendErrorMsg(player, e.getMessage(player));
				}
			})
			.setTitle(Translatable.of("msg_district_already_exists_did_you_want_to_transfer", townBlock.getDistrict().getName(), name))
			.sendTo(player);
		} else {
			// Create a brand new district.
			createOrAddOnToDistrict(townBlock, town, player, districtName);
			resident.setDistrictName(districtName);
			TownyMessaging.sendMsg(player, Translatable.of("msg_plot_was_put_into_district_x", townBlock.getX(), townBlock.getZ(), townBlock.getDistrict().getName()));
		}
	}

	private void createOrAddOnToDistrict(TownBlock townBlock, Town town, Player player, String districtName) throws TownyException {
		District newDistrict;
		
		// Don't add the district to the town data if it's already there.
		if (town.hasDistrictName(districtName)) {
			newDistrict = town.getDistrictFromName(districtName);

			ProximityUtil.testAdjacentAddDistrictRulesOrThrow(townBlock.getWorldCoord(), town, newDistrict, 1);

			BukkitTools.ifCancelledThenThrow(new DistrictAddEvent(newDistrict, townBlock, player));

		} else {
			// This is a brand new District, register it.
			newDistrict = new District(UUID.randomUUID(), districtName, town);

			BukkitTools.ifCancelledThenThrow(new DistrictCreatedEvent(newDistrict, townBlock, player));

			TownyUniverse.getInstance().registerDistrict(newDistrict);
		}

		// Add district to townblock, this also adds the townblock to the district.
		townBlock.setDistrict(newDistrict);

		// Add the district to the town set.
		town.addDistrict(newDistrict);

		// Save changes.
		newDistrict.save();
		townBlock.save(); 
	}

	public void parseDistrictDelete(TownBlock townBlock, Player player, Town town) throws TownyException {
		checkPermOrThrow(player, PermissionNodes.TOWNY_COMMAND_PLOT_DISTRICT_DELETE.getNode());

		District district = catchMissingDistrict(townBlock);

		Confirmation.runOnAccept(()-> {
			String name = district.getName();
			if (!BukkitTools.isEventCancelled(new DistrictDeletedEvent(district, player, DistrictDeletedEvent.Cause.DELETED))) {
				town.removeDistrict(district);
				TownyUniverse.getInstance().getDataSource().removeDistrict(district);
				TownyMessaging.sendMsg(player, Translatable.of("msg_district_deleted", name));
			}
		}).sendTo(player);
	}

	public void parseDistrictRemove(TownBlock townBlock, Player player, Town town) throws TownyException {
		checkPermOrThrow(player, PermissionNodes.TOWNY_COMMAND_PLOT_DISTRICT_REMOVE.getNode());

		District district = catchMissingDistrict(townBlock);
		String name = district.getName();
		
		try {
			ProximityUtil.testAdjacentRemoveDistrictRulesOrThrow(townBlock.getWorldCoord(), town, district, 1);
		} catch (TownyException e) {
			throw new TownyException(Translatable.of("msg_err_cannot_remove_from_district_not_enough_adjacent_claims", name));
		}
		
		// Remove the plot from the district.
		district.removeTownBlock(townBlock);

		// Detach district from townblock.
		townBlock.removeDistrict();

		// Save
		townBlock.save();
		TownyMessaging.sendMsg(player, Translatable.of("msg_plot_was_removed_from_district_x", townBlock.getX(), townBlock.getZ(), name));
		
		if (district.getTownBlocks().isEmpty() && !BukkitTools.isEventCancelled(new DistrictDeletedEvent(district, player, DistrictDeletedEvent.Cause.NO_TOWNBLOCKS))) {
			town.removeDistrict(district);
			TownyUniverse.getInstance().getDataSource().removeDistrict(district);
			TownyMessaging.sendMsg(player, Translatable.of("msg_district_empty_deleted", name));
		}
	}
	
	public void parseDistrictRename(String[] split, TownBlock townBlock, Player player) throws TownyException, AlreadyRegisteredException {
		checkPermOrThrow(player, PermissionNodes.TOWNY_COMMAND_PLOT_DISTRICT_RENAME.getNode());

		if (split.length == 1)
			throw new TownyException(Translatable.of("msg_err_rename_district_name_required"));

		District district = catchMissingDistrict(townBlock);
		String newName = split[1];
		NameValidation.checkAndFilterDistrictNameOrThrow(newName);
		String oldName = district.getName();
		// Change name;
		TownyUniverse.getInstance().getDataSource().renameDistrict(district, newName);
		TownyMessaging.sendMsg(player, Translatable.of("msg_district_renamed_from_x_to_y", oldName, newName));
	}

	private void parsePlotGroup(String[] split, Resident resident, TownBlock townBlock, Player player) throws TownyException {

		Town town = townBlock.getTownOrNull();
		if (town == null)
			throw new TownyException(Translatable.of("msg_not_claimed_1"));

		// Test we are allowed to work on this plot
		// If this fails it will trigger a TownyException.
		TownyAPI.getInstance().testPlotOwnerOrThrow(resident, townBlock);

		if (split.length <= 0 || split[0].equalsIgnoreCase("?")) {
			HelpMenu.PLOT_GROUP_HELP.send(player);
			if (townBlock.hasPlotObjectGroup())
				TownyMessaging.sendMsg(player, Translatable.of("status_plot_group_name_and_size", townBlock.getPlotObjectGroup().getName(), townBlock.getPlotObjectGroup().getTownBlocks().size()));
			return;
		}

		switch (split[0].toLowerCase(Locale.ROOT)) {
		case "add", "new", "create" -> parsePlotGroupAdd(split, townBlock, player, town);
		case "delete" -> parsePlotGroupDelete(townBlock, player, town);
		case "fs", "forsale" -> parsePlotGroupForSale(split, resident, townBlock, player, town);
		case "nfs", "notforsale" -> parsePlotGroupNotForSale(resident, townBlock, player, town);
		case "perm" -> parsePlotGroupPerm(split, resident, townBlock, player);
		case "remove" -> parsePlotGroupRemove(townBlock, player, town);
		case "rename" ->  parsePlotGroupRename(split, townBlock, player);
		case "set" -> parsePlotGroupSet(split, resident, townBlock, player, town);
		case "toggle" -> parsePlotGroupToggle(split, townBlock, player, resident);
		case "trust" -> parsePlotGroupTrust(split, townBlock, player);
		default -> {
			HelpMenu.PLOT_GROUP_HELP.send(player);
			if (townBlock.hasPlotObjectGroup())
				TownyMessaging.sendMsg(player, Translatable.of("status_plot_group_name_and_size", townBlock.getPlotObjectGroup().getName(), townBlock.getPlotObjectGroup().getTownBlocks().size()));
		}
		}
	}

	public void parsePlotGroupAdd(String[] split, TownBlock townBlock, Player player, Town town) throws TownyException {
		checkPermOrThrow(player, PermissionNodes.TOWNY_COMMAND_PLOT_GROUP_ADD.getNode());

		Resident resident = getResidentOrThrow(player);

		if (split.length != 2 && !resident.hasPlotGroupName())
			throw new TownyException(Translatable.of("msg_err_plot_group_name_required"));

		String plotGroupName = split.length == 2
				? NameValidation.checkAndFilterPlotGroupNameOrThrow(split[1])
				: resident.hasPlotGroupName()
					? resident.getPlotGroupName()
					: null;

		if (town.hasPlotGroupName(plotGroupName)) {
			TownBlockType groupType = town.getPlotObjectGroupFromName(plotGroupName).getTownBlockType();
			if (townBlock.getType() != groupType)
				throw new TownyException(Translatable.of("msg_err_this_townblock_doesnt_match_the_groups_type", groupType.getName()));
		}

		if (townBlock.hasPlotObjectGroup()) {
			// Already has a PlotGroup and it is the same name being used to re-add.
			if (townBlock.getPlotObjectGroup().getName().equalsIgnoreCase(plotGroupName))
				throw new TownyException(Translatable.of("msg_err_this_plot_is_already_part_of_the_plot_group_x", plotGroupName));

			final String name = plotGroupName;
			// Already has a PlotGroup, ask if they want to transfer from one group to another.
			Confirmation.runOnAccept( ()-> {
				PlotGroup oldGroup = townBlock.getPlotObjectGroup();
				oldGroup.removeTownBlock(townBlock);
				if (oldGroup.getTownBlocks().isEmpty()) {
					new PlotGroupDeletedEvent(oldGroup, player, PlotGroupDeletedEvent.Cause.NO_TOWNBLOCKS).callEvent();
					String oldName = oldGroup.getName();
					town.removePlotGroup(oldGroup);
					TownyUniverse.getInstance().getDataSource().removePlotGroup(oldGroup);
					TownyMessaging.sendMsg(player, Translatable.of("msg_plotgroup_deleted", oldName));
				} else 
					oldGroup.save();
				
				try {
					createOrAddOnToPlotGroup(townBlock, town, player, name);
					resident.setPlotGroupName(name);
					TownyMessaging.sendMsg(player, Translatable.of("msg_townblock_transferred_from_x_to_x_group", oldGroup.getName(), townBlock.getPlotObjectGroup().getName()));
				} catch (TownyException e) {
					TownyMessaging.sendErrorMsg(player, e.getMessage(player));
				}
			})
			.setTitle(Translatable.of("msg_plot_group_already_exists_did_you_want_to_transfer", townBlock.getPlotObjectGroup().getName(), split[1]))
			.sendTo(player);
		} else {
			// Create a brand new plot group.
			createOrAddOnToPlotGroup(townBlock, town, player, plotGroupName);
			resident.setPlotGroupName(plotGroupName);
			TownyMessaging.sendMsg(player, Translatable.of("msg_plot_was_put_into_group_x", townBlock.getX(), townBlock.getZ(), townBlock.getPlotObjectGroup().getName()));
		}
	}

	private void createOrAddOnToPlotGroup(TownBlock townBlock, Town town, Player player, String plotGroupName) throws TownyException {
		PlotGroup newGroup;
		
		// Don't add the group to the town data if it's already there.
		if (town.hasPlotGroupName(plotGroupName)) {
			newGroup = town.getPlotObjectGroupFromName(plotGroupName);
			
			BukkitTools.ifCancelledThenThrow(new PlotGroupAddEvent(newGroup, townBlock, player));
			
			townBlock.setPermissions(newGroup.getPermissions().toString());
			townBlock.setChanged(!townBlock.getPermissions().toString().equals(town.getPermissions().toString()));
			townBlock.setMaxTownMembershipDays(newGroup.getMaxTownMembershipDays());
			townBlock.setMinTownMembershipDays(newGroup.getMinTownMembershipDays());
		} else {
			// This is a brand new PlotGroup, register it.
			newGroup = new PlotGroup(UUID.randomUUID(), plotGroupName, town);
			newGroup.setPermissions(townBlock.getPermissions());
			newGroup.setTrustedResidents(townBlock.getTrustedResidents());
			newGroup.setPermissionOverrides(townBlock.getPermissionOverrides());
			
			BukkitTools.ifCancelledThenThrow(new PlotGroupCreatedEvent(newGroup, townBlock, player));

			TownyUniverse.getInstance().registerGroup(newGroup);
		}

		// Add group to townblock, this also adds the townblock to the group.
		townBlock.setPlotObjectGroup(newGroup);

		// Check if a plot price is available.
		if (townBlock.getPlotPrice() > 0)
			newGroup.addPlotPrice(townBlock.getPlotPrice());

		// Add the plot group to the town set.
		town.addPlotGroup(newGroup);

		// Save changes.
		newGroup.save();
		townBlock.save(); 
	}

	public void parsePlotGroupDelete(TownBlock townBlock, Player player, Town town) throws TownyException {
		checkPermOrThrow(player, PermissionNodes.TOWNY_COMMAND_PLOT_GROUP_DELETE.getNode());

		PlotGroup group = catchMissingPlotGroup(townBlock);

		Confirmation.runOnAccept(()-> {
			String name = group.getName();
			if (!BukkitTools.isEventCancelled(new PlotGroupDeletedEvent(group, player, PlotGroupDeletedEvent.Cause.DELETED))) {
				town.removePlotGroup(group);
				TownyUniverse.getInstance().getDataSource().removePlotGroup(group);
				TownyMessaging.sendMsg(player, Translatable.of("msg_plotgroup_deleted", name));
			}
		}).sendTo(player);
	}

	public void parsePlotGroupForSale(String[] split, Resident resident, TownBlock townBlock, Player player, Town town) throws TownyException {
		checkPermOrThrow(player, PermissionNodes.TOWNY_COMMAND_PLOT_GROUP_FORSALE.getNode());

		// This means the player wants to fs the plot group they are in.
		if (split.length < 2 && TownyEconomyHandler.isActive())
			throw new TownyException(Translatable.of("msg_err_plot_group_specify_price"));

		PlotGroup group = catchMissingPlotGroup(townBlock);

		double price = split.length >= 2 ? MoneyUtil.getMoneyAboveZeroOrThrow(split[1]) : 0;
		group.setPrice(Math.min(price, TownySettings.getMaxPlotPrice()));
		
		// Save
		group.save();

		Translatable message = TownyEconomyHandler.isActive()
			? Translatable.of("msg_player_put_group_up_for_sale_amount", player.getName(), group.getName(), prettyMoney(group.getPrice()))
			: Translatable.of("msg_player_put_group_up_for_sale", player.getName(), group.getName());
		
		TownyMessaging.sendPrefixedTownMessage(town, message);
		
		if (!resident.hasTown() || resident.getTownOrNull() != town)
			TownyMessaging.sendMsg(player, message);
	}

	public void parsePlotGroupNotForSale(Resident resident, TownBlock townBlock, Player player, Town town) throws TownyException {
		checkPermOrThrow(player, PermissionNodes.TOWNY_COMMAND_PLOT_GROUP_NOTFORSALE.getNode());

		// This means the player wants to nfs the plot group they are in.
		PlotGroup group = catchMissingPlotGroup(townBlock);

		group.setPrice(-1);

		// Save
		group.save();

		TownyMessaging.sendPrefixedTownMessage(town, Translatable.of("msg_player_made_group_not_for_sale", player.getName(), group.getName()));
		
		if (!resident.hasTown() || resident.getTownOrNull() != town)
			TownyMessaging.sendMsg(player, Translatable.of("msg_player_made_group_not_for_sale", player.getName(), group.getName()));
	}

	public void parsePlotGroupPerm(String[] split, Resident resident, TownBlock townBlock, Player player) throws TownyException {
		checkPermOrThrow(player, PermissionNodes.TOWNY_COMMAND_PLOT_GROUP_PERM.getNode());

		if (split.length < 2) {
			HelpMenu.PLOT_GROUP_PERM_HELP.send(player);
			return;
		}

		PlotGroup group = catchMissingPlotGroup(townBlock);

		if (split[1].equalsIgnoreCase("gui")) {
			PermissionGUIUtil.openPermissionGUI(resident, townBlock);
			return;
		} 
		if (split.length < 3) {
			HelpMenu.PLOT_GROUP_PERM_HELP.send(player);
			return;
		}
		
		Resident overrideResident = TownyAPI.getInstance().getResident(split[2]);
		if (overrideResident == null || overrideResident.isNPC())
			throw new TownyException(Translatable.of("msg_err_not_registered_1", split[2]));

		if (split[1].equalsIgnoreCase("add")) {
			if (group.getPermissionOverrides() != null && group.getPermissionOverrides().containsKey(overrideResident))
				throw new TownyException(Translatable.of("msg_overrides_already_set", overrideResident.getName(), Translatable.of("plotgroup_sing")));

			group.putPermissionOverride(overrideResident, new PermissionData(PermissionGUIUtil.getDefaultTypes(), player.getName()));

			TownyMessaging.sendMsg(player, Translatable.of("msg_overrides_added", overrideResident.getName()));
		} else if (split[1].equalsIgnoreCase("remove")) {
			if (group.getPermissionOverrides() != null && !group.getPermissionOverrides().containsKey(overrideResident))
				throw new TownyException(Translatable.of("msg_no_overrides_set", overrideResident.getName(), Translatable.of("plotgroup_sing")));

			group.removePermissionOverride(overrideResident);

			TownyMessaging.sendMsg(player, Translatable.of("msg_overrides_removed", overrideResident.getName()));
		} else {
			throw new TownyException(Translatable.of("msg_err_invalid_property", split[1]));
		}
	}

	public void parsePlotGroupRemove(TownBlock townBlock, Player player, Town town) throws TownyException {
		checkPermOrThrow(player, PermissionNodes.TOWNY_COMMAND_PLOT_GROUP_REMOVE.getNode());

		PlotGroup group = catchMissingPlotGroup(townBlock);
		String name = group.getName();
		// Remove the plot from the group.
		group.removeTownBlock(townBlock);

		// Detach group from townblock.
		townBlock.removePlotObjectGroup();

		// Save
		townBlock.save();
		TownyMessaging.sendMsg(player, Translatable.of("msg_plot_was_removed_from_group_x", townBlock.getX(), townBlock.getZ(), name));
		
		if (group.getTownBlocks().isEmpty()) {
			new PlotGroupDeletedEvent(group, player, PlotGroupDeletedEvent.Cause.NO_TOWNBLOCKS).callEvent();
			town.removePlotGroup(group);
			TownyUniverse.getInstance().getDataSource().removePlotGroup(group);
			TownyMessaging.sendMsg(player, Translatable.of("msg_plotgroup_empty_deleted", name));
		}
	}

	public void parsePlotGroupRename(String[] split, TownBlock townBlock, Player player) throws TownyException, AlreadyRegisteredException {
		checkPermOrThrow(player, PermissionNodes.TOWNY_COMMAND_PLOT_GROUP_RENAME.getNode());

		if (split.length == 1)
			throw new TownyException(Translatable.of("msg_err_plot_group_name_required"));

		PlotGroup group = catchMissingPlotGroup(townBlock);
		String newName = split[1];
		NameValidation.checkAndFilterPlotGroupNameOrThrow(newName);
		String oldName = group.getName();
		// Change name;
		TownyUniverse.getInstance().getDataSource().renameGroup(group, newName);
		TownyMessaging.sendMsg(player, Translatable.of("msg_plot_renamed_from_x_to_y", oldName, newName));
	}

	public void parsePlotGroupSet(String[] split, Resident resident, TownBlock townBlock, Player player, Town town) throws TownyException {
		checkPermOrThrow(player, PermissionNodes.TOWNY_COMMAND_PLOT_GROUP_SET.getNode());

		// Check if group is present.
		PlotGroup group = catchMissingPlotGroup(townBlock);

		TownBlockOwner townBlockOwner = townBlock.getTownBlockOwner();
		if (split.length < 2) {
			showPlotGroupHelp(player, townBlockOwner);
			return;
		}

		split = StringMgmt.remFirstArg(split);
		switch(split[0].toLowerCase(Locale.ROOT)) {
		case "perm" -> parsePlotGroupSetPerm(split, townBlock, player, town, group, townBlockOwner);
		case "maxjoindays" -> parsePlotGroupSetMaxJoinDays(player, resident, townBlock, group, StringMgmt.remFirstArg(split));
		case "minjoindays" -> parsePlotGroupSetMinJoinDays(player, resident, townBlock, group, StringMgmt.remFirstArg(split));

		/*
		 * After all other set commands are tested for we attempt to set the townblocktype.
		 */
		default -> parsePlotGroupSetTownBlockType(split, resident, townBlock, group, player, town);
			
		}
	}

	private void showPlotGroupHelp(Player player, TownBlockOwner townBlockOwner) {
		HelpMenu.PLOT_GROUP_SET.send(player);
		if (townBlockOwner instanceof Town)
			TownyMessaging.sendMessage(player, ChatTools.formatCommand("Level", "[resident/nation/ally/outsider]", "", ""));
		if (townBlockOwner instanceof Resident)
			TownyMessaging.sendMessage(player, ChatTools.formatCommand("Level", "[friend/town/ally/outsider]", "", ""));
		TownyMessaging.sendMessage(player, ChatTools.formatCommand("Type", "[build/destroy/switch/itemuse]", "", ""));
		TownyMessaging.sendMessage(player, ChatTools.formatCommand("/plot group set", "perm", "[on/off]", "Toggle all permissions"));
		TownyMessaging.sendMessage(player, ChatTools.formatCommand("/plot group set", "perm", "[level/type] [on/off]", ""));
		TownyMessaging.sendMessage(player, ChatTools.formatCommand("/plot group set", "perm", "[level] [type] [on/off]", ""));
		TownyMessaging.sendMessage(player, ChatTools.formatCommand("/plot group set", "perm", "reset", ""));
		TownyMessaging.sendMessage(player, ChatTools.formatCommand("Eg", "/plot group set perm", "friend build on", ""));
		TownyMessaging.sendMessage(player, ChatTools.formatCommand("/plot group set", "[townblocktype]", "", "Farm, Wilds, Bank, Embassy, etc."));
	}

	public void parsePlotGroupSetPerm(String[] args, TownBlock townBlock, Player player, Town town, PlotGroup plotGroup, TownBlockOwner townBlockOwner) {
		// Set plot level permissions (if the plot owner) or
		// Mayor/Assistant of the town.
		Runnable permHandler = () -> {

			// setTownBlockPermissions returns a towny permission change object
			TownyPermissionChange permChange = null;
			try {
				permChange = PlotCommand.setTownBlockPermissions(player, townBlockOwner, townBlock, StringMgmt.remFirstArg(args));
			} catch (CancelledEventException e) {
				TownyMessaging.sendErrorMsg(player, e.getMessage(player));
				return;
			}

			// Fire a cancellable event over each townblock to see if this should be allowed.
			for (TownBlock tb : plotGroup.getTownBlocks()) {
				try {
					BukkitTools.ifCancelledThenThrow(new TownBlockPermissionChangeEvent(tb, permChange));
				} catch (CancelledEventException e) {
					TownyMessaging.sendErrorMsg(player, e.getMessage(player));
					return;
				}
			}

			// If the perm change object is not null
			if (permChange != null) {
				plotGroup.getPermissions().change(permChange);
				
				plotGroup.getTownBlocks().stream()
					.forEach(tb -> {
						tb.setPermissions(plotGroup.getPermissions().toString());
						tb.setChanged(!tb.getPermissions().toString().equals(town.getPermissions().toString()));
						tb.save();
						// Change settings event
						BukkitTools.fireEvent(new TownBlockSettingsChangedEvent(tb));
					});

				plugin.resetCache();

				Translator translator = Translator.locale(player);
				TownyPermission perm = plotGroup.getPermissions();
				TownyMessaging.sendMessage(player, translator.of("msg_set_perms"));
				TownyMessaging.sendMessage(player, Colors.Green + translator.of("status_perm") + " " + ((townBlockOwner instanceof Resident) ? perm.getColourString().replace("n", "t") : perm.getColourString().replace("f", "r")));
				TownyMessaging.sendMessage(player, Colors.Green + translator.of("status_pvp") + " " + (perm.pvp ? translator.of("status_on") : translator.of("status_off")) + 
													Colors.Green + translator.of("explosions") + " " + (perm.explosion ? translator.of("status_on") : translator.of("status_off")) + 
													Colors.Green + translator.of("firespread") + " " + (perm.fire ? translator.of("status_on") : translator.of("status_off")) +  
													Colors.Green + translator.of("mobspawns") + " " + (perm.mobs ? translator.of("status_on") : translator.of("status_off")));
			}
		};

		// Create confirmation.
		Confirmation.runOnAccept(permHandler)
			.setTitle(Translatable.of("msg_plot_group_set_perm_confirmation", plotGroup.getTownBlocks().size()).append(" ").append(Translatable.of("are_you_sure_you_want_to_continue")))
			.sendTo(player);
	}

	private void parsePlotGroupSetMinJoinDays(Player player, Resident resident, TownBlock townBlock, PlotGroup plotGroup, String[] args) throws TownyException {
		checkPermOrThrow(player, PermissionNodes.TOWNY_COMMAND_PLOT_ASMAYOR.getNode());
		TownyAPI.getInstance().testPlotOwnerOrThrow(resident, townBlock); // Test we are allowed to work on this plot

		if (args.length == 0 || args[0].equalsIgnoreCase("?")) {
			showPlotGroupHelp(player, townBlock.getTownBlockOwner());
			return;
		}
		if (args[0].equalsIgnoreCase("clear")) {
			plotGroup.getTownBlocks().forEach(tb -> {
				tb.setMinTownMembershipDays(-1);
				tb.save();
			});
			TownyMessaging.sendMsg(player, Translatable.of("msg_townblock_min_join_days_removed"));
			return;
		}

		int days = MathUtil.getPositiveIntOrThrow(args[0]);
		if (days == 0)
			throw new TownyException(Translatable.of("msg_err_days_must_be_greater_than_0"));
		plotGroup.getTownBlocks().forEach(tb -> {
			tb.setMinTownMembershipDays(days);
			tb.save();
		});
		TownyMessaging.sendMsg(player, Translatable.of("msg_townblock_min_join_days_set_to", townBlock.getMinTownMembershipDays()));
	}

	private void parsePlotGroupSetMaxJoinDays(Player player, Resident resident, TownBlock townBlock, PlotGroup plotGroup, String[] args) throws TownyException {
		checkPermOrThrow(player, PermissionNodes.TOWNY_COMMAND_PLOT_ASMAYOR.getNode());
		TownyAPI.getInstance().testPlotOwnerOrThrow(resident, townBlock); // Test we are allowed to work on this plot

		if (args.length == 0 || args[0].equalsIgnoreCase("?")) {
			showPlotGroupHelp(player, townBlock.getTownBlockOwner());
			return;
		}
		if (args[0].equalsIgnoreCase("clear")) {
			plotGroup.getTownBlocks().forEach(tb -> {
				tb.setMinTownMembershipDays(-1);
				tb.save();
			});
			TownyMessaging.sendMsg(player, Translatable.of("msg_townblock_max_join_days_removed"));
			return;
		}

		int days = MathUtil.getPositiveIntOrThrow(args[0]);
		if (days == 0)
			throw new TownyException(Translatable.of("msg_err_days_must_be_greater_than_0"));
		plotGroup.getTownBlocks().forEach(tb -> {
			tb.setMinTownMembershipDays(days);
			tb.save();
		});
		TownyMessaging.sendMsg(player, Translatable.of("msg_townblock_max_join_days_set_to", townBlock.getMaxTownMembershipDays()));
	}

	public void parsePlotGroupSetTownBlockType(String[] split, Resident resident, TownBlock townBlock, PlotGroup group, Player player, Town town) throws TownyException {

		String plotTypeName = split[0];

		// Stop setting plot groups to Jail plot, because that would set a spawn point for each plot in the location of the player.
		if (plotTypeName.equalsIgnoreCase("jail"))
			throw new TownyException(Translatable.of("msg_err_cannot_set_group_to_jail"));

		// Handle type being reset
		if (plotTypeName.equalsIgnoreCase("reset"))
			plotTypeName = "default";

		TownBlockType type = TownBlockTypeHandler.getType(plotTypeName);

		if (type == null)
			throw new TownyException(Translatable.of("msg_err_not_block_type"));

		Collection<TownBlock> plotGroupTownBlocks = group.getTownBlocks();
		for (TownBlock tb : plotGroupTownBlocks) {

			// Test we are allowed to work on this plot
			TownyAPI.getInstance().testPlotOwnerOrThrow(resident, tb); // ignore the return as we

			if (TownBlockType.ARENA.equals(type) && TownySettings.getOutsidersPreventPVPToggle()) {
				for (Player target : Bukkit.getOnlinePlayers()) {
					if (!town.hasResident(target) && !player.getName().equals(target.getName()) && tb.getWorldCoord().equals(WorldCoord.parseWorldCoord(target)))
						throw new TownyException(Translatable.of("msg_cant_toggle_pvp_outsider_in_plot"));
				}
			}

			// Allow for PlotPreChangeTypeEvent to trigger
			// If any one of the townblocks is not allowed to be set, cancel setting all of them.
			BukkitTools.ifCancelledThenThrow(new PlotPreChangeTypeEvent(type, tb, resident));
		}

		double cost = type.getCost() * plotGroupTownBlocks.size();
		// Test if we can pay first to throw an exception.
		if (cost > 0 && TownyEconomyHandler.isActive() && !resident.getAccount().canPayFromHoldings(cost))
			throw new TownyException(Translatable.of("msg_err_cannot_afford_plot_set_type_cost", type, prettyMoney(cost)));

		// Handle payment via a confirmation to avoid suprise costs.
		if (cost > 0 && TownyEconomyHandler.isActive()) {
			Confirmation.runOnAccept(() -> {
				if (townBlock.getPlotObjectGroup() == null)
					return;

				TownyMessaging.sendMsg(resident, Translatable.of("msg_plot_set_cost", prettyMoney(cost), type));

				for (TownBlock tb : townBlock.getPlotObjectGroup().getTownBlocks()) {
					try {
						TownBlockType oldType = tb.getType();
						tb.setType(type, resident);
						BukkitTools.fireEvent(new PlayerChangePlotTypeEvent(type, oldType, tb, player));
					} catch (TownyException ignored) {
						// Cannot be set to jail type as a group.
					}
				}
				TownyMessaging.sendMsg(player, Translatable.of("msg_set_group_type_to_x", type));
			})
				.setCost(new ConfirmationTransaction(() -> cost, resident, String.format("Plot group (%s) set to %s",  plotGroupTownBlocks.size(), type),
						Translatable.of("msg_err_cannot_afford_plot_set_type_cost", type, prettyMoney(cost))))
				.setTitle(Translatable.of("msg_confirm_purchase", prettyMoney(cost)))
				.sendTo(BukkitTools.getPlayerExact(resident.getName()));
		// No cost or economy so no confirmation.
		} else {
			for (TownBlock tb : plotGroupTownBlocks) {
				TownBlockType oldType = tb.getType();
				tb.setType(type, resident);
				BukkitTools.fireEvent(new PlayerChangePlotTypeEvent(type, oldType, tb, player));
			}
			TownyMessaging.sendMsg(player, Translatable.of("msg_set_group_type_to_x", plotTypeName));
		}
	}

	public void parsePlotGroupToggle(String[] split, TownBlock townBlock, Player player, Resident resident) throws TownyException {
		checkPermOrThrow(player, PermissionNodes.TOWNY_COMMAND_PLOT_GROUP_TOGGLE.getNode());
		PlotGroup group = catchMissingPlotGroup(townBlock);

		// Create confirmation.
		Confirmation.runOnAccept(() -> {
			// Perform the toggle.
			try {
				new PlotCommand(Towny.getPlugin()).plotGroupToggle(player, resident, group, StringMgmt.remArgs(split, 1));
			} catch (TownyException e) {
				TownyMessaging.sendErrorMsg(player, e.getMessage(player));
			}
		})
		.setTitle(Translatable.of("msg_plot_group_toggle_confirmation", group.getTownBlocks().size()).append(" ").append(Translatable.of("are_you_sure_you_want_to_continue")))
		.sendTo(player);
	}

	/**
	 * Toggle the plot group flags for pvp/explosion/fire/mobs (if town/world
	 * permissions allow)
	 *
	 * @param player - Player.
	 * @param resident Resident.
	 * @param plotGroup - PlotObjectGroup object.
	 * @param split  - Current command arguments.
	 */
	public void plotGroupToggle(Player player, Resident resident, PlotGroup plotGroup, String[] split) throws TownyException {

		if (split.length == 0 || split[0].equalsIgnoreCase("?")) {
			HelpMenu.PLOT_GROUP_TOGGLE.send(player);
			return;
		}
		// We need to keep an ending string to show the message only after the transaction is over,
		// to prevent chat log spam.
		Translatable endingMessage = null;

		Optional<Boolean> choice = Optional.empty();
		if (split.length == 2)
			choice = BaseCommand.parseToggleChoice(split[1]);

		// Check permissions once before looping over the group's plots.
		switch(split[0].toLowerCase(Locale.ROOT)) {
		case "pvp":
			checkPermOrThrow(player, PermissionNodes.TOWNY_COMMAND_PLOT_TOGGLE_PVP.getNode());
			break;
		case "explosion":
			checkPermOrThrow(player, PermissionNodes.TOWNY_COMMAND_PLOT_TOGGLE_EXPLOSION.getNode());
			break;
		case "fire":
			checkPermOrThrow(player, PermissionNodes.TOWNY_COMMAND_PLOT_TOGGLE_FIRE.getNode());
			break;
		case "mobs":
			checkPermOrThrow(player, PermissionNodes.TOWNY_COMMAND_PLOT_TOGGLE_MOBS.getNode());
			break;
		case "taxed":
			checkPermOrThrow(player, PermissionNodes.TOWNY_COMMAND_PLOT_ASMAYOR.getNode());
			break;
		}

		for (TownBlock groupBlock : plotGroup.getTownBlocks()) {
			Town town = groupBlock.getTownOrNull();
			if (town == null) continue;

			try {
				switch(split[0].toLowerCase(Locale.ROOT)) {
				case "pvp":
					tryToggleTownBlockPVP(player, resident, groupBlock, split, town, choice);
					endingMessage = Translatable.of("msg_changed_pvp", Translatable.of("msg_the_plot_group"), groupBlock.getPermissions().pvp ? Translatable.of("enabled") : Translatable.of("disabled"));
					break;
				case "explosion":
					tryToggleTownBlockExplosion(player, groupBlock, split, choice);
					endingMessage = Translatable.of("msg_changed_expl", Translatable.of("msg_the_plot_group"), groupBlock.getPermissions().explosion ? Translatable.of("enabled") : Translatable.of("disabled"));
					break;
				case "fire":
					tryToggleTownBlockFire(player, groupBlock, split, choice);
					endingMessage = Translatable.of("msg_changed_fire", Translatable.of("msg_the_plot_group"), groupBlock.getPermissions().fire ? Translatable.of("enabled") : Translatable.of("disabled"));
					break;
				case "mobs":
					tryToggleTownBlockMobs(player, groupBlock, split, choice);
					endingMessage = Translatable.of("msg_changed_mobs", Translatable.of("msg_the_plot_group"), groupBlock.getPermissions().mobs ? Translatable.of("enabled") : Translatable.of("disabled"));
					break;
				case "taxed":
					 tryToggleTownBlockTaxed(player, groupBlock, split, choice);
					 endingMessage = Translatable.of("msg_changed_plotgroup_taxed", groupBlock.isTaxed() ? Translatable.of("enabled") : Translatable.of("disabled"));
					 break;
				default:
					TownyMessaging.sendErrorMsg(player, Translatable.of("msg_err_invalid_property", "plot"));
					return;
				}
			} catch (TownyException e) {
				// Catch the error message and continue through the PlotGroup's townblocks.
				TownyMessaging.sendErrorMsg(player, e.getMessage(player));
				continue;
			}

			groupBlock.setChanged(true);

			//Change settings event
			BukkitTools.fireEvent(new TownBlockSettingsChangedEvent(groupBlock));

			// Save
			groupBlock.save();
		}

		// Finally send the message.
		if (endingMessage != null)
			TownyMessaging.sendMsg(player, endingMessage);
	}

	public void parsePlotGroupTrust(String[] split, TownBlock townBlock, Player player) throws TownyException {
		checkPermOrThrow(player, PermissionNodes.TOWNY_COMMAND_PLOT_GROUP_TRUST.getNode());

		if (split.length < 3) {
			HelpMenu.PLOT_GROUP_TRUST_HELP.send(player);
			return;
		}

		PlotGroup group = catchMissingPlotGroup(townBlock);

		Resident trustedResident = TownyAPI.getInstance().getResident(split[2]);
		if (trustedResident == null || trustedResident.isNPC())
			throw new TownyException(Translatable.of("msg_err_not_registered_1", split[2]));

		if (split[1].equalsIgnoreCase("add")) {
			if (group.hasTrustedResident(trustedResident))
				throw new TownyException(Translatable.of("msg_already_trusted", trustedResident.getName(), Translatable.of("plotgroup_sing")));

			if (townBlock.getTownOrNull().hasOutlaw(trustedResident))
				throw new TownyException(Translatable.of("msg_err_you_cannot_add_trust_on_outlaw"));

			if (trustedResident.hasNation() && townBlock.getTownOrNull().hasNation() && townBlock.getTownOrNull().getNationOrNull().hasEnemy(trustedResident.getNationOrNull()))
				throw new TownyException(Translatable.of("msg_err_you_cannot_add_trust_on_enemy"));

			BukkitTools.ifCancelledThenThrow(new PlotTrustAddEvent(new ArrayList<>(group.getTownBlocks()), trustedResident, player));

			group.addTrustedResident(trustedResident);
			plugin.deleteCache(trustedResident);

			TownyMessaging.sendMsg(player, Translatable.of("msg_trusted_added", trustedResident.getName(), Translatable.of("plotgroup_sing")));
			if (BukkitTools.isOnline(trustedResident.getName()) && !trustedResident.getName().equals(player.getName()))
				TownyMessaging.sendMsg(trustedResident, Translatable.of("msg_trusted_added_2", player.getName(), Translatable.of("plotgroup_sing"), group.getName()));

			return;
		} else if (split[1].equalsIgnoreCase("remove")) {
			if (!group.hasTrustedResident(trustedResident))
				throw new TownyException(Translatable.of("msg_not_trusted", trustedResident.getName(), Translatable.of("plotgroup_sing")));

			BukkitTools.ifCancelledThenThrow(new PlotTrustRemoveEvent(new ArrayList<>(group.getTownBlocks()), trustedResident, player));

			group.removeTrustedResident(trustedResident);
			plugin.deleteCache(trustedResident);

			TownyMessaging.sendMsg(player, Translatable.of("msg_trusted_removed", trustedResident.getName(), Translatable.of("plotgroup_sing")));
			if (BukkitTools.isOnline(trustedResident.getName()) && !trustedResident.getName().equals(player.getName()))
				TownyMessaging.sendMsg(trustedResident, Translatable.of("msg_trusted_removed_2", player.getName(), Translatable.of("plotgroup_sing"), group.getName()));

			return;
		} else {
			TownyMessaging.sendErrorMsg(player, Translatable.of("msg_err_invalid_property", split[1]));
		}
	}

	public static void parsePlotTrustCommand(Player player, String[] args) throws TownyException {
		checkPermOrThrow(player, PermissionNodes.TOWNY_COMMAND_PLOT_TRUST.getNode());

		if (args.length < 2) {
			HelpMenu.PLOT_TRUST_HELP.send(player);
			return;
		}

		TownBlock townBlock = WorldCoord.parseWorldCoord(player).getTownBlockOrNull();
		if (townBlock == null)
			throw new TownyException(Translatable.of("msg_not_claimed_1"));

		// Test we are allowed to work on this plot
		// If this fails it will trigger a TownyException.
		TownyAPI.getInstance().testPlotOwnerOrThrow(getResidentOrThrow(player), townBlock);

		catchPlotGroup(townBlock, "/plot group trust");

		Resident resident = TownyAPI.getInstance().getResident(args[1]);
		if (resident == null || resident.isNPC())
			throw new TownyException(Translatable.of("msg_err_not_registered_1", args[1]));

		if (args[0].equalsIgnoreCase("add")) {
			if (townBlock.hasTrustedResident(resident))
				throw new TownyException(Translatable.of("msg_already_trusted", resident.getName(), Translatable.of("townblock")));

			if (townBlock.getTownOrNull().hasOutlaw(resident))
				throw new TownyException(Translatable.of("msg_err_you_cannot_add_trust_on_outlaw"));

			if (resident.hasNation() && townBlock.getTownOrNull().hasNation() && townBlock.getTownOrNull().getNationOrNull().hasEnemy(resident.getNationOrNull()))
				throw new TownyException(Translatable.of("msg_err_you_cannot_add_trust_on_enemy"));

			BukkitTools.ifCancelledThenThrow(new PlotTrustAddEvent(townBlock, resident, player));

			townBlock.addTrustedResident(resident);
			plugin.deleteCache(resident);

			TownyMessaging.sendMsg(player, Translatable.of("msg_trusted_added", resident.getName(), Translatable.of("townblock")));
			if (BukkitTools.isOnline(resident.getName()) && !resident.getName().equals(player.getName()))
				TownyMessaging.sendMsg(resident, Translatable.of("msg_trusted_added_2", player.getName(), Translatable.of("townblock"), townBlock.getWorldCoord().getCoord().toString()));
		} else if (args[0].equalsIgnoreCase("remove")) {
			if (!townBlock.hasTrustedResident(resident))
				throw new TownyException(Translatable.of("msg_not_trusted", resident.getName(), Translatable.of("townblock")));

			BukkitTools.ifCancelledThenThrow(new PlotTrustRemoveEvent(townBlock, resident, player));

			townBlock.removeTrustedResident(resident);
			plugin.deleteCache(resident);

			TownyMessaging.sendMsg(player, Translatable.of("msg_trusted_removed", resident.getName(), Translatable.of("townblock")));
			if (BukkitTools.isOnline(resident.getName()) && !resident.getName().equals(player.getName()))
				TownyMessaging.sendMsg(resident, Translatable.of("msg_trusted_removed_2", player.getName(), Translatable.of("townblock"), townBlock.getWorldCoord().getCoord().toString()));
		} else {
			throw new TownyException(Translatable.of("msg_err_invalid_property", args[0]));
		}
		
		townBlock.save();
	}
	
	private void continuePlotClaimProcess(List<WorldCoord> selection, Resident resident, Player player) throws TownyException {
		double cost = 0;

		// Remove any plots Not for sale (if not the mayor) and
		// tally up costs.
		for (WorldCoord worldCoord : new ArrayList<>(selection)) {
			if (!worldCoord.hasTownBlock()) 
				selection.remove(worldCoord);
			else { 
				TownBlock tb = worldCoord.getTownBlockOrNull();
				if (tb == null) {
					selection.remove(worldCoord);
					continue;
				}
				double price = tb.getPlotPrice();
				
				if (tb.hasPlotObjectGroup()) {
					// This block is part of a group, special tasks need to be done.
					PlotGroup group = tb.getPlotObjectGroup();
					
					if (TownyEconomyHandler.isActive() && (!resident.getAccount().canPayFromHoldings(group.getPrice())))
						throw new TownyException(Translatable.of("msg_no_funds_claim_plot_group", group.getTownBlocks().size(), prettyMoney(group.getPrice())));

					tb.testTownMembershipAgePreventsThisClaimOrThrow(resident);

					// Add the confirmation for claiming a plot group.
					Confirmation.runOnAccept(() -> {
						ArrayList<WorldCoord> coords = new ArrayList<>();

						// Get worldcoords from plot group.
						group.getTownBlocks().forEach((tblock) -> coords.add(tblock.getWorldCoord()));

						// Execute the plot claim.
						plugin.getScheduler().runAsync(new PlotClaim(plugin, player, resident, coords, true, false, true));
					})
					.setTitle(Translatable.of("msg_plot_group_claim_confirmation", group.getTownBlocks().size()).append(" ").append(prettyMoney(group.getPrice())).append(". ").append(Translatable.of("are_you_sure_you_want_to_continue")))
					.sendTo(player);
					
					return;
				}
				
				// Check if a plot has a price.
				if (price > -1)
					cost += tb.getPlotPrice();
				else {
					if (!tb.getTownOrNull().isMayor(resident)) 
						selection.remove(worldCoord);
				}
			}
		}

		int maxPlots = TownySettings.getMaxResidentPlots(resident);
		int extraPlots = TownySettings.getMaxResidentExtraPlots(resident);
		
		//Infinite plots
		if (maxPlots != -1) {
			maxPlots = maxPlots + extraPlots;
		}
		
		if (maxPlots >= 0 && resident.getTownBlocks().size() + selection.size() > maxPlots)
			throw new TownyException(Translatable.of("msg_max_plot_own", maxPlots));

		if (TownyEconomyHandler.isActive() && (!resident.getAccount().canPayFromHoldings(cost)))
			throw new TownyException(Translatable.of("msg_no_funds_claim_plot", prettyMoney(cost)));

		if (cost != 0) {
			Confirmation.runOnAcceptAsync(
				// Start the claim task
				new PlotClaim(plugin, player, resident, selection, true, false, false)
			)
			.setTitle(Translatable.of("msg_confirm_purchase", prettyMoney(cost)))
			.sendTo(player);
		} else {
			// Start the claim task
			plugin.getScheduler().runAsync(new PlotClaim(plugin, player, resident, selection, true, false, false));
		}
	}
		
	public void parsePlotPermCommand(Player player, String[] args) throws TownyException {
		if (args.length == 0) {
			checkPermOrThrow(player, PermissionNodes.TOWNY_COMMAND_PLOT_PERM_INFO.getNode());
			sendPlotInfo(player, args);
			return;
		}

		if (args[0].equalsIgnoreCase("hud")) {
			checkPermOrThrow(player, PermissionNodes.TOWNY_COMMAND_PLOT_PERM_HUD.getNode());
			HUDManager.togglePermHUD(player);
			return;
		}

		// All other subcommands require the player to be in a claimed area
		TownBlock townBlock = TownyAPI.getInstance().getTownBlock(player);
		if (townBlock == null) {
			TownyMessaging.sendErrorMsg(player, Translatable.of("msg_not_claimed_1"));
			return;
		}

		if (args[0].equalsIgnoreCase("gui")) {
			checkPermOrThrow(player, PermissionNodes.TOWNY_COMMAND_PLOT_PERM_GUI.getNode());
			PermissionGUIUtil.openPermissionGUI(getResidentOrThrow(player), townBlock);
			return;
		} 

		// Make sure that we have enough arguments and that the first arg is either remove or add.
		if (args.length < 2 || !(args[0].equalsIgnoreCase("remove") || args[0].equalsIgnoreCase("add"))) {
			HelpMenu.PLOT_PERM_HELP.send(player);
			return;
		}

		// Test that the command sender counts as a plot owner.
		TownyAPI.getInstance().testPlotOwnerOrThrow(getResidentOrThrow(player), townBlock);

		// Don't allow this command to be run on plot groups.
		catchPlotGroup(townBlock, "/plot group perm " + args[0].toLowerCase(Locale.ROOT));

		// Get the resident which will have perms added or removed on the TownBlock. 
		Resident resident = getResidentOrThrow(args[1]);

		if (args[0].equalsIgnoreCase("remove")) {
			checkPermOrThrow(player, PermissionNodes.TOWNY_COMMAND_PLOT_PERM_REMOVE.getNode());

			if (!townBlock.getPermissionOverrides().containsKey(resident))
				throw new TownyException(Translatable.of("msg_no_overrides_set", resident.getName(), Translatable.of("townblock")));

			townBlock.getPermissionOverrides().remove(resident);
			townBlock.save();

			TownyMessaging.sendMsg(player, Translatable.of("msg_overrides_removed", resident.getName()));
		} else if (args[0].equalsIgnoreCase("add")) {
			checkPermOrThrow(player, PermissionNodes.TOWNY_COMMAND_PLOT_PERM_ADD.getNode());

			if (townBlock.getPermissionOverrides().containsKey(resident))
				throw new TownyException(Translatable.of("msg_overrides_already_set", resident.getName(), Translatable.of("townblock")));

			townBlock.getPermissionOverrides().put(resident, new PermissionData(PermissionGUIUtil.getDefaultTypes(), player.getName()));
			townBlock.save();

			TownyMessaging.sendMsg(player, Translatable.of("msg_overrides_added", resident.getName()));
		}
	}
	
	public void sendPlotInfo(Player player, String[] args) throws NoPermissionException {
		checkPermOrThrow(player, PermissionNodes.TOWNY_COMMAND_PLOT_PERM_INFO.getNode());

		WorldCoord coord = WorldCoord.parseWorldCoord(player);

		try {
			coord = new WorldCoord(player.getWorld(), Integer.parseInt(args[0]), Integer.parseInt(args[1]));
		} catch (NumberFormatException | ArrayIndexOutOfBoundsException ignored) {}

		if (TownyAPI.getInstance().isWilderness(coord))
			TownyMessaging.sendStatusScreen(player, TownyFormatter.getStatus(coord.getTownyWorld(), player));
		else
			TownyMessaging.sendStatusScreen(player, TownyFormatter.getStatus(coord.getTownBlockOrNull(), player));
	}
	
	private List<String> getPlotSetCompletions() {
		List<String> completions = new ArrayList<>(plotSetTabCompletes);

		for (String townBlockType : TownBlockTypeHandler.getTypeNames())
			if (!completions.contains(townBlockType))
				completions.add(townBlockType);

		return completions;
	}

	private static void catchPlotGroup(TownBlock townBlock, String command) throws TownyException {
		// Make sure that the player is only operating on a single plot and not a plotgroup.
		if (townBlock.hasPlotObjectGroup())
			throw new TownyException(Translatable.of("msg_err_plot_belongs_to_group", command));
	}

	private PlotGroup catchMissingPlotGroup(TownBlock townBlock) throws TownyException {
		// Make sure that the player is in a plotgroup.
		if (!townBlock.hasPlotObjectGroup())
			throw new TownyException(Translatable.of("msg_err_plot_not_associated_with_a_group"));
		
		return townBlock.getPlotObjectGroup();
	}
	
	private District catchMissingDistrict(TownBlock townBlock) throws TownyException {
		// Make sure that the player is in a plotgroup.
		if (!townBlock.hasDistrict())
			throw new TownyException(Translatable.of("msg_err_plot_not_associated_with_a_district"));
		
		return townBlock.getDistrict();
	}
}
