package com.palmergames.bukkit.towny.command;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.palmergames.bukkit.towny.AlreadyRegisteredException;
import com.palmergames.bukkit.towny.IConomyException;
import com.palmergames.bukkit.towny.NotRegisteredException;
import com.palmergames.bukkit.towny.Towny;
import com.palmergames.bukkit.towny.TownyException;
import com.palmergames.bukkit.towny.TownySettings;
import com.palmergames.bukkit.towny.TownyUtil;
import com.palmergames.bukkit.towny.object.Coord;
import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.towny.object.Town;
import com.palmergames.bukkit.towny.object.TownBlock;
import com.palmergames.bukkit.towny.object.TownyWorld;
import com.palmergames.bukkit.towny.object.WorldCoord;
import com.palmergames.bukkit.util.ChatTools;
import com.palmergames.bukkit.util.Colors;
import com.palmergames.util.StringMgmt;

/**
 * Send a list of all general towny plot help commands to player
 * Command: /plot
 */

public class PlotCommand implements CommandExecutor  {
        
        private static Towny plugin;
        public static final List<String> output = new ArrayList<String>();
        
        static {
                output.add(ChatTools.formatTitle("/plot"));
                output.add(ChatTools.formatCommand(TownySettings.getLangString("res_sing"), "/plot claim", "", TownySettings.getLangString("msg_block_claim")));
                output.add(ChatTools.formatCommand(TownySettings.getLangString("res_sing"), "/plot claim", "[rect/circle] [radius]", ""));
                output.add(ChatTools.formatCommand(TownySettings.getLangString("res_sing") + "/" + TownySettings.getLangString("mayor_sing"), "/plot notforsale", "", TownySettings.getLangString("msg_plot_nfs")));
                output.add(ChatTools.formatCommand(TownySettings.getLangString("res_sing") + "/" + TownySettings.getLangString("mayor_sing"), "/plot notforsale", "[rect/circle] [radius]", ""));
                output.add(ChatTools.formatCommand(TownySettings.getLangString("res_sing") + "/" + TownySettings.getLangString("mayor_sing"), "/plot forsale [$]", "", TownySettings.getLangString("msg_plot_fs")));
                output.add(ChatTools.formatCommand(TownySettings.getLangString("res_sing") + "/" + TownySettings.getLangString("mayor_sing"), "/plot forsale [$]", "within [rect/circle] [radius]", ""));
        output.add(ChatTools.formatCommand(TownySettings.getLangString("res_sing") + "/" + TownySettings.getLangString("mayor_sing"), "/plot set ...", "", TownySettings.getLangString("msg_plot_fs")));
                output.add(TownySettings.getLangString("msg_nfs_abr"));
        }

        public PlotCommand(Towny instance) {
                plugin = instance;
        }       

        @Override
        public boolean onCommand(CommandSender sender, Command cmd, String commandLabel, String[] args) {
                
                if (sender instanceof Player) {
                        Player player = (Player)sender;
                        System.out.println("[PLAYER_COMMAND] " + player.getName() + ": /" + commandLabel + " " + StringMgmt.join(args));
                        if (args == null){
                                for (String line : output)
                                        player.sendMessage(line);
                        } else{
                                try {
                                        parsePlotCommand(player, args);
                                } catch (TownyException x) {
                                        // No permisisons
                                        plugin.sendErrorMsg(player, x.getError());
                                }
                        }

                } else
                        // Console
                        for (String line : output)
                                sender.sendMessage(Colors.strip(line));
                return true;
        }
        
