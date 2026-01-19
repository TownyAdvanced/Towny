package com.palmergames.bukkit.towny.listeners;

import com.palmergames.bukkit.config.ConfigNodes;
import com.palmergames.bukkit.towny.TownyMessaging;
import com.palmergames.bukkit.towny.TownySettings;
import com.palmergames.bukkit.towny.object.Translatable;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

import org.bukkit.NamespacedKey;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent.Result;

public class TownyLoginListener implements Listener {
	
	private Set<String> disallowedNames = populateDisallowedNames();
	
	public TownyLoginListener() {
		TownySettings.addReloadListener(NamespacedKey.fromString("towny:login-listener"), config -> disallowedNames = populateDisallowedNames());
	}

	@EventHandler(priority = EventPriority.NORMAL)
	public void onPlayerLogin(AsyncPlayerPreLoginEvent event) {

		final String logInName = event.getName();
		boolean disallowed = isServerAccount(logInName) || isGovernmentAccount(logInName) || isPossibleNPCName(logInName);

		if (!disallowed)
			return;

		event.disallow(Result.KICK_OTHER, "Towny is preventing you from logging in using this account name.");
		TownyMessaging.sendMsgToOnlineAdmins(Translatable.of("msg_admin_blocked_login", 
				event.getAddress().toString().substring(1), logInName));
	}

	private boolean isServerAccount(String logInName) {
		return disallowedNames.contains(logInName);
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

	static boolean isPossibleNPCName(String loginName) {
		final String npcPrefix = TownySettings.getNPCPrefix();
		if (!loginName.toLowerCase(Locale.ROOT).startsWith(npcPrefix.toLowerCase(Locale.ROOT)))
			return false;

		final String sub = loginName.substring(npcPrefix.length());
		if (sub.isEmpty())
			return false;

		// A npc name can only consist of npc prefix + digits
		final Pattern onlyDigits = Pattern.compile("^\\d+$");
		return onlyDigits.matcher(sub).find();
	}

	private boolean isDisallowedName(String logInName, String disallowedName) {
		return logInName.startsWith(disallowedName) || logInName.startsWith(disallowedName.replace("-", "_")); 
	}

	private Set<String> populateDisallowedNames() {
		String serverAccount = TownySettings.getString(ConfigNodes.ECO_CLOSED_ECONOMY_SERVER_ACCOUNT);
		return new HashSet<>(Arrays.asList("towny-war-chest", "towny_war_chest", serverAccount, serverAccount.replace("-", "_")));
	}
}
