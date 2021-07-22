package com.palmergames.bukkit.towny.listeners;

import com.palmergames.bukkit.config.ConfigNodes;
import com.palmergames.bukkit.towny.TownyMessaging;
import com.palmergames.bukkit.towny.TownySettings;
import com.palmergames.bukkit.towny.TownyUniverse;
import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.util.Colors;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerLoginEvent;


public class TownyLoginListener implements Listener {
	@EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerLogin(PlayerLoginEvent event) {
		String npcPrefix = TownySettings.getNPCPrefix();
		String warChest = "towny-war-chest";
	    String serverAccount = TownySettings.getString(ConfigNodes.ECO_CLOSED_ECONOMY_SERVER_ACCOUNT);		
		boolean disallowed = false;
		Player player = event.getPlayer();
		TownyUniverse townyUniverse = TownyUniverse.getInstance();
		
		if (player.getName().startsWith(npcPrefix)) {
			Resident npcRes = townyUniverse.getResident(player.getName());
			if (npcRes != null && npcRes.isMayor()) {
				event.disallow(PlayerLoginEvent.Result.KICK_OTHER, "Towny is preventing you from logging in using this account name.");
				disallowed = true;
			}
		} else if (player.getName().equals(warChest) || player.getName().equals(warChest.replace("-", "_"))){
			// Deny because this is the warChest account.
			event.disallow(PlayerLoginEvent.Result.KICK_OTHER, "Towny is preventing you from logging in using this account name.");
			disallowed = true;
		} else if (player.getName().equals(serverAccount) || player.getName().equals(serverAccount.replace("-", "_"))){
			// Deny because this is the closed economy server account.
			event.disallow(PlayerLoginEvent.Result.KICK_OTHER, "Towny is preventing you from logging in using this account name.");
			disallowed = true;
		} else if (player.getName().startsWith(TownySettings.getTownAccountPrefix()) || 
				   player.getName().startsWith(TownySettings.getTownAccountPrefix().replace("-","_")) ||
				   player.getName().startsWith(TownySettings.getNationAccountPrefix()) || 
				   player.getName().startsWith(TownySettings.getNationAccountPrefix().replace("-","_")) ) {
			event.disallow(PlayerLoginEvent.Result.KICK_OTHER, "Towny is preventing you from logging in using this account name.");
			disallowed = true;
		}
		
		if (disallowed) {
			String ip = event.getAddress().toString();
			ip = ip.substring(1);
			String msg = "A player using the IP address " + Colors.Red + ip + Colors.Green + " tried to log in using an account name (" + event.getPlayer().getName() + ") which could damage your server's economy, but was prevented by Towny. Consider banning this IP address!";
			TownyMessaging.sendMsg(msg);
			for (Player ops : Bukkit.getOnlinePlayers()) {     	 
        		if (ops.isOp() || ops.hasPermission("towny.admin"))
        			TownyMessaging.sendMsg(ops, msg);        		
        	}
		}
	}
}
