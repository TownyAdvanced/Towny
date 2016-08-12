package com.palmergames.bukkit.towny.command;

import ca.xshade.bukkit.questioner.Questioner;
import ca.xshade.questionmanager.Option;
import ca.xshade.questionmanager.Question;

import com.earth2me.essentials.Teleport;
import com.earth2me.essentials.User;
import com.palmergames.bukkit.towny.*;
import com.palmergames.bukkit.towny.event.NewTownEvent;
import com.palmergames.bukkit.towny.event.TownBlockSettingsChangedEvent;
import com.palmergames.bukkit.towny.exceptions.*;
import com.palmergames.bukkit.towny.object.*;
import com.palmergames.bukkit.towny.permissions.PermissionNodes;
import com.palmergames.bukkit.towny.permissions.TownyPerms;
import com.palmergames.bukkit.towny.questioner.JoinTownTask;
import com.palmergames.bukkit.towny.questioner.ResidentTownQuestionTask;
import com.palmergames.bukkit.towny.questioner.TownQuestionTask;
import com.palmergames.bukkit.towny.regen.PlotBlockData;
import com.palmergames.bukkit.towny.regen.TownyRegenAPI;
import com.palmergames.bukkit.towny.tasks.TownClaim;
import com.palmergames.bukkit.towny.utils.AreaSelectionUtil;
import com.palmergames.bukkit.util.BukkitTools;
import com.palmergames.bukkit.util.ChatTools;
import com.palmergames.bukkit.util.Colors;
import com.palmergames.bukkit.util.NameValidation;
import com.palmergames.util.StringMgmt;

import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerTeleportEvent.TeleportCause;
import org.bukkit.plugin.Plugin;

import javax.naming.InvalidNameException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import static com.palmergames.bukkit.towny.object.TownyObservableType.TOWN_ADD_RESIDENT;
import static com.palmergames.bukkit.towny.object.TownyObservableType.TOWN_REMOVE_RESIDENT;

/**
 * Send a list of all town help commands to player Command: /town
 */

public class TownCommand extends BaseCommand implements CommandExecutor {

	private static Towny plugin;
	private static final List<String> output = new ArrayList<String>();

	static {
		output.add(ChatTools.formatTitle("/town"));
		output.add(ChatTools.formatCommand("", "/town", "", TownySettings.getLangString("town_help_1")));
		output.add(ChatTools.formatCommand("", "/town", "[town]", TownySettings.getLangString("town_help_3")));
		output.add(ChatTools.formatCommand("", "/town", "here", TownySettings.getLangString("town_help_4")));
		output.add(ChatTools.formatCommand("", "/town", "list", ""));
		output.add(ChatTools.formatCommand("", "/town", "online", TownySettings.getLangString("town_help_10")));
		output.add(ChatTools.formatCommand("", "/town", "leave", ""));
		output.add(ChatTools.formatCommand("", "/town", "reslist", ""));
		output.add(ChatTools.formatCommand("", "/town", "ranklist", ""));
		output.add(ChatTools.formatCommand("", "/town", "spawn", TownySettings.getLangString("town_help_5")));
//		if (!TownySettings.isTownCreationAdminOnly())
//			output.add(ChatTools.formatCommand("", "/town", "new [town]", TownySettings.getLangString("town_help_6")));
		output.add(ChatTools.formatCommand(TownySettings.getLangString("admin_sing"), "/town", "new [town] " + TownySettings.getLangString("town_help_2"), TownySettings.getLangString("town_help_7")));
		output.add(ChatTools.formatCommand(TownySettings.getLangString("res_sing"), "/town", "deposit [$]", ""));
		output.add(ChatTools.formatCommand(TownySettings.getLangString("res_sing"), "/town", "rank add/remove [resident] [rank]", ""));
		output.add(ChatTools.formatCommand(TownySettings.getLangString("mayor_sing"), "/town", "mayor ?", TownySettings.getLangString("town_help_8")));
		output.add(ChatTools.formatCommand(TownySettings.getLangString("admin_sing"), "/town", "delete [town]", ""));
	}

	public TownCommand(Towny instance) {

		plugin = instance;
	}

	@Override
	public boolean onCommand(CommandSender sender, Command cmd, String commandLabel, String[] args) {

		if (sender instanceof Player) {
			Player player = (Player) sender;
			System.out.println("[PLAYER_COMMAND] " + player.getName() + ": /" + commandLabel + " " + StringMgmt.join(args));
			parseTownCommand(player, args);
		} else
			// Console
			for (String line : output)
				sender.sendMessage(Colors.strip(line));
		return true;
	}

	private void parseTownCommand(Player player, String[] split) {

		try {

			if (split.length == 0) {

				try {
					Resident resident = TownyUniverse.getDataSource().getResident(player.getName());
					Town town = resident.getTown();
					TownyMessaging.sendMessage(player, TownyFormatter.getStatus(town));
				} catch (NotRegisteredException x) {
					throw new TownyException(TownySettings.getLangString("msg_err_dont_belong_town"));
				}

			} else if (split[0].equalsIgnoreCase("?") || split[0].equalsIgnoreCase("help")) {

				for (String line : output)
					player.sendMessage(line);

			} else if (split[0].equalsIgnoreCase("here")) {

				if (!TownyUniverse.getPermissionSource().testPermission(player, PermissionNodes.TOWNY_COMMAND_TOWN_HERE.getNode()))
					throw new TownyException(TownySettings.getLangString("msg_err_command_disable"));

				showTownStatusHere(player);

			} else if (split[0].equalsIgnoreCase("list")) {

				if (!TownyUniverse.getPermissionSource().testPermission(player, PermissionNodes.TOWNY_COMMAND_TOWN_LIST.getNode()))
					throw new TownyException(TownySettings.getLangString("msg_err_command_disable"));

				listTowns(player);

			} else if (split[0].equalsIgnoreCase("new")) {

				if (!TownyUniverse.getPermissionSource().testPermission(player, PermissionNodes.TOWNY_COMMAND_TOWN_NEW.getNode()))
					throw new TownyException(TownySettings.getLangString("msg_err_command_disable"));

				if (split.length == 1) {
					throw new TownyException(TownySettings.getLangString("msg_specify_name"));
				} else if (split.length == 2) {
					newTown(player, split[1], player.getName());
				} else {

					/*
					 * Admin only
					 */
					if (!TownyUniverse.getPermissionSource().testPermission(player, PermissionNodes.TOWNY_COMMAND_TOWNYADMIN_TOWN_NEW.getNode()))
						throw new TownyException(TownySettings.getLangString("msg_err_command_disable"));

					newTown(player, split[1], split[2]);
				}

			} else if (split[0].equalsIgnoreCase("leave")) {

				if (!TownyUniverse.getPermissionSource().testPermission(player, PermissionNodes.TOWNY_COMMAND_TOWN_LEAVE.getNode()))
					throw new TownyException(TownySettings.getLangString("msg_err_command_disable"));

				townLeave(player);

			} else if (split[0].equalsIgnoreCase("withdraw")) {

				if (!TownyUniverse.getPermissionSource().testPermission(player, PermissionNodes.TOWNY_COMMAND_TOWN_WITHDRAW.getNode()))
					throw new TownyException(TownySettings.getLangString("msg_err_command_disable"));

				if (split.length == 2)
					try {
						townWithdraw(player, Integer.parseInt(split[1].trim()));
					} catch (NumberFormatException e) {
						throw new TownyException(TownySettings.getLangString("msg_error_must_be_int"));
					}
				else
					throw new TownyException(String.format(TownySettings.getLangString("msg_must_specify_amnt"), "/town withdraw"));

			} else if (split[0].equalsIgnoreCase("deposit")) {

				if (!TownyUniverse.getPermissionSource().testPermission(player, PermissionNodes.TOWNY_COMMAND_TOWN_DEPOSIT.getNode()))
					throw new TownyException(TownySettings.getLangString("msg_err_command_disable"));

				if (split.length == 2)
					try {
						townDeposit(player, Integer.parseInt(split[1].trim()));
					} catch (NumberFormatException e) {
						throw new TownyException(TownySettings.getLangString("msg_error_must_be_int"));
					}
				else
					throw new TownyException(String.format(TownySettings.getLangString("msg_must_specify_amnt"), "/town deposit"));

			} else {
				String[] newSplit = StringMgmt.remFirstArg(split);
				
				if (split[0].equalsIgnoreCase("rank")) {

					/*
					 * perm tests performed in method.
					 */
					townRank(player, newSplit);

				} else if (split[0].equalsIgnoreCase("set")) {

					/*
					 * perm test performed in method.
					 */
					townSet(player, newSplit);

				} else if (split[0].equalsIgnoreCase("buy")) {

					if (!TownyUniverse.getPermissionSource().testPermission(player, PermissionNodes.TOWNY_COMMAND_TOWN_BUY.getNode()))
						throw new TownyException(TownySettings.getLangString("msg_err_command_disable"));

					townBuy(player, newSplit);

				} else if (split[0].equalsIgnoreCase("toggle")) {

					/*
					 * perm test performed in method.
					 */
					townToggle(player, newSplit);

				} else if (split[0].equalsIgnoreCase("mayor")) {

					if (!TownyUniverse.getPermissionSource().testPermission(player, PermissionNodes.TOWNY_COMMAND_TOWN_MAYOR.getNode()))
						throw new TownyException(TownySettings.getLangString("msg_err_command_disable"));

					townMayor(player, newSplit);

				} else if (split[0].equalsIgnoreCase("spawn")) {

					/*
					 * town spawn handles it's own perms.
					 */
					townSpawn(player, newSplit, false);

				} else if (split[0].equalsIgnoreCase("outpost")) {
					/*
					 * outpost spawns handle their own perms.
					 */
					townSpawn(player, newSplit, true);

				} else if (split[0].equalsIgnoreCase("delete")) {

					if (!TownyUniverse.getPermissionSource().testPermission(player, PermissionNodes.TOWNY_COMMAND_TOWN_DELETE.getNode()))
						throw new TownyException(TownySettings.getLangString("msg_err_command_disable"));

					townDelete(player, newSplit);

				} else if (split[0].equalsIgnoreCase("reslist")) {

					if (!TownyUniverse.getPermissionSource().testPermission(player, PermissionNodes.TOWNY_COMMAND_TOWN_RESLIST.getNode()))

						throw new TownyException(TownySettings.getLangString("msg_err_command_disable"));

					try {

						Resident resident = TownyUniverse.getDataSource().getResident(player.getName());

						Town town = resident.getTown();

						TownyMessaging.sendMessage(player, TownyFormatter.getFormattedResidents(town));

					} catch (NotRegisteredException x) {

						throw new TownyException(TownySettings.getLangString("msg_err_dont_belong_town"));

					}

				} else if (split[0].equalsIgnoreCase("ranklist")) {

					if (!TownyUniverse.getPermissionSource().testPermission(player, PermissionNodes.TOWNY_COMMAND_TOWN_RANKLIST.getNode()))
						throw new TownyException(TownySettings.getLangString("msg_err_command_disable"));

					try {
						Resident resident = TownyUniverse.getDataSource().getResident(player.getName());
						Town town = resident.getTown();
						TownyMessaging.sendMessage(player, TownyFormatter.getRanks(town));
					} catch (NotRegisteredException x) {
						throw new TownyException(TownySettings.getLangString("msg_err_dont_belong_town"));
					}

				} else if (split[0].equalsIgnoreCase("join")) {

					if (!TownyUniverse.getPermissionSource().testPermission(player, PermissionNodes.TOWNY_COMMAND_TOWN_JOIN.getNode()))
						throw new TownyException(TownySettings.getLangString("msg_err_command_disable"));

					parseTownJoin(player, newSplit);

				} else if (split[0].equalsIgnoreCase("add")) {

					if (!TownyUniverse.getPermissionSource().testPermission(player, PermissionNodes.TOWNY_COMMAND_TOWN_ADD.getNode()))
						throw new TownyException(TownySettings.getLangString("msg_err_command_disable"));

					townAdd(player, null, newSplit);

				} else if (split[0].equalsIgnoreCase("kick")) {

					if (!TownyUniverse.getPermissionSource().testPermission(player, PermissionNodes.TOWNY_COMMAND_TOWN_KICK.getNode()))
						throw new TownyException(TownySettings.getLangString("msg_err_command_disable"));

					townKick(player, newSplit);

				} else if (split[0].equalsIgnoreCase("claim")) {

					parseTownClaimCommand(player, newSplit);

				} else if (split[0].equalsIgnoreCase("unclaim")) {

					if (!TownyUniverse.getPermissionSource().testPermission(player, PermissionNodes.TOWNY_COMMAND_TOWN_UNCLAIM.getNode()))
						throw new TownyException(TownySettings.getLangString("msg_err_command_disable"));

					parseTownUnclaimCommand(player, newSplit);

				} else if (split[0].equalsIgnoreCase("online")) {

					if (!TownyUniverse.getPermissionSource().testPermission(player, PermissionNodes.TOWNY_COMMAND_TOWN_ONLINE.getNode()))
						throw new TownyException(TownySettings.getLangString("msg_err_command_disable"));

					try {
						Resident resident = TownyUniverse.getDataSource().getResident(player.getName());
						Town town = resident.getTown();
						TownyMessaging.sendMessage(player, TownyFormatter.getFormattedOnlineResidents(TownySettings.getLangString("msg_town_online"), town, player));
					} catch (NotRegisteredException x) {
						throw new TownyException(TownySettings.getLangString("msg_err_dont_belong_town"));
					}
				} else
					try {
						Town town = TownyUniverse.getDataSource().getTown(split[0]);
						TownyMessaging.sendMessage(player, TownyFormatter.getStatus(town));
					} catch (NotRegisteredException x) {
						throw new TownyException(String.format(TownySettings.getLangString("msg_err_not_registered_1"), split[0]));
					}
			}

		} catch (Exception x) {
			TownyMessaging.sendErrorMsg(player, x.getMessage());
		}

	}