        public void parsePlotCommand(Player player, String[] split) throws TownyException {
                
                if ((!plugin.isTownyAdmin(player)) && ((plugin.isPermissions()) && (!plugin.hasPermission(player, "towny.town.plot"))))
                        throw new TownyException(TownySettings.getLangString("msg_err_command_disable"));
                
                if (split.length == 0 || split[0].equalsIgnoreCase("?")) {
                        for (String line : output)
                                player.sendMessage(line);
                } else {
                        Resident resident;
                        TownyWorld world;
                        try {
                                resident = plugin.getTownyUniverse().getResident(player.getName());
                                world = plugin.getTownyUniverse().getWorld(player.getWorld().getName());
                        } catch (TownyException x) {
                                plugin.sendErrorMsg(player, x.getError());
                                return;
                        }

                        try {
                                if (split[0].equalsIgnoreCase("claim")) {
                                        List<WorldCoord> selection = TownyUtil.selectWorldCoordArea(resident, new WorldCoord(world, Coord.parseCoord(player)), StringMgmt.remFirstArg(split));
                                        selection = TownyUtil.filterUnownedPlots(selection);
                                        if (selection.size() > 0) {
                                                for (WorldCoord worldCoord : selection) {
                                                        try {
                                                                if (residentClaim(resident, worldCoord)) {
                                                                        plugin.sendMsg(player, TownySettings.getLangString("msg_claimed") + " (" + worldCoord + ").");
                                                                        plugin.updateCache(worldCoord);
                                                                }       
                                                        } catch (TownyException x) {
                                                                plugin.sendErrorMsg(player, x.getError());
                                                        }
                                                }
                                                plugin.getTownyUniverse().getDataSource().saveResident(resident);
                                                plugin.getTownyUniverse().getDataSource().saveWorld(world);
                                        } else {
                                                player.sendMessage(TownySettings.getLangString("msg_err_empty_area_selection"));
                                        }
                                } else if (split[0].equalsIgnoreCase("unclaim")) {
                                        List<WorldCoord> selection = TownyUtil.selectWorldCoordArea(resident, new WorldCoord(world, Coord.parseCoord(player)), StringMgmt.remFirstArg(split));
                                        System.out.println("pre "+selection.size());
                                        selection = TownyUtil.filterOwnedBlocks(resident, selection);
                                        System.out.println("post "+selection.size());
                                        if (selection.size() > 0) {
                                                for (WorldCoord worldCoord : selection) {
                                                        try {
                                                                if (residentUnclaim(resident, worldCoord, false)) {
                                                                        plugin.sendMsg(player, TownySettings.getLangString("msg_unclaimed") + " (" + worldCoord + ").");
                                                                        plugin.updateCache(worldCoord);
                                                                }
                                                        } catch (TownyException x) {
                                                                plugin.sendErrorMsg(player, x.getError());
                                                        }
                                                }
                                                plugin.getTownyUniverse().getDataSource().saveResident(resident);
                                                plugin.getTownyUniverse().getDataSource().saveWorld(world);
                                        } else {
                                                player.sendMessage(TownySettings.getLangString("msg_err_empty_area_selection"));
                                        }
                                } else if (split[0].equalsIgnoreCase("notforsale") || split[0].equalsIgnoreCase("nfs")) {
                                        List<WorldCoord> selection = TownyUtil.selectWorldCoordArea(resident, new WorldCoord(world, Coord.parseCoord(player)), StringMgmt.remFirstArg(split));
                                        selection = TownyUtil.filterOwnedBlocks(resident.getTown(), selection);
                                        
                                        for (WorldCoord worldCoord : selection) {
                                                setPlotForSale(resident, worldCoord, -1);
                                        }
                                } else if (split[0].equalsIgnoreCase("forsale") || split[0].equalsIgnoreCase("fs")) {
                                        WorldCoord pos = new WorldCoord(world, Coord.parseCoord(player));
                                        double plotPrice = 0;
                                        switch (pos.getTownBlock().getType().ordinal()) {
                                        
                                        case 0:
                                                plotPrice = pos.getTownBlock().getTown().getPlotPrice();
                                                break;
                                        case 1:
                                                plotPrice = pos.getTownBlock().getTown().getCommercialPlotPrice();
                                                break;                                                  
                                        }

                                        if (split.length > 1) {
                                                
                                                int areaSelectPivot = TownyUtil.getAreaSelectPivot(split);
                                                List<WorldCoord> selection;
                                                if (areaSelectPivot >= 0) {
                                                        selection = TownyUtil.selectWorldCoordArea(resident, new WorldCoord(world, Coord.parseCoord(player)), StringMgmt.subArray(split, areaSelectPivot+1, split.length));
                                                        selection = TownyUtil.filterOwnedBlocks(resident.getTown(), selection);
                                                        if (selection.size() == 0) {
                                                                player.sendMessage(TownySettings.getLangString("msg_err_empty_area_selection"));
                                                                return;
                                                        }
                                                } else {
                                                        selection = new ArrayList<WorldCoord>();
                                                        selection.add(pos);
                                                }
                                                
                                                
                                        
                                                // Check that it's not: /plot forsale within rect 3
                                                if (areaSelectPivot != 1) {
                                                        try {
                                                                plotPrice = Double.parseDouble(split[1]);
                                                        } catch (NumberFormatException e) {
                                                                player.sendMessage(String.format(TownySettings.getLangString("msg_error_must_be_num")));
                                                                return;
                                                        }
                                                }
                                                
                                                for (WorldCoord worldCoord : selection) {
                                                        setPlotForSale(resident, worldCoord, plotPrice);
                                                }
                                        } else {
                                                setPlotForSale(resident, pos, plotPrice);
                                        }
                                } else if (split[0].equalsIgnoreCase("set")) {
                    if (split.length > 1) {
                        WorldCoord worldCoord = new WorldCoord(world, Coord.parseCoord(player));
                        setPlotType(resident, worldCoord, split[1]);
                        player.sendMessage(String.format(TownySettings.getLangString("msg_plot_set_type"),split[1]));

                    } else {
                        player.sendMessage(ChatTools.formatCommand("", "/plot set", "reset", ""));
                        player.sendMessage(ChatTools.formatCommand("", "/plot set", "shop", ""));
                    }
                }
                        } catch (TownyException x) {
                                plugin.sendErrorMsg(player, x.getError());
                        } catch (IConomyException x) {
                                plugin.sendErrorMsg(player, x.getError());
                        }
                }
        }
        
