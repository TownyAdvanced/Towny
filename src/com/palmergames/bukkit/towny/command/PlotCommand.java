package com.palmergames.bukkit.towny.command;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

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

/**
 * Send a list of all general towny plot help commands to player Command: /plot
 */

public class PlotCommand extends BaseCommand implements CommandExecutor {

	private static Towny plugin;
	public static final List<String> output = new ArrayList<String>();

	static {
		output.add(ChatTools.formatTitle("/토지"));
		output.add(ChatTools.formatCommand(TownySettings.getLangString("res_sing"), "/토지 점유", "", TownySettings.getLangString("msg_block_claim")));
		output.add(ChatTools.formatCommand(TownySettings.getLangString("res_sing"), "/토지 점유", "[각형/원형] [반지름]", ""));
		output.add(ChatTools.formatCommand(TownySettings.getLangString("res_sing") + "/" + TownySettings.getLangString("mayor_sing"), "/토지 판매취소", "", TownySettings.getLangString("msg_plot_nfs")));
		output.add(ChatTools.formatCommand(TownySettings.getLangString("res_sing") + "/" + TownySettings.getLangString("mayor_sing"), "/토지 판매취소", "[각형/원형] [반지름]", ""));
		output.add(ChatTools.formatCommand(TownySettings.getLangString("res_sing") + "/" + TownySettings.getLangString("mayor_sing"), "/토지 판매 [$]", "", TownySettings.getLangString("msg_plot_fs")));
		output.add(ChatTools.formatCommand(TownySettings.getLangString("res_sing") + "/" + TownySettings.getLangString("mayor_sing"), "/토지 판매 [$]", "within [각형/원형] [반지름]", ""));
		output.add(ChatTools.formatCommand(TownySettings.getLangString("res_sing") + "/" + TownySettings.getLangString("mayor_sing"), "/토지 초기화", "", ""));
		output.add(ChatTools.formatCommand(TownySettings.getLangString("res_sing") + "/" + TownySettings.getLangString("mayor_sing"), "/토지 설정 ...", "", TownySettings.getLangString("msg_plot_fs")));
		output.add(ChatTools.formatCommand(TownySettings.getLangString("res_sing"), "/토지 토글", "[pvp/불/폭발/몹]", ""));
		output.add(TownySettings.getLangString("msg_nfs_abr"));
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
				//resident.getTown();
			} catch (TownyException x) {
				TownyMessaging.sendErrorMsg(player, x.getMessage());
				return true;
			}

