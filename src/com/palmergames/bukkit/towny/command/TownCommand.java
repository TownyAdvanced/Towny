package com.palmergames.bukkit.towny.command;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.palmergames.bukkit.towny.object.*;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import com.earth2me.essentials.Essentials;
import com.earth2me.essentials.Teleport;
import com.earth2me.essentials.User;
import com.iConomy.iConomy;

import ca.xshade.bukkit.questioner.Questioner;
import com.palmergames.bukkit.towny.AlreadyRegisteredException;
import com.palmergames.bukkit.towny.IConomyException;
import com.palmergames.bukkit.towny.NotRegisteredException;
import com.palmergames.bukkit.towny.EmptyTownException;
import com.palmergames.bukkit.towny.Towny;
import com.palmergames.bukkit.towny.TownyException;
import com.palmergames.bukkit.towny.TownySettings;
import com.palmergames.bukkit.towny.questioner.JoinTownTask;
import com.palmergames.bukkit.towny.questioner.ResidentTownQuestionTask;
import com.palmergames.bukkit.util.ChatTools;
import com.palmergames.bukkit.util.Colors;
import ca.xshade.questionmanager.Option;
import ca.xshade.questionmanager.Question;
import com.palmergames.util.StringMgmt;

/**
 * Send a list of all town help commands to player
 * Command: /town
 */

public class TownCommand implements CommandExecutor  {
	
	private static Towny plugin;
	private static final List<String> output = new ArrayList<String>();
	
	static {
		output.add(ChatTools.formatTitle("/town"));
		output.add(ChatTools.formatCommand("", "/town", "", TownySettings.getLangString("town_help_1")));
		output.add(ChatTools.formatCommand("", "/town", "[town]", TownySettings.getLangString("town_help_3")));
		output.add(ChatTools.formatCommand("", "/town", "here", TownySettings.getLangString("town_help_4")));
		output.add(ChatTools.formatCommand("", "/town", "list", ""));
		output.add(ChatTools.formatCommand("", "/town", "leave", ""));
		output.add(ChatTools.formatCommand("", "/town", "spawn", TownySettings.getLangString("town_help_5")));
		if (!TownySettings.isTownCreationAdminOnly())
			output.add(ChatTools.formatCommand("", "/town", "new [town]", TownySettings.getLangString("town_help_6")));
		output.add(ChatTools.formatCommand(TownySettings.getLangString("admin_sing"), "/town", "new [town] " + TownySettings.getLangString("town_help_2"), TownySettings.getLangString("town_help_7")));
		output.add(ChatTools.formatCommand(TownySettings.getLangString("res_sing"), "/town", "deposit [$]", ""));
		output.add(ChatTools.formatCommand(TownySettings.getLangString("mayor_sing"), "/town", "mayor ?", TownySettings.getLangString("town_help_8")));
		output.add(ChatTools.formatCommand(TownySettings.getLangString("admin_sing"), "/town", "delete [town]", ""));
	}
	
	
	public TownCommand(Towny instance) {
		plugin = instance;
	}	

	@Override
	public boolean onCommand(CommandSender sender, Command cmd, String commandLabel, String[] args) {
		
		if (sender instanceof Player) {
			Player player = (Player)sender;
			System.out.println("[PLAYER_COMMAND] " + player.getName() + ": /" + commandLabel + " " + StringMgmt.join(args));
			parseTownCommand(player,args);
		} else
			// Console
			for (String line : output)
				sender.sendMessage(Colors.strip(line));
		return true;
	}
	
