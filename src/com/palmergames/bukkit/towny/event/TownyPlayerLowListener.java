package com.palmergames.bukkit.towny.event;

import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerChatEvent;
import org.bukkit.event.player.PlayerListener;

import com.palmergames.bukkit.towny.NotRegisteredException;
import com.palmergames.bukkit.towny.Towny;
import com.palmergames.bukkit.towny.TownyChat;
import com.palmergames.bukkit.towny.object.Resident;


/**
 * Handle chat processing when a player has set mode nc or tc
 * 
 * @author Shade, ElgarL
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
		Resident resident;
		try {
			resident = plugin.getTownyUniverse().getResident(player.getName());
		} catch (NotRegisteredException e) {
			return;
		}
		
		// Setup the chat prefix BEFORE we speak.
		TownyChat.setDisplayName(plugin, player);
		
		if (plugin.hasPlayerMode(player, "tc"))
			TownyChat.parseTownChatCommand(plugin, player, event.getMessage());
		else if (plugin.hasPlayerMode(player, "nc")) 
			TownyChat.parseNationChatCommand(plugin, player, event.getMessage());
		else {
			// All chat modes are disabled, or this is open chat.
			event.setFormat(event.getFormat().replace("%1$s", resident.getChatFormattedName()));
			//player.setDisplayName(resident.getChatFormattedName());
			return;
		}
		event.setCancelled(true);
	}
	
	
}