	/**
	 * Send a list of all towns in the universe to player Command: /town list
	 * 
	 * @param player
	 */

	@SuppressWarnings({ "unchecked", "rawtypes" })
	public void listTowns(Player player) {
		player.sendMessage(ChatTools.formatTitle(TownySettings.getLangString("town_plu")));
		player.sendMessage(Colors.Blue + "Town Name" + Colors.Gray + " - " + Colors.LightBlue + "(Number of Residents)");
		List<Town> townsToSort = TownyUniverse.getDataSource().getTowns();		 

		Collections.sort(townsToSort, new Comparator() {
			@Override
	        public int compare(Object t1, Object t2) {
				if (((Town) t2).getNumResidents() == ((Town) t1).getNumResidents()) return 0;
				return (((Town) t2).getNumResidents() > ((Town) t1).getNumResidents()) ? 1 : -1;
	        }
		});
		for (Town town : townsToSort) {
			String output = Colors.Blue + town.getName() + Colors.Gray + " - " + Colors.LightBlue + "(" + town.getNumResidents() + ")";
            if (town.isOpen())
                    output += Colors.White + " (Open)";
            player.sendMessage(output);
		}		
	}

	public void townMayor(Player player, String[] split) {

		if (split.length == 0 || split[0].equalsIgnoreCase("?"))
			showTownMayorHelp(player);
	}

	/**
	 * Send a the status of the town the player is physically at to him
	 * 
	 * @param player
	 */
	public void showTownStatusHere(Player player) {

		try {
			TownyWorld world = TownyUniverse.getDataSource().getWorld(player.getWorld().getName());
			Coord coord = Coord.parseCoord(player);
			showTownStatusAtCoord(player, world, coord);
		} catch (TownyException e) {
			TownyMessaging.sendErrorMsg(player, e.getMessage());
		}
	}

	/**
	 * Send a the status of the town at the target coordinates to the player
	 * 
	 * @param player
	 * @param world
	 * @param coord
	 * @throws TownyException
	 */
	public void showTownStatusAtCoord(Player player, TownyWorld world, Coord coord) throws TownyException {

		if (!world.hasTownBlock(coord))
			throw new TownyException(String.format(TownySettings.getLangString("msg_not_claimed"), coord));

		Town town = world.getTownBlock(coord).getTown();
		TownyMessaging.sendMessage(player, TownyFormatter.getStatus(town));
	}

	public void showTownMayorHelp(Player player) {

		player.sendMessage(ChatTools.formatTitle("Town Mayor Help"));
		player.sendMessage(ChatTools.formatCommand(TownySettings.getLangString("mayor_sing"), "/town", "withdraw [$]", ""));
		player.sendMessage(ChatTools.formatCommand(TownySettings.getLangString("mayor_sing"), "/town", "claim", "'/town claim ?' " + TownySettings.getLangString("res_5")));
		player.sendMessage(ChatTools.formatCommand(TownySettings.getLangString("mayor_sing"), "/town", "unclaim", "'/town " + TownySettings.getLangString("res_5")));
		player.sendMessage(ChatTools.formatCommand(TownySettings.getLangString("mayor_sing"), "/town", "[add/kick] " + TownySettings.getLangString("res_2") + " .. []", TownySettings.getLangString("res_6")));
		player.sendMessage(ChatTools.formatCommand(TownySettings.getLangString("mayor_sing"), "/town", "set [] .. []", "'/town set' " + TownySettings.getLangString("res_5")));
		player.sendMessage(ChatTools.formatCommand(TownySettings.getLangString("mayor_sing"), "/town", "buy [] .. []", "'/town buy' " + TownySettings.getLangString("res_5")));
		player.sendMessage(ChatTools.formatCommand(TownySettings.getLangString("mayor_sing"), "/town", "jail", "'/town jail ?' " + TownySettings.getLangString("res_5")));
		player.sendMessage(ChatTools.formatCommand(TownySettings.getLangString("mayor_sing"), "/town", "toggle", ""));
		player.sendMessage(ChatTools.formatCommand(TownySettings.getLangString("mayor_sing"), "/town", "rank add/remove [resident] [rank]", "'/town rank ?' " + TownySettings.getLangString("res_5")));
		// TODO: player.sendMessage(ChatTools.formatCommand("Mayor", "/town",
		// "wall [type] [height]", ""));
		// TODO: player.sendMessage(ChatTools.formatCommand("Mayor", "/town",
		// "wall remove", ""));
		player.sendMessage(ChatTools.formatCommand(TownySettings.getLangString("mayor_sing"), "/town", "delete", ""));
	}

	public void townToggle(Player player, String[] split) throws TownyException {

		if (split.length == 0) {
			player.sendMessage(ChatTools.formatTitle("/town toggle"));
			player.sendMessage(ChatTools.formatCommand("", "/town toggle", "pvp", ""));
			player.sendMessage(ChatTools.formatCommand("", "/town toggle", "public", ""));
			player.sendMessage(ChatTools.formatCommand("", "/town toggle", "explosion", ""));
			player.sendMessage(ChatTools.formatCommand("", "/town toggle", "fire", ""));
			player.sendMessage(ChatTools.formatCommand("", "/town toggle", "mobs", ""));
			player.sendMessage(ChatTools.formatCommand("", "/town toggle", "taxpercent", ""));
			player.sendMessage(ChatTools.formatCommand("", "/town toggle", "open", ""));
			player.sendMessage(ChatTools.formatCommand("", "/town toggle", "jail [number] [resident]", ""));
		} else {
			Resident resident;
			Town town;
			try {
				resident = TownyUniverse.getDataSource().getResident(player.getName());
				town = resident.getTown();

			} catch (TownyException x) {
				throw new TownyException(x.getMessage());
			}

			if (!TownyUniverse.getPermissionSource().testPermission(player, PermissionNodes.TOWNY_COMMAND_TOWN_TOGGLE.getNode(split[0].toLowerCase())))
				throw new TownyException(TownySettings.getLangString("msg_err_command_disable"));

			// TODO: Let admin's call a subfunction of this.
			if (split[0].equalsIgnoreCase("public")) {

				town.setPublic(!town.isPublic());
				TownyMessaging.sendTownMessage(town, String.format(TownySettings.getLangString("msg_changed_public"), town.isPublic() ? "Enabled" : "Disabled"));

			} else if (split[0].equalsIgnoreCase("pvp")) {
				// Make sure we are allowed to set these permissions.
				toggleTest(player, town, StringMgmt.join(split, " "));
				town.setPVP(!town.isPVP());
				TownyMessaging.sendTownMessage(town, String.format(TownySettings.getLangString("msg_changed_pvp"), "Town", town.isPVP() ? "Enabled" : "Disabled"));

			} else if (split[0].equalsIgnoreCase("explosion")) {
				// Make sure we are allowed to set these permissions.
				toggleTest(player, town, StringMgmt.join(split, " "));
				town.setBANG(!town.isBANG());
				TownyMessaging.sendTownMessage(town, String.format(TownySettings.getLangString("msg_changed_expl"), "Town", town.isBANG() ? "Enabled" : "Disabled"));

			} else if (split[0].equalsIgnoreCase("fire")) {
				// Make sure we are allowed to set these permissions.
				toggleTest(player, town, StringMgmt.join(split, " "));
				town.setFire(!town.isFire());
				TownyMessaging.sendTownMessage(town, String.format(TownySettings.getLangString("msg_changed_fire"), "Town", town.isFire() ? "Enabled" : "Disabled"));

			} else if (split[0].equalsIgnoreCase("mobs")) {
				// Make sure we are allowed to set these permissions.
				toggleTest(player, town, StringMgmt.join(split, " "));
				town.setHasMobs(!town.hasMobs());
				TownyMessaging.sendTownMessage(town, String.format(TownySettings.getLangString("msg_changed_mobs"), "Town", town.hasMobs() ? "Enabled" : "Disabled"));

			} else if (split[0].equalsIgnoreCase("taxpercent")) {
				town.setTaxPercentage(!town.isTaxPercentage());
				TownyMessaging.sendTownMessage(town, String.format(TownySettings.getLangString("msg_changed_taxpercent"), town.isTaxPercentage() ? "Enabled" : "Disabled"));
			} else if (split[0].equalsIgnoreCase("open")) {

				town.setOpen(!town.isOpen());
				TownyMessaging.sendTownMessage(town, String.format(TownySettings.getLangString("msg_changed_open"), town.isOpen() ? "Enabled" : "Disabled"));

				// Send a warning when toggling on (a reminder about plot
				// permissions).
				if (town.isOpen())
					throw new TownyException(String.format(TownySettings.getLangString("msg_toggle_open_on_warning")));
				
			} else if (split[0].equalsIgnoreCase("jail")) {
				if (!town.hasJailSpawn())
					throw new TownyException(String.format("Town does not have any jails set."));
								
				Integer index;
				if (split.length <= 2) {
					player.sendMessage(ChatTools.formatTitle("/town toggle jail"));
					player.sendMessage(ChatTools.formatCommand("", "/town toggle jail", "[number] [resident]", ""));
									    
				} else if (split.length == 3) {
				    try { 
				        Integer.parseInt(split[1]);
						index = Integer.valueOf(split[1]);
						Resident jailedresident = TownyUniverse.getDataSource().getResident(split[2]);
						if (!player.hasPermission("towny.command.town.toggle.jail")) 
							throw new TownyException("You do not have permission to jail your own residents.");
						if (!jailedresident.hasTown())
							throw new TownyException("That player is not a part of any town.");
																		
						try {
							
							if (jailedresident.isJailed() && index != jailedresident.getJailSpawn())
								index = jailedresident.getJailSpawn();
								
							Player jailedplayer = TownyUniverse.getPlayer(jailedresident);
							Town sendertown = resident.getTown();
							if (jailedresident.isJailed()) {
								Town jailTown = TownyUniverse.getDataSource().getTown(jailedresident.getJailTown());
								if (jailTown != sendertown) {
									throw new TownyException("That player is not jailed in your town.");
								} else {
									jailedresident.setJailed(jailedplayer, index, sendertown);
									return;
								
								}
							}
							
							if (jailedresident.getTown() != sendertown) 
								throw new TownyException("That player is not a resident of your town.");
							
							jailedresident.setJailed(jailedplayer, index, sendertown);
							

														
						} catch (NotRegisteredException x) {
							throw new TownyException(String.format(TownySettings.getLangString("msg_err_not_registered_1"), split[0]));
						}

				    } catch(NumberFormatException e) {
				    	player.sendMessage(ChatTools.formatTitle("/town toggle jail"));
						player.sendMessage(ChatTools.formatCommand("", "/town toggle jail", "[number] [resident]", ""));
				        return;
				    } catch(NullPointerException e) {
				    	player.sendMessage(ChatTools.formatTitle("/town toggle jail"));
						player.sendMessage(ChatTools.formatCommand("", "/town toggle jail", "[number] [resident]", ""));
				        return;
				    }
				}

				
			} else {
				throw new TownyException(String.format(TownySettings.getLangString("msg_err_invalid_property"), split[0]));
			}

			//Propagate perms to all unchanged, town owned, townblocks

			for (TownBlock townBlock : town.getTownBlocks()) {
				if (!townBlock.hasResident() && !townBlock.isChanged()) {
					townBlock.setType(townBlock.getType());
					TownyUniverse.getDataSource().saveTownBlock(townBlock);
				}
			}
			
			//Change settings event
			TownBlockSettingsChangedEvent event = new TownBlockSettingsChangedEvent(town);
			Bukkit.getServer().getPluginManager().callEvent(event);

			TownyUniverse.getDataSource().saveTown(town);
		}
	}

