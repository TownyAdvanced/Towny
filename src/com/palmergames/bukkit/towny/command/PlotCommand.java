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
import com.palmergames.bukkit.towny.event.PlotClearEvent;
import com.palmergames.bukkit.towny.event.PlotPreChangeTypeEvent;
import com.palmergames.bukkit.towny.event.PlotPreClearEvent;
import com.palmergames.bukkit.towny.event.TownBlockSettingsChangedEvent;
import com.palmergames.bukkit.towny.event.plot.PlotNotForSaleEvent;
import com.palmergames.bukkit.towny.event.plot.PlotSetForSaleEvent;
import com.palmergames.bukkit.towny.event.plot.PlotTrustAddEvent;
import com.palmergames.bukkit.towny.event.plot.PlotTrustRemoveEvent;
import com.palmergames.bukkit.towny.event.plot.toggle.PlotToggleExplosionEvent;
import com.palmergames.bukkit.towny.event.plot.toggle.PlotToggleFireEvent;
import com.palmergames.bukkit.towny.event.plot.toggle.PlotToggleMobsEvent;
import com.palmergames.bukkit.towny.event.plot.toggle.PlotTogglePvpEvent;
import com.palmergames.bukkit.towny.exceptions.AlreadyRegisteredException;
import com.palmergames.bukkit.towny.exceptions.TownyException;
import com.palmergames.bukkit.towny.huds.HUDManager;
import com.palmergames.bukkit.towny.object.Coord;
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
import com.palmergames.bukkit.towny.object.WorldCoord;
import com.palmergames.bukkit.towny.object.jail.Jail;
import com.palmergames.bukkit.towny.permissions.PermissionNodes;
import com.palmergames.bukkit.towny.permissions.TownyPermissionSource;
import com.palmergames.bukkit.towny.regen.TownyRegenAPI;
import com.palmergames.bukkit.towny.tasks.CooldownTimerTask;
import com.palmergames.bukkit.towny.tasks.PlotClaim;
import com.palmergames.bukkit.towny.tasks.CooldownTimerTask.CooldownType;
import com.palmergames.bukkit.towny.utils.AreaSelectionUtil;
import com.palmergames.bukkit.towny.utils.CombatUtil;
import com.palmergames.bukkit.towny.utils.MoneyUtil;
import com.palmergames.bukkit.towny.utils.NameUtil;
import com.palmergames.bukkit.towny.utils.OutpostUtil;
import com.palmergames.bukkit.towny.utils.PermissionGUIUtil;
import com.palmergames.bukkit.util.BukkitTools;
import com.palmergames.bukkit.util.ChatTools;
import com.palmergames.bukkit.util.Colors;
import com.palmergames.bukkit.util.NameValidation;
import com.palmergames.util.StringMgmt;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

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
		"perm",
		"set",
		"toggle",
		"clear",
		"group",
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
		"outpost",
		"name",
		"perm"
	);
	
	private static final List<String> plotRectCircleCompletes = Arrays.asList(
		"rect",
		"circle"
	);
	
	private static final List<String> plotToggleTabCompletes = Arrays.asList(
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
		
		if (sender instanceof Player) {
			if (plugin.isError()) {
				TownyMessaging.sendErrorMsg(sender, "Locked in Safe mode!");
				return false;
			}
			Player player = (Player) sender;
			if (!TownyAPI.getInstance().isTownyWorld(player.getWorld())) {
				TownyMessaging.sendErrorMsg(player, Translatable.of("msg_set_use_towny_off"));
				return false;
			}

			if (args == null)
				HelpMenu.PLOT_HELP.send(player);
			else
				return parsePlotCommand(player, args);

		} else
			// Console
			HelpMenu.PLOT_HELP.send(sender);
		
		return true;
	}
	
	@Override
	public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
		if (sender instanceof Player) {
			switch (args[0].toLowerCase()) {
				case "set":
					if (args.length == 2) {
						return NameUtil.filterByStart(TownyCommandAddonAPI.getTabCompletes(CommandType.PLOT_SET, getPlotSetCompletions()), args[1]);
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
				case "group":
					if (args.length == 2)
						return NameUtil.filterByStart(plotGroupTabCompletes, args[1]);
					
					if (args.length < 2)
						break;
					
					switch (args[1].toLowerCase()) {
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
						default:
							return permTabComplete(StringMgmt.remFirstArg(args));
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
					if (args.length == 3)
						return getTownyStartingWith(args[2], "r");
				default:
					if (args.length == 1)
						return NameUtil.filterByStart(TownyCommandAddonAPI.getTabCompletes(CommandType.PLOT, plotTabCompletes), args[0]);
					else if (args.length > 1 && TownyCommandAddonAPI.hasCommand(CommandType.PLOT, args[0]))
						return NameUtil.filterByStart(TownyCommandAddonAPI.getAddonCommand(CommandType.PLOT, args[0]).getTabCompletion(sender, args), args[args.length-1]);
			}
		}

		return Collections.emptyList();
	}

	public boolean parsePlotCommand(Player player, String[] split) {
		TownyPermissionSource permSource = TownyUniverse.getInstance().getPermissionSource();

		if (split.length == 0 || split[0].equalsIgnoreCase("?")) {
			HelpMenu.PLOT_HELP.send(player);
		} else {

			Resident resident = TownyUniverse.getInstance().getResident(player.getUniqueId());
			
			if (resident == null) {
				TownyMessaging.sendErrorMsg(player, Translatable.of("msg_err_not_registered"));
				return true;
			}
			
			String world = player.getWorld().getName();

			try {
				
				TownBlock townBlock = TownyAPI.getInstance().getTownBlock(player);
				if (townBlock == null && !split[0].equalsIgnoreCase("perm") && !split[0].equalsIgnoreCase("claim"))
					throw new TownyException(Translatable.of("msg_not_claimed_1"));
				
				if (!TownyAPI.getInstance().isWilderness(player.getLocation()) && townBlock.getTownOrNull().isRuined())
					throw new TownyException(Translatable.of("msg_err_cannot_use_command_because_town_ruined"));

				if (split[0].equalsIgnoreCase("claim")) {

					if (!permSource.testPermission(player, PermissionNodes.TOWNY_COMMAND_PLOT_CLAIM.getNode()))
						throw new TownyException(Translatable.of("msg_err_command_disable"));

					List<WorldCoord> selection = AreaSelectionUtil.selectWorldCoordArea(resident, new WorldCoord(world, Coord.parseCoord(player)), StringMgmt.remFirstArg(split), true);
					
					// Fast-fail if this is a single plot and it is already claimed.
					if (selection.size() == 1 && selection.get(0).hasTownBlock() && selection.get(0).getTownBlock().hasResident() && !selection.get(0).getTownBlock().isForSale())
						throw new TownyException(Translatable.of("msg_already_claimed", selection.get(0).getTownBlock().getResidentOrNull()));
					
					// Filter to just plots that are for sale.
					selection = AreaSelectionUtil.filterPlotsForSale(selection);

					// Filter out plots already owned by the player.
					selection = AreaSelectionUtil.filterUnownedBlocks(resident, selection);

					if (selection.size() > 0) {
						
						if (selection.size() + resident.getTownBlocks().size()  > TownySettings.getMaxResidentPlots(resident))
							throw new TownyException(Translatable.of("msg_max_plot_own", TownySettings.getMaxResidentPlots(resident)));
						
						/*
						 * If a resident has no town, the Town is open, and the plot is not an Embassy:
						 * Attempt to add the Resident to the town IF the town is not null, the Town is
						 * not going to exceed the maxResidentsWithoutANation value, and the Town will 
						 * not exceed the maxResidentsPerTown value.
						 */
						if (!resident.hasTown() && townBlock.getTownOrNull().isOpen() && !townBlock.getType().equals(TownBlockType.EMBASSY)) {
							final Town town = townBlock.getTownOrNull();
							if (town == null ||
								(TownySettings.getMaxNumResidentsWithoutNation() > 0 && !town.hasNation() && town.getResidents().size() >= TownySettings.getMaxNumResidentsWithoutNation()) ||
								(TownySettings.getMaxResidentsPerTown() > 0 && town.getResidents().size() >= TownySettings.getMaxResidentsForTown(town)) ||
								town.hasOutlaw(resident) ||
								(resident.isOnline() && !resident.getPlayer().hasPermission(PermissionNodes.TOWNY_COMMAND_TOWN_JOIN.getNode()))) {
								// Town is null (unlikely) or it would have too many residents, we won't be adding 
								// them to the town, continue as per usual (it could be an embassy plot.)
							} else {
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
									.sendTo(player);
								return true;
							}
						}
						continuePlotClaimProcess(selection, resident, player);

					} else {
						TownyMessaging.sendErrorMsg(player, Translatable.of("msg_err_empty_area_selection"));
					}
				} else if (split[0].equalsIgnoreCase("evict")) {

					if (!permSource.testPermission(player, PermissionNodes.TOWNY_COMMAND_PLOT_EVICT.getNode()))
						throw new TownyException(Translatable.of("msg_err_command_disable"));

					if (!townBlock.hasResident()) {
						TownyMessaging.sendErrorMsg(player, Translatable.of("msg_no_one_to_evict"));						

					} else {
						// Test we are allowed to work on this plot
						// If this fails it will trigger a TownyException.
						plotTestOwner(resident, townBlock);

						if (townBlock.hasPlotObjectGroup()) {
							townBlock.getPlotObjectGroup().getTownBlocks().stream().forEach(tb -> {
									tb.setResident(null);            // Remove Resident.
									tb.setPlotPrice(-1);             // Set to NotForSale.
									tb.setType(townBlock.getType()); // Re-set the plot permissions while maintaining plot type. 
									tb.save();                       // Save townblock.
							});
							TownyMessaging.sendMsg(player, Translatable.of("msg_plot_evict_group", townBlock.getPlotObjectGroup().getName()));
							return true;
						}
						
						townBlock.setResident(null); // Remove Resident.
						townBlock.setPlotPrice(-1);  // Set to NotForSale.
						townBlock.setType(townBlock.getType()); // Re-set the plot permissions while maintaining plot type.
						townBlock.save(); // Save townblock.
						
						TownyMessaging.sendMsg(player, Translatable.of("msg_plot_evict"));
					}

				} else if (split[0].equalsIgnoreCase("unclaim")) {

					if (!permSource.testPermission(player, PermissionNodes.TOWNY_COMMAND_PLOT_UNCLAIM.getNode()))
						throw new TownyException(Translatable.of("msg_err_command_disable"));

					if (split.length == 2 && split[1].equalsIgnoreCase("all")) {
						// Start the unclaim task
						new PlotClaim(plugin, player, resident, null, false, false, false).start();

					} else {
						
						List<WorldCoord> selection = AreaSelectionUtil.selectWorldCoordArea(resident, new WorldCoord(world, Coord.parseCoord(player)), StringMgmt.remFirstArg(split));
						selection = AreaSelectionUtil.filterOwnedBlocks(resident, selection);

						if (selection.size() > 0) {

							for (WorldCoord coord : selection) {
								TownBlock block = coord.getTownBlock();

								if (!block.hasPlotObjectGroup()) {
									// Start the unclaim task
									new PlotClaim(plugin, player, resident, selection, false, false, false).start();
									return true;
								}
								
								// Get all the townblocks part of the group.
								final List<WorldCoord> groupSelection = new ArrayList<>();
								block.getPlotObjectGroup().getTownBlocks().forEach((tb) -> {
									groupSelection.add(tb.getWorldCoord());
								});
								
								// Create confirmation.
								Confirmation.runOnAccept(() -> {
									new PlotClaim(Towny.getPlugin(), player, resident, groupSelection, false, false, false).start();
								})
								.setTitle(Translatable.of("msg_plot_group_unclaim_confirmation", block.getPlotObjectGroup().getTownBlocks().size()).append(" " + Translatable.of("are_you_sure_you_want_to_continue")))
								.sendTo(player);
								
								return true;
							}

						} else {
							TownyMessaging.sendErrorMsg(player, Translatable.of("msg_err_empty_area_selection"));
						}
					}

				} else if (split[0].equalsIgnoreCase("notforsale") || split[0].equalsIgnoreCase("nfs")) {

					if (!permSource.testPermission(player, PermissionNodes.TOWNY_COMMAND_PLOT_NOTFORSALE.getNode()))
						throw new TownyException(Translatable.of("msg_err_command_disable"));

					List<WorldCoord> selection = AreaSelectionUtil.selectWorldCoordArea(resident, new WorldCoord(world, Coord.parseCoord(player)), StringMgmt.remFirstArg(split));
					selection = AreaSelectionUtil.filterPlotsForSale(selection);
					
					if (permSource.testPermission(player, PermissionNodes.TOWNY_ADMIN.getNode())) {
						for (WorldCoord worldCoord : selection) {
							if (worldCoord.getTownBlock().hasPlotObjectGroup()) {
								TownyMessaging.sendErrorMsg(player, Translatable.of("msg_err_plot_belongs_to_group_plot_nfs", worldCoord));
								return false;
							}
							setPlotForSale(resident, worldCoord, -1);
						}
						return true;
					}
					
					// The follow test will clean up the initial selection fairly well, the plotTestOwner later on in the setPlotForSale will ultimately stop any funny business.
					if (permSource.testPermission(player, PermissionNodes.TOWNY_COMMAND_PLOT_ASMAYOR.getNode()) && (resident.hasTown() && townBlock.hasTown() && resident.getTown() == townBlock.getTownOrNull())) {
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
						

				} else if (split[0].equalsIgnoreCase("forsale") || split[0].equalsIgnoreCase("fs")) {

					if (!permSource.testPermission(player, PermissionNodes.TOWNY_COMMAND_PLOT_FORSALE.getNode()))
						throw new TownyException(Translatable.of("msg_err_command_disable"));

					WorldCoord pos = new WorldCoord(world, Coord.parseCoord(player));
					Town town = townBlock.getTownOrNull();
					if (town == null)
						throw new TownyException(Translatable.of("msg_err_empty_area_selection"));
					double plotPrice = town.getPlotTypePrice(townBlock.getType());

					if (split.length > 1) {
						/*
						 * This is not a case of '/plot fs' and has a cost and/or an area selection involved.
						 */

						// areaSelectPivot is how Towny handles the 'within' area selection when setting plots for sale.
						// Will return -1 if the word 'within' is not found.
						int areaSelectPivot = AreaSelectionUtil.getAreaSelectPivot(split);
						List<WorldCoord> selection;
						
						if (areaSelectPivot >= 0) { // 'within' has been used in the command, make a selection.
							selection = AreaSelectionUtil.selectWorldCoordArea(resident, new WorldCoord(world, Coord.parseCoord(player)), StringMgmt.subArray(split, areaSelectPivot + 1, split.length));
							
							// The follow test will clean up the initial selection fairly well, the plotTestOwner later on in the setPlotForSale will ultimately stop any funny business.
							if (permSource.testPermission(player, PermissionNodes.TOWNY_COMMAND_PLOT_ASMAYOR.getNode()) && (resident.hasTown() && resident.getTown() == town)) {
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
						if (areaSelectPivot != 1) {
							// command was 'plot fs $'
							plotPrice = MoneyUtil.getMoneyAboveZeroOrThrow(split[1]);
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
					} else {
						/*
						 * basic 'plot fs' command
						 */
						
						// Skip over any plots that are part of a PlotGroup.
						if (pos.getTownBlock().hasPlotObjectGroup()) {
							TownyMessaging.sendErrorMsg(player, Translatable.of("msg_err_plot_belongs_to_group_plot_fs2", pos));
							return false;
						}

						// Otherwise continue on normally.
						setPlotForSale(resident, pos, plotPrice);
					}

				} else if (split[0].equalsIgnoreCase("perm")) {
					
					parsePlotPermCommand(player, StringMgmt.remFirstArg(split));
					
				} else if (split[0].equalsIgnoreCase("info")) {
					if (!permSource.testPermission(player, PermissionNodes.TOWNY_COMMAND_PLOT_PERM_INFO.getNode()))
						throw new TownyException(Translatable.of("msg_err_command_disable"));
					
					sendPlotInfo(player, StringMgmt.remFirstArg(split));
				} else if (split[0].equalsIgnoreCase("toggle")) {

					/*
					 * perm test in the plottoggle.
					 */
					// Test we are allowed to work on this plot
					plotTestOwner(resident, townBlock); // ignore the return as
					// we are only checking
					// for an exception
					
					// Make sure that the player is only operating on a single plot and not a plotgroup.
					if (townBlock.hasPlotObjectGroup()) {
						TownyMessaging.sendErrorMsg(player, Translatable.of("msg_err_plot_belongs_to_group_toggle"));
						return false;
					}

					plotToggle(player, new WorldCoord(world, Coord.parseCoord(player)).getTownBlock(), StringMgmt.remFirstArg(split));

				} else if (split[0].equalsIgnoreCase("set")) {

					split = StringMgmt.remFirstArg(split);
					
					if (split.length == 0 || split[0].equalsIgnoreCase("?")) {

						HelpMenu.PLOT_SET.send(player);

					} else if (split.length > 0) {

						if (!permSource.testPermission(player, PermissionNodes.TOWNY_COMMAND_PLOT_SET.getNode(split[0].toLowerCase())))
							throw new TownyException(Translatable.of("msg_err_command_disable"));
						
						// Make sure that the player is only operating on a plot object group if one exists.
						if (townBlock.hasPlotObjectGroup()) {
							TownyMessaging.sendErrorMsg(player, Translatable.of("msg_err_plot_belongs_to_group_set"));
							return false;
						}

						if (split[0].equalsIgnoreCase("perm")) {

							// Set plot level permissions (if the plot owner) or
							// Mayor/Assistant of the town.
							
							// Test we are allowed to work on this plot
							plotTestOwner(resident, townBlock);

							setTownBlockPermissions(player, townBlock.getTownBlockOwner(), townBlock, StringMgmt.remFirstArg(split));

							return true;

						} else if (split[0].equalsIgnoreCase("name")) {
							
							// Test we are allowed to work on this plot
							plotTestOwner(resident, townBlock);
							if (split.length == 1) {
								townBlock.setName("");
								TownyMessaging.sendMsg(player, Translatable.of("msg_plot_name_removed"));
								townBlock.save();
								return true;
							}
							
							String newName = StringMgmt.join(StringMgmt.remFirstArg(split), "_");
							
							// Test if the plot name contains invalid characters.
							if (!NameValidation.isBlacklistName(newName)) {								
								townBlock.setName(newName);

								//townBlock.setChanged(true);
								townBlock.save();

								TownyMessaging.sendMsg(player, Translatable.of("msg_plot_name_set_to", townBlock.getName()));

							} else {

								TownyMessaging.sendErrorMsg(player, Translatable.of("msg_invalid_name"));

							}
							return true;
						} else if (split[0].equalsIgnoreCase("outpost")) {

							if (TownySettings.isAllowingOutposts()) {
								if (!permSource.testPermission(player, PermissionNodes.TOWNY_COMMAND_TOWN_CLAIM_OUTPOST.getNode()))
									throw new TownyException(Translatable.of("msg_err_command_disable"));
								
								// Test we are allowed to work on this plot
								plotTestOwner(resident, townBlock);
								
								Town town = townBlock.getTownOrNull();
								TownyWorld townyWorld = townBlock.getWorld();
								boolean isAdmin = permSource.isTownyAdmin(player);
								Coord key = Coord.parseCoord(plugin.getCache(player).getLastLocation());
								
								if (OutpostUtil.OutpostTests(town, resident, townyWorld, key, isAdmin, true)) {
									// Test if they can pay.
									if (TownyEconomyHandler.isActive() && !town.getAccount().canPayFromHoldings(TownySettings.getOutpostCost())) 
										throw new TownyException(Translatable.of("msg_err_cannot_afford_to_set_outpost"));
									 
									// Create a confirmation for setting outpost.
									Confirmation.runOnAccept(() -> {
										// Make them pay.
										if (TownyEconomyHandler.isActive() && TownySettings.getOutpostCost() > 0 
											&& !town.getAccount().withdraw(TownySettings.getOutpostCost(), "Plot Set Outpost")) {
												TownyMessaging.sendErrorMsg(player, Translatable.of("msg_err_cannot_afford_to_set_outpost"));
												return;
										}

										// Set the outpost spawn and display feedback.
										town.addOutpostSpawn(player.getLocation());
										TownyMessaging.sendMsg(player, Translatable.of("msg_plot_set_cost", TownyEconomyHandler.getFormattedBalance(TownySettings.getOutpostCost()), Translatable.of("outpost")));
									})
									.setTitle(Translatable.of("msg_confirm_purchase", TownyEconomyHandler.getFormattedBalance(TownySettings.getOutpostCost())))
									.sendTo(player);
								}
							}
							return true;
						} else if (TownyCommandAddonAPI.hasCommand(CommandType.PLOT_SET, split[0])) {
							TownyCommandAddonAPI.getAddonCommand(CommandType.PLOT_SET, split[0]).execute(player, "plot", split);
							return true;
						}
						
						/*
						 * After trying all of the other /plot set subcommands, attempt to set the townblock type.
						 */
						
						try {
							String plotTypeName = split[0];
							
							// Handle type being reset
							if (plotTypeName.equalsIgnoreCase("reset"))
								plotTypeName = "default";

							TownBlockType townBlockType = TownBlockTypeHandler.getType(plotTypeName);
							
							if (townBlockType == null)
								throw new TownyException(Translatable.of("msg_err_not_block_type"));
							
							try {
								// Test we are allowed to work on this plot
								plotTestOwner(resident, townBlock); // ignore the return as we
								// are only checking for an
								// exception
							} catch (TownyException e) {
								TownyMessaging.sendErrorMsg(player, e.getMessage(player));
								return false;
							}

							if (TownBlockType.ARENA.equals(townBlockType) && TownySettings.getOutsidersPreventPVPToggle()) {
								for (Player target : Bukkit.getOnlinePlayers()) {
									if (!townBlock.getTownOrNull().hasResident(target) && !player.getName().equals(target.getName()) && townBlock.getWorldCoord().equals(WorldCoord.parseWorldCoord(target)))
										throw new TownyException(Translatable.of("msg_cant_toggle_pvp_outsider_in_plot"));
								}
							}
							
							PlotPreChangeTypeEvent preEvent = new PlotPreChangeTypeEvent(townBlockType, townBlock, resident);
							BukkitTools.getPluginManager().callEvent(preEvent);

							if (preEvent.isCancelled()) {
								TownyMessaging.sendErrorMsg(player, preEvent.getCancelMessage());
								return false;
							}

							double cost = townBlockType.getData().getCost();
							
							// Test if we can pay first to throw an exception.
							if (cost > 0 && TownyEconomyHandler.isActive() && !resident.getAccount().canPayFromHoldings(cost))
								throw new TownyException(Translatable.of("msg_err_cannot_afford_plot_set_type_cost", townBlockType, TownyEconomyHandler.getFormattedBalance(cost)));

							// Handle payment via a confirmation to avoid suprise costs.
							if (cost > 0 && TownyEconomyHandler.isActive()) {
								Confirmation.runOnAccept(() -> {
							
									if (!resident.getAccount().withdraw(cost, String.format("Plot set to %s", townBlockType))) {
										TownyMessaging.sendErrorMsg(player, Translatable.of("msg_err_cannot_afford_plot_set_type_cost", townBlockType, TownyEconomyHandler.getFormattedBalance(cost)));
										return;
									}

									TownyMessaging.sendMsg(resident, Translatable.of("msg_plot_set_cost", TownyEconomyHandler.getFormattedBalance(cost), townBlockType));

									try {
										townBlock.setType(townBlockType, resident);
										
									} catch (TownyException e) {
										TownyMessaging.sendErrorMsg(resident, e.getMessage(player));
										return;
									}
									TownyMessaging.sendMsg(player, Translatable.of("msg_plot_set_type", townBlockType));
								})
									.setTitle(Translatable.of("msg_confirm_purchase", TownyEconomyHandler.getFormattedBalance(cost)))
									.sendTo(BukkitTools.getPlayerExact(resident.getName()));
							
							// No cost or economy so no confirmation.
							} else {
								townBlock.setType(townBlockType, resident);
								TownyMessaging.sendMsg(player, Translatable.of("msg_plot_set_type", plotTypeName));
							}
						} catch (TownyException te){
							TownyMessaging.sendErrorMsg(player, te.getMessage(player));
						}

					} else {

						HelpMenu.PLOT_SET.send(player);
					}

				} else if (split[0].equalsIgnoreCase("clear")) {

					if (!permSource.testPermission(player, PermissionNodes.TOWNY_COMMAND_PLOT_CLEAR.getNode()))
						throw new TownyException(Translatable.of("msg_err_command_disable"));
					
					if (townBlock != null) {
						
						if (!townBlock.getWorld().isUsingPlotManagementMayorDelete())
							throw new TownyException(Translatable.of("msg_err_plot_clear_disabled_in_this_world"));

						/*
						 * Only allow mayors or plot owners to use this command.
						 * This will throw an exception if the player isn't a mayor or owner of the plot. 
						 */
						plotTestOwner(resident, townBlock);

						PlotPreClearEvent preEvent = new PlotPreClearEvent(townBlock);
						BukkitTools.getPluginManager().callEvent(preEvent);
						
						if (preEvent.isCancelled()) {
							TownyMessaging.sendErrorMsg(player, preEvent.getCancelMessage());
							return false;
						}
							

						EnumSet<Material> materialsToDelete = TownyAPI.getInstance().getTownyWorld(world).getPlotManagementMayorDelete();
						if (!materialsToDelete.isEmpty()) {
							TownyRegenAPI.deleteMaterialsFromTownBlock(townBlock, materialsToDelete);
							TownyMessaging.sendMsg(player, Translatable.of("msg_clear_plot_material", StringMgmt.join(materialsToDelete, ", ")));
						}

						// Raise an event for the claim
						BukkitTools.getPluginManager().callEvent(new PlotClearEvent(townBlock));

					} else {
						// Shouldn't ever reach here as a null townBlock should
						// be caught already in WorldCoord.
						TownyMessaging.sendErrorMsg(player, Translatable.of("msg_err_empty_area_selection"));
					}

				} else if (split[0].equalsIgnoreCase("group")) {

					return handlePlotGroupCommand(StringMgmt.remFirstArg(split), player);

				} else if (split[0].equalsIgnoreCase("jailcell")) {
					
					parsePlotJailCell(player, TownyAPI.getInstance().getTownBlock(player), StringMgmt.remFirstArg(split));
					return true;
					
				} else if (split[0].equalsIgnoreCase("trust")) {
					if (!permSource.testPermission(player, PermissionNodes.TOWNY_COMMAND_PLOT_TRUST.getNode()))
						throw new TownyException(Translatable.of("msg_err_command_disable"));

					parsePlotTrustCommand(player, StringMgmt.remFirstArg(split));
					return true;
					
				} else if (TownyCommandAddonAPI.hasCommand(CommandType.PLOT, split[0])) {
					TownyCommandAddonAPI.getAddonCommand(CommandType.PLOT, split[0]).execute(player, "plot", split);
				} else
					throw new TownyException(Translatable.of("msg_err_invalid_property", split[0]));

			} catch (TownyException x) {
				TownyMessaging.sendErrorMsg(player, x.getMessage(player));
			}
		}

		return true;
	}

	private void parsePlotJailCell(Player player, TownBlock townBlock, String[] args) {
		
		if (args.length == 0 || args[0].equalsIgnoreCase("?") || args[0].equalsIgnoreCase("help"))
			HelpMenu.PLOT_JAILCELL.send(player);

		try {
			if (!TownyUniverse.getInstance().getPermissionSource().has(player, PermissionNodes.TOWNY_COMMAND_PLOT_JAILCELL.getNode()))
				throw new TownyException("msg_err_command_disable");

			// Fail if the resident isn't registered. (Very unlikely.)
			Resident resident = TownyAPI.getInstance().getResident(player.getUniqueId());
			if (resident == null)
				return;
			
			// Fail if we're not in a jail plot.
			if (townBlock == null || !townBlock.isJail())
				throw new TownyException("msg_err_location_is_not_within_a_jail_plot");

			// Test we are allowed to work on this plot, and able to set a jail spawn.
			// If this fails it will trigger a TownyException.
			plotTestOwner(resident, townBlock);

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
					
					if (jail.getCellMap().size() == 1) 
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
		} catch (TownyException e) {
			TownyMessaging.sendErrorMsg(player, e.getMessage(player));
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
	 */
	public static TownyPermissionChange setTownBlockPermissions(Player player, TownBlockOwner townBlockOwner, TownBlock townBlock, String[] split) {
		TownyPermissionChange permChange = null;

		if (split.length == 0 || split[0].equalsIgnoreCase("?")) {

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
			} else if (split.length == 3) {
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

			if (permChange != null)
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
			TownBlockSettingsChangedEvent event = new TownBlockSettingsChangedEvent(townBlock);
			Bukkit.getServer().getPluginManager().callEvent(event);

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
		plotTestOwner(resident, townBlock);
		townBlock.setPlotPrice(Math.min(TownySettings.getMaxPlotPrice(), forSale));

		if (forSale != -1) {
			Translatable message = TownyEconomyHandler.isActive()
				? Translatable.of("msg_plot_for_sale_amount", resident.getName(), worldCoord.toString(), TownyEconomyHandler.getFormattedBalance(townBlock.getPlotPrice()))
				: Translatable.of("msg_plot_for_sale", resident.getName(), worldCoord.toString());
			
			TownyMessaging.sendPrefixedTownMessage(townBlock.getTownOrNull(), message);
			
			if (!resident.hasTown() || (resident.hasTown() && townBlock.getTownOrNull() != resident.getTownOrNull()))
				TownyMessaging.sendMsg(resident, message);
			
			Bukkit.getPluginManager().callEvent(new PlotSetForSaleEvent(resident, townBlock.getPlotPrice(), townBlock));
		} else {
			TownyMessaging.sendMsg(resident, Translatable.of("msg_plot_set_to_nfs"));
			Bukkit.getPluginManager().callEvent(new PlotNotForSaleEvent(resident, townBlock));
		}

		// Save this townblock so the for sale status is remembered.
		townBlock.save();
	}

	/**
	 * Toggle the plots flags for pvp/explosion/fire/mobs (if town/world
	 * permissions allow)
	 * 
	 * @param player - Player.
	 * @param townBlock - TownBlock object.
	 * @param split  - Current command arguments.
	 */
	public void plotToggle(Player player, TownBlock townBlock, String[] split) {
		TownyPermissionSource permSource = TownyUniverse.getInstance().getPermissionSource();
		
		if (split.length == 0 || split[0].equalsIgnoreCase("?")) {
			HelpMenu.PLOT_TOGGLE.send(player);
		} else {

			try {

				if (!permSource.testPermission(player, PermissionNodes.TOWNY_COMMAND_PLOT_TOGGLE.getNode(split[0].toLowerCase())))
					throw new TownyException(Translatable.of("msg_err_command_disable"));
				
				Town town = townBlock.getTownOrNull();
				if (town == null)
					throw new TownyException(Translatable.of("msg_not_claimed_1"));
				
				Optional<Boolean> choice = Optional.empty();
				if (split.length == 2) {
					choice = BaseCommand.parseToggleChoice(split[1]);
				}

				if (split[0].equalsIgnoreCase("pvp")) {
					// Make sure we are allowed to set these permissions.
					toggleTest(player, townBlock, StringMgmt.join(split, " "));
					
					if (TownySettings.getPVPCoolDownTime() > 0 && !permSource.isTownyAdmin(player)) {
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
					PlotTogglePvpEvent plotTogglePvpEvent = new PlotTogglePvpEvent(townBlock, player, choice.orElse(!townBlock.getPermissions().pvp));
					Bukkit.getPluginManager().callEvent(plotTogglePvpEvent);
					if (plotTogglePvpEvent.isCancelled())
						throw new TownyException(plotTogglePvpEvent.getCancellationMsg());

					townBlock.getPermissions().pvp = choice.orElse(!townBlock.getPermissions().pvp);
					// Add a cooldown timer for this plot.
					if (TownySettings.getPVPCoolDownTime() > 0 && !permSource.isTownyAdmin(player))
						CooldownTimerTask.addCooldownTimer(townBlock.getWorldCoord().toString(), CooldownType.PVP);
					TownyMessaging.sendMsg(player, Translatable.of("msg_changed_pvp", "Plot", townBlock.getPermissions().pvp ? Translatable.of("enabled") : Translatable.of("disabled")));

				} else if (split[0].equalsIgnoreCase("explosion")) {
					// Make sure we are allowed to set these permissions.
					toggleTest(player, townBlock, StringMgmt.join(split, " "));
					// Fire cancellable event directly before setting the toggle.
					PlotToggleExplosionEvent plotToggleExplosionEvent = new PlotToggleExplosionEvent(townBlock, player, choice.orElse(!townBlock.getPermissions().explosion));
					Bukkit.getPluginManager().callEvent(plotToggleExplosionEvent);
					if (plotToggleExplosionEvent.isCancelled())
						throw new TownyException(plotToggleExplosionEvent.getCancellationMsg());

					townBlock.getPermissions().explosion = choice.orElse(!townBlock.getPermissions().explosion);
					TownyMessaging.sendMsg(player, Translatable.of("msg_changed_expl", "the Plot", townBlock.getPermissions().explosion ? Translatable.of("enabled") : Translatable.of("disabled")));

				} else if (split[0].equalsIgnoreCase("fire")) {
					// Make sure we are allowed to set these permissions.
					toggleTest(player, townBlock, StringMgmt.join(split, " "));
					// Fire cancellable event directly before setting the toggle.
					PlotToggleFireEvent plotToggleFireEvent = new PlotToggleFireEvent(townBlock, player, choice.orElse(!townBlock.getPermissions().fire));
					Bukkit.getPluginManager().callEvent(plotToggleFireEvent);
					if (plotToggleFireEvent.isCancelled())
						throw new TownyException(plotToggleFireEvent.getCancellationMsg());

					townBlock.getPermissions().fire = choice.orElse(!townBlock.getPermissions().fire);
					TownyMessaging.sendMsg(player, Translatable.of("msg_changed_fire", "the Plot", townBlock.getPermissions().fire ? Translatable.of("enabled") : Translatable.of("disabled")));

				} else if (split[0].equalsIgnoreCase("mobs")) {
					// Make sure we are allowed to set these permissions.
					toggleTest(player, townBlock, StringMgmt.join(split, " "));
					// Fire cancellable event directly before setting the toggle.
					PlotToggleMobsEvent plotToggleMobsEvent= new PlotToggleMobsEvent(townBlock, player, choice.orElse(!townBlock.getPermissions().mobs));
					Bukkit.getPluginManager().callEvent(plotToggleMobsEvent);
					if (plotToggleMobsEvent.isCancelled())
						throw new TownyException(plotToggleMobsEvent.getCancellationMsg());

					townBlock.getPermissions().mobs = choice.orElse(!townBlock.getPermissions().mobs);
					
					TownyMessaging.sendMsg(player, Translatable.of("msg_changed_mobs", "the Plot", townBlock.getPermissions().mobs ? Translatable.of("enabled") : Translatable.of("disabled")));
				} else if (TownyCommandAddonAPI.hasCommand(CommandType.PLOT_TOGGLE, split[0])) {
					TownyCommandAddonAPI.getAddonCommand(CommandType.PLOT_TOGGLE, split[0]).execute(player, "plot", split);
				} else {
					TownyMessaging.sendErrorMsg(player, Translatable.of("msg_err_invalid_property", "plot"));
					return;
				}

				townBlock.setChanged(true);

				//Change settings event
				TownBlockSettingsChangedEvent event = new TownBlockSettingsChangedEvent(townBlock);
				Bukkit.getServer().getPluginManager().callEvent(event);

			} catch (TownyException e) {
				TownyMessaging.sendErrorMsg(player, e.getMessage(player));
			}
			
			townBlock.save();
		}
	}

	/**
	 * Toggle the plot group flags for pvp/explosion/fire/mobs (if town/world
	 * permissions allow)
	 *
	 * @param player - Player.
	 * @param plotGroup - PlotObjectGroup object.
	 * @param split  - Current command arguments.
	 */
	public void plotGroupToggle(Player player, PlotGroup plotGroup, String[] split) {
		TownyPermissionSource permSource = TownyUniverse.getInstance().getPermissionSource();

		if (split.length == 0 || split[0].equalsIgnoreCase("?")) {
			HelpMenu.PLOT_GROUP_TOGGLE.send(player);
		} else {

			try {
				// We need to keep an ending string to show the message only after the transaction is over,
				// to prevent chat log spam.
				Translatable endingMessage = null;
				
				Optional<Boolean> choice = Optional.empty();
				if (split.length == 2) {
					choice = BaseCommand.parseToggleChoice(split[1]);
				}
				
				for (TownBlock groupBlock : plotGroup.getTownBlocks()) {
					
					Town town = groupBlock.getTownOrNull();
					if (town == null) continue;
					
					if (!permSource.testPermission(player, PermissionNodes.TOWNY_COMMAND_PLOT_TOGGLE.getNode(split[0].toLowerCase())))
						throw new TownyException(Translatable.of("msg_err_command_disable"));

					if (split[0].equalsIgnoreCase("pvp")) {
						// Make sure we are allowed to set these permissions.
						toggleTest(player, groupBlock, StringMgmt.join(split, " "));

						if (TownySettings.getPVPCoolDownTime() > 0 && !permSource.isTownyAdmin(player)) {
							// Test to see if the pvp cooldown timer is active for the town this plot belongs to.
							if (CooldownTimerTask.hasCooldown(town.getUUID().toString(), CooldownType.PVP))
								throw new TownyException(Translatable.of("msg_err_cannot_toggle_pvp_x_seconds_remaining", CooldownTimerTask.getCooldownRemaining(town.getUUID().toString(), CooldownType.PVP)));

							// Test to see if the pvp cooldown timer is active for this plot.
							if (CooldownTimerTask.hasCooldown(groupBlock.getWorldCoord().toString(), CooldownType.PVP))
								throw new TownyException(Translatable.of("msg_err_cannot_toggle_pvp_x_seconds_remaining", CooldownTimerTask.getCooldownRemaining(groupBlock.getWorldCoord().toString(), CooldownType.PVP)));
						}

						if (TownySettings.getOutsidersPreventPVPToggle() && choice.orElse(!groupBlock.getPermissions().pvp)) {
							for (Player target : Bukkit.getOnlinePlayers()) {
								if (!town.hasResident(target) && !player.getName().equals(target.getName()) && groupBlock.getWorldCoord().equals(WorldCoord.parseWorldCoord(target)))
									throw new TownyException(Translatable.of("msg_cant_toggle_pvp_outsider_in_plot"));
							}
						}

						PlotTogglePvpEvent plotTogglePvpEvent = new PlotTogglePvpEvent(groupBlock, player, choice.orElse(!groupBlock.getPermissions().pvp));
						Bukkit.getPluginManager().callEvent(plotTogglePvpEvent);
						if (plotTogglePvpEvent.isCancelled()) {
							TownyMessaging.sendErrorMsg(player, plotTogglePvpEvent.getCancellationMsg());
							return;
						}

						groupBlock.getPermissions().pvp = choice.orElse(!groupBlock.getPermissions().pvp);
						// Add a cooldown timer for this plot.
						if (TownySettings.getPVPCoolDownTime() > 0 && !permSource.isTownyAdmin(player))
							CooldownTimerTask.addCooldownTimer(groupBlock.getWorldCoord().toString(), CooldownType.PVP);
						
						endingMessage = Translatable.of("msg_changed_pvp", Translatable.of("msg_the_plot_group"), groupBlock.getPermissions().pvp ? Translatable.of("enabled") : Translatable.of("disabled"));

					} else if (split[0].equalsIgnoreCase("explosion")) {
						// Make sure we are allowed to set these permissions.
						toggleTest(player, groupBlock, StringMgmt.join(split, " "));

						PlotToggleExplosionEvent plotToggleExplosionEvent = new PlotToggleExplosionEvent(groupBlock, player, choice.orElse(!groupBlock.getPermissions().explosion));
						Bukkit.getPluginManager().callEvent(plotToggleExplosionEvent);
						if (plotToggleExplosionEvent.isCancelled()) {
							TownyMessaging.sendErrorMsg(player, plotToggleExplosionEvent.getCancellationMsg());
							return;
						}

						groupBlock.getPermissions().explosion = choice.orElse(!groupBlock.getPermissions().explosion);
						endingMessage = Translatable.of("msg_changed_expl", Translatable.of("msg_the_plot_group"), groupBlock.getPermissions().explosion ? Translatable.of("enabled") : Translatable.of("disabled"));

					} else if (split[0].equalsIgnoreCase("fire")) {
						// Make sure we are allowed to set these permissions.
						toggleTest(player, groupBlock, StringMgmt.join(split, " "));

						PlotToggleFireEvent plotToggleFireEvent = new PlotToggleFireEvent(groupBlock, player, choice.orElse(!groupBlock.getPermissions().fire));
						Bukkit.getPluginManager().callEvent(plotToggleFireEvent);
						if (plotToggleFireEvent.isCancelled()) {
							TownyMessaging.sendErrorMsg(player, plotToggleFireEvent.getCancellationMsg());
							return;
						}
						
						groupBlock.getPermissions().fire = choice.orElse(!groupBlock.getPermissions().fire);
						endingMessage = Translatable.of("msg_changed_fire", Translatable.of("msg_the_plot_group"), groupBlock.getPermissions().fire ? Translatable.of("enabled") : Translatable.of("disabled"));

					} else if (split[0].equalsIgnoreCase("mobs")) {
						// Make sure we are allowed to set these permissions.
						toggleTest(player, groupBlock, StringMgmt.join(split, " "));

						PlotToggleMobsEvent plotToggleMobsEvent = new PlotToggleMobsEvent(groupBlock, player, choice.orElse(!groupBlock.getPermissions().mobs));
						Bukkit.getPluginManager().callEvent(plotToggleMobsEvent);
						if (plotToggleMobsEvent.isCancelled()) {
							TownyMessaging.sendErrorMsg(player, plotToggleMobsEvent.getCancellationMsg());
							return;
						}

						groupBlock.getPermissions().mobs = choice.orElse(!groupBlock.getPermissions().mobs);
						endingMessage = Translatable.of("msg_changed_mobs", Translatable.of("msg_the_plot_group"), groupBlock.getPermissions().mobs ? Translatable.of("enabled") : Translatable.of("disabled"));

					} else {
						TownyMessaging.sendErrorMsg(player, Translatable.of("msg_err_invalid_property", "plot"));
						return;
					}

					groupBlock.setChanged(true);

					//Change settings event
					TownBlockSettingsChangedEvent event = new TownBlockSettingsChangedEvent(groupBlock);
					Bukkit.getServer().getPluginManager().callEvent(event);
					
					// Save
					groupBlock.save();
				}
				
				// Finally send the message.
				if (endingMessage != null)
					TownyMessaging.sendMsg(player, endingMessage);
				

			} catch (TownyException e) {
				TownyMessaging.sendErrorMsg(player, e.getMessage(player));
			} catch (Exception e) {
				TownyMessaging.sendErrorMsg(player, e.getMessage());
			}

			
		}
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

		split = split.toLowerCase();

		if (split.contains("mobs")) {
			if (townBlock.getWorld().isForceTownMobs())
				throw new TownyException(Translatable.of("msg_world_mobs"));
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

	/**
	 * Test the townBlock to ensure we are either the plot owner, or the
	 * mayor/assistant, or a TownyAdmin.
	 * 
	 * @param resident Resident Object.
	 * @param townBlock TownBlock Object.
	 * @throws TownyException Exception thrown to trigger failures in the methods using this method.
	 */
	public static void plotTestOwner(Resident resident, TownBlock townBlock) throws TownyException {

		Player player = BukkitTools.getPlayer(resident.getName());
		boolean isAdmin = TownyUniverse.getInstance().getPermissionSource().isTownyAdmin(player);
		boolean isMayor = player.hasPermission(PermissionNodes.TOWNY_COMMAND_PLOT_ASMAYOR.getNode());

		if (townBlock.hasResident()) {
			boolean isMayorInTheirOwnTown = isMayor && resident.hasTown() && resident.getTownOrNull() == townBlock.getTownOrNull();

			if (isAdmin || isMayorInTheirOwnTown || townBlock.hasResident(resident)) {
				// Resident is either an admin, or the mayor (or equivalent) of the townblock, or the townblock's actual owner.
				return;
			}

			// Not the plot owner or the towns mayor or an admin.
			throw new TownyException(Translatable.of("msg_area_not_own"));

		} else {

			final boolean asMayorInUnowned = player.hasPermission(PermissionNodes.TOWNY_COMMAND_PLOT_ASMAYORINUNOWNED.getNode());
			boolean isSameTown = townBlock.getTownOrNull().hasResident(resident);

			if (isSameTown && !isMayor && !asMayorInUnowned)
				throw new TownyException(Translatable.of("msg_not_mayor_ass"));

			if (!isSameTown && !isAdmin)
				throw new TownyException(Translatable.of("msg_err_not_part_town"));

			// Nothing to complain about, this resident is the owner of the townblock's town or an admin.
			return;
		}
	}
	
	private boolean handlePlotGroupCommand(String[] split, Player player) throws TownyException {

		TownyPermissionSource permSource = TownyUniverse.getInstance().getPermissionSource();

		Resident resident = getResidentOrThrow(player.getUniqueId());
		TownBlock townBlock = TownyAPI.getInstance().getTownBlock(player);
		if (townBlock == null)
			throw new TownyException(Translatable.of("msg_not_claimed_1"));
		Town town = townBlock.getTownOrNull();
		if (town == null)
			throw new TownyException(Translatable.of("msg_not_claimed_1"));
		
		// Test we are allowed to work on this plot
		// If this fails it will trigger a TownyException.
		plotTestOwner(resident, townBlock);

		if (split.length <= 0 || split[0].equalsIgnoreCase("?")) {

			HelpMenu.PLOT_GROUP_HELP.send(player);

			if (townBlock.hasPlotObjectGroup())
				TownyMessaging.sendMsg(player, Translatable.of("status_plot_group_name_and_size", townBlock.getPlotObjectGroup().getName(), townBlock.getPlotObjectGroup().getTownBlocks().size()));
			
			return true;
		}

		if (split[0].equalsIgnoreCase("add") || split[0].equalsIgnoreCase("new") || split[0].equalsIgnoreCase("create")) {

			if (!permSource.testPermission(player, PermissionNodes.TOWNY_COMMAND_PLOT_GROUP_ADD.getNode()))
				throw new TownyException(Translatable.of("msg_err_command_disable"));
			
			if (split.length == 1) {
				TownyMessaging.sendErrorMsg(player, Translatable.of("msg_err_you_must_specify_a_group_name"));
				return false;
			}

			if (split.length == 2) {
				String plotGroupName = NameValidation.filterName(split[1]);
				plotGroupName = NameValidation.filterCommas(plotGroupName);
				
				if (town.hasPlotGroupName(plotGroupName)) {
					TownBlockType groupType = town.getPlotObjectGroupFromName(plotGroupName).getTownBlockType();
					if (townBlock.getType() != groupType)
						throw new TownyException(Translatable.of("msg_err_this_townblock_doesnt_match_the_groups_type", groupType.getName()));
				}
				
				if (townBlock.hasPlotObjectGroup()) {
					
					// Already has a PlotGroup and it is the same name being used to re-add.
					if (townBlock.getPlotObjectGroup().getName().equalsIgnoreCase(plotGroupName)) {
						TownyMessaging.sendErrorMsg(player, Translatable.of("msg_err_this_plot_is_already_part_of_the_plot_group_x", plotGroupName));
						return false;
					}

					final String name = plotGroupName;
					// Already has a PlotGroup, ask if they want to transfer from one group to another.
					Confirmation.runOnAccept( ()-> {
						PlotGroup oldGroup = townBlock.getPlotObjectGroup();
						oldGroup.removeTownBlock(townBlock);
						if (oldGroup.getTownBlocks().isEmpty()) {
							String oldName = oldGroup.getName();
							town.removePlotGroup(oldGroup);
							TownyUniverse.getInstance().getDataSource().removePlotGroup(oldGroup);
							TownyMessaging.sendMsg(player, Translatable.of("msg_plotgroup_deleted", oldName));
						} else 
							oldGroup.save();
						createOrAddOnToPlotGroup(townBlock, town, name);
						TownyMessaging.sendMsg(player, Translatable.of("msg_townblock_transferred_from_x_to_x_group", oldGroup.getName(), townBlock.getPlotObjectGroup().getName()));
					})
					.setTitle(Translatable.of("msg_plot_group_already_exists_did_you_want_to_transfer", townBlock.getPlotObjectGroup().getName(), split[1]))
					.sendTo(player);
				} else {
					// Create a brand new plot group.
					createOrAddOnToPlotGroup(townBlock, town, plotGroupName);
					TownyMessaging.sendMsg(player, Translatable.of("msg_plot_was_put_into_group_x", townBlock.getX(), townBlock.getZ(), townBlock.getPlotObjectGroup().getName()));
				}
			} else {
				TownyMessaging.sendErrorMsg(player, Translatable.of("msg_err_plot_group_name_required"));
				return false;
			}

		} else if (split[0].equalsIgnoreCase("delete")) {
			if (!permSource.testPermission(player, PermissionNodes.TOWNY_COMMAND_PLOT_GROUP_DELETE.getNode()))
				throw new TownyException(Translatable.of("msg_err_command_disable"));
			
			if (!townBlock.hasPlotObjectGroup()) {
				TownyMessaging.sendErrorMsg(player, Translatable.of("msg_err_plot_not_associated_with_a_group"));
				return false;
			}
			Confirmation.runOnAccept(()-> {
				PlotGroup group = townBlock.getPlotObjectGroup();
				String name = group.getName();
				town.removePlotGroup(group);
				TownyUniverse.getInstance().getDataSource().removePlotGroup(group);
				TownyMessaging.sendMsg(player, Translatable.of("msg_plotgroup_deleted", name));
			}).sendTo(player);
			
		} else if (split[0].equalsIgnoreCase("remove")) {
			if (!permSource.testPermission(player, PermissionNodes.TOWNY_COMMAND_PLOT_GROUP_REMOVE.getNode()))
				throw new TownyException(Translatable.of("msg_err_command_disable"));
			
			if (!townBlock.hasPlotObjectGroup()) {
				TownyMessaging.sendErrorMsg(player, Translatable.of("msg_err_plot_not_associated_with_a_group"));
				return false;
			}
			PlotGroup group = townBlock.getPlotObjectGroup();
			String name = group.getName();
			// Remove the plot from the group.
			group.removeTownBlock(townBlock);

			// Detach group from townblock.
			townBlock.removePlotObjectGroup();

			// Save
			townBlock.save();
			TownyMessaging.sendMsg(player, Translatable.of("msg_plot_was_removed_from_group_x", townBlock.getX(), townBlock.getZ(), name));
			
			if (group.getTownBlocks().isEmpty()) {
				town.removePlotGroup(group);
				TownyUniverse.getInstance().getDataSource().removePlotGroup(group);
				TownyMessaging.sendMsg(player, Translatable.of("msg_plotgroup_empty_deleted", name));
			}

		} else if (split[0].equalsIgnoreCase("rename")) {
			if (!permSource.testPermission(player, PermissionNodes.TOWNY_COMMAND_PLOT_GROUP_RENAME.getNode()))
				throw new TownyException(Translatable.of("msg_err_command_disable"));

			if (!townBlock.hasPlotObjectGroup()) {
				TownyMessaging.sendErrorMsg(player, Translatable.of("msg_err_plot_not_associated_with_a_group"));
				return false;
			}

			if (split.length == 1) {
				TownyMessaging.sendErrorMsg(player, Translatable.of("msg_err_plot_group_name_required"));
				return false;
			}
		
			String newName= split[1];			
			String oldName = townBlock.getPlotObjectGroup().getName();
			// Change name;
			TownyUniverse.getInstance().getDataSource().renameGroup(townBlock.getPlotObjectGroup(), newName);
			TownyMessaging.sendMsg(player, Translatable.of("msg_plot_renamed_from_x_to_y", oldName, newName));

		} else if (split[0].equalsIgnoreCase("forsale") || split[0].equalsIgnoreCase("fs")) {
			if (!permSource.testPermission(player, PermissionNodes.TOWNY_COMMAND_PLOT_GROUP_FORSALE.getNode()))
				throw new TownyException(Translatable.of("msg_err_command_disable"));
			
			// This means the player wants to fs the plot group they are in.
			PlotGroup group = townBlock.getPlotObjectGroup();
			
			if (group == null) {
				TownyMessaging.sendErrorMsg(player, Translatable.of("msg_err_plot_not_associated_with_a_group"));
				return false;
			}
			
			if (split.length < 2) {
				TownyMessaging.sendErrorMsg(player, Translatable.of("msg_err_plot_group_specify_price"));
				return false;
			}
			
			double price = MoneyUtil.getMoneyAboveZeroOrThrow(split[1]);
			group.setPrice(Math.min(price, TownySettings.getMaxPlotPrice()));
			
			// Save
			group.save();

			Translatable message = Translatable.of("msg_player_put_group_up_for_sale", player.getName(), group.getName(), TownyEconomyHandler.getFormattedBalance(group.getPrice()));
			TownyMessaging.sendPrefixedTownMessage(town, message);
			
			if (!resident.hasTown() || resident.getTownOrNull() != town)
				TownyMessaging.sendMsg(player, message);
		} else if (split[0].equalsIgnoreCase("notforsale") || split[0].equalsIgnoreCase("nfs")) {
			if (!permSource.testPermission(player, PermissionNodes.TOWNY_COMMAND_PLOT_GROUP_NOTFORSALE.getNode()))
				throw new TownyException(Translatable.of("msg_err_command_disable"));
			
			// This means the player wants to nfs the plot group they are in.
			PlotGroup group = townBlock.getPlotObjectGroup();
			
			if (group == null) {
				TownyMessaging.sendErrorMsg(player, Translatable.of("msg_err_plot_not_associated_with_a_group"));
				return false;
			}

			group.setPrice(-1);

			// Save
			group.save();

			TownyMessaging.sendPrefixedTownMessage(town, Translatable.of("msg_player_made_group_not_for_sale", player.getName(), group.getName()));
			
			if (!resident.hasTown() || resident.getTownOrNull() != town)
				TownyMessaging.sendMsg(player, Translatable.of("msg_player_made_group_not_for_sale", player.getName(), group.getName()));
		} else if (split[0].equalsIgnoreCase("toggle")) {
			if (!permSource.testPermission(player, PermissionNodes.TOWNY_COMMAND_PLOT_GROUP_TOGGLE.getNode()))
				throw new TownyException(Translatable.of("msg_err_command_disable"));

			if (townBlock.getPlotObjectGroup() == null) {
				TownyMessaging.sendErrorMsg(player, Translatable.of("msg_err_plot_not_associated_with_a_group"));
				return false;
			}
			
			// Create confirmation.
			PlotGroup plotGroup = townBlock.getPlotObjectGroup();
			Confirmation.runOnAccept(() -> {
				// Perform the toggle.
				new PlotCommand(Towny.getPlugin()).plotGroupToggle(player, plotGroup, StringMgmt.remArgs(split, 1));
			})
			.setTitle(Translatable.of("msg_plot_group_toggle_confirmation", townBlock.getPlotObjectGroup().getTownBlocks().size()).append(" " + Translatable.of("are_you_sure_you_want_to_continue")))
			.sendTo(player);
			
			return true;
		} else if (split[0].equalsIgnoreCase("set")) {
			if (!permSource.testPermission(player, PermissionNodes.TOWNY_COMMAND_PLOT_GROUP_SET.getNode()))
				throw new TownyException(Translatable.of("msg_err_command_disable"));
			
			// Check if group is present.
			if (townBlock.getPlotObjectGroup() == null) {
				TownyMessaging.sendErrorMsg(player, Translatable.of("msg_err_plot_not_associated_with_a_group"));
				return false;
			}
			TownBlockOwner townBlockOwner = townBlock.getTownBlockOwner();
			
			if (split.length < 2) {
				TownyMessaging.sendMessage(player, ChatTools.formatTitle("/plot group set"));
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
				return false;
			}

			if (split[1].equalsIgnoreCase("perm")) {
				
				// Set plot level permissions (if the plot owner) or
				// Mayor/Assistant of the town.
				
				PlotGroup plotGroup = townBlock.getPlotObjectGroup();
				
				Runnable permHandler = () -> {

					// setTownBlockPermissions returns a towny permission change object
					TownyPermissionChange permChange = PlotCommand.setTownBlockPermissions(player, townBlockOwner, townBlock, StringMgmt.remArgs(split, 2));
					// If the perm change object is not null
					if (permChange != null) {
						plotGroup.getPermissions().change(permChange);
						
						plotGroup.getTownBlocks().stream()
							.forEach(tb -> {
								tb.setPermissions(plotGroup.getPermissions().toString());
								tb.setChanged(!tb.getPermissions().toString().equals(town.getPermissions().toString()));
								tb.save();
								// Change settings event
								TownBlockSettingsChangedEvent event = new TownBlockSettingsChangedEvent(tb);
								Bukkit.getServer().getPluginManager().callEvent(event);
							});

						plugin.resetCache();
						
						TownyPermission perm = plotGroup.getPermissions();
						TownyMessaging.sendMessage(player, Translatable.of("msg_set_perms").forLocale(player));
						TownyMessaging.sendMessage(player, (Colors.Green + Translatable.of("status_perm").forLocale(player) + " " + ((townBlockOwner instanceof Resident) ? perm.getColourString().replace("n", "t") : perm.getColourString().replace("f", "r"))));
						TownyMessaging.sendMessage(player, Colors.Green + Translatable.of("status_pvp").forLocale(player) + " " + (!(CombatUtil.preventPvP(townBlock.getWorld(), townBlock)) ? Colors.Red + "ON" : Colors.LightGreen + "OFF") + 
															Colors.Green + Translatable.of("explosions").forLocale(player) + " " + ((perm.explosion) ? Colors.Red + "ON" : Colors.LightGreen + "OFF") + 
															Colors.Green + Translatable.of("firespread").forLocale(player) + " " + ((perm.fire) ? Colors.Red + "ON" : Colors.LightGreen + "OFF") + 
															Colors.Green + Translatable.of("mobspawns").forLocale(player) + " " + ((perm.mobs) ? Colors.Red + "ON" : Colors.LightGreen + "OFF"));
					}
				};

				// Create confirmation.
				Confirmation.runOnAccept(permHandler)
					.setTitle(Translatable.of("msg_plot_group_set_perm_confirmation", townBlock.getPlotObjectGroup().getTownBlocks().size()).append(" " + Translatable.of("are_you_sure_you_want_to_continue")))
					.sendTo(player);
				
				return true;
			}
			
			/*
			 * After all other set commands are tested for we attempt to set the townblocktype.
			 */
			
			String plotTypeName = split[1];

			// Stop setting plot groups to Jail plot, because that would set a spawn point for each plot in the location of the player.			
			if (plotTypeName.equalsIgnoreCase("jail")) {
				throw new TownyException(Translatable.of("msg_err_cannot_set_group_to_jail"));
			}

			// Handle type being reset
			if (plotTypeName.equalsIgnoreCase("reset"))
				plotTypeName = "default";

			TownBlockType type = TownBlockTypeHandler.getType(plotTypeName);

			if (type == null)
				throw new TownyException(Translatable.of("msg_err_not_block_type"));
				
			for (TownBlock tb : townBlock.getPlotObjectGroup().getTownBlocks()) {
				
				try {
					// Test we are allowed to work on this plot
					plotTestOwner(resident, townBlock); // ignore the return as we
					// are only checking for an
					// exception
				} catch (TownyException e) {
					TownyMessaging.sendErrorMsg(resident, e.getMessage(player));
					return false;
				}

				if (TownBlockType.ARENA.equals(type) && TownySettings.getOutsidersPreventPVPToggle()) {
					for (Player target : Bukkit.getOnlinePlayers()) {
						if (!town.hasResident(target) && !player.getName().equals(target.getName()) && tb.getWorldCoord().equals(WorldCoord.parseWorldCoord(target)))
							throw new TownyException(Translatable.of("msg_cant_toggle_pvp_outsider_in_plot"));
					}
				}
				
				// Allow for PlotPreChangeTypeEvent to trigger
				PlotPreChangeTypeEvent preEvent = new PlotPreChangeTypeEvent(type, tb, resident);
				BukkitTools.getPluginManager().callEvent(preEvent);
				
				// If any one of the townblocks is not allowed to be set, cancel setting all of them.
				if (preEvent.isCancelled()) {
					TownyMessaging.sendErrorMsg(player, preEvent.getCancelMessage());
					return false;
				}
			}
			
			int amount = townBlock.getPlotObjectGroup().getTownBlocks().size();
			double cost = type.getCost() * amount;
			
			try {
				// Test if we can pay first to throw an exception.
				if (cost > 0 && TownyEconomyHandler.isActive() && !resident.getAccount().canPayFromHoldings(cost))
					throw new TownyException(Translatable.of("msg_err_cannot_afford_plot_set_type_cost", type, TownyEconomyHandler.getFormattedBalance(cost)));

				// Handle payment via a confirmation to avoid suprise costs.
				if (cost > 0 && TownyEconomyHandler.isActive()) {
					Confirmation.runOnAccept(() -> {
						if (townBlock.getPlotObjectGroup() == null)
							return;
						
						final double finalCost = type.getCost() * townBlock.getPlotObjectGroup().getTownBlocks().size();
				
						if (!resident.getAccount().withdraw(finalCost, String.format("Plot group (" + townBlock.getPlotObjectGroup().getTownBlocks().size() + ") set to %s", type))) {
							TownyMessaging.sendErrorMsg(player, Translatable.of("msg_err_cannot_afford_plot_set_type_cost", type, TownyEconomyHandler.getFormattedBalance(finalCost)));
							return;
						}					

						TownyMessaging.sendMsg(resident, Translatable.of("msg_plot_set_cost", TownyEconomyHandler.getFormattedBalance(finalCost), type));

						for (TownBlock tb : townBlock.getPlotObjectGroup().getTownBlocks()) {
							try {
								tb.setType(type, resident);
							} catch (TownyException ignored) {
								// Cannot be set to jail type as a group.
							}
						}
						TownyMessaging.sendMsg(player, Translatable.of("msg_set_group_type_to_x", type));
						
					})
						.setTitle(Translatable.of("msg_confirm_purchase", TownyEconomyHandler.getFormattedBalance(cost)))
						.sendTo(BukkitTools.getPlayerExact(resident.getName()));
				
				// No cost or economy so no confirmation.
				} else {

					for (TownBlock tb : townBlock.getPlotObjectGroup().getTownBlocks())
						tb.setType(type, resident);
					TownyMessaging.sendMsg(player, Translatable.of("msg_set_group_type_to_x", plotTypeName));

				}

				
			} catch (TownyException e) {
				TownyMessaging.sendErrorMsg(resident, e.getMessage(player));
			}
		} else if (split[0].equalsIgnoreCase("trust")) {
			if (!permSource.testPermission(player, PermissionNodes.TOWNY_COMMAND_PLOT_GROUP_TRUST.getNode()))
				throw new TownyException(Translatable.of("msg_err_command_disable"));
			
			if (split.length < 3) {
				HelpMenu.PLOT_GROUP_TRUST_HELP.send(player);
				return true;
			}

			PlotGroup group = townBlock.getPlotObjectGroup();

			if (group == null) {
				TownyMessaging.sendErrorMsg(player, Translatable.of("msg_err_plot_not_associated_with_a_group"));
				return false;
			}

			Resident trustedResident = TownyAPI.getInstance().getResident(split[2]);
			if (trustedResident == null || trustedResident.isNPC()) {
				TownyMessaging.sendErrorMsg(player, Translatable.of("msg_err_not_registered_1", split[2]));
				return false;
			}

			if (split[1].equalsIgnoreCase("add")) {
				if (group.hasTrustedResident(trustedResident)) {
					TownyMessaging.sendErrorMsg(player, Translatable.of("msg_already_trusted", trustedResident.getName(), Translatable.of("plotgroup_sing")));
					return false;
				}

				PlotTrustAddEvent event = new PlotTrustAddEvent(new ArrayList<>(group.getTownBlocks()), trustedResident, player);
				Bukkit.getPluginManager().callEvent(event);

				if (event.isCancelled()) {
					TownyMessaging.sendErrorMsg(player, event.getCancelMessage());
					return false;
				}

				group.addTrustedResident(trustedResident);
				plugin.deleteCache(trustedResident);

				TownyMessaging.sendMsg(player, Translatable.of("msg_trusted_added", trustedResident.getName(), Translatable.of("plotgroup_sing")));
				if (BukkitTools.isOnline(trustedResident.getName()) && !trustedResident.getName().equals(player.getName()))
					TownyMessaging.sendMsg(trustedResident, Translatable.of("msg_trusted_added_2", player.getName(), Translatable.of("plotgroup_sing"), group.getName()));

				return true;
			} else if (split[1].equalsIgnoreCase("remove")) {
				if (!group.hasTrustedResident(trustedResident)) {
					TownyMessaging.sendErrorMsg(player, Translatable.of("msg_not_trusted", trustedResident.getName(), Translatable.of("plotgroup_sing")));
					return false;
				}

				PlotTrustRemoveEvent event = new PlotTrustRemoveEvent(new ArrayList<>(group.getTownBlocks()), trustedResident, player);
				Bukkit.getPluginManager().callEvent(event);

				if (event.isCancelled()) {
					TownyMessaging.sendErrorMsg(player, event.getCancelMessage());
					return false;
				}

				group.removeTrustedResident(trustedResident);
				plugin.deleteCache(trustedResident);

				TownyMessaging.sendMsg(player, Translatable.of("msg_trusted_removed", trustedResident.getName(), Translatable.of("plotgroup_sing")));
				if (BukkitTools.isOnline(trustedResident.getName()) && !trustedResident.getName().equals(player.getName()))
					TownyMessaging.sendMsg(trustedResident, Translatable.of("msg_trusted_removed_2", player.getName(), Translatable.of("plotgroup_sing"), group.getName()));

				return true;
			} else {
				TownyMessaging.sendErrorMsg(player, Translatable.of("msg_err_invalid_property", split[1]));
				return false;
			}

		} else if (split[0].equalsIgnoreCase("perm")) {
			if (!permSource.testPermission(player, PermissionNodes.TOWNY_COMMAND_PLOT_GROUP_PERM.getNode()))
				throw new TownyException(Translatable.of("msg_err_command_disable"));
			
			if (split.length < 2) {
				HelpMenu.PLOT_GROUP_PERM_HELP.send(player);
				return true;
			}

			PlotGroup group = townBlock.getPlotObjectGroup();

			if (group == null) {
				TownyMessaging.sendErrorMsg(player, Translatable.of("msg_err_plot_not_associated_with_a_group"));
				return false;
			}
			
			if (split[1].equalsIgnoreCase("gui")) {
				PermissionGUIUtil.openPermissionGUI(resident, townBlock);
			} else {
				if (split.length < 3) {
					HelpMenu.PLOT_GROUP_PERM_HELP.send(player);
					return true;
				}
				
				Resident overrideResident = TownyAPI.getInstance().getResident(split[2]);
				if (overrideResident == null || overrideResident.isNPC()) {
					TownyMessaging.sendErrorMsg(player, Translatable.of("msg_err_not_registered_1", split[2]));
					return true;
				}

				if (split[1].equalsIgnoreCase("add")) {
					if (group.getPermissionOverrides() != null && group.getPermissionOverrides().containsKey(overrideResident)) {
						TownyMessaging.sendErrorMsg(player, Translatable.of("msg_overrides_already_set", overrideResident.getName(), Translatable.of("plotgroup_sing")));
						return true;
					}

					group.putPermissionOverride(overrideResident, new PermissionData(PermissionGUIUtil.getDefaultTypes(), player.getName()));

					TownyMessaging.sendMsg(player, Translatable.of("msg_overrides_added", overrideResident.getName()));
				} else if (split[1].equalsIgnoreCase("remove")) {
					if (group.getPermissionOverrides() != null && !group.getPermissionOverrides().containsKey(overrideResident)) {
						TownyMessaging.sendErrorMsg(player, Translatable.of("msg_no_overrides_set", overrideResident.getName(), Translatable.of("plotgroup_sing")));
						return true;
					}
					
					group.removePermissionOverride(overrideResident);

					TownyMessaging.sendMsg(player, Translatable.of("msg_overrides_removed", overrideResident.getName()));
				} else {
					TownyMessaging.sendErrorMsg(player, Translatable.of("msg_err_invalid_property", split[1]));
					return false;
				}
			}
		} else {

			HelpMenu.PLOT_GROUP_HELP.send(player);

			if (townBlock.hasPlotObjectGroup())
				TownyMessaging.sendMsg(player, Translatable.of("status_plot_group_name_and_size", townBlock.getPlotObjectGroup().getName(), townBlock.getPlotObjectGroup().getTownBlocks().size()));
			
			return true;
		}
		
		return false;
	}

	private void createOrAddOnToPlotGroup(TownBlock townBlock, Town town, String plotGroupName) {
		PlotGroup newGroup = null;
		
		// Don't add the group to the town data if it's already there.
		if (town.hasPlotGroupName(plotGroupName)) {
			newGroup = town.getPlotObjectGroupFromName(plotGroupName);
			townBlock.setPermissions(newGroup.getPermissions().toString());
			townBlock.setChanged(!townBlock.getPermissions().toString().equals(town.getPermissions().toString()));
		} else {			
			// This is a brand new PlotGroup, register it.
			newGroup = new PlotGroup(UUID.randomUUID(), plotGroupName, town);
			TownyUniverse.getInstance().registerGroup(newGroup);
			newGroup.setPermissions(townBlock.getPermissions());
			newGroup.setTrustedResidents(townBlock.getTrustedResidents());
			newGroup.setPermissionOverrides(townBlock.getPermissionOverrides());
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

	public static void parsePlotTrustCommand(Player player, String[] args) {
		if (args.length < 2) {
			HelpMenu.PLOT_TRUST_HELP.send(player);
			return;
		}

		TownBlock townBlock = WorldCoord.parseWorldCoord(player).getTownBlockOrNull();
		if (townBlock == null) {
			TownyMessaging.sendErrorMsg(player, Translatable.of("msg_not_claimed_1"));
			return;
		}

		try {
			// Test we are allowed to work on this plot
			// If this fails it will trigger a TownyException.
			plotTestOwner(TownyAPI.getInstance().getResident(player.getName()), townBlock);
		} catch (TownyException e) {
			TownyMessaging.sendErrorMsg(player, e.getMessage(player));
			return;
		}

		if (townBlock.hasPlotObjectGroup()) {
			TownyMessaging.sendErrorMsg(player, Translatable.of("msg_err_plot_belongs_to_group", "/plot group trust"));
			return;
		}

		Resident resident = TownyAPI.getInstance().getResident(args[1]);
		if (resident == null || resident.isNPC()) {
			TownyMessaging.sendErrorMsg(player, Translatable.of("msg_err_not_registered_1", args[1]));
			return;
		}

		if (args[0].equalsIgnoreCase("add")) {
			if (townBlock.hasTrustedResident(resident)) {
				TownyMessaging.sendErrorMsg(player, Translatable.of("msg_already_trusted", resident.getName(), Translatable.of("townblock")));
				return;
			}

			PlotTrustAddEvent event = new PlotTrustAddEvent(townBlock, resident, player);
			Bukkit.getPluginManager().callEvent(event);
			
			if (event.isCancelled()) {
				TownyMessaging.sendErrorMsg(player, event.getCancelMessage());
				return;
			}

			townBlock.addTrustedResident(resident);
			plugin.deleteCache(resident);

			TownyMessaging.sendMsg(player, Translatable.of("msg_trusted_added", resident.getName(), Translatable.of("townblock")));
			if (BukkitTools.isOnline(resident.getName()) && !resident.getName().equals(player.getName()))
				TownyMessaging.sendMsg(resident, Translatable.of("msg_trusted_added_2", player.getName(), Translatable.of("townblock"), townBlock.getWorldCoord().getCoord().toString()));
		} else if (args[0].equalsIgnoreCase("remove")) {
			if (!townBlock.hasTrustedResident(resident)) {
				TownyMessaging.sendErrorMsg(player, Translatable.of("msg_not_trusted", resident.getName(), Translatable.of("townblock")));
				return;
			}

			PlotTrustRemoveEvent event = new PlotTrustRemoveEvent(townBlock, resident, player);
			Bukkit.getPluginManager().callEvent(event);
			
			if (event.isCancelled()) {
				TownyMessaging.sendErrorMsg(player, event.getCancelMessage());
				return;
			}

			townBlock.removeTrustedResident(resident);
			plugin.deleteCache(resident);

			TownyMessaging.sendMsg(player, Translatable.of("msg_trusted_removed", resident.getName(), Translatable.of("townblock")));
			if (BukkitTools.isOnline(resident.getName()) && !resident.getName().equals(player.getName()))
				TownyMessaging.sendMsg(resident, Translatable.of("msg_trusted_removed_2", player.getName(), Translatable.of("townblock"), townBlock.getWorldCoord().getCoord().toString()));
		} else {
			TownyMessaging.sendErrorMsg(player, Translatable.of("msg_err_invalid_property", args[0]));
			return;
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
						throw new TownyException(Translatable.of("msg_no_funds_claim_plot_group", group.getTownBlocks().size(), TownyEconomyHandler.getFormattedBalance(group.getPrice())));

					// Add the confirmation for claiming a plot group.
					Confirmation.runOnAccept(() -> {
						ArrayList<WorldCoord> coords = new ArrayList<>();

						// Get worldcoords from plot group.
						group.getTownBlocks().forEach((tblock) -> coords.add(tblock.getWorldCoord()));

						// Execute the plot claim.
						new PlotClaim(Towny.getPlugin(), player, resident, coords, true, false, true).start();
					})
					.setTitle(Translatable.of("msg_plot_group_claim_confirmation", group.getTownBlocks().size()).append(" " + TownyEconomyHandler.getFormattedBalance(group.getPrice())).append(". " + Translatable.of("are_you_sure_you_want_to_continue")))
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
			throw new TownyException(Translatable.of("msg_no_funds_claim_plot", TownyEconomyHandler.getFormattedBalance(cost)));

		if (cost != 0) {
			final List<WorldCoord> finalSelection = selection;
			Confirmation.runOnAccept(() ->  {	
				// Start the claim task
				new PlotClaim(plugin, player, resident, finalSelection, true, false, false).start();
			})
			.setTitle(Translatable.of("msg_confirm_purchase", TownyEconomyHandler.getFormattedBalance(cost)))
			.sendTo(player);
		} else {
			// Start the claim task
			new PlotClaim(plugin, player, resident, selection, true, false, false).start();
		}
	}
		
	public void parsePlotPermCommand(Player player, String[] args) throws TownyException {
		if (args.length == 0) {
			if (!TownyUniverse.getInstance().getPermissionSource().testPermission(player, PermissionNodes.TOWNY_COMMAND_PLOT_PERM_INFO.getNode()))
				throw new TownyException(Translatable.of("msg_err_command_disable"));

			sendPlotInfo(player, args);
			return;
		}

		if (!TownyUniverse.getInstance().getPermissionSource().testPermission(player, PermissionNodes.TOWNY_COMMAND_PLOT_PERM.getNode(args[0].toLowerCase())))
			throw new TownyException(Translatable.of("msg_err_command_disable"));
		
		if (args[0].equalsIgnoreCase("hud")) {
			HUDManager.togglePermHUD(player);
		} else {
			// All other subcommands require the player to be in a claimed area
			TownBlock townBlock = TownyAPI.getInstance().getTownBlock(player);
			if (townBlock == null) {
				TownyMessaging.sendErrorMsg(player, Translatable.of("msg_not_claimed_1"));
				return;
			}
			
			if (args[0].equalsIgnoreCase("gui")) {
				Resident resident = TownyAPI.getInstance().getResident(player.getName());
				if (resident == null) {
					TownyMessaging.sendErrorMsg(player, Translatable.of("msg_err_not_registered"));
					return;
				}

				PermissionGUIUtil.openPermissionGUI(resident, townBlock);
			} else if (args[0].equalsIgnoreCase("remove")) {
				if (args.length < 2) {
					HelpMenu.PLOT_PERM_HELP.send(player);
					return;
				}
				
				try {
					plotTestOwner(getResidentOrThrow(player.getUniqueId()), townBlock);
				} catch (TownyException e) {
					TownyMessaging.sendErrorMsg(player, e.getMessage(player));
					return;
				}

				if (townBlock.hasPlotObjectGroup()) {
					TownyMessaging.sendErrorMsg(player, Translatable.of("msg_err_plot_belongs_to_group", "/plot group perm remove"));
					return;
				}
				
				Resident resident = TownyAPI.getInstance().getResident(args[1]);
				if (resident == null) {
					TownyMessaging.sendErrorMsg(player, Translatable.of("msg_err_not_registered_1", args[1]));
					return;
				}
				
				if (!townBlock.getPermissionOverrides().containsKey(resident)) {
					TownyMessaging.sendErrorMsg(player, Translatable.of("msg_no_overrides_set", resident.getName(), Translatable.of("townblock")));
					return;
				}
				
				townBlock.getPermissionOverrides().remove(resident);
				townBlock.save();
				
				TownyMessaging.sendMsg(player, Translatable.of("msg_overrides_removed", resident.getName()));
			} else if (args[0].equalsIgnoreCase("add")) {
				if (args.length < 2) {
					HelpMenu.PLOT_PERM_HELP.send(player);
					return;
				}

				try {
					plotTestOwner(getResidentOrThrow(player.getUniqueId()), townBlock);
				} catch (TownyException e) {
					TownyMessaging.sendErrorMsg(player, e.getMessage(player));
					return;
				}

				if (townBlock.hasPlotObjectGroup()) {
					TownyMessaging.sendErrorMsg(player, Translatable.of("msg_err_plot_belongs_to_group", "/plot group perm add"));
					return;
				}

				Resident resident = TownyAPI.getInstance().getResident(args[1]);
				if (resident == null || resident.isNPC()) {
					TownyMessaging.sendErrorMsg(player, Translatable.of("msg_err_not_registered_1", args[1]));
					return;
				}
				
				if (townBlock.getPermissionOverrides().containsKey(resident)) {
					TownyMessaging.sendErrorMsg(player, Translatable.of("msg_overrides_already_set", resident.getName(), Translatable.of("townblock")));
					return;
				}
				
				townBlock.getPermissionOverrides().put(resident, new PermissionData(PermissionGUIUtil.getDefaultTypes(), player.getName()));
				townBlock.save();
				
				TownyMessaging.sendMsg(player, Translatable.of("msg_overrides_added", resident.getName()));
			}
		}
	}
	
	public void sendPlotInfo(Player player, String[] args) {
		WorldCoord coord = WorldCoord.parseWorldCoord(player);
		String world = player.getWorld().getName();

		try {
			coord = new WorldCoord(world, Integer.parseInt(args[0]), Integer.parseInt(args[1]));
		} catch (NumberFormatException | ArrayIndexOutOfBoundsException ignored) {
		}

		if (TownyAPI.getInstance().isWilderness(coord))
			TownyMessaging.sendStatusScreen(player, TownyFormatter.getStatus(coord.getTownyWorldOrNull(), player));
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
}