        public boolean residentClaim(Resident resident, WorldCoord worldCoord) throws TownyException, IConomyException {
                if (plugin.getTownyUniverse().isWarTime())
                        throw new TownyException(TownySettings.getLangString("msg_war_cannot_do"));
                
                if (resident.hasTown())
                        try {
                                TownBlock townBlock = worldCoord.getTownBlock();
                                Town town = townBlock.getTown();
                                if (resident.getTown() != town)
                                        throw new TownyException(TownySettings.getLangString("msg_not_town"));

                                try {
                                        Resident owner = townBlock.getResident();
                                        if (townBlock.getPlotPrice() != -1) {
                                                if (TownySettings.isUsingIConomy() && !resident.pay(townBlock.getPlotPrice(), owner))
                                                        throw new TownyException(TownySettings.getLangString("msg_no_money_purchase_plot"));
                                                if (resident.getTownBlocks().size() + 1 > TownySettings.getMaxPlotsPerResident())
                                                        throw new TownyException(String.format(TownySettings.getLangString("msg_no_money_purchase_plot"), TownySettings.getMaxPlotsPerResident()));
                                                
                                                plugin.getTownyUniverse().sendTownMessage(town, TownySettings.getBuyResidentPlotMsg(resident.getName(), owner.getName(), townBlock.getPlotPrice()));
                                                townBlock.setPlotPrice(-1);
                                                townBlock.setResident(resident);
                                                plugin.getTownyUniverse().getDataSource().saveResident(owner);
                                                return true;
                                        } else if (town.isMayor(resident) || town.hasAssistant(resident)) {
                                                if (TownySettings.isUsingIConomy() && !town.pay(townBlock.getPlotPrice(), owner))
                                                        throw new TownyException(TownySettings.getLangString("msg_town_no_money_purchase_plot"));
                                                
                                                plugin.getTownyUniverse().sendTownMessage(town, TownySettings.getBuyResidentPlotMsg(town.getName(), owner.getName(), townBlock.getPlotPrice()));
                                                townBlock.setResident(null);
                                                townBlock.setPlotPrice(-1);
                                                return true;
                                        } else
                                                throw new AlreadyRegisteredException(String.format(TownySettings.getLangString("msg_already_claimed"), owner.getName()));
                                } catch (NotRegisteredException e) {
                                        if (townBlock.getPlotPrice() == -1)
                                                throw new TownyException(TownySettings.getLangString("msg_err_plot_nfs"));
                                        
                                        if (TownySettings.isUsingIConomy() && !resident.pay(townBlock.getPlotPrice(), town))
                                                throw new TownyException(TownySettings.getLangString("msg_no_money_purchase_plot"));
                                        
                                        townBlock.setPlotPrice(-1);
                                        townBlock.setResident(resident);
                                        return true;
                                }
                        } catch (NotRegisteredException e) {
                                throw new TownyException(TownySettings.getLangString("msg_err_not_part_town"));
                        }
                else
                        throw new TownyException(TownySettings.getLangString("msg_err_not_in_town_claim"));
        }
        
