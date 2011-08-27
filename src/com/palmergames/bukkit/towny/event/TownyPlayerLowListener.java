package com.palmergames.bukkit.towny.event;

import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerChatEvent;
import org.bukkit.event.player.PlayerListener;

import com.palmergames.bukkit.towny.NotRegisteredException;
import com.palmergames.bukkit.towny.Towny;
import com.palmergames.bukkit.towny.TownySettings;
import com.palmergames.bukkit.towny.object.Nation;
import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.towny.object.Town;
import com.palmergames.bukkit.util.ChatTools;
import com.palmergames.bukkit.util.Colors;


/**
 * Handle chat processing when a player has set mode nc or tc
 * 
 * @author Shade
 * 
 */
public class TownyPlayerLowListener extends PlayerListener {
	private final Towny plugin;

	public TownyPlayerLowListener(Towny instance) {
		this.plugin = instance;
	}

	@Override
	public void onPlayerChat(PlayerChatEvent event) {
		if (event.isCancelled())
			return;
		
		Player player = event.getPlayer();
		
		// Setup the chat prefix BEFORE we speak.
		plugin.setDisplayName(player);
		
		if (plugin.hasPlayerMode(player, "tc"))
			parseTownChatCommand(player, event.getMessage());
		else if (plugin.hasPlayerMode(player, "nc")) 
			parseNationChatCommand(player, event.getMessage());
		else {
			// All chat modes are disabled, or this is open chat.
			return;
		}
		event.setCancelled(true);
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

	public void parseNationChatCommand(Player player, String msg) {
		try {
			Resident resident = plugin.getTownyUniverse().getResident(player.getName());
			Nation nation = resident.getTown().getNation();
			
			String prefix = TownySettings.getModifyChatFormat().contains("{nation}") ? "" : "[" + nation.getName() + "] ";
			String line = Colors.Gold + "[NC] " + prefix
					+ player.getDisplayName()
					+ Colors.White + ": "
					+ Colors.Yellow + msg;
			plugin.getTownyUniverse().sendNationMessage(nation, ChatTools.color(line));
		} catch (NotRegisteredException x) {
			plugin.sendErrorMsg(player, x.getError());
		}
	}
}