	private void toggleTest(Player player, Town town, String split) throws TownyException {

		// Make sure we are allowed to set these permissions.

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
	}

	public void townRank(Player player, String[] split) throws TownyException {

		if (split.length == 0) {
			// Help output.
			player.sendMessage(ChatTools.formatTitle("/town rank"));
			player.sendMessage(ChatTools.formatCommand("", "/town rank", "add/remove [resident] rank", ""));

		} else {

			Resident resident, target;
			Town town = null;
			String rank;

			/*
			 * Does the command have enough arguments?
			 */
			if (split.length < 3)
				throw new TownyException("Eg: /town rank add/remove [resident] [rank]");

			try {
				resident = TownyUniverse.getDataSource().getResident(player.getName());
				target = TownyUniverse.getDataSource().getResident(split[1]);
				town = resident.getTown();

				if (town != target.getTown())
					throw new TownyException("This resident is not a member of your Town!");

			} catch (TownyException x) {
				throw new TownyException(x.getMessage());
			}

			rank = split[2].toLowerCase();
			/*
			 * Is this a known rank?
			 */
			if (!TownyPerms.getTownRanks().contains(rank))
				throw new TownyException("Unknown rank '" + rank + "'. Permissible ranks are :- " + StringMgmt.join(TownyPerms.getTownRanks(), ",") + ".");

			/*
			 * Only allow the player to assign ranks if they have the grant perm
			 * for it.
			 */
			if (!TownyUniverse.getPermissionSource().testPermission(player, PermissionNodes.TOWNY_COMMAND_TOWN_RANK.getNode(rank)))
				throw new TownyException("You do not have permission to grant this rank.");

			if (split[0].equalsIgnoreCase("add")) {
				try {
					if (target.addTownRank(rank)) {
						TownyMessaging.sendMsg(target, "You have been granted the Town rank of '" + rank + "'.");
						TownyMessaging.sendMsg(player, "You have granted the Town rank of '" + rank + "' to " + target.getName() + ".");
					} else {
						// Not in a town or Rank doesn't exist
						TownyMessaging.sendErrorMsg(player, "That resident isn't a member of a town!");
						return;
					}
				} catch (AlreadyRegisteredException e) {
					// Must already have this rank
					TownyMessaging.sendMsg(player, target.getName() + " already holds this Town rank.");
					return;
				}

			} else if (split[0].equalsIgnoreCase("remove")) {
				try {
					if (target.removeTownRank(rank)) {
						TownyMessaging.sendMsg(target, "You have been demoted from the Town rank of '" + rank + "'.");
						TownyMessaging.sendMsg(player, "You have removed the Town rank of '" + rank + "' from " + target.getName() + ".");
					}
				} catch (NotRegisteredException e) {
					// Must already have this rank
					TownyMessaging.sendMsg(player, target.getName() + " doesn't hold this Town rank.");
					return;
				}

			} else {
				TownyMessaging.sendErrorMsg(player, String.format(TownySettings.getLangString("msg_err_invalid_property"), split[0]));
				return;
			}

			/*
			 * If we got here we have made a change Save the altered resident
			 * data.
			 */
			TownyUniverse.getDataSource().saveResident(target);

		}

	}
	
