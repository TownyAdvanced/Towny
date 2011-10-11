package com.palmergames.bukkit.towny.command;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.palmergames.bukkit.towny.Towny;
import com.palmergames.bukkit.towny.TownyChat;
import com.palmergames.bukkit.towny.TownySettings;
import com.palmergames.bukkit.util.ChatTools;
import com.palmergames.bukkit.util.Colors;
import com.palmergames.util.StringMgmt;

/**
 * Send a list of all general nationchat help commands to player
 * Command: /nationchat
 */

public class NationChatCommand implements CommandExecutor  {
	
	private static Towny plugin;
	public static final List<String> output = new ArrayList<String>();
	
	static {
		output.add(ChatTools.formatTitle(TownySettings.getLangString("help_0")));
		output.add(TownySettings.getLangString("help_1"));
		output.add(ChatTools.formatCommand("", "/resident", "?", "")
				+ ", " + ChatTools.formatCommand("", "/town", "?", "") 
				+ ", " + ChatTools.formatCommand("", "/nation", "?", "")
				+ ", " + ChatTools.formatCommand("", "/plot", "?", "")
				+ ", " + ChatTools.formatCommand("", "/towny", "?", ""));
		output.add(ChatTools.formatCommand("", "/townchat", " [msg]", TownySettings.getLangString("help_2"))
				+ ", " + ChatTools.formatCommand("", "/nationchat", " [msg]", TownySettings.getLangString("help_3")));
		output.add(ChatTools.formatCommand(TownySettings.getLangString("admin_sing"), "/townyadmin", "?", ""));
	}
	
	public NationChatCommand(Towny instance) {
		plugin = instance;
	}	

	@Override
	public boolean onCommand(CommandSender sender, Command cmd, String commandLabel, String[] args) {
		
		if (sender instanceof Player) {
			Player player = (Player)sender;
			
			// Setup the chat prefix BEFORE we speak.
			TownyChat.setDisplayName(plugin, player);
			
			if (args == null){
				for (String line : output)
					player.sendMessage(line);
			} else{
				TownyChat.parseNationChatCommand(plugin, player, StringMgmt.join(args, " "));
			}

		} else
			// Console
			for (String line : output)
				sender.sendMessage(Colors.strip(line));
		return true;
	}

}
