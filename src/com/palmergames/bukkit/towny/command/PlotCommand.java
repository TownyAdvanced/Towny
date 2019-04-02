package com.palmergames.bukkit.towny.command;

import com.palmergames.bukkit.towny.Towny;
import com.palmergames.bukkit.towny.TownyEconomyHandler;
import com.palmergames.bukkit.towny.TownyFormatter;
import com.palmergames.bukkit.towny.TownyMessaging;
import com.palmergames.bukkit.towny.TownySettings;
import com.palmergames.bukkit.towny.event.PlotClearEvent;
import com.palmergames.bukkit.towny.event.TownBlockSettingsChangedEvent;
import com.palmergames.bukkit.towny.exceptions.EconomyException;
import com.palmergames.bukkit.towny.exceptions.NotRegisteredException;
import com.palmergames.bukkit.towny.exceptions.TownyException;
import com.palmergames.bukkit.towny.object.Coord;
import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.towny.object.Town;
import com.palmergames.bukkit.towny.object.TownBlock;
import com.palmergames.bukkit.towny.object.TownBlockOwner;
import com.palmergames.bukkit.towny.object.TownBlockType;
import com.palmergames.bukkit.towny.object.TownyPermission;
import com.palmergames.bukkit.towny.object.TownyUniverse;
import com.palmergames.bukkit.towny.object.WorldCoord;
import com.palmergames.bukkit.towny.permissions.PermissionNodes;
import com.palmergames.bukkit.towny.regen.TownyRegenAPI;
import com.palmergames.bukkit.towny.tasks.PlotClaim;
import com.palmergames.bukkit.towny.utils.AreaSelectionUtil;
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
import java.util.LinkedList;
import java.util.List;

/**
 * Send a list of all general towny plot help commands to player Command: /plot
 */

public class PlotCommand extends BaseCommand implements CommandExecutor {

	private static Towny plugin;
	public static final List<String> output = new ArrayList<String>();

	static {
		output.add(ChatTools.formatTitle("/plot"));
		output.add(ChatTools.formatCommand(TownySettings.getLangString("res_sing"), "/plot claim", "", TownySettings.getLangString("msg_block_claim")));
		output.add(ChatTools.formatCommand(TownySettings.getLangString("res_sing"), "/plot claim", "[rect/circle] [radius]", ""));
		output.add(ChatTools.formatCommand(TownySettings.getLangString("res_sing"), "/plot perm", "[hud]", ""));
		output.add(ChatTools.formatCommand(TownySettings.getLangString("res_sing") + "/" + TownySettings.getLangString("mayor_sing"), "/plot notforsale", "", TownySettings.getLangString("msg_plot_nfs")));
		output.add(ChatTools.formatCommand(TownySettings.getLangString("res_sing") + "/" + TownySettings.getLangString("mayor_sing"), "/plot notforsale", "[rect/circle] [radius]", ""));
		output.add(ChatTools.formatCommand(TownySettings.getLangString("res_sing") + "/" + TownySettings.getLangString("mayor_sing"), "/plot forsale [$]", "", TownySettings.getLangString("msg_plot_fs")));
		output.add(ChatTools.formatCommand(TownySettings.getLangString("res_sing") + "/" + TownySettings.getLangString("mayor_sing"), "/plot forsale [$]", "within [rect/circle] [radius]", ""));
		output.add(ChatTools.formatCommand(TownySettings.getLangString("res_sing") + "/" + TownySettings.getLangString("mayor_sing"), "/plot evict", "" , ""));		
		output.add(ChatTools.formatCommand(TownySettings.getLangString("res_sing") + "/" + TownySettings.getLangString("mayor_sing"), "/plot clear", "", ""));
		output.add(ChatTools.formatCommand(TownySettings.getLangString("res_sing") + "/" + TownySettings.getLangString("mayor_sing"), "/plot set ...", "", TownySettings.getLangString("msg_plot_fs")));
		output.add(ChatTools.formatCommand(TownySettings.getLangString("res_sing"), "/plot toggle", "[pvp/fire/explosion/mobs]", ""));
		output.add(TownySettings.getLangString("msg_nfs_abr"));
	}

	public PlotCommand(Towny instance) {

		plugin = instance;
	}

