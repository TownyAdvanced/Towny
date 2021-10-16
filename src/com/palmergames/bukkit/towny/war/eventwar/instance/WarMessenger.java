package com.palmergames.bukkit.towny.war.eventwar.instance;

import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import com.palmergames.bukkit.towny.TownyMessaging;
import com.palmergames.bukkit.towny.TownyUniverse;
import com.palmergames.bukkit.towny.object.Translatable;
import com.palmergames.bukkit.towny.object.Translation;

public class WarMessenger {

	private static War war;

	public WarMessenger(War war) {
		WarMessenger.war = war;
	}

	private static List<Player> getPlayers() {
		return war.getWarParticipants().getOnlineWarriors();
	}

	public void sendGlobalMessage(String msg) {
		for (Player player : getPlayers())
			player.sendMessage(Translation.of("default_towny_prefix") + msg);
	}

	public void sendGlobalMessage(List<String> lines) {
		for (String line : lines)
			sendGlobalMessage(line);
	}
	
	public void sendPlainGlobalMessage(String msg) {
		for (Player player : getPlayers())
			player.sendMessage(msg);
	}
	
	public void sendPlainGlobalMessage(List<String> lines) {
		for (Player player : getPlayers())
			for (String line : lines)
				player.sendMessage(line);
	}
	
	public void sendPlainGlobalMessage(Translatable translatable) {
		for (Player player : getPlayers())
			TownyMessaging.sendMessage(player, translatable);
	}
	
	public void sendGlobalMessage(Translatable translatable) {
		for (Player player : getPlayers())
			TownyMessaging.sendMsg(player, translatable);
	}
	
	public static void reportErrors() {
		for (Player player : Bukkit.getOnlinePlayers())
			if (player.isOp() || TownyUniverse.getInstance().getPermissionSource().isTownyAdmin(player)) {
				TownyMessaging.sendErrorMsg(player, "War Could not be started, this may be helpful:");
				for (String line : war.getErrorMsgs())
					TownyMessaging.sendErrorMsg(player, line);
			}
	}
	
	public static void reportMinorErrors() {
		for (Player player : getPlayers())
			for (String line : war.getErrorMsgs())
				TownyMessaging.sendErrorMsg(player, line);
	}
}