	private void parseTownCommand(Player player, String[] split) {
		
		if (split.length == 0)
			try {
				Resident resident = plugin.getTownyUniverse().getResident(player.getName());
				Town town = resident.getTown();
				plugin.getTownyUniverse().sendMessage(player, plugin.getTownyUniverse().getStatus(town));
			} catch (NotRegisteredException x) {
				plugin.sendErrorMsg(player, TownySettings.getLangString("msg_err_dont_belong_town"));
			}
		else if (split[0].equalsIgnoreCase("?") || split[0].equalsIgnoreCase("help"))
			for (String line : output)
				player.sendMessage(line);
		else if (split[0].equalsIgnoreCase("here"))
			showTownStatusHere(player);
		else if (split[0].equalsIgnoreCase("list"))
			listTowns(player);
		else if (split[0].equalsIgnoreCase("new")) {
			if (split.length == 1)
				plugin.sendErrorMsg(player, TownySettings.getLangString("msg_specify_name"));
			else if (split.length == 2)
				newTown(player, split[1], player.getName());
			else
				// TODO: Check if player is an admin
				newTown(player, split[1], split[2]);
		} else if (split[0].equalsIgnoreCase("leave"))
			townLeave(player);
		else if (split[0].equalsIgnoreCase("spawn"))
			try {
				
				boolean isTownyAdmin = plugin.isTownyAdmin(player);
				Town town;
				String notAffordMSG;
				
				// Check permission to use spawn travel
				if (!isTownyAdmin && (
						(split.length == 1 && (!TownySettings.isAllowingTownSpawn() || (plugin.isPermissions() && !plugin.hasPermission(player, "towny.spawntp")))) ||
						(split.length > 1 && (!TownySettings.isAllowingPublicTownSpawnTravel() || (plugin.isPermissions() && !plugin.hasPermission(player, "towny.publicspawntp"))))))
					throw new TownyException(TownySettings.getLangString("msg_err_town_spawn_forbidden"));
				
				Resident resident = plugin.getTownyUniverse().getResident(player.getName());
				
				// fetch the spawn location for the teleport
				// and setup the error message if they can't afford.
				if (split.length > 1) {
					town = plugin.getTownyUniverse().getTown(split[1]);
					if (!isTownyAdmin && !town.isPublic())
						throw new TownyException(TownySettings.getLangString("msg_err_not_public"));
					notAffordMSG = String.format(TownySettings.getLangString("msg_err_cant_afford_tp_town"),town.getName());
				} else {
					town = resident.getTown();
					notAffordMSG = TownySettings.getLangString("msg_err_cant_afford_tp");
				}

                // Prevent enemies from using spawn travel.
				if (resident.hasTown() && resident.hasNation())
                    if (town.hasNation())
                        if (town.getNation().hasEnemy(resident.getTown().getNation()))
                            throw new TownyException(TownySettings.getLangString("msg_err_public_spawn_enemy"));

                if (!isTownyAdmin) {
                    // Prevent spawn travel while in disallowed zones (if configured)
                    List<String> disallowedZones = TownySettings.getDisallowedTownSpawnZones();
                    
                    if (!disallowedZones.isEmpty()) {
                        String inTown = plugin.getTownyUniverse().getTownName(plugin.getCache(player).getLastLocation());
                        
                        if (inTown == null && disallowedZones.contains("unclaimed"))
                            throw new TownyException(String.format(TownySettings.getLangString("msg_err_town_spawn_disallowed_from"), "the Wilderness"));
                        if (inTown != null && resident.hasNation() && plugin.getTownyUniverse().getTown(inTown).hasNation()) {
                            Nation inNation = plugin.getTownyUniverse().getTown(inTown).getNation();
                            Nation playerNation = resident.getTown().getNation();
                            if (inNation.getEnemies().contains(playerNation) && disallowedZones.contains("enemy"))
                            	throw new TownyException(String.format(TownySettings.getLangString("msg_err_town_spawn_disallowed_from"), "Enemy areas"));
                            if (!inNation.getAllies().contains(playerNation) && !inNation.getEnemies().contains(playerNation) && disallowedZones.contains("neutral"))
                                    throw new TownyException(String.format(TownySettings.getLangString("msg_err_town_spawn_disallowed_from"), "Neutral towns"));

                        }
                    }
                }
				
				double travelCost;
				if (resident.getTown() == town)
					travelCost = TownySettings.getTownSpawnTravelPrice();
				else
					travelCost = TownySettings.getTownPublicSpawnTravelPrice();
				
				// Check if need/can pay
				if (!isTownyAdmin && TownySettings.isUsingIConomy() && (resident.getHoldingBalance() < travelCost))
					throw new TownyException(notAffordMSG);
				
				//essentials tests
				boolean notUsingESS = false;
				
				if (TownySettings.isUsingEssentials()) {
					Plugin handle = plugin.getServer().getPluginManager().getPlugin("Essentials");
					if (!handle.equals(null)) {
						
						Essentials essentials = (Essentials)handle;
						plugin.sendDebugMsg("Using Essentials");
						
						try {
							User user = essentials.getUser(player);
							
							if (!user.isTeleportEnabled())
								//Ess teleport is disabled
								notUsingESS = true;
							
							if (!user.isJailed()){
								Teleport teleport = user.getTeleport();
								teleport.teleport(town.getSpawn(),null);
							}
						} catch (Exception e) {
							plugin.sendErrorMsg(player, "Error: " + e.getMessage());
							// cooldown?
							return;
						}
					}
				}
				//show message if we are using iConomy and are charging for spawn travel.
				if (!isTownyAdmin && TownySettings.isUsingIConomy() && resident.pay(travelCost, town))
					plugin.sendMsg(player, String.format(TownySettings.getLangString("msg_cost_spawn"),
							travelCost + TownyIConomyObject.getIConomyCurrency()));
				
				
				// if an Admin or essentials teleport isn't being used, use our own.
				if(isTownyAdmin || !notUsingESS)
						player.teleport(town.getSpawn());

				
			} catch (TownyException e) {
				plugin.sendErrorMsg(player, e.getMessage());
			} catch (IConomyException e) {
				plugin.sendErrorMsg(player, e.getMessage());
			}
		else if (split[0].equalsIgnoreCase("withdraw")) {
			if (split.length == 2)
				try {
					townWithdraw(player, Integer.parseInt(split[1]));
				} catch (NumberFormatException e) {
					plugin.sendErrorMsg(player, TownySettings.getLangString("msg_error_must_be_int"));
				}
			else
				plugin.sendErrorMsg(player, String.format(TownySettings.getLangString("msg_must_specify_amnt"), "/town"));
		} else if (split[0].equalsIgnoreCase("deposit")) {
			if (split.length == 2)
				try {
					townDeposit(player, Integer.parseInt(split[1]));
				} catch (NumberFormatException e) {
					plugin.sendErrorMsg(player, TownySettings.getLangString("msg_error_must_be_int"));
				}
			else
				plugin.sendErrorMsg(player, String.format(TownySettings.getLangString("msg_must_specify_amnt"), "/town"));
		} else {
			String[] newSplit = StringMgmt.remFirstArg(split);
			
			if (split[0].equalsIgnoreCase("set"))
				townSet(player, newSplit);
			else if (split[0].equalsIgnoreCase("toggle"))
				townToggle(player, newSplit);
			else  if (split[0].equalsIgnoreCase("mayor"))
				townMayor(player, newSplit);
			else if (split[0].equalsIgnoreCase("assistant"))
				townAssistant(player, newSplit);
			else if (split[0].equalsIgnoreCase("delete"))
				townDelete(player, newSplit);
			else if (split[0].equalsIgnoreCase("add"))
				townAdd(player, null, newSplit, true);
			else if (split[0].equalsIgnoreCase("kick"))
				townKick(player, newSplit, true);
			else if (split[0].equalsIgnoreCase("add+"))
				townAdd(player, null, newSplit, false);
			else if (split[0].equalsIgnoreCase("kick+"))
				townKick(player, newSplit, false);
			else if (split[0].equalsIgnoreCase("claim"))
				parseTownClaimCommand(player, newSplit);
			else if (split[0].equalsIgnoreCase("unclaim"))
				parseTownUnclaimCommand(player, newSplit);
			else
				try {
					Town town = plugin.getTownyUniverse().getTown(split[0]);
					plugin.getTownyUniverse().sendMessage(player, plugin.getTownyUniverse().getStatus(town));
				} catch (NotRegisteredException x) {
					plugin.sendErrorMsg(player, String.format(TownySettings.getLangString("msg_err_not_registered_1"), split[0]));
				}
		}
	}
	
	/**
	 * Send a list of all towns in the universe to player Command: /town list
	 * 
	 * @param player
	 */

	public void listTowns(Player player) {
		player.sendMessage(ChatTools.formatTitle(TownySettings.getLangString("town_plu")));
		ArrayList<String> formatedList = new ArrayList<String>();
		for (Town town : plugin.getTownyUniverse().getTowns())
			formatedList.add(Colors.LightBlue + town.getName() + Colors.Blue + " [" + town.getNumResidents() + "]" + Colors.White);
		for (String line : ChatTools.list(formatedList))
			player.sendMessage(line);
	}
	
	public void townMayor(Player player, String[] split) {
		if (split.length == 0 || split[0].equalsIgnoreCase("?"))
			showTownMayorHelp(player);
	}
	
	public void townAssistant(Player player, String[] split) {
		if (split.length == 0) {
			//TODO: assistant help
		} else if (split[0].equalsIgnoreCase("add")) {
			String[] newSplit = StringMgmt.remFirstArg(split);
			townAssistantsAdd(player, newSplit, true);
		} else if (split[0].equalsIgnoreCase("remove")) {
			String[] newSplit = StringMgmt.remFirstArg(split);
			townAssistantsRemove(player, newSplit, true);
		} else if (split[0].equalsIgnoreCase("add+")) {
			String[] newSplit = StringMgmt.remFirstArg(split);
			townAssistantsAdd(player, newSplit, false);
		} else if (split[0].equalsIgnoreCase("remove+")) {
			String[] newSplit = StringMgmt.remFirstArg(split);
			townAssistantsRemove(player, newSplit, false);
		}
	}
	
