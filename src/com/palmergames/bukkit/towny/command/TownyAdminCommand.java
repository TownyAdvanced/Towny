package com.palmergames.bukkit.towny.command;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.palmergames.bukkit.towny.AlreadyRegisteredException;
import com.palmergames.bukkit.towny.EmptyTownException;
import com.palmergames.bukkit.towny.NotRegisteredException;
import com.palmergames.bukkit.towny.Towny;
import com.palmergames.bukkit.towny.TownyException;
import com.palmergames.bukkit.towny.TownyFormatter;
import com.palmergames.bukkit.towny.TownySettings;
import com.palmergames.bukkit.towny.TownyUtil;
import com.palmergames.bukkit.towny.object.Coord;
import com.palmergames.bukkit.towny.object.Nation;
import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.towny.object.Town;
import com.palmergames.bukkit.towny.object.TownBlock;
import com.palmergames.bukkit.towny.object.TownyEconomyObject;
import com.palmergames.bukkit.towny.object.TownyUniverse;
import com.palmergames.bukkit.towny.object.TownyWorld;
import com.palmergames.bukkit.towny.object.WorldCoord;
import com.palmergames.bukkit.util.ChatTools;
import com.palmergames.bukkit.util.Colors;
import com.palmergames.util.MemMgmt;
import com.palmergames.util.StringMgmt;

/**
 * Send a list of all general townyadmin help commands to player
 * Command: /townyadmin
 */

public class TownyAdminCommand implements CommandExecutor  {
        
        private static Towny plugin;
        private static final List<String> ta_help = new ArrayList<String>();
        private static final List<String> ta_panel = new ArrayList<String>();
        private static final List<String> ta_unclaim = new ArrayList<String>();
        
        static {
                ta_help.add(ChatTools.formatTitle("/townyadmin"));
                ta_help.add(ChatTools.formatCommand("", "/townyadmin", "", TownySettings.getLangString("admin_panel_1")));
                ta_help.add(ChatTools.formatCommand("", "/townyadmin", "set [] .. []", "'/townyadmin set' " + TownySettings.getLangString("res_5")));
                //ta_help.add(ChatTools.formatCommand("", "/townyadmin", "war toggle [on/off]", ""));
                //ta_help.add(ChatTools.formatCommand("", "/townyadmin", "war neutral [on/off]", ""));
                ta_help.add(ChatTools.formatCommand("", "/townyadmin", "givebonus [town] [num]", ""));
                ta_help.add(ChatTools.formatCommand("", "/townyadmin", "toggle neutral/war", ""));
                ta_help.add(ChatTools.formatCommand("", "/townyadmin", "          debug/devmode", ""));

                //TODO: ta_help.add(ChatTools.formatCommand("", "/townyadmin", "npc rename [old name] [new name]", ""));
                //TODO: ta_help.add(ChatTools.formatCommand("", "/townyadmin", "npc list", ""));
                ta_help.add(ChatTools.formatCommand("", "/townyadmin", "reload", TownySettings.getLangString("admin_panel_2")));
                ta_help.add(ChatTools.formatCommand("", "/townyadmin", "reset", ""));
                ta_help.add(ChatTools.formatCommand("", "/townyadmin", "backup", ""));
                ta_help.add(ChatTools.formatCommand("", "/townyadmin", "newday", TownySettings.getLangString("admin_panel_3")));
                
                ta_unclaim.add(ChatTools.formatTitle("/townyadmin unclaim"));
                ta_unclaim.add(ChatTools.formatCommand(TownySettings.getLangString("admin_sing"), "/townyadmin unclaim", "", TownySettings.getLangString("townyadmin_help_1")));
                ta_unclaim.add(ChatTools.formatCommand(TownySettings.getLangString("admin_sing"), "/townyadmin unclaim", "[radius]", TownySettings.getLangString("townyadmin_help_2")));
                
        }
        
        public TownyAdminCommand(Towny instance) {
                plugin = instance;
        }       

