package com.palmergames.bukkit.towny.command;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;

import com.google.common.collect.Sets.SetView;
import com.palmergames.bukkit.towny.Towny;
import com.palmergames.bukkit.towny.TownyEconomyHandler;
import com.palmergames.bukkit.towny.TownyFormatter;
import com.palmergames.bukkit.towny.TownyMessaging;
import com.palmergames.bukkit.towny.TownySettings;
import com.palmergames.bukkit.towny.exceptions.EconomyException;
import com.palmergames.bukkit.towny.exceptions.NotRegisteredException;
import com.palmergames.bukkit.towny.exceptions.TownyException;
import com.palmergames.bukkit.towny.object.Coord;
import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.towny.object.Town;
import com.palmergames.bukkit.towny.object.TownBlock;
import com.palmergames.bukkit.towny.object.TownBlockOwner;
import com.palmergames.bukkit.towny.object.TownBlockType;
import com.palmergames.bukkit.towny.object.TownyUniverse;
import com.palmergames.bukkit.towny.object.WorldCoord;
import com.palmergames.bukkit.towny.permissions.PermissionNodes;
import com.palmergames.bukkit.towny.regen.TownyRegenAPI;
import com.palmergames.bukkit.towny.tasks.PlotClaim;
import com.palmergames.bukkit.towny.utils.AreaSelectionUtil;
import com.palmergames.bukkit.util.ChatTools;
import com.palmergames.bukkit.util.Colors;
import com.palmergames.util.StringMgmt;

/**
 * Send a list of all general towny plot help commands to player Command: /plot
 */

public class PlotCommand implements TabExecutor {

	private static Towny plugin;
	public static final List<String> output = new ArrayList<String>();
 	public static final List<String> subCommands = new ArrayList<String>();
 	public static final List<String> toggles = new ArrayList<String>();
 	public static final List<String> setVars = new ArrayList<String>();

	static {
		output.add(ChatTools.formatTitle("/plot"));
		output.add(ChatTools.formatCommand(TownySettings.getLangString("res_sing"), "/plot claim", "", TownySettings.getLangString("msg_block_claim")));
		output.add(ChatTools.formatCommand(TownySettings.getLangString("res_sing"), "/plot claim", "[rect/circle] [radius]", ""));
		output.add(ChatTools.formatCommand(TownySettings.getLangString("res_sing") + "/" + TownySettings.getLangString("mayor_sing"), "/plot notforsale", "", TownySettings.getLangString("msg_plot_nfs")));
		output.add(ChatTools.formatCommand(TownySettings.getLangString("res_sing") + "/" + TownySettings.getLangString("mayor_sing"), "/plot notforsale", "[rect/circle] [radius]", ""));
		output.add(ChatTools.formatCommand(TownySettings.getLangString("res_sing") + "/" + TownySettings.getLangString("mayor_sing"), "/plot forsale [$]", "", TownySettings.getLangString("msg_plot_fs")));
		output.add(ChatTools.formatCommand(TownySettings.getLangString("res_sing") + "/" + TownySettings.getLangString("mayor_sing"), "/plot forsale [$]", "within [rect/circle] [radius]", ""));
		output.add(ChatTools.formatCommand(TownySettings.getLangString("res_sing") + "/" + TownySettings.getLangString("mayor_sing"), "/plot clear", "", ""));
		output.add(ChatTools.formatCommand(TownySettings.getLangString("res_sing") + "/" + TownySettings.getLangString("mayor_sing"), "/plot set ...", "", TownySettings.getLangString("msg_plot_fs")));
		output.add(ChatTools.formatCommand(TownySettings.getLangString("res_sing"), "/plot toggle", "[pvp/fire/explosion/mobs]", ""));
		output.add(TownySettings.getLangString("msg_nfs_abr"));
		
		subCommands.add("claim");
		subCommands.add("unclaim");
		subCommands.add("forsale");
		subCommands.add("fs");
		subCommands.add("notforsale");
		subCommands.add("nfs");
		subCommands.add("toggle");
		subCommands.add("set");
		subCommands.add("clear");
		
		toggles.add("mobs");
		toggles.add("explosion");
		toggles.add("fire");
		toggles.add("pvp");
		
		setVars.add("perm");
		setVars.add("reset");
		setVars.add("shop");
		setVars.add("embassy");
		setVars.add("wilds");
		setVars.add("arena");
		setVars.add("spleef");
	}

