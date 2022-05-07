package com.palmergames.bukkit.towny.listeners;

import com.palmergames.bukkit.config.ConfigNodes;
import com.palmergames.bukkit.towny.Towny;
import com.palmergames.bukkit.towny.TownyMessaging;
import com.palmergames.bukkit.towny.TownySettings;
import com.palmergames.bukkit.towny.TownyUniverse;
import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.towny.object.Translatable;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent.Result;

public class TownyLoginListener implements Listener {
	@EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerLogin(AsyncPlayerPreLoginEvent event) {
		String npcPrefix = TownySettings.getNPCPrefix();
		String warChest = "towny-war-chest";
	    String serverAccount = TownySettings.getString(ConfigNodes.ECO_CLOSED_ECONOMY_SERVER_ACCOUNT);		
		boolean disallowed = false;
		TownyUniverse townyUniverse = TownyUniverse.getInstance();
		
		if (event.getName().startsWith(npcPrefix)) {
			Resident npcRes = townyUniverse.getResident(event.getUniqueId());
			if (npcRes != null && npcRes.isMayor()) {
				event.disallow(Result.KICK_OTHER, "Towny is preventing you from logging in using this account name.");
				disallowed = true;
			}
		} else if (event.getName().equals(warChest) || event.getName().equals(warChest.replace("-", "_"))){
			// Deny because this is the warChest account.
			event.disallow(Result.KICK_OTHER, "Towny is preventing you from logging in using this account name.");
			disallowed = true;
		} else if (event.getName().equals(serverAccount) || event.getName().equals(serverAccount.replace("-", "_"))){
			// Deny because this is the closed economy server account.
			event.disallow(Result.KICK_OTHER, "Towny is preventing you from logging in using this account name.");
			disallowed = true;
		} else if (event.getName().startsWith(TownySettings.getTownAccountPrefix()) || 
				   event.getName().startsWith(TownySettings.getTownAccountPrefix().replace("-","_")) ||
				   event.getName().startsWith(TownySettings.getNationAccountPrefix()) || 
				   event.getName().startsWith(TownySettings.getNationAccountPrefix().replace("-","_")) ) {
			event.disallow(Result.KICK_OTHER, "Towny is preventing you from logging in using this account name.");
			disallowed = true;
		}
		
		if (disallowed) {
			String ip = event.getAddress().toString().substring(1);
			Towny.getPlugin().getLogger().warning("A player using the IP address " + ip + " tried to log in using an account name (" + event.getName() + ") which could damage your server's economy, but was prevented by Towny. Consider banning this IP address!");
			for (Player ops : Bukkit.getOnlinePlayers()) {     	 
        		if (ops.isOp() || ops.hasPermission("towny.admin"))
        			TownyMessaging.sendMsg(ops, Translatable.of("msg_admin_blocked_login", ip, event.getName()));        		
        	}
		}
	}
}