        @Override
        public boolean onCommand(CommandSender sender, Command cmd, String commandLabel, String[] args) {
                
                if (sender instanceof Player) {
                        Player player = (Player)sender;
                        System.out.println("[PLAYER_COMMAND] " + player.getName() + ": /" + commandLabel + " " + StringMgmt.join(args));
                        if (plugin.isTownyAdmin(player))
                                try {
                                        parseTownyAdminCommand(player,args);
                                } catch (TownyException e) {
                                        // TODO Auto-generated catch block
                                        e.printStackTrace();
                                }
                        else
                                sender.sendMessage(Colors.strip(TownySettings.getLangString("msg_err_admin_only")));
                } else
                        // Console
                        if (args.length == 0)
                        for (String line : ta_help)
                                sender.sendMessage(Colors.strip(line));
                        else if (args[0].equalsIgnoreCase("reload"))
                                reloadTowny(null, false);
                        else if (args[0].equalsIgnoreCase("reset"))
                                reloadTowny(null, true);
                        else if (args[0].equalsIgnoreCase("backup"))
                                try {
										TownyUniverse.getDataSource().backup();
                                        sender.sendMessage(Colors.strip(TownySettings.getLangString("mag_backup_success")));
                                } catch (IOException e) {
                                        sender.sendMessage(Colors.strip("Error: " + e.getMessage()));
                                }
                return true;
        }
        
        public void parseTownyAdminCommand(Player player, String[] split) throws TownyException {
                if (split.length == 0){
                        buildTAPanel();
                        for (String line : ta_panel)
                                player.sendMessage(line);
                } else if (split[0].equalsIgnoreCase("?") || split[0].equalsIgnoreCase("help"))
                        for (String line : ta_help)
                                player.sendMessage(line);
                else if (split[0].equalsIgnoreCase("set"))
                        adminSet(player, StringMgmt.remFirstArg(split));
                else if (split[0].equalsIgnoreCase("town"))
                        parseAdminTownCommand(player, StringMgmt.remFirstArg(split));
                else if (split[0].equalsIgnoreCase("nation"))
                        parseAdminNationCommand(player, StringMgmt.remFirstArg(split));
                else if (split[0].equalsIgnoreCase("toggle"))
                        parseToggleCommand(player, StringMgmt.remFirstArg(split));
                else if (split[0].equalsIgnoreCase("givebonus"))
                        try {
                                if (split.length != 3)
                                        throw new TownyException(String.format(TownySettings.getLangString("msg_err_invalid_input"), "Eg: givebonus [town] [n]"));
                                
                                Town town = plugin.getTownyUniverse().getTown(split[1]);
                                try {
                                        town.setBonusBlocks(town.getBonusBlocks() + Integer.parseInt(split[2].trim()));
                                        plugin.sendMsg(player, String.format(TownySettings.getLangString("msg_give_total"), town.getName(), split[2], town.getBonusBlocks()));
                                } catch (NumberFormatException nfe) {
                                        throw new TownyException(TownySettings.getLangString("msg_error_must_be_int"));
                                }
								TownyUniverse.getDataSource().saveTown(town);
                        } catch (TownyException e) {
                                plugin.sendErrorMsg(player, e.getError());
                        }
                else if (split[0].equalsIgnoreCase("reload"))
                        reloadTowny(player, false);
                else if (split[0].equalsIgnoreCase("reset"))
                        reloadTowny(player, true);
                else if (split[0].equalsIgnoreCase("backup"))
                        try {
								TownyUniverse.getDataSource().backup();
                                plugin.sendMsg(player, TownySettings.getLangString("mag_backup_success"));
                        } catch (IOException e) {
                                plugin.sendErrorMsg(player, "Error: " + e.getMessage());
                        }
                else if (split[0].equalsIgnoreCase("newday"))
                        plugin.getTownyUniverse().newDay();
                else if (split[0].equalsIgnoreCase("unclaim"))
                        parseAdminUnclaimCommand(player, StringMgmt.remFirstArg(split));
                else if (split[0].equalsIgnoreCase("seed") && TownySettings.getDebug())
                        seedTowny();
                else if (split[0].equalsIgnoreCase("warseed") && TownySettings.getDebug())
                        warSeed(player);
                else
                        plugin.sendErrorMsg(player, TownySettings.getLangString("msg_err_invalid_sub"));
        }
        
