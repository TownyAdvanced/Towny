package com.palmergames.bukkit.towny.listeners;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerLoginEvent;

import com.palmergames.bukkit.config.ConfigNodes;
import com.palmergames.bukkit.towny.Towny;
import com.palmergames.bukkit.towny.TownyMessaging;
import com.palmergames.bukkit.towny.TownySettings;
import com.palmergames.bukkit.towny.exceptions.NotRegisteredException;
import com.palmergames.bukkit.towny.object.TownyUniverse;


public class TownyLoginListener implements Listener {
	
	@SuppressWarnings("unused")
	private final Towny plugin;
	
	public TownyLoginListener(Towny instance) {

		plugin = instance;
	}

	@EventHandler(priority = EventPriority.MONITOR) 
    public void onPlayerLogin(PlayerLoginEvent event) throws NotRegisteredException {
		String NPCPrefix = TownySettings.getNPCPrefix();
		String warChest = "towny-war-chest";
	    String serverAccount = TownySettings.getString(ConfigNodes.ECO_CLOSED_ECONOMY_SERVER_ACCOUNT);
		Player player = event.getPlayer();
		Boolean disallowed = false;
		
		if (player.getName().startsWith(NPCPrefix)) {
			if (TownyUniverse.getDataSource().hasResident(player.getName()))
			    if (TownyUniverse.getDataSource().getResident(player.getName()).isMayor()){
			    	// Deny because this is an NPC account which is a mayor of a town.
			    	event.disallow(null, "Towny is preventing you from logging in using this account name");
			    	disallowed = true;
			    }
				
		} else if (player.getName() == warChest || player.getName() == warChest.replace("-", "_")){
			// Deny because this is the warChest account.
			event.disallow(null, "Towny is preventing you from logging in using this account name");
			disallowed = true;
		} else if (player.getName() == serverAccount || player.getName() == serverAccount.replace("-", "_")){
			// Deny because this is the warChest account.
			event.disallow(null, "Towny is preventing you from logging in using this account name");
			disallowed = true;
		} else if (player.getName().startsWith(TownySettings.getTownAccountPrefix()) || 
				   player.getName().startsWith(TownySettings.getTownAccountPrefix().replace("-","_")) ||
				   player.getName().startsWith(TownySettings.getNationAccountPrefix()) || 
				   player.getName().startsWith(TownySettings.getNationAccountPrefix().replace("-","_")) ) {
			event.disallow(null, "Towny is preventing you from logging in using this account name");
			disallowed = true;
		}
		
		if (disallowed) {
			String msg = "A player using the IP address " + event.getAddress() + " tried to log in using account which could damage your server's economy, but was prevent by Towny. Consider banning this IP address!";
			TownyMessaging.sendMsg(msg);
			for (Player ops : Bukkit.getOnlinePlayers()) {      	 
        		if (ops.isOp() || ops.hasPermission("monitordeniedjoins.announce"))
        			TownyMessaging.sendMsg(ops, msg);        		
        	}
		}
	}
}