	public void townSet(Player player, String[] split) throws TownyException {

		if (split.length == 0) {
			player.sendMessage(ChatTools.formatTitle("/town set"));
			player.sendMessage(ChatTools.formatCommand("", "/town set", "board [message ... ]", ""));
			player.sendMessage(ChatTools.formatCommand("", "/town set", "mayor " + TownySettings.getLangString("town_help_2"), ""));
			player.sendMessage(ChatTools.formatCommand("", "/town set", "homeblock", ""));
			player.sendMessage(ChatTools.formatCommand("", "/town set", "spawn/outpost/jail", ""));
			player.sendMessage(ChatTools.formatCommand("", "/town set", "perm ...", "'/town set perm' " + TownySettings.getLangString("res_5")));
			// player.sendMessage(ChatTools.formatCommand("", "/town set",
			// "pvp [on/off]", ""));
			player.sendMessage(ChatTools.formatCommand("", "/town set", "taxes [$]", ""));
			player.sendMessage(ChatTools.formatCommand("", "/town set", "[plottax/shoptax/embassytax] [$]", ""));
			// player.sendMessage(ChatTools.formatCommand("", "/town set",
			// "shoptax [$]", ""));
			player.sendMessage(ChatTools.formatCommand("", "/town set", "[plotprice/shopprice/embassyprice] [$]", ""));
			player.sendMessage(ChatTools.formatCommand("", "/town set", "name [name]", ""));
			player.sendMessage(ChatTools.formatCommand("", "/town set", "tag [upto 4 letters] or clear", ""));
			// player.sendMessage(ChatTools.formatCommand("", "/town set",
			// "public [on/off]", ""));
			// player.sendMessage(ChatTools.formatCommand("", "/town set",
			// "explosion [on/off]", ""));
			// player.sendMessage(ChatTools.formatCommand("", "/town set",
			// "fire [on/off]", ""));
		} else {
			Resident resident;
			Town town = null;
			Nation nation = null;
			TownyWorld oldWorld = null;

			try {
				resident = TownyUniverse.getDataSource().getResident(player.getName());
				town = resident.getTown();

				if (town.hasNation())
					nation = town.getNation();
			} catch (TownyException x) {
				TownyMessaging.sendErrorMsg(player, x.getMessage());
				return;
			}

			// TODO: Let admin's call a subfunction of this.
			if (split[0].equalsIgnoreCase("board")) {

				if (!TownyUniverse.getPermissionSource().testPermission(player, PermissionNodes.TOWNY_COMMAND_TOWN_SET_BOARD.getNode()))
					throw new TownyException(TownySettings.getLangString("msg_err_command_disable"));

				if (split.length < 2) {
					TownyMessaging.sendErrorMsg(player, "Eg: /town set board " + TownySettings.getLangString("town_help_9"));
					return;
				} else {
					String line = StringMgmt.join(StringMgmt.remFirstArg(split), " ");									
					
					if (!NameValidation.isValidString(line)){
						TownyMessaging.sendErrorMsg(player, "Invalid string, Town Board not set.");
						return;
					}
						
					town.setTownBoard(line);
					TownyMessaging.sendTownBoard(player, town);
				}

			} else {

				/*
				 * Test we have permission to use this command.
				 */
				if (!TownyUniverse.getPermissionSource().testPermission(player, PermissionNodes.TOWNY_COMMAND_TOWN_SET.getNode(split[0].toLowerCase())))
					throw new TownyException(TownySettings.getLangString("msg_err_command_disable"));

				if (split[0].equalsIgnoreCase("mayor")) {

					if (split.length < 2) {
						TownyMessaging.sendErrorMsg(player, "Eg: /town set mayor Dumbo");
						return;
					} else
						try {
							if (!resident.isMayor())
								throw new TownyException(TownySettings.getLangString("msg_not_mayor"));

							String oldMayor = town.getMayor().getName();
							Resident newMayor = TownyUniverse.getDataSource().getResident(split[1]);
							town.setMayor(newMayor);
							TownyPerms.assignPermissions(TownyUniverse.getDataSource().getResident(oldMayor), null);
							plugin.deleteCache(oldMayor);
							plugin.deleteCache(newMayor.getName());
							TownyMessaging.sendTownMessage(town, TownySettings.getNewMayorMsg(newMayor.getName()));
						} catch (TownyException e) {
							TownyMessaging.sendErrorMsg(player, e.getMessage());
							return;
						}

				} else if (split[0].equalsIgnoreCase("taxes")) {

					if (split.length < 2) {
						TownyMessaging.sendErrorMsg(player, "Eg: /town set taxes 7");
						return;
					} else {
						try {
							Double amount = Double.parseDouble(split[1]);
							if (amount < 0) {
								TownyMessaging.sendErrorMsg(player, TownySettings.getLangString("msg_err_negative_money"));
								return;
							}
							if (town.isTaxPercentage() && amount > 100) {
								TownyMessaging.sendErrorMsg(player, TownySettings.getLangString("msg_err_not_percentage"));
								return;
							}
							town.setTaxes(amount);
							TownyMessaging.sendTownMessage(town, String.format(TownySettings.getLangString("msg_town_set_tax"), player.getName(), split[1]));
						} catch (NumberFormatException e) {
							TownyMessaging.sendErrorMsg(player, TownySettings.getLangString("msg_error_must_be_num"));
							return;
						}
					}

				} else if (split[0].equalsIgnoreCase("plottax")) {

					if (split.length < 2) {
						TownyMessaging.sendErrorMsg(player, "Eg: /town set plottax 10");
						return;
					} else {
						try {
							Double amount = Double.parseDouble(split[1]);
							if (amount < 0) {
								TownyMessaging.sendErrorMsg(player, TownySettings.getLangString("msg_err_negative_money"));
								return;
							}
							town.setPlotTax(amount);
							TownyMessaging.sendTownMessage(town, String.format(TownySettings.getLangString("msg_town_set_plottax"), player.getName(), split[1]));
						} catch (NumberFormatException e) {
							TownyMessaging.sendErrorMsg(player, TownySettings.getLangString("msg_error_must_be_num"));
							return;
						}
					}
				} else if (split[0].equalsIgnoreCase("shoptax")) {

					if (split.length < 2) {
						TownyMessaging.sendErrorMsg(player, "Eg: /town set shoptax 10");
						return;
					} else {
						try {
							Double amount = Double.parseDouble(split[1]);
							if (amount < 0) {
								TownyMessaging.sendErrorMsg(player, TownySettings.getLangString("msg_err_negative_money"));
								return;
							}
							town.setCommercialPlotTax(amount);
							TownyMessaging.sendTownMessage(town, String.format(TownySettings.getLangString("msg_town_set_alttax"), player.getName(), "shop", split[1]));
						} catch (NumberFormatException e) {
							TownyMessaging.sendErrorMsg(player, TownySettings.getLangString("msg_error_must_be_num"));
							return;
						}
					}

				} else if (split[0].equalsIgnoreCase("embassytax")) {

					if (split.length < 2) {
						TownyMessaging.sendErrorMsg(player, "Eg: /town set embassytax 10");
						return;
					} else {
						try {
							Double amount = Double.parseDouble(split[1]);
							if (amount < 0) {
								TownyMessaging.sendErrorMsg(player, TownySettings.getLangString("msg_err_negative_money"));
								return;
							}
							town.setEmbassyPlotTax(amount);
							TownyMessaging.sendTownMessage(town, String.format(TownySettings.getLangString("msg_town_set_alttax"), player.getName(), "embassy", split[1]));
						} catch (NumberFormatException e) {
							TownyMessaging.sendErrorMsg(player, TownySettings.getLangString("msg_error_must_be_num"));
							return;
						}
					}

				} else if (split[0].equalsIgnoreCase("plotprice")) {

					if (split.length < 2) {
						TownyMessaging.sendErrorMsg(player, "Eg: /town set plotprice 50");
						return;
					} else {
						try {
							Double amount = Double.parseDouble(split[1]);
							if (amount < 0) {
								TownyMessaging.sendErrorMsg(player, TownySettings.getLangString("msg_err_negative_money"));
								return;
							}
							town.setPlotPrice(amount);
							TownyMessaging.sendTownMessage(town, String.format(TownySettings.getLangString("msg_town_set_plotprice"), player.getName(), split[1]));
						} catch (NumberFormatException e) {
							TownyMessaging.sendErrorMsg(player, TownySettings.getLangString("msg_error_must_be_num"));
							return;
						}
					}

				} else if (split[0].equalsIgnoreCase("shopprice")) {

					if (split.length < 2) {
						TownyMessaging.sendErrorMsg(player, "Eg: /town set shopprice 50");
						return;
					} else {
						try {
							Double amount = Double.parseDouble(split[1]);
							if (amount < 0) {
								TownyMessaging.sendErrorMsg(player, TownySettings.getLangString("msg_err_negative_money"));
								return;
							}
							town.setCommercialPlotPrice(amount);
							TownyMessaging.sendTownMessage(town, String.format(TownySettings.getLangString("msg_town_set_altprice"), player.getName(), "shop", split[1]));
						} catch (NumberFormatException e) {
							TownyMessaging.sendErrorMsg(player, TownySettings.getLangString("msg_error_must_be_num"));
							return;
						}
					}
				} else if (split[0].equalsIgnoreCase("embassyprice")) {

					if (split.length < 2) {
						TownyMessaging.sendErrorMsg(player, "Eg: /town set embassyprice 50");
						return;
					} else {
						try {
							Double amount = Double.parseDouble(split[1]);
							if (amount < 0) {
								TownyMessaging.sendErrorMsg(player, TownySettings.getLangString("msg_err_negative_money"));
								return;
							}
							town.setEmbassyPlotPrice(amount);
							TownyMessaging.sendTownMessage(town, String.format(TownySettings.getLangString("msg_town_set_altprice"), player.getName(), "embassy", split[1]));
						} catch (NumberFormatException e) {
							TownyMessaging.sendErrorMsg(player, TownySettings.getLangString("msg_error_must_be_num"));
							return;
						}
					}

				} else if (split[0].equalsIgnoreCase("name")) {

					if (split.length < 2) {
						TownyMessaging.sendErrorMsg(player, "Eg: /town set name BillyBobTown");
						return;
					}

					if (!NameValidation.isBlacklistName(split[1]))
						townRename(player, town, split[1]);
					else
						TownyMessaging.sendErrorMsg(player, TownySettings.getLangString("msg_invalid_name"));

				} else if (split[0].equalsIgnoreCase("tag")) {

					if (split.length < 2)
						TownyMessaging.sendErrorMsg(player, "Eg: /town set tag PLTC");
					else if (split[1].equalsIgnoreCase("clear")) {
						try {
							town.setTag(" ");
							TownyMessaging.sendTownMessage(town, String.format(TownySettings.getLangString("msg_reset_town_tag"), player.getName()));
						} catch (TownyException e) {
							TownyMessaging.sendErrorMsg(player, e.getMessage());
						}
					} else
						try {
							town.setTag(NameValidation.checkAndFilterName(split[1]));
							TownyMessaging.sendTownMessage(town, String.format(TownySettings.getLangString("msg_set_town_tag"), player.getName(), town.getTag()));
						} catch (TownyException e) {
							TownyMessaging.sendErrorMsg(player, e.getMessage());
						} catch (InvalidNameException e) {
							TownyMessaging.sendErrorMsg(player, e.getMessage());
						}

				} else if (split[0].equalsIgnoreCase("homeblock")) {

					Coord coord = Coord.parseCoord(player);
					TownBlock townBlock;
					TownyWorld world;
					try {
						if (TownyUniverse.isWarTime())
							throw new TownyException(TownySettings.getLangString("msg_war_cannot_do"));

						world = TownyUniverse.getDataSource().getWorld(player.getWorld().getName());
						if (world.getMinDistanceFromOtherTowns(coord, resident.getTown()) < TownySettings.getMinDistanceFromTownHomeblocks())
							throw new TownyException(TownySettings.getLangString("msg_too_close"));

						if (TownySettings.getMaxDistanceBetweenHomeblocks() > 0)
							if ((world.getMinDistanceFromOtherTowns(coord, resident.getTown()) > TownySettings.getMaxDistanceBetweenHomeblocks()) && world.hasTowns())
								throw new TownyException(TownySettings.getLangString("msg_too_far"));

						townBlock = TownyUniverse.getDataSource().getWorld(player.getWorld().getName()).getTownBlock(coord);
						oldWorld = town.getWorld();
						town.setHomeBlock(townBlock);
						town.setSpawn(player.getLocation());

						TownyMessaging.sendMsg(player, String.format(TownySettings.getLangString("msg_set_town_home"), coord.toString()));

					} catch (TownyException e) {
						TownyMessaging.sendErrorMsg(player, e.getMessage());
						return;
					}

				} else if (split[0].equalsIgnoreCase("spawn")) {

					try {
						town.setSpawn(player.getLocation());
						TownyMessaging.sendMsg(player, TownySettings.getLangString("msg_set_town_spawn"));
					} catch (TownyException e) {
						TownyMessaging.sendErrorMsg(player, e.getMessage());
						return;
					}

				} else if (split[0].equalsIgnoreCase("outpost")) {

					try {
						town.addOutpostSpawn(player.getLocation());
						TownyMessaging.sendMsg(player, TownySettings.getLangString("msg_set_outpost_spawn"));
					} catch (TownyException e) {
						TownyMessaging.sendErrorMsg(player, e.getMessage());
						return;
					}
					
				} else if (split[0].equalsIgnoreCase("jail")) {

					try {
						town.addJailSpawn(player.getLocation());
						// TODO: add msg_set_jail_spawn						
						TownyMessaging.sendMsg(player, "Successfully set jail plot's spawn.");
					} catch (TownyException e) {
						TownyMessaging.sendErrorMsg(player, e.getMessage());
						return;
					}

				} else if (split[0].equalsIgnoreCase("perm")) {

					// Make sure we are allowed to set these permissions.
					try {
						toggleTest(player, town, StringMgmt.join(split, " "));
					} catch (Exception e) {
						TownyMessaging.sendErrorMsg(player, e.getMessage());
						return;
					}
					String[] newSplit = StringMgmt.remFirstArg(split);
					setTownBlockOwnerPermissions(player, town, newSplit);

				} else {
					TownyMessaging.sendErrorMsg(player, String.format(TownySettings.getLangString("msg_err_invalid_property"), "town"));
					return;
				}
			}

			TownyUniverse.getDataSource().saveTown(town);
			TownyUniverse.getDataSource().saveTownList();

			if (nation != null) {
				TownyUniverse.getDataSource().saveNation(nation);
				// TownyUniverse.getDataSource().saveNationList();
			}

			// If the town (homeblock) has moved worlds we need to update the
			// world files.
			if (oldWorld != null) {
				TownyUniverse.getDataSource().saveWorld(town.getWorld());
				TownyUniverse.getDataSource().saveWorld(oldWorld);
			}
		}
	}

	
	public void townBuy(Player player, String[] split) {

		Resident resident;
		Town town;
		try {
			resident = TownyUniverse.getDataSource().getResident(player.getName());
			town = resident.getTown();

		} catch (TownyException x) {
			TownyMessaging.sendErrorMsg(player, x.getMessage());
			return;
		}
		if (split.length == 0) {
			player.sendMessage(ChatTools.formatTitle("/town buy"));
			if (TownySettings.isSellingBonusBlocks()) {
				String line = Colors.Yellow + "[Purchased Bonus] " + Colors.Green + "Cost: " + Colors.LightGreen + "%s" + Colors.Gray + " | " + Colors.Green + "Max: " + Colors.LightGreen + "%d";
				player.sendMessage(String.format(line, TownyEconomyHandler.getFormattedBalance(town.getBonusBlockCost()), TownySettings.getMaxPurchedBlocks()));
				player.sendMessage(ChatTools.formatCommand("", "/town buy", "bonus [n]", ""));
			} else {
				// Temp placeholder.
				player.sendMessage("Nothing for sale right now.");
			}
		} else {
			try {
				if (split[0].equalsIgnoreCase("bonus")) {
					if (split.length == 2) {
						try {
							townBuyBonusTownBlocks(town, Integer.parseInt(split[1].trim()), player);
						} catch (NumberFormatException e) {
							throw new TownyException(TownySettings.getLangString("msg_error_must_be_int"));
						}
					} else {
						throw new TownyException(String.format(TownySettings.getLangString("msg_must_specify_amnt"), "/town buy bonus"));
					}
				}

				TownyUniverse.getDataSource().saveTown(town);
			} catch (TownyException x) {
				TownyMessaging.sendErrorMsg(player, x.getMessage());
			}
		}
	}