	/**
	 * Send a the status of the town the player is physically at to him
	 * 
	 * @param player
	 */
	public void showTownStatusHere(Player player) {
		try {
			TownyWorld world = plugin.getTownyUniverse().getWorld(player.getWorld().getName());
			Coord coord = Coord.parseCoord(player);
			showTownStatusAtCoord(player, world, coord);
		} catch (TownyException e) {
			plugin.sendErrorMsg(player, e.getError());
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
		plugin.getTownyUniverse().sendMessage(player, plugin.getTownyUniverse().getStatus(town));
	}
	
	public void showTownMayorHelp(Player player) {
		player.sendMessage(ChatTools.formatTitle("Town Mayor Help"));
		player.sendMessage(ChatTools.formatCommand(TownySettings.getLangString("mayor_sing"), "/town", "withdraw [$]", ""));
		player.sendMessage(ChatTools.formatCommand(TownySettings.getLangString("mayor_sing"), "/town", "claim", "'/town claim ?' " + TownySettings.getLangString("res_5")));
		player.sendMessage(ChatTools.formatCommand(TownySettings.getLangString("mayor_sing"), "/town", "unclaim", "'/town " + TownySettings.getLangString("res_5")));
		player.sendMessage(ChatTools.formatCommand(TownySettings.getLangString("mayor_sing"), "/town", "[add/kick] " + TownySettings.getLangString("res_2") + " .. []", TownySettings.getLangString("res_6")));
		player.sendMessage(ChatTools.formatCommand(TownySettings.getLangString("mayor_sing"), "/town", "[add+/kick+] " + TownySettings.getLangString("res_2"), TownySettings.getLangString("res_7")));
		player.sendMessage(ChatTools.formatCommand(TownySettings.getLangString("mayor_sing"), "/town", "set [] .. []", "'/town set' " + TownySettings.getLangString("res_5")));
		player.sendMessage(ChatTools.formatCommand(TownySettings.getLangString("mayor_sing"), "/town", "toggle", ""));
		player.sendMessage(ChatTools.formatCommand(TownySettings.getLangString("mayor_sing"), "/town", "assistant [add/remove] [player]", TownySettings.getLangString("res_6")));
		player.sendMessage(ChatTools.formatCommand(TownySettings.getLangString("mayor_sing"), "/town", "assistant [add+/remove+] [player]", TownySettings.getLangString("res_7")));
		// TODO: player.sendMessage(ChatTools.formatCommand("Mayor", "/town", "wall [type] [height]", ""));
		// TODO: player.sendMessage(ChatTools.formatCommand("Mayor", "/town", "wall remove", ""));
		player.sendMessage(ChatTools.formatCommand(TownySettings.getLangString("mayor_sing"), "/town", "delete", ""));
	}
	
	public void townToggle(Player player, String[] split) {
		if (split.length == 0) {
			player.sendMessage(ChatTools.formatTitle("/town toggle"));
			player.sendMessage(ChatTools.formatCommand("", "/town toggle", "pvp", ""));
			player.sendMessage(ChatTools.formatCommand("", "/town toggle", "public", ""));
			player.sendMessage(ChatTools.formatCommand("", "/town toggle", "explosion", ""));
			player.sendMessage(ChatTools.formatCommand("", "/town toggle", "fire", ""));
			player.sendMessage(ChatTools.formatCommand("", "/town toggle", "mobs", ""));
			player.sendMessage(ChatTools.formatCommand("", "/town toggle", "taxpercent", ""));
		} else {
			Resident resident;
			Town town;
			try {
				resident = plugin.getTownyUniverse().getResident(player.getName());
				town = resident.getTown();
				if (!resident.isMayor())
					if (!town.hasAssistant(resident))
						throw new TownyException(TownySettings.getLangString("msg_not_mayor_ass"));
			} catch (TownyException x) {
				plugin.sendErrorMsg(player, x.getError());
				return;
			}

			// TODO: Let admin's call a subfunction of this.
			if (split[0].equalsIgnoreCase("pvp")) {
				try {
					if (town.getWorld().isForcePVP()) {
						//town.setPVP(true);
						throw new TownyException(TownySettings.getLangString("msg_world_pvp"));
					}
					town.setPVP(!town.isPVP());
					plugin.getTownyUniverse().sendTownMessage(town, String.format(TownySettings.getLangString("msg_changed_pvp"), town.isPVP() ? "Enabled" : "Disabled"));
					
				} catch (Exception e) {
					plugin.sendErrorMsg(player, e.getMessage());
				}	
			} else if (split[0].equalsIgnoreCase("public")) {
				try {
					town.setPublic(!town.isPublic());
					plugin.getTownyUniverse().sendTownMessage(town, String.format(TownySettings.getLangString("msg_changed_public"), town.isPublic() ? "Enabled" : "Disabled"));
					
				} catch (Exception e) {
					plugin.sendErrorMsg(player, e.getMessage());
				}
			} else if (split[0].equalsIgnoreCase("explosion")) {
				try {
					if (town.getWorld().isForceExpl()) {
						//town.setBANG(true);
						throw new TownyException(TownySettings.getLangString("msg_world_expl"));
					}
					town.setBANG(!town.isBANG());
					plugin.getTownyUniverse().sendTownMessage(town, String.format(TownySettings.getLangString("msg_changed_expl"), town.isBANG() ? "Enabled" : "Disabled"));

				} catch (Exception e) {
					plugin.sendErrorMsg(player, e.getMessage());
				}
			} else if (split[0].equalsIgnoreCase("fire")) {
				try {
					if (town.getWorld().isForceFire()) {
						//town.setFire(true);
						throw new TownyException(TownySettings.getLangString("msg_world_fire"));
					}
					town.setFire(!town.isFire());
					plugin.getTownyUniverse().sendTownMessage(town, String.format(TownySettings.getLangString("msg_changed_fire"), town.isFire() ? "Enabled" : "Disabled"));

				} catch (Exception e) {
					plugin.sendErrorMsg(player, e.getMessage());
				}
			} else if (split[0].equalsIgnoreCase("mobs")) {
				try {
					if (town.getWorld().isForceTownMobs()) {
						//town.setHasMobs(true);
						throw new TownyException(TownySettings.getLangString("msg_world_mobs"));
					}
					town.setHasMobs(!town.hasMobs());
					plugin.getTownyUniverse().sendTownMessage(town, String.format(TownySettings.getLangString("msg_changed_mobs"), town.hasMobs() ? "Enabled" : "Disabled"));

				} catch (Exception e) {
					plugin.sendErrorMsg(player, e.getMessage());
				}
            }else if (split[0].equalsIgnoreCase("taxpercent")) {
				try {
					town.setTaxPercentage(!town.isTaxPercentage());
					plugin.getTownyUniverse().sendTownMessage(town, String.format(TownySettings.getLangString("msg_changed_taxpercent"), town.isTaxPercentage() ? "Enabled" : "Disabled"));

				} catch (Exception e) {
					plugin.sendErrorMsg(player, e.getMessage());
				}
			} else {
				plugin.sendErrorMsg(player, String.format(TownySettings.getLangString("msg_err_invalid_property"), "town"));
				return;
			} 

			plugin.getTownyUniverse().getDataSource().saveTown(town);
		}
	}
	
	
	public void townSet(Player player, String[] split) {
		if (split.length == 0) {
			player.sendMessage(ChatTools.formatTitle("/town set"));
			player.sendMessage(ChatTools.formatCommand("", "/town set", "board [message ... ]", ""));
			player.sendMessage(ChatTools.formatCommand("", "/town set", "mayor " + TownySettings.getLangString("town_help_2"), ""));
			player.sendMessage(ChatTools.formatCommand("", "/town set", "homeblock", ""));
			player.sendMessage(ChatTools.formatCommand("", "/town set", "spawn", ""));
			player.sendMessage(ChatTools.formatCommand("", "/town set", "perm ...", "'/town set perm' " + TownySettings.getLangString("res_5")));
			//player.sendMessage(ChatTools.formatCommand("", "/town set", "pvp [on/off]", ""));
			player.sendMessage(ChatTools.formatCommand("", "/town set", "taxes [$]", ""));
			player.sendMessage(ChatTools.formatCommand("", "/town set", "plottax [$]", ""));
			player.sendMessage(ChatTools.formatCommand("", "/town set", "plotprice [$]", ""));
			player.sendMessage(ChatTools.formatCommand("", "/town set", "name [name]", ""));
			//player.sendMessage(ChatTools.formatCommand("", "/town set", "public [on/off]", ""));
			//player.sendMessage(ChatTools.formatCommand("", "/town set", "explosion [on/off]", ""));
			//player.sendMessage(ChatTools.formatCommand("", "/town set", "fire [on/off]", ""));
		} else {
			Resident resident;
			Town town;
			try {
				resident = plugin.getTownyUniverse().getResident(player.getName());
				town = resident.getTown();
				if (!resident.isMayor())
					if (!town.hasAssistant(resident))
						throw new TownyException(TownySettings.getLangString("msg_not_mayor_ass"));
			} catch (TownyException x) {
				plugin.sendErrorMsg(player, x.getError());
				return;
			}

			// TODO: Let admin's call a subfunction of this.
			if (split[0].equalsIgnoreCase("board")) {
				if (split.length < 2) {
					plugin.sendErrorMsg(player, "Eg: /town set board " + TownySettings.getLangString("town_help_9"));
					return;
				} else {
					String line = split[1];
					for (int i = 2; i < split.length; i++)
						line += " " + split[i];
					town.setTownBoard(line);
					plugin.getTownyUniverse().sendTownBoard(player, town);
				}
			} else if (split[0].equalsIgnoreCase("mayor")) {
				if (split.length < 2) {
					plugin.sendErrorMsg(player, "Eg: /town set mayor Dumbo");
					return;
				} else
					try {
						if (!resident.isMayor())
							throw new TownyException(TownySettings.getLangString("msg_not_mayor"));
						
						String oldMayor = town.getMayor().getName();
						Resident newMayor = plugin.getTownyUniverse().getResident(split[1]);
						town.setMayor(newMayor);
						plugin.deleteCache(oldMayor);
						plugin.deleteCache(newMayor.getName());
						plugin.getTownyUniverse().sendTownMessage(town, TownySettings.getNewMayorMsg(newMayor.getName()));
					} catch (TownyException e) {
						plugin.sendErrorMsg(player, e.getError());
						return;
					}
			} else if (split[0].equalsIgnoreCase("taxes")) {
				if (split.length < 2) {
					plugin.sendErrorMsg(player, "Eg: /town set taxes 7");
					return;
				} else {
					Integer amount = Integer.parseInt(split[1]);
					if (amount < 0) {
						plugin.sendErrorMsg(player, TownySettings.getLangString("msg_err_negative_money"));
						return;
					}
                    if(town.isTaxPercentage() && amount > 100)
                    {
                        plugin.sendErrorMsg(player, TownySettings.getLangString("msg_err_not_percentage"));
                        return;
                    }
					try {
						town.setTaxes(Integer.parseInt(split[1]));
						plugin.getTownyUniverse().sendTownMessage(town, String.format(TownySettings.getLangString("msg_town_set_tax"), player.getName(), split[1]));
					} catch (NumberFormatException e) {
						plugin.sendErrorMsg(player, TownySettings.getLangString("msg_error_must_be_int"));
						return;
					}
				}
			} else if (split[0].equalsIgnoreCase("plottax")) {
				if (split.length < 2) {
					plugin.sendErrorMsg(player, "Eg: /town set plottax 10");
					return;
				} else {
					Integer amount = Integer.parseInt(split[1]);
					if (amount < 0) {
						plugin.sendErrorMsg(player, TownySettings.getLangString("msg_err_negative_money"));
						return;
					}
					try {
						town.setPlotTax(Integer.parseInt(split[1]));
						plugin.getTownyUniverse().sendTownMessage(town, String.format(TownySettings.getLangString("msg_town_set_plottax"), player.getName(), split[1]));
					} catch (NumberFormatException e) {
						plugin.sendErrorMsg(player, TownySettings.getLangString("msg_error_must_be_int"));
						return;
					}
				}
			} else if (split[0].equalsIgnoreCase("plotprice")) {
				if (split.length < 2) {
					plugin.sendErrorMsg(player, "Eg: /town set plotprice 50");
					return;
				} else {
					Integer amount = Integer.parseInt(split[1]);
					if (amount < 0) {
						plugin.sendErrorMsg(player, TownySettings.getLangString("msg_err_negative_money"));
						return;
					}
					try {
						town.setPlotPrice(amount);
						plugin.getTownyUniverse().sendTownMessage(town, String.format(TownySettings.getLangString("msg_town_set_plotprice"), player.getName(), split[1]));
					} catch (NumberFormatException e) {
						plugin.sendErrorMsg(player, TownySettings.getLangString("msg_error_must_be_int"));
						return;
					}
				}
			} else if (split[0].equalsIgnoreCase("name")) {
				if (split.length < 2) {
					plugin.sendErrorMsg(player, "Eg: /town set name BillyBobTown");
					return;
				} else
					//plugin.sendErrorMsg(player, TownySettings.getLangString("msg_town_rename_disabled"));
					if (TownySettings.isValidRegionName(split[1]))
						townRename(player, town, split[1]);
					else
						plugin.sendErrorMsg(player, TownySettings.getLangString("msg_invalid_name"));
			} else if (split[0].equalsIgnoreCase("homeblock")) {
				Coord coord = Coord.parseCoord(player);
				TownBlock townBlock;
				TownyWorld world;
				try {
					if (plugin.getTownyUniverse().isWarTime())
						throw new TownyException(TownySettings.getLangString("msg_war_cannot_do"));
					
					world = plugin.getTownyUniverse().getWorld(player.getWorld().getName());
					if (world.getMinDistanceFromOtherTowns(coord, resident.getTown()) < TownySettings.getMinDistanceFromTownHomeblocks())
						throw new TownyException(TownySettings.getLangString("msg_too_close"));
					
					if (TownySettings.getMaxDistanceBetweenHomeblocks() > 0)
						if (world.getMinDistanceFromOtherTowns(coord, resident.getTown()) > TownySettings.getMaxDistanceBetweenHomeblocks())
							throw new TownyException(TownySettings.getLangString("msg_too_far"));
					
					townBlock = plugin.getTownyUniverse().getWorld(player.getWorld().getName()).getTownBlock(coord);
					town.setHomeBlock(townBlock);
					plugin.sendMsg(player, String.format(TownySettings.getLangString("msg_set_town_home"), coord.toString()));
				} catch (TownyException e) {
					plugin.sendErrorMsg(player, e.getError());
					return;
				}
			} else if (split[0].equalsIgnoreCase("spawn"))
				try {
					town.setSpawn(player.getLocation());
					plugin.sendMsg(player, TownySettings.getLangString("msg_set_town_spawn"));
				} catch (TownyException e) {
					plugin.sendErrorMsg(player, e.getError());
					return;
				}
			else if (split[0].equalsIgnoreCase("perm")) {
				String[] newSplit = StringMgmt.remFirstArg(split);
				setTownBlockOwnerPermissions(player, town, newSplit);
				plugin.updateCache();
			} else {
				plugin.sendErrorMsg(player, String.format(TownySettings.getLangString("msg_err_invalid_property"), "town"));
				return;
			}

			plugin.getTownyUniverse().getDataSource().saveTown(town);
		}
	}
	
	/**
	 * Create a new town. Command: /town new [town] *[mayor]
	 * 
	 * @param player
	 */

	public void newTown(Player player, String name, String mayorName) {
		TownyUniverse universe = plugin.getTownyUniverse();
		try {
			if (universe.isWarTime())
				throw new TownyException(TownySettings.getLangString("msg_war_cannot_do"));
			
			if (!plugin.isTownyAdmin(player) && (TownySettings.isTownCreationAdminOnly() ||  (plugin.isPermissions() && !plugin.hasPermission(player, "towny.town.new"))))
				throw new TownyException(TownySettings.getNotPermToNewTownLine());
			
			if (TownySettings.hasTownLimit() && universe.getTowns().size() >= TownySettings.getTownLimit())
				throw new TownyException(TownySettings.getLangString("msg_err_universe_limit"));
			
			if (!TownySettings.isValidRegionName(name))
				throw new TownyException(String.format(TownySettings.getLangString("msg_err_invalid_name"), name));
			
			Resident resident = universe.getResident(mayorName);
			if (resident.hasTown())
				throw new TownyException(String.format(TownySettings.getLangString("msg_err_already_res"), resident.getName()));

			TownyWorld world = universe.getWorld(player.getWorld().getName());
			Coord key = Coord.parseCoord(player);
			if (world.hasTownBlock(key))
				throw new TownyException(String.format(TownySettings.getLangString("msg_already_claimed_1"), key));
			
			if (world.getMinDistanceFromOtherTowns(key) < TownySettings.getMinDistanceFromTownHomeblocks())
				throw new TownyException(TownySettings.getLangString("msg_too_close"));
			
			if (TownySettings.getMaxDistanceBetweenHomeblocks() > 0)
				if (world.getMinDistanceFromOtherTowns(key) > TownySettings.getMaxDistanceBetweenHomeblocks())
					throw new TownyException(TownySettings.getLangString("msg_too_far"));

			if (TownySettings.isUsingIConomy() && !resident.pay(TownySettings.getNewTownPrice()))
				throw new TownyException(String.format(TownySettings.getLangString("msg_no_funds_new_town"), (resident.getName().equals(player.getName()) ? "You" : resident.getName())));

			newTown(universe, world, name, resident, key, player.getLocation());			
			universe.sendGlobalMessage(TownySettings.getNewTownMsg(player.getName(), name));
		} catch (TownyException x) {
			plugin.sendErrorMsg(player, x.getError());
			// TODO: delete town data that might have been done
		} catch (IConomyException x) {
			plugin.sendErrorMsg(player, x.getError());
		}
	}
	
	public Town newTown(TownyUniverse universe, TownyWorld world, String name, Resident resident, Coord key, Location spawn) throws TownyException {
		world.newTownBlock(key);
		universe.newTown(name);
		Town town = universe.getTown(name);
		town.addResident(resident);
		town.setMayor(resident);
		TownBlock townBlock = world.getTownBlock(key);
		townBlock.setTown(town);
		town.setHomeBlock(townBlock);
		town.setSpawn(spawn);
		world.addTown(town);
		plugin.sendDebugMsg("Creating new Town account: " + "town-"+name);
		if(TownySettings.isUsingIConomy())
		{
			iConomy.getAccount("town-"+name);
			iConomy.getAccount("town-"+name).getHoldings().set(0);
		}
		
		universe.getDataSource().saveResident(resident);
		universe.getDataSource().saveTown(town);
		universe.getDataSource().saveWorld(world);
		universe.getDataSource().saveTownList();
		
		plugin.updateCache();
		return town;
	}
	
	public void townRename(Player player, Town town, String newName) {
		try {
			plugin.getTownyUniverse().renameTown(town, newName);
			plugin.getTownyUniverse().sendTownMessage(town, String.format(TownySettings.getLangString("msg_town_set_name"), player.getName(), town.getName()));
		} catch (TownyException e) {
			plugin.sendErrorMsg(player, e.getError());
		}
	}
	
	public void townLeave(Player player) {
		Resident resident;
		Town town;
		try {
			//TODO: Allow leaving town during war.
			if (plugin.getTownyUniverse().isWarTime()) 
				throw new TownyException(TownySettings.getLangString("msg_war_cannot_do"));
			
			resident = plugin.getTownyUniverse().getResident(player.getName());
			town = resident.getTown();
			plugin.deleteCache(resident.getName());
			
			
		} catch (TownyException x) {
			plugin.sendErrorMsg(player, x.getError());
			return;
		}
		
		if (resident.isMayor()) {
			plugin.sendErrorMsg(player, TownySettings.getMayorAbondonMsg());
			return;
		}
		
		try {
			town.removeResident(resident);
		} catch (EmptyTownException et) {
			plugin.getTownyUniverse().removeTown(et.getTown());

		} catch (NotRegisteredException x) {
			plugin.sendErrorMsg(player, x.getError());
			return;
		}
		
		plugin.getTownyUniverse().getDataSource().saveResident(resident);
		plugin.getTownyUniverse().getDataSource().saveTown(town);
		
		plugin.updateCache();
		
		plugin.getTownyUniverse().sendTownMessage(town, String.format(TownySettings.getLangString("msg_left_town"), resident.getName()));
		plugin.sendMsg(player, String.format(TownySettings.getLangString("msg_left_town"), resident.getName()));
	}
	
	public void townDelete(Player player, String[] split) {
		if (split.length == 0)
			try {
				Resident resident = plugin.getTownyUniverse().getResident(player.getName());
				Town town = resident.getTown();
				if (!resident.isMayor())
					throw new TownyException(TownySettings.getLangString("msg_not_mayor"));
				plugin.getTownyUniverse().removeTown(town);
				plugin.getTownyUniverse().sendGlobalMessage(TownySettings.getDelTownMsg(town));
			} catch (TownyException x) {
				plugin.sendErrorMsg(player, x.getError());
				return;
			}
		else
			try {
				if (!plugin.isTownyAdmin(player))
					throw new TownyException(TownySettings.getLangString("msg_err_admin_only_delete_town"));
				Town town = plugin.getTownyUniverse().getTown(split[0]);
				plugin.getTownyUniverse().removeTown(town);
				plugin.getTownyUniverse().sendGlobalMessage(TownySettings.getDelTownMsg(town));
			} catch (TownyException x) {
				plugin.sendErrorMsg(player, x.getError());
				return;
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

	public void townKick(Player player, String[] names, boolean matchOnline) {
		Resident resident;
		Town town;
		try {
			resident = plugin.getTownyUniverse().getResident(player.getName());
			town = resident.getTown();
			if (!resident.isMayor())
				if (!town.hasAssistant(resident))
					throw new TownyException(TownySettings.getLangString("msg_not_mayor_ass"));
		} catch (TownyException x) {
			plugin.sendErrorMsg(player, x.getError());
			return;
		}

		townKickResidents(player, resident, town, (matchOnline ? plugin.getTownyUniverse().getOnlineResidents(player, names) : getResidents(player, names)));
		
		plugin.updateCache();
	}
	
	private static List<Resident> getResidents(Player player, String[] names) {
		List<Resident> invited = new ArrayList<Resident>();
		for (String name : names)
			try {
				Resident target = plugin.getTownyUniverse().getResident(name);
				invited.add(target);
			} catch (TownyException x) {
				plugin.sendErrorMsg(player, x.getError());
			}
		return invited;
	}
	
	public static void townAddResidents(Player player, Town town, List<Resident> invited, boolean online) {
		ArrayList<Resident> remove = new ArrayList<Resident>();
		for (Resident newMember : invited)
			try {
				// only add players with the right permissions.
				if (plugin.isPermissions()) {
						if (!online) {
							plugin.sendErrorMsg(player, String.format(TownySettings.getLangString("msg_offline_no_join"), newMember.getName()));
							remove.add(newMember);
						} else if (!plugin.hasPermission(plugin.getServer().getPlayer(newMember.getName()), "towny.town.resident")) {
							plugin.sendErrorMsg(player, String.format(TownySettings.getLangString("msg_not_allowed_join"), newMember.getName()));
							remove.add(newMember);
						} else {
							town.addResidentCheck(newMember);
							townInviteResident(town, newMember);
						}
				} else {
					town.addResidentCheck(newMember);
					townInviteResident(town, newMember);
				}
			} catch (AlreadyRegisteredException e) {
				remove.add(newMember);
				plugin.sendErrorMsg(player, e.getError());
			}
		for (Resident newMember : remove)
			invited.remove(newMember);

		if (invited.size() > 0) {
			String msg = "";
			for (Resident newMember : invited)
				msg += newMember.getName() + ", ";
			
			msg = msg.substring(0, msg.length()-2);

			msg = String.format(TownySettings.getLangString("msg_invited_join_town"), player.getName(), msg);
			plugin.getTownyUniverse().sendTownMessage(town, ChatTools.color(msg));
			plugin.getTownyUniverse().getDataSource().saveTown(town);
		} else
			plugin.sendErrorMsg(player, TownySettings.getLangString("msg_invalid_name"));
	}
	
	public static void townAddResident(Town town, Resident resident) throws AlreadyRegisteredException {
		town.addResident(resident);
		plugin.deleteCache(resident.getName());
		plugin.getTownyUniverse().getDataSource().saveResident(resident);
		plugin.getTownyUniverse().getDataSource().saveTown(town);
	}

	private static void townInviteResident(Town town, Resident newMember) throws AlreadyRegisteredException {
		Plugin test = plugin.getServer().getPluginManager().getPlugin("Questioner");
		
		if (TownySettings.isUsingQuestioner() && test != null && test instanceof Questioner && test.isEnabled()) {
			Questioner questioner = (Questioner)test;
			questioner.loadClasses();
			
			List<Option> options = new ArrayList<Option>();
			options.add(new Option("accept", new JoinTownTask(newMember, town)));
			options.add(new Option("deny", new ResidentTownQuestionTask(newMember, town) {
				@Override
				public void run() {
					getUniverse().sendTownMessage(getTown(), String.format(TownySettings.getLangString("msg_deny_invite"), getResident().getName()));
				}
			}));
			Question question = new Question(newMember.getName(), String.format(TownySettings.getLangString("msg_invited"), town.getName()), options);
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
	
	public void townKickResidents(Player player, Resident resident, Town town, List<Resident> kicking) {
		ArrayList<Resident> remove = new ArrayList<Resident>();
		for (Resident member : kicking)
			if (resident == member || member.isMayor() || town.hasAssistant(member))
				remove.add(member);
			else
				try {
					town.removeResident(member);
					plugin.deleteCache(member.getName());
					plugin.getTownyUniverse().getDataSource().saveResident(member);
				} catch (NotRegisteredException e) {
					remove.add(member);
				} catch (EmptyTownException e) {
					// You can't kick yourself and only the mayor can kick
					// assistants
					// so there will always be at least one resident.
				}
		
		for (Resident member : remove)
			kicking.remove(member);

		if (kicking.size() > 0) {
			String msg = "";
			for (Resident member : kicking) {
				msg += member.getName() + ", ";
				Player p = plugin.getServer().getPlayer(member.getName());
				if (p != null)
					p.sendMessage(String.format(TownySettings.getLangString("msg_kicked_by"), player.getName()));
			}
			msg = msg.substring(0, msg.length()-2);
			msg = String.format(TownySettings.getLangString("msg_kicked"), player.getName(), msg);
			plugin.getTownyUniverse().sendTownMessage(town, ChatTools.color(msg));
			plugin.getTownyUniverse().getDataSource().saveTown(town);
		} else
			plugin.sendErrorMsg(player, TownySettings.getLangString("msg_invalid_name"));
	}
	
	/**
	 * Confirm player is a mayor or assistant, then get list of filter names
	 * with online players and invite them to town. Command: /town add
	 * [resident] .. [resident]
	 * 
	 * @param player
	 * @param names
	 */

	public void townAssistantsAdd(Player player, String[] names, boolean matchOnline) {
		Resident resident;
		Town town;
		try {
			resident = plugin.getTownyUniverse().getResident(player.getName());
			town = resident.getTown();
			if (!resident.isMayor())
				throw new TownyException(TownySettings.getLangString("msg_not_mayor"));
		} catch (TownyException x) {
			plugin.sendErrorMsg(player, x.getError());
			return;
		}

		townAssistantsAdd(player, town, (matchOnline ? plugin.getTownyUniverse().getOnlineResidents(player, names) : getResidents(player, names)));
	}

	public void townAssistantsAdd(Player player, Town town, List<Resident> invited) {
		//TODO: change variable names from townAdd copypasta
		ArrayList<Resident> remove = new ArrayList<Resident>();
		for (Resident newMember : invited)
			try {
				town.addAssistant(newMember);
				plugin.deleteCache(newMember.getName());
				plugin.getTownyUniverse().getDataSource().saveResident(newMember);
			} catch (AlreadyRegisteredException e) {
				remove.add(newMember);
			}
		for (Resident newMember : remove)
			invited.remove(newMember);

		if (invited.size() > 0) {
			String msg = "";

			for (Resident newMember : invited)
				msg += newMember.getName() + ", ";
			msg = String.format(TownySettings.getLangString("msg_raised_ass"), player.getName(), msg, "town");
			plugin.getTownyUniverse().sendTownMessage(town, ChatTools.color(msg));
			plugin.getTownyUniverse().getDataSource().saveTown(town);
		} else
			plugin.sendErrorMsg(player, TownySettings.getLangString("msg_invalid_name"));
	}

	/**
	 * Confirm player is a mayor or assistant, then get list of filter names
	 * with online players and kick them from town. Command: /town kick
	 * [resident] .. [resident]
	 * 
	 * @param player
	 * @param names
	 */

	public void townAssistantsRemove(Player player, String[] names, boolean matchOnline) {
		Resident resident;
		Town town;
		try {
			resident = plugin.getTownyUniverse().getResident(player.getName());
			town = resident.getTown();
			if (!resident.isMayor())
				throw new TownyException(TownySettings.getLangString("msg_not_mayor"));
		} catch (TownyException x) {
			plugin.sendErrorMsg(player, x.getError());
			return;
		}

		townAssistantsRemove(player, resident, town, (matchOnline ? plugin.getTownyUniverse().getOnlineResidents(player, names) : getResidents(player, names)));
	}

	public void townAssistantsRemove(Player player, Resident resident, Town town, List<Resident> kicking) {
		ArrayList<Resident> remove = new ArrayList<Resident>();
		List<Resident> toKick = new ArrayList<Resident>(kicking);
		
		for (Resident member : toKick)
			try {
				town.removeAssistant(member);
				plugin.deleteCache(member.getName());
				plugin.getTownyUniverse().getDataSource().saveResident(member);
				plugin.getTownyUniverse().getDataSource().saveTown(town);
			} catch (NotRegisteredException e) {
				remove.add(member);
			}
		
		// remove invalid names so we don't try to send them messages			
				if (remove.size() > 0)
					for (Resident member : remove)
						toKick.remove(member);
							
		if (toKick.size() > 0) {
			String msg = "";
			Player p;
			
			for (Resident member : toKick) {
				msg += member.getName() + ", ";
				p = plugin.getServer().getPlayer(member.getName());
				if (p != null)
					p.sendMessage(String.format(TownySettings.getLangString("msg_lowered_to_res_by"), player.getName()));
			}
			msg = msg.substring(0, msg.length()-2);
			msg = String.format(TownySettings.getLangString("msg_lowered_to_res"), player.getName(), msg);
			plugin.getTownyUniverse().sendTownMessage(town, ChatTools.color(msg));
			plugin.getTownyUniverse().getDataSource().saveTown(town);
		} else
			plugin.sendErrorMsg(player, TownySettings.getLangString("msg_invalid_name"));
	}
	
	/**
	 * Confirm player is a mayor or assistant, then get list of filter names
	 * with online players and invite them to town. Command: /town add
	 * [resident] .. [resident]
	 * 
	 * @param player
	 * @param specifiedTown to add to if not null
	 * @param names
	 */

	public static void townAdd(Player player, Town specifiedTown, String[] names, boolean matchOnline) {
		Resident resident;
		Town town;
		try {
			resident = plugin.getTownyUniverse().getResident(player.getName());
			if (specifiedTown == null)
				town = resident.getTown();
			else
				town = specifiedTown;
			if (!plugin.isTownyAdmin(player) && !resident.isMayor() && !town.hasAssistant(resident))
				throw new TownyException(TownySettings.getLangString("msg_not_mayor_ass"));
			
		} catch (TownyException x) {
			plugin.sendErrorMsg(player, x.getError());
			return;
		}

		townAddResidents(player, town, (matchOnline ? plugin.getTownyUniverse().getOnlineResidents(player, names) : getResidents(player, names)), matchOnline);
		
		plugin.updateCache();
	}
	
	// wrapper function for non friend setting of perms
	public static void setTownBlockOwnerPermissions(Player player, TownBlockOwner townBlockOwner, String[] split) {
		
		setTownBlockOwnerPermissions(player, townBlockOwner, split, false);
		
	}
	public static void setTownBlockOwnerPermissions(Player player, TownBlockOwner townBlockOwner, String[] split, boolean friend) {
		
		// TODO: switches
		if (split.length == 0 || split[0].equalsIgnoreCase("?")) {
			player.sendMessage(ChatTools.formatTitle("/... set perm"));
			player.sendMessage(ChatTools.formatCommand("Level", "[resident/ally/outsider]", "", ""));
			player.sendMessage(ChatTools.formatCommand("Type", "[build/destroy/switch/itemuse]", "", ""));
			player.sendMessage(ChatTools.formatCommand("", "set perm", "[on/off]", "Toggle all permissions"));
			player.sendMessage(ChatTools.formatCommand("", "set perm", "[level/type] [on/off]", ""));
			player.sendMessage(ChatTools.formatCommand("", "set perm", "[level] [type] [on/off]", ""));
			player.sendMessage(ChatTools.formatCommand("Eg", "/town set perm", "ally off", ""));
			player.sendMessage(ChatTools.formatCommand("Eg", "/resident set perm", "friend build on", ""));
			player.sendMessage(String.format(TownySettings.getLangString("plot_perms"), "'friend'", "'resident'"));
			player.sendMessage(TownySettings.getLangString("plot_perms_1"));
		} else {
			TownyPermission perm = townBlockOwner.getPermissions();
			
			// reset the friend to resident so the perm settings don't fail
			if (friend && split[0].equalsIgnoreCase("friend"))
				split[0] = "resident";
			
			if (split.length == 1)
				try {
					perm.setAll(plugin.parseOnOff(split[0]));
				} catch (Exception e) {
				}
			else if (split.length == 2)
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
			else if (split.length == 3)
				try {
					boolean b = plugin.parseOnOff(split[2]);
					String s = "";
					s = split[0] + split[1];
					perm.set(s, b);
				} catch (Exception e) {
				}
			String perms = townBlockOwner.getPermissions().toString();
			//change perm name to friend is this is a resident setting
			if (friend)
				perms = perms.replaceAll("resident", "friend");
			plugin.sendMsg(player, String.format(TownySettings.getLangString("msg_set_perms"), perms));
			plugin.updateCache();
		}
	}
	
	public static void parseTownClaimCommand(Player player, String[] split) {
		
		if (split.length == 1 && split[0].equalsIgnoreCase("?")) {
			player.sendMessage(ChatTools.formatTitle("/town claim"));
			player.sendMessage(ChatTools.formatCommand(TownySettings.getLangString("mayor_sing"), "/town claim", "", TownySettings.getLangString("msg_block_claim")));
			player.sendMessage(ChatTools.formatCommand(TownySettings.getLangString("mayor_sing"), "/town claim", "outpost", TownySettings.getLangString("mayor_help_3")));
			player.sendMessage(ChatTools.formatCommand(TownySettings.getLangString("mayor_sing"), "/town claim", "[radius]", TownySettings.getLangString("mayor_help_4")));
			player.sendMessage(ChatTools.formatCommand(TownySettings.getLangString("mayor_sing"), "/town claim", "auto", TownySettings.getLangString("mayor_help_5")));
		} else {
			Resident resident;
			Town town;
			TownyWorld world;
			try {
				if (plugin.getTownyUniverse().isWarTime())
					throw new TownyException(TownySettings.getLangString("msg_war_cannot_do"));
				
				if (!plugin.isTownyAdmin(player) && plugin.isPermissions() && !plugin.hasPermission(player, "towny.town.claim"))
					throw new TownyException(TownySettings.getLangString("msg_no_perms_claim"));
				
				resident = plugin.getTownyUniverse().getResident(player.getName());
				town = resident.getTown();
				if (!resident.isMayor() && !town.hasAssistant(resident))
					throw new TownyException(TownySettings.getLangString("msg_not_mayor_ass"));
				world = plugin.getTownyUniverse().getWorld(player.getWorld().getName());
				
				

				double blockCost = 0;
				List<WorldCoord> selection;
				boolean attachedToEdge = true;
				
				if (split.length == 1 && split[0].equalsIgnoreCase("outpost")) {
					if (TownySettings.isAllowingOutposts()) {
						selection = new ArrayList<WorldCoord>();
						selection.add(new WorldCoord(world, Coord.parseCoord(plugin.getCache(player).getLastLocation())));
						blockCost = TownySettings.getOutpostCost();
						attachedToEdge = false;
					} else
						throw new TownyException(TownySettings.getLangString("msg_outpost_disable"));
				} else {
					selection = selectWorldCoordArea(town, new WorldCoord(world, Coord.parseCoord(plugin.getCache(player).getLastLocation())), split);
					blockCost = TownySettings.getClaimPrice();
				}
				
				plugin.sendDebugMsg("townClaim: Pre-Filter Selection " + Arrays.toString(selection.toArray(new WorldCoord[0])));
				selection = removeTownOwnedBlocks(selection);
				plugin.sendDebugMsg("townClaim: Post-Filter Selection " + Arrays.toString(selection.toArray(new WorldCoord[0])));
				checkIfSelectionIsValid(town, selection, attachedToEdge, blockCost, false);
				
				try {
					double cost = blockCost * selection.size();
					if (TownySettings.isUsingIConomy() && !town.pay(cost))
						throw new TownyException(String.format(TownySettings.getLangString("msg_no_funds_claim"), selection.size(), cost) + TownyIConomyObject.getIConomyCurrency());
				} catch (IConomyException e1) {
					throw new TownyException("Iconomy Error");
				}
				
				for (WorldCoord worldCoord : selection)
					townClaim(town, worldCoord);
				
				plugin.getTownyUniverse().getDataSource().saveTown(town);
				plugin.getTownyUniverse().getDataSource().saveWorld(world);
				
				plugin.sendMsg(player, String.format(TownySettings.getLangString("msg_annexed_area"), Arrays.toString(selection.toArray(new WorldCoord[0]))));
				plugin.updateCache();
			} catch (TownyException x) {
				plugin.sendErrorMsg(player, x.getError());
				return;
			}
		}
	}
	
	public static void parseTownUnclaimCommand(Player player, String[] split) {
		
		if (split.length == 1 && split[0].equalsIgnoreCase("?")) {
			player.sendMessage(ChatTools.formatTitle("/town unclaim"));
			player.sendMessage(ChatTools.formatCommand(TownySettings.getLangString("mayor_sing"), "/town unclaim", "", TownySettings.getLangString("mayor_help_6")));
			player.sendMessage(ChatTools.formatCommand(TownySettings.getLangString("mayor_sing"), "/town unclaim", "[radius]", TownySettings.getLangString("mayor_help_7")));
			player.sendMessage(ChatTools.formatCommand(TownySettings.getLangString("mayor_sing"), "/town unclaim", "all", TownySettings.getLangString("mayor_help_8")));
		} else {
			Resident resident;
			Town town;
			TownyWorld world;
			try {
				if (plugin.getTownyUniverse().isWarTime())
					throw new TownyException(TownySettings.getLangString("msg_war_cannot_do"));
				
				resident = plugin.getTownyUniverse().getResident(player.getName());
				town = resident.getTown();
				if (!resident.isMayor())
					if (!town.hasAssistant(resident))
						throw new TownyException(TownySettings.getLangString("msg_not_mayor_ass"));
				world = plugin.getTownyUniverse().getWorld(player.getWorld().getName());
				
				List<WorldCoord> selection;
				if (split.length == 1 && split[0].equalsIgnoreCase("all"))
					townUnclaimAll(town);
				else {
					selection = selectWorldCoordArea(town, new WorldCoord(world, Coord.parseCoord(plugin.getCache(player).getLastLocation())), split);
					selection = filterOwnedBlocks(town, selection);
					
					for (WorldCoord worldCoord : selection)
						townUnclaim(town, worldCoord, false);
	
					plugin.sendMsg(player, String.format(TownySettings.getLangString("msg_abandoned_area"), Arrays.toString(selection.toArray(new WorldCoord[0]))));
				}
				plugin.getTownyUniverse().getDataSource().saveTown(town);
				plugin.getTownyUniverse().getDataSource().saveWorld(world);
				plugin.updateCache();
			} catch (TownyException x) {
				plugin.sendErrorMsg(player, x.getError());
				return;
			}
		}
	}
	
	public static List<WorldCoord> removeTownOwnedBlocks(List<WorldCoord> selection) {
		List<WorldCoord> out = new ArrayList<WorldCoord>();
		for (WorldCoord worldCoord : selection)
			try {
				if (!worldCoord.getTownBlock().hasTown())
					out.add(worldCoord);
			} catch (NotRegisteredException e) {
				out.add(worldCoord);
			}
		return out;
	}
	
	public static List<WorldCoord> filterOwnedBlocks(TownBlockOwner owner, List<WorldCoord> selection) {
		List<WorldCoord> out = new ArrayList<WorldCoord>();
		for (WorldCoord worldCoord : selection)
			try {
				if (worldCoord.getTownBlock().isOwner(owner))
					out.add(worldCoord);
			} catch (NotRegisteredException e) {
			}
		return out;
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
				TownBlock edgeTownBlock = worldCoord.getWorld().getTownBlock(new Coord(worldCoord.getX() + offset[i][0], worldCoord.getZ() + offset[i][1]));
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
	
	public static List<WorldCoord> selectWorldCoordArea(TownBlockOwner owner, WorldCoord pos, String[] args) throws TownyException {
		List<WorldCoord> out = new ArrayList<WorldCoord>();
		
		if (args.length == 0) {
			
			// claim with no sub command entered so attempt selection of one plot
			if (pos.getWorld().isClaimable())
				out.add(pos);
			else
				throw new TownyException(TownySettings.getLangString("msg_not_claimable"));
		} else {
			 int r;
				if (args[0].equalsIgnoreCase("auto")) {
					
					// Attempt to select outwards until no town blocks remain
					if (owner instanceof Town) {
						Town town = (Town)owner;
						int available = TownySettings.getMaxTownBlocks(town) - town.getTownBlocks().size();
						r = 0;
						while (available - Math.pow((r + 1) * 2 - 1, 2) >= 0)
							r += 1;
					} else
						throw new TownyException(TownySettings.getLangString("msg_err_rect_auto"));
				} else {
					
					// if a value was given attempt to select a radius of plots
					try {
						r = Integer.parseInt(args[0]);
					} catch (NumberFormatException e) {
						throw new TownyException(TownySettings.getLangString("msg_err_invalid_radius"));
					}
				}
				
				r -= 1;
				
				for (int z = pos.getZ() - r; z <= pos.getZ() + r; z++)
					for (int x = pos.getX() - r; x <= pos.getX() + r; x++)
						if (pos.getWorld().isClaimable())
							out.add(new WorldCoord(pos.getWorld(), x, z));	
			}

		return out;
	}
	
	public static void checkIfSelectionIsValid(TownBlockOwner owner, List<WorldCoord> selection, boolean attachedToEdge, double blockCost, boolean force) throws TownyException {
		if (force)
			return;
		Town town = (Town)owner;
		
		//System.out.print("isEdgeBlock: "+ isEdgeBlock(owner, selection));
		
		if (attachedToEdge && !isEdgeBlock(owner, selection) && !town.getTownBlocks().isEmpty()) {
			if (selection.size() == 0)
				throw new TownyException(TownySettings.getLangString("msg_already_claimed_2"));
			else
				throw new TownyException(TownySettings.getLangString("msg_err_not_attached_edge"));
		}
		
		if (owner instanceof Town) {
			//Town town = (Town)owner;
			int available = TownySettings.getMaxTownBlocks(town) - town.getTownBlocks().size();
			plugin.sendDebugMsg("Claim Check Available: " + available);
			if (available - selection.size() < 0)
				throw new TownyException(TownySettings.getLangString("msg_err_not_enough_blocks"));
		}
		
		try {
			double cost = blockCost * selection.size();
			if (TownySettings.isUsingIConomy() && !owner.canPayFromHoldings(cost))
				throw new TownyException(String.format(TownySettings.getLangString("msg_err_cant_afford_blocks"), selection.size(), cost + TownyIConomyObject.getIConomyCurrency()));
		} catch (IConomyException e1) {
			throw new TownyException("Iconomy Error");
		}
	}
	
	public static boolean townClaim(Town town, WorldCoord worldCoord) throws TownyException {		
		try {
			TownBlock townBlock = worldCoord.getTownBlock();
			try {
				throw new AlreadyRegisteredException(String.format(TownySettings.getLangString("msg_already_claimed"), townBlock.getTown().getName()));
			} catch (NotRegisteredException e) {
				throw new AlreadyRegisteredException(TownySettings.getLangString("msg_already_claimed_2"));
			}
		} catch (NotRegisteredException e) {
			TownBlock townBlock = worldCoord.getWorld().newTownBlock(worldCoord);
			townBlock.setTown(town);
			if (!town.hasHomeBlock())
				town.setHomeBlock(townBlock);
			return true;
		}
	}
	
	public static boolean townUnclaim(Town town, WorldCoord worldCoord, boolean force) throws TownyException {
		try {
			TownBlock townBlock = worldCoord.getTownBlock();
			if (town != townBlock.getTown() && !force)
				throw new TownyException(TownySettings.getLangString("msg_area_not_own"));
			
			plugin.getTownyUniverse().removeTownBlock(townBlock);
			
			return true;
		} catch (NotRegisteredException e) {
			throw new TownyException(TownySettings.getLangString("msg_not_claimed_1"));
		}
	}
	
	public static boolean townUnclaimAll(Town town) {
		plugin.getTownyUniverse().removeTownBlocks(town);
		plugin.getTownyUniverse().sendTownMessage(town, TownySettings.getLangString("msg_abandoned_area_1"));
		
		return true;
	}
	
	private void townWithdraw(Player player, int amount) {
		Resident resident;
		Town town;
		try {
			if (amount < 0)
				throw new TownyException(TownySettings.getLangString("msg_err_negative_money")); //TODO
			
			resident = plugin.getTownyUniverse().getResident(player.getName());
			town = resident.getTown();
			
			town.withdrawFromBank(resident, amount);
			plugin.getTownyUniverse().sendTownMessage(town, String.format(TownySettings.getLangString("msg_xx_withdrew_xx"), resident.getName(), amount, "town"));
		} catch (TownyException x) {
			plugin.sendErrorMsg(player, x.getError());
		} catch (IConomyException x) {
			plugin.sendErrorMsg(player, x.getError());
		}
	}
	
	private void townDeposit(Player player, int amount) {
		Resident resident;
		Town town;
		try {
			resident = plugin.getTownyUniverse().getResident(player.getName());
			town = resident.getTown();
			
			if (amount < 0)
				throw new TownyException(TownySettings.getLangString("msg_err_negative_money")); //TODO
			
			if (!resident.pay(amount, town))
				throw new TownyException(TownySettings.getLangString("msg_insuf_funds"));
			
			plugin.getTownyUniverse().sendTownMessage(town, String.format(TownySettings.getLangString("msg_xx_deposited_xx"), resident.getName(), amount, "town"));
		} catch (TownyException x) {
			plugin.sendErrorMsg(player, x.getError());
		} catch (IConomyException x) {
			plugin.sendErrorMsg(player, x.getError());
		}
	}

	
}
