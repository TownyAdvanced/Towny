package com.palmergames.bukkit.towny.listeners;

import com.palmergames.bukkit.config.ConfigNodes;
import com.palmergames.bukkit.towny.TownyMessaging;
import com.palmergames.bukkit.towny.TownySettings;
import com.palmergames.bukkit.towny.TownyUniverse;
import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.towny.object.Translatable;

import java.util.Arrays;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent.Result;

public class TownyLoginListener implements Listener {
	
	String[] disallowedNames = populateDisallowedNames();

	@EventHandler(priority = EventPriority.NORMAL)
	public void onPlayerLogin(AsyncPlayerPreLoginEvent event) {

		String logInName = event.getName();
		boolean disallowed = isServerAccount(logInName) || isGovernmentAccount(logInName);

		if (!disallowed && logInName.startsWith(TownySettings.getNPCPrefix())) {
			Resident npcRes = TownyUniverse.getInstance().getResident(event.getUniqueId());
			disallowed = npcRes != null && npcRes.isMayor();
		}

		if (!disallowed)
			return;

		event.disallow(Result.KICK_OTHER, "Towny is preventing you from logging in using this account name.");
		TownyMessaging.sendMsgToOnlineAdmins(Translatable.of("msg_admin_blocked_login", 
				event.getAddress().toString().substring(1), logInName));
	}

	private boolean isServerAccount(String logInName) {
		return Arrays.stream(disallowedNames).anyMatch(name -> logInName.equals(name));
	}

	private boolean isGovernmentAccount(String logInName) {
		return isTownBank(logInName) || isNationBank(logInName);
	}

	private boolean isTownBank(String logInName) {
		return isDisallowedName(logInName, TownySettings.getTownAccountPrefix());
	}

	private boolean isNationBank(String logInName) {
		return isDisallowedName(logInName, TownySettings.getNationAccountPrefix());
	}

	private boolean isDisallowedName(String logInName, String disallowedName) {
		return logInName.startsWith(disallowedName) || logInName.startsWith(disallowedName.replace("-", "_")); 
	}

	private String[] populateDisallowedNames() {
		String serverAccount = TownySettings.getString(ConfigNodes.ECO_CLOSED_ECONOMY_SERVER_ACCOUNT);
		return new String[]{"towny-war-chest", "towny_war_chest", serverAccount, serverAccount.replace("-", "_")};
	}
}