	/**
	 * Town buys bonus blocks after checking the configured maximum.
	 * 
	 * @param town
	 * @param inputN
	 * @param player
	 * @return The number of purchased bonus blocks.
	 * @throws TownyException
	 */
	public static int townBuyBonusTownBlocks(Town town, int inputN, Object player) throws TownyException {

		if (inputN < 0)
			throw new TownyException(TownySettings.getLangString("msg_err_negative"));

		int current = town.getPurchasedBlocks();

		int n;
		if (current + inputN > TownySettings.getMaxPurchedBlocks()) {
			n = TownySettings.getMaxPurchedBlocks() - current;
		} else {
			n = inputN;
		}

		if (n == 0)
			return n;
		double cost = town.getBonusBlockCostN(n);
		try {
			boolean pay = town.pay(cost, String.format("Town Buy Bonus (%d)", n));
			if (TownySettings.isUsingEconomy() && !pay) {
				throw new TownyException(String.format(TownySettings.getLangString("msg_no_funds_to_buy"), n, "bonus town blocks", TownyEconomyHandler.getFormattedBalance(cost)));
			} else if (TownySettings.isUsingEconomy() && pay) {
				town.addPurchasedBlocks(n);
				TownyMessaging.sendMsg(player, String.format(TownySettings.getLangString("msg_buy"), n, "bonus town blocks", TownyEconomyHandler.getFormattedBalance(cost)));
			}

		} catch (EconomyException e1) {
			throw new TownyException("Economy Error");
		}

		return n;
	}

	/**
	 * Create a new town. Command: /town new [town] *[mayor]
	 * 
	 * @param player
	 */

	public void newTown(Player player, String name, String mayorName) {

		try {
			if (TownyUniverse.isWarTime())
				throw new TownyException(TownySettings.getLangString("msg_war_cannot_do"));

			if (TownySettings.hasTownLimit() && TownyUniverse.getDataSource().getTowns().size() >= TownySettings.getTownLimit())
				throw new TownyException(TownySettings.getLangString("msg_err_universe_limit"));

			// Check the name is valid and doesn't already exist.
			String filteredName;
			try {
				filteredName = NameValidation.checkAndFilterName(name);
			} catch (InvalidNameException e) {
				filteredName = null;
			}

			if ((filteredName == null) || TownyUniverse.getDataSource().hasTown(filteredName))
				throw new TownyException(String.format(TownySettings.getLangString("msg_err_invalid_name"), name));

			Resident resident = TownyUniverse.getDataSource().getResident(mayorName);
			if (resident.hasTown())
				throw new TownyException(String.format(TownySettings.getLangString("msg_err_already_res"), resident.getName()));

			TownyWorld world = TownyUniverse.getDataSource().getWorld(player.getWorld().getName());

			if (!world.isUsingTowny())
				throw new TownyException(TownySettings.getLangString("msg_set_use_towny_off"));

			if (!world.isClaimable())
				throw new TownyException(TownySettings.getLangString("msg_not_claimable"));

			Coord key = Coord.parseCoord(player);

			if (world.hasTownBlock(key))
				throw new TownyException(String.format(TownySettings.getLangString("msg_already_claimed_1"), key));

			if (world.getMinDistanceFromOtherTowns(key) < TownySettings.getMinDistanceFromTownHomeblocks())
				throw new TownyException(TownySettings.getLangString("msg_too_close"));

			if (TownySettings.getMaxDistanceBetweenHomeblocks() > 0)
				if ((world.getMinDistanceFromOtherTowns(key) > TownySettings.getMaxDistanceBetweenHomeblocks()) && world.hasTowns())
					throw new TownyException(TownySettings.getLangString("msg_too_far"));

			if (TownySettings.isUsingEconomy() && !resident.pay(TownySettings.getNewTownPrice(), "New Town Cost"))
				throw new TownyException(String.format(TownySettings.getLangString("msg_no_funds_new_town"), (resident.getName().equals(player.getName()) ? "You" : resident.getName())));

			newTown(world, name, resident, key, player.getLocation());
			TownyMessaging.sendGlobalMessage(TownySettings.getNewTownMsg(player.getName(), name));
		} catch (TownyException x) {
			TownyMessaging.sendErrorMsg(player, x.getMessage());
			// TODO: delete town data that might have been done
		} catch (EconomyException x) {
			TownyMessaging.sendErrorMsg(player, x.getMessage());
		}
	}

	public Town newTown(TownyWorld world, String name, Resident resident, Coord key, Location spawn) throws TownyException {

		world.newTownBlock(key);
		TownyUniverse.getDataSource().newTown(name);
		Town town = TownyUniverse.getDataSource().getTown(name);
		town.addResident(resident);
		town.setMayor(resident);
		TownBlock townBlock = world.getTownBlock(key);
		townBlock.setTown(town);
		town.setHomeBlock(townBlock);
		// Set the plot permissions to mirror the towns.
		townBlock.setType(townBlock.getType());

		town.setSpawn(spawn);
		// world.addTown(town);

		if (world.isUsingPlotManagementRevert()) {
			PlotBlockData plotChunk = TownyRegenAPI.getPlotChunk(townBlock);
			if (plotChunk != null) {

				TownyRegenAPI.deletePlotChunk(plotChunk); // just claimed so stop regeneration.

			} else {

				plotChunk = new PlotBlockData(townBlock); // Not regenerating so create a new snapshot.
				plotChunk.initialize();

			}
			TownyRegenAPI.addPlotChunkSnapshot(plotChunk); // Save a snapshot.
			plotChunk = null;
		}
		TownyMessaging.sendDebugMsg("Creating new Town account: " + "town-" + name);
		if (TownySettings.isUsingEconomy()) {
			// TODO
			try {
				town.setBalance(0, "Deleting Town");
			} catch (EconomyException e) {
				e.printStackTrace();
			}
		}

		TownyUniverse.getDataSource().saveResident(resident);
		TownyUniverse.getDataSource().saveTownBlock(townBlock);
		TownyUniverse.getDataSource().saveTown(town);
		TownyUniverse.getDataSource().saveWorld(world);

		TownyUniverse.getDataSource().saveTownList();
		TownyUniverse.getDataSource().saveTownBlockList();

		// Reset cache permissions for anyone in this TownBlock
		plugin.updateCache(townBlock.getWorldCoord());

		BukkitTools.getPluginManager().callEvent(new NewTownEvent(town));

		return town;
	}

	public void townRename(Player player, Town town, String newName) {

		try {
			TownyUniverse.getDataSource().renameTown(town, newName);
			town = TownyUniverse.getDataSource().getTown(newName);
			TownyMessaging.sendTownMessage(town, String.format(TownySettings.getLangString("msg_town_set_name"), player.getName(), town.getName()));
		} catch (TownyException e) {
			TownyMessaging.sendErrorMsg(player, e.getMessage());
		}
	}

	public void townLeave(Player player) {

		Resident resident;
		Town town;
		try {
			// TODO: Allow leaving town during war.
			if (TownyUniverse.isWarTime())
				throw new TownyException(TownySettings.getLangString("msg_war_cannot_do"));

			resident = TownyUniverse.getDataSource().getResident(player.getName());
			town = resident.getTown();
			plugin.deleteCache(resident.getName());

		} catch (TownyException x) {
			TownyMessaging.sendErrorMsg(player, x.getMessage());
			return;
		}

		if (resident.isMayor()) {
			TownyMessaging.sendErrorMsg(player, TownySettings.getMayorAbondonMsg());
			return;
		}

		if (resident.isJailed()) {
			if (TownySettings.JailDeniesTownLeave()) {
				TownyMessaging.sendErrorMsg(player, "Abandoning your town is not allowed when you are jailed.");
				return;
			}
			resident.setJailed(false);
			resident.setJailSpawn(0);
			resident.setJailTown("");
			TownyMessaging.sendTownMessage(town, String.format(resident.getName() + " has escaped jail by becoming a nomad.", resident.getName()));
		} 
		
		try {
			townRemoveResident(town, resident);
		} catch (EmptyTownException et) {
			TownyUniverse.getDataSource().removeTown(et.getTown());

		} catch (NotRegisteredException x) {
			TownyMessaging.sendErrorMsg(player, x.getMessage());
			return;
		}
		
		
		TownyUniverse.getDataSource().saveResident(resident);
		TownyUniverse.getDataSource().saveTown(town);

		// Reset everyones cache permissions as this player leaving could affect
		// multiple areas
		plugin.resetCache();

		TownyMessaging.sendTownMessage(town, String.format(TownySettings.getLangString("msg_left_town"), resident.getName()));
		TownyMessaging.sendMsg(player, String.format(TownySettings.getLangString("msg_left_town"), resident.getName()));
		
	    try {
	    	checkTownResidents(town, resident);
		} catch (NotRegisteredException e) {
			e.printStackTrace();
		 }
	}

	/**
	 * Wrapper for the townSpawn() method. All calls should be through here
	 * unless bypassing for admins.
	 * 
	 * @param player
	 * @param split
	 * @param outpost
	 * @throws TownyException 
	 */
	public static void townSpawn(Player player, String[] split, Boolean outpost) throws TownyException {

		try {

			Resident resident = TownyUniverse.getDataSource().getResident(player.getName());
			Town town;
			String notAffordMSG;

			// Set target town and affiliated messages.
			if ((split.length == 0) || ((split.length > 0) && (outpost))) {

				if(!resident.hasTown()) {
					TownyMessaging.sendErrorMsg(player, TownySettings.getLangString("msg_err_dont_belong_town"));
					return;
				}
				
				town = resident.getTown();
				notAffordMSG = TownySettings.getLangString("msg_err_cant_afford_tp");

				townSpawn(player, split, town, notAffordMSG, outpost);

			} else {
				// split.length > 1
				town = TownyUniverse.getDataSource().getTown(split[0]);
				notAffordMSG = String.format(TownySettings.getLangString("msg_err_cant_afford_tp_town"), town.getName());

				townSpawn(player, split, town, notAffordMSG, outpost);

			}
		} catch (NotRegisteredException e) {

			throw new TownyException(String.format(TownySettings.getLangString("msg_err_not_registered_1"), split[0]));

		}

	}

