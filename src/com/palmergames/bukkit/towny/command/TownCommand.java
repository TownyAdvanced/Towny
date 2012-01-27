package com.palmergames.bukkit.towny.command;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.naming.InvalidNameException;

import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import ca.xshade.bukkit.questioner.Questioner;
import ca.xshade.questionmanager.Option;
import ca.xshade.questionmanager.Question;

import com.earth2me.essentials.Teleport;
import com.earth2me.essentials.User;
import com.palmergames.bukkit.towny.AlreadyRegisteredException;
import com.palmergames.bukkit.towny.EconomyException;
import com.palmergames.bukkit.towny.EmptyTownException;
import com.palmergames.bukkit.towny.NotRegisteredException;
import com.palmergames.bukkit.towny.Towny;
import com.palmergames.bukkit.towny.TownyException;
import com.palmergames.bukkit.towny.TownyFormatter;
import com.palmergames.bukkit.towny.TownyMessaging;
import com.palmergames.bukkit.towny.TownySettings;
import com.palmergames.bukkit.towny.TownyUtil;
import com.palmergames.bukkit.towny.object.Coord;
import com.palmergames.bukkit.towny.object.Nation;
import com.palmergames.bukkit.towny.object.PlotBlockData;
import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.towny.object.Town;
import com.palmergames.bukkit.towny.object.TownBlock;
import com.palmergames.bukkit.towny.object.TownBlockOwner;
import com.palmergames.bukkit.towny.object.TownSpawnLevel;
import com.palmergames.bukkit.towny.object.TownyEconomyObject;
import com.palmergames.bukkit.towny.object.TownyPermission;
import com.palmergames.bukkit.towny.object.TownyRegenAPI;
import com.palmergames.bukkit.towny.object.TownyUniverse;
import com.palmergames.bukkit.towny.object.TownyWorld;
import com.palmergames.bukkit.towny.object.WorldCoord;
import com.palmergames.bukkit.towny.permissions.PermissionNodes;
import com.palmergames.bukkit.towny.questioner.JoinTownTask;
import com.palmergames.bukkit.towny.questioner.ResidentTownQuestionTask;
import com.palmergames.bukkit.towny.tasks.TownClaim;
import com.palmergames.bukkit.util.ChatTools;
import com.palmergames.bukkit.util.Colors;
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
                output.add(ChatTools.formatCommand("", "/town", "online", TownySettings.getLangString("town_help_10")));
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
                                Resident resident = TownyUniverse.getDataSource().getResident(player.getName());
                                Town town = resident.getTown();
                                TownyMessaging.sendMessage(player, TownyFormatter.getStatus(town));
                        } catch (NotRegisteredException x) {
                                TownyMessaging.sendErrorMsg(player, TownySettings.getLangString("msg_err_dont_belong_town"));
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
                                TownyMessaging.sendErrorMsg(player, TownySettings.getLangString("msg_specify_name"));
                        else if (split.length == 2)
                                newTown(player, split[1], player.getName());
                        else
                                // TODO: Check if player is an admin
                                newTown(player, split[1], split[2]);
                } else if (split[0].equalsIgnoreCase("leave")) {
                    townLeave(player);
        		} else if (split[0].equalsIgnoreCase("withdraw")) {
                        if (split.length == 2)
                                try {
                                        townWithdraw(player, Integer.parseInt(split[1].trim()));
                                } catch (NumberFormatException e) {
                                        TownyMessaging.sendErrorMsg(player, TownySettings.getLangString("msg_error_must_be_int"));
                                }
                        else
                                TownyMessaging.sendErrorMsg(player, String.format(TownySettings.getLangString("msg_must_specify_amnt"), "/town withdraw"));
                } else if (split[0].equalsIgnoreCase("deposit")) {
                        if (split.length == 2)
                                try {
                                        townDeposit(player, Integer.parseInt(split[1].trim()));
                                } catch (NumberFormatException e) {
                                        TownyMessaging.sendErrorMsg(player, TownySettings.getLangString("msg_error_must_be_int"));
                                }
                        else
                                TownyMessaging.sendErrorMsg(player, String.format(TownySettings.getLangString("msg_must_specify_amnt"), "/town deposit"));
                } else {
                        String[] newSplit = StringMgmt.remFirstArg(split);
                        
                        if (split[0].equalsIgnoreCase("set"))
                                townSet(player, newSplit);
                        else if (split[0].equalsIgnoreCase("buy"))
                            	townBuy(player, newSplit);
                        else if (split[0].equalsIgnoreCase("toggle"))
                            	townToggle(player, newSplit);
                        else  if (split[0].equalsIgnoreCase("mayor"))
                                townMayor(player, newSplit);
                        else if (split[0].equalsIgnoreCase("assistant"))
                                townAssistant(player, newSplit);
                        else if (split[0].equalsIgnoreCase("spawn"))
                     			townSpawn(player, newSplit, false);
                        else if (split[0].equalsIgnoreCase("outpost"))
                 				townSpawn(player, newSplit, true);
                        else if (split[0].equalsIgnoreCase("delete"))
                                townDelete(player, newSplit);
                        else if (split[0].equalsIgnoreCase("join"))
                            	parseTownJoin(player, newSplit);
                        else if (split[0].equalsIgnoreCase("add"))
                                townAdd(player, null, newSplit);
                        else if (split[0].equalsIgnoreCase("kick"))
                                townKick(player, newSplit);
                        else if (split[0].equalsIgnoreCase("claim"))
                                parseTownClaimCommand(player, newSplit);
                        else if (split[0].equalsIgnoreCase("unclaim"))
                                parseTownUnclaimCommand(player, newSplit);
                        else if (split[0].equalsIgnoreCase("online")) {
        					try {
        						Resident resident = TownyUniverse.getDataSource().getResident(player.getName());
        						Town town = resident.getTown();
        						TownyMessaging.sendMessage(player, TownyFormatter.getFormattedOnlineResidents(plugin, TownySettings.getLangString("msg_town_online"), town));
        					} catch (NotRegisteredException x) {
        						TownyMessaging.sendErrorMsg(player, TownySettings.getLangString("msg_err_dont_belong_town"));
        					}
                        } else
                                try {
                                        Town town = TownyUniverse.getDataSource().getTown(split[0]);
                                        TownyMessaging.sendMessage(player, TownyFormatter.getStatus(town));
                                } catch (NotRegisteredException x) {
                                        TownyMessaging.sendErrorMsg(player, String.format(TownySettings.getLangString("msg_err_not_registered_1"), split[0]));
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
                for (Town town : TownyUniverse.getDataSource().getTowns()) {
                	String townToken = Colors.LightBlue + town.getName();
                	townToken += town.isOpen() ? Colors.White + " (Open)" : "";
                	townToken += Colors.Blue + " [" + town.getNumResidents() + "]";
                	townToken += Colors.White;
                	formatedList.add(townToken);
                }
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
                        townAssistantsAdd(player, newSplit);
                } else if (split[0].equalsIgnoreCase("remove")) {
                        String[] newSplit = StringMgmt.remFirstArg(split);
                        townAssistantsRemove(player, newSplit);
                }
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
                player.sendMessage(ChatTools.formatCommand(TownySettings.getLangString("mayor_sing"), "/town", "[add+/kick+] " + TownySettings.getLangString("res_2"), TownySettings.getLangString("res_7")));
                player.sendMessage(ChatTools.formatCommand(TownySettings.getLangString("mayor_sing"), "/town", "set [] .. []", "'/town set' " + TownySettings.getLangString("res_5")));
                player.sendMessage(ChatTools.formatCommand(TownySettings.getLangString("mayor_sing"), "/town", "buy [] .. []", "'/town buy' " + TownySettings.getLangString("res_5")));
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
                player.sendMessage(ChatTools.formatCommand("", "/town toggle", "open", ""));
            } else {
            	Resident resident;
                Town town;
                try {
                	resident = TownyUniverse.getDataSource().getResident(player.getName());
                	town = resident.getTown();
                	if (!resident.isMayor())
                		if (!town.hasAssistant(resident))
                			throw new TownyException(TownySettings.getLangString("msg_not_mayor_ass"));
                } catch (TownyException x) {
                	TownyMessaging.sendErrorMsg(player, x.getMessage());
                	return;
                }
                
                try {
                	// TODO: Let admin's call a subfunction of this.
                	if (split[0].equalsIgnoreCase("public")) {
                		if (plugin.isPermissions() && (!TownyUniverse.getPermissionSource().hasPermission(player, PermissionNodes.TOWNY_TOGGLE_PUBLIC.getNode())))
                    		throw new Exception(TownySettings.getLangString("msg_err_command_disable"));
                    		
                     	town.setPublic(!town.isPublic());
                         TownyMessaging.sendTownMessage(town, String.format(TownySettings.getLangString("msg_changed_public"), town.isPublic() ? "Enabled" : "Disabled"));
                          
                     } else if (split[0].equalsIgnoreCase("pvp")) {
                    	//Make sure we are allowed to set these permissions.
                    	toggleTest(player,town,StringMgmt.join(split, " "));   
                        town.setPVP(!town.isPVP());
                        TownyMessaging.sendTownMessage(town, String.format(TownySettings.getLangString("msg_changed_pvp"), "Town", town.isPVP() ? "Enabled" : "Disabled"));
                                        
                    } else if (split[0].equalsIgnoreCase("explosion")) {
                    	//Make sure we are allowed to set these permissions.
                    	toggleTest(player,town,StringMgmt.join(split, " "));
                    	town.setBANG(!town.isBANG());
                    	TownyMessaging.sendTownMessage(town, String.format(TownySettings.getLangString("msg_changed_expl"), "Town", town.isBANG() ? "Enabled" : "Disabled"));

                    } else if (split[0].equalsIgnoreCase("fire")) {
                    	//Make sure we are allowed to set these permissions.
                    	toggleTest(player,town,StringMgmt.join(split, " "));
                    	town.setFire(!town.isFire());
                    	TownyMessaging.sendTownMessage(town, String.format(TownySettings.getLangString("msg_changed_fire"), "Town", town.isFire() ? "Enabled" : "Disabled"));

                    } else if (split[0].equalsIgnoreCase("mobs")) {
                    	//Make sure we are allowed to set these permissions.
                    	toggleTest(player,town,StringMgmt.join(split, " "));
                    	town.setHasMobs(!town.hasMobs());
                    	TownyMessaging.sendTownMessage(town, String.format(TownySettings.getLangString("msg_changed_mobs"), "Town", town.hasMobs() ? "Enabled" : "Disabled"));
            
                    } else if (split[0].equalsIgnoreCase("taxpercent")) {
                    	town.setTaxPercentage(!town.isTaxPercentage());
                        TownyMessaging.sendTownMessage(town, String.format(TownySettings.getLangString("msg_changed_taxpercent"), town.isTaxPercentage() ? "Enabled" : "Disabled"));
                    } else if (split[0].equalsIgnoreCase("open")) {
                    	if (plugin.isPermissions() && (!TownyUniverse.getPermissionSource().hasPermission(player, PermissionNodes.TOWNY_TOGGLE_OPEN.getNode())))
                    		throw new Exception(TownySettings.getLangString("msg_err_command_disable"));
                    	
                    	town.setOpen(!town.isOpen());
                        TownyMessaging.sendTownMessage(town, String.format(TownySettings.getLangString("msg_changed_open"), town.isOpen() ? "Enabled" : "Disabled"));
                        
                        // Send a warning when toggling on (a reminder about plot permissions).
                        if (town.isOpen())
                        	TownyMessaging.sendErrorMsg(player, String.format(TownySettings.getLangString("msg_toggle_open_on_warning")));
                    } else {
                    	TownyMessaging.sendErrorMsg(player, String.format(TownySettings.getLangString("msg_err_invalid_property"), "town"));
                    	return;
                    } 
                	
                	// Propagate perms to all unchanged, town owned, townblocks
        			for (TownBlock townBlock : town.getTownBlocks()) {
        				if (!townBlock.hasResident() && !townBlock.isChanged()) {
        					townBlock.setType(townBlock.getType());
        					TownyUniverse.getDataSource().saveTownBlock(townBlock);
        				}
        			}
                } catch (Exception e) {
                    TownyMessaging.sendErrorMsg(player, e.getMessage());
                }

				TownyUniverse.getDataSource().saveTown(town);
            }
        }
        
        private void toggleTest(Player player, Town town, String split) throws TownyException {
        	
        	//Make sure we are allowed to set these permissions.
        	
        	if (split.contains("mobs")) {
        		if (town.getWorld().isForceTownMobs()) 
                    throw new TownyException(TownySettings.getLangString("msg_world_mobs"));
        		if (plugin.isPermissions() && (!TownyUniverse.getPermissionSource().hasPermission(player, PermissionNodes.TOWNY_TOGGLE_MOBS.getNode()))) 
        			throw new TownyException(TownySettings.getLangString("msg_err_command_disable"));
        	}
        	
        	if (split.contains("fire")) {
        		if (town.getWorld().isForceFire())
        			throw new TownyException(TownySettings.getLangString("msg_world_fire"));
        		if (plugin.isPermissions() && (!TownyUniverse.getPermissionSource().hasPermission(player, PermissionNodes.TOWNY_TOGGLE_FIRE.getNode())))
        			throw new TownyException(TownySettings.getLangString("msg_err_command_disable"));
        	}
        	
        	if (split.contains("explosion")) {
        		if (town.getWorld().isForceExpl())
        			throw new TownyException(TownySettings.getLangString("msg_world_expl"));
        		if (plugin.isPermissions() && (!TownyUniverse.getPermissionSource().hasPermission(player, PermissionNodes.TOWNY_TOGGLE_EXPLOSION.getNode())))
        			throw new TownyException(TownySettings.getLangString("msg_err_command_disable"));
        	}
        	
        	if (split.contains("pvp")) {
        		if (town.getWorld().isForcePVP())
        			throw new TownyException(TownySettings.getLangString("msg_world_pvp"));
        		if (plugin.isPermissions() && (!TownyUniverse.getPermissionSource().hasPermission(player, PermissionNodes.TOWNY_TOGGLE_PVP.getNode())))
        			throw new TownyException(TownySettings.getLangString("msg_err_command_disable"));
        	}
        }
        
        public void townSet(Player player, String[] split) {
                if (split.length == 0) {
                        player.sendMessage(ChatTools.formatTitle("/town set"));
                        player.sendMessage(ChatTools.formatCommand("", "/town set", "board [message ... ]", ""));
                        player.sendMessage(ChatTools.formatCommand("", "/town set", "mayor " + TownySettings.getLangString("town_help_2"), ""));
                        player.sendMessage(ChatTools.formatCommand("", "/town set", "homeblock", ""));
                        player.sendMessage(ChatTools.formatCommand("", "/town set", "spawn/outpost", ""));
                        player.sendMessage(ChatTools.formatCommand("", "/town set", "perm ...", "'/town set perm' " + TownySettings.getLangString("res_5")));
                        //player.sendMessage(ChatTools.formatCommand("", "/town set", "pvp [on/off]", ""));
                        player.sendMessage(ChatTools.formatCommand("", "/town set", "taxes [$]", ""));
                        player.sendMessage(ChatTools.formatCommand("", "/town set", "[plottax/shoptax/embassytax] [$]", ""));
                        //player.sendMessage(ChatTools.formatCommand("", "/town set", "shoptax [$]", ""));
                        player.sendMessage(ChatTools.formatCommand("", "/town set", "[plotprice/shopprice/embassyprice] [$]", ""));
                        player.sendMessage(ChatTools.formatCommand("", "/town set", "name [name]", ""));
                        player.sendMessage(ChatTools.formatCommand("", "/town set", "tag [upto 4 letters] or clear", ""));
                        //player.sendMessage(ChatTools.formatCommand("", "/town set", "public [on/off]", ""));
                        //player.sendMessage(ChatTools.formatCommand("", "/town set", "explosion [on/off]", ""));
                        //player.sendMessage(ChatTools.formatCommand("", "/town set", "fire [on/off]", ""));
                } else {
                        Resident resident;
                        Town town;
                        TownyWorld oldWorld = null;
                        
                        try {
                                resident = TownyUniverse.getDataSource().getResident(player.getName());
                                town = resident.getTown();
                                if (!resident.isMayor())
                                        if (!town.hasAssistant(resident))
                                                throw new TownyException(TownySettings.getLangString("msg_not_mayor_ass"));
                        } catch (TownyException x) {
                                TownyMessaging.sendErrorMsg(player, x.getMessage());
                                return;
                        }

                        // TODO: Let admin's call a subfunction of this.
                        if (split[0].equalsIgnoreCase("board")) {
                                if (split.length < 2) {
                                        TownyMessaging.sendErrorMsg(player, "Eg: /town set board " + TownySettings.getLangString("town_help_9"));
                                        return;
                                } else {
                                        String line = split[1];
                                        for (int i = 2; i < split.length; i++)
                                                line += " " + split[i];
                                        town.setTownBoard(line);
                                        TownyMessaging.sendTownBoard(player, town);
                                }
                        } else if (split[0].equalsIgnoreCase("mayor")) {
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
                        if(town.isTaxPercentage() && amount > 100)
                        {
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
                                } else
                                	if (plugin.isPermissions() && (!TownyUniverse.getPermissionSource().hasPermission(player, PermissionNodes.TOWNY_TOWN_RENAME.getNode()))) {
                                		TownyMessaging.sendErrorMsg(player, TownySettings.getLangString("msg_err_command_disable"));
                                		return;
                                	}
                                		
                                    //TownyMessaging.sendErrorMsg(player, TownySettings.getLangString("msg_town_rename_disabled"));
                                    if (TownySettings.isValidRegionName(split[1]))
                                        townRename(player, town, split[1]);
                                    else
                                        TownyMessaging.sendErrorMsg(player, TownySettings.getLangString("msg_invalid_name"));
                                    
                        } else if (split[0].equalsIgnoreCase("tag")) {
                        	if (split.length < 2)
                                TownyMessaging.sendErrorMsg(player, "Eg: /town set tag PLTC");
                        	else
                        		if (split[1].equalsIgnoreCase("clear")) {
                        			try {
										town.setTag(" ");
										TownyMessaging.sendTownMessage(town, String.format(TownySettings.getLangString("msg_reset_town_tag"), player.getName()));
									} catch (TownyException e) {
										TownyMessaging.sendErrorMsg(player, e.getMessage());
									}
                        		} else
	                                try {
	                                	town.setTag(plugin.getTownyUniverse().checkAndFilterName(split[1]));
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
                                        if (plugin.getTownyUniverse().isWarTime())
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
                        } else if (split[0].equalsIgnoreCase("perm")) {
                        	//Make sure we are allowed to set these permissions.
                        	try {
                        		toggleTest(player,town,StringMgmt.join(split, " "));
                        	} catch (Exception e) {
                                TownyMessaging.sendErrorMsg(player, e.getMessage());
                                return;
                            }
                                String[] newSplit = StringMgmt.remFirstArg(split);
                                setTownBlockOwnerPermissions(player, town, newSplit);
                                plugin.updateCache();
                        } else {
                                TownyMessaging.sendErrorMsg(player, String.format(TownySettings.getLangString("msg_err_invalid_property"), "town"));
                                return;
                        }

						TownyUniverse.getDataSource().saveTown(town);
						
						// If the town (homeblock) has moved worlds we need to update the world files.
						if (oldWorld != null) {
							TownyUniverse.getDataSource().saveWorld(town.getWorld());
							TownyUniverse.getDataSource().saveWorld(oldWorld);
						}
                }
        }
        
        public void townBuy(Player player, String[] split) {
            if (split.length == 0) {
                player.sendMessage(ChatTools.formatTitle("/town buy"));
                if (TownySettings.isSellingBonusBlocks()) {
                	String line = Colors.Yellow + "[Purchased Bonus] "
	                    + Colors.Green + "Cost: " + Colors.LightGreen + "%s"
	                    + Colors.Gray + " | "
	                    + Colors.Green + "Max: " + Colors.LightGreen + "%d";
                    player.sendMessage(String.format(line, TownyFormatter.formatMoney(TownySettings.getPurchasedBonusBlocksCost()), TownySettings.getMaxPurchedBlocks()));
                    player.sendMessage(ChatTools.formatCommand("", "/town buy", "bonus [n]", ""));
                } else {
                	// Temp placeholder.
                	player.sendMessage("Nothing for sale right now.");
                }
            } else {
                Resident resident;
                Town town;
                try {
                    resident = TownyUniverse.getDataSource().getResident(player.getName());
                    town = resident.getTown();
                    if (!resident.isMayor())
                        if (!town.hasAssistant(resident))
                            throw new TownyException(TownySettings.getLangString("msg_not_mayor_ass"));
                } catch (TownyException x) {
                    TownyMessaging.sendErrorMsg(player, x.getMessage());
                    return;
                }
                try {
	                if (split[0].equalsIgnoreCase("bonus")) {
	                	if (split.length == 2) {
	                        try {
	                        	int bought = townBuyBonusTownBlocks(town, Integer.parseInt(split[1].trim()));
	                        	double cost = bought * TownySettings.getPurchasedBonusBlocksCost();
	                        	TownyMessaging.sendMsg(player, String.format(TownySettings.getLangString("msg_buy"), bought, "bonus town blocks", TownyFormatter.formatMoney(cost)));
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
         * @param town
         * @param inputN
         * @return The number of purchased bonus blocks.
         * @throws TownyException
         */
    	public static int townBuyBonusTownBlocks(Town town, int inputN) throws TownyException {
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
    		
    		try {
    			double cost = n * TownySettings.getPurchasedBonusBlocksCost();
                if (TownySettings.isUsingEconomy() && !town.pay(cost, String.format("Town Buy Bonus (%d)", n)))
                	throw new TownyException(String.format(TownySettings.getLangString("msg_no_funds_to_buy"), n, "bonus town blocks", cost + TownyEconomyObject.getEconomyCurrency()));
    	    } catch (EconomyException e1) {
                throw new TownyException("Economy Error");
    	    }
    	    
    		town.addPurchasedBlocks(n);
    		
    		return n;
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
                        
                        if (!TownyUniverse.getPermissionSource().isTownyAdmin(player) && ((TownySettings.isTownCreationAdminOnly() && !plugin.isPermissions())
                        	|| (plugin.isPermissions() && !TownyUniverse.getPermissionSource().hasPermission(player, PermissionNodes.TOWNY_TOWN_NEW.getNode()))))
                                throw new TownyException(TownySettings.getNotPermToNewTownLine());
                        
                        if (TownySettings.hasTownLimit() && TownyUniverse.getDataSource().getTowns().size() >= TownySettings.getTownLimit())
                                throw new TownyException(TownySettings.getLangString("msg_err_universe_limit"));
                        
                        if (!TownySettings.isValidRegionName(name))
                                throw new TownyException(String.format(TownySettings.getLangString("msg_err_invalid_name"), name));
                        
                        Resident resident = TownyUniverse.getDataSource().getResident(mayorName);
                        if (resident.hasTown())
                                throw new TownyException(String.format(TownySettings.getLangString("msg_err_already_res"), resident.getName()));

                        TownyWorld world = TownyUniverse.getDataSource().getWorld(player.getWorld().getName());
                        
                        if (!world.isUsingTowny())
                                throw new TownyException(TownySettings.getLangString("msg_set_use_towny_off"));
                        
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

                        newTown(universe, world, name, resident, key, player.getLocation());                    
                        TownyMessaging.sendGlobalMessage(TownySettings.getNewTownMsg(player.getName(), name));
                } catch (TownyException x) {
                        TownyMessaging.sendErrorMsg(player, x.getMessage());
                        // TODO: delete town data that might have been done
                } catch (EconomyException x) {
                        TownyMessaging.sendErrorMsg(player, x.getMessage());
                }
        }
        
        public Town newTown(TownyUniverse universe, TownyWorld world, String name, Resident resident, Coord key, Location spawn) throws TownyException {
                world.newTownBlock(key);
                TownyUniverse.getDataSource().newTown(name);
                Town town = TownyUniverse.getDataSource().getTown(name);
                town.addResident(resident);
                town.setMayor(resident);
                TownBlock townBlock = world.getTownBlock(key);
                townBlock.setTown(town);
                town.setHomeBlock(townBlock);
                town.setSpawn(spawn);
                //world.addTown(town);
                
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
                TownyMessaging.sendDebugMsg("Creating new Town account: " + "town-"+name);
                if(TownySettings.isUsingEconomy())
                {
                        town.setBalance(0);
                }
                
                TownyUniverse.getDataSource().saveResident(resident);
                TownyUniverse.getDataSource().saveTown(town);
                TownyUniverse.getDataSource().saveWorld(world);
                TownyUniverse.getDataSource().saveTownList();
                
                plugin.updateCache();
                return town;
        }
        
        public void townRename(Player player, Town town, String newName) {
                try {
                	TownyUniverse.getDataSource().renameTown(town, newName);
                        TownyMessaging.sendTownMessage(town, String.format(TownySettings.getLangString("msg_town_set_name"), player.getName(), town.getName()));
                } catch (TownyException e) {
                        TownyMessaging.sendErrorMsg(player, e.getMessage());
                }
        }
        
        public void townLeave(Player player) {
                Resident resident;
                Town town;
                try {
                        //TODO: Allow leaving town during war.
                        if (plugin.getTownyUniverse().isWarTime()) 
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
                
                try {
                        town.removeResident(resident);
                } catch (EmptyTownException et) {
                	TownyUniverse.getDataSource().removeTown(et.getTown());

                } catch (NotRegisteredException x) {
                        TownyMessaging.sendErrorMsg(player, x.getMessage());
                        return;
                }
                
				TownyUniverse.getDataSource().saveResident(resident);
				TownyUniverse.getDataSource().saveTown(town);
                
                plugin.updateCache();
                
                TownyMessaging.sendTownMessage(town, String.format(TownySettings.getLangString("msg_left_town"), resident.getName()));
                TownyMessaging.sendMsg(player, String.format(TownySettings.getLangString("msg_left_town"), resident.getName()));
        }
        
        public static void townSpawn(Player player, String[] split, Boolean outpost) {
        	try {
                boolean isTownyAdmin = TownyUniverse.getPermissionSource().isTownyAdmin(player);
                Resident resident = TownyUniverse.getDataSource().getResident(player.getName());
                Town town;
                Location spawnLoc;
                String notAffordMSG;
                TownSpawnLevel townSpawnPermission;
                
                // Set target town and affiliated messages.
                if ((split.length == 0) || ((split.length > 0) && (outpost))) {
                	town = resident.getTown();
                    notAffordMSG = TownySettings.getLangString("msg_err_cant_afford_tp");
            	} else {
            		// split.length > 1
            		town = TownyUniverse.getDataSource().getTown(split[0]);
                	notAffordMSG = String.format(TownySettings.getLangString("msg_err_cant_afford_tp_town"), town.getName());
            	}
                
                if (outpost) {
                	
                	if (!town.hasOutpostSpawn())
                		throw new TownyException(TownySettings.getLangString("msg_err_outpost_spawn"));
                	
                	Integer index;
                	try {
                		index = Integer.parseInt(split[split.length-1]);
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
                } else if (split.length == 0) {
                	townSpawnPermission = TownSpawnLevel.TOWN_RESIDENT;
                } else {
                	// split.length > 1
                	if (!resident.hasTown()) {
                		townSpawnPermission = TownSpawnLevel.UNAFFILIATED;
                	} else if (resident.getTown() == town) {
                		townSpawnPermission = TownSpawnLevel.TOWN_RESIDENT;
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
                
                if (!(isTownyAdmin || townSpawnPermission == TownSpawnLevel.TOWN_RESIDENT) && !town.isPublic())
                	throw new TownyException(TownySettings.getLangString("msg_err_not_public"));
                
                if (!isTownyAdmin) {
                    // Prevent spawn travel while in disallowed zones (if configured)
                    List<String> disallowedZones = TownySettings.getDisallowedTownSpawnZones();
                    
                    if (!disallowedZones.isEmpty()) {
                        String inTown = null;
                        try {
                            Location loc = plugin.getCache(player).getLastLocation();
                            inTown = plugin.getTownyUniverse().getTownName(loc);
                        } catch (NullPointerException e) {
                            inTown = plugin.getTownyUniverse().getTownName(player.getLocation());
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
                
                // Essentials tests
                boolean UsingESS = plugin.isEssentials();
                
                if (UsingESS && !isTownyAdmin) {
                        try {
                            User user = plugin.getEssentials().getUser(player);
                            
                            if (!user.isJailed()) {
                            	
                                Teleport teleport = user.getTeleport();
                                if (!chunk.isLoaded()) chunk.load();
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
                
                // Show message if we are using iConomy and are charging for spawn travel.
                if (travelCost > 0 && TownySettings.isUsingEconomy() && resident.payTo(travelCost, town, String.format("Town Spawn (%s)", townSpawnPermission))) {
                	TownyMessaging.sendMsg(player, String.format(TownySettings.getLangString("msg_cost_spawn"), 
                    		TownyEconomyObject.getFormattedBalance(travelCost))); // + TownyEconomyObject.getEconomyCurrency()));
                }
                
                
                // If an Admin or Essentials teleport isn't being used, use our own.
                if(isTownyAdmin) {
                	if (player.getVehicle() != null)
                		player.getVehicle().eject();
                	if (!chunk.isLoaded()) chunk.load();
                    player.teleport(spawnLoc);
                    return;
                }
                
                if (!UsingESS) {
                    if (plugin.getTownyUniverse().isTeleportWarmupRunning()) {
                    	// Use teleport warmup
                        player.sendMessage(String.format(TownySettings.getLangString("msg_town_spawn_warmup"),
                                TownySettings.getTeleportWarmupTime()));
                        plugin.getTownyUniverse().requestTeleport(player, town, travelCost);
                    } else {
                    	// Don't use teleport warmup
                    	if (player.getVehicle() != null)
                    		player.getVehicle().eject();
                    	if (!chunk.isLoaded()) chunk.load();
                        player.teleport(spawnLoc);
                    }
                }
            } catch (TownyException e) {
                TownyMessaging.sendErrorMsg(player, e.getMessage());
            } catch (EconomyException e) {
                TownyMessaging.sendErrorMsg(player, e.getMessage());
            }
        }
        
        public void townDelete(Player player, String[] split) {
                if (split.length == 0)
                        try {
                                Resident resident = TownyUniverse.getDataSource().getResident(player.getName());
                                Town town = resident.getTown();

                                if (!resident.isMayor())
                                    throw new TownyException(TownySettings.getLangString("msg_not_mayor"));
                                if (plugin.isPermissions() && (!TownyUniverse.getPermissionSource().hasPermission(player, PermissionNodes.TOWNY_TOWN_DELETE.getNode())))
                                	throw new TownyException(TownySettings.getLangString("msg_err_command_disable"));
                                
                                TownyUniverse.getDataSource().removeTown(town);
                                TownyMessaging.sendGlobalMessage(TownySettings.getDelTownMsg(town));
                        } catch (TownyException x) {
                                TownyMessaging.sendErrorMsg(player, x.getMessage());
                                return;
                        }
                else
                        try {
                                if (!TownyUniverse.getPermissionSource().isTownyAdmin(player))
                                        throw new TownyException(TownySettings.getLangString("msg_err_admin_only_delete_town"));
                                Town town = TownyUniverse.getDataSource().getTown(split[0]);
                                TownyUniverse.getDataSource().removeTown(town);
                                TownyMessaging.sendGlobalMessage(TownySettings.getDelTownMsg(town));
                        } catch (TownyException x) {
                                TownyMessaging.sendErrorMsg(player, x.getMessage());
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

        public static void townKick(Player player, String[] names) {
                Resident resident;
                Town town;
                try {
                        resident = TownyUniverse.getDataSource().getResident(player.getName());
                        town = resident.getTown();
                        if (!resident.isMayor())
                                if (!town.hasAssistant(resident))
                                        throw new TownyException(TownySettings.getLangString("msg_not_mayor_ass"));
                } catch (TownyException x) {
                        TownyMessaging.sendErrorMsg(player, x.getMessage());
                        return;
                }

                townKickResidents(player, resident, town, plugin.getTownyUniverse().getValidatedResidents(player, names));
                
                plugin.updateCache();
        }

	/*
	private static List<Resident> getResidents(Player player, String[] names) {
	        List<Resident> invited = new ArrayList<Resident>();
	        for (String name : names)
	                try {
	                        Resident target = plugin.getTownyUniverse().getResident(name);
	                        invited.add(target);
	                } catch (TownyException x) {
	                        TownyMessaging.sendErrorMsg(player, x.getMessage());
	                }
	        return invited;
	}
	*/
	public static void townAddResidents(Object sender, Town town, List<Resident> invited) {

		ArrayList<Resident> remove = new ArrayList<Resident>();
		for (Resident newMember : invited)
			try {
				// only add players with the right permissions.
				if (plugin.isPermissions()) {
					if (Bukkit.getServer().matchPlayer(newMember.getName()).isEmpty()) { //Not online
						TownyMessaging.sendErrorMsg(sender, String.format(TownySettings.getLangString("msg_offline_no_join"), newMember.getName()));
						remove.add(newMember);
					} else if (!TownyUniverse.getPermissionSource().hasPermission(Bukkit.getServer().getPlayer(newMember.getName()), PermissionNodes.TOWNY_TOWN_RESIDENT.getNode())) {
						TownyMessaging.sendErrorMsg(sender, String.format(TownySettings.getLangString("msg_not_allowed_join"), newMember.getName()));
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
				TownyMessaging.sendErrorMsg(sender, e.getMessage());
			}
		for (Resident newMember : remove)
			invited.remove(newMember);

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
        }

        private static void townInviteResident(Town town, Resident newMember) throws AlreadyRegisteredException {
                Plugin test = Bukkit.getServer().getPluginManager().getPlugin("Questioner");
                
                if (TownySettings.isUsingQuestioner() && test != null && test instanceof Questioner && test.isEnabled()) {
                        Questioner questioner = (Questioner)test;
                        questioner.loadClasses();
                        
                        List<Option> options = new ArrayList<Option>();
                        options.add(new Option(TownySettings.questionerAccept(), new JoinTownTask(newMember, town)));
                        options.add(new Option(TownySettings.questionerDeny(), new ResidentTownQuestionTask(newMember, town) {
                                @Override
                                public void run() {
                                	TownyMessaging.sendTownMessage(getTown(), String.format(TownySettings.getLangString("msg_deny_invite"), getResident().getName()));
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
        
	public static void townKickResidents(Object sender, Resident resident, Town town, List<Resident> kicking) {

		Player player = null;

		if (sender instanceof Player)
			player = (Player) sender;

		ArrayList<Resident> remove = new ArrayList<Resident>();
		for (Resident member : kicking)
			if (resident == member || member.isMayor() || town.hasAssistant(member))
				remove.add(member);
			else
				try {
					town.removeResident(member);
					plugin.deleteCache(member.getName());
					TownyUniverse.getDataSource().saveResident(member);
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
				Player p = Bukkit.getServer().getPlayer(member.getName());
				if (p != null)
					p.sendMessage(String.format(TownySettings.getLangString("msg_kicked_by"), (player != null)? player.getName() : "CONSOLE"));
			}
			msg = msg.substring(0, msg.length() - 2);
			msg = String.format(TownySettings.getLangString("msg_kicked"), (player != null)? player.getName() : "CONSOLE", msg);
			TownyMessaging.sendTownMessage(town, ChatTools.color(msg));
			TownyUniverse.getDataSource().saveTown(town);
		} else
			TownyMessaging.sendErrorMsg(sender, TownySettings.getLangString("msg_invalid_name"));
	}
        
        /**
         * Confirm player is a mayor or assistant, then get list of filter names
         * with online players and invite them to town. Command: /town add
         * [resident] .. [resident]
         * 
         * @param player
         * @param names
         */

        public void townAssistantsAdd(Player player, String[] names) {
                Resident resident;
                Town town;
                try {
                        resident = TownyUniverse.getDataSource().getResident(player.getName());
                        town = resident.getTown();
                        if (!resident.isMayor())
                                throw new TownyException(TownySettings.getLangString("msg_not_mayor"));
                } catch (TownyException x) {
                        TownyMessaging.sendErrorMsg(player, x.getMessage());
                        return;
                }

                townAssistantsAdd(player, town, plugin.getTownyUniverse().getValidatedResidents(player, names));
        }

	public void townAssistantsAdd(Player player, Town town, List<Resident> invited) {
		//TODO: change variable names from townAdd copypasta
		ArrayList<Resident> remove = new ArrayList<Resident>();
		for (Resident newMember : invited)
			try {
				town.addAssistant(newMember);
				plugin.deleteCache(newMember.getName());
				TownyUniverse.getDataSource().saveResident(newMember);
			} catch (AlreadyRegisteredException e) {
				remove.add(newMember);
			} catch (NotRegisteredException e) {
				remove.add(newMember);
				TownyMessaging.sendErrorMsg(player, e.getMessage());
			}
		for (Resident newMember : remove)
			invited.remove(newMember);

		if (invited.size() > 0) {
			String msg = "";

			for (Resident newMember : invited)
				msg += newMember.getName() + ", ";
			msg = String.format(TownySettings.getLangString("msg_raised_ass"), player.getName(), msg, "town");
			TownyMessaging.sendTownMessage(town, ChatTools.color(msg));
			TownyUniverse.getDataSource().saveTown(town);
		} else
			TownyMessaging.sendErrorMsg(player, TownySettings.getLangString("msg_invalid_name"));
	}

        /**
         * Confirm player is a mayor or assistant, then get list of filter names
         * with online players and kick them from town. Command: /town kick
         * [resident] .. [resident]
         * 
         * @param player
         * @param names
         */

        public void townAssistantsRemove(Player player, String[] names) {
                Resident resident;
                Town town;
                try {
                        resident = TownyUniverse.getDataSource().getResident(player.getName());
                        town = resident.getTown();
                        if (!resident.isMayor())
                                throw new TownyException(TownySettings.getLangString("msg_not_mayor"));
                } catch (TownyException x) {
                        TownyMessaging.sendErrorMsg(player, x.getMessage());
                        return;
                }

                townAssistantsRemove(player, resident, town, plugin.getTownyUniverse().getValidatedResidents(player, names));
        }

        public void townAssistantsRemove(Player player, Resident resident, Town town, List<Resident> kicking) {
                ArrayList<Resident> remove = new ArrayList<Resident>();
                List<Resident> toKick = new ArrayList<Resident>(kicking);
                
                for (Resident member : toKick)
                        try {
                                town.removeAssistant(member);
                                plugin.deleteCache(member.getName());
								TownyUniverse.getDataSource().saveResident(member);
								TownyUniverse.getDataSource().saveTown(town);
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
                                p = Bukkit.getServer().getPlayer(member.getName());
                                if (p != null)
                                        p.sendMessage(String.format(TownySettings.getLangString("msg_lowered_to_res_by"), player.getName()));
                        }
                        msg = msg.substring(0, msg.length()-2);
                        msg = String.format(TownySettings.getLangString("msg_lowered_to_res"), player.getName(), msg);
                        TownyMessaging.sendTownMessage(town, ChatTools.color(msg));
						TownyUniverse.getDataSource().saveTown(town);
                } else
                        TownyMessaging.sendErrorMsg(player, TownySettings.getLangString("msg_invalid_name"));
        }
        
        /**
         * If no arguments are given (or error), send usage of command.
         * If sender is a player: args = [town].
         * Elsewise: args = [resident] [town]
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
	        		
	        		Player player = (Player)sender;
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
    			TownyMessaging.sendTownMessage(town,  ChatTools.color(String.format(TownySettings.getLangString("msg_join_town"), resident.getName())));
	    		
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
        		name = ((Player)sender).getName();
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
                        if (!TownyUniverse.getPermissionSource().isTownyAdmin((Player)sender) && !resident.isMayor() && !town.hasAssistant(resident))
                                throw new TownyException(TownySettings.getLangString("msg_not_mayor_ass"));
                	}
                        
                } catch (TownyException x) {
                        TownyMessaging.sendErrorMsg(sender, x.getMessage());
                        return;
                }

                townAddResidents(sender, town, plugin.getTownyUniverse().getValidatedResidents(sender, names));
                
                plugin.updateCache();
        }
        
        // wrapper function for non friend setting of perms
        public static void setTownBlockOwnerPermissions(Player player, TownBlockOwner townBlockOwner, String[] split) {
                
                setTownBlockPermissions(player, townBlockOwner, townBlockOwner.getPermissions(), split, false);
                
        }

	public static void setTownBlockPermissions(Player player, TownBlockOwner townBlockOwner, TownyPermission perm, String[] split, boolean friend) {

		// TODO: switches
		if (split.length == 0 || split[0].equalsIgnoreCase("?")) {
			player.sendMessage(ChatTools.formatTitle("/... set perm"));
			player.sendMessage(ChatTools.formatCommand("Level", "[resident/ally/outsider]", "", ""));
			player.sendMessage(ChatTools.formatCommand("Type", "[build/destroy/switch/itemuse]", "", ""));
			player.sendMessage(ChatTools.formatCommand("", "set perm", "[on/off]", "Toggle all permissions"));
			player.sendMessage(ChatTools.formatCommand("", "set perm", "[level/type] [on/off]", ""));
			player.sendMessage(ChatTools.formatCommand("", "set perm", "[level] [type] [on/off]", ""));
			if (townBlockOwner instanceof Town)
				player.sendMessage(ChatTools.formatCommand("Eg", "/town set perm", "ally off", ""));
			if (townBlockOwner instanceof Resident)
				player.sendMessage(ChatTools.formatCommand("Eg", "/resident|plot set perm", "friend build on", ""));
			player.sendMessage(String.format(TownySettings.getLangString("plot_perms"), "'friend'", "'resident'"));
			player.sendMessage(TownySettings.getLangString("plot_perms_1"));
		} else {
			//TownyPermission perm = townBlockOwner.getPermissions();

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
							townBlock.setChanged(false);
							TownyUniverse.getDataSource().saveTownBlock(townBlock);
						}
					}
					if (townBlockOwner instanceof Town)
						TownyMessaging.sendMsg(player, String.format(TownySettings.getLangString("msg_set_perms_reset"), "Town owned"));
					else
						TownyMessaging.sendMsg(player, String.format(TownySettings.getLangString("msg_set_perms_reset"), "your"));

					plugin.updateCache();
					return;
				} else
					try {
						perm.setAll(plugin.parseOnOff(split[0]));
					} catch (Exception e) {
					}
			} else if (split.length == 2)
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
					} else if (split[0].equalsIgnoreCase("pvp")) {
						perm.pvp = b;
					} else if (split[0].equalsIgnoreCase("fire")) {
						perm.fire = b;
					} else if (split[0].equalsIgnoreCase("explosion")) {
						perm.explosion = b;
					} else if (split[0].equalsIgnoreCase("mobs")) {
						perm.mobs = b;
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

			// Propagate perms to all unchanged, town owned, townblocks
			for (TownBlock townBlock : townBlockOwner.getTownBlocks()) {
				if ((townBlockOwner instanceof Town) && (!townBlock.hasResident())) {
					if (!townBlock.isChanged()) {
						townBlock.setType(townBlock.getType());
						TownyUniverse.getDataSource().saveTownBlock(townBlock);
					}
				}
			}
			//String perms = perm.toString();
			//change perm name to friend is this is a resident setting
			//if (friend)
			//	perms = perms.replaceAll("resident", "friend");
			TownyMessaging.sendMsg(player, TownySettings.getLangString("msg_set_perms"));
			TownyMessaging.sendMessage(player, (Colors.Green + " Perm: " + ((townBlockOwner instanceof Resident)  ? perm.getColourString().replace("f", "r") : perm.getColourString()) ));
			TownyMessaging.sendMessage(player, Colors.Green + "PvP: " + ((perm.pvp) ? Colors.Red + "ON" : Colors.LightGreen + "OFF") + Colors.Green + "  Explosions: " + ((perm.explosion) ? Colors.Red + "ON" : Colors.LightGreen + "OFF") + Colors.Green + "  Firespread: " + ((perm.fire) ? Colors.Red + "ON" : Colors.LightGreen + "OFF") + Colors.Green + "  Mob Spawns: " + ((perm.mobs) ? Colors.Red + "ON" : Colors.LightGreen + "OFF"));
			plugin.updateCache();
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
				if (plugin.getTownyUniverse().isWarTime())
					throw new TownyException(TownySettings.getLangString("msg_war_cannot_do"));

				if (!TownyUniverse.getPermissionSource().isTownyAdmin(player) && plugin.isPermissions() && !TownyUniverse.getPermissionSource().hasPermission(player, PermissionNodes.TOWNY_TOWN_CLAIM.getNode()))
					throw new TownyException(TownySettings.getLangString("msg_no_perms_claim"));

				resident = TownyUniverse.getDataSource().getResident(player.getName());
				town = resident.getTown();
				if (!resident.isMayor() && !town.hasAssistant(resident))
					throw new TownyException(TownySettings.getLangString("msg_not_mayor_ass"));
				world = TownyUniverse.getDataSource().getWorld(player.getWorld().getName());

				if (!world.isUsingTowny())
					throw new TownyException(TownySettings.getLangString("msg_set_use_towny_off"));

				double blockCost = 0;
				List<WorldCoord> selection;
				boolean attachedToEdge = true, outpost = false;
				Coord key = Coord.parseCoord(plugin.getCache(player).getLastLocation());

				if (split.length == 1 && split[0].equalsIgnoreCase("outpost")) {
					if ((TownySettings.isAllowingOutposts())
						&& (!plugin.isPermissions() || ((plugin.isPermissions()) && TownyUniverse.getPermissionSource().hasPermission(player, PermissionNodes.TOWNY_TOWN_CLAIM_OUTPOST.getNode())))){

						if (world.hasTownBlock(key))
							throw new TownyException(String.format(TownySettings.getLangString("msg_already_claimed_1"), key));

						if (world.getMinDistanceFromOtherTowns(key) < TownySettings.getMinDistanceFromTownHomeblocks())
							throw new TownyException(TownySettings.getLangString("msg_too_close"));

						selection = new ArrayList<WorldCoord>();
						selection.add(new WorldCoord(world, key));
						blockCost = TownySettings.getOutpostCost();
						attachedToEdge = false;
						outpost = true;
					} else
						throw new TownyException(TownySettings.getLangString("msg_outpost_disable"));
				} else {
					selection = TownyUtil.selectWorldCoordArea(town, new WorldCoord(world, key), split);
					blockCost = TownySettings.getClaimPrice();
				}

				TownyMessaging.sendDebugMsg("townClaim: Pre-Filter Selection " + Arrays.toString(selection.toArray(new WorldCoord[0])));
				selection = TownyUtil.filterTownOwnedBlocks(selection);
				TownyMessaging.sendDebugMsg("townClaim: Post-Filter Selection " + Arrays.toString(selection.toArray(new WorldCoord[0])));
				checkIfSelectionIsValid(town, selection, attachedToEdge, blockCost, false);

				try {
					double cost = blockCost * selection.size();
					if (TownySettings.isUsingEconomy() && !town.pay(cost, String.format("Town Claim (%d)", selection.size())))
						throw new TownyException(String.format(TownySettings.getLangString("msg_no_funds_claim"), selection.size(), cost + TownyEconomyObject.getEconomyCurrency()));
				} catch (EconomyException e1) {
					throw new TownyException("Economy Error");
				}

				new TownClaim(plugin, player, town, selection, outpost, true, false).start();

				//for (WorldCoord worldCoord : selection)
				//        townClaim(town, worldCoord);

				//TownyUniverse.getDataSource().saveTown(town);
				//TownyUniverse.getDataSource().saveWorld(world);

				//plugin.sendMsg(player, String.format(TownySettings.getLangString("msg_annexed_area"), Arrays.toString(selection.toArray(new WorldCoord[0]))));
				//plugin.updateCache();
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
                                if (plugin.getTownyUniverse().isWarTime())
                                        throw new TownyException(TownySettings.getLangString("msg_war_cannot_do"));
                                
                                resident = TownyUniverse.getDataSource().getResident(player.getName());
                                town = resident.getTown();
                                if (!resident.isMayor())
                                        if (!town.hasAssistant(resident))
                                                throw new TownyException(TownySettings.getLangString("msg_not_mayor_ass"));
								world = TownyUniverse.getDataSource().getWorld(player.getWorld().getName());
                                
                                List<WorldCoord> selection;
                                if (split.length == 1 && split[0].equalsIgnoreCase("all"))
                                	new TownClaim(plugin, player, town, null, false, false, false).start();
                                        //townUnclaimAll(town);
                                else {
                                        selection = TownyUtil.selectWorldCoordArea(town, new WorldCoord(world, Coord.parseCoord(plugin.getCache(player).getLastLocation())), split);
                                        selection = TownyUtil.filterOwnedBlocks(town, selection);
                                        
                                        // Set the area to unclaim
                                        new TownClaim(plugin, player, town, selection, false, false, false).start();
                                        
                                        //for (WorldCoord worldCoord : selection)
                                        //        townUnclaim(town, worldCoord, false);
        
                                        TownyMessaging.sendMsg(player, String.format(TownySettings.getLangString("msg_abandoned_area"), Arrays.toString(selection.toArray(new WorldCoord[0]))));
                                }
								TownyUniverse.getDataSource().saveTown(town);
								TownyUniverse.getDataSource().saveWorld(world);
                                plugin.updateCache();
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
                        TownyMessaging.sendDebugMsg("Claim Check Available: " + available);
                        TownyMessaging.sendDebugMsg("Claim Selection Size: " + selection.size());
                        if (available - selection.size() < 0)
                                throw new TownyException(TownySettings.getLangString("msg_err_not_enough_blocks"));
                }
                
                try {
                        double cost = blockCost * selection.size();
                        if (TownySettings.isUsingEconomy() && !owner.canPayFromHoldings(cost))
                                throw new TownyException(String.format(TownySettings.getLangString("msg_err_cant_afford_blocks"), selection.size(), cost + TownyEconomyObject.getEconomyCurrency()));
                } catch (EconomyException e1) {
                        throw new TownyException("Economy Error");
                }
        }
        /*
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
                        if (town.getWorld().isUsingPlotManagementRevert()) {
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
                TownyMessaging.sendTownMessage(town, TownySettings.getLangString("msg_abandoned_area_1"));
                
                return true;
        }
        */
        
        private void townWithdraw(Player player, int amount) {
                Resident resident;
                Town town;
                try {
                        if(!TownySettings.getTownBankAllowWithdrawls())
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
                                if(amount + town.getHoldingBalance() > bankcap)
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