        private void buildTAPanel () {
                
                ta_panel.clear();
                Runtime run = Runtime.getRuntime();
                ta_panel.add(ChatTools.formatTitle(TownySettings.getLangString("ta_panel_1")));
                ta_panel.add(Colors.Blue + "[" + Colors.LightBlue + "Towny" + Colors.Blue + "] "
                                + Colors.Green + TownySettings.getLangString("ta_panel_2") + Colors.LightGreen + plugin.getTownyUniverse().isWarTime()
                                + Colors.Gray + " | "
                                + Colors.Green + TownySettings.getLangString("ta_panel_3") + (plugin.getTownyUniverse().isHealthRegenRunning() ? Colors.LightGreen + "On" : Colors.Rose + "Off")
                                + Colors.Gray + " | "
                                + (Colors.Green + TownySettings.getLangString("ta_panel_5") + (plugin.getTownyUniverse().isDailyTimerRunning() ? Colors.LightGreen + "On" : Colors.Rose + "Off")));
                /*
                ta_panel.add(Colors.Blue + "[" + Colors.LightBlue + "Towny" + Colors.Blue + "] "
                                + Colors.Green + TownySettings.getLangString("ta_panel_4")
                                + (TownySettings.isRemovingWorldMobs() ? Colors.LightGreen + "On" : Colors.Rose + "Off")
                                + Colors.Gray + " | "
                                + Colors.Green + TownySettings.getLangString("ta_panel_4_1")
                                + (TownySettings.isRemovingTownMobs() ? Colors.LightGreen + "On" : Colors.Rose + "Off"));
                */
                try {
                        TownyEconomyObject.checkEconomy();
                        ta_panel.add(Colors.Blue + "[" + Colors.LightBlue + "Economy" + Colors.Blue + "] "
                                        + Colors.Green + TownySettings.getLangString("ta_panel_6") + Colors.LightGreen + TownyFormatter.formatMoney(getTotalEconomy()) + Colors.Gray + " | "
                                        + Colors.Green + TownySettings.getLangString("ta_panel_7") + Colors.LightGreen + getNumBankAccounts());
                } catch (Exception e) {
                }
                ta_panel.add(Colors.Blue + "[" + Colors.LightBlue + TownySettings.getLangString("ta_panel_8") + Colors.Blue + "] "
                                + Colors.Green + TownySettings.getLangString("ta_panel_9") + Colors.LightGreen + MemMgmt.getMemSize(run.totalMemory()) + Colors.Gray + " | "
                                + Colors.Green + TownySettings.getLangString("ta_panel_10") + Colors.LightGreen + Thread.getAllStackTraces().keySet().size() + Colors.Gray + " | "
                                + Colors.Green + TownySettings.getLangString("ta_panel_11") + Colors.LightGreen + TownyFormatter.getTime());
                ta_panel.add(Colors.Yellow + MemMgmt.getMemoryBar(50, run));
        
        }
        
