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
		output.add(ChatTools.formatCommand(TownySettings.getLangString("res_sing"), "/plot claim", "",TownySettings.getLangString("msg_block_claim")));
		output.add(ChatTools.formatCommand(TownySettings.getLangString("res_sing") + "/Mayor", "/plot notforsale", "", TownySettings.getLangString("msg_plot_nfs")));
		output.add(ChatTools.formatCommand(TownySettings.getLangString("res_sing") + "/Mayor", "/plot forsale [$]", "", TownySettings.getLangString("msg_plot_fs")));
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
					WorldCoord coord = new WorldCoord(world, Coord.parseCoord(player));
					if (residentClaim(resident, new WorldCoord(world, Coord.parseCoord(player)))) {
						plugin.sendMsg(player, TownySettings.getLangString("msg_claimed") + " (" + coord + ").");

						plugin.updateCache(coord);
						plugin.getTownyUniverse().getDataSource().saveResident(resident);
						plugin.getTownyUniverse().getDataSource().saveWorld(world);
					}
				} else if (split[0].equalsIgnoreCase("unclaim")) {
					WorldCoord coord = new WorldCoord(world, Coord.parseCoord(player));
					residentUnclaim(resident, new WorldCoord(world, Coord.parseCoord(player)), false);

					plugin.sendMsg(player, TownySettings.getLangString("msg_unclaimed") + " (" + coord + ").");

					plugin.updateCache(coord);
					plugin.getTownyUniverse().getDataSource().saveResident(resident);
					plugin.getTownyUniverse().getDataSource().saveWorld(world);
				} else if (split[0].equalsIgnoreCase("notforsale") || split[0].equalsIgnoreCase("nfs")) {
					WorldCoord worldCoord = new WorldCoord(world, Coord.parseCoord(player));
					setPlotForSale(resident, worldCoord, -1);
				} else if (split[0].equalsIgnoreCase("forsale") || split[0].equalsIgnoreCase("fs")) {
					WorldCoord worldCoord = new WorldCoord(world, Coord.parseCoord(player));
					if (split.length > 1)
						setPlotForSale(resident, worldCoord, Integer.parseInt(split[1]));
					else
						setPlotForSale(resident, worldCoord, worldCoord.getTownBlock().getTown().getPlotPrice());
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
					if (townBlock.isForSale() != -1) {
						if (TownySettings.isUsingIConomy() && !resident.pay(townBlock.isForSale(), owner))
							throw new TownyException(TownySettings.getLangString("msg_no_money_purchase_plot"));
						if (resident.getTownBlocks().size() + 1 > TownySettings.getMaxPlotsPerResident())
							throw new TownyException(String.format(TownySettings.getLangString("msg_no_money_purchase_plot"), TownySettings.getMaxPlotsPerResident()));
						
						plugin.getTownyUniverse().sendTownMessage(town, TownySettings.getBuyResidentPlotMsg(resident.getName(), owner.getName(), townBlock.isForSale()));
						townBlock.setForSale(-1);
						townBlock.setResident(resident);
						plugin.getTownyUniverse().getDataSource().saveResident(owner);
						return true;
					} else if (town.isMayor(resident) || town.hasAssistant(resident)) {
						if (TownySettings.isUsingIConomy() && !town.pay(town.getPlotPrice(), owner))
							throw new TownyException(TownySettings.getLangString("msg_town_no_money_purchase_plot"));
						
						plugin.getTownyUniverse().sendTownMessage(town, TownySettings.getBuyResidentPlotMsg(town.getName(), owner.getName(), town.getPlotPrice()));
						townBlock.setResident(null);
						townBlock.setForSale(-1);
						return true;
					} else
						throw new AlreadyRegisteredException(String.format(TownySettings.getLangString("msg_already_claimed"), owner.getName()));
				} catch (NotRegisteredException e) {
					if (townBlock.isForSale() == -1)
						throw new TownyException(TownySettings.getLangString("msg_err_plot_nfs"));
					
					if (TownySettings.isUsingIConomy() && !resident.pay(town.getPlotPrice(), town))
						throw new TownyException(TownySettings.getLangString("msg_no_money_purchase_plot"));
					
					townBlock.setForSale(-1);
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
				townBlock.setForSale(townBlock.getTown().getPlotPrice());
				plugin.getTownyUniverse().getDataSource().saveResident(resident);
				return true;
			} else
				throw new TownyException(TownySettings.getLangString("msg_not_own_area"));
		} catch (NotRegisteredException e) {
			throw new TownyException(TownySettings.getLangString("msg_not_own_place"));
		}
	}
	
	public void setPlotForSale(Resident resident, WorldCoord worldCoord, int forSale) throws TownyException {
		if (resident.hasTown())
			try {
				TownBlock townBlock = worldCoord.getTownBlock();
				Town town = townBlock.getTown();
				if (resident.getTown() != town)
					throw new TownyException(TownySettings.getLangString("msg_err_not_part_town"));

				if (town.isMayor(resident) || town.hasAssistant(resident))
					townBlock.setForSale(forSale);
				else
					try {
						Resident owner = townBlock.getResident();
						if (resident != owner)
							throw new AlreadyRegisteredException(TownySettings.getLangString("msg_not_own_area"));
						townBlock.setForSale(forSale);
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