	public PlotCommand(Towny instance) {

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
			} else {
				try {
					return parsePlotCommand(player, args);
				} catch (TownyException x) {
					// No permisisons
					TownyMessaging.sendErrorMsg(player, x.getMessage());
				}
			}

		} else
			// Console
			for (String line : output)
				sender.sendMessage(Colors.strip(line));
		return true;
	}

	public boolean parsePlotCommand(Player player, String[] split) throws TownyException {

		if (split.length == 0 || split[0].equalsIgnoreCase("?")) {
			for (String line : output)
				player.sendMessage(line);
		} else {

			Resident resident;
			String world;

			try {
				resident = TownyUniverse.getDataSource().getResident(player.getName());
				world = player.getWorld().getName();
				resident.getTown();
			} catch (TownyException x) {
				TownyMessaging.sendErrorMsg(player, x.getMessage());
				return true;
			}

			try {
				if (split[0].equalsIgnoreCase("claim")) {

					if (!TownyUniverse.getPermissionSource().testPermission(player, PermissionNodes.TOWNY_COMMAND_PLOT_CLAIM.getNode()))
						throw new TownyException(TownySettings.getLangString("msg_err_command_disable"));

					if (TownyUniverse.isWarTime())
						throw new TownyException(TownySettings.getLangString("msg_war_cannot_do"));

					List<WorldCoord> selection = AreaSelectionUtil.selectWorldCoordArea(resident, new WorldCoord(world, Coord.parseCoord(player)), StringMgmt.remFirstArg(split));
					// selection = TownyUtil.filterUnownedPlots(selection);

					if (selection.size() > 0) {

						double cost = 0;

						// Remove any plots Not for sale (if not the mayor) and
						// tally up costs.
						for (WorldCoord worldCoord : new ArrayList<WorldCoord>(selection)) {
							try {
								double price = worldCoord.getTownBlock().getPlotPrice();
								if (price > -1)
									cost += worldCoord.getTownBlock().getPlotPrice();
								else {
									if (!worldCoord.getTownBlock().getTown().isMayor(resident)) // ||
																								// worldCoord.getTownBlock().getTown().hasAssistant(resident))
										selection.remove(worldCoord);
								}
							} catch (NotRegisteredException e) {
								selection.remove(worldCoord);
							}
						}

						int maxPlots = TownySettings.getMaxResidentPlots(resident);
						if (maxPlots >= 0 && resident.getTownBlocks().size() + selection.size() > maxPlots)
							throw new TownyException(String.format(TownySettings.getLangString("msg_max_plot_own"), maxPlots));

						if (TownySettings.isUsingEconomy() && (!resident.canPayFromHoldings(cost)))
							throw new TownyException(String.format(TownySettings.getLangString("msg_no_funds_claim"), selection.size(), TownyEconomyHandler.getFormattedBalance(cost)));

						// Start the claim task
						new PlotClaim(plugin, player, resident, selection, true).start();

					} else {
						player.sendMessage(TownySettings.getLangString("msg_err_empty_area_selection"));
					}

				} else if (split[0].equalsIgnoreCase("unclaim")) {

					if (!TownyUniverse.getPermissionSource().testPermission(player, PermissionNodes.TOWNY_COMMAND_PLOT_UNCLAIM.getNode()))
						throw new TownyException(TownySettings.getLangString("msg_err_command_disable"));

					if (TownyUniverse.isWarTime())
						throw new TownyException(TownySettings.getLangString("msg_war_cannot_do"));

					if (split.length == 2 && split[1].equalsIgnoreCase("all")) {
						// Start the unclaim task
						new PlotClaim(plugin, player, resident, null, false).start();

					} else {
						List<WorldCoord> selection = AreaSelectionUtil.selectWorldCoordArea(resident, new WorldCoord(world, Coord.parseCoord(player)), StringMgmt.remFirstArg(split));
						selection = AreaSelectionUtil.filterOwnedBlocks(resident, selection);

						if (selection.size() > 0) {

							// Start the unclaim task
							new PlotClaim(plugin, player, resident, selection, false).start();

						} else {
							player.sendMessage(TownySettings.getLangString("msg_err_empty_area_selection"));
						}
					}

				} else if (split[0].equalsIgnoreCase("notforsale") || split[0].equalsIgnoreCase("nfs")) {

					if (!TownyUniverse.getPermissionSource().testPermission(player, PermissionNodes.TOWNY_COMMAND_PLOT_NOTFORSALE.getNode()))
						throw new TownyException(TownySettings.getLangString("msg_err_command_disable"));

					List<WorldCoord> selection = AreaSelectionUtil.selectWorldCoordArea(resident, new WorldCoord(world, Coord.parseCoord(player)), StringMgmt.remFirstArg(split));
					selection = AreaSelectionUtil.filterOwnedBlocks(resident.getTown(), selection);

					for (WorldCoord worldCoord : selection) {
						setPlotForSale(resident, worldCoord, -1);
					}

				} else if (split[0].equalsIgnoreCase("forsale") || split[0].equalsIgnoreCase("fs")) {

					if (!TownyUniverse.getPermissionSource().testPermission(player, PermissionNodes.TOWNY_COMMAND_PLOT_FORSALE.getNode()))
						throw new TownyException(TownySettings.getLangString("msg_err_command_disable"));

					WorldCoord pos = new WorldCoord(world, Coord.parseCoord(player));
					double plotPrice = pos.getTownBlock().getTown().getPlotTypePrice(pos.getTownBlock().getType());

					if (split.length > 1) {

						int areaSelectPivot = AreaSelectionUtil.getAreaSelectPivot(split);
						List<WorldCoord> selection;
						if (areaSelectPivot >= 0) {
							selection = AreaSelectionUtil.selectWorldCoordArea(resident, new WorldCoord(world, Coord.parseCoord(player)), StringMgmt.subArray(split, areaSelectPivot + 1, split.length));
							selection = AreaSelectionUtil.filterOwnedBlocks(resident.getTown(), selection);
							if (selection.size() == 0) {
								player.sendMessage(TownySettings.getLangString("msg_err_empty_area_selection"));
								return true;
							}
						} else {
							selection = new ArrayList<WorldCoord>();
							selection.add(pos);
						}

						// Check that it's not: /plot forsale within rect 3
						if (areaSelectPivot != 1) {
							try {
								// command was 'plot fs $'
								plotPrice = Double.parseDouble(split[1]);
								if (plotPrice < 0) {
									TownyMessaging.sendErrorMsg(player, TownySettings.getLangString("msg_err_negative_money"));
									return true;
								}
							} catch (NumberFormatException e) {
								player.sendMessage(String.format(TownySettings.getLangString("msg_error_must_be_num")));
								return true;
							}
						}

						for (WorldCoord worldCoord : selection) {
							if (selection.size() > 1)
								plotPrice = worldCoord.getTownBlock().getTown().getPlotTypePrice(worldCoord.getTownBlock().getType());

							setPlotForSale(resident, worldCoord, plotPrice);
						}
					} else {
						// basic 'plot fs' command
						setPlotForSale(resident, pos, plotPrice);
					}

				} else if (split[0].equalsIgnoreCase("perm")) {

					if (!TownyUniverse.getPermissionSource().testPermission(player, PermissionNodes.TOWNY_COMMAND_PLOT_PERM.getNode()))
						throw new TownyException(TownySettings.getLangString("msg_err_command_disable"));

					TownBlock townBlock = new WorldCoord(world, Coord.parseCoord(player)).getTownBlock();
					TownyMessaging.sendMessage(player, TownyFormatter.getStatus(townBlock));

				} else if (split[0].equalsIgnoreCase("toggle")) {

					/*
					 * perm test in the plottoggle.
					 */

					TownBlock townBlock = new WorldCoord(world, Coord.parseCoord(player)).getTownBlock();
					// Test we are allowed to work on this plot
					plotTestOwner(resident, townBlock); // ignore the return as
														// we are only checking
														// for an exception

					plotToggle(player, new WorldCoord(world, Coord.parseCoord(player)).getTownBlock(), StringMgmt.remFirstArg(split));

				} else if (split[0].equalsIgnoreCase("set")) {

					split = StringMgmt.remFirstArg(split);

					if (split.length > 0) {
						
						if (!TownyUniverse.getPermissionSource().testPermission(player, PermissionNodes.TOWNY_COMMAND_PLOT_SET.getNode(split[0].toLowerCase())))
							throw new TownyException(TownySettings.getLangString("msg_err_command_disable"));
						
						if (split[0].equalsIgnoreCase("perm")) {

							// Set plot level permissions (if the plot owner) or
							// Mayor/Assistant of the town.

							TownBlock townBlock = new WorldCoord(world, Coord.parseCoord(player)).getTownBlock();
							// Test we are allowed to work on this plot
							TownBlockOwner owner = plotTestOwner(resident, townBlock);

							// Check we are allowed to set these perms
							toggleTest(player, townBlock, StringMgmt.join(StringMgmt.remFirstArg(split), ""));

							TownCommand.setTownBlockPermissions(player, owner, townBlock.getPermissions(), StringMgmt.remFirstArg(split), true);
							townBlock.setChanged(true);
							TownyUniverse.getDataSource().saveTownBlock(townBlock);
							return true;
						}

						WorldCoord worldCoord = new WorldCoord(world, Coord.parseCoord(player));
						TownBlock townBlock = worldCoord.getTownBlock();
						setPlotType(resident, worldCoord, split[0]);
						townBlock.setChanged(true);
						TownyUniverse.getDataSource().saveTownBlock(townBlock);
						player.sendMessage(String.format(TownySettings.getLangString("msg_plot_set_type"), split[0]));

					} else {
						player.sendMessage(ChatTools.formatCommand("", "/plot set", "reset", ""));
						player.sendMessage(ChatTools.formatCommand("", "/plot set", "shop|embassy|arena|wilds|spleef", ""));
						player.sendMessage(ChatTools.formatCommand("", "/plot set perm", "?", ""));
					}

				} else if (split[0].equalsIgnoreCase("clear")) {

					if (!TownyUniverse.getPermissionSource().testPermission(player, PermissionNodes.TOWNY_COMMAND_PLOT_CLEAR.getNode()))
						throw new TownyException(TownySettings.getLangString("msg_err_command_disable"));

					TownBlock townBlock = new WorldCoord(world, Coord.parseCoord(player)).getTownBlock();

					if (townBlock != null) {

						/**
						 * Only allow mayors or plot owners to use this command.
						 */
						if (townBlock.hasResident()) {
							if (!townBlock.isOwner(resident)) {
								player.sendMessage(TownySettings.getLangString("msg_area_not_own"));
								return true;
							}

						} else if (!townBlock.getTown().equals(resident.getTown())) {
							player.sendMessage(TownySettings.getLangString("msg_area_not_own"));
							return true;
						}

						for (String material : TownyUniverse.getDataSource().getWorld(world).getPlotManagementMayorDelete())
							if (Material.matchMaterial(material) != null) {
								TownyRegenAPI.deleteTownBlockMaterial(townBlock, Material.getMaterial(material).getId());
								player.sendMessage(String.format(TownySettings.getLangString("msg_clear_plot_material"), material));
							} else
								throw new TownyException(String.format(TownySettings.getLangString("msg_err_invalid_property"), material));

					} else {
						// Shouldn't ever reach here as a null townBlock should
						// be caught already in WorldCoord.
						player.sendMessage(TownySettings.getLangString("msg_err_empty_area_selection"));
					}

				} else
					throw new TownyException(String.format(TownySettings.getLangString("msg_err_invalid_property"), split[0]));

			} catch (TownyException x) {
				TownyMessaging.sendErrorMsg(player, x.getMessage());
			} catch (EconomyException x) {
				TownyMessaging.sendErrorMsg(player, x.getMessage());
			}
		}

		return true;
	}

	/**
	 * Set the plot type if we are permitted
	 * 
	 * @param resident
	 * @param worldCoord
	 * @param type
	 * @throws TownyException
	 */
	public void setPlotType(Resident resident, WorldCoord worldCoord, String type) throws TownyException {

		if (resident.hasTown())
			try {
				TownBlock townBlock = worldCoord.getTownBlock();

				// Test we are allowed to work on this plot
				plotTestOwner(resident, townBlock); // ignore the return as we
													// are only checking for an
													// exception

				townBlock.setType(type);

			} catch (NotRegisteredException e) {
				throw new TownyException(TownySettings.getLangString("msg_err_not_part_town"));
			}
		else
			throw new TownyException(TownySettings.getLangString("msg_err_must_belong_town"));
	}

	/**
	 * Set the plot for sale/not for sale if permitted
	 * 
	 * @param resident
	 * @param worldCoord
	 * @param forSale
	 * @throws TownyException
	 */
	public void setPlotForSale(Resident resident, WorldCoord worldCoord, double forSale) throws TownyException {

		if (resident.hasTown())
			try {
				TownBlock townBlock = worldCoord.getTownBlock();

				// Test we are allowed to work on this plot
				plotTestOwner(resident, townBlock); // ignore the return as we
													// are only checking for an
													// exception

				townBlock.setPlotPrice(forSale);

				if (forSale != -1)
					TownyMessaging.sendTownMessage(townBlock.getTown(), TownySettings.getPlotForSaleMsg(resident.getName(), worldCoord));
				else
					TownyUniverse.getPlayer(resident).sendMessage(TownySettings.getLangString("msg_err_plot_nfs"));

				// Save this townblocks town so the for sale status to
				// remembered.
				TownyUniverse.getDataSource().saveTown(townBlock.getTown());

			} catch (NotRegisteredException e) {
				throw new TownyException(TownySettings.getLangString("msg_err_not_part_town"));
			}
		else
			throw new TownyException(TownySettings.getLangString("msg_err_must_belong_town"));
	}

	/**
	 * Toggle the plots flags for pvp/explosion/fire/mobs (if town/world
	 * permissions allow)
	 * 
	 * @param player
	 * @param townBlock
	 * @param split
	 */
	public void plotToggle(Player player, TownBlock townBlock, String[] split) {

		if (split.length == 0) {
			player.sendMessage(ChatTools.formatTitle("/res toggle"));
			player.sendMessage(ChatTools.formatCommand("", "/res toggle", "pvp", ""));
			player.sendMessage(ChatTools.formatCommand("", "/res toggle", "explosion", ""));
			player.sendMessage(ChatTools.formatCommand("", "/res toggle", "fire", ""));
			player.sendMessage(ChatTools.formatCommand("", "/res toggle", "mobs", ""));
		} else {

			try {

				if (!TownyUniverse.getPermissionSource().testPermission(player, PermissionNodes.TOWNY_COMMAND_PLOT_TOGGLE.getNode(split[0].toLowerCase())))
					throw new TownyException(TownySettings.getLangString("msg_err_command_disable"));

				if (split[0].equalsIgnoreCase("pvp")) {
					// Make sure we are allowed to set these permissions.
					toggleTest(player, townBlock, StringMgmt.join(split, " "));
					townBlock.getPermissions().pvp = !townBlock.getPermissions().pvp;
					TownyMessaging.sendMessage(player, String.format(TownySettings.getLangString("msg_changed_pvp"), "Plot", townBlock.getPermissions().pvp ? "Enabled" : "Disabled"));

				} else if (split[0].equalsIgnoreCase("explosion")) {
					// Make sure we are allowed to set these permissions.
					toggleTest(player, townBlock, StringMgmt.join(split, " "));
					townBlock.getPermissions().explosion = !townBlock.getPermissions().explosion;
					TownyMessaging.sendMessage(player, String.format(TownySettings.getLangString("msg_changed_expl"), "the Plot", townBlock.getPermissions().explosion ? "Enabled" : "Disabled"));

				} else if (split[0].equalsIgnoreCase("fire")) {
					// Make sure we are allowed to set these permissions.
					toggleTest(player, townBlock, StringMgmt.join(split, " "));
					townBlock.getPermissions().fire = !townBlock.getPermissions().fire;
					TownyMessaging.sendMessage(player, String.format(TownySettings.getLangString("msg_changed_fire"), "the Plot", townBlock.getPermissions().fire ? "Enabled" : "Disabled"));

				} else if (split[0].equalsIgnoreCase("mobs")) {
					// Make sure we are allowed to set these permissions.
					toggleTest(player, townBlock, StringMgmt.join(split, " "));
					townBlock.getPermissions().mobs = !townBlock.getPermissions().mobs;
					TownyMessaging.sendMessage(player, String.format(TownySettings.getLangString("msg_changed_mobs"), "the Plot", townBlock.getPermissions().mobs ? "Enabled" : "Disabled"));

				} else {
					TownyMessaging.sendErrorMsg(player, String.format(TownySettings.getLangString("msg_err_invalid_property"), "plot"));
					return;
				}

				townBlock.setChanged(true);
				TownyUniverse.getDataSource().saveTownBlock(townBlock);

			} catch (Exception e) {
				TownyMessaging.sendErrorMsg(player, e.getMessage());
			}

			TownyUniverse.getDataSource().saveTownBlock(townBlock);
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
		Town town = townBlock.getTown();
		split = split.toLowerCase();

		if (split.contains("mobs")) {
			if (town.getWorld().isForceTownMobs())
				throw new TownyException(TownySettings.getLangString("msg_world_mobs"));
		}

		if (split.contains("fire")) {
			if (town.getWorld().isForceFire())
				throw new TownyException(TownySettings.getLangString("msg_world_fire"));
		}

		if (split.contains("explosion")) {
			if (town.getWorld().isForceExpl())
				throw new TownyException(TownySettings.getLangString("msg_world_expl"));
		}

		if (split.contains("pvp")) {
			if (town.getWorld().isForcePVP())
				throw new TownyException(TownySettings.getLangString("msg_world_pvp"));
		}
		if ((split.contains("pvp")) || (split.trim().equalsIgnoreCase("off"))) {
			if (townBlock.getType().equals(TownBlockType.ARENA))
				throw new TownyException(TownySettings.getLangString("msg_plot_pvp"));
		}
	}

	/**
	 * Test the townBlock to ensure we are either the plot owner, or the
	 * mayor/assistant
	 * 
	 * @param resident
	 * @param townBlock
	 * @throws TownyException
	 */
	public TownBlockOwner plotTestOwner(Resident resident, TownBlock townBlock) throws TownyException {

		if (townBlock.hasResident()) {
			Resident owner = townBlock.getResident();

			// If not the plot owner or the towns mayor
			if ((resident != owner) && (!townBlock.getTown().getMayor().equals(resident)) && (!townBlock.getTown().hasAssistant(resident)))
				throw new TownyException(TownySettings.getLangString("msg_area_not_own"));

			return owner;

		} else {
			Town owner = townBlock.getTown();

			if ((!owner.isMayor(resident)) && (!owner.hasAssistant(resident)))
				throw new TownyException(TownySettings.getLangString("msg_not_mayor_ass"));

			if ((resident.getTown() != owner))
				throw new TownyException(TownySettings.getLangString("msg_err_not_part_town"));

			return owner;
		}

	}

	public List<String> subCommandCompletion(String partial) {
		List<String> matches = new ArrayList<String>();
		for (String c: subCommands) {
			if (c.startsWith(partial.toLowerCase())) {
				matches.add(c);
			}
		}
		return matches;
	}
	
	public List<String> toggleCompletion(String partial) {
		List<String> matches = new ArrayList<String>();
		for (String t: toggles) {
			if (t.startsWith(partial.toLowerCase())) {
				matches.add(t);
			}
		}
		return matches;
	}
	
	public List<String> setCompletion(String partial) {
		List<String> matches = new ArrayList<String>();
		for (String s: setVars) {
			if (s.startsWith(partial.toLowerCase())) {
				matches.add(s);
			}
		}
		return matches;
	}
	
	@Override
	public List<String> onTabComplete(CommandSender sender, Command command,
			String alias, String[] args) {
		
		if (args.length == 1) {
			return subCommandCompletion(args[0]);
		} else if (args.length >= 2 && args[0].equalsIgnoreCase("toggle")) {
			if (args.length == 2) {
				if (args[1].equalsIgnoreCase("toggle")) {
					return toggleCompletion(args[1]);
				} else if (args[0].equalsIgnoreCase("set")) {
					return setCompletion(args[1]);
				}
			} else if (args.length == 3) {
				return PermCompletion.onOffCompletion(args[2]);
			}
		} else if (args.length >= 3 && args[0].equalsIgnoreCase("set") && args[1].equalsIgnoreCase("perm")) {
			return PermCompletion.handleSetPerm(args, "friend");
		}
		return null;
	}

}