        public void parseAdminUnclaimCommand(Player player, String[] split) {

                if (split.length == 1 && split[0].equalsIgnoreCase("?")) {
                        for (String line : ta_unclaim)
                                player.sendMessage(line);
                } else {
                        TownyWorld world;
                        try {
                                if (plugin.getTownyUniverse().isWarTime())
                                        throw new TownyException(TownySettings.getLangString("msg_war_cannot_do"));
                                
								world = TownyUniverse.getWorld(player.getWorld().getName());
                                
                                List<WorldCoord> selection;
                                selection = TownyUtil.selectWorldCoordArea(null, new WorldCoord(world, Coord.parseCoord(player)), split);
                                List<Resident> residents = new ArrayList<Resident>();
                                List<Town> towns = new ArrayList<Town>();
                                
                                for (WorldCoord worldCoord : selection) {
                                        // Store town and resident data for sending messages later.
                                        try {
                                                Town town = worldCoord.getTownBlock().getTown();
                                                if (!towns.contains(town))
                                                        towns.add(town);
                                        } catch (NotRegisteredException e) {
                                        }
                                        try {
                                                Resident resident = worldCoord.getTownBlock().getResident();
                                                if (!residents.contains(resident))
                                                        residents.add(resident);
                                        } catch (NotRegisteredException e) {
                                        }
                                        residentUnclaim(player, worldCoord);
                                        TownCommand.townUnclaim(null, worldCoord, true);
                                }

                                plugin.sendMsg(player, String.format(TownySettings.getLangString("msg_admin_unclaim_area"), Arrays.toString(selection.toArray(new WorldCoord[0]))));
                                for (Resident resident : residents) {
									TownyUniverse.getDataSource().saveResident(resident);
								}
                                for (Town town : towns) {
									TownyUniverse.getDataSource().saveTown(town);
								}
								TownyUniverse.getDataSource().saveWorld(world);
                                plugin.updateCache();
                        } catch (TownyException x) {
                                plugin.sendErrorMsg(player, x.getError());
                                return;
                        }
                }
        }
        
        public void parseAdminTownCommand(Player player, String[] split) {
                //TODO Make this use the actual town command procedually.
                
                if (split.length == 0 || split[0].equalsIgnoreCase("?")) {
                        
                        player.sendMessage(ChatTools.formatTitle("/townyadmin town"));
                        player.sendMessage(ChatTools.formatCommand(TownySettings.getLangString("admin_sing"), "/townyadmin town", "[town]", ""));
                        player.sendMessage(ChatTools.formatCommand(TownySettings.getLangString("admin_sing"), "/townyadmin town", "[town] add [] .. []", ""));
                } else
                        try {
                                Town town = plugin.getTownyUniverse().getTown(split[0]);
                                if (split.length == 1)
                                        plugin.getTownyUniverse().sendMessage(player, plugin.getTownyUniverse().getStatus(town));
                                else if (split[1].equalsIgnoreCase("add"))
                                        TownCommand.townAdd(player, town, StringMgmt.remArgs(split, 2));
                        } catch (NotRegisteredException e) {
                                plugin.sendErrorMsg(player, e.getError());
                        }
        }
        
        public void parseAdminNationCommand(Player player, String[] split) {
                //TODO Make this use the actual town command procedually.
                
                if (split.length == 0 || split[0].equalsIgnoreCase("?")) {
                        
                        player.sendMessage(ChatTools.formatTitle("/townyadmin nation"));
                        player.sendMessage(ChatTools.formatCommand(TownySettings.getLangString("admin_sing"), "/townyadmin nation", "[nation]", ""));
                        player.sendMessage(ChatTools.formatCommand(TownySettings.getLangString("admin_sing"), "/townyadmin nation", "[nation] add [] .. []", ""));
                } else
                        try {
                                Nation nation = plugin.getTownyUniverse().getNation(split[0]);
                                if (split.length == 1)
                                        plugin.getTownyUniverse().sendMessage(player, plugin.getTownyUniverse().getStatus(nation));
                                else if (split[1].equalsIgnoreCase("add"))
                                        NationCommand.nationAdd(nation, plugin.getTownyUniverse().getTowns(StringMgmt.remArgs(split, 2)));
                        } catch (NotRegisteredException e) {
                                plugin.sendErrorMsg(player, e.getError());
                        } catch (AlreadyRegisteredException e) {
                                plugin.sendErrorMsg(player, e.getError());
                        }
        }
        