        private boolean residentUnclaim(Resident resident, WorldCoord worldCoord, boolean force) throws TownyException {
                if (plugin.getTownyUniverse().isWarTime())
                        throw new TownyException(TownySettings.getLangString("msg_war_cannot_do"));
                
                try {
                        TownBlock townBlock = worldCoord.getTownBlock();
                        Resident owner = townBlock.getResident();
                        if (resident == owner || force) {
                                townBlock.setResident(null);
                                townBlock.setPlotPrice(townBlock.getTown().getPlotPrice());
                                plugin.getTownyUniverse().getDataSource().saveResident(resident);
                                return true;
                        } else
                                throw new TownyException(TownySettings.getLangString("msg_not_own_area"));
                } catch (NotRegisteredException e) {
                        throw new TownyException(TownySettings.getLangString("msg_not_own_place"));
                }
        }

    public void setPlotType(Resident resident, WorldCoord worldCoord, String type) throws TownyException {
        if (resident.hasTown())
                        try {
                                TownBlock townBlock = worldCoord.getTownBlock();
                                Town town = townBlock.getTown();
                                if (resident.getTown() != town)
                                        throw new TownyException(TownySettings.getLangString("msg_err_not_part_town"));

                                if (town.isMayor(resident) || town.hasAssistant(resident))
                                        townBlock.setType(type);
                                else
                    throw new TownyException(TownySettings.getLangString("msg_not_mayor_ass"));
                        } catch (NotRegisteredException e) {
                                throw new TownyException(TownySettings.getLangString("msg_err_not_part_town"));
                        }
                else
                        throw new TownyException(TownySettings.getLangString("msg_err_must_belong_town"));
    }

        public void setPlotForSale(Resident resident, WorldCoord worldCoord, double forSale) throws TownyException {
                if (resident.hasTown())
                        try {
                                TownBlock townBlock = worldCoord.getTownBlock();
                                Town town = townBlock.getTown();
                                if (resident.getTown() != town)
                                        throw new TownyException(TownySettings.getLangString("msg_err_not_part_town"));

                                if (town.isMayor(resident) || town.hasAssistant(resident))
                                        townBlock.setPlotPrice(forSale);
                                else
                                        try {
                                                Resident owner = townBlock.getResident();
                                                if (resident != owner)
                                                        throw new AlreadyRegisteredException(TownySettings.getLangString("msg_not_own_area"));
                                                townBlock.setPlotPrice(forSale);
                                        } catch (NotRegisteredException e) {
                                                throw new TownyException(TownySettings.getLangString("msg_not_own_area"));
                                        }
                                if (forSale != -1)
                                        plugin.getTownyUniverse().sendTownMessage(town, TownySettings.getPlotForSaleMsg(resident.getName(), worldCoord));
                                else
                                        plugin.getTownyUniverse().getPlayer(resident).sendMessage(TownySettings.getLangString("msg_err_plot_nfs"));
                        } catch (NotRegisteredException e) {
                                throw new TownyException(TownySettings.getLangString("msg_err_not_part_town"));
                        }
                else
                        throw new TownyException(TownySettings.getLangString("msg_err_must_belong_town"));
        }

}
