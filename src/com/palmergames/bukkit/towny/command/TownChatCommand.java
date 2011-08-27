package com.palmergames.bukkit.towny.command;


import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.palmergames.bukkit.towny.NotRegisteredException;
import com.palmergames.bukkit.towny.Towny;
import com.palmergames.bukkit.towny.TownySettings;
import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.towny.object.Town;
import com.palmergames.bukkit.util.ChatTools;
import com.palmergames.bukkit.util.Colors;
import com.palmergames.util.StringMgmt;

/**
 * handles Town Chat.
 * 
 * Command: /townchat
 */

public class TownChatCommand implements CommandExecutor  {
	
	private static Towny plugin;
	
	public TownChatCommand(Towny instance) {
		plugin = instance;
	}		

	@Override
	public boolean onCommand(CommandSender sender, Command cmd, String commandLabel, String[] args) {
		
		if (sender instanceof Player) {
			Player player = (Player)sender;
			
			// Setup the chat prefix BEFORE we speak.
			plugin.setDisplayName(player);
			
			if (args.length != 0)
				parseTownChatCommand(player, StringMgmt.join(args, " "));
		}
		return true;
	}
	
	public void parseTownChatCommand(Player player, String msg) {
		try {
			Resident resident = plugin.getTownyUniverse().getResident(player.getName());
			Town town = resident.getTown();
			
			String prefix = TownySettings.getModifyChatFormat().contains("{town}") ? "" : "[" + town.getName() + "] ";
			String line = Colors.Blue + "[TC] " + prefix
					+ player.getDisplayName()
					+ Colors.White + ": "
					+ Colors.LightBlue + msg;
			plugin.getTownyUniverse().sendTownMessage(town, ChatTools.color(line));
		} catch (NotRegisteredException x) {
			plugin.sendErrorMsg(player, x.getError());
		}
	}

}