        public void adminSet(Player player, String[] split) {
                
                if (split.length == 0) {
                        player.sendMessage(ChatTools.formatTitle("/townyadmin set"));
                        //TODO: player.sendMessage(ChatTools.formatCommand("", "/townyadmin set", "king [nation] [king]", ""));
                        player.sendMessage(ChatTools.formatCommand("", "/townyadmin set", "mayor [town] " + TownySettings.getLangString("town_help_2"), ""));
                        player.sendMessage(ChatTools.formatCommand("", "/townyadmin set", "mayor [town] npc", ""));
                        //player.sendMessage(ChatTools.formatCommand("", "/townyadmin set", "debugmode [on/off]", ""));
                        //player.sendMessage(ChatTools.formatCommand("", "/townyadmin set", "devmode [on/off]", ""));
                } else if (split[0].equalsIgnoreCase("mayor")) {
                        if (split.length < 3) {
                                player.sendMessage(ChatTools.formatTitle("/townyadmin set mayor"));
                                player.sendMessage(ChatTools.formatCommand("Eg", "/townyadmin set mayor", "[town] " + TownySettings.getLangString("town_help_2"), ""));
                                player.sendMessage(ChatTools.formatCommand("Eg", "/townyadmin set mayor", "[town] npc", ""));
                        } else
                                try {
                                        Resident newMayor = null;
                                        Town town = plugin.getTownyUniverse().getTown(split[1]);
                                        
                                        if (split[2].equalsIgnoreCase("npc")) {
                                                String name = nextNpcName();
                                                plugin.getTownyUniverse().newResident(name);
                                                
                                                newMayor = plugin.getTownyUniverse().getResident(name);
                                                
                                                newMayor.setRegistered(System.currentTimeMillis());
                                                newMayor.setLastOnline(0);
                                                newMayor.setNPC(true);
                                                
												TownyUniverse.getDataSource().saveResident(newMayor);
												TownyUniverse.getDataSource().saveResidentList();
                                                
                                                // set for no upkeep as an NPC mayor is assigned
                                                town.setHasUpkeep(false);

                                        } else {
                                                newMayor = plugin.getTownyUniverse().getResident(split[2]);
                                                
                                                //set upkeep again
                                                town.setHasUpkeep(true);
                                        }
                                        
                                        if (!town.hasResident(newMayor))
                                                TownCommand.townAddResident(town, newMayor);
                                        // Delete the resident if the old mayor was an NPC.
                                        Resident oldMayor = town.getMayor();
                                                                                        
                                        town.setMayor(newMayor);
                                        
                                        if (oldMayor.isNPC()) {
                                                try {
                                                        town.removeResident(oldMayor);
                                                        plugin.getTownyUniverse().removeResident(oldMayor);
                                                        
                                                        plugin.getTownyUniverse().removeResidentList(oldMayor);
                                                        
                                                } catch (EmptyTownException e) {
                                                        // Should never reach here as we are setting a new mayor before removing the old one.
                                                        e.printStackTrace();
                                                }       
                                        }
										TownyUniverse.getDataSource().saveTown(town);
										String[] msg = TownySettings.getNewMayorMsg(newMayor.getName());
                                        plugin.getTownyUniverse().sendTownMessage(town, msg);
                                        //plugin.getTownyUniverse().sendMessage(player, msg);
                                } catch (TownyException e) {
                                        plugin.sendErrorMsg(player, e.getError());
                                }
                } else {
                        plugin.sendErrorMsg(player, String.format(TownySettings.getLangString("msg_err_invalid_property"), "administrative"));
                        return;
                }
        }
        
        private boolean residentUnclaim(Player player, WorldCoord worldCoord) throws TownyException {
                if (plugin.getTownyUniverse().isWarTime())
                        throw new TownyException(TownySettings.getLangString("msg_war_cannot_do"));
                
                try {
                        TownBlock townBlock = worldCoord.getTownBlock();
                        Resident owner = townBlock.getResident();

                        townBlock.setResident(null);
                        townBlock.setPlotPrice(townBlock.getTown().getPlotPrice());
						TownyUniverse.getDataSource().saveResident(owner);
                        return true;

                } catch (NotRegisteredException e) {
                        // Not a claimed area
                        //plugin.sendErrorMsg(player, e.getError());
                        return false;
                }
        }
        
