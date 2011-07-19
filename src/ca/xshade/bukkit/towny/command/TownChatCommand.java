package ca.xshade.bukkit.towny.command;


import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import ca.xshade.bukkit.towny.NotRegisteredException;
import ca.xshade.bukkit.towny.Towny;
import ca.xshade.bukkit.towny.object.Resident;
import ca.xshade.bukkit.towny.object.Town;
import ca.xshade.bukkit.util.ChatTools;
import ca.xshade.bukkit.util.Colors;
import ca.xshade.util.StringMgmt;

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
			
			if (args.length != 0)
				parseTownChatCommand(player, StringMgmt.join(args, " "));
		}
		return true;
	}
	
	public void parseTownChatCommand(Player player, String msg) {
		try {
			Resident resident = plugin.getTownyUniverse().getResident(player.getName());
			Town town = resident.getTown();
			String line = Colors.Blue + "[" + town.getName() + "] "
					+ player.getDisplayName() + ": "
					+ Colors.LightBlue + msg;
			plugin.getTownyUniverse().sendTownMessage(town, ChatTools.color(line));
		} catch (NotRegisteredException x) {
			player.sendMessage(x.getError());
		}
	}

}