	/**
	 * Core spawn function to allow admin use.
	 * 
	 * @param player
	 * @param split
	 * @param town
	 * @param notAffordMSG
	 * @param outpost
	 */
	public static void townSpawn(Player player, String[] split, Town town, String notAffordMSG, Boolean outpost) {

		try {

			boolean isTownyAdmin = TownyUniverse.getPermissionSource().isTownyAdmin(player);
			Resident resident = TownyUniverse.getDataSource().getResident(player.getName());
			Location spawnLoc;
			TownSpawnLevel townSpawnPermission;

			if (outpost) {

				if (!town.hasOutpostSpawn())
					throw new TownyException(TownySettings.getLangString("msg_err_outpost_spawn"));

				Integer index;
				try {
					index = Integer.parseInt(split[split.length - 1]);
				} catch (NumberFormatException e) {
					// invalid entry so assume the first outpost
					index = 1;
				} catch (ArrayIndexOutOfBoundsException i) {
					// Number not present so assume the first outpost.
					index = 1;
				}
				spawnLoc = town.getOutpostSpawn(Math.max(1, index));
			} else
				spawnLoc = town.getSpawn();

			// Determine conditions
			if (isTownyAdmin) {
				townSpawnPermission = TownSpawnLevel.ADMIN;
			} else if ((split.length == 0) && (!outpost)) {
				townSpawnPermission = TownSpawnLevel.TOWN_RESIDENT;
			} else {
				// split.length > 1
				if (!resident.hasTown()) {
					townSpawnPermission = TownSpawnLevel.UNAFFILIATED;
				} else if (resident.getTown() == town) {
					townSpawnPermission = outpost ? TownSpawnLevel.TOWN_RESIDENT_OUTPOST : TownSpawnLevel.TOWN_RESIDENT;
				} else if (resident.hasNation() && town.hasNation()) {
					Nation playerNation = resident.getTown().getNation();
					Nation targetNation = town.getNation();

					if (playerNation == targetNation) {
						townSpawnPermission = TownSpawnLevel.PART_OF_NATION;
					} else if (targetNation.hasEnemy(playerNation)) {
						// Prevent enemies from using spawn travel.
						throw new TownyException(TownySettings.getLangString("msg_err_public_spawn_enemy"));
					} else if (targetNation.hasAlly(playerNation)) {
						townSpawnPermission = TownSpawnLevel.NATION_ALLY;
					} else {
						townSpawnPermission = TownSpawnLevel.UNAFFILIATED;
					}
				} else {
					townSpawnPermission = TownSpawnLevel.UNAFFILIATED;
				}
			}

			TownyMessaging.sendDebugMsg(townSpawnPermission.toString() + " " + townSpawnPermission.isAllowed());
			townSpawnPermission.checkIfAllowed(plugin, player);

			// Check the permissions
			if (!(isTownyAdmin || ((townSpawnPermission == TownSpawnLevel.UNAFFILIATED) ? town.isPublic() : townSpawnPermission.hasPermissionNode(plugin, player)))) {

				throw new TownyException(TownySettings.getLangString("msg_err_not_public"));

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
						throw new TownyException(String.format(TownySettings.getLangString("msg_err_town_spawn_disallowed_from"), "the Wilderness"));
					if (inTown != null && resident.hasNation() && TownyUniverse.getDataSource().getTown(inTown).hasNation()) {
						Nation inNation = TownyUniverse.getDataSource().getTown(inTown).getNation();
						Nation playerNation = resident.getTown().getNation();
						if (inNation.hasEnemy(playerNation) && disallowedZones.contains("enemy"))
							throw new TownyException(String.format(TownySettings.getLangString("msg_err_town_spawn_disallowed_from"), "Enemy areas"));
						if (!inNation.hasAlly(playerNation) && !inNation.hasEnemy(playerNation) && disallowedZones.contains("neutral"))
							throw new TownyException(String.format(TownySettings.getLangString("msg_err_town_spawn_disallowed_from"), "Neutral towns"));
					}
				}
			}

			double travelCost = townSpawnPermission.getCost();

			// Check if need/can pay
			if (travelCost > 0 && TownySettings.isUsingEconomy() && (resident.getHoldingBalance() < travelCost))
				throw new TownyException(notAffordMSG);

			// Used later to make sure the chunk we teleport to is loaded.
			Chunk chunk = spawnLoc.getChunk();
			
			// isJailed test
			if (resident.isJailed()) {
				TownyMessaging.sendErrorMsg(player, "Can not spawn while Jailed.");
				return;
			}
			
			// Essentials tests
			boolean UsingESS = plugin.isEssentials();

			if (UsingESS && !isTownyAdmin) {
				try {
					User user = plugin.getEssentials().getUser(player);

					if (!user.isJailed() && !resident.isJailed()) {

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
			if (travelCost > 0 && TownySettings.isUsingEconomy() && resident.payTo(travelCost, town, String.format("Town Spawn (%s)", townSpawnPermission))) {
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

	public void townDelete(Player player, String[] split) {

		Town town = null;

		if (split.length == 0)
			try {
				Resident resident = TownyUniverse.getDataSource().getResident(player.getName());
				town = resident.getTown();

			} catch (TownyException x) {
				TownyMessaging.sendErrorMsg(player, x.getMessage());
				return;
			}
		else
			try {
				if (!TownyUniverse.getPermissionSource().testPermission(player, PermissionNodes.TOWNY_COMMAND_TOWNYADMIN_TOWN_DELETE.getNode()))
					throw new TownyException(TownySettings.getLangString("msg_err_admin_only_delete_town"));

				town = TownyUniverse.getDataSource().getTown(split[0]);

			} catch (TownyException x) {
				TownyMessaging.sendErrorMsg(player, x.getMessage());
				return;
			}

		// Use questioner to confirm.
		Plugin test = BukkitTools.getServer().getPluginManager().getPlugin("Questioner");

		if (TownySettings.isUsingQuestioner() && test != null && test instanceof Questioner && test.isEnabled()) {
			Questioner questioner = (Questioner) test;
			questioner.loadClasses();

			List<Option> options = new ArrayList<Option>();
			options.add(new Option(TownySettings.questionerAccept(), new TownQuestionTask(player, town) {

				@Override
				public void run() {

					TownyUniverse.getDataSource().removeTown(town);
					TownyMessaging.sendGlobalMessage(TownySettings.getDelTownMsg(town));
				}

			}));
			options.add(new Option(TownySettings.questionerDeny(), new TownQuestionTask(player, town) {

				@Override
				public void run() {

					TownyMessaging.sendMessage(getSender(), "Delete Aborted!");
				}
			}));
			String output = "Do you really want to delete this town";
			if (TownyUniverse.getDataSource().getTownWorld(town.getName()).isUsingPlotManagementRevert())
				TownyMessaging.sendMessage(player, TownySettings.getLangString("default_towny_prefix") + Colors.Red + "Warning: Deleting this town will revert all townblocks to their pre-claimed state.");
			Question question = new Question(player.getName(), output, options);

			try {
				plugin.appendQuestion(questioner, question);
			} catch (Exception e) {
				System.out.println(e.getMessage());
			}
		} else {
			TownyMessaging.sendGlobalMessage(TownySettings.getDelTownMsg(town));
			TownyUniverse.getDataSource().removeTown(town);
			
		}
	}

	/**
	 * Confirm player is a mayor or assistant, then get list of filter names
	 * with online players and kick them from town. Command: /town kick
	 * [resident] .. [resident]
	 * 
	 * @param player
	 * @param names
	 */

	public static void townKick(Player player, String[] names) {

		Resident resident;
		Town town;
		try {
			resident = TownyUniverse.getDataSource().getResident(player.getName());
			town = resident.getTown();
		} catch (TownyException x) {
			TownyMessaging.sendErrorMsg(player, x.getMessage());
			return;
		}

		townKickResidents(player, resident, town, TownyUniverse.getValidatedResidents(player, names));

		// Reset everyones cache permissions as this player leaving can affect
		// multiple areas.
		plugin.resetCache();
	}

	/*
	 * private static List<Resident> getResidents(Player player, String[] names)
	 * { List<Resident> invited = new ArrayList<Resident>(); for (String name :
	 * names) try { Resident target =
	 * plugin.getTownyUniverse().getResident(name); invited.add(target); } catch
	 * (TownyException x) { TownyMessaging.sendErrorMsg(player, x.getMessage());
	 * } return invited; }
	 */
	public static void townAddResidents(Object sender, Town town, List<Resident> invited) {

		for (Resident newMember : new ArrayList<Resident>(invited)) {
			try {
				// only add players with the right permissions.
				if (plugin.isPermissions()) {
					if (BukkitTools.matchPlayer(newMember.getName()).isEmpty()) { // Not
																					// online
						TownyMessaging.sendErrorMsg(sender, String.format(TownySettings.getLangString("msg_offline_no_join"), newMember.getName()));
						invited.remove(newMember);
					} else if (!TownyUniverse.getPermissionSource().has(BukkitTools.getPlayer(newMember.getName()), PermissionNodes.TOWNY_TOWN_RESIDENT.getNode())) {
						TownyMessaging.sendErrorMsg(sender, String.format(TownySettings.getLangString("msg_not_allowed_join"), newMember.getName()));
						invited.remove(newMember);
					} else {
						town.addResidentCheck(newMember);
						townInviteResident(town, newMember);
					}
				} else {
					town.addResidentCheck(newMember);
					townInviteResident(town, newMember);
				}
			} catch (AlreadyRegisteredException e) {
				invited.remove(newMember);
				TownyMessaging.sendErrorMsg(sender, e.getMessage());
			}
		}

		if (invited.size() > 0) {
			String msg = "";
			for (Resident newMember : invited)
				msg += newMember.getName() + ", ";

			msg = msg.substring(0, msg.length() - 2);

			String name;

			if (sender instanceof Player) {
				name = ((Player) sender).getName();
			} else
				name = "Console";

			msg = String.format(TownySettings.getLangString("msg_invited_join_town"), name, msg);
			TownyMessaging.sendTownMessage(town, ChatTools.color(msg));
			TownyUniverse.getDataSource().saveTown(town);
		} else
			TownyMessaging.sendErrorMsg(sender, TownySettings.getLangString("msg_invalid_name"));
	}

	public static void townAddResident(Town town, Resident resident) throws AlreadyRegisteredException {

		town.addResident(resident);
		plugin.deleteCache(resident.getName());
		TownyUniverse.getDataSource().saveResident(resident);
		TownyUniverse.getDataSource().saveTown(town);

		plugin.getTownyUniverse().setChangedNotify(TOWN_ADD_RESIDENT);
	}

	private static void townInviteResident(Town town, Resident newMember) throws AlreadyRegisteredException {

		Plugin test = BukkitTools.getServer().getPluginManager().getPlugin("Questioner");

		if (TownySettings.isUsingQuestioner() && test != null && test instanceof Questioner && test.isEnabled()) {
			Questioner questioner = (Questioner) test;
			questioner.loadClasses();

			List<Option> options = new ArrayList<Option>();
			options.add(new Option(TownySettings.questionerAccept(), new JoinTownTask(newMember, town)));
			options.add(new Option(TownySettings.questionerDeny(), new ResidentTownQuestionTask(newMember, town) {

				@Override
				public void run() {

					TownyMessaging.sendTownMessage(getTown(), String.format(TownySettings.getLangString("msg_deny_invite"), getResident().getName()));
				}
			}));
			Question question = new Question(newMember.getName(), String.format(TownySettings.getLangString("msg_invited"), TownySettings.getLangString("town_sing") + ": " +  town.getName()), options);
			try {
				plugin.appendQuestion(questioner, question);
			} catch (Exception e) {
				System.out.println(e.getMessage());
			}
		} else
			try {
				townAddResident(town, newMember);
			} catch (AlreadyRegisteredException e) {
			}
	}

	public static void townRemoveResident(Town town, Resident resident) throws EmptyTownException, NotRegisteredException {

		town.removeResident(resident);
		plugin.deleteCache(resident.getName());
		TownyUniverse.getDataSource().saveResident(resident);
		TownyUniverse.getDataSource().saveTown(town);

		plugin.getTownyUniverse().setChangedNotify(TOWN_REMOVE_RESIDENT);
	}

	public static void townKickResidents(Object sender, Resident resident, Town town, List<Resident> kicking) {

		Player player = null;

		if (sender instanceof Player)
			player = (Player) sender;

		for (Resident member : new ArrayList<Resident>(kicking)) {
			if (resident == member || member.isMayor() || town.hasAssistant(member)) {
				TownyMessaging.sendMessage(sender, "You cannot kick yourself, the mayor, or an assistant.");
				kicking.remove(member);
			} else {
				try {
					townRemoveResident(town, member);
				} catch (NotRegisteredException e) {
					kicking.remove(member);
				} catch (EmptyTownException e) {
					// You can't kick yourself and only the mayor can kick
					// assistants
					// so there will always be at least one resident.
				}
			}
		}

		if (kicking.size() > 0) {
			String msg = "";
			for (Resident member : kicking) {
				msg += member.getName() + ", ";
				Player p = BukkitTools.getPlayer(member.getName());
				if (p != null)
					p.sendMessage(String.format(TownySettings.getLangString("msg_kicked_by"), (player != null) ? player.getName() : "CONSOLE"));
			}
			msg = msg.substring(0, msg.length() - 2);
			msg = String.format(TownySettings.getLangString("msg_kicked"), (player != null) ? player.getName() : "CONSOLE", msg);
			TownyMessaging.sendTownMessage(town, ChatTools.color(msg));
			TownyUniverse.getDataSource().saveTown(town);
		} else {
			TownyMessaging.sendErrorMsg(sender, TownySettings.getLangString("msg_invalid_name"));
		}
		
	    try {
	    	checkTownResidents(town, resident);
	    } catch (NotRegisteredException e) {
	    	e.printStackTrace();
	    }
	}
	
	  public static void checkTownResidents(Town town, Resident removedResident) throws NotRegisteredException {
		    if ((town.isCapital()) && (TownySettings.getNumResidentsCreateNation() > 0) && (town.getNumResidents() < TownySettings.getNumResidentsCreateNation())) {
			      for (Town newCapital : town.getNation().getTowns())
				        if (newCapital.getNumResidents() >= TownySettings.getNumResidentsCreateNation()) {
					          town.getNation().setCapital(newCapital);
					          if ((TownySettings.getNumResidentsJoinNation() > 0) && (removedResident.getTown().getNumResidents() < TownySettings.getNumResidentsJoinNation())) {
					        	  try {
					        		  town.getNation().removeTown(town);
					        		  TownyMessaging.sendNationMessage(town.getNation(), String.format(TownySettings.getLangString("msg_capital_not_enough_residents_left_nation"), town.getName()));
							      } catch (EmptyNationException e) {
							    	  e.printStackTrace();
							      }
					          }
					          TownyMessaging.sendNationMessage(newCapital.getNation(), String.format(TownySettings.getLangString("msg_not_enough_residents_no_longer_capital"), newCapital.getName()));
					          return;
				        }
			      TownyMessaging.sendNationMessage(town.getNation(), String.format(TownySettings.getLangString("msg_nation_disbanded_town_not_enough_residents"), town.getName()));
			      TownyMessaging.sendGlobalMessage(TownySettings.getDelNationMsg(town.getNation()));
			      TownyUniverse.getDataSource().removeNation(town.getNation());
			      
			      if (TownySettings.isRefundNationDisbandLowResidents()) {
			    	  try {
						town.pay(TownySettings.getNewNationPrice(), "nation refund");
					} catch (EconomyException e) {
						e.printStackTrace();
					}
				      TownyMessaging.sendTownMessage(town, String.format(TownySettings.getLangString("msg_not_enough_residents_refunded"), Double.valueOf(TownySettings.getNewNationPrice())));
			      }
		    } else if ((!town.isCapital()) && (TownySettings.getNumResidentsJoinNation() > 0) && (town.getNumResidents() < TownySettings.getNumResidentsJoinNation())) {
			      try {
			    	  TownyMessaging.sendNationMessage(town.getNation(), String.format(TownySettings.getLangString("msg_capital_not_enough_residents_left_nation"), town.getName()));
			    	  town.getNation().removeTown(town);
				   } catch (EmptyNationException e) {
					   e.printStackTrace();
				   }
		    }
	}

	/**
	 * If no arguments are given (or error), send usage of command. If sender is
	 * a player: args = [town]. Elsewise: args = [resident] [town]
	 * 
	 * @param sender
	 * @param args
	 */
	public static void parseTownJoin(CommandSender sender, String[] args) {

		try {
			Resident resident;
			Town town;
			String residentName, townName, contextualResidentName;
			boolean console = false;

			if (sender instanceof Player) {
				// Player
				if (args.length < 1)
					throw new Exception(String.format("Usage: /town join [town]"));

				Player player = (Player) sender;
				residentName = player.getName();
				townName = args[0];
				contextualResidentName = "You";
			} else {
				// Console
				if (args.length < 2)
					throw new Exception(String.format("Usage: town join [resident] [town]"));

				residentName = args[0];
				townName = args[1];
				contextualResidentName = residentName;
			}

			resident = TownyUniverse.getDataSource().getResident(residentName);
			town = TownyUniverse.getDataSource().getTown(townName);

			// Check if resident is currently in a town.
			if (resident.hasTown())
				throw new Exception(String.format(TownySettings.getLangString("msg_err_already_res"), contextualResidentName));

			if (!console) {
				// Check if town is town is free to join.
				if (!town.isOpen())
					throw new Exception(String.format(TownySettings.getLangString("msg_err_not_open"), town.getFormattedName()));
			}

			// Check if player is already in selected town (Pointless)
			// Then add player to town.
			townAddResident(town, resident);

			// Resident was added successfully.
			TownyMessaging.sendTownMessage(town, ChatTools.color(String.format(TownySettings.getLangString("msg_join_town"), resident.getName())));

		} catch (Exception e) {
			TownyMessaging.sendErrorMsg(sender, e.getMessage());
		}
	}

	/**
	 * Confirm player is a mayor or assistant, then get list of filter names
	 * with online players and invite them to town. Command: /town add
	 * [resident] .. [resident]
	 * 
	 * @param sender
	 * @param specifiedTown to add to if not null
	 * @param names
	 */

	public static void townAdd(Object sender, Town specifiedTown, String[] names) {

		String name;
		if (sender instanceof Player) {
			name = ((Player) sender).getName();
		} else {
			name = "Console";
		}
		Resident resident;
		Town town;
		try {
			if (name.equalsIgnoreCase("Console")) {
				town = specifiedTown;
			} else {
				resident = TownyUniverse.getDataSource().getResident(name);
				if (specifiedTown == null)
					town = resident.getTown();
				else
					town = specifiedTown;
			}

		} catch (TownyException x) {
			TownyMessaging.sendErrorMsg(sender, x.getMessage());
			return;
		}

		townAddResidents(sender, town, TownyUniverse.getValidatedResidents(sender, names));

		// Reset this players cached permissions
		if (!name.equalsIgnoreCase("Console"))
			plugin.resetCache(BukkitTools.getPlayerExact(name));
	}

	// wrapper function for non friend setting of perms
	public static void setTownBlockOwnerPermissions(Player player, TownBlockOwner townBlockOwner, String[] split) {

		setTownBlockPermissions(player, townBlockOwner, townBlockOwner.getPermissions(), split, false);
	}

	public static void setTownBlockPermissions(Player player, TownBlockOwner townBlockOwner, TownyPermission perm, String[] split, boolean friend) {

		if (split.length == 0 || split[0].equalsIgnoreCase("?")) {

			player.sendMessage(ChatTools.formatTitle("/... set perm"));
			player.sendMessage(ChatTools.formatCommand("Level", "[resident/ally/outsider]", "", ""));
			player.sendMessage(ChatTools.formatCommand("Type", "[build/destroy/switch/itemuse]", "", ""));
			player.sendMessage(ChatTools.formatCommand("", "set perm", "[on/off]", "Toggle all permissions"));
			player.sendMessage(ChatTools.formatCommand("", "set perm", "[level/type] [on/off]", ""));
			player.sendMessage(ChatTools.formatCommand("", "set perm", "[level] [type] [on/off]", ""));
			player.sendMessage(ChatTools.formatCommand("", "set perm", "reset", ""));
			if (townBlockOwner instanceof Town)
				player.sendMessage(ChatTools.formatCommand("Eg", "/town set perm", "ally off", ""));
			if (townBlockOwner instanceof Resident)
				player.sendMessage(ChatTools.formatCommand("Eg", "/resident set perm", "friend build on", ""));
			player.sendMessage(String.format(TownySettings.getLangString("plot_perms"), "'friend'", "'resident'"));
			player.sendMessage(TownySettings.getLangString("plot_perms_1"));

		} else {

			// reset the friend to resident so the perm settings don't fail
			if (friend && split[0].equalsIgnoreCase("friend"))
				split[0] = "resident";

			if (split.length == 1) {

				if (split[0].equalsIgnoreCase("reset")) {

					// reset all townBlock permissions (by town/resident)
					for (TownBlock townBlock : townBlockOwner.getTownBlocks()) {

						if (((townBlockOwner instanceof Town) && (!townBlock.hasResident())) || ((townBlockOwner instanceof Resident) && (townBlock.hasResident()))) {

							// Reset permissions
							townBlock.setType(townBlock.getType());
							TownyUniverse.getDataSource().saveTownBlock(townBlock);

						}
					}

					if (townBlockOwner instanceof Town)
						TownyMessaging.sendMsg(player, String.format(TownySettings.getLangString("msg_set_perms_reset"), "Town owned"));
					else
						TownyMessaging.sendMsg(player, String.format(TownySettings.getLangString("msg_set_perms_reset"), "your"));

					// Reset all caches as this can affect everyone.
					plugin.resetCache();

					return;

				} else {

					// Set all perms to On or Off
					// '/town set perm off'

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

					if (split[0].equalsIgnoreCase("resident") || split[0].equalsIgnoreCase("friend")) {
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
				}

			} else if (split.length == 3) {

				try {
					boolean b = plugin.parseOnOff(split[2]);
					String s = "";
					s = split[0] + split[1];
					perm.set(s, b);
				} catch (Exception e) {
				}

			}

			// Propagate perms to all unchanged, town owned, townblocks
			for (TownBlock townBlock : townBlockOwner.getTownBlocks()) {
				if ((townBlockOwner instanceof Town) && (!townBlock.hasResident())) {
					if (!townBlock.isChanged()) {
						townBlock.setType(townBlock.getType());
						TownyUniverse.getDataSource().saveTownBlock(townBlock);
					}
				}
			}

			TownyMessaging.sendMsg(player, TownySettings.getLangString("msg_set_perms"));
			TownyMessaging.sendMessage(player, (Colors.Green + " Perm: " + ((townBlockOwner instanceof Resident) ? perm.getColourString().replace("f", "r") : perm.getColourString())));
			TownyMessaging.sendMessage(player, Colors.Green + "PvP: " + ((perm.pvp) ? Colors.Red + "ON" : Colors.LightGreen + "OFF") + Colors.Green + "  Explosions: " + ((perm.explosion) ? Colors.Red + "ON" : Colors.LightGreen + "OFF") + Colors.Green + "  Firespread: " + ((perm.fire) ? Colors.Red + "ON" : Colors.LightGreen + "OFF") + Colors.Green + "  Mob Spawns: " + ((perm.mobs) ? Colors.Red + "ON" : Colors.LightGreen + "OFF"));

			// Reset all caches as this can affect everyone.
			plugin.resetCache();
		}
	}

	public static void parseTownClaimCommand(Player player, String[] split) {

		if (split.length == 1 && split[0].equalsIgnoreCase("?")) {
			player.sendMessage(ChatTools.formatTitle("/town claim"));
			player.sendMessage(ChatTools.formatCommand(TownySettings.getLangString("mayor_sing"), "/town claim", "", TownySettings.getLangString("msg_block_claim")));
			player.sendMessage(ChatTools.formatCommand(TownySettings.getLangString("mayor_sing"), "/town claim", "outpost", TownySettings.getLangString("mayor_help_3")));
			player.sendMessage(ChatTools.formatCommand(TownySettings.getLangString("mayor_sing"), "/town claim", "[circle/rect] [radius]", TownySettings.getLangString("mayor_help_4")));
			player.sendMessage(ChatTools.formatCommand(TownySettings.getLangString("mayor_sing"), "/town claim", "[circle/rect] auto", TownySettings.getLangString("mayor_help_5")));
		} else {
			Resident resident;
			Town town;
			TownyWorld world;
			try {
				if (TownyUniverse.isWarTime())
					throw new TownyException(TownySettings.getLangString("msg_war_cannot_do"));

				resident = TownyUniverse.getDataSource().getResident(player.getName());
				town = resident.getTown();
				world = TownyUniverse.getDataSource().getWorld(player.getWorld().getName());

				if (!world.isUsingTowny())
					throw new TownyException(TownySettings.getLangString("msg_set_use_towny_off"));

				double blockCost = 0;
				List<WorldCoord> selection;
				boolean attachedToEdge = true, outpost = false;
				Coord key = Coord.parseCoord(plugin.getCache(player).getLastLocation());

				if (split.length == 1 && split[0].equalsIgnoreCase("outpost")) {

					if (!TownyUniverse.getPermissionSource().testPermission(player, PermissionNodes.TOWNY_COMMAND_TOWN_CLAIM_OUPTPOST.getNode()))
						throw new TownyException(TownySettings.getLangString("msg_err_command_disable"));

					int maxOutposts = TownySettings.getMaxResidentOutposts(resident);
					if (!TownyUniverse.getPermissionSource().isTownyAdmin(player) && maxOutposts != -1 && (maxOutposts <= resident.getTown().getAllOutpostSpawns().size()))
						throw new TownyException(String.format(TownySettings.getLangString("msg_max_outposts_own"), maxOutposts));

					if (TownySettings.isAllowingOutposts()) {

						if (world.hasTownBlock(key))
							throw new TownyException(String.format(TownySettings.getLangString("msg_already_claimed_1"), key));

						if (world.getMinDistanceFromOtherTowns(key) < TownySettings.getMinDistanceFromTownHomeblocks())
							throw new TownyException(TownySettings.getLangString("msg_too_close"));

						if ((world.getMinDistanceFromOtherTownsPlots(key) < TownySettings.getMinDistanceFromTownPlotblocks()))
							throw new TownyException(TownySettings.getLangString("msg_too_close"));

						selection = AreaSelectionUtil.selectWorldCoordArea(town, new WorldCoord(world.getName(), key), new String[0]);
						blockCost = TownySettings.getOutpostCost();
						attachedToEdge = false;
						outpost = true;
					} else
						throw new TownyException(TownySettings.getLangString("msg_outpost_disable"));
				} else {

					if (!TownyUniverse.getPermissionSource().testPermission(player, PermissionNodes.TOWNY_COMMAND_TOWN_CLAIM_TOWN.getNode()))
						throw new TownyException(TownySettings.getLangString("msg_err_command_disable"));

					selection = AreaSelectionUtil.selectWorldCoordArea(town, new WorldCoord(world.getName(), key), split);
					blockCost = TownySettings.getClaimPrice();
				}

				if ((world.getMinDistanceFromOtherTownsPlots(key, town) < TownySettings.getMinDistanceFromTownPlotblocks()))
					throw new TownyException(TownySettings.getLangString("msg_too_close"));

				TownyMessaging.sendDebugMsg("townClaim: Pre-Filter Selection " + Arrays.toString(selection.toArray(new WorldCoord[0])));
				selection = AreaSelectionUtil.filterTownOwnedBlocks(selection);
				TownyMessaging.sendDebugMsg("townClaim: Post-Filter Selection " + Arrays.toString(selection.toArray(new WorldCoord[0])));
				checkIfSelectionIsValid(town, selection, attachedToEdge, blockCost, false);

				try {
					double cost = blockCost * selection.size();
					if (TownySettings.isUsingEconomy() && !town.pay(cost, String.format("Town Claim (%d)", selection.size())))
						throw new TownyException(String.format(TownySettings.getLangString("msg_no_funds_claim"), selection.size(), TownyEconomyHandler.getFormattedBalance(cost)));
				} catch (EconomyException e1) {
					throw new TownyException("Economy Error");
				}

				new TownClaim(plugin, player, town, selection, outpost, true, false).start();

			} catch (TownyException x) {
				TownyMessaging.sendErrorMsg(player, x.getMessage());
				return;
			}
		}
	}

	public static void parseTownUnclaimCommand(Player player, String[] split) {

		if (split.length == 1 && split[0].equalsIgnoreCase("?")) {
			player.sendMessage(ChatTools.formatTitle("/town unclaim"));
			player.sendMessage(ChatTools.formatCommand(TownySettings.getLangString("mayor_sing"), "/town unclaim", "", TownySettings.getLangString("mayor_help_6")));
			player.sendMessage(ChatTools.formatCommand(TownySettings.getLangString("mayor_sing"), "/town unclaim", "[circle/rect] [radius]", TownySettings.getLangString("mayor_help_7")));
			player.sendMessage(ChatTools.formatCommand(TownySettings.getLangString("mayor_sing"), "/town unclaim", "all", TownySettings.getLangString("mayor_help_8")));
		} else {
			Resident resident;
			Town town;
			TownyWorld world;
			try {
				if (TownyUniverse.isWarTime())
					throw new TownyException(TownySettings.getLangString("msg_war_cannot_do"));

				resident = TownyUniverse.getDataSource().getResident(player.getName());
				town = resident.getTown();
				world = TownyUniverse.getDataSource().getWorld(player.getWorld().getName());

				List<WorldCoord> selection;
				if (split.length == 1 && split[0].equalsIgnoreCase("all")) {
					new TownClaim(plugin, player, town, null, false, false, false).start();
					// townUnclaimAll(town);
				} else {
					selection = AreaSelectionUtil.selectWorldCoordArea(town, new WorldCoord(world.getName(), Coord.parseCoord(plugin.getCache(player).getLastLocation())), split);
					selection = AreaSelectionUtil.filterOwnedBlocks(town, selection);

					// Set the area to unclaim
					new TownClaim(plugin, player, town, selection, false, false, false).start();

					TownyMessaging.sendMsg(player, String.format(TownySettings.getLangString("msg_abandoned_area"), Arrays.toString(selection.toArray(new WorldCoord[0]))));
				}

			} catch (TownyException x) {
				TownyMessaging.sendErrorMsg(player, x.getMessage());
				return;
			}
		}
	}

	public static boolean isEdgeBlock(TownBlockOwner owner, List<WorldCoord> worldCoords) {

		// TODO: Better algorithm that doesn't duplicates checks.

		for (WorldCoord worldCoord : worldCoords)
			if (isEdgeBlock(owner, worldCoord))
				return true;
		return false;
	}

	public static boolean isEdgeBlock(TownBlockOwner owner, WorldCoord worldCoord) {

		if (TownySettings.getDebug())
			System.out.print("[Towny] Debug: isEdgeBlock(" + worldCoord.toString() + ") = ");

		int[][] offset = { { -1, 0 }, { 1, 0 }, { 0, -1 }, { 0, 1 } };
		for (int i = 0; i < 4; i++)
			try {
				TownBlock edgeTownBlock = worldCoord.getTownyWorld().getTownBlock(new Coord(worldCoord.getX() + offset[i][0], worldCoord.getZ() + offset[i][1]));
				if (edgeTownBlock.isOwner(owner)) {
					if (TownySettings.getDebug())
						System.out.println("true");
					return true;
				}
			} catch (NotRegisteredException e) {
			}
		if (TownySettings.getDebug())
			System.out.println("false");
		return false;
	}

	public static void checkIfSelectionIsValid(TownBlockOwner owner, List<WorldCoord> selection, boolean attachedToEdge, double blockCost, boolean force) throws TownyException {

		if (force)
			return;
		Town town = (Town) owner;

		// System.out.print("isEdgeBlock: "+ isEdgeBlock(owner, selection));

		if (attachedToEdge && !isEdgeBlock(owner, selection) && !town.getTownBlocks().isEmpty()) {
			if (selection.size() == 0)
				throw new TownyException(TownySettings.getLangString("msg_already_claimed_2"));
			else
				throw new TownyException(TownySettings.getLangString("msg_err_not_attached_edge"));
		}

		if (owner instanceof Town) {
			// Town town = (Town)owner;
			int available = TownySettings.getMaxTownBlocks(town) - town.getTownBlocks().size();
			TownyMessaging.sendDebugMsg("Claim Check Available: " + available);
			TownyMessaging.sendDebugMsg("Claim Selection Size: " + selection.size());
			if (available - selection.size() < 0)
				throw new TownyException(TownySettings.getLangString("msg_err_not_enough_blocks"));
		}

		try {
			double cost = blockCost * selection.size();
			if (TownySettings.isUsingEconomy() && !owner.canPayFromHoldings(cost))
				throw new TownyException(String.format(TownySettings.getLangString("msg_err_cant_afford_blocks"), selection.size(), TownyEconomyHandler.getFormattedBalance(cost)));
		} catch (EconomyException e1) {
			throw new TownyException("Economy Error");
		}
	}

	private void townWithdraw(Player player, int amount) {

		Resident resident;
		Town town;
		try {
			if (!TownySettings.getTownBankAllowWithdrawls())
				throw new TownyException(TownySettings.getLangString("msg_err_withdraw_disabled"));

			if (amount < 0)
				throw new TownyException(TownySettings.getLangString("msg_err_negative_money"));

			resident = TownyUniverse.getDataSource().getResident(player.getName());
			town = resident.getTown();

			town.withdrawFromBank(resident, amount);
			TownyMessaging.sendTownMessage(town, String.format(TownySettings.getLangString("msg_xx_withdrew_xx"), resident.getName(), amount, "town"));
		} catch (TownyException x) {
			TownyMessaging.sendErrorMsg(player, x.getMessage());
		} catch (EconomyException x) {
			TownyMessaging.sendErrorMsg(player, x.getMessage());
		}
	}

	private void townDeposit(Player player, int amount) {

		Resident resident;
		Town town;
		try {
			resident = TownyUniverse.getDataSource().getResident(player.getName());
			town = resident.getTown();

			double bankcap = TownySettings.getTownBankCap();
			if (bankcap > 0) {
				if (amount + town.getHoldingBalance() > bankcap)
					throw new TownyException(String.format(TownySettings.getLangString("msg_err_deposit_capped"), bankcap));
			}

			if (amount < 0)
				throw new TownyException(TownySettings.getLangString("msg_err_negative_money"));

			if (!resident.payTo(amount, town, "Town Deposit"))
				throw new TownyException(TownySettings.getLangString("msg_insuf_funds"));

			TownyMessaging.sendTownMessage(town, String.format(TownySettings.getLangString("msg_xx_deposited_xx"), resident.getName(), amount, "town"));
		} catch (TownyException x) {
			TownyMessaging.sendErrorMsg(player, x.getMessage());
		} catch (EconomyException x) {
			TownyMessaging.sendErrorMsg(player, x.getMessage());
		}
	}
}