        public String nextNpcName() throws TownyException {
                String name;
                int i = 0;
                do {
                        name = TownySettings.getNPCPrefix() + ++i;
                        if (!plugin.getTownyUniverse().hasResident(name))
                                return name;
                        if (i > 100000)
                                throw new TownyException(TownySettings.getLangString("msg_err_too_many_npc"));
                } while (true);
        }
        
        public void reloadTowny(Player player, Boolean reset) {
                if (reset) {
					TownyUniverse.getDataSource().deleteFile(plugin.getConfigPath());
				}
                plugin.load();
                if (player != null)
                        plugin.sendMsg(player, TownySettings.getLangString("msg_reloaded"));
                plugin.sendMsg(TownySettings.getLangString("msg_reloaded"));
        }
        
        public void parseToggleCommand(Player player, String[] split) throws TownyException {
                boolean choice;
                
                if (split.length == 0) {
                        //command was '/townyadmin toggle'
                        player.sendMessage(ChatTools.formatTitle("/townyadmin toggle"));
                        player.sendMessage(ChatTools.formatCommand("", "/townyadmin toggle", "war", ""));
                        player.sendMessage(ChatTools.formatCommand("", "/townyadmin toggle", "neutral", ""));
                        player.sendMessage(ChatTools.formatCommand("", "/townyadmin toggle", "devmode", ""));
                        player.sendMessage(ChatTools.formatCommand("", "/townyadmin toggle", "debug", ""));
                        player.sendMessage(ChatTools.formatCommand("", "/townyadmin toggle", "townwithdraw/nationwithdraw", ""));
                        return;
                        
                } else if (split[0].equalsIgnoreCase("war")) {
                        boolean isWarTime = plugin.getTownyUniverse().isWarTime();
                        choice = !isWarTime;
                        
                        if (choice) {
                                plugin.getTownyUniverse().startWarEvent();
                                plugin.sendMsg(player, TownySettings.getLangString("msg_war_started"));
                        } else {
                                plugin.getTownyUniverse().endWarEvent();
                                plugin.sendMsg(player, TownySettings.getLangString("msg_war_ended"));
                        }
                } else if (split[0].equalsIgnoreCase("neutral")) {
                        
                                try {
                                        choice = !TownySettings.isDeclaringNeutral();
                                        TownySettings.setDeclaringNeutral(choice);
                                        plugin.sendMsg(player, String.format(TownySettings.getLangString("msg_nation_allow_neutral"), choice ? "Enabled" : "Disabled"));
                                        
                                } catch (Exception e) {
                                        plugin.sendErrorMsg(player, TownySettings.getLangString("msg_err_invalid_choice"));
                                        return;
                                }
                /*
                } else if (split[0].equalsIgnoreCase("townmobs")) {

                        try {
                                choice = !TownySettings.isRemovingTownMobs();
                                plugin.setSetting("protection.mob_removal_town", choice);
                                plugin.getTownyUniverse().toggleMobRemoval(TownySettings.isRemovingWorldMobs() || TownySettings.isRemovingTownMobs() );
                                plugin.sendMsg(player, String.format(TownySettings.getLangString("msg_mobremoval_town"), choice ? "Enabled" : "Disabled"));
                                
                        } catch (Exception e) {
                                plugin.sendErrorMsg(player, TownySettings.getLangString("msg_err_invalid_choice"));
                                return;
                        }
                
                }  else if (split[0].equalsIgnoreCase("worldmobs")) {
                        
                        try {
                                choice = !TownySettings.isRemovingWorldMobs();
                                plugin.setSetting("protection.mob_removal_world", choice);
                                plugin.getTownyUniverse().toggleMobRemoval(TownySettings.isRemovingWorldMobs() || TownySettings.isRemovingTownMobs() );
                                plugin.sendMsg(player, String.format(TownySettings.getLangString("msg_mobremoval_world"), choice ? "Enabled" : "Disabled"));
                                
                        } catch (Exception e) {
                                plugin.sendErrorMsg(player, TownySettings.getLangString("msg_err_invalid_choice"));
                                return;
                        }
                */
                } else if (split[0].equalsIgnoreCase("devmode")) {
                        try {
                                choice = !TownySettings.isDevMode();
                                TownySettings.setDevMode(choice);
                                plugin.sendMsg(player, "Dev Mode " + (choice ? Colors.Green + "Enabled" : Colors.Red + "Disabled"));
                        } catch (Exception e) {
                                plugin.sendErrorMsg(player, TownySettings.getLangString("msg_err_invalid_choice"));
                        }
                } else if (split[0].equalsIgnoreCase("debug")) {
                        try {
                                choice = !TownySettings.getDebug();
                                TownySettings.setDebug(choice);
                                plugin.sendMsg(player, "Debug Mode " + (choice ? Colors.Green + "Enabled" : Colors.Red + "Disabled"));
                        } catch (Exception e) {
                                plugin.sendErrorMsg(player, TownySettings.getLangString("msg_err_invalid_choice"));
                        }
                } else if (split[0].equalsIgnoreCase("townwithdraw")) {
                		try {
                				choice = !TownySettings.getTownBankAllowWithdrawls();
                				TownySettings.SetTownBankAllowWithdrawls(choice);
                				plugin.sendMsg(player, "Town Withdrawls " + (choice ? Colors.Green + "Enabled" : Colors.Red + "Disabled"));
                		} catch (Exception e) {
                				plugin.sendErrorMsg(player, TownySettings.getLangString("msg_err_invalid_choice"));
                		}
                } else if (split[0].equalsIgnoreCase("nationwithdraw")) {
            		try {
            				choice = !TownySettings.geNationBankAllowWithdrawls();
            				TownySettings.SetNationBankAllowWithdrawls(choice);
            				plugin.sendMsg(player, "Nation Withdrawls " + (choice ? Colors.Green + "Enabled" : Colors.Red + "Disabled"));
            		} catch (Exception e) {
            				plugin.sendErrorMsg(player, TownySettings.getLangString("msg_err_invalid_choice"));
            		}
                } else {
                        // parameter error message
                        // neutral/war/townmobs/worldmobs
                        plugin.sendErrorMsg(player, TownySettings.getLangString("msg_err_invalid_choice"));
                }
        }
        
