package com.palmergames.bukkit.towny.command;


import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.palmergames.bukkit.towny.Towny;
import com.palmergames.bukkit.towny.TownyChat;
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
			TownyChat.setDisplayName(plugin, player);
			
			if (args.length != 0)
				TownyChat.parseTownChatCommand(plugin, player, StringMgmt.join(args, " "));
		}
		return true;
	}

}