	@Override
	public boolean onCommand(CommandSender sender, Command cmd, String commandLabel, String[] args) {

		if (sender instanceof Player) {
			Player player = (Player) sender;
			try {
				if (!TownyUniverse.getDataSource().getWorld(player.getWorld().getName()).isUsingTowny()) {
					TownyMessaging.sendErrorMsg(player, TownySettings.getLangString("msg_set_use_towny_off"));
					return false;
				}
			} catch (NotRegisteredException e) {
				// World not registered				
			}

			if (args == null) {
				for (String line : output)
					player.sendMessage(line);
			} else {
				try {
					return parsePlotCommand(player, args);
				} catch (TownyException x) {
					// No permisisons
					 x.getMessage();
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
				//resident.getTown();
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
						int extraPlots = TownySettings.getMaxResidentExtraPlots(resident);
						
						//Infinite plots
						if (maxPlots != -1) {
							maxPlots = maxPlots + extraPlots;
						}
						
						if (maxPlots >= 0 && resident.getTownBlocks().size() + selection.size() > maxPlots)
							throw new TownyException(String.format(TownySettings.getLangString("msg_max_plot_own"), maxPlots));

						if (TownySettings.isUsingEconomy() && (!resident.canPayFromHoldings(cost)))
							throw new TownyException(String.format(TownySettings.getLangString("msg_no_funds_claim"), selection.size(), TownyEconomyHandler.getFormattedBalance(cost)));

						// Start the claim task
						new PlotClaim(plugin, player, resident, selection, true).start();

					} else {
						player.sendMessage(TownySettings.getLangString("msg_err_empty_area_selection"));
					}
				} else if (split[0].equalsIgnoreCase("evict")) {

					if (!TownyUniverse.getPermissionSource().testPermission(player, PermissionNodes.TOWNY_COMMAND_PLOT_EVICT.getNode()))
						throw new TownyException(TownySettings.getLangString("msg_err_command_disable"));

					if (TownyUniverse.isWarTime())
						throw new TownyException(TownySettings.getLangString("msg_war_cannot_do"));
					
					if (!TownyUniverse.getPermissionSource().testPermission(player, PermissionNodes.TOWNY_COMMAND_PLOT_ASMAYOR.getNode()))
						throw new TownyException(TownySettings.getLangString("msg_err_command_disable"));					

					TownBlock townBlock = new WorldCoord(world, Coord.parseCoord(player)).getTownBlock();
					Town town = townBlock.getTown();										
					
					if (townBlock.getResident() == null) {
						
						TownyMessaging.sendErrorMsg(player, TownySettings.getLangString("msg_no_one_to_evict"));						
					} else {
						
						Resident owner = townBlock.getResident();
						if (!town.equals(resident.getTown())){ 
							
							TownyMessaging.sendErrorMsg(player, TownySettings.getLangString("msg_err_not_part_town"));
							return false;							
						}

						townBlock.setResident(null);
						townBlock.setPlotPrice(-1);

						// Set the plot permissions to mirror the towns.
						townBlock.setType(townBlock.getType());

						TownyUniverse.getDataSource().saveResident(owner);
						// Update the townBlock data file so it's no longer using custom settings.
						TownyUniverse.getDataSource().saveTownBlock(townBlock);
						
						player.sendMessage(TownySettings.getLangString("msg_plot_evict"));
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
					TownBlock townBlock = new WorldCoord(world, Coord.parseCoord(player)).getTownBlock();
					if (!townBlock.getType().equals(TownBlockType.EMBASSY)) 
						selection = AreaSelectionUtil.filterOwnedBlocks(resident.getTown(), selection);
					else
						selection = AreaSelectionUtil.filterOwnedBlocks(resident, selection);

					for (WorldCoord worldCoord : selection) {
						setPlotForSale(resident, worldCoord, -1);
					}
					
					if (selection.isEmpty()){
						throw new TownyException(TownySettings.getLangString("msg_area_not_own"));
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
							setPlotForSale(resident, worldCoord, plotPrice);
						}
					} else {
						// basic 'plot fs' command
						setPlotForSale(resident, pos, plotPrice);
					}

				} else if (split[0].equalsIgnoreCase("perm") || split[0].equalsIgnoreCase("info")) {

					if (!TownyUniverse.getPermissionSource().testPermission(player, PermissionNodes.TOWNY_COMMAND_PLOT_PERM.getNode()))
						throw new TownyException(TownySettings.getLangString("msg_err_command_disable"));

					if (split.length > 1 && split[1].equalsIgnoreCase("hud")) {
						
						if (!TownyUniverse.getPermissionSource().testPermission(player, PermissionNodes.TOWNY_COMMAND_PLOT_PERM_HUD.getNode()))
							throw new TownyException(TownySettings.getLangString("msg_err_command_disable"));
						
						plugin.getHUDManager().togglePermHUD(player);
						
					} else {
						TownBlock townBlock = new WorldCoord(world, Coord.parseCoord(player)).getTownBlock();
						TownyMessaging.sendMessage(player, TownyFormatter.getStatus(townBlock));
					}

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
					
					if (split.length == 0 || split[0].equalsIgnoreCase("?")) {

						player.sendMessage(ChatTools.formatTitle("/... set"));
						player.sendMessage(ChatTools.formatCommand("", "set", "[plottype]", "Ex: Inn, Wilds, Farm, Embassy etc"));
						player.sendMessage(ChatTools.formatCommand("", "set", "reset", "Removes a plot type"));
						player.sendMessage(ChatTools.formatCommand("", "set", "[name]", "Names a plot"));
						player.sendMessage(ChatTools.formatCommand("Level", "[resident/ally/outsider]", "", ""));
						player.sendMessage(ChatTools.formatCommand("Type", "[build/destroy/switch/itemuse]", "", ""));
						player.sendMessage(ChatTools.formatCommand("", "set perm", "[on/off]", "Toggle all permissions"));
						player.sendMessage(ChatTools.formatCommand("", "set perm", "[level/type] [on/off]", ""));
						player.sendMessage(ChatTools.formatCommand("", "set perm", "[level] [type] [on/off]", ""));
						player.sendMessage(ChatTools.formatCommand("", "set perm", "reset", ""));
						player.sendMessage(ChatTools.formatCommand("Eg", "/plot set perm", "ally off", ""));
						player.sendMessage(ChatTools.formatCommand("Eg", "/plot set perm", "friend build on", ""));
						player.sendMessage(String.format(TownySettings.getLangString("plot_perms"), "'friend'", "'resident'"));
						player.sendMessage(TownySettings.getLangString("plot_perms_1"));

					} else if (split.length > 0) {

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

							setTownBlockPermissions(player, owner, townBlock, StringMgmt.remFirstArg(split));

							return true;

						} else if (split[0].equalsIgnoreCase("name")) {

							TownBlock townBlock = new WorldCoord(world, Coord.parseCoord(player)).getTownBlock();
							// Test we are allowed to work on this plot
							plotTestOwner(resident, townBlock);
							if (split.length == 1) {
								townBlock.setName("");
								TownyMessaging.sendMsg(player, String.format(TownySettings.getLangString("msg_plot_name_removed")));
								TownyUniverse.getDataSource().saveTownBlock(townBlock);
								return true;
							}
							
							// Test if the plot name contains invalid characters.
							if (!NameValidation.isBlacklistName(split[1])) {								
								townBlock.setName(StringMgmt.join(StringMgmt.remFirstArg(split), ""));

								//townBlock.setChanged(true);
								TownyUniverse.getDataSource().saveTownBlock(townBlock);

								TownyMessaging.sendMsg(player, String.format(TownySettings.getLangString("msg_plot_name_set_to"), townBlock.getName()));

							} else {

								TownyMessaging.sendErrorMsg(player, TownySettings.getLangString("msg_invalid_name"));

							}
							return true;
						} 

						WorldCoord worldCoord = new WorldCoord(world, Coord.parseCoord(player));

						setPlotType(resident, worldCoord, split[0]);

						player.sendMessage(String.format(TownySettings.getLangString("msg_plot_set_type"), split[0]));

					} else {

						player.sendMessage(ChatTools.formatCommand("", "/plot set", "name", ""));
						player.sendMessage(ChatTools.formatCommand("", "/plot set", "reset", ""));
						player.sendMessage(ChatTools.formatCommand("", "/plot set", "shop|embassy|arena|wilds|spleef|inn|jail|farm|bank", ""));
						player.sendMessage(ChatTools.formatCommand("", "/plot set perm", "?", ""));
					}

				} else if (split[0].equalsIgnoreCase("clear")) {

					if (!TownyUniverse.getPermissionSource().testPermission(player, PermissionNodes.TOWNY_COMMAND_PLOT_CLEAR.getNode()))
						throw new TownyException(TownySettings.getLangString("msg_err_command_disable"));

					TownBlock townBlock = new WorldCoord(world, Coord.parseCoord(player)).getTownBlock();

					if (townBlock != null) {

						/*
						  Only allow mayors or plot owners to use this command.
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
								TownyRegenAPI.deleteTownBlockMaterial(townBlock, Material.getMaterial(material));
								player.sendMessage(String.format(TownySettings.getLangString("msg_clear_plot_material"), material));
							} else
								throw new TownyException(String.format(TownySettings.getLangString("msg_err_invalid_property"), material));

						// Raise an event for the claim
						BukkitTools.getPluginManager().callEvent(new PlotClearEvent(townBlock));

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

	public static void setTownBlockPermissions(Player player, TownBlockOwner townBlockOwner, TownBlock townBlock, String[] split) {

		if (split.length == 0 || split[0].equalsIgnoreCase("?")) {

			player.sendMessage(ChatTools.formatTitle("/... set perm"));
			player.sendMessage(ChatTools.formatCommand("Level", "[friend/ally/outsider]", "", ""));
			player.sendMessage(ChatTools.formatCommand("Type", "[build/destroy/switch/itemuse]", "", ""));
			player.sendMessage(ChatTools.formatCommand("", "set perm", "[on/off]", "Toggle all permissions"));
			player.sendMessage(ChatTools.formatCommand("", "set perm", "[level/type] [on/off]", ""));
			player.sendMessage(ChatTools.formatCommand("", "set perm", "[level] [type] [on/off]", ""));
			player.sendMessage(ChatTools.formatCommand("", "set perm", "reset", ""));
			player.sendMessage(ChatTools.formatCommand("Eg", "/plot set perm", "friend build on", ""));
			player.sendMessage(String.format(TownySettings.getLangString("plot_perms"), "'friend'", "'resident'"));
			player.sendMessage(TownySettings.getLangString("plot_perms_1"));

		} else {

			TownyPermission perm = townBlock.getPermissions();

			if (split.length == 1) {

				if (split[0].equalsIgnoreCase("reset")) {

					// reset this townBlock permissions (by town/resident)
					townBlock.setType(townBlock.getType());
					TownyUniverse.getDataSource().saveTownBlock(townBlock);

					if (townBlockOwner instanceof Town)
						TownyMessaging.sendMsg(player, String.format(TownySettings.getLangString("msg_set_perms_reset"), "Town owned"));
					else
						TownyMessaging.sendMsg(player, String.format(TownySettings.getLangString("msg_set_perms_reset"), "your"));

					// Reset all caches as this can affect everyone.
					plugin.resetCache();

					return;

				} else {

					// Set all perms to On or Off
					// '/plot set perm off'

					try {
						boolean b = plugin.parseOnOff(split[0]);
						for (String element : new String[] { "residentBuild",
								"residentDestroy", "residentSwitch",
								"residentItemUse", "outsiderBuild",
								"outsiderDestroy", "outsiderSwitch",
								"outsiderItemUse", "allyBuild", "allyDestroy",
								"allySwitch", "allyItemUse" })
							perm.set(element, b);
					} catch (Exception e) {
						TownyMessaging.sendErrorMsg(player, TownySettings.getLangString("msg_plot_set_perm_syntax_error"));
						return;
					}

				}

			} else if (split.length == 2) {
				if ((!split[0].equalsIgnoreCase("resident") 
						&& !split[0].equalsIgnoreCase("friend") 
						&& !split[0].equalsIgnoreCase("ally") 
						&& !split[0].equalsIgnoreCase("outsider")) 
						&& !split[0].equalsIgnoreCase("build")
						&& !split[0].equalsIgnoreCase("destroy")
						&& !split[0].equalsIgnoreCase("switch")
						&& !split[0].equalsIgnoreCase("itemuse")) {
					TownyMessaging.sendErrorMsg(player, TownySettings.getLangString("msg_plot_set_perm_syntax_error"));
					return;
				}

				try {

					boolean b = plugin.parseOnOff(split[1]);

					if (split[0].equalsIgnoreCase("friend")) {
						perm.residentBuild = b;
						perm.residentDestroy = b;
						perm.residentSwitch = b;
						perm.residentItemUse = b;
					} else if (split[0].equalsIgnoreCase("outsider")) {
						perm.outsiderBuild = b;
						perm.outsiderDestroy = b;
						perm.outsiderSwitch = b;
						perm.outsiderItemUse = b;
					} else if (split[0].equalsIgnoreCase("ally")) {
						perm.allyBuild = b;
						perm.allyDestroy = b;
						perm.allySwitch = b;
						perm.allyItemUse = b;
					} else if (split[0].equalsIgnoreCase("build")) {
						perm.residentBuild = b;
						perm.outsiderBuild = b;
						perm.allyBuild = b;
					} else if (split[0].equalsIgnoreCase("destroy")) {
						perm.residentDestroy = b;
						perm.outsiderDestroy = b;
						perm.allyDestroy = b;
					} else if (split[0].equalsIgnoreCase("switch")) {
						perm.residentSwitch = b;
						perm.outsiderSwitch = b;
						perm.allySwitch = b;
					} else if (split[0].equalsIgnoreCase("itemuse")) {
						perm.residentItemUse = b;
						perm.outsiderItemUse = b;
						perm.allyItemUse = b;
					}

				} catch (Exception e) {
					TownyMessaging.sendErrorMsg(player, TownySettings.getLangString("msg_plot_set_perm_syntax_error"));
					return;
				}

			} else if (split.length == 3) {
				if ((!split[0].equalsIgnoreCase("resident") 
						&& !split[0].equalsIgnoreCase("friend") 
						&& !split[0].equalsIgnoreCase("ally") 
						&& !split[0].equalsIgnoreCase("outsider")) 
						|| (!split[1].equalsIgnoreCase("build")
						&& !split[1].equalsIgnoreCase("destroy")
						&& !split[1].equalsIgnoreCase("switch")
						&& !split[1].equalsIgnoreCase("itemuse"))) {
					TownyMessaging.sendErrorMsg(player, TownySettings.getLangString("msg_plot_set_perm_syntax_error"));
					return;
				}

				// reset the friend to resident so the perm settings don't fail
				if (split[0].equalsIgnoreCase("friend"))
					split[0] = "resident";

				try {
					boolean b = plugin.parseOnOff(split[2]);
					String s = "";
					s = split[0] + split[1];
					perm.set(s, b);
				} catch (Exception e) {
					TownyMessaging.sendErrorMsg(player, TownySettings.getLangString("msg_plot_set_perm_syntax_error"));
					return;
				}

			}

			townBlock.setChanged(true);
			TownyUniverse.getDataSource().saveTownBlock(townBlock);

			TownyMessaging.sendMsg(player, TownySettings.getLangString("msg_set_perms"));
			TownyMessaging.sendMessage(player, (Colors.Green + " Perm: " + ((townBlockOwner instanceof Resident) ? perm.getColourString().replace("f", "r") : perm.getColourString())));
			TownyMessaging.sendMessage(player, Colors.Green + "PvP: " + ((perm.pvp) ? Colors.Red + "ON" : Colors.LightGreen + "OFF") + Colors.Green + "  Explosions: " + ((perm.explosion) ? Colors.Red + "ON" : Colors.LightGreen + "OFF") + Colors.Green + "  Firespread: " + ((perm.fire) ? Colors.Red + "ON" : Colors.LightGreen + "OFF") + Colors.Green + "  Mob Spawns: " + ((perm.mobs) ? Colors.Red + "ON" : Colors.LightGreen + "OFF"));

			//Change settings event
			TownBlockSettingsChangedEvent event = new TownBlockSettingsChangedEvent(townBlock);
			Bukkit.getServer().getPluginManager().callEvent(event);
			
			// Reset all caches as this can affect everyone.
			plugin.resetCache();
		}
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
				Town town = resident.getTown();
				if (townBlock.isJail())			
					town.addJailSpawn(TownyUniverse.getPlayer(resident).getLocation());				

				TownyUniverse.getDataSource().saveTownBlock(townBlock);

			} catch (NotRegisteredException e) {
				throw new TownyException(TownySettings.getLangString("msg_err_not_part_town"));
			}
		else if (!resident.hasTown()) {
			
			TownBlock townBlock = worldCoord.getTownBlock();

			// Test we are allowed to work on this plot
			plotTestOwner(resident, townBlock); // ignore the return as we
												// are only checking for an
												// exception
			townBlock.setType(type);		
			Town town = resident.getTown();
			if (townBlock.isJail())			
				town.addJailSpawn(TownyUniverse.getPlayer(resident).getLocation());				
			
			TownyUniverse.getDataSource().saveTownBlock(townBlock);
		
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
				if (forSale > TownySettings.getMaxPlotPrice() ) 
					townBlock.setPlotPrice(TownySettings.getMaxPlotPrice());
				else
					townBlock.setPlotPrice(forSale);

				if (forSale != -1) {
					TownyMessaging.sendTownMessage(townBlock.getTown(), TownySettings.getPlotForSaleMsg(resident.getName(), worldCoord));
					if (townBlock.getTown() != resident.getTown())
						TownyMessaging.sendMessage(resident, TownySettings.getPlotForSaleMsg(resident.getName(), worldCoord));
				} else
					TownyUniverse.getPlayer(resident).sendMessage(TownySettings.getLangString("msg_err_plot_nfs"));

				// Save this townblock so the for sale status is remembered.
				TownyUniverse.getDataSource().saveTownBlock(townBlock);

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
					TownyMessaging.sendMessage(player, String.format(TownySettings.getLangString("msg_changed_pvp"), "Plot", townBlock.getPermissions().pvp ? TownySettings.getLangString("enabled") : TownySettings.getLangString("disabled")));

				} else if (split[0].equalsIgnoreCase("explosion")) {
					// Make sure we are allowed to set these permissions.
					toggleTest(player, townBlock, StringMgmt.join(split, " "));
					townBlock.getPermissions().explosion = !townBlock.getPermissions().explosion;
					TownyMessaging.sendMessage(player, String.format(TownySettings.getLangString("msg_changed_expl"), "the Plot", townBlock.getPermissions().explosion ? TownySettings.getLangString("enabled") : TownySettings.getLangString("disabled")));

				} else if (split[0].equalsIgnoreCase("fire")) {
					// Make sure we are allowed to set these permissions.
					toggleTest(player, townBlock, StringMgmt.join(split, " "));
					townBlock.getPermissions().fire = !townBlock.getPermissions().fire;
					TownyMessaging.sendMessage(player, String.format(TownySettings.getLangString("msg_changed_fire"), "the Plot", townBlock.getPermissions().fire ? TownySettings.getLangString("enabled") : TownySettings.getLangString("disabled")));

				} else if (split[0].equalsIgnoreCase("mobs")) {
					// Make sure we are allowed to set these permissions.
					toggleTest(player, townBlock, StringMgmt.join(split, " "));
					townBlock.getPermissions().mobs = !townBlock.getPermissions().mobs;
					TownyMessaging.sendMessage(player, String.format(TownySettings.getLangString("msg_changed_mobs"), "the Plot", townBlock.getPermissions().mobs ? TownySettings.getLangString("enabled") : TownySettings.getLangString("disabled")));

				} else {
					TownyMessaging.sendErrorMsg(player, String.format(TownySettings.getLangString("msg_err_invalid_property"), "plot"));
					return;
				}

				townBlock.setChanged(true);

				//Change settings event
				TownBlockSettingsChangedEvent event = new TownBlockSettingsChangedEvent(townBlock);
				Bukkit.getServer().getPluginManager().callEvent(event);

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

		Player player = BukkitTools.getPlayer(resident.getName());
		boolean isAdmin = TownyUniverse.getPermissionSource().isTownyAdmin(player);

		if (townBlock.hasResident()) {
			
			Resident owner = townBlock.getResident();
			if ((!owner.hasTown() 
					&& (player.hasPermission(PermissionNodes.TOWNY_COMMAND_PLOT_ASMAYOR.getNode())))
					&& (townBlock.getTown() == resident.getTown()))				
					return owner;
					
			boolean isSameTown = (resident.hasTown()) ? resident.getTown() == owner.getTown() : false;			

			if ((resident == owner)
					|| ((isSameTown) && (player.hasPermission(PermissionNodes.TOWNY_COMMAND_PLOT_ASMAYOR.getNode())))
					|| ((townBlock.getTown() == resident.getTown())) && (player.hasPermission(PermissionNodes.TOWNY_COMMAND_PLOT_ASMAYOR.getNode()))
					|| isAdmin) {

				return owner;
			}

			// Not the plot owner or the towns mayor or an admin.
			throw new TownyException(TownySettings.getLangString("msg_area_not_own"));

		} else {

			Town owner = townBlock.getTown();
			boolean isSameTown = (resident.hasTown()) ? resident.getTown() == owner : false;

			if (isSameTown && !BukkitTools.getPlayer(resident.getName()).hasPermission(PermissionNodes.TOWNY_COMMAND_PLOT_ASMAYOR.getNode()))
				throw new TownyException(TownySettings.getLangString("msg_not_mayor_ass"));

			if (!isSameTown && !isAdmin)
				throw new TownyException(TownySettings.getLangString("msg_err_not_part_town"));

			return owner;
		}

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