        private void warSeed(Player player) {
                /*Resident r1 = plugin.getTownyUniverse().newResident("r1");
                Resident r2 = plugin.getTownyUniverse().newResident("r2");
                Resident r3 = plugin.getTownyUniverse().newResident("r3");
                Coord key = Coord.parseCoord(player);
                Town t1 = newTown(plugin.getTownyUniverse(), player.getWorld(), "t1", r1, key, player.getLocation());
                Town t2 = newTown(plugin.getTownyUniverse(), player.getWorld(), "t2", r2, new Coord(key.getX() + 1, key.getZ()), player.getLocation());
                Town t3 = newTown(plugin.getTownyUniverse(), player.getWorld(), "t3", r3, new Coord(key.getX(), key.getZ() + 1), player.getLocation());
                Nation n1 = */
                
        }

        public void seedTowny() {
                TownyUniverse townyUniverse = plugin.getTownyUniverse();
                Random r = new Random();
                for (int i = 0; i < 1000; i++) {

                        try {
                                townyUniverse.newNation(Integer.toString(r.nextInt()));
                        } catch (TownyException e) {
                        }
                        try {
                                townyUniverse.newTown(Integer.toString(r.nextInt()));
                        } catch (TownyException e) {
                        }
                        try {
                                townyUniverse.newResident(Integer.toString(r.nextInt()));
                        } catch (TownyException e) {
                        }
                }
        }
        
        private static double getTotalEconomy() {
                double total = 0;
                try {
                        return total;
                } catch (Exception e) {
                }
                return total;
        }
        
        private static int getNumBankAccounts() {
                try {
                        return 0;
                } catch (Exception e) {
                        return 0; 
                }
        }
                

}