			try {
				if (split[0].equalsIgnoreCase("claim") || split[0].equalsIgnoreCase("점유")) {

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

				} else if (split[0].equalsIgnoreCase("unclaim") || split[0].equalsIgnoreCase("점유해제")) {

					if (!TownyUniverse.getPermissionSource().testPermission(player, PermissionNodes.TOWNY_COMMAND_PLOT_UNCLAIM.getNode()))
						throw new TownyException(TownySettings.getLangString("msg_err_command_disable"));

					if (TownyUniverse.isWarTime())
						throw new TownyException(TownySettings.getLangString("msg_war_cannot_do"));
					if (split.length == 2 && split[1].equalsIgnoreCase("모두")) {// Additional Code by Neder. 2014-06-14
						split[1] = "all";
					}										// End of Additional Code.
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

				} else if (split[0].equalsIgnoreCase("notforsale") || split[0].equalsIgnoreCase("nfs") || split[0].equalsIgnoreCase("판매취소") || split[0].equalsIgnoreCase("안팜")) {

					if (!TownyUniverse.getPermissionSource().testPermission(player, PermissionNodes.TOWNY_COMMAND_PLOT_NOTFORSALE.getNode()))
						throw new TownyException(TownySettings.getLangString("msg_err_command_disable"));

					List<WorldCoord> selection = AreaSelectionUtil.selectWorldCoordArea(resident, new WorldCoord(world, Coord.parseCoord(player)), StringMgmt.remFirstArg(split));
					selection = AreaSelectionUtil.filterOwnedBlocks(resident.getTown(), selection);

					for (WorldCoord worldCoord : selection) {
						setPlotForSale(resident, worldCoord, -1);
					}

				} else if (split[0].equalsIgnoreCase("forsale") || split[0].equalsIgnoreCase("fs") || split[0].equalsIgnoreCase("판매") || split[0].equalsIgnoreCase("팜")) {

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

				} else if (split[0].equalsIgnoreCase("perm") || split[0].equalsIgnoreCase("권한")) {

					if (!TownyUniverse.getPermissionSource().testPermission(player, PermissionNodes.TOWNY_COMMAND_PLOT_PERM.getNode()))
						throw new TownyException(TownySettings.getLangString("msg_err_command_disable"));

					if (split.length > 1 && split[1].equalsIgnoreCase("hud"))
						if (plugin.isPermissions() && TownyUniverse.getPermissionSource().has(player, PermissionNodes.TOWNY_COMMAND_PLOT_PERM_HUD.getNode())) {
							plugin.getHUDManager().togglePermHuD(player);
						} else {
							TownyMessaging.sendErrorMsg(player, TownySettings.getLangString("msg_err_command_disable"));
						}
					else {
						TownBlock townBlock = new WorldCoord(world, Coord.parseCoord(player)).getTownBlock();
						TownyMessaging.sendMessage(player, TownyFormatter.getStatus(townBlock));
					}

				} else if (split[0].equalsIgnoreCase("toggle") || split[0].equalsIgnoreCase("토글")) {

					/*
					 * perm test in the plottoggle.
					 */

					TownBlock townBlock = new WorldCoord(world, Coord.parseCoord(player)).getTownBlock();
					// Test we are allowed to work on this plot
					plotTestOwner(resident, townBlock); // ignore the return as
					// we are only checking
					// for an exception

					plotToggle(player, new WorldCoord(world, Coord.parseCoord(player)).getTownBlock(), StringMgmt.remFirstArg(split));

				} else if (split[0].equalsIgnoreCase("set") || split[0].equalsIgnoreCase("설정")) {

					split = StringMgmt.remFirstArg(split);

					if (split.length > 0) {

						if (!TownyUniverse.getPermissionSource().testPermission(player, PermissionNodes.TOWNY_COMMAND_PLOT_SET.getNode(split[0].toLowerCase())))
							throw new TownyException(TownySettings.getLangString("msg_err_command_disable"));
						
						if (split[0].equalsIgnoreCase("perm") || split[0].equalsIgnoreCase("권한")) {

							// Set plot level permissions (if the plot owner) or
							// Mayor/Assistant of the town.

							TownBlock townBlock = new WorldCoord(world, Coord.parseCoord(player)).getTownBlock();
							// Test we are allowed to work on this plot
							TownBlockOwner owner = plotTestOwner(resident, townBlock);

							// Check we are allowed to set these perms
							toggleTest(player, townBlock, StringMgmt.join(StringMgmt.remFirstArg(split), ""));

							setTownBlockPermissions(player, owner, townBlock, StringMgmt.remFirstArg(split));

							return true;
							
						} else if (split[0].equalsIgnoreCase("name") || split[0].equalsIgnoreCase("이름")) {

							TownBlock townBlock = new WorldCoord(world, Coord.parseCoord(player)).getTownBlock();
							// Test we are allowed to work on this plot
							plotTestOwner(resident, townBlock);
							if (split.length == 1) {
								townBlock.setName("");
								TownyMessaging.sendMsg(player, String.format("Plot name removed"));
								TownyUniverse.getDataSource().saveTownBlock(townBlock);
								return true;
							}
							
							// Test if the plot name contains invalid characters.
							if (!NameValidation.isBlacklistName(split[1])) {								
								townBlock.setName(StringMgmt.join(StringMgmt.remFirstArg(split), ""));
							
     							//townBlock.setChanged(true);
							    TownyUniverse.getDataSource().saveTownBlock(townBlock);
							
							    TownyMessaging.sendMsg(player, String.format("토지의 이름이 [%s] (으)로 설정되었습니다", townBlock.getName()));

							} else {

								TownyMessaging.sendErrorMsg(player, TownySettings.getLangString("msg_invalid_name"));

							}
							return true;
						} 

						WorldCoord worldCoord = new WorldCoord(world, Coord.parseCoord(player));

						setPlotType(resident, worldCoord, split[0]);

						player.sendMessage(String.format(TownySettings.getLangString("msg_plot_set_type"), split[0]));

					} else {

						player.sendMessage(ChatTools.formatCommand("", "/토지 설정", "이름", ""));
						player.sendMessage(ChatTools.formatCommand("", "/토지 설정", "초기화", ""));
						player.sendMessage(ChatTools.formatCommand("", "/토지 설정", "상점|대사관|전장|야생|보호구역|여관|감옥|농장", ""));
						player.sendMessage(ChatTools.formatCommand("", "/토지 설정 권한", "?", ""));
					}

				} else if (split[0].equalsIgnoreCase("clear") || split[0].equalsIgnoreCase("초기화")) {

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
			
			player.sendMessage(ChatTools.formatTitle("/... 설정 권한"));
			player.sendMessage(ChatTools.formatCommand("대상", "[친구/동맹/외부인]", "", ""));
			player.sendMessage(ChatTools.formatCommand("동작", "[건축/파괴/스위치/아이템사용]", "", ""));
			player.sendMessage(ChatTools.formatCommand("", "설정 권한", "[켜기/끄기]", "모든 권한을 토글합니다"));
			player.sendMessage(ChatTools.formatCommand("", "설정 권한", "[대상/동작] [켜기/끄기]", ""));
			player.sendMessage(ChatTools.formatCommand("", "설정 권한", "[대상] [동작] [켜기/끄기]", ""));
			player.sendMessage(ChatTools.formatCommand("", "설정 권한", "초기화", ""));
			player.sendMessage(ChatTools.formatCommand("예시", "/토지 설정 권한", "친구 건축 켜기", ""));
			player.sendMessage(String.format(TownySettings.getLangString("plot_perms"), "'친구'", "'주민'"));
			player.sendMessage(TownySettings.getLangString("plot_perms_1"));

		} else {

			TownyPermission perm = townBlock.getPermissions();

			if (split.length == 1) {
				
				if (split[0].equalsIgnoreCase("reset") || split[0].equalsIgnoreCase("초기화")) {
					
					// reset this townBlock permissions (by town/resident)
					townBlock.setType(townBlock.getType());
					TownyUniverse.getDataSource().saveTownBlock(townBlock);

					if (townBlockOwner instanceof Town)
						TownyMessaging.sendMsg(player, String.format(TownySettings.getLangString("msg_set_perms_reset"), "마을이 소유한"));
					else
						TownyMessaging.sendMsg(player, String.format(TownySettings.getLangString("msg_set_perms_reset"), "당신의"));

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
						// invalid entry
					}

				}

			} else if (split.length == 2) {

				try {

					boolean b = plugin.parseOnOff(split[1]);
					
					if (split[0].equalsIgnoreCase("friend") || split[0].equalsIgnoreCase("친구")) {
						perm.residentBuild = b;
						perm.residentDestroy = b;
						perm.residentSwitch = b;
						perm.residentItemUse = b;
					} else if (split[0].equalsIgnoreCase("outsider") || split[0].equalsIgnoreCase("외부인")) {
						perm.outsiderBuild = b;
						perm.outsiderDestroy = b;
						perm.outsiderSwitch = b;
						perm.outsiderItemUse = b;
					} else if (split[0].equalsIgnoreCase("ally") || split[0].equalsIgnoreCase("동맹")) {
						perm.allyBuild = b;
						perm.allyDestroy = b;
						perm.allySwitch = b;
						perm.allyItemUse = b;
					} else if (split[0].equalsIgnoreCase("build") || split[0].equalsIgnoreCase("건축")) {
						perm.residentBuild = b;
						perm.outsiderBuild = b;
						perm.allyBuild = b;
					} else if (split[0].equalsIgnoreCase("destroy") || split[0].equalsIgnoreCase("파괴")) {
						perm.residentDestroy = b;
						perm.outsiderDestroy = b;
						perm.allyDestroy = b;
					} else if (split[0].equalsIgnoreCase("switch") || split[0].equalsIgnoreCase("스위치")) {
						perm.residentSwitch = b;
						perm.outsiderSwitch = b;
						perm.allySwitch = b;
					} else if (split[0].equalsIgnoreCase("itemuse") || split[0].equalsIgnoreCase("아이템사용")) {
						perm.residentItemUse = b;
						perm.outsiderItemUse = b;
						perm.allyItemUse = b;
					}

				} catch (Exception e) {
				}

			} else if (split.length == 3) {

				// reset the friend to resident so the perm settings don't fail
				if (split[0].equalsIgnoreCase("friend"))
					split[0] = "resident";
				else if (split[0].equalsIgnoreCase("친구")) // Additional Code by Neder, 2014-06-12
					split[0] = "주민";
				
				try {
					boolean b = plugin.parseOnOff(split[2]);
					String s = "";
					s = split[0] + split[1];
					// Additional Code by Neder. "Special Thanks to itstake"
					if (s.equalsIgnoreCase("주민"+"건축")) {
						s = "residentBuild";
					} else if (s.equalsIgnoreCase("주민"+"파괴")) {
						s = "residentDestroy";
					} else if (s.equalsIgnoreCase("주민"+"스위치")) {
						s = "residentSwitch";
					} else if (s.equalsIgnoreCase("주민"+"아이템사용")) {
						s = "residentItemuse";
					} else if (s.equalsIgnoreCase("동맹"+"건축")) {
						s = "allyBuild";
					} else if (s.equalsIgnoreCase("동맹"+"파괴")) {
						s = "allyDestroy";
					} else if (s.equalsIgnoreCase("동맹"+"스위치")) {
						s = "allySwitch";
					} else if (s.equalsIgnoreCase("동맹"+"아이템사용")) {
						s = "allyItemuse";
					} else if (s.equalsIgnoreCase("외부인"+"건축")) {
						s = "outsiderBuild";
					} else if (s.equalsIgnoreCase("외부인"+"파괴")) {
						s = "outsiderDestroy";
					} else if (s.equalsIgnoreCase("외부인"+"스위치")) {
						s = "outsiderSwitch";
					} else if (s.equalsIgnoreCase("외부인"+"아이템사용")) {
						s = "outsiderItemuse";
					} else {
					}
					// End of Additional Code. Coded in 2014-02-28, by Neder(neder(@)neder.me).
					perm.set(s, b);
				} catch (Exception e) {
				}

			}

			townBlock.setChanged(true);
			TownyUniverse.getDataSource().saveTownBlock(townBlock);

			TownyMessaging.sendMsg(player, TownySettings.getLangString("msg_set_perms"));
			TownyMessaging.sendMessage(player, (Colors.Green + " 권한: " + ((townBlockOwner instanceof Resident) ? perm.getColourString().replace("f", "r") : perm.getColourString())));
			TownyMessaging.sendMessage(player, Colors.Green + "PvP: " + ((perm.pvp) ? Colors.Red + "켜짐" : Colors.LightGreen + "꺼짐") + Colors.Green + "  폭발: " + ((perm.explosion) ? Colors.Red + "켜짐" : Colors.LightGreen + "꺼짐") + Colors.Green + "  불번짐: " + ((perm.fire) ? Colors.Red + "켜짐" : Colors.LightGreen + "꺼짐") + Colors.Green + "  몹 스폰: " + ((perm.mobs) ? Colors.Red + "켜짐" : Colors.LightGreen + "꺼짐"));

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
				if (forSale > 1000000 ) {
					TownyUniverse.getPlayer(resident).sendMessage("Plot price too expensive.");

				} else {
					townBlock.setPlotPrice(forSale);

					if (forSale != -1)
						TownyMessaging.sendTownMessage(townBlock.getTown(), TownySettings.getPlotForSaleMsg(resident.getName(), worldCoord));
					else
						TownyUniverse.getPlayer(resident).sendMessage(TownySettings.getLangString("msg_err_plot_nfs"));

					// Save this townblock so the for sale status is remembered.
					TownyUniverse.getDataSource().saveTownBlock(townBlock);
				}

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
			player.sendMessage(ChatTools.formatTitle("/주민 토글"));
			player.sendMessage(ChatTools.formatCommand("", "/주민 토글", "pvp", ""));
			player.sendMessage(ChatTools.formatCommand("", "/주민 토글", "폭발", ""));
			player.sendMessage(ChatTools.formatCommand("", "/주민 토글", "불", ""));
			player.sendMessage(ChatTools.formatCommand("", "/주민 토글", "몹", ""));
		} else {

			try {

				if (!TownyUniverse.getPermissionSource().testPermission(player, PermissionNodes.TOWNY_COMMAND_PLOT_TOGGLE.getNode(split[0].toLowerCase())))
					throw new TownyException(TownySettings.getLangString("msg_err_command_disable"));

				if (split[0].equalsIgnoreCase("pvp")) {
					// Make sure we are allowed to set these permissions.
					toggleTest(player, townBlock, StringMgmt.join(split, " "));
					townBlock.getPermissions().pvp = !townBlock.getPermissions().pvp;
					TownyMessaging.sendMessage(player, String.format(TownySettings.getLangString("msg_changed_pvp"), "토지", townBlock.getPermissions().pvp ? "활성" : "비활성"));

				} else if (split[0].equalsIgnoreCase("explosion") || split[0].equalsIgnoreCase("폭발")) {
					// Make sure we are allowed to set these permissions.
					toggleTest(player, townBlock, StringMgmt.join(split, " "));
					townBlock.getPermissions().explosion = !townBlock.getPermissions().explosion;
					TownyMessaging.sendMessage(player, String.format(TownySettings.getLangString("msg_changed_expl"), "토지", townBlock.getPermissions().explosion ? "활성" : "비활성"));

				} else if (split[0].equalsIgnoreCase("fire") || split[0].equalsIgnoreCase("불")) {
					// Make sure we are allowed to set these permissions.
					toggleTest(player, townBlock, StringMgmt.join(split, " "));
					townBlock.getPermissions().fire = !townBlock.getPermissions().fire;
					TownyMessaging.sendMessage(player, String.format(TownySettings.getLangString("msg_changed_fire"), "토지", townBlock.getPermissions().fire ? "활성" : "비활성"));

				} else if (split[0].equalsIgnoreCase("mobs") || split[0].equalsIgnoreCase("몹")) {
					// Make sure we are allowed to set these permissions.
					toggleTest(player, townBlock, StringMgmt.join(split, " "));
					townBlock.getPermissions().mobs = !townBlock.getPermissions().mobs;
					TownyMessaging.sendMessage(player, String.format(TownySettings.getLangString("msg_changed_mobs"), "토지", townBlock.getPermissions().mobs ? "활성" : "비활성"));

				} else {
					TownyMessaging.sendErrorMsg(player, String.format(TownySettings.getLangString("msg_err_invalid_property"), "토지"));
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

		if (split.contains("mobs") || split.contains("몹")) {
			if (town.getWorld().isForceTownMobs())
				throw new TownyException(TownySettings.getLangString("msg_world_mobs"));
		}

		if (split.contains("fire") || split.contains("불")) {
			if (town.getWorld().isForceFire())
				throw new TownyException(TownySettings.getLangString("msg_world_fire"));
		}

		if (split.contains("explosion") || split.contains("폭발")) {
			if (town.getWorld().isForceExpl())
				throw new TownyException(TownySettings.getLangString("msg_world_expl"));
		}

		if (split.contains("pvp")) {
			if (town.getWorld().isForcePVP())
				throw new TownyException(TownySettings.getLangString("msg_world_pvp"));
		}
		if ((split.contains("pvp")) || (split.trim().equalsIgnoreCase("off") || split.trim().equalsIgnoreCase("끄기"))) {
